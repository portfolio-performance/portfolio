package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;

public class DividendTransaction extends Transaction
{
    private Account account;

    private long shares;
    private long amount;

    private boolean isDiv12;
    private int divEventId;

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

    public boolean getIsDiv12()
    {
        return isDiv12;
    }

    public void setIsDiv12(boolean isDiv12)
    {
        this.isDiv12 = isDiv12;
    }

    public int getDivEventId()
    {
        return divEventId;
    }

    public void setDivEventId(int divEventId)
    {
        this.divEventId = divEventId;
    }

    static public long amountFractionPerShare(long amount, long shares)
    {
        if (shares == 0)
            return 0;

        return Math.round((double) (amount * (Values.AmountFraction.factor() / Values.Amount.factor()) * Values.Share
                        .divider()) / (double) shares);
    }

    static public long amountPerShare(long amount, long shares)
    {
        if (shares != 0)
        {
            return Math.round((double) amount / (double) shares * Values.Share.divider());
        }
        else
        {
            return 0;
        }
    }

    static public long amountTimesShares(long price, long shares)
    {
        if (shares != 0)
        {
            return Math.round((double) price * (double) shares / Values.Share.divider());
        }
        else
        {
            return 0;
        }
    }

}
