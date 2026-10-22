package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The <code>transaction.add</code> operation for single-sided cash-account
 * events: deposits, removals, interest, dividends, fees, taxes and their
 * refunds. The input mirrors the <code>transactions</code> output, so an agent
 * can send back what it read. Every rule reports a field-level failure, and the
 * whole batch stays atomic because nothing is added to the model until the
 * operation is clean.
 */
final class TransactionOperations
{
    private static final String OP = "transaction.add";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** The cash-account types this operation records, in the published order. */
    private static final List<String> TYPE_NAMES = List.of("DEPOSIT", "REMOVAL", "INTEREST", "INTEREST_CHARGE",
                    "DIVIDENDS", "FEES", "FEES_REFUND", "TAXES", "TAX_REFUND");

    private static final Set<Type> TYPES = EnumSet.of(Type.DEPOSIT, Type.REMOVAL, Type.INTEREST, Type.INTEREST_CHARGE,
                    Type.DIVIDENDS, Type.FEES, Type.FEES_REFUND, Type.TAXES, Type.TAX_REFUND);

    /** A security is accepted on these; on the others it is refused. */
    private static final Set<Type> SECURITY_ALLOWED = EnumSet.of(Type.DIVIDENDS, Type.TAXES, Type.TAX_REFUND,
                    Type.FEES, Type.FEES_REFUND);

    /** A security is mandatory on these. */
    private static final Set<Type> SECURITY_REQUIRED = EnumSet.of(Type.DIVIDENDS);

    /** Shares are accepted only on these cash types. */
    private static final Set<Type> SHARES_ALLOWED = EnumSet.of(Type.DIVIDENDS);

    private static final List<String> UNIT_TYPES = List.of("FEE", "TAX", "GROSS_VALUE");

    private static final Set<String> KNOWN = Set.of(ApplyBatch.OPERATION_MEMBER, "type", "cashAccount", "date", "time",
                    "amount", "currencyCode", "instrument", "shares", "units", "exDate", "grossAmount", "note",
                    "source");

    /** Core's non-mutating import checks, run on the built transaction as a safety net. */
    private static final List<ImportAction> CORE_CHECKS = List.of(new CheckTransactionDateAction(),
                    new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction(),
                    new CheckForexGrossValueAction(), new CheckValidTypesAction());

    static final OperationSpec ADD = new OperationSpec(OP,
                    "Adds a single-sided cash-account transaction (deposit, removal, interest, dividend, fee, tax or a refund). The input matches the transactions output; the amount is in the cash account's currency.",
                    schema(), TransactionOperations::add);

    static final List<OperationSpec> ALL = List.of(ADD);

    private TransactionOperations()
    {
    }

    private static Schema schema()
    {
        var unit = Schema.object("A fee, tax or forex gross value, in the shape the transactions output uses.")
                        .property("type", Schema.enumOf("Kind of unit.", UNIT_TYPES))
                        .property("amount", Schema.number("Positive amount, in the transaction's currency."))
                        .property("currencyCode", Schema.string("ISO currency code of amount; the transaction's currency."))
                        .optionalProperty("forexAmount", Schema.number("Original amount in the instrument's currency."))
                        .optionalProperty("forexCurrencyCode", Schema.string("ISO currency code of forexAmount."))
                        .optionalProperty("exchangeRate", Schema.number(
                                        "Positive, unrounded: forexAmount times exchangeRate is approximately amount."));

        return Schema.object("A single-sided cash-account event, in the shape the transactions output uses.")
                        .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(OP)))
                        .property("type", Schema.enumOf("Cash-account transaction type.", TYPE_NAMES))
                        .property("cashAccount", Schema.string("UUID of the cash account; it must already be in the file."))
                        .property("date", Schema.string("ISO date of the event."))
                        .optionalProperty("time", Schema.string("HH:mm; omit for midnight."))
                        .property("amount", Schema.number("Positive net amount, in the cash account's currency."))
                        .property("currencyCode", Schema.string("ISO currency code; must be the cash account's currency."))
                        .optionalProperty("instrument", Schema.string(
                                        "UUID of the instrument. Required for DIVIDENDS, refused for deposits, removals and interest, optional for fees and taxes."))
                        .optionalProperty("shares", Schema.number("Positive number of shares; accepted only on a dividend."))
                        .optionalProperty("units", Schema.array(
                                        "Fees, taxes and the forex gross value an instrument in another currency needs.",
                                        unit))
                        .optionalProperty("exDate", Schema.string(
                                        "ISO ex-date; accepted only on a dividend with an instrument and never after the date."))
                        .optionalProperty("grossAmount", Schema.number(
                                        "Optional cross-check: amount plus or minus fees and taxes. Refused if it does not reconcile; amount stays authoritative."))
                        .optionalProperty("note", Schema.string("Note."))
                        .optionalProperty("source", Schema.string("Where the record came from."));
    }

    private static void add(OperationContext context)
    {
        context.rejectUnknownFields(KNOWN);
        var input = context.input();

        var type = parseType(context, input);
        var account = parseCashAccount(context, input);
        var dateTime = parseDateTime(context, input);
        var currencyCode = parseCurrency(context, input, account);
        var amount = parseAmount(context, input, "amount", Values.Amount, true);
        var security = parseInstrument(context, input, type);
        var shares = parseShares(context, input, type);
        var units = parseUnits(context, input, currencyCode, security, type);
        var exDate = exDate(context, input, type, security, dateTime);

        parseGrossAmount(context, input, type, amount, units);

        var note = parseText(context, input, "note");
        var source = parseText(context, input, "source");

        if (context.failed() || type == null || account == null || dateTime == null || currencyCode == null
                        || amount == null)
            return;

        var transaction = new AccountTransaction(dateTime, currencyCode, amount, security, type);
        if (shares != null)
            transaction.setShares(shares);
        if (exDate != null)
            transaction.setExDate(exDate.atStartOfDay());
        if (note != null)
            transaction.setNote(note);
        if (source != null)
            transaction.setSource(source);
        units.forEach(transaction::addUnit);

        for (var check : CORE_CHECKS)
        {
            var status = check.process(transaction, account);
            if (status.getCode() == ImportAction.Status.Code.ERROR)
                context.fail("", "import-check-failed", status.getMessage(), check.getClass().getSimpleName());
        }

        if (context.failed())
            return;

        account.addTransaction(transaction);

        var record = TransactionRecord.allOf(context.client()).stream().filter(r -> r.primary() == transaction)
                        .findFirst().orElseThrow();
        context.created(new OperationContext.Change("transaction", record.id(), null,
                        TransactionsCommand.render(record)));
    }

    private static Type parseType(OperationContext context, JsonObject input)
    {
        var member = input.get("type");
        if (member == null || member.isJsonNull())
        {
            context.fail("type", "required", "A transaction needs a type.");
            return null;
        }
        if (!OperationContext.isText(member))
        {
            context.fail("type", "invalid-type", "The type must be a string.");
            return null;
        }

        var name = member.getAsString();
        if (!TYPE_NAMES.contains(name))
        {
            context.fail("type", "unsupported-type", "'" + name
                            + "' is not a cash-account transaction type. Supported: " + String.join(", ", TYPE_NAMES)
                            + ".");
            return null;
        }
        return Type.valueOf(name);
    }

    private static Account parseCashAccount(OperationContext context, JsonObject input)
    {
        var uuid = parseText(context, input, "cashAccount");
        if (uuid == null)
        {
            requireText(context, input, "cashAccount", "A transaction needs a cash account.");
            return null;
        }

        var found = context.client().getAccounts().stream()
                        .filter(a -> uuid.equals(a.getUUID()) && context.existedBeforeBatch(a)).findFirst();
        if (found.isEmpty())
        {
            context.fail("cashAccount", "reference-not-found", "No cash account with UUID " + uuid + " in the file.");
            return null;
        }
        return found.get();
    }

    private static LocalDateTime parseDateTime(OperationContext context, JsonObject input)
    {
        var date = parseText(context, input, "date");
        if (date == null)
        {
            requireText(context, input, "date", "A transaction needs a date.");
            return null;
        }

        LocalDate day;
        try
        {
            day = LocalDate.parse(date);
        }
        catch (DateTimeParseException e)
        {
            context.fail("date", "invalid-date", "'" + date + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }

        var timeText = parseText(context, input, "time");
        if (timeText == null)
            return day.atStartOfDay();

        try
        {
            return LocalDateTime.of(day, LocalTime.parse(timeText, TIME));
        }
        catch (DateTimeParseException e)
        {
            context.fail("time", "invalid-time", "'" + timeText + "' is not a time of day (HH:mm).");
            return null;
        }
    }

    private static String parseCurrency(OperationContext context, JsonObject input, Account account)
    {
        var code = parseText(context, input, "currencyCode");
        if (code == null)
        {
            requireText(context, input, "currencyCode", "A transaction needs a currency.");
            return null;
        }

        if (!CurrencyUnit.containsCurrencyCode(code))
        {
            context.fail("currencyCode", "invalid-currency", "'" + code + "' is not a known ISO currency code.");
            return null;
        }

        if (account != null && !code.equals(account.getCurrencyCode()))
        {
            context.fail("currencyCode", "currency-mismatch", "The amount must be in the cash account's currency "
                            + account.getCurrencyCode() + ", not " + code + ".");
            return null;
        }
        return code;
    }

    private static Security parseInstrument(OperationContext context, JsonObject input, Type type)
    {
        var uuid = parseText(context, input, "instrument");
        var present = input.has("instrument") && !input.get("instrument").isJsonNull()
                        && OperationContext.isText(input.get("instrument")) && uuid != null;

        if (!present)
        {
            if (type != null && SECURITY_REQUIRED.contains(type))
                context.fail("instrument", "required", type + " needs an instrument.");
            return null;
        }

        if (type != null && !SECURITY_ALLOWED.contains(type))
        {
            context.fail("instrument", "instrument-not-allowed", type + " must not have an instrument.");
            return null;
        }

        var found = context.client().getSecurities().stream()
                        .filter(s -> uuid.equals(s.getUUID()) && context.existedBeforeBatch(s)).findFirst();
        if (found.isEmpty())
        {
            context.fail("instrument", "reference-not-found", "No instrument with UUID " + uuid + " in the file.");
            return null;
        }
        return found.get();
    }

    private static Long parseShares(OperationContext context, JsonObject input, Type type)
    {
        if (!input.has("shares") || input.get("shares").isJsonNull())
            return null;

        if (type != null && !SHARES_ALLOWED.contains(type))
        {
            context.fail("shares", "shares-not-allowed", type + " must not have shares.");
            return null;
        }
        return parseAmount(context, input, "shares", Values.Share, true);
    }

    /** The valid ex-date to store, or null if it is absent or refused. */
    private static LocalDate exDate(OperationContext context, JsonObject input, Type type, Security security,
                    LocalDateTime dateTime)
    {
        if (!input.has("exDate") || input.get("exDate").isJsonNull())
            return null;

        if (!OperationContext.isText(input.get("exDate")))
        {
            context.fail("exDate", "invalid-type", "The ex-date must be a string.");
            return null;
        }

        if (type != Type.DIVIDENDS || security == null)
        {
            context.fail("exDate", "exdate-not-allowed", "An ex-date is allowed only on a dividend with an instrument.");
            return null;
        }

        LocalDate ex;
        try
        {
            ex = LocalDate.parse(input.get("exDate").getAsString());
        }
        catch (DateTimeParseException e)
        {
            context.fail("exDate", "invalid-date",
                            "'" + input.get("exDate").getAsString() + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }

        if (dateTime != null && ex.isAfter(dateTime.toLocalDate()))
        {
            context.fail("exDate", "exdate-after-payment", "The ex-date must not be after the payment date.");
            return null;
        }
        return ex;
    }

    private static void parseGrossAmount(OperationContext context, JsonObject input, Type type, Long amount,
                    List<Transaction.Unit> units)
    {
        if (!input.has("grossAmount") || input.get("grossAmount").isJsonNull())
            return;

        var gross = parseAmount(context, input, "grossAmount", Values.Amount, false);
        if (gross == null || amount == null || type == null)
            return;

        var taxAndFees = units.stream()
                        .filter(u -> u.getType() == Transaction.Unit.Type.FEE || u.getType() == Transaction.Unit.Type.TAX)
                        .mapToLong(u -> u.getAmount().getAmount()).sum();
        var expected = amount + (type.isCredit() ? taxAndFees : -taxAndFees);

        if (gross != expected)
            context.fail("grossAmount", "gross-amount-mismatch",
                            "The gross amount does not reconcile with the amount, fees and taxes.");
    }

    private static List<Transaction.Unit> parseUnits(OperationContext context, JsonObject input, String currencyCode,
                    Security security, Type type)
    {
        var units = new ArrayList<Transaction.Unit>();

        if (input.has("units") && !input.get("units").isJsonNull())
        {
            if (!input.get("units").isJsonArray())
            {
                context.fail("units", "invalid-type", "The units must be an array.");
            }
            else
            {
                var array = input.get("units").getAsJsonArray();
                for (var i = 0; i < array.size(); i++)
                {
                    var unit = parseUnit(context, array.get(i), i, currencyCode);
                    if (unit != null)
                        units.add(unit);
                }
            }
        }

        requireForexGrossValue(context, currencyCode, security, type, units);
        return units;
    }

    private static Transaction.Unit parseUnit(OperationContext context, JsonElement element, int index,
                    String currencyCode)
    {
        var field = "units[" + index + "]";
        if (!element.isJsonObject())
        {
            context.fail(field, "invalid-type", "A unit must be a JSON object.");
            return null;
        }

        var unit = element.getAsJsonObject();
        context.rejectUnknownFields(unit, field,
                        Set.of("type", "amount", "currencyCode", "forexAmount", "forexCurrencyCode", "exchangeRate"));

        var typeName = text(unit, "type");
        if (typeName == null || !UNIT_TYPES.contains(typeName))
        {
            context.fail(field + ".type", "unsupported-unit-type",
                            "A unit type must be one of " + String.join(", ", UNIT_TYPES) + ".");
            return null;
        }
        var unitType = Transaction.Unit.Type.valueOf(typeName);

        var amount = number(context, unit, field + ".amount", "amount", Values.Amount, true);
        var unitCurrency = text(unit, "currencyCode");
        if (unitCurrency == null)
            context.fail(field + ".currencyCode", "required", "A unit needs a currency.");
        else if (currencyCode != null && !unitCurrency.equals(currencyCode))
            context.fail(field + ".currencyCode", "currency-mismatch",
                            "A unit must be in the transaction's currency " + currencyCode + ".");

        var hasForex = unit.has("forexAmount") || unit.has("forexCurrencyCode") || unit.has("exchangeRate");
        var forexAmount = number(context, unit, field + ".forexAmount", "forexAmount", Values.Amount, true);
        var forexCurrency = text(unit, "forexCurrencyCode");
        var rate = rawNumber(unit, "exchangeRate");

        if (unitType == Transaction.Unit.Type.GROSS_VALUE && !hasForex)
        {
            context.fail(field, "forex-required", "A GROSS_VALUE unit needs forexAmount, forexCurrencyCode and exchangeRate.");
            return null;
        }

        if (hasForex)
        {
            if (forexAmount == null || forexCurrency == null || rate == null)
            {
                context.fail(field, "forex-incomplete",
                                "forexAmount, forexCurrencyCode and exchangeRate are given together or not at all.");
                return null;
            }
            if (rate.signum() <= 0)
            {
                context.fail(field + ".exchangeRate", "not-positive", "The exchange rate must be positive.");
                return null;
            }
            if (!forexCurrency.equals(currencyCode) && !CurrencyUnit.containsCurrencyCode(forexCurrency))
            {
                context.fail(field + ".forexCurrencyCode", "invalid-currency",
                                "'" + forexCurrency + "' is not a known ISO currency code.");
                return null;
            }
        }

        if (amount == null || unitCurrency == null || currencyCode == null || !unitCurrency.equals(currencyCode))
            return null;

        if (!hasForex)
        {
            if (unitType == Transaction.Unit.Type.GROSS_VALUE)
                return null;
            return new Transaction.Unit(unitType, Money.of(currencyCode, amount));
        }

        var amountMoney = Money.of(currencyCode, amount);
        var forexMoney = Money.of(forexCurrency, forexAmount);
        if (!Transaction.Unit.isWithinRoundingTolerance(amountMoney, forexMoney, rate))
        {
            context.fail(field, "forex-mismatch",
                            "forexAmount times exchangeRate is not amount within core's rounding tolerance.");
            return null;
        }
        return new Transaction.Unit(unitType, amountMoney, forexMoney, rate);
    }

    /** A dividend on an instrument in another currency needs a forex gross-value unit in that currency. */
    private static void requireForexGrossValue(OperationContext context, String currencyCode, Security security,
                    Type type, List<Transaction.Unit> units)
    {
        if (security == null || currencyCode == null || security.getCurrencyCode() == null
                        || security.getCurrencyCode().equals(currencyCode))
            return;

        var hasMatch = units.stream().filter(u -> u.getType() == Transaction.Unit.Type.GROSS_VALUE)
                        .anyMatch(u -> u.getForex() != null
                                        && security.getCurrencyCode().equals(u.getForex().getCurrencyCode()));
        if (!hasMatch)
            context.fail("units", "forex-gross-value-required",
                            "The instrument is in " + security.getCurrencyCode() + " but the transaction is in "
                                            + currencyCode
                                            + "; add a GROSS_VALUE unit with its forex amount and exchange rate.");
    }

    /** Reads a positive stored value at the given precision, reporting any problem on {@code field}. */
    private static Long parseAmount(OperationContext context, JsonObject input, String field, Values<Long> values,
                    boolean positive)
    {
        return number(context, input, field, field, values, positive);
    }

    private static Long number(OperationContext context, JsonObject input, String field, String member,
                    Values<Long> values, boolean positive)
    {
        var value = rawNumber(input, member);
        if (value == null)
        {
            if (input.has(member) && !input.get(member).isJsonNull())
                context.fail(field, "invalid-type", "The " + member + " must be a number.");
            else if (field.equals(member))
                context.fail(field, "required", "A transaction needs " + member + ".");
            return null;
        }

        if (positive && value.signum() <= 0)
        {
            context.fail(field, "not-positive", "The " + member + " must be positive.");
            return null;
        }

        var stored = Decimals.toStored(value, values);
        if (stored.isEmpty())
        {
            context.fail(field, "invalid-amount", "The " + member + " has more decimal places than are stored.");
            return null;
        }
        return stored.get();
    }

    /** The member as a number, or null if it is absent or not a number; reports nothing. */
    private static BigDecimal rawNumber(JsonObject input, String member)
    {
        var element = input.get(member);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                        ? element.getAsBigDecimal()
                        : null;
    }

    /** The string value of the member, or null if it is absent, null, blank or not a string. */
    private static String text(JsonObject input, String member)
    {
        var element = input.get(member);
        if (element == null || !OperationContext.isText(element))
            return null;
        var text = element.getAsString();
        return text.isBlank() ? null : text;
    }

    private static String parseText(OperationContext context, JsonObject input, String member)
    {
        var element = input.get(member);
        if (element != null && !element.isJsonNull() && !OperationContext.isText(element))
        {
            context.fail(member, "invalid-type", "The " + member + " must be a string.");
            return null;
        }
        return text(input, member);
    }

    /** Reports {@code required} for a text member that is absent or blank; a wrong type is already reported. */
    private static void requireText(OperationContext context, JsonObject input, String member, String message)
    {
        var element = input.get(member);
        if (element == null || element.isJsonNull() || OperationContext.isText(element))
            context.fail(member, "required", message);
    }
}
