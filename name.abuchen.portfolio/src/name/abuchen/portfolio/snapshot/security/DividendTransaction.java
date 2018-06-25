package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class DividendTransaction extends Transaction
{
    private Account account;

    private long totalShares;
    private Money fifoCost;
    private Money movingAverageCost;

    /**
     * Converts a {@link AccountTransaction} of type DIVIDENDS into a
     * DividendTransaction.
     * 
     * @exception IllegalArgumentException
     *                when transaction is not of type DIVIDENDS
     */
    public static DividendTransaction from(AccountTransaction t)
    {
        if (t.getType() != Type.DIVIDENDS && t.getType() != Type.INTEREST)
            throw new IllegalArgumentException();

        DividendTransaction dt = new DividendTransaction();
        dt.setDateTime(t.getDateTime());
        dt.setSecurity(t.getSecurity());
        dt.setCurrencyCode(t.getCurrencyCode());
        dt.setAmount(t.getAmount());
        dt.setShares(t.getShares());
        dt.setNote(t.getNote());
        dt.addUnits(t.getUnits());
        return dt;
    }

    public Account getAccount()
    {
        return account;
    }

    /* package */
    void setAccount(Account account)
    {
        this.account = account;
    }

    public long getDividendPerShare()
    {
        return amountFractionPerShare(getGrossValueAmount(), getShares());
    }

    public Money getFifoCost()
    {
        return fifoCost;
    }

    /* package */
    void setFifoCost(Money fifoCost)
    {
        this.fifoCost = fifoCost;
    }

    public Money getMovingAverageCost()
    {
        return movingAverageCost;
    }

    /* package */
    void setMovingAverageCost(Money movingAverageCost)
    {
        this.movingAverageCost = movingAverageCost;
    }

    /* package */
    void setTotalShares(long totalShares)
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

        return getGrossValueAmount() / cost;
    }

    public double getPersonalDividendYieldMovingAverage()
    {
        if (movingAverageCost.getAmount() <= 0)
            return 0;

        double cost = movingAverageCost.getAmount();

        if (getShares() > 0)
            cost = movingAverageCost.getAmount() * (getShares() / (double) totalShares);

        return getGrossValueAmount() / cost;
    }

    static long amountFractionPerShare(long amount, long shares)
    {
        if (shares == 0)
            return 0;

        return Math.round((amount * (Values.AmountFraction.factor() / (double) Values.Amount.factor())
                        * Values.Share.divider()) / (double) shares);
    }

    public long getGrossValueAmount()
    {
        long taxes = getUnits().filter(u -> u.getType() == Unit.Type.TAX)
                        .collect(MoneyCollectors.sum(getCurrencyCode(), Unit::getAmount)).getAmount();

        return getAmount() + taxes;
    }

    public Money getGrossValue()
    {
        return Money.of(getCurrencyCode(), getGrossValueAmount());
    }
}
