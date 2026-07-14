package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.money.Money;

/**
 * A tax-neutral fund transfer links two portfolio transactions with different
 * securities. The transaction amounts describe the market value at transfer
 * time, while carried lots preserve the original acquisition basis for later
 * capital-gains calculations.
 */
public class FundTransferEntry implements CrossEntry, Annotated
{
    public static class CarriedLot
    {
        private LocalDate acquisitionDate;
        private long sourceShares;
        private long targetShares;
        private Money acquisitionValue;
        private String sourceTransactionUUID;

        public CarriedLot()
        {
            // needed for xstream de-serialization
        }

        public CarriedLot(LocalDate acquisitionDate, long sourceShares, long targetShares, Money acquisitionValue,
                        String sourceTransactionUUID)
        {
            this.acquisitionDate = acquisitionDate;
            this.sourceShares = sourceShares;
            this.targetShares = targetShares;
            this.acquisitionValue = acquisitionValue;
            this.sourceTransactionUUID = sourceTransactionUUID;
        }

        /**
         * Creates an independent copy for editing in transaction dialogs.
         */
        public CarriedLot copy()
        {
            return new CarriedLot(acquisitionDate, sourceShares, targetShares, acquisitionValue,
                            sourceTransactionUUID);
        }

        public LocalDate getAcquisitionDate()
        {
            return acquisitionDate;
        }

        public void setAcquisitionDate(LocalDate acquisitionDate)
        {
            this.acquisitionDate = acquisitionDate;
        }

        public long getSourceShares()
        {
            return sourceShares;
        }

        public void setSourceShares(long sourceShares)
        {
            this.sourceShares = sourceShares;
        }

        public long getTargetShares()
        {
            return targetShares;
        }

        public void setTargetShares(long targetShares)
        {
            this.targetShares = targetShares;
        }

        public Money getAcquisitionValue()
        {
            return acquisitionValue;
        }

        public void setAcquisitionValue(Money acquisitionValue)
        {
            this.acquisitionValue = acquisitionValue;
        }

        public String getSourceTransactionUUID()
        {
            return sourceTransactionUUID;
        }

        public void setSourceTransactionUUID(String sourceTransactionUUID)
        {
            this.sourceTransactionUUID = sourceTransactionUUID;
        }
    }

    private Portfolio sourcePortfolio;
    private PortfolioTransaction sourceTransaction;
    private Portfolio targetPortfolio;
    private PortfolioTransaction targetTransaction;
    private List<CarriedLot> carriedLots = new ArrayList<>();

    public FundTransferEntry()
    {
        this(null, new PortfolioTransaction(), null, new PortfolioTransaction());
    }

    public FundTransferEntry(Portfolio sourcePortfolio, Portfolio targetPortfolio)
    {
        this(sourcePortfolio, new PortfolioTransaction(), targetPortfolio, new PortfolioTransaction());
    }

    /* protobuf only */ FundTransferEntry(Portfolio sourcePortfolio, PortfolioTransaction sourceTransaction,
                    Portfolio targetPortfolio, PortfolioTransaction targetTransaction)
    {
        this.sourcePortfolio = sourcePortfolio;
        setSourceTransaction(sourceTransaction);

        this.targetPortfolio = targetPortfolio;
        setTargetTransaction(targetTransaction);
    }

    public void setSourceTransaction(PortfolioTransaction sourceTransaction)
    {
        this.sourceTransaction = sourceTransaction;
        this.sourceTransaction.setType(PortfolioTransaction.Type.FUND_TRANSFER_OUT);
        this.sourceTransaction.setCrossEntry(this);
    }

    public PortfolioTransaction getSourceTransaction()
    {
        return sourceTransaction;
    }

    public void setTargetTransaction(PortfolioTransaction targetTransaction)
    {
        this.targetTransaction = targetTransaction;
        this.targetTransaction.setType(PortfolioTransaction.Type.FUND_TRANSFER_IN);
        this.targetTransaction.setCrossEntry(this);
    }

    public PortfolioTransaction getTargetTransaction()
    {
        return targetTransaction;
    }

    public void setSourcePortfolio(Portfolio sourcePortfolio)
    {
        this.sourcePortfolio = sourcePortfolio;
    }

    public Portfolio getSourcePortfolio()
    {
        return sourcePortfolio;
    }

    public void setTargetPortfolio(Portfolio targetPortfolio)
    {
        this.targetPortfolio = targetPortfolio;
    }

    public Portfolio getTargetPortfolio()
    {
        return targetPortfolio;
    }

    public void setDate(LocalDateTime date)
    {
        this.sourceTransaction.setDateTime(date);
        this.targetTransaction.setDateTime(date);
    }

    public void setSourceSecurity(Security security)
    {
        this.sourceTransaction.setSecurity(security);
    }

    public void setTargetSecurity(Security security)
    {
        this.targetTransaction.setSecurity(security);
    }

    public void setSourceShares(long shares)
    {
        this.sourceTransaction.setShares(shares);
    }

    public void setTargetShares(long shares)
    {
        this.targetTransaction.setShares(shares);
    }

    public void setSourceAmount(long amount)
    {
        this.sourceTransaction.setAmount(amount);
    }

    public void setTargetAmount(long amount)
    {
        this.targetTransaction.setAmount(amount);
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.sourceTransaction.setCurrencyCode(currencyCode);
        this.targetTransaction.setCurrencyCode(currencyCode);
    }

    public void setSourceMonetaryAmount(Money amount)
    {
        this.sourceTransaction.setMonetaryAmount(amount);
    }

    public void setTargetMonetaryAmount(Money amount)
    {
        this.targetTransaction.setMonetaryAmount(amount);
    }

    public List<CarriedLot> getCarriedLots()
    {
        return carriedLots;
    }

    public void setCarriedLots(List<CarriedLot> carriedLots)
    {
        this.carriedLots = new ArrayList<>(carriedLots);
    }

    public void addCarriedLot(CarriedLot lot)
    {
        this.carriedLots.add(lot);
    }

    /**
     * Returns whether both sides of this transfer belong to the given client.
     * Filtered clients can intentionally contain only one side; that visible
     * leg is an external flow for performance reporting while still carrying
     * its original acquisition lots.
     */
    public boolean isInternalTo(Client client)
    {
        return client.getPortfolios().contains(sourcePortfolio) && client.getPortfolios().contains(targetPortfolio);
    }

    @Override
    public String getNote()
    {
        return this.sourceTransaction.getNote();
    }

    @Override
    public void setNote(String note)
    {
        this.sourceTransaction.setNote(note);
        this.targetTransaction.setNote(note);
    }

    @Override
    public String getSource()
    {
        return this.sourceTransaction.getSource();
    }

    @Override
    public void setSource(String source)
    {
        this.sourceTransaction.setSource(source);
        this.targetTransaction.setSource(source);
    }

    @Override
    public void insert()
    {
        sourcePortfolio.addTransaction(sourceTransaction);
        targetPortfolio.addTransaction(targetTransaction);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t.equals(sourceTransaction))
            copySharedAttributesOver(sourceTransaction, targetTransaction);
        else if (t.equals(targetTransaction))
            copySharedAttributesOver(targetTransaction, sourceTransaction);
        else
            throw new UnsupportedOperationException("unable to update from transaction " + t); //$NON-NLS-1$
    }

    private void copySharedAttributesOver(PortfolioTransaction source, PortfolioTransaction target)
    {
        // Security, shares, and transfer amount are intentionally asymmetric:
        // only metadata common to both sides is mirrored during cross editing.
        target.setDateTime(source.getDateTime());
        target.setNote(source.getNote());
        target.setSource(source.getSource());
    }

    @Override
    public TransactionOwner<? extends Transaction> getOwner(Transaction t)
    {
        if (t.equals(sourceTransaction))
            return sourcePortfolio;
        else if (t.equals(targetTransaction))
            return targetPortfolio;
        else
            throw new UnsupportedOperationException("unable to get owner of transaction " + t); //$NON-NLS-1$
    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        if (!(owner instanceof Portfolio))
            throw new IllegalArgumentException(
                            "invalid owner type for owner " + owner + " when trying to set it to transaction " + t); //$NON-NLS-1$ //$NON-NLS-2$

        if (t.equals(sourceTransaction) && !targetPortfolio.equals(owner))
            sourcePortfolio = (Portfolio) owner;
        else if (t.equals(targetTransaction) && !sourcePortfolio.equals(owner))
            targetPortfolio = (Portfolio) owner;
        else
            throw new IllegalArgumentException("unable to set owner " + owner + " to transaction " + t); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(sourceTransaction))
            return targetTransaction;
        else if (t.equals(targetTransaction))
            return sourceTransaction;
        else
            throw new UnsupportedOperationException("unable to get cross transaction for transaction " + t); //$NON-NLS-1$
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        if (t.equals(sourceTransaction))
            return targetPortfolio;
        else if (t.equals(targetTransaction))
            return sourcePortfolio;
        else
            throw new UnsupportedOperationException("unable to get cross owner for transaction " + t); //$NON-NLS-1$
    }
}
