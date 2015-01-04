package name.abuchen.portfolio.money;

public final class MutableMoney
{
    private final String currencyCode;
    private long amount;

    private MutableMoney(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public static MutableMoney of(String currencyCode)
    {
        return new MutableMoney(currencyCode);
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

    public void add(Money money)
    {
        if (!this.currencyCode.equals(money.getCurrencyCode()))
            throw new MonetaryException();

        this.amount += money.getAmount();
    }

    public void substract(Money money)
    {
        if (!currencyCode.equals(money.getCurrencyCode()))
            throw new MonetaryException();

        this.amount -= money.getAmount();
    }

    public MutableMoney addAll(MutableMoney other)
    {
        if (!this.currencyCode.equals(other.currencyCode))
            throw new MonetaryException();

        this.amount += other.amount;

        return this;
    }

    public Money toMoney()
    {
        return Money.of(currencyCode, amount);
    }
}