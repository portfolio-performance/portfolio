package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.util.Isin;

/** The write operations on instruments. Fields are named as in the <code>instruments</code> output. */
final class InstrumentOperations
{
    private static final String ENTITY = "instrument";

    private static final List<String> TEXT_FIELDS = List.of("name", "currencyCode", "isin", "tickerSymbol", "wkn",
                    "note");
    private static final String RETIRED = "retired";

    private static final Set<String> ADD_MEMBERS = Set.of(ApplyBatch.OPERATION_MEMBER, "name", "currencyCode", "isin",
                    "tickerSymbol", "wkn", "note", RETIRED);
    private static final Set<String> UPDATE_MEMBERS = Stream.concat(ADD_MEMBERS.stream(), Stream.of("uuid"))
                    .collect(Collectors.toUnmodifiableSet());

    static final OperationSpec ADD = new OperationSpec("instrument.add",
                    "Adds an instrument. Only the name is required.", fields("instrument.add", false),
                    InstrumentOperations::add);

    static final OperationSpec UPDATE = new OperationSpec("instrument.update",
                    "Updates an instrument by UUID. Only the fields present change; null or an empty string clears an optional field. The currency can't change once the instrument has transactions.",
                    fields("instrument.update", true).property("uuid", Schema.string("UUID of the instrument.")),
                    InstrumentOperations::update);

    static final List<OperationSpec> ALL = List.of(ADD, UPDATE);

    private InstrumentOperations()
    {
    }

    private static Schema fields(String operation, boolean update)
    {
        var name = Schema.string("Instrument name; never empty.");
        var schema = Schema.object("Fields of the instrument; they match the instruments output.")
                        .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(operation)));

        if (update)
            schema.optionalProperty("name", name);
        else
            schema.property("name", name);

        return schema.optionalProperty("currencyCode",
                        Schema.string("ISO currency code; omit for an index.").nullable())
                        .optionalProperty("isin", Schema.string("ISIN: valid and not used by another instrument.")
                                        .nullable())
                        .optionalProperty("tickerSymbol", Schema.string("Ticker symbol.").nullable())
                        .optionalProperty("wkn", Schema.string("WKN.").nullable())
                        .optionalProperty("note", Schema.string("Note.").nullable())
                        .optionalProperty(RETIRED, Schema.bool(update ? "Whether the instrument is retired."
                                        : "Whether the instrument is retired; defaults to false."));
    }

    private static void add(OperationContext context)
    {
        context.rejectUnknownFields(ADD_MEMBERS);
        var values = read(context);

        if (values.get("name") == null)
            context.fail("name", "required", "An instrument needs a name.");

        validate(context, values, null);
        if (context.failed())
            return;

        var security = new Security();
        values.forEach((field, value) -> set(security, field, value));
        context.client().addSecurity(security);

        var after = new LinkedHashMap<String, Object>();
        after.put("name", security.getName());
        for (var field : TEXT_FIELDS.subList(1, TEXT_FIELDS.size()))
        {
            var value = current(security, field);
            if (value != null)
                after.put(field, value);
        }
        after.put(RETIRED, security.isRetired());

        context.created(new OperationContext.Change(ENTITY, security.getUUID(), null, after));
    }

    private static void update(OperationContext context)
    {
        context.rejectUnknownFields(UPDATE_MEMBERS);

        var security = find(context);
        var values = read(context);

        if (values.containsKey("name") && values.get("name") == null)
            context.fail("name", "required", "An instrument needs a name.");

        validate(context, values, security);
        if (security == null || context.failed())
            return;

        var before = new LinkedHashMap<String, Object>();
        var after = new LinkedHashMap<String, Object>();
        values.forEach((field, value) -> {
            var old = current(security, field);
            if (!Objects.equals(old, value))
            {
                before.put(field, old);
                after.put(field, value);
            }
        });

        after.forEach((field, value) -> set(security, field, value));

        if (!after.isEmpty())
            context.updated(new OperationContext.Change(ENTITY, security.getUUID(), before, after));
    }

    /** The instrument named by <code>uuid</code>, or null after reporting why not. */
    private static Security find(OperationContext context)
    {
        var member = context.input().get("uuid");
        if (member == null || member.isJsonNull())
        {
            context.fail("uuid", "required", "An update needs the UUID of the instrument.");
            return null;
        }
        if (!member.isJsonPrimitive() || !member.getAsJsonPrimitive().isString())
        {
            context.fail("uuid", "invalid-type", "The UUID must be a string.");
            return null;
        }

        var uuid = member.getAsString();
        var found = context.client().getSecurities().stream()
                        .filter(s -> uuid.equals(s.getUUID()) && context.existedBeforeBatch(s)).findFirst();
        if (found.isEmpty())
        {
            context.fail("uuid", "reference-not-found", "No instrument with UUID " + uuid + " in the file.");
            return null;
        }

        return context.claim("uuid", uuid) ? found.get() : null;
    }

    /**
     * The fields that are present in the input. A null or blank string counts
     * as unset and is read as null.
     */
    private static Map<String, Object> read(OperationContext context)
    {
        var values = new LinkedHashMap<String, Object>();
        JsonObject input = context.input();

        for (var field : TEXT_FIELDS)
        {
            var member = input.get(field);
            if (member == null)
                continue;

            if (member.isJsonNull())
                values.put(field, null);
            else if (member.isJsonPrimitive() && member.getAsJsonPrimitive().isString())
                values.put(field, member.getAsString().isBlank() ? null : member.getAsString());
            else
                context.fail(field, "invalid-type", "Must be a string or null.");
        }

        var retired = input.get(RETIRED);
        if (retired != null)
        {
            if (retired.isJsonPrimitive() && retired.getAsJsonPrimitive().isBoolean())
                values.put(RETIRED, retired.getAsBoolean());
            else
                context.fail(RETIRED, "invalid-type", "Must be true or false.");
        }

        return values;
    }

    private static void validate(OperationContext context, Map<String, Object> values, Security existing)
    {
        if (values.get("currencyCode") instanceof String code)
        {
            if (!CurrencyUnit.containsCurrencyCode(code))
                context.fail("currencyCode", "invalid-currency", "'" + code + "' is not a known ISO currency code.");
        }

        if (existing != null && values.containsKey("currencyCode")
                        && !Objects.equals(current(existing, "currencyCode"), values.get("currencyCode"))
                        && hasTransactions(context.client(), existing))
            context.fail("currencyCode", "currency-immutable",
                            "The currency can't change because the instrument has transactions.");

        if (values.get("isin") instanceof String isin)
        {
            if (!Isin.isValid(isin) || !isin.matches(Isin.PATTERN))
                context.fail("isin", "invalid-isin", "'" + isin + "' is not a valid ISIN.", "Isin.isValid");
            else
                context.client().getSecurities().stream()
                                .filter(other -> other != existing && isin.equalsIgnoreCase(other.getIsin()))
                                .findFirst().ifPresent(other -> context.fail("isin", "duplicate-isin",
                                                "The ISIN is already used by instrument " + other.getUUID() + "."));
        }
    }

    private static boolean hasTransactions(Client client, Security security)
    {
        return client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .anyMatch(t -> t.getSecurity() == security)
                        || client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                                        .anyMatch(t -> t.getSecurity() == security);
    }

    /** The field as the read output shows it: blank counts as unset. */
    private static Object current(Security security, String field)
    {
        var value = switch (field)
        {
            case "name" -> security.getName();
            case "currencyCode" -> security.getCurrencyCode();
            case "isin" -> security.getIsin();
            case "tickerSymbol" -> security.getTickerSymbol();
            case "wkn" -> security.getWkn();
            case "note" -> security.getNote();
            case RETIRED -> security.isRetired();
            default -> throw new IllegalArgumentException(field);
        };

        return value instanceof String text && text.isBlank() ? null : value;
    }

    private static void set(Security security, String field, Object value)
    {
        switch (field)
        {
            case "name" -> security.setName((String) value);
            case "currencyCode" -> security.setCurrencyCode((String) value);
            case "isin" -> security.setIsin((String) value);
            case "tickerSymbol" -> security.setTickerSymbol((String) value);
            case "wkn" -> security.setWkn((String) value);
            case "note" -> security.setNote((String) value);
            case RETIRED -> security.setRetired((Boolean) value);
            default -> throw new IllegalArgumentException(field);
        }
    }
}
