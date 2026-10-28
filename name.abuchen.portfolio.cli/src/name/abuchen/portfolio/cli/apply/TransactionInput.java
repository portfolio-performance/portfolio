package name.abuchen.portfolio.cli.apply;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.cli.transaction.TransactionShape;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Reads the members that every kind of <code>transaction.add</code> shares:
 * accounts, instrument, date, currency, amounts and units, with the rules of a
 * transaction record. Failures are reported on the operation.
 */
final class TransactionInput
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

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

    private final OperationContext context;
    private final OperationInput input;

    TransactionInput(OperationContext context)
    {
        this.context = context;
        this.input = context.input();
    }

    /** Reports a failure for each of the members that is present, because the kind of transaction doesn't take it. */
    void refuse(String... members)
    {
        for (var member : members)
        {
            if (input.has(member))
                context.fail(member, NOT_ALLOWED.get(member),
                                "This kind of transaction must not have " + member + ".");
        }
    }

    String text(String member)
    {
        return input.text(member);
    }

    Account cashAccount()
    {
        return cashAccount("cashAccount");
    }

    Account cashAccount(String member)
    {
        return input.existing(member, CashAccounts.CASH_ACCOUNTS.entityKind(),
                        "A transaction needs " + member + ".");
    }

    Portfolio investmentAccount()
    {
        return investmentAccount("investmentAccount");
    }

    Portfolio investmentAccount(String member)
    {
        return input.existing(member, InvestmentAccounts.INVESTMENT_ACCOUNTS.entityKind(),
                        "A transaction needs " + member + ".");
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
            context.fail("date", "invalid-date", "'" + date + "' is not an ISO date (yyyy-MM-dd).");
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
            context.fail("time", "invalid-time", "'" + timeText + "' is not a time of day (HH:mm).");
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

    Security instrument(boolean allowed, boolean required)
    {
        var uuid = input.text("instrument");
        if (uuid == null)
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

        return input.find("instrument", uuid, Instruments.INSTRUMENTS.entityKind());
    }

    /** A positive amount, read at the given precision; a missing one is required. */
    Long amount(String member, Values<Long> values)
    {
        return input.amount(member, values, true, "A transaction needs " + member + ".");
    }

    /** Checks the optional gross amount against the amount, fees and taxes. */
    void grossAmount(boolean credit, Long amount, List<Transaction.Unit> units)
    {
        if (!input.has("grossAmount"))
            return;

        var gross = input.amount("grossAmount", Values.Amount, false, "A transaction needs grossAmount.");
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

    /** The valid units; an instrument in another currency needs a forex gross value. */
    List<Transaction.Unit> units(String currencyCode, Security security)
    {
        var units = new ArrayList<Transaction.Unit>();

        input.eachObject("units", "A unit must be a JSON object.", element -> {
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

    /** A dividend on an instrument in another currency needs a forex gross-value unit in that currency. */
    private void requireForexGrossValue(String currencyCode, Security security, List<Transaction.Unit> units)
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
}
