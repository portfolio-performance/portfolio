package name.abuchen.portfolio.model;

import java.time.LocalDateTime;

public class PortfolioTransferEntry implements CrossEntry, Annotated
{
    private Portfolio portfolioFrom;
    private PortfolioTransaction transactionFrom;
    private Portfolio portfolioTo;
    private PortfolioTransaction transactionTo;
    private boolean readOnly;

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
        this(portfolioFrom, txFrom, portfolioTo, txTo, false);
    }

    private PortfolioTransferEntry(Portfolio portfolioFrom, PortfolioTransaction txFrom,
                    Portfolio portfolioTo, PortfolioTransaction txTo, boolean readOnly)
    {
        this.transactionFrom = txFrom;
        this.transactionTo = txTo;
        this.portfolioFrom = portfolioFrom;
        this.portfolioTo = portfolioTo;
        this.readOnly = readOnly;

        if (!readOnly)
        {
            this.transactionFrom.setType(PortfolioTransaction.Type.TRANSFER_OUT);
            this.transactionFrom.setCrossEntry(this);

            this.transactionTo.setType(PortfolioTransaction.Type.TRANSFER_IN);
            this.transactionTo.setCrossEntry(this);
        }
    }

    public static PortfolioTransferEntry readOnly(Portfolio portfolioFrom, PortfolioTransaction txFrom,
                    Portfolio portfolioTo, PortfolioTransaction txTo)
    {
        return new PortfolioTransferEntry(portfolioFrom, txFrom, portfolioTo, txTo, true);
    }

    public void setSourceTransaction(PortfolioTransaction transaction)
    {
        assertWritable();

        this.transactionFrom = transaction;
    }

    public void setTargetTransaction(PortfolioTransaction transaction)
    {
        assertWritable();

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
        assertWritable();

        this.portfolioFrom = portfolio;
    }

    public void setTargetPortfolio(Portfolio portfolio)
    {
        assertWritable();

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
        assertWritable();

        this.transactionFrom.setDateTime(date);
        this.transactionTo.setDateTime(date);
    }

    public void setSecurity(Security security)
    {
        assertWritable();

        this.transactionFrom.setSecurity(security);
        this.transactionTo.setSecurity(security);
    }

    public void setShares(long shares)
    {
        assertWritable();

        this.transactionFrom.setShares(shares);
        this.transactionTo.setShares(shares);
    }

    public void setAmount(long amount)
    {
        assertWritable();

        this.transactionFrom.setAmount(amount);
        this.transactionTo.setAmount(amount);
    }

    public void setCurrencyCode(String currencyCode)
    {
        assertWritable();

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
        assertWritable();

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
        assertWritable();

        this.transactionFrom.setSource(source);
        this.transactionTo.setSource(source);
    }

    @Override
    public void insert()
    {
        assertWritable();

        portfolioFrom.addTransaction(transactionFrom);
        portfolioTo.addTransaction(transactionTo);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t.equals(transactionFrom))
        {
            if (readOnly)
                return;

            copyAttributesOver(transactionFrom, transactionTo);
        }
        else if (t.equals(transactionTo))
        {
            if (readOnly)
                return;

            copyAttributesOver(transactionTo, transactionFrom);
        }
        else
            throw new UnsupportedOperationException("unable to update from transaction " + t); //$NON-NLS-1$
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
            throw new UnsupportedOperationException("unable to get owner of transaction " + t); //$NON-NLS-1$
    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        assertWritable();

        if (!(owner instanceof Portfolio))
            throw new IllegalArgumentException(
                            "invalid owner type for owner " + owner + " when trying to set it to transaction " + t); //$NON-NLS-1$ //$NON-NLS-2$

        if (t.equals(transactionFrom) && !portfolioTo.equals(owner))
            portfolioFrom = (Portfolio) owner;
        else if (t.equals(transactionTo) && !portfolioFrom.equals(owner))
            portfolioTo = (Portfolio) owner;
        else
            throw new IllegalArgumentException("unable to set owner " + owner + " to transaction " + t); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(transactionFrom))
            return transactionTo;
        else if (t.equals(transactionTo))
            return transactionFrom;
        else
            throw new UnsupportedOperationException("unable to get cross transaction for transaction " + t); //$NON-NLS-1$
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        if (t.equals(transactionFrom))
            return portfolioTo;
        else if (t.equals(transactionTo))
            return portfolioFrom;
        else
            throw new UnsupportedOperationException("unable to get cross owner for transaction " + t); //$NON-NLS-1$
    }

    private void assertWritable()
    {
        if (readOnly)
            throw new UnsupportedOperationException("Ledger-backed portfolio transfer cross entries are read-only"); //$NON-NLS-1$
    }
}
