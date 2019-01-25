package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;

public class Trade
{
    private Security security;
    private LocalDateTime start;
    private LocalDateTime end;
    private long shares;

    private List<TransactionPair<?>> transactions = new ArrayList<>();

    public Trade(Security security, long shares)
    {
        this.security = security;
        this.shares = shares;
    }

    public Security getSecurity()
    {
        return security;
    }

    public LocalDateTime getEnd()
    {
        return end;
    }

    /* package */ void setEnd(LocalDateTime end)
    {
        this.end = end;
    }

    public LocalDateTime getStart()
    {
        return start;
    }

    /* package */ void setStart(LocalDateTime start)
    {
        this.start = start;
    }

    public long getShares()
    {
        return shares;
    }

    public List<TransactionPair<?>> getTransactions()
    {
        return transactions;
    }

}
