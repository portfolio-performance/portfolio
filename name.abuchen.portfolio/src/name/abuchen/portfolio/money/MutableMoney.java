package name.abuchen.portfolio.money;

public final class MutableMoney
{
    private final String currencyCode;
    private long amount;

    private MutableMoney(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    private MutableMoney(String currencyCode, long amount)
    {
        this(currencyCode);
        this.amount = amount;
    }

    public static MutableMoney of(String currencyCode)
    {
        return new MutableMoney(currencyCode);
    }

    public static MutableMoney of(Money money)
    {
        return new MutableMoney(money.getCurrencyCode(), money.getAmount());
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public boolean isZero()
    {
        return this.amount == 0;
    }

    public MutableMoney add(Money money)
    {
        if (!this.currencyCode.equals(money.getCurrencyCode()))
            throw new MonetaryException();

        this.amount += money.getAmount();

        return this;
    }

    public MutableMoney subtract(Money money)
    {
        if (!currencyCode.equals(money.getCurrencyCode()))
            throw new MonetaryException();

        this.amount -= money.getAmount();

        return this;
    }

    public MutableMoney add(MutableMoney money)
    {
        if (!this.currencyCode.equals(money.currencyCode))
            throw new MonetaryException();

        this.amount += money.amount;

        return this;
    }

    public MutableMoney subtract(MutableMoney money)
    {
        if (!currencyCode.equals(money.getCurrencyCode()))
            throw new MonetaryException();

        this.amount -= money.amount;

        return this;
    }

    public MutableMoney divide(double divisor)
    {
        this.amount = Math.round(this.amount / divisor);
        return this;
    }

    public Money toMoney()
    {
        return Money.of(currencyCode, amount);
    }

    @Override
    public String toString()
    {
        return Values.Money.format(this.toMoney());
    }
}
