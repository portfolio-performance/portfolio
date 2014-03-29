package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public abstract class Transaction
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

    private Date date;
    private Security security;
    private CrossEntry crossEntry;

    private long shares;

    private String note;

    public Transaction()
    {}

    public Transaction(Date date, Security security)
    {
        this(date, security, 0);
    }

    public Transaction(Date date, Security security, long shares)
    {
        this.date = date;
        this.security = security;
        this.shares = shares;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
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

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public abstract long getAmount();

    public static final <E extends Transaction> List<E> sortByDate(List<E> transactions)
    {
        Collections.sort(transactions, new ByDate());
        return transactions;
    }
}
