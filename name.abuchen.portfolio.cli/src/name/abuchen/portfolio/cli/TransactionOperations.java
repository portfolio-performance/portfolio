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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The <code>transaction.add</code> operation. This class takes the cash-account
 * events (deposits, removals, interest, dividends, fees, taxes and their
 * refunds), dispatches the other types to {@link TradeOperations} and
 * {@link TransferOperations}, and holds the parsing that they share. The input
 * mirrors the <code>transactions</code> output, so an agent can send back what
 * it read. Every rule reports a field-level failure, and the whole batch stays
 * atomic because nothing is added to the model until the operation is clean.
 */
final class TransactionOperations
{
    private static final String OP = "transaction.add";
    private static final String UPDATE_OP = "transaction.update";

    private static final String UPDATE_DESCRIPTION = "The fields of a transaction to change, in the shape the transactions output uses. Only the fields present change; null clears an optional field.";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** The transaction types this operation records, in the published order. */
    private static final List<String> TYPE_NAMES = List.of("DEPOSIT", "REMOVAL", "INTEREST", "INTEREST_CHARGE",
                    "DIVIDENDS", "FEES", "FEES_REFUND", "TAXES", "TAX_REFUND", "BUY", "SELL", "DELIVERY_INBOUND",
                    "DELIVERY_OUTBOUND", TransactionRecord.TRANSFER);

    /** The types that are recorded in an investment account, as opposed to a cash account. */
    private static final Set<String> PORTFOLIO_TYPES = Set.of("BUY", "SELL", "DELIVERY_INBOUND", "DELIVERY_OUTBOUND");

    /** A security is accepted on these; on the others it is refused. */
    private static final Set<Type> SECURITY_ALLOWED = EnumSet.of(Type.DIVIDENDS, Type.TAXES, Type.TAX_REFUND,
                    Type.FEES, Type.FEES_REFUND);

    /** A security is mandatory on these. */
    private static final Set<Type> SECURITY_REQUIRED = EnumSet.of(Type.DIVIDENDS);

    /** Shares are accepted only on these cash types. */
    private static final Set<Type> SHARES_ALLOWED = EnumSet.of(Type.DIVIDENDS);

    private static final List<String> UNIT_TYPES = List.of("FEE", "TAX", "GROSS_VALUE");

    private static final Set<String> KNOWN = Set.of(ApplyBatch.OPERATION_MEMBER, "type", "cashAccount",
                    "investmentAccount", "date", "time", "amount", "currencyCode", "instrument", "shares", "units",
                    "exDate", "grossAmount", "note", "source", "kind", "from", "to", "targetAmount", "exchangeRate");

    /** The failure code for a member that the kind of transaction doesn't take. */
    private static final Map<String, String> NOT_ALLOWED = Map.ofEntries(
                    Map.entry("cashAccount", "cash-account-not-allowed"),
                    Map.entry("investmentAccount", "investment-account-not-allowed"),
                    Map.entry("instrument", "instrument-not-allowed"), Map.entry("shares", "shares-not-allowed"),
                    Map.entry("units", "units-not-allowed"), Map.entry("exDate", "exdate-not-allowed"),
                    Map.entry("grossAmount", "gross-amount-not-allowed"), Map.entry("kind", "kind-not-allowed"),
                    Map.entry("from", "from-not-allowed"), Map.entry("to", "to-not-allowed"),
                    Map.entry("targetAmount", "target-amount-not-allowed"),
                    Map.entry("exchangeRate", "exchange-rate-not-allowed"));

    /** Core's non-mutating import checks, run on the built transaction as a safety net. */
    private static final List<ImportAction> CORE_CHECKS = List.of(new CheckTransactionDateAction(),
                    new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction(),
                    new CheckForexGrossValueAction(), new CheckValidTypesAction());

    static final OperationSpec ADD = new OperationSpec(OP,
                    "Adds a transaction in the shape the transactions output uses. Cash-account events (deposit, removal, interest, dividend, fee, tax, refund) are one-sided. BUY and SELL create an investment-account side and a cash-account side together; TRANSFER creates both sides of a cash or security transfer; DELIVERY_INBOUND and DELIVERY_OUTBOUND move shares without a cash side. The response is one created change whose uuid is the record id. A write that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    schema(false), TransactionOperations::add);

    static final OperationSpec UPDATE = new OperationSpec(UPDATE_OP,
                    "Changes a whole transaction record, found by the UUID of either of its sides. Only the fields present change; null clears an optional field. The patch is overlaid on the current record and then validated exactly like an add, so a partial edit can't bypass validation and nothing is recomputed: fees, taxes and forex units that no longer fit a changed amount or shares are refused with a field-level error. The type, and the kind of a transfer, can't change; delete the record and add a new one. Both sides stay in step and keep their UUIDs. The response is one updated change, with before and after for the changed fields, whose uuid is the record id. A change that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    schema(true), TransactionUpdates::update);

    static final List<OperationSpec> ALL = List.of(ADD, UPDATE, TransactionUpdates.DELETE);

    private TransactionOperations()
    {
    }

    private static Schema schema(boolean update)
    {
        var unit = Schema.object("A fee, tax or forex gross value, in the shape the transactions output uses.")
                        .property("type", Schema.enumOf("Kind of unit.", UNIT_TYPES))
                        .property("amount", Schema.number("Positive amount, in the transaction's currency."))
                        .property("currencyCode", Schema.string("ISO currency code of amount; the transaction's currency."))
                        .optionalProperty("forexAmount", Schema.number("Original amount in the instrument's currency."))
                        .optionalProperty("forexCurrencyCode", Schema.string("ISO currency code of forexAmount."))
                        .optionalProperty("exchangeRate", Schema.number(
                                        "Positive, unrounded: forexAmount times exchangeRate is approximately amount."));

        var schema = Schema.object(update ? UPDATE_DESCRIPTION : "A transaction, in the shape the transactions output uses.")
                        .property(ApplyBatch.OPERATION_MEMBER,
                                        Schema.enumOf("Operation name.", List.of(update ? UPDATE_OP : OP)));
        if (update)
            schema.property(TransactionUpdates.UUID_MEMBER,
                            Schema.string("UUID of either side of the record to change."));

        new Members(schema, update)
                        .required("type", Schema.enumOf(
                                        "Transaction type. TRANSFER needs a kind, from and to; BUY and SELL need an investmentAccount and a cashAccount; deliveries need an investmentAccount; the others need a cashAccount.",
                                        TYPE_NAMES))
                        .optional("kind", Schema.enumOf("Of a TRANSFER only: cash or security.",
                                        TransactionRecord.TransferKind.ids()))
                        .optional("cashAccount", Schema.string(
                                        "UUID of the cash account; it must already be in the file. Required for cash-account events, BUY and SELL; refused for deliveries and transfers."))
                        .optional("investmentAccount", Schema.string(
                                        "UUID of the investment account; it must already be in the file. Required for BUY, SELL and deliveries; refused for cash-account events and transfers."))
                        .optional("from", Schema.string(
                                        "Of a TRANSFER only: UUID of the account it leaves, a cash account for a cash transfer and an investment account for a security transfer. Must differ from to."))
                        .optional("to", Schema.string(
                                        "Of a TRANSFER only: UUID of the account it enters, of the same kind as from."))
                        .required("date", Schema.string("ISO date of the event."))
                        .optional("time", Schema.string("HH:mm; omit for midnight."))
                        .required("amount", Schema.number(
                                        "Positive net amount. Of a BUY or SELL in the cash account's currency, of a cash transfer in the currency of from, of a security transfer in the instrument's currency."))
                        .required("currencyCode", Schema.string(
                                        "ISO currency code of amount: the cash account's, or for a transfer the currency of from (cash) or of the instrument (security). A delivery may use any currency; if it is not the instrument's, a GROSS_VALUE unit is required."))
                        .optional("targetAmount", Schema.number(
                                        "Of a cash transfer between accounts of different currencies: positive, in the currency of to. Refused when the currencies are the same."))
                        .optional("exchangeRate", Schema.number(
                                        "Of a cross-currency cash transfer: optional, only checked against core's rounding tolerance. The stored rate is always amount divided by targetAmount and is returned in the record."))
                        .optional("instrument", Schema.string(
                                        "UUID of the instrument. Required for DIVIDENDS, BUY, SELL, deliveries and security transfers; optional for fees and taxes; refused for deposits, removals, interest and cash transfers."))
                        .optional("shares", Schema.number(
                                        "Positive number of shares. Required for BUY, SELL, deliveries and security transfers; accepted on a dividend; refused otherwise."))
                        .optional("units", Schema.array(
                                        "Fees, taxes and the forex gross value an instrument in another currency needs. Refused on transfers.",
                                        unit))
                        .optional("exDate", Schema.string(
                                        "ISO ex-date; accepted only on a dividend with an instrument and never after the date."))
                        .optional("grossAmount", Schema.number(
                                        "Optional cross-check: amount plus or minus fees and taxes (for a BUY or inbound delivery amount minus them). Refused if it does not reconcile and on transfers; amount stays authoritative."))
                        .optional("note", Schema.string("Note."))
                        .optional("source", Schema.string("Where the record came from."));

        return schema;
    }

    /** Declares the members of the input; of an update every member is optional and the optional ones can be cleared with null. */
    private record Members(Schema schema, boolean update)
    {
        Members required(String name, Schema type)
        {
            if (update)
                schema.optionalProperty(name, type);
            else
                schema.property(name, type);
            return this;
        }

        Members optional(String name, Schema type)
        {
            schema.optionalProperty(name, update ? type.nullable() : type);
            return this;
        }
    }

    static void add(OperationContext context)
    {
        context.rejectUnknownFields(KNOWN);
        var input = context.input();

        var name = parseTypeName(context, input);
        if (TransactionRecord.TRANSFER.equals(name))
            TransferOperations.add(context);
        else if (name != null && PORTFOLIO_TYPES.contains(name))
            TradeOperations.add(context, PortfolioTransaction.Type.valueOf(name));
        else
            addCash(context, name == null ? null : Type.valueOf(name));
    }

    private static void addCash(OperationContext context, Type type)
    {
        var input = context.input();

        refuse(context, input, "investmentAccount", "kind", "from", "to", "targetAmount", "exchangeRate");

        var account = parseCashAccount(context, input);
        var dateTime = parseDateTime(context, input);
        var currencyCode = parseCurrency(context, input, account);
        var amount = parseAmount(context, input, "amount", Values.Amount, true);
        var security = parseInstrument(context, input, type == null || SECURITY_ALLOWED.contains(type),
                        type != null && SECURITY_REQUIRED.contains(type));
        var shares = parseShares(context, input, type);
        var units = parseUnits(context, input, currencyCode, security);
        var exDate = exDate(context, input, type, security, dateTime);

        if (type != null)
            parseGrossAmount(context, input, type.isCredit(), amount, units);

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

        runCoreChecks(context, check -> check.process(transaction, account));

        commit(context, BuiltRecord.of(transaction, account));
    }

    /**
     * Puts the checked record into the model and reports it, or in an update
     * has it replace the record it stands for. Refuses a change that would
     * oversell.
     */
    static void commit(OperationContext context, BuiltRecord built)
    {
        var replaced = context.replacing();

        OversellGuard.check(context, "shares", replaced.map(TransactionUpdates.Existing::effects).orElse(List.of()),
                        OversellGuard.effectsOf(built.sides()));
        if (context.failed())
            return;

        if (replaced.isPresent())
        {
            TransactionUpdates.replace(context, replaced.get(), built);
            return;
        }

        built.insert().run();
        reportCreated(context, built.primary());
    }

    /** Runs core's non-mutating import checks, reporting every error they find. */
    static void runCoreChecks(OperationContext context, Function<ImportAction, ImportAction.Status> run)
    {
        for (var check : CORE_CHECKS)
        {
            var status = run.apply(check);
            if (status.getCode() == ImportAction.Status.Code.ERROR)
                context.fail(check instanceof CheckForexGrossValueAction ? "units" : "", "import-check-failed",
                                status.getMessage(), check.getClass().getSimpleName());
        }
    }

    /** Reports the record the transaction is the primary side of as created. */
    private static void reportCreated(OperationContext context, Transaction primary)
    {
        var record = TransactionRecord.allOf(context.client()).stream().filter(r -> r.primary() == primary).findFirst()
                        .orElseThrow();
        context.created(new OperationContext.Change("transaction", record.id(), null,
                        TransactionsCommand.render(record)));
    }

    /** Reports a failure for each of the members that is present, because the kind of transaction doesn't take it. */
    static void refuse(OperationContext context, JsonObject input, String... members)
    {
        for (var member : members)
        {
            if (input.has(member) && !input.get(member).isJsonNull())
                context.fail(member, NOT_ALLOWED.get(member),
                                "This kind of transaction must not have " + member + ".");
        }
    }

    private static String parseTypeName(OperationContext context, JsonObject input)
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
                            + "' is not a transaction type this operation adds. Supported: "
                            + String.join(", ", TYPE_NAMES) + ".");
            return null;
        }
        return name;
    }

    static Account parseCashAccount(OperationContext context, JsonObject input)
    {
        return parseCashAccount(context, input, "cashAccount");
    }

    static Account parseCashAccount(OperationContext context, JsonObject input, String member)
    {
        return find(context, input, member, "A transaction needs " + member + ".", "cash account",
                        context.client().getAccounts(), Account::getUUID);
    }

    static Portfolio parseInvestmentAccount(OperationContext context, JsonObject input)
    {
        return parseInvestmentAccount(context, input, "investmentAccount");
    }

    static Portfolio parseInvestmentAccount(OperationContext context, JsonObject input, String member)
    {
        return find(context, input, member, "A transaction needs " + member + ".", "investment account",
                        context.client().getPortfolios(), Portfolio::getUUID);
    }

    /** The entity of the UUID given in the member, which must have been in the file before the batch. */
    private static <T> T find(OperationContext context, JsonObject input, String member, String requiredMessage,
                    String noun, List<T> candidates, Function<T, String> uuidOf)
    {
        var uuid = parseText(context, input, member);
        if (uuid == null)
        {
            requireText(context, input, member, requiredMessage);
            return null;
        }

        var found = candidates.stream().filter(c -> uuid.equals(uuidOf.apply(c)) && context.existedBeforeBatch(c))
                        .findFirst();
        if (found.isEmpty())
        {
            context.fail(member, "reference-not-found", "No " + noun + " with UUID " + uuid + " in the file.");
            return null;
        }
        return found.get();
    }

    static LocalDateTime parseDateTime(OperationContext context, JsonObject input)
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

    static String parseCurrency(OperationContext context, JsonObject input, Account account)
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

    static Security parseInstrument(OperationContext context, JsonObject input, boolean allowed, boolean required)
    {
        var uuid = parseText(context, input, "instrument");
        var present = input.has("instrument") && !input.get("instrument").isJsonNull()
                        && OperationContext.isText(input.get("instrument")) && uuid != null;

        if (!present)
        {
            if (required)
                context.fail("instrument", "required", "This kind of transaction needs an instrument.");
            return null;
        }

        if (!allowed)
        {
            context.fail("instrument", "instrument-not-allowed",
                            "This kind of transaction must not have an instrument.");
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

    static void parseGrossAmount(OperationContext context, JsonObject input, boolean credit, Long amount,
                    List<Transaction.Unit> units)
    {
        if (!input.has("grossAmount") || input.get("grossAmount").isJsonNull())
            return;

        var gross = parseAmount(context, input, "grossAmount", Values.Amount, false);
        if (gross == null || amount == null)
            return;

        var taxAndFees = units.stream()
                        .filter(u -> u.getType() == Transaction.Unit.Type.FEE || u.getType() == Transaction.Unit.Type.TAX)
                        .mapToLong(u -> u.getAmount().getAmount()).sum();
        var expected = amount + (credit ? taxAndFees : -taxAndFees);

        if (gross != expected)
            context.fail("grossAmount", "gross-amount-mismatch",
                            "The gross amount does not reconcile with the amount, fees and taxes.");
    }

    static List<Transaction.Unit> parseUnits(OperationContext context, JsonObject input, String currencyCode,
                    Security security)
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
                    var unit = parseUnit(context, array.get(i), i, currencyCode, security);
                    if (unit != null)
                        units.add(unit);
                }
            }
        }

        requireForexGrossValue(context, currencyCode, security, units);
        return units;
    }

    private static Transaction.Unit parseUnit(OperationContext context, JsonElement element, int index,
                    String currencyCode, Security security)
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
            if (unitType != Transaction.Unit.Type.GROSS_VALUE && security != null
                            && !forexCurrency.equals(security.getCurrencyCode()))
            {
                context.fail(field + ".forexCurrencyCode", "forex-currency-mismatch",
                                "A fee or tax may carry forex only in the instrument's currency "
                                                + security.getCurrencyCode() + ".");
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
                    List<Transaction.Unit> units)
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
    static Long parseAmount(OperationContext context, JsonObject input, String field, Values<Long> values,
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
    static BigDecimal rawNumber(JsonObject input, String member)
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

    static String parseText(OperationContext context, JsonObject input, String member)
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
