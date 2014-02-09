package name.abuchen.portfolio.snapshot.security;

import java.util.Date;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public class DividendInitialTransaction extends Transaction
{
    private SecurityPosition position;

    public DividendInitialTransaction(SecurityPosition position, Date time)
    {
        this.position = position;
        this.setSecurity(position.getSecurity());
        this.setDate(time);
    }

    @Override
    public long getAmount()
    {
        return position.calculateValue();
    }

    public SecurityPosition getPosition()
    {
        return position;
    }
}