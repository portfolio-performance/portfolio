package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.money.Money;

public abstract class Transaction implements Annotated
{
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
    private ForexData forex;
    private long shares;
    private String note;

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

    public ForexData getForex()
    {
        return forex;
    }

    public void setForex(ForexData forex)
    {
        this.forex = forex;
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public boolean isPotentialDuplicate(Transaction other)
    {
        if (!other.getDate().equals(date))
            return false;

        if (!other.getCurrencyCode().equals(currencyCode))
            return false;

        if (other.getAmount() != getAmount())
            return false;

        if (other.getShares() != shares)
            return false;

        if (!Objects.equals(other.getSecurity(), security))
            return false;

        return true;
    }

    public static final <E extends Transaction> List<E> sortByDate(List<E> transactions)
    {
        Collections.sort(transactions, new ByDate());
        return transactions;
    }
}
