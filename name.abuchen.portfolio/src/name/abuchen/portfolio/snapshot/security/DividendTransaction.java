package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class DividendTransaction extends Transaction
{
    private Account account;

    private long totalShares;
    private Money fifoCost;

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

    public long getDividendPerShare()
    {
        return amountFractionPerShare(getAmount(), getShares());
    }

    public Money getFifoCost()
    {
        return fifoCost;
    }

    /* package */void setFifoCost(Money fifoCost)
    {
        this.fifoCost = fifoCost;
    }

    /* package */void setTotalShares(long totalShares)
    {
        this.totalShares = totalShares;
    }

    public double getPersonalDividendYield()
    {
        if (fifoCost.getAmount() <= 0)
            return 0;

        double cost = fifoCost.getAmount();

        if (getShares() > 0)
            cost = fifoCost.getAmount() * (getShares() / (double) totalShares);

        return getAmount() / cost;
    }

    static long amountFractionPerShare(long amount, long shares)
    {
        if (shares == 0)
            return 0;

        return Math.round((double) (amount * (Values.AmountFraction.factor() / Values.Amount.factor()) * Values.Share
                        .divider()) / (double) shares);
    }
}
