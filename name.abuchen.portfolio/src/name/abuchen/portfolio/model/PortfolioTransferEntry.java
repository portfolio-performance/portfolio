package name.abuchen.portfolio.model;

import java.time.LocalDateTime;

public class PortfolioTransferEntry implements CrossEntry, Annotated
{
    private Portfolio portfolioFrom;
    private PortfolioTransaction transactionFrom;
    private Portfolio portfolioTo;
    private PortfolioTransaction transactionTo;

    public PortfolioTransferEntry()
    {
        this.transactionFrom = new PortfolioTransaction();
        this.transactionFrom.setType(PortfolioTransaction.Type.TRANSFER_OUT);
        this.transactionFrom.setCrossEntry(this);

        this.transactionTo = new PortfolioTransaction();
        this.transactionTo.setType(PortfolioTransaction.Type.TRANSFER_IN);
        this.transactionTo.setCrossEntry(this);
    }

    public PortfolioTransferEntry(Portfolio portfolioFrom, Portfolio portfolioTo)
    {
        this();
        this.portfolioFrom = portfolioFrom;
        this.portfolioTo = portfolioTo;
    }

    public PortfolioTransaction getSourceTransaction()
    {
        return this.transactionFrom;
    }

    public PortfolioTransaction getTargetTransaction()
    {
        return this.transactionTo;
    }

    public void setSourcePortfolio(Portfolio portfolio)
    {
        this.portfolioFrom = portfolio;
    }

    public void setTargetPortfolio(Portfolio portfolio)
    {
        this.portfolioTo = portfolio;
    }

    public void setDate(LocalDateTime date)
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

    public void setCurrencyCode(String currencyCode)
    {
        this.transactionFrom.setCurrencyCode(currencyCode);
        this.transactionTo.setCurrencyCode(currencyCode);
    }

    @Override
    public String getNote()
    {
        return this.transactionFrom.getNote();
    }

    @Override
    public void setNote(String note)
    {
        this.transactionFrom.setNote(note);
        this.transactionTo.setNote(note);
    }

    public void insert()
    {
        portfolioFrom.addTransaction(transactionFrom);
        portfolioTo.addTransaction(transactionTo);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t.equals(transactionFrom))
            copyAttributesOver(transactionFrom, transactionTo);
        else if (t.equals(transactionTo))
            copyAttributesOver(transactionTo, transactionFrom);
        else
            throw new UnsupportedOperationException();
    }

    private void copyAttributesOver(PortfolioTransaction source, PortfolioTransaction target)
    {
        target.setDate(source.getDateTime());
        target.setSecurity(source.getSecurity());
        target.setShares(source.getShares());
        target.setNote(source.getNote());
    }

    @Override
    public TransactionOwner<? extends Transaction> getOwner(Transaction t)
    {
        if (t.equals(transactionFrom))
            return portfolioFrom;
        else if (t.equals(transactionTo))
            return portfolioTo;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(transactionFrom))
            return transactionTo;
        else if (t.equals(transactionTo))
            return transactionFrom;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        if (t.equals(transactionFrom))
            return portfolioTo;
        else if (t.equals(transactionTo))
            return portfolioFrom;
        else
            throw new UnsupportedOperationException();
    }
}
