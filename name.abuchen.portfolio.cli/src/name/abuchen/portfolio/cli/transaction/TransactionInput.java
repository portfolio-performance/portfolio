package name.abuchen.portfolio.cli.transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.cli.command.EntityKind;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Reads the members of a record to add, by the rules of its row of the
 * {@link RuleTable}: a refused member that is present is reported, a required
 * one that is missing is reported, and an optional one is read if it is
 * there. Failures go to the sink of the {@link OperationInput}.
 */
final class TransactionInput
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final OperationInput input;
    private final String type;
    private final RuleTable.Row row;

    /**
     * @param type
     *            the record's type, or null if it is not known
     * @param row
     *            the row of the record's type, or null if the type or the
     *            kind is not known; then no member that depends on the row is
     *            read
     */
    TransactionInput(OperationInput input, String type, RuleTable.Row row)
    {
        this.input = input;
        this.type = type;
        this.row = row;
    }

    /** The rule of the row for the member; without a row, read nothing. */
    private RuleTable.Rule rule(RuleTable.Ruled member)
    {
        return row == null ? RuleTable.Rule.REFUSED : row.rule(member);
    }

    /**
     * Reports each member that is present although the row refuses it.
     * Without a row, those that every row of the type refuses.
     */
    void refuseByTable()
    {
        var refused = row != null
                        ? Arrays.stream(RuleTable.Ruled.values())
                                        .filter(member -> row.rule(member) == RuleTable.Rule.REFUSED).toList()
                        : RuleTable.refusedByEveryRowOf(type);
        refused.forEach(this::refuseIfPresent);
    }

    /** Reports the member if it is present, under its refusal code. */
    void refuseIfPresent(RuleTable.Ruled member)
    {
        refuseIfPresent(member, member.refusalMessage(row != null ? row.label() : type));
    }

    /** Reports the member if it is present, under its refusal code, because a rule across members refuses it. */
    void refuseIfPresent(RuleTable.Ruled member, String message)
    {
        if (input.has(member.id()))
            input.fail(member.id(), member.refusalCode(), message);
    }

    String text(String member)
    {
        return input.text(member);
    }

    /** The existing entity the member names, by the rule of the row; null if refused, absent or failed. */
    <T> T reference(RuleTable.Ruled member, EntityKind<T> kind)
    {
        return switch (rule(member))
        {
            case REFUSED -> null;
            case REQUIRED -> input.existing(member.id(), kind, "A transaction needs " + member.id() + ".");
            case OPTIONAL -> {
                var uuid = input.text(member.id());
                yield uuid == null ? null : input.find(member.id(), uuid, kind);
            }
        };
    }

    /** A positive number of the member, by the rule of the row; null if refused, absent or failed. */
    Long number(RuleTable.Ruled member, Values<Long> values)
    {
        return switch (rule(member))
        {
            case REFUSED -> null;
            case REQUIRED -> input.amount(member.id(), values, true, "A transaction needs " + member.id() + ".");
            case OPTIONAL -> input.amount(member.id(), values, true, null);
        };
    }

    LocalDateTime dateTime()
    {
        var date = input.requiredText("date", "A transaction needs a date.");
        if (date == null)
            return null;

        LocalDate day;
        try
        {
            day = LocalDate.parse(date);
        }
        catch (DateTimeParseException e)
        {
            input.fail("date", "invalid-date", "'" + date + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }

        var timeText = input.text("time");
        if (timeText == null)
            return day.atStartOfDay();

        try
        {
            return LocalDateTime.of(day, LocalTime.parse(timeText, TIME));
        }
        catch (DateTimeParseException e)
        {
            input.fail("time", "invalid-time", "'" + timeText + "' is not a time of day (HH:mm).");
            return null;
        }
    }

    /**
     * @param account
     *            the cash account whose currency the amount must be in, or
     *            null if any currency goes
     */
    String currency(Account account)
    {
        var code = input.requiredText("currencyCode", "A transaction needs a currency.");
        if (code == null)
            return null;

        if (!CurrencyUnit.containsCurrencyCode(code))
        {
            input.fail("currencyCode", "invalid-currency", "'" + code + "' is not a known ISO currency code.");
            return null;
        }

        if (account != null && !code.equals(account.getCurrencyCode()))
        {
            input.fail("currencyCode", "currency-mismatch", "The amount must be in the cash account's currency "
                            + account.getCurrencyCode() + ", not " + code + ".");
            return null;
        }
        return code;
    }

    /** A positive amount, read at the given precision; a missing one is required. */
    Long amount(String member, Values<Long> values)
    {
        return input.amount(member, values, true, "A transaction needs " + member + ".");
    }

    /** Checks the optional gross amount against the amount, fees and taxes. */
    void grossAmount(boolean credit, Long amount, List<Transaction.Unit> units)
    {
        var member = RuleTable.Ruled.GROSS_AMOUNT.id();
        if (rule(RuleTable.Ruled.GROSS_AMOUNT) == RuleTable.Rule.REFUSED || !input.has(member))
            return;

        var gross = input.amount(member, Values.Amount, false, null);
        if (gross == null || amount == null)
            return;

        var taxAndFees = units.stream()
                        .filter(u -> u.getType() == Transaction.Unit.Type.FEE || u.getType() == Transaction.Unit.Type.TAX)
                        .mapToLong(u -> u.getAmount().getAmount()).sum();
        var expected = amount + (credit ? taxAndFees : -taxAndFees);

        if (gross != expected)
            input.fail(member, "gross-amount-mismatch",
                            "The gross amount does not reconcile with the amount, fees and taxes.");
    }

    /**
     * The valid ex-date to store, or null if it is absent, refused or
     * invalid. It is never after the payment date.
     */
    LocalDate exDate(LocalDateTime dateTime)
    {
        var member = RuleTable.Ruled.EX_DATE.id();
        if (rule(RuleTable.Ruled.EX_DATE) == RuleTable.Rule.REFUSED || !input.has(member))
            return null;

        var text = input.json().get(member);
        if (!OperationInput.isText(text))
        {
            input.fail(member, "invalid-type", "The ex-date must be a string.");
            return null;
        }

        LocalDate ex;
        try
        {
            ex = LocalDate.parse(text.getAsString());
        }
        catch (DateTimeParseException e)
        {
            input.fail(member, "invalid-date", "'" + text.getAsString() + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }

        if (dateTime != null && ex.isAfter(dateTime.toLocalDate()))
        {
            input.fail(member, "exdate-after-payment", "The ex-date must not be after the payment date.");
            return null;
        }
        return ex;
    }

    /** The valid units; an instrument in another currency needs a forex gross value. */
    List<Transaction.Unit> units(String currencyCode, Security security)
    {
        var units = new ArrayList<Transaction.Unit>();
        if (rule(RuleTable.Ruled.UNITS) == RuleTable.Rule.REFUSED)
            return units;

        input.eachObject(RuleTable.Ruled.UNITS.id(), "A unit must be a JSON object.", element -> {
            var unit = unit(element, currencyCode, security);
            if (unit != null)
                units.add(unit);
        });

        requireForexGrossValue(currencyCode, security, units);
        return units;
    }

    private static Transaction.Unit unit(OperationInput unit, String currencyCode, Security security)
    {
        unit.rejectUnknownFields(TransactionShape.UNIT_INPUT);

        var typeName = unit.rawText("type");
        if (typeName == null || !TransactionShape.UNIT_TYPES.contains(typeName))
        {
            unit.fail("type", "unsupported-unit-type",
                            "A unit type must be one of " + String.join(", ", TransactionShape.UNIT_TYPES) + ".");
            return null;
        }
        var unitType = Transaction.Unit.Type.valueOf(typeName);

        var amount = unit.amount("amount", Values.Amount, true, null);
        var unitCurrency = unit.rawText("currencyCode");
        if (unitCurrency == null)
            unit.fail("currencyCode", "required", "A unit needs a currency.");
        else if (currencyCode != null && !unitCurrency.equals(currencyCode))
            unit.fail("currencyCode", "currency-mismatch",
                            "A unit must be in the transaction's currency " + currencyCode + ".");

        var json = unit.json();
        var hasForex = json.has("forexAmount") || json.has("forexCurrencyCode") || json.has("exchangeRate");
        var forexAmount = unit.amount("forexAmount", Values.Amount, true, null);
        var forexCurrency = unit.rawText("forexCurrencyCode");
        var rate = unit.rawNumber("exchangeRate");

        if (unitType == Transaction.Unit.Type.GROSS_VALUE && !hasForex)
        {
            unit.fail("", "forex-required", "A GROSS_VALUE unit needs forexAmount, forexCurrencyCode and exchangeRate.");
            return null;
        }

        if (hasForex)
        {
            if (forexAmount == null || forexCurrency == null || rate == null)
            {
                unit.fail("", "forex-incomplete",
                                "forexAmount, forexCurrencyCode and exchangeRate are given together or not at all.");
                return null;
            }
            if (rate.signum() <= 0)
            {
                unit.fail("exchangeRate", "not-positive", "The exchange rate must be positive.");
                return null;
            }
            if (!forexCurrency.equals(currencyCode) && !CurrencyUnit.containsCurrencyCode(forexCurrency))
            {
                unit.fail("forexCurrencyCode", "invalid-currency",
                                "'" + forexCurrency + "' is not a known ISO currency code.");
                return null;
            }
            if (unitType != Transaction.Unit.Type.GROSS_VALUE && security != null
                            && !forexCurrency.equals(security.getCurrencyCode()))
            {
                unit.fail("forexCurrencyCode", "forex-currency-mismatch",
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
            unit.fail("", "forex-mismatch",
                            "forexAmount times exchangeRate is not amount within core's rounding tolerance.");
            return null;
        }
        return new Transaction.Unit(unitType, amountMoney, forexMoney, rate);
    }

    /** A record on an instrument in another currency needs a forex gross-value unit in that currency. */
    private void requireForexGrossValue(String currencyCode, Security security, List<Transaction.Unit> units)
    {
        if (security == null || currencyCode == null || security.getCurrencyCode() == null
                        || security.getCurrencyCode().equals(currencyCode))
            return;

        var hasMatch = units.stream().filter(u -> u.getType() == Transaction.Unit.Type.GROSS_VALUE)
                        .anyMatch(u -> u.getForex() != null
                                        && security.getCurrencyCode().equals(u.getForex().getCurrencyCode()));
        if (!hasMatch)
            input.fail(RuleTable.Ruled.UNITS.id(), "forex-gross-value-required",
                            "The instrument is in " + security.getCurrencyCode() + " but the transaction is in "
                                            + currencyCode
                                            + "; add a GROSS_VALUE unit with its forex amount and exchange rate.");
    }
}
