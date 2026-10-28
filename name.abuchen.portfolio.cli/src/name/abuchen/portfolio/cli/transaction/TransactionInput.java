package name.abuchen.portfolio.cli.transaction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import name.abuchen.portfolio.cli.command.EntityKind;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The read phase of a write: reads the members of the input into a
 * {@link Draft}, by the rules of the record's row of the {@link RuleTable}. A
 * member the row refuses is reported if it is present; every other member is
 * read and checked on its own. Whether a member is missing, and every check
 * that looks at more than one member, is left to {@link TransactionAdd}.
 * Failures go to the sink of the {@link OperationInput}.
 */
final class TransactionInput
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** The members that every type takes. */
    private static final List<String> COMMON = List.of(TransactionShape.DATE.name(), TransactionShape.TIME.name(),
                    TransactionShape.AMOUNT.name(), TransactionShape.CURRENCY_CODE.name(),
                    TransactionShape.NOTE.name(), TransactionShape.SOURCE.name());

    private final OperationInput input;
    private final List<FieldFailure> failures;
    private final Draft draft;

    /**
     * @param failures
     *            the sink of the input, which tells whether reading a member
     *            failed
     */
    private TransactionInput(OperationInput input, List<FieldFailure> failures, Draft draft)
    {
        this.input = input;
        this.failures = failures;
        this.draft = draft;
    }

    /** Reads the input of an add into a draft. */
    static Draft read(OperationInput input, List<FieldFailure> failures)
    {
        var type = type(input);
        var kind = TransactionRecord.TRANSFER.equals(type) ? kind(input) : null;
        var reader = new TransactionInput(input, failures, new Draft(type, kind));

        reader.refuseByTable();
        reader.readable().forEach(reader::read);
        return reader.draft;
    }

    /**
     * Reads the members of a patch onto the draft of a stored record, each as
     * an add reads it; null clears a member. The type and the kind are
     * checked by {@link Draft#refuseTypeOrKindChange} before.
     *
     * @param skip
     *            the members that address the record rather than change it
     * @return whether the patch has a member that an add does not declare
     */
    static boolean lay(OperationInput patch, List<FieldFailure> failures, Draft draft, Set<String> skip)
    {
        var reader = new TransactionInput(patch, failures, draft);
        var readable = reader.readable();
        var unknown = false;

        for (var member : patch.json().keySet())
        {
            if (skip.contains(member) || member.equals(TransactionShape.TYPE.name())
                            || member.equals(TransactionShape.KIND.name()) && draft.kind != null)
                continue;

            if (!TransactionAdd.SCHEMA.declares(member))
                unknown = true;
            else if (readable.contains(member))
                reader.read(member);
            else
                RuleTable.Ruled.byId(member).ifPresent(reader::refuseIfPresent);
        }
        return unknown;
    }

    /** The type of an add, or null after reporting why there is none. */
    private static String type(OperationInput input)
    {
        var member = TransactionShape.TYPE.name();
        var element = input.json().get(member);
        if (element == null || element.isJsonNull())
        {
            input.fail(member, ErrorCode.REQUIRED, "A transaction needs a type.");
            return null;
        }
        if (!OperationInput.isText(element))
        {
            input.fail(member, ErrorCode.INVALID_TYPE, "The type must be a string.");
            return null;
        }

        var name = element.getAsString();
        if (!TransactionShape.TYPES.contains(name))
        {
            unsupportedType(input, name);
            return null;
        }
        return name;
    }

    /** Reports a type that no operation adds. */
    static void unsupportedType(OperationInput input, String name)
    {
        input.fail(TransactionShape.TYPE.name(), ErrorCode.UNSUPPORTED_TYPE, "'" + name
                        + "' is not a transaction type this operation adds. Supported: "
                        + String.join(", ", TransactionShape.TYPES) + ".");
    }

    private static TransactionRecord.TransferKind kind(OperationInput input)
    {
        var member = TransactionShape.KIND.name();
        var kinds = String.join(", ", TransactionRecord.TransferKind.ids());
        var kind = input.requiredText(member, "A transfer needs a kind: " + kinds + ".");
        if (kind == null)
            return null;

        for (var candidate : TransactionRecord.TransferKind.values())
        {
            if (candidate.id().equals(kind))
                return candidate;
        }

        input.fail(member, ErrorCode.UNSUPPORTED_KIND, "'" + kind + "' is not a kind of transfer: " + kinds + ".");
        return null;
    }

    /** The members to read: those every type takes and those the row doesn't refuse. */
    private List<String> readable()
    {
        if (draft.row == null)
            return COMMON;
        return Stream.concat(COMMON.stream(), Arrays.stream(RuleTable.Ruled.values())
                        .filter(member -> member != RuleTable.Ruled.KIND
                                        && draft.row.rule(member) != RuleTable.Rule.REFUSED)
                        .map(RuleTable.Ruled::id)).toList();
    }

    /**
     * Reports each member that is present although the row refuses it.
     * Without a row, those that every row of the type refuses.
     */
    private void refuseByTable()
    {
        var refused = draft.row != null
                        ? Arrays.stream(RuleTable.Ruled.values())
                                        .filter(member -> draft.row.rule(member) == RuleTable.Rule.REFUSED).toList()
                        : RuleTable.refusedByEveryRowOf(draft.type);
        refused.forEach(this::refuseIfPresent);
    }

    /** Reports the member if it is present, under its refusal code. */
    private void refuseIfPresent(RuleTable.Ruled member)
    {
        if (input.has(member.id()))
            input.fail(member.id(), member.refusalCode(),
                            member.refusalMessage(draft.row != null ? draft.row.label() : draft.type));
    }

    /** Reads one member onto the draft; null or absent clears it. */
    private void read(String member)
    {
        var before = failures.size();

        var ruled = RuleTable.Ruled.byId(member);
        if (ruled.isPresent())
            read(ruled.get());
        else if (member.equals(TransactionShape.DATE.name()))
            draft.date = date();
        else if (member.equals(TransactionShape.TIME.name()))
            draft.time = time();
        else if (member.equals(TransactionShape.AMOUNT.name()))
            draft.amount = input.amount(member, Values.Amount, true, null);
        else if (member.equals(TransactionShape.CURRENCY_CODE.name()))
            draft.currencyCode = currency();
        else if (member.equals(TransactionShape.NOTE.name()))
            draft.note = input.text(member);
        else if (member.equals(TransactionShape.SOURCE.name()))
            draft.source = input.text(member);
        else
            throw new IllegalArgumentException(member);

        if (failures.size() > before)
            draft.failed.add(member);
        else
            draft.failed.remove(member);
    }

    private void read(RuleTable.Ruled member)
    {
        var id = member.id();
        switch (member)
        {
            case KIND -> throw new IllegalArgumentException(id);
            case TARGET_AMOUNT -> draft.targetAmount = input.amount(id, Values.Amount, true, null);
            case EXCHANGE_RATE -> draft.exchangeRate = input.number(id, null, true);
            case INSTRUMENT -> draft.instrument = reference(member, Instruments.INSTRUMENTS.entityKind());
            case SHARES -> draft.shares = input.amount(id, Values.Share, true, null);
            case INVESTMENT_ACCOUNT -> draft.investmentAccount = reference(member,
                            InvestmentAccounts.INVESTMENT_ACCOUNTS.entityKind());
            case CASH_ACCOUNT -> draft.cashAccount = reference(member, CashAccounts.CASH_ACCOUNTS.entityKind());
            case FROM -> draft.from = transferAccount(member);
            case TO -> draft.to = transferAccount(member);
            case EX_DATE -> draft.exDate = exDate();
            case GROSS_AMOUNT -> draft.grossAmount = input.amount(id, Values.Amount, false, null);
            case UNITS -> draft.units = units();
        }
    }

    /** The existing entity the member names; null if absent or not found. */
    private <T> T reference(RuleTable.Ruled member, EntityKind<T> kind)
    {
        var uuid = input.text(member.id());
        return uuid == null ? null : input.find(member.id(), uuid, kind);
    }

    /** A side of a transfer: a cash account or an investment account, by the kind. */
    private TransactionOwner<?> transferAccount(RuleTable.Ruled member)
    {
        return draft.kind == TransactionRecord.TransferKind.CASH
                        ? reference(member, CashAccounts.CASH_ACCOUNTS.entityKind())
                        : reference(member, InvestmentAccounts.INVESTMENT_ACCOUNTS.entityKind());
    }

    private LocalDate date()
    {
        var member = TransactionShape.DATE.name();
        var text = input.text(member);
        if (text == null)
            return null;

        try
        {
            return LocalDate.parse(text);
        }
        catch (DateTimeParseException e)
        {
            input.fail(member, ErrorCode.INVALID_DATE, "'" + text + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }
    }

    private LocalTime time()
    {
        var member = TransactionShape.TIME.name();
        var text = input.text(member);
        if (text == null)
            return null;

        try
        {
            return LocalTime.parse(text, TIME);
        }
        catch (DateTimeParseException e)
        {
            input.fail(member, ErrorCode.INVALID_TIME, "'" + text + "' is not a time of day (HH:mm).");
            return null;
        }
    }

    private String currency()
    {
        var member = TransactionShape.CURRENCY_CODE.name();
        var code = input.text(member);
        if (code != null && !CurrencyUnit.containsCurrencyCode(code))
        {
            input.fail(member, ErrorCode.INVALID_CURRENCY, "'" + code + "' is not a known ISO currency code.");
            return null;
        }
        return code;
    }

    private LocalDate exDate()
    {
        var member = RuleTable.Ruled.EX_DATE.id();
        if (!input.has(member))
            return null;

        var text = input.json().get(member);
        if (!OperationInput.isText(text))
        {
            input.fail(member, ErrorCode.INVALID_TYPE, "The ex-date must be a string.");
            return null;
        }

        try
        {
            return LocalDate.parse(text.getAsString());
        }
        catch (DateTimeParseException e)
        {
            input.fail(member, ErrorCode.INVALID_DATE, "'" + text.getAsString() + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }
    }

    /** The units that pass the checks of the unit alone; an absent or null list is empty. */
    private List<Draft.Unit> units()
    {
        var units = new ArrayList<Draft.Unit>();
        input.eachObject(RuleTable.Ruled.UNITS.id(), "A unit must be a JSON object.", element -> {
            var unit = unit(element);
            if (unit != null)
                units.add(new Draft.Unit(element.field(""), unit));
        });
        return units;
    }

    private static Transaction.Unit unit(OperationInput unit)
    {
        unit.rejectUnknownFields(TransactionShape.UNIT_INPUT);

        var typeName = unit.rawText("type");
        if (typeName == null || !TransactionShape.UNIT_TYPES.contains(typeName))
        {
            unit.fail("type", ErrorCode.UNSUPPORTED_UNIT_TYPE,
                            "A unit type must be one of " + String.join(", ", TransactionShape.UNIT_TYPES) + ".");
            return null;
        }
        var unitType = Transaction.Unit.Type.valueOf(typeName);

        var amount = unit.amount("amount", Values.Amount, true, null);
        var unitCurrency = unit.rawText("currencyCode");
        if (unitCurrency == null)
            unit.fail("currencyCode", ErrorCode.REQUIRED, "A unit needs a currency.");

        var json = unit.json();
        var hasForex = json.has("forexAmount") || json.has("forexCurrencyCode") || json.has("exchangeRate");
        var forexAmount = unit.amount("forexAmount", Values.Amount, true, null);
        var forexCurrency = unit.rawText("forexCurrencyCode");
        var rate = unit.rawNumber("exchangeRate");

        if (unitType == Transaction.Unit.Type.GROSS_VALUE && !hasForex)
        {
            unit.fail("", ErrorCode.FOREX_REQUIRED, "A GROSS_VALUE unit needs forexAmount, forexCurrencyCode and exchangeRate.");
            return null;
        }

        if (hasForex)
        {
            if (forexAmount == null || forexCurrency == null || rate == null)
            {
                unit.fail("", ErrorCode.FOREX_INCOMPLETE,
                                "forexAmount, forexCurrencyCode and exchangeRate are given together or not at all.");
                return null;
            }
            if (rate.signum() <= 0)
            {
                unit.fail("exchangeRate", ErrorCode.NOT_POSITIVE, "The exchange rate must be positive.");
                return null;
            }
            if (!CurrencyUnit.containsCurrencyCode(forexCurrency))
            {
                unit.fail("forexCurrencyCode", ErrorCode.INVALID_CURRENCY,
                                "'" + forexCurrency + "' is not a known ISO currency code.");
                return null;
            }
        }

        if (amount == null || unitCurrency == null)
            return null;

        var amountMoney = Money.of(unitCurrency, amount);
        if (!hasForex)
            return new Transaction.Unit(unitType, amountMoney);

        var forexMoney = Money.of(forexCurrency, forexAmount);
        if (!Transaction.Unit.isWithinRoundingTolerance(amountMoney, forexMoney, rate))
        {
            unit.fail("", ErrorCode.FOREX_MISMATCH,
                            "forexAmount times exchangeRate is not amount within core's rounding tolerance.");
            return null;
        }
        return new Transaction.Unit(unitType, amountMoney, forexMoney, rate);
    }
}
