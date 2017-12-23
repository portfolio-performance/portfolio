package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDateTime;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public class DividendFinalTransaction extends Transaction
{
    private SecurityPosition position;

    public DividendFinalTransaction(SecurityPosition position, LocalDateTime time)
    {
        this.position = position;
        this.setDateTime(time);
        this.setSecurity(position.getSecurity());
        this.setMonetaryAmount(position.calculateValue());
    }

    public SecurityPosition getPosition()
    {
        return position;
    }
}
