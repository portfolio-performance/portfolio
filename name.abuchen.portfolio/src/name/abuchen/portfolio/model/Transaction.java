package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public abstract class Transaction implements Comparable<Transaction>
{
    private static final class ByDateComparator implements Comparator<Transaction>, Serializable
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

    public Transaction()
    {}

    public Transaction(Date date, Security security)
    {
        this.date = date;
        this.security = security;
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

    public abstract long getAmount();

    @Override
    public int compareTo(Transaction o)
    {
        if (date == null)
            return -1;
        if (o.date == null)
            return 1;
        return date.compareTo(o.date);
    }

    public static final <E extends Transaction> List<E> sortByDate(List<E> transactions)
    {
        Collections.sort(transactions, new ByDateComparator());
        return transactions;
    }
}
