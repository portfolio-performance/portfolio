package name.abuchen.portfolio.model;

import java.util.Date;

public class PortfolioTransferEntry implements CrossEntry
{
    private Portfolio portfolioFrom;
    private PortfolioTransaction transactionFrom;
    private Portfolio portfolioTo;
    private PortfolioTransaction transactionTo;

    public PortfolioTransferEntry(Portfolio accountFrom, Portfolio accountTo)
    {
        this.portfolioFrom = accountFrom;
        this.transactionFrom = new PortfolioTransaction();
        this.transactionFrom.setType(PortfolioTransaction.Type.TRANSFER_OUT);
        this.transactionFrom.setCrossEntry(this);

        this.portfolioTo = accountTo;
        this.transactionTo = new PortfolioTransaction();
        this.transactionTo.setType(PortfolioTransaction.Type.TRANSFER_IN);
        this.transactionTo.setCrossEntry(this);
    }

    public void setDate(Date date)
    {
        this.transactionFrom.setDate(date);
        this.transactionTo.setDate(date);
    }

    public void setSecurity(Security security)
    {
        this.transactionFrom.setSecurity(security);
        this.transactionTo.setSecurity(security);
    }

    public void setShares(long shares)
    {
        this.transactionFrom.setShares(shares);
        this.transactionTo.setShares(shares);
    }

    public void setAmount(long amount)
    {
        this.transactionFrom.setAmount(amount);
        this.transactionTo.setAmount(amount);
    }

    public void insert()
    {
        portfolioFrom.addTransaction(transactionFrom);
        portfolioTo.addTransaction(transactionTo);
    }

    @Override
    public void delete()
    {
        portfolioFrom.getTransactions().remove(transactionFrom);
        portfolioTo.getTransactions().remove(transactionTo);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        // fees are not supported for transfers
        // -> needs separate transaction on account
        transactionTo.setFees(0);
        transactionFrom.setFees(0);

        if (t.equals(transactionFrom))
        {
            transactionTo.setDate(transactionFrom.getDate());
            transactionTo.setSecurity(transactionFrom.getSecurity());
            transactionTo.setShares(transactionFrom.getShares());
            transactionTo.setAmount(transactionFrom.getAmount());
        }
        else if (t.equals(transactionTo))
        {
            transactionFrom.setDate(transactionTo.getDate());
            transactionFrom.setSecurity(transactionTo.getSecurity());
            transactionFrom.setShares(transactionTo.getShares());
            transactionFrom.setAmount(transactionTo.getAmount());
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

}
