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
        this(null, new PortfolioTransaction(), null, new PortfolioTransaction());
    }

    public PortfolioTransferEntry(Portfolio portfolioFrom, Portfolio portfolioTo)
    {
        this(portfolioFrom, new PortfolioTransaction(), portfolioTo, new PortfolioTransaction());
    }

    /* protobuf only */ PortfolioTransferEntry(Portfolio portfolioFrom, PortfolioTransaction txFrom,
                    Portfolio portfolioTo, PortfolioTransaction txTo)
    {
        this.transactionFrom = txFrom;
        this.transactionFrom.setType(PortfolioTransaction.Type.TRANSFER_OUT);
        this.transactionFrom.setCrossEntry(this);

        this.transactionTo = txTo;
        this.transactionTo.setType(PortfolioTransaction.Type.TRANSFER_IN);
        this.transactionTo.setCrossEntry(this);

        this.portfolioFrom = portfolioFrom;
        this.portfolioTo = portfolioTo;
    }

    public void setSourceTransaction(PortfolioTransaction transaction)
    {
        this.transactionFrom = transaction;
    }

    public void setTargetTransaction(PortfolioTransaction transaction)
    {
        this.transactionTo = transaction;
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

    public Portfolio getSourcePortfolio()
    {
        return this.portfolioFrom;
    }

    public Portfolio getTargetPortfolio()
    {
        return this.portfolioTo;
    }

    public void setDate(LocalDateTime date)
    {
        this.transactionFrom.setDateTime(date);
        this.transactionTo.setDateTime(date);
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

    @Override
    public String getSource()
    {
        return this.transactionFrom.getSource();
    }

    @Override
    public void setSource(String source)
    {
        this.transactionFrom.setSource(source);
        this.transactionTo.setSource(source);
    }

    @Override
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
        target.setDateTime(source.getDateTime());
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
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        if (!(owner instanceof Portfolio))
            throw new IllegalArgumentException();

        if (t.equals(transactionFrom) && !portfolioTo.equals(owner))
            portfolioFrom = (Portfolio) owner;
        else if (t.equals(transactionTo) && !portfolioFrom.equals(owner))
            portfolioTo = (Portfolio) owner;
        else
            throw new IllegalArgumentException();
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
