package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public class DividendInitialTransaction extends Transaction
{
    private SecurityPosition position;

    public DividendInitialTransaction(SecurityPosition position, LocalDate time)
    {
        this.position = position;
        this.setDate(time);
        this.setSecurity(position.getSecurity());
        this.setMonetaryAmount(position.calculateValue());
    }

    public SecurityPosition getPosition()
    {
        return position;
    }
}
