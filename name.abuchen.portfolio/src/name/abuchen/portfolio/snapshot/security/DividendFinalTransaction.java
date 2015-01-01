package name.abuchen.portfolio.snapshot.security;

import java.util.Date;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public class DividendFinalTransaction extends Transaction
{
    private SecurityPosition position;

    public DividendFinalTransaction(SecurityPosition position, Date time)
    {
        this.position = position;
        this.setSecurity(position.getSecurity());
        this.setDate(time);
    }

    @Override
    public long getAmount()
    {
        return position.calculateValue().getAmount(); // FIXME c
    }

    public SecurityPosition getPosition()
    {
        return position;
    }
}
