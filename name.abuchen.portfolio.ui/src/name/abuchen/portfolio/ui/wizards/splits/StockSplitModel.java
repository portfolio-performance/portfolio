package name.abuchen.portfolio.ui.wizards.splits;

import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class StockSplitModel extends BindingHelper.Model
{
    private Security security;
    private LocalDate exDate = LocalDate.now();
    private int newShares = 1;
    private int oldShares = 1;

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

    public int getNewShares()
    {
        return newShares;
    }

    public void setNewShares(int newShares)
    {
        firePropertyChange("newShares", this.newShares, this.newShares = newShares); //$NON-NLS-1$
    }

    public int getOldShares()
    {
        return oldShares;
    }

    public void setOldShares(int oldShares)
    {
        firePropertyChange("oldShares", this.oldShares, this.oldShares = oldShares); //$NON-NLS-1$
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

    @Override
    public void applyChanges()
    {
        SecurityEvent event = new SecurityEvent(exDate, SecurityEvent.Type.STOCK_SPLIT, newShares + ":" + oldShares); //$NON-NLS-1$
        security.addEvent(event);

        if (isChangeTransactions())
        {
            List<TransactionPair<?>> transactions = security.getTransactions(getClient());
            for (TransactionPair<?> pair : transactions)
            {
                Transaction t = pair.getTransaction();
                if (t.getDate().isBefore(exDate))
                    t.setShares(t.getShares() * newShares / oldShares);
            }
        }

        if (isChangeHistoricalQuotes())
        {
            List<SecurityPrice> quotes = security.getPrices();
            for (SecurityPrice p : quotes)
            {
                if (p.getDate().isBefore(exDate))
                    p.setValue(p.getValue() * oldShares / newShares);
            }
        }

    }
}
