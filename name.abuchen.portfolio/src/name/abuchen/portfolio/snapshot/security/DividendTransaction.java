package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;

public class DividendTransaction extends Transaction
{
    private Account account;

    private long shares;
    private long amount;

    private long fifoCost;

    public DividendTransaction()
    {}

    public Account getAccount()
    {
        return account;
    }

    /* package */void setAccount(Account account)
    {
        this.account = account;
    }

    @Override
    public long getAmount()
    {
        return amount;
    }

    /* package */void setAmount(long amount)
    {
        this.amount = amount;
    }

    public long getShares()
    {
        return shares;
    }

    /* package */void setShares(long shares)
    {
        this.shares = shares;
    }

    public long getDividendPerShare()
    {
        return amountFractionPerShare(amount, shares);
    }

    public long getFifoCost()
    {
        return fifoCost;
    }

    /* package */void setFifoCost(long fifoCost)
    {
        this.fifoCost = fifoCost;
    }

    public double getPersonalDividendYield()
    {
        return fifoCost > 0 ? amount / (double) fifoCost : 0;
    }

    static long amountFractionPerShare(long amount, long shares)
    {
        if (shares == 0)
            return 0;

        return Math.round((double) (amount * (Values.AmountFraction.factor() / Values.Amount.factor()) * Values.Share
                        .divider()) / (double) shares);
    }
}
