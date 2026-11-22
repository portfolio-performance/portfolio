package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.rest.WriteApprovals;
import name.abuchen.portfolio.util.Isin;

public final class SecuritiesHandler
{
    /**
     * validates one field of the patch; returns null if the value is acceptable
     */
    @FunctionalInterface
    private interface Validator
    {
        ApiException.FieldError validate(Client client, Security security, String field, JsonElement value);
    }

    @FunctionalInterface
    private interface Setter
    {
        void set(Security security, JsonElement value);
    }

    @FunctionalInterface
    private interface Getter
    {
        String get(Security security);
    }

    private record WritableField(Validator validator, Setter setter, Getter getter)
    {}

    private record AttributeAssignment(AttributeType type, boolean clear, Object value)
    {}

    public record PatchResult(JsonElement entity, String instrumentName, List<InstrumentChangeLog.Change> changes)
    {}

    /** the parameter behind which one custom attribute is named: {@code attribute:<id>} */
    public static final String ATTRIBUTE_PARAMETER = "attribute:"; //$NON-NLS-1$

    /**
     * How a caller narrows an instrument list beyond the free-text {@code query}:
     * to the vehicles one watchlist holds, to those assigned to one class, or to
     * those carrying one value for a custom attribute. Keys of {@code attributes}
     * are attribute ids as {@code pp_list_attribute_types} gives them, values the
     * raw text of the parameter - a query string carries text and nothing else.
     */
    public record Filters(String watchlist, String classification, Map<String, String> attributes)
    {
        /** nothing narrowed at all */
        public static final Filters NONE = new Filters(null, null, Map.of());
    }

    /** the narrowing a request names, read out of its query parameters */
    public static Filters filtersOf(Request request)
    {
        Map<String, String> attributes = Map.of();

        for (var entry : request.queryParams().entrySet())
        {
            if (!entry.getKey().startsWith(ATTRIBUTE_PARAMETER))
                continue;

            if (attributes.isEmpty())
                attributes = new LinkedHashMap<>();
            attributes.put(entry.getKey().substring(ATTRIBUTE_PARAMETER.length()), entry.getValue());
        }

        return new Filters(request.queryParam("watchlist"), request.queryParam("classification"), attributes); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * One narrowing that resolved: what it contributes to a cursor's scope, and
     * how an instrument is weighed against it.
     */
    private record Criterion(String scope, Predicate<Security> test)
    {}

    /**
     * An instrument that did not exist before this call: the answer to hand back,
     * and what to say about it in the log the desktop user reads. A
     * {@code JsonObject} rather than a {@code JsonElement} because the answer has
     * named members, and whoever reads it should not have to ask first.
     */
    public record CreationResult(JsonObject answer, String instrumentName,
                    List<InstrumentChangeLog.Change> changes)
    {}

    /**
     * The writable fields of an instrument. Validating and applying a field are
     * kept side by side so that the two cannot drift apart; a field that is not
     * listed here is not writable.
     * <p/>
     * The retired flag is deliberately absent: the model calls it "retired"
     * while the UI speaks of activating and deactivating an instrument. The
     * vocabulary must be settled before it becomes part of the API contract - a
     * published field name is hard to take back.
     */
    private static final Map<String, WritableField> WRITABLE_FIELDS = Map.of( //
                    "name", new WritableField(SecuritiesHandler::requireText, //$NON-NLS-1$
                                    (security, value) -> security.setName(value.getAsString()), Security::getName), //
                    "isin", new WritableField(SecuritiesHandler::allowIsinOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setIsin(isinOrNull(value)), Security::getIsin), //
                    "wkn", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setWkn(stringOrNull(value)), Security::getWkn), //
                    "tickerSymbol", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setTickerSymbol(stringOrNull(value)),
                                    Security::getTickerSymbol), //
                    "note", new WritableField(SecuritiesHandler::allowTextOrNull, //$NON-NLS-1$
                                    (security, value) -> security.setNote(stringOrNull(value)), Security::getNote), //
                    "currencyCode", new WritableField(SecuritiesHandler::allowCurrencyOrNull, // //$NON-NLS-1$
                                    (security, value) -> security.setCurrencyCode(stringOrNull(value)),
                                    Security::getCurrencyCode));

    private SecuritiesHandler()
    {
    }

    public static JsonElement list(Client client)
    {
        return list(client, null);
    }

    /**
     * The instruments, or with {@code query} only those it finds: a name that
     * contains it, or an ISIN, WKN or ticker symbol equal to it - all ignoring
     * case and surrounding whitespace. A file can hold more instruments than
     * one answer carries, and this is how a client gets from what the user
     * said to a uuid without paging through all of them.
     */
    public static JsonElement list(Client client, String query)
    {
        return list(client, query, null, null);
    }

    /**
     * As above, one page of them with {@code cursor} and {@code limit}: paged,
     * the instruments are in the order of their uuid, which nothing changes;
     * the whole list stays in the order of the file.
     */
    public static JsonElement list(Client client, String query, String cursorParam, String limitParam)
    {
        return list(client, null, query, cursorParam, limitParam);
    }

    /**
     * As above, for the file of this id: a cursor is good only for the file
     * and the {@code query} it came with.
     */
    public static JsonElement list(Client client, String fileId, String query, String cursorParam,
                    String limitParam)
    {
        return list(client, fileId, query, Filters.NONE, cursorParam, limitParam);
    }

    /**
     * As above, narrowed by {@code filters} first and paged after: a page is a
     * page of the narrowed list, so an instrument that does not pass a criterion
     * is not counted in {@code total} either.
     * <p/>
     * Every criterion is validated against the file before anything is answered -
     * a watchlist the file does not hold, a class id it does not define and an
     * attribute id it does not declare are refusals, not empty results, because
     * an empty answer to a question about something that does not exist reads as
     * "none of your instruments match" and sends the caller off looking elsewhere.
     */
    public static JsonElement list(Client client, String fileId, String query, Filters filters, String cursorParam,
                    String limitParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var wanted = query == null ? null : query.strip();
        if (wanted != null && wanted.isEmpty())
            errors.add(new ApiException.FieldError("query", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "query must not be blank; omit it to list every instrument")); //$NON-NLS-1$

        var criteria = resolve(client, filters == null ? Filters.NONE : filters, errors);

        // the query matches ignoring case: queries that differ in case are one list
        var scope = new StringBuilder("file=").append(fileId == null ? "" : fileId) //$NON-NLS-1$ //$NON-NLS-2$
                        .append("|query=").append(wanted == null ? "" : wanted.toLowerCase(Locale.ROOT)); //$NON-NLS-1$ //$NON-NLS-2$
        for (var criterion : criteria)
            scope.append('|').append(criterion.scope());

        var paging = Paging.parse(Paging.Kind.INSTRUMENTS, scope.toString(), cursorParam, limitParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var securities = client.getSecurities();
        if (wanted != null)
        {
            var lower = wanted.toLowerCase(Locale.ROOT);
            securities = securities.stream() //
                            .filter(security -> matches(security, wanted, lower)) //
                            .toList();
        }
        for (var criterion : criteria)
            securities = securities.stream().filter(criterion.test()).toList();

        // only the page is rendered; the rest of the list is counted, not built
        var response = new JsonObject();
        paging.apply(Paging.Kind.INSTRUMENTS, response, "items", securities, Security::getUUID, //$NON-NLS-1$
                        security -> EntityJson.toJson(client, security));
        return response;
    }

    /**
     * The narrowing turned into tests, collecting what cannot resolve on the way.
     * A criterion that resolves contributes the canonical text it was given, so
     * that a cursor is bound to the narrowing it was produced under: the same
     * page of a differently narrowed list is refused rather than silently skipped.
     */
    private static List<Criterion> resolve(Client client, Filters filters, List<ApiException.FieldError> errors)
    {
        var criteria = new ArrayList<Criterion>();

        if (filters.watchlist() != null)
        {
            var named = filters.watchlist().strip();
            var lists = client.getWatchlists().stream() //
                            .filter(list -> named.equals(list.getName())) //
                            .toList();
            if (named.isEmpty() || lists.isEmpty())
            {
                errors.add(new ApiException.FieldError("watchlist", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "no watchlist of this file is named \"" + named + "\"" + namesOfWatchlists(client))); //$NON-NLS-1$
            }
            else
            {
                // more than one under the name is possible in a file the
                // application wrote: an instrument counts if any of them holds it
                criteria.add(new Criterion("watchlist=" + named, //$NON-NLS-1$
                                security -> lists.stream().anyMatch(list -> list.getSecurities().contains(security))));
            }
        }

        if (filters.classification() != null)
        {
            var id = filters.classification().strip();
            var classes = client.getTaxonomies().stream() //
                            .flatMap(taxonomy -> taxonomy.getAllClassifications().stream()) //
                            .filter(classification -> id.equals(classification.getId())) //
                            .toList();
            if (id.isEmpty() || classes.isEmpty())
            {
                errors.add(new ApiException.FieldError("classification", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "no class of this file has the id \"" + id //$NON-NLS-1$
                                                + "\"; pp_list_taxonomies gives the classes and their ids"));
            }
            else
            {
                criteria.add(new Criterion("classification=" + id, security -> classes.stream() //
                                .anyMatch(classification -> classification.getAssignments().stream().anyMatch(
                                                assignment -> security.getUUID()
                                                                .equals(assignment.getInvestmentVehicle().getUUID())))));
            }
        }

        for (var entry : filters.attributes().entrySet())
            resolveAttribute(client, entry.getKey(), entry.getValue(), errors, criteria);

        return criteria;
    }

    /**
     * One {@code attribute:<id>} parameter. The comparison runs on the wire form
     * the API reports the value as, in both directions, so that an instrument is
     * found by exactly the text a caller could read back out of it - which is why
     * an attribute whose value never travels out cannot be filtered on either.
     */
    private static void resolveAttribute(Client client, String id, String text,
                    List<ApiException.FieldError> errors, List<Criterion> criteria)
    {
        var field = ATTRIBUTE_PARAMETER + id;

        var type = client.getSettings().getAttributeTypes().filter(candidate -> id.equals(candidate.getId()))
                        .findAny().orElse(null);
        if (type == null)
        {
            errors.add(new ApiException.FieldError(field, "unknown-attribute", //$NON-NLS-1$
                            "no instrument attribute of this file has that id; pp_list_attribute_types lists them"));
            return;
        }
        if (!type.supports(Security.class))
        {
            errors.add(new ApiException.FieldError(field, "attribute-not-applicable", //$NON-NLS-1$
                            "attribute does not apply to instruments")); //$NON-NLS-1$
            return;
        }

        var fieldType = AttributeCodec.fieldType(type);
        if (fieldType == null || !AttributeCodec.isReadable(fieldType))
        {
            errors.add(new ApiException.FieldError(field, "unsupported-attribute-type", //$NON-NLS-1$
                            "the value of this attribute type is not part of the API, so it cannot be filtered on"));
            return;
        }

        if (text == null || text.isEmpty())
        {
            // the empty parameter asks for what holds no value: a text attribute
            // can hold anything, so no other spelling of "unset" would be reachable
            criteria.add(new Criterion(field + "=", security -> valueOf(security, id) == null)); //$NON-NLS-1$
            return;
        }

        final JsonElement expected;
        try
        {
            expected = AttributeCodec.encode(fieldType, AttributeCodec.decode(fieldType, wireOf(fieldType, text)));
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            errors.add(new ApiException.FieldError(field, "invalid-value", e.getMessage())); //$NON-NLS-1$
            return;
        }

        criteria.add(new Criterion(field + "=" + expected, security -> {
            var stored = valueOf(security, id);
            if (stored == null)
                return false;

            try
            {
                return expected.equals(AttributeCodec.encode(fieldType, stored));
            }
            catch (RuntimeException e)
            {
                // a stored value inconsistent with its declared type cannot be
                // compared, and is left out of the answer like it is left out of
                // an instrument's own attributes
                return false;
            }
        }));
    }

    /** the stored value of one attribute, as the API reads it: keyed by attribute id, absent when unset */
    private static Object valueOf(Security security, String attributeId)
    {
        return security.getAttributes().getMap().get(attributeId);
    }

    private static final Set<AttributeFieldType> NUMERIC = EnumSet.of(AttributeFieldType.AMOUNT,
                    AttributeFieldType.AMOUNTPLAIN, AttributeFieldType.QUOTE, AttributeFieldType.SHARE,
                    AttributeFieldType.PERCENT, AttributeFieldType.PERCENTPLAIN);

    /**
     * A query parameter is text; the codec takes JSON. So a number is handed over
     * as a number and a boolean as one of the two words - the only readings a
     * caller typing a query string can mean. Dates, limit prices and text go
     * across as they stand and are validated by the codec itself.
     */
    private static JsonElement wireOf(AttributeFieldType fieldType, String text)
    {
        if (fieldType == AttributeFieldType.BOOLEAN)
        {
            if ("true".equalsIgnoreCase(text.strip())) //$NON-NLS-1$
                return new JsonPrimitive(true);
            if ("false".equalsIgnoreCase(text.strip())) //$NON-NLS-1$
                return new JsonPrimitive(false);
            throw new AttributeCodec.InvalidValueException("expected true or false"); //$NON-NLS-1$
        }

        if (NUMERIC.contains(fieldType))
        {
            try
            {
                return new JsonPrimitive(new BigDecimal(text.strip()));
            }
            catch (NumberFormatException e)
            {
                throw new AttributeCodec.InvalidValueException("expected a number"); //$NON-NLS-1$
            }
        }

        return new JsonPrimitive(text);
    }

    private static String namesOfWatchlists(Client client)
    {
        var lists = client.getWatchlists();
        if (lists.isEmpty())
            return " - this file has no watchlists at all"; //$NON-NLS-1$

        return " - it has: " + lists.stream().map(list -> "\"" + list.getName() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
                        .collect(java.util.stream.Collectors.joining(", ")); //$NON-NLS-1$
    }

    private static boolean matches(Security security, String wanted, String lower)
    {
        var name = security.getName();
        if (name != null && name.toLowerCase(Locale.ROOT).contains(lower))
            return true;

        for (var code : new String[] { security.getIsin(), security.getWkn(), security.getTickerSymbol() })
            if (code != null && code.strip().equalsIgnoreCase(wanted))
                return true;

        return false;
    }

    public static JsonElement get(Client client, String uuid)
    {
        return EntityJson.toJson(client, find(client, uuid));
    }

    /**
     * The attribute-type definitions that apply to an instrument, so a client
     * can resolve an id to its name and value type. A compound type is listed
     * but flagged by what the API cannot do with it: limit price and bookmark
     * neither read nor write; an image writes, yet never reads back. A type
     * whose converter this API does not recognise is omitted entirely, as it is
     * not part of the contract.
     */
    public static JsonElement attributeTypes(Client client)
    {
        var items = new JsonArray();

        client.getSettings().getAttributeTypes() //
                        .filter(type -> type.supports(Security.class)) //
                        .forEach(type -> {
                            var fieldType = AttributeCodec.fieldType(type);
                            if (fieldType == null)
                                return;

                            var item = new JsonObject();
                            item.addProperty("id", type.getId()); //$NON-NLS-1$
                            item.addProperty("name", type.getName()); //$NON-NLS-1$
                            if (type.getColumnLabel() != null)
                                item.addProperty("columnLabel", type.getColumnLabel()); //$NON-NLS-1$
                            item.addProperty("type", AttributeCodec.wireType(fieldType)); //$NON-NLS-1$
                            item.addProperty("supported", AttributeCodec.isReadable(fieldType)); //$NON-NLS-1$
                            item.addProperty("writable", AttributeCodec.isWritable(fieldType)); //$NON-NLS-1$
                            items.add(item);
                        });

        return EntityJson.envelope(items, "this file defines no custom attributes for instruments"); //$NON-NLS-1$
    }

    /**
     * What the prompt will be about, with the patch already read and rejected if it
     * cannot be carried out: a user is not asked to approve a change the request
     * could not have made, and an instrument the file does not have is turned away
     * before anybody is interrupted.
     */
    public static WriteApprovals.Facts describe(Client client, String uuid, JsonObject body, String fileLabel)
    {
        var security = find(client, uuid);

        var check = check(client, security, body);
        if (!check.errors().isEmpty())
            throw ApiException.validation(check.errors());

        return new WriteApprovals.Facts(null, fileLabel, "instrument", security.getName(), null, //$NON-NLS-1$
                        String.join(", ", body.keySet())); //$NON-NLS-1$
    }

    /**
     * A new instrument: its name, its currency and, if the caller has them, its
     * identifiers. Deliberately without a quote feed - asking a provider for quotes
     * is a side effect the caller can neither see nor order, and it would happen
     * while nobody is looking. The feed is set in the application; the master data
     * after it can be filled by the tool that changes them.
     */
    public static CreationResult create(Client client, JsonObject body)
    {
        var security = new Security();

        var check = check(client, security, body);
        var errors = check.errors();
        creationRules(client, body, errors);
        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        apply(security, body, check.assignments());
        client.addSecurity(security);
        client.markDirty();

        // answer and log name the same thing: the fields the caller named, with the
        // value the model holds of each now
        var named = namedFields(security, body, check.assignments());

        var json = new JsonObject();
        json.add("instrument", EntityJson.toJson(client, security)); //$NON-NLS-1$
        json.add("fields", fieldsOf(named)); //$NON-NLS-1$
        // what the record alone cannot say: nothing was fetched for this instrument,
        // and nothing in the file refers to it yet - which is exactly what makes the
        // delete tool able to undo this one
        json.addProperty("hasQuoteFeed", false); //$NON-NLS-1$
        json.addProperty("hasTransactions", false); //$NON-NLS-1$

        // not a refusal, and not silence either: an instrument of a name the file
        // already has is addressable through its uuid, but a caller who did not mean
        // to add a second one should hear that it now has two
        var names = sharing(client, security, true);
        if (names.size() > 0)
            json.add("sameNameAs", names); //$NON-NLS-1$
        var tickers = sharing(client, security, false);
        if (tickers.size() > 0)
            json.add("sameTickerAs", tickers); //$NON-NLS-1$

        // what the log the desktop user reads gets: the same list the answer shows.
        // A difference list would leave out the currency, which a new security
        // already carries before anything is set on it.
        var changes = named.stream() //
                        .map(f -> new InstrumentChangeLog.Change(f.field(), null, f.value())) //
                        .toList();

        return new CreationResult(json, security.getName(), changes);
    }

    /** what the prompt will name, with the creation already rejected if it cannot be */
    public static WriteApprovals.Facts describeCreation(Client client, JsonObject body, String fileLabel)
    {
        var security = new Security();

        var errors = new ArrayList<ApiException.FieldError>();
        var check = check(client, security, body);
        errors.addAll(check.errors());
        creationRules(client, body, errors);
        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        var proposed = body.get("name").getAsString().trim(); //$NON-NLS-1$
        return new WriteApprovals.Facts(null, fileLabel, "instrument", proposed, null, //$NON-NLS-1$
                        String.join(", ", body.keySet())); //$NON-NLS-1$
    }

    /** the identifiers one instrument must not share with another */
    private static final List<String> UNIQUE_IDENTIFIERS = List.of("isin", "wkn"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * What only a creation asks for: a currency, and identifiers no other
     * instrument of the file carries.
     *
     * <p>
     * A currency-less series is what an index is, so a creation insists on one and
     * the existing change tool can still take it away afterwards. A duplicate ISIN
     * is a warning in the application and a reason to do nothing here, because two
     * instruments answering to one ISIN send every later search for it to whichever
     * the file happens to list first. A ticker symbol is deliberately not among
     * these: the same ticker is ordinary for share classes and trading venues, so it
     * is reported in the answer instead of refused.
     * </p>
     */
    private static void creationRules(Client client, JsonObject body, List<ApiException.FieldError> errors)
    {
        if (!body.has("name")) //$NON-NLS-1$
            errors.add(new ApiException.FieldError("name", "required", "name is required to create an instrument")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        var currency = body.get("currencyCode"); //$NON-NLS-1$
        if (currency == null || currency.isJsonNull())
            errors.add(new ApiException.FieldError("currencyCode", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "currencyCode is required - a currency-less series is an index, and the change tool can make one")); //$NON-NLS-1$

        for (var identifier : UNIQUE_IDENTIFIERS)
        {
            var element = body.get(identifier);
            if (element == null || !isString(element))
                continue;

            var wanted = element.getAsString().trim();
            if (wanted.isEmpty())
                continue;

            for (var other : client.getSecurities())
            {
                if (!sameText(identifierOf(other, identifier), wanted))
                    continue;

                var detail = wanted + " is already the " + identifier + " of " + other.getName() //$NON-NLS-1$ //$NON-NLS-2$
                                + " (uuid " + other.getUUID() + ")" //$NON-NLS-1$ //$NON-NLS-2$
                                + " - create nothing, or change the instrument that has it"; //$NON-NLS-1$
                errors.add(new ApiException.FieldError(identifier, "duplicate-identifier", detail)); //$NON-NLS-1$
                break;
            }
        }
    }

    private static String identifierOf(Security security, String identifier)
    {
        return switch (identifier)
        {
            case "isin" -> security.getIsin(); //$NON-NLS-1$
            case "wkn" -> security.getWkn(); //$NON-NLS-1$
            default -> security.getTickerSymbol(); //$NON-NLS-1$
        };
    }

    /** other instruments of the file that answer to the same name or the same ticker */
    private static JsonArray sharing(Client client, Security security, boolean byName)
    {
        var wanted = byName ? security.getName() : security.getTickerSymbol();
        var others = new JsonArray();
        if (wanted == null || wanted.isBlank())
            return others;

        for (var other : client.getSecurities())
        {
            if (other == security)
                continue;

            if (sameText(byName ? other.getName() : other.getTickerSymbol(), wanted))
                others.add(InvestmentPlansHandler.namedReference(other.getUUID(), other.getName()));
        }

        return others;
    }

    private static boolean sameText(String one, String other)
    {
        if (one == null || other == null)
            return false;

        return one.trim().equalsIgnoreCase(other.trim());
    }

    /** one field the caller named, with the value the model holds of it */
    private record Named(String field, String value)
    {}

    /**
     * The fields the new instrument arrives with. For something that did not exist
     * before, the useful question is what the caller asked for and what the model
     * holds of it now - not what differs from an earlier value: a new security
     * already answers to the file's currency before anything is set on it, so a
     * comparison would hide the one field that is never left empty. One list for
     * the answer and for the log, so the two cannot disagree.
     */
    private static List<Named> namedFields(Security security, JsonObject body,
                    List<AttributeAssignment> assignments)
    {
        var named = new ArrayList<Named>();

        for (var entry : body.entrySet())
        {
            if ("attributes".equals(entry.getKey())) //$NON-NLS-1$
                continue;

            var field = WRITABLE_FIELDS.get(entry.getKey());
            if (field == null)
                continue;

            named.add(new Named(entry.getKey(), field.getter().get(security)));
        }

        for (var assignment : assignments)
        {
            if (assignment.clear())
                continue;

            named.add(new Named(assignment.type().getName(), renderAttribute(assignment.type(), assignment.value())));
        }

        return named;
    }

    private static JsonArray fieldsOf(List<Named> named)
    {
        var fields = new JsonArray();

        for (var f : named)
        {
            var item = new JsonObject();
            item.addProperty("field", f.field()); //$NON-NLS-1$
            item.addProperty("value", f.value()); //$NON-NLS-1$
            fields.add(item);
        }

        return fields;
    }

    /** the fields a patch or a creation names that are staged but not yet applied, and the violations */
    private record Check(List<AttributeAssignment> assignments, List<ApiException.FieldError> errors)
    {
    }

    /**
     * Reads a merge patch without touching the security: the same verdict the change
     * comes to, reached before anything is applied, so that prompt and change cannot
     * disagree about whether a request was possible.
     */
    private static Check check(Client client, Security security, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var assignments = new ArrayList<AttributeAssignment>();

        for (var entry : body.entrySet())
        {
            if ("attributes".equals(entry.getKey())) //$NON-NLS-1$
            {
                stageAttributes(client, security, entry.getValue(), errors, assignments);
                continue;
            }

            var field = WRITABLE_FIELDS.get(entry.getKey());
            if (field == null)
            {
                errors.add(new ApiException.FieldError(entry.getKey(), "unknown-field", "field is not writable")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var error = field.validator().validate(client, security, entry.getKey(), entry.getValue());
            if (error != null)
                errors.add(error);
        }

        return new Check(assignments, errors);
    }

    /**
     * Applies a JSON Merge Patch (RFC 7386) to the security: absent fields stay
     * untouched, null clears optional fields. All violations are collected and
     * reported at once; nothing is applied unless everything validates.
     */
    public static PatchResult patch(Client client, String uuid, JsonObject body)
    {
        var security = find(client, uuid);

        var check = check(client, security, body);
        var errors = check.errors();
        var assignments = check.assignments();

        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        // an empty patch changes nothing; do not mark the file dirty and
        // thereby prompt the user to save a file the API did not touch
        var hasWritableField = body.keySet().stream().anyMatch(WRITABLE_FIELDS::containsKey);
        if (!hasWritableField && assignments.isEmpty())
            return new PatchResult(EntityJson.toJson(client, security), security.getName(), List.of());

        var changes = apply(security, body, check.assignments());

        // a patch that only restates stored values changes nothing, so the user
        // is not asked to save a file the API did not change
        if (!changes.isEmpty())
            client.markDirty();
        return new PatchResult(EntityJson.toJson(client, security), security.getName(), changes);
    }

    /**
     * Writes validated fields and staged attributes onto a security, and reports
     * what moved. Shared with creation, so that both paths reach a security by
     * exactly the same steps - only one of them has values to compare against.
     */
    private static List<InstrumentChangeLog.Change> apply(Security security, JsonObject body,
                    List<AttributeAssignment> assignments)
    {
        var changes = new ArrayList<InstrumentChangeLog.Change>();

        for (var entry : body.entrySet())
        {
            if ("attributes".equals(entry.getKey())) //$NON-NLS-1$
                continue;

            var field = WRITABLE_FIELDS.get(entry.getKey());
            if (field == null)
                continue;

            var before = field.getter().get(security);
            field.setter().set(security, entry.getValue());
            var after = field.getter().get(security);

            if (!Objects.equals(before, after))
                changes.add(new InstrumentChangeLog.Change(entry.getKey(), before, after));
        }

        for (var assignment : assignments)
        {
            var type = assignment.type();
            var before = security.getAttributes().get(type);

            if (assignment.clear())
            {
                security.getAttributes().remove(type);
                if (before != null)
                    changes.add(new InstrumentChangeLog.Change(type.getName(), renderAttribute(type, before), null));
            }
            else
            {
                var after = assignment.value();
                security.getAttributes().put(type, after);
                if (!Objects.equals(before, after))
                    changes.add(new InstrumentChangeLog.Change(type.getName(),
                                    before == null ? null : renderAttribute(type, before),
                                    renderAttribute(type, after)));
            }
        }

        return changes;
    }

    /**
     * Validates and stages the nested attributes merge patch: a present key
     * sets the attribute, an explicit null clears it. Every violation is
     * collected; staged assignments are applied only if the whole patch
     * validates.
     */
    private static void stageAttributes(Client client, Security security, JsonElement element,
                    List<ApiException.FieldError> errors, List<AttributeAssignment> assignments)
    {
        if (!element.isJsonObject())
        {
            errors.add(new ApiException.FieldError("attributes", "invalid-type", "attributes must be a JSON object")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return;
        }

        for (var entry : element.getAsJsonObject().entrySet())
        {
            var id = entry.getKey();
            var field = "attributes." + id; //$NON-NLS-1$

            var type = client.getSettings().getAttributeTypes().filter(a -> id.equals(a.getId())).findAny()
                            .orElse(null);
            if (type == null)
            {
                errors.add(new ApiException.FieldError(field, "unknown-attribute", "no such attribute")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }
            if (!type.supports(Security.class))
            {
                errors.add(new ApiException.FieldError(field, "attribute-not-applicable", //$NON-NLS-1$
                                "attribute does not apply to instruments")); //$NON-NLS-1$
                continue;
            }

            var fieldType = AttributeCodec.fieldType(type);
            if (fieldType == null || !AttributeCodec.isWritable(fieldType))
            {
                errors.add(new ApiException.FieldError(field, "unsupported-attribute-type", //$NON-NLS-1$
                                "attribute type is not supported by the API")); //$NON-NLS-1$
                continue;
            }

            var value = entry.getValue();
            if (value.isJsonNull())
            {
                if (security.getAttributes().exists(type))
                    assignments.add(new AttributeAssignment(type, true, null));
                continue;
            }

            try
            {
                assignments.add(new AttributeAssignment(type, false, AttributeCodec.decode(fieldType, value)));
            }
            catch (AttributeCodec.InvalidValueException e)
            {
                errors.add(new ApiException.FieldError(field, "invalid-value", e.getMessage())); //$NON-NLS-1$
            }
        }
    }

    private static ApiException.FieldError requireText(Client client, Security security, String field,
                    JsonElement value)
    {
        if (value.isJsonNull() || !isString(value) || value.getAsString().isBlank())
            return new ApiException.FieldError(field, "required", field + " must be a non-empty string"); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    private static ApiException.FieldError allowTextOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        if (!value.isJsonNull() && !isString(value))
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string or null"); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    /**
     * Validates an ISIN the way the application's ISIN column does: surrounding
     * whitespace is ignored, and a value that is left must pass
     * {@link Isin#isValid} - twelve characters and a matching check digit, with
     * the exemption for internal country codes that check implements. Blank
     * clears the ISIN like null. Without this the API stored values the user
     * could not have typed.
     */
    private static ApiException.FieldError allowIsinOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        var typeError = allowTextOrNull(client, security, field, value);
        if (typeError != null)
            return typeError;

        var isin = isinOrNull(value);
        if (isin != null && !Isin.isValid(isin))
            return new ApiException.FieldError(field, "invalid-isin", //$NON-NLS-1$
                            isin + " is not a valid ISIN: it needs twelve characters and a matching check digit"); //$NON-NLS-1$

        return null;
    }

    /** trimmed, and null when nothing is left - as the ISIN column stores it */
    private static String isinOrNull(JsonElement value)
    {
        var text = stringOrNull(value);
        if (text == null)
            return null;
        var trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Validates a currency change, mirroring the master-data page's rules.
     * Clearing the currency (null) marks the instrument as an index; it is
     * refused for an exchange rate - which needs a currency - and, like any
     * currency change, while the instrument has transactions.
     */
    private static ApiException.FieldError allowCurrencyOrNull(Client client, Security security, String field,
                    JsonElement value)
    {
        String code;

        if (value.isJsonNull())
        {
            if (security.isExchangeRate())
                return new ApiException.FieldError(field, "exchange-rate-requires-currency", //$NON-NLS-1$
                                "an exchange rate must keep its currency and cannot be cleared"); //$NON-NLS-1$
            code = null;
        }
        else if (isString(value))
        {
            code = value.getAsString();
            if (CurrencyUnit.getInstance(code) == null)
                return new ApiException.FieldError(field, "unknown-currency", code + " is not a known currency"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            return new ApiException.FieldError(field, "invalid-type", field + " must be a string or null"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (!Objects.equals(code, security.getCurrencyCode()) && security.hasTransactions(client))
            return new ApiException.FieldError(field, "locked-by-transactions", //$NON-NLS-1$
                            "currency cannot be changed while the instrument has transactions"); //$NON-NLS-1$

        return null;
    }

    /**
     * What a deletion prompt will be about, refused first if the instrument is
     * referenced: the block is the whole verdict of a delete, so asking about a
     * deletion that cannot happen would only train the user to approve refusals.
     */
    public static WriteApprovals.Facts describeDeletion(Client client, String uuid, String fileLabel)
    {
        var security = find(client, uuid);

        var errors = refusals(client, security);
        if (!errors.isEmpty())
            throw ApiException.conflict("delete-blocked", "Instrument is referenced and cannot be deleted", null, errors); //$NON-NLS-1$ //$NON-NLS-2$

        return new WriteApprovals.Facts(null, fileLabel, "instrument", security.getName(), null, null); //$NON-NLS-1$
    }

    /**
     * Deletes the security only if it is not referenced by transactions or
     * investment plans; Client#removeSecurity would cascade into deleting
     * transaction history the API client may never have seen.
     */
    public static String delete(Client client, String uuid)
    {
        var security = find(client, uuid);

        var errors = refusals(client, security);
        if (!errors.isEmpty())
            throw ApiException.conflict("delete-blocked", "Instrument is referenced and cannot be deleted", null, errors); //$NON-NLS-1$ //$NON-NLS-2$

        client.removeSecurity(security);
        client.markDirty();
        return security.getName();
    }

    /** what stands in the way of removing this instrument, as field violations */
    private static List<ApiException.FieldError> refusals(Client client, Security security)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        if (security.hasTransactions(client))
            errors.add(new ApiException.FieldError("transactions", "referenced", "instrument has transactions")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (client.getPlans().stream().anyMatch(plan -> security.equals(plan.getSecurity())))
            errors.add(new ApiException.FieldError("plans", "referenced", "instrument is used by investment plans")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return errors;
    }

    /* package */ static Security find(Client client, String uuid)
    {
        return Entities.byUuid(client.getSecurities(), Security::getUUID, uuid);
    }

    private static boolean isString(JsonElement value)
    {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static String stringOrNull(JsonElement value)
    {
        return value.isJsonNull() ? null : value.getAsString();
    }

    /**
     * Renders a stored attribute value the way the desktop UI shows it. Guarded
     * against a poisoned stored value (wrong runtime type from a corrupted
     * file) so that logging can never break a write.
     */
    private static String renderAttribute(AttributeType type, Object value)
    {
        try
        {
            return type.getConverter().toString(value);
        }
        catch (RuntimeException e)
        {
            return String.valueOf(value);
        }
    }
}
