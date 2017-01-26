package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public abstract class Transaction implements Annotated
{
    public static class Unit
    {
        public enum Type
        {
            GROSS_VALUE, TAX, FEE
        }

        /**
         * Type of transaction unit
         */
        private final Type type;

        /**
         * Amount in transaction currency
         */
        private final Money amount;

        /**
         * Original amount in foreign currency; can be null if unit is recorded
         * in currency of transaction
         */
        private final Money forex;

        /**
         * Exchange rate used to convert forex amount to amount
         */
        private final BigDecimal exchangeRate;

        public Unit(Type type, Money amount)
        {
            this.type = Objects.requireNonNull(type);
            this.amount = Objects.requireNonNull(amount);
            this.forex = null;
            this.exchangeRate = null;
        }

        public Unit(Type type, Money amount, Money forex, BigDecimal exchangeRate)
        {
            this.type = Objects.requireNonNull(type);
            this.amount = Objects.requireNonNull(amount);
            this.forex = Objects.requireNonNull(forex);
            this.exchangeRate = Objects.requireNonNull(exchangeRate);

            // check whether given amount is in range of converted amount
            long upper = Math.round(exchangeRate.add(BigDecimal.valueOf(0.001))
                            .multiply(BigDecimal.valueOf(forex.getAmount())).doubleValue());
            long lower = Math.round(exchangeRate.add(BigDecimal.valueOf(-0.001))
                            .multiply(BigDecimal.valueOf(forex.getAmount())).doubleValue());

            if (amount.getAmount() < lower || amount.getAmount() > upper)
                throw new IllegalArgumentException(
                                MessageFormat.format(Messages.MsgErrorIllegalForexUnit, type.toString(),
                                                Values.Money.format(forex), exchangeRate, Values.Money.format(amount)));
        }

        public Type getType()
        {
            return type;
        }

        public Money getAmount()
        {
            return amount;
        }

        public Money getForex()
        {
            return forex;
        }

        public BigDecimal getExchangeRate()
        {
            return exchangeRate;
        }

    }

    public static final class ByDate implements Comparator<Transaction>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Transaction t1, Transaction t2)
        {
            return t1.getDate().compareTo(t2.getDate());
        }
    }

    private LocalDate date;
    private String currencyCode;
    private long amount;

    private Security security;
    private CrossEntry crossEntry;
    private long shares;
    private String note;

    private List<Unit> units;

    public Transaction()
    {}

    public Transaction(LocalDate date, String currencyCode, long amount)
    {
        this(date, currencyCode, amount, null, 0, null);
    }

    public Transaction(LocalDate date, String currencyCode, long amount, Security security, long shares, String note)
    {
        this.date = date;
        this.currencyCode = currencyCode;
        this.amount = amount;
        this.security = security;
        this.shares = shares;
        this.note = note;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = date;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }

    public Money getMonetaryAmount()
    {
        return Money.of(currencyCode, amount);
    }

    public void setMonetaryAmount(Money value)
    {
        this.currencyCode = value.getCurrencyCode();
        this.amount = value.getAmount();
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public CrossEntry getCrossEntry()
    {
        return crossEntry;
    }

    /* package */void setCrossEntry(CrossEntry crossEntry)
    {
        this.crossEntry = crossEntry;
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
    }

    public Stream<Unit> getUnits()
    {
        return units != null ? units.stream() : Stream.empty();
    }

    /**
     * Returns any unit of the given type
     */
    public Optional<Unit> getUnit(Unit.Type type)
    {
        return getUnits().filter(u -> u.getType() == type).findAny();
    }

    /**
     * Clears all currently set units
     */
    public void clearUnits()
    {
        units = null;
    }

    public void addUnit(Unit unit)
    {
        Objects.requireNonNull(unit.getAmount());
        if (!unit.getAmount().getCurrencyCode().equals(currencyCode))
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorUnitCurrencyMismatch,
                            unit.getType().toString(), unit.getAmount().getCurrencyCode(), currencyCode));

        if (units == null)
            units = new ArrayList<>();
        units.add(unit);
    }

    public void addUnits(Stream<Unit> items)
    {
        if (units == null)
            units = new ArrayList<>();

        items.forEach(units::add);
    }

    public void removeUnit(Unit unit)
    {
        if (units == null)
            units = new ArrayList<>();
        units.remove(unit);
    }

    /**
     * Returns the sum of units in transaction currency
     */
    public Money getUnitSum(Unit.Type type)
    {
        return getUnits().filter(u -> u.getType() == type) //
                        .collect(MoneyCollectors.sum(getCurrencyCode(), Unit::getAmount));
    }

    /**
     * Returns the sum of units in the term currency of the currency converter
     */
    public Money getUnitSum(Unit.Type type, CurrencyConverter converter)
    {
        return getUnits().filter(u -> u.getType() == type)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency(), unit -> {
                            if (converter.getTermCurrency().equals(unit.getAmount().getCurrencyCode()))
                                return unit.getAmount();
                            else
                                return unit.getAmount().with(converter.at(date));
                        }));
    }

    public static final <E extends Transaction> List<E> sortByDate(List<E> transactions)
    {
        Collections.sort(transactions, new ByDate());
        return transactions;
    }
}
