package name.abuchen.portfolio.cli;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * The write operations on cash accounts and investment accounts. Fields are
 * named as in the <code>cash-accounts</code> and
 * <code>investment-accounts</code> outputs.
 */
final class AccountOperations
{
    private static final String CASH_ENTITY = "cash-account";
    private static final String INVESTMENT_ENTITY = "investment-account";
    private static final String RETIRED = "retired";

    private static final List<String> INVESTMENT_TEXT_FIELDS = List.of("name", "referenceCashAccount", "note");
    private static final Set<String> INVESTMENT_ADD_MEMBERS = Set.of(ApplyBatch.OPERATION_MEMBER, "name",
                    "referenceCashAccount", "note", RETIRED);
    private static final Set<String> INVESTMENT_UPDATE_MEMBERS = Stream
                    .concat(INVESTMENT_ADD_MEMBERS.stream(), Stream.of("uuid"))
                    .collect(Collectors.toUnmodifiableSet());

    private static final List<String> CASH_TEXT_FIELDS = List.of("name", "currencyCode", "note");
    private static final Set<String> CASH_ADD_MEMBERS = Set.of(ApplyBatch.OPERATION_MEMBER, "name", "currencyCode",
                    "note", RETIRED);
    private static final Set<String> CASH_UPDATE_MEMBERS = Stream.concat(CASH_ADD_MEMBERS.stream(), Stream.of("uuid"))
                    .collect(Collectors.toUnmodifiableSet());

    static final OperationSpec CASH_ADD = new OperationSpec("cash-account.add",
                    "Adds a cash account. The name and the currency are required.",
                    cashFields("cash-account.add", false), AccountOperations::addCashAccount);

    static final OperationSpec CASH_UPDATE = new OperationSpec("cash-account.update",
                    "Updates a cash account by UUID. Only the fields present change; null or an empty string clears the note. The currency can't change once the account has transactions.",
                    cashFields("cash-account.update", true).property("uuid",
                                    Schema.string("UUID of the cash account.")),
                    AccountOperations::updateCashAccount);

    static final OperationSpec INVESTMENT_ADD = new OperationSpec("investment-account.add",
                    "Adds an investment account. The name and a reference cash account that is already in the file are required.",
                    investmentFields("investment-account.add", false), AccountOperations::addInvestmentAccount);

    static final OperationSpec INVESTMENT_UPDATE = new OperationSpec("investment-account.update",
                    "Updates an investment account by UUID. Only the fields present change; null or an empty string clears the note. A new reference cash account must already be in the file.",
                    investmentFields("investment-account.update", true).property("uuid",
                                    Schema.string("UUID of the investment account.")),
                    AccountOperations::updateInvestmentAccount);

    static final List<OperationSpec> ALL = List.of(CASH_ADD, CASH_UPDATE, INVESTMENT_ADD, INVESTMENT_UPDATE);

    private AccountOperations()
    {
    }

    private static Schema cashFields(String operation, boolean update)
    {
        var name = Schema.string("Account name; never empty.");
        var currency = Schema.string("ISO currency code of the account.");
        var schema = Schema.object("Fields of the cash account; they match the cash-accounts output.")
                        .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(operation)));

        if (update)
            schema.optionalProperty("name", name).optionalProperty("currencyCode", currency);
        else
            schema.property("name", name).property("currencyCode", currency);

        return schema.optionalProperty("note", Schema.string("Note.").nullable()).optionalProperty(RETIRED,
                        Schema.bool(update ? "Whether the account is retired."
                                        : "Whether the account is retired; defaults to false."));
    }

    private static void addCashAccount(OperationContext context)
    {
        context.rejectUnknownFields(CASH_ADD_MEMBERS);
        var values = read(context, CASH_TEXT_FIELDS);

        if (values.get("name") == null)
            context.fail("name", "required", "A cash account needs a name.");
        if (values.get("currencyCode") == null)
            context.fail("currencyCode", "required", "A cash account needs a currency.");
        validateCurrency(context, values);

        if (context.failed())
            return;

        var account = new Account();
        values.forEach((field, value) -> set(account, field, value));
        context.client().addAccount(account);

        var after = new LinkedHashMap<String, Object>();
        for (var field : CASH_TEXT_FIELDS)
        {
            var value = current(account, field);
            if (value != null)
                after.put(field, value);
        }
        after.put(RETIRED, account.isRetired());

        context.created(new OperationContext.Change(CASH_ENTITY, account.getUUID(), null, after));
    }

    private static void updateCashAccount(OperationContext context)
    {
        context.rejectUnknownFields(CASH_UPDATE_MEMBERS);

        var account = find(context, "cash account", context.client().getAccounts(), Account::getUUID);
        var values = read(context, CASH_TEXT_FIELDS);

        if (values.containsKey("name") && values.get("name") == null)
            context.fail("name", "required", "A cash account needs a name.");
        if (values.containsKey("currencyCode") && values.get("currencyCode") == null)
            context.fail("currencyCode", "required", "A cash account needs a currency.");
        validateCurrency(context, values);

        if (account != null && values.containsKey("currencyCode")
                        && !Objects.equals(account.getCurrencyCode(), values.get("currencyCode"))
                        && !account.getTransactions().isEmpty())
            context.fail("currencyCode", "currency-immutable",
                            "The currency can't change because the account has transactions.");

        if (account == null || context.failed())
            return;

        var before = new LinkedHashMap<String, Object>();
        var after = new LinkedHashMap<String, Object>();
        values.forEach((field, value) -> {
            var old = current(account, field);
            if (!Objects.equals(old, value))
            {
                before.put(field, old);
                after.put(field, value);
            }
        });

        after.forEach((field, value) -> set(account, field, value));

        if (!after.isEmpty())
            context.updated(new OperationContext.Change(CASH_ENTITY, account.getUUID(), before, after));
    }

    private static Schema investmentFields(String operation, boolean update)
    {
        var name = Schema.string("Account name; never empty.");
        var reference = Schema.string(
                        "UUID of the cash account that settles this account; it must already be in the file.");
        var schema = Schema.object("Fields of the investment account; they match the investment-accounts output.")
                        .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(operation)));

        if (update)
            schema.optionalProperty("name", name).optionalProperty("referenceCashAccount", reference);
        else
            schema.property("name", name).property("referenceCashAccount", reference);

        return schema.optionalProperty("note", Schema.string("Note.").nullable()).optionalProperty(RETIRED,
                        Schema.bool(update ? "Whether the account is retired."
                                        : "Whether the account is retired; defaults to false."));
    }

    private static void addInvestmentAccount(OperationContext context)
    {
        context.rejectUnknownFields(INVESTMENT_ADD_MEMBERS);
        var values = read(context, INVESTMENT_TEXT_FIELDS);

        if (values.get("name") == null)
            context.fail("name", "required", "An investment account needs a name.");
        if (values.get("referenceCashAccount") == null)
            context.fail("referenceCashAccount", "required", "An investment account needs a reference cash account.");
        var reference = findReference(context, values);

        if (context.failed())
            return;

        var portfolio = new Portfolio();
        values.forEach((field, value) -> set(portfolio, field, value, reference));
        context.client().addPortfolio(portfolio);

        var after = new LinkedHashMap<String, Object>();
        for (var field : INVESTMENT_TEXT_FIELDS)
        {
            var value = current(portfolio, field);
            if (value != null)
                after.put(field, value);
        }
        after.put(RETIRED, portfolio.isRetired());

        context.created(new OperationContext.Change(INVESTMENT_ENTITY, portfolio.getUUID(), null, after));
    }

    private static void updateInvestmentAccount(OperationContext context)
    {
        context.rejectUnknownFields(INVESTMENT_UPDATE_MEMBERS);

        var portfolio = find(context, "investment account", context.client().getPortfolios(), Portfolio::getUUID);
        var values = read(context, INVESTMENT_TEXT_FIELDS);

        if (values.containsKey("name") && values.get("name") == null)
            context.fail("name", "required", "An investment account needs a name.");
        if (values.containsKey("referenceCashAccount") && values.get("referenceCashAccount") == null)
            context.fail("referenceCashAccount", "required", "An investment account needs a reference cash account.");
        var reference = findReference(context, values);

        if (portfolio == null || context.failed())
            return;

        var before = new LinkedHashMap<String, Object>();
        var after = new LinkedHashMap<String, Object>();
        values.forEach((field, value) -> {
            var old = current(portfolio, field);
            if (!Objects.equals(old, value))
            {
                before.put(field, old);
                after.put(field, value);
            }
        });

        after.forEach((field, value) -> set(portfolio, field, value, reference));

        if (!after.isEmpty())
            context.updated(new OperationContext.Change(INVESTMENT_ENTITY, portfolio.getUUID(), before, after));
    }

    /**
     * The cash account named by <code>referenceCashAccount</code>, if the
     * input has one. It must have been in the file before the batch.
     */
    private static Account findReference(OperationContext context, Map<String, Object> values)
    {
        if (!(values.get("referenceCashAccount") instanceof String uuid))
            return null;

        var found = context.client().getAccounts().stream()
                        .filter(a -> uuid.equals(a.getUUID()) && context.existedBeforeBatch(a)).findFirst();
        if (found.isEmpty())
        {
            context.fail("referenceCashAccount", "reference-not-found",
                            "No cash account with UUID " + uuid + " in the file.");
            return null;
        }

        return found.get();
    }

    private static void validateCurrency(OperationContext context, Map<String, Object> values)
    {
        if (values.get("currencyCode") instanceof String code && !CurrencyUnit.containsCurrencyCode(code))
            context.fail("currencyCode", "invalid-currency", "'" + code + "' is not a known ISO currency code.");
    }

    /**
     * The entity named by <code>uuid</code> that was in the file before the
     * batch, or null after reporting why not.
     */
    private static <T> T find(OperationContext context, String kind, Collection<T> candidates,
                    Function<T, String> uuidOf)
    {
        var member = context.input().get("uuid");
        if (member == null || member.isJsonNull())
        {
            context.fail("uuid", "required", "An update needs the UUID of the " + kind + ".");
            return null;
        }
        if (!OperationContext.isText(member))
        {
            context.fail("uuid", "invalid-type", "The UUID must be a string.");
            return null;
        }

        var uuid = member.getAsString();
        var found = candidates.stream().filter(c -> uuid.equals(uuidOf.apply(c)) && context.existedBeforeBatch(c))
                        .findFirst();
        if (found.isEmpty())
        {
            context.fail("uuid", "reference-not-found", "No " + kind + " with UUID " + uuid + " in the file.");
            return null;
        }

        return context.claim("uuid", uuid) ? found.get() : null;
    }

    /**
     * The fields that are present in the input. A null or blank string counts
     * as unset and is read as null.
     */
    private static Map<String, Object> read(OperationContext context, List<String> textFields)
    {
        var values = new LinkedHashMap<String, Object>();
        var input = context.input();

        for (var field : textFields)
        {
            var member = input.get(field);
            if (member == null)
                continue;

            if (member.isJsonNull())
                values.put(field, null);
            else if (OperationContext.isText(member))
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

    /** The field as the read output shows it: blank counts as unset. */
    private static Object current(Portfolio portfolio, String field)
    {
        var value = switch (field)
        {
            case "name" -> portfolio.getName();
            case "referenceCashAccount" -> portfolio.getReferenceAccount() != null
                            ? portfolio.getReferenceAccount().getUUID()
                            : null;
            case "note" -> portfolio.getNote();
            case RETIRED -> portfolio.isRetired();
            default -> throw new IllegalArgumentException(field);
        };

        return value instanceof String text && text.isBlank() ? null : value;
    }

    private static void set(Portfolio portfolio, String field, Object value, Account reference)
    {
        switch (field)
        {
            case "name" -> portfolio.setName((String) value);
            case "referenceCashAccount" -> portfolio.setReferenceAccount(reference);
            case "note" -> portfolio.setNote((String) value);
            case RETIRED -> portfolio.setRetired((Boolean) value);
            default -> throw new IllegalArgumentException(field);
        }
    }

    /** The field as the read output shows it: blank counts as unset. */
    private static Object current(Account account, String field)
    {
        var value = switch (field)
        {
            case "name" -> account.getName();
            case "currencyCode" -> account.getCurrencyCode();
            case "note" -> account.getNote();
            case RETIRED -> account.isRetired();
            default -> throw new IllegalArgumentException(field);
        };

        return value instanceof String text && text.isBlank() ? null : value;
    }

    private static void set(Account account, String field, Object value)
    {
        switch (field)
        {
            case "name" -> account.setName((String) value);
            case "currencyCode" -> account.setCurrencyCode((String) value);
            case "note" -> account.setNote((String) value);
            case RETIRED -> account.setRetired((Boolean) value);
            default -> throw new IllegalArgumentException(field);
        }
    }
}
