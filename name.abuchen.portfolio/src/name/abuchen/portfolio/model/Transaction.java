package name.abuchen.portfolio.model;

import java.util.Date;

public abstract class Transaction implements Comparable<Transaction>
{
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
}
