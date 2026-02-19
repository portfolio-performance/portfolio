package name.abuchen.portfolio.ui.wizards.splits;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class StockSplitModel extends BindingHelper.Model
{
    private Security security;
    private LocalDate exDate = LocalDate.now();
    private BigDecimal newShares = BigDecimal.ONE;
    private BigDecimal oldShares = BigDecimal.ONE;
    private BigDecimal stockMultiplier = BigDecimal.ONE;

    private boolean changeTransactions = true;
    private boolean changeHistoricalQuotes = true;

    public StockSplitModel(Client client, Security security)
    {
        super(client);

        this.security = security;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
    }

    public LocalDate getExDate()
    {
        return exDate;
    }

    public void setExDate(LocalDate exDate)
    {
        firePropertyChange("exDate", this.exDate, this.exDate = exDate); //$NON-NLS-1$
    }

    public BigDecimal getNewShares()
    {
        return newShares;
    }

    public void setNewShares(BigDecimal newShares)
    {
        firePropertyChange("newShares", this.newShares, this.newShares = newShares); //$NON-NLS-1$
        calculateStockMultiplier();
    }

    public BigDecimal getOldShares()
    {
        return oldShares;
    }

    public void setOldShares(BigDecimal oldShares)
    {
        firePropertyChange("oldShares", this.oldShares, this.oldShares = oldShares); //$NON-NLS-1$
        calculateStockMultiplier();
    }

    public boolean isChangeTransactions()
    {
        return changeTransactions;
    }

    public void setChangeTransactions(boolean changeTransactions)
    {
        firePropertyChange("changeTransactions", this.changeTransactions, //$NON-NLS-1$
                        this.changeTransactions = changeTransactions);
    }

    public boolean isChangeHistoricalQuotes()
    {
        return changeHistoricalQuotes;
    }

    public void setChangeHistoricalQuotes(boolean changeHistoricalQuotes)
    {
        firePropertyChange("changeHistoricalQuotes", this.changeHistoricalQuotes, //$NON-NLS-1$
                        this.changeHistoricalQuotes = changeHistoricalQuotes);
    }

    private void calculateStockMultiplier()
    {
        stockMultiplier =  newShares.divide(oldShares, Values.MC);
    }
    
    public long calculateNewStock(long oldStock)
    {
        return BigDecimal.valueOf(oldStock).multiply(stockMultiplier)
                        .setScale(0, RoundingMode.HALF_EVEN).longValue();
    }
    
    public long calculateNewQuote(long oldQuote)
    {
        return BigDecimal.valueOf(oldQuote).divide(stockMultiplier, Values.MC) // when stock is multiplied, quote must be divided
                        .setScale(0, RoundingMode.HALF_EVEN).longValue();
    }
    
    @Override
    public void applyChanges()
    {
        // save stock split ratio as technical values (and hence do not format
        // in the local of user) in order to restore/retrieve ratio later
        SecurityEvent event = new SecurityEvent(exDate, SecurityEvent.Type.STOCK_SPLIT, newShares + ":" + oldShares); //$NON-NLS-1$
        security.addEvent(event);

        if (isChangeTransactions())
        {
            List<TransactionPair<?>> transactions = security.getTransactions(getClient());
            for (TransactionPair<?> pair : transactions)
            {
                Transaction t = pair.getTransaction();
                if (t.getDateTime().toLocalDate().isBefore(exDate))
                    t.setShares(calculateNewStock(t.getShares()));
            }
        }

        if (isChangeHistoricalQuotes())
        {
            List<SecurityPrice> quotes = security.getPrices();
            for (SecurityPrice p : quotes)
            {
                if (p.getDate().isBefore(exDate))
                    p.setValue(calculateNewQuote(p.getValue()));
            }
        }

    }
}
