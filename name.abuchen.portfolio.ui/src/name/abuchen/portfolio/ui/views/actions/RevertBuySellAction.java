package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertBuySellAction<T extends Transaction> extends Action
{
    private final Client client;
    private final TransactionPair<T> transaction;

    public RevertBuySellAction(Client client, TransactionPair<T> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        
//        if (transaction instanceof TransactionPair<?> && transaction.getTransaction() instanceof AccountTransaction)
//        {
//            // we have made sure transaction is of class TransactionPair<AccountTransaction> so the cast is type safe
//            @SuppressWarnings("unchecked")
//            TransactionPair<AccountTransaction> accountTransaction = (TransactionPair<AccountTransaction>) transaction;  
//        
        
        
        if (transaction instanceof TransactionPair<?>)
        {        
            if (transaction.getTransaction() instanceof PortfolioTransaction)
            {            
                // we have made sure transaction is of class TransactionPair<PortfolioTransaction> so the cast is type safe
                @SuppressWarnings("unchecked")
                TransactionPair<PortfolioTransaction> portfolioTransaction = (TransactionPair<PortfolioTransaction>) transaction;
                if (portfolioTransaction.getTransaction().getType() != PortfolioTransaction.Type.BUY
                            && portfolioTransaction.getTransaction().getType() != PortfolioTransaction.Type.SELL)
                    throw new IllegalArgumentException();
            }
            else if (transaction.getTransaction() instanceof AccountTransaction)
            {
                // we have made sure transaction is of class TransactionPair<AccountTransaction> so the cast is type safe
                @SuppressWarnings("unchecked")
                TransactionPair<AccountTransaction> accountTransaction = (TransactionPair<AccountTransaction>) transaction;
                if (accountTransaction.getTransaction().getType() != AccountTransaction.Type.BUY
                            && accountTransaction.getTransaction().getType() != AccountTransaction.Type.SELL)
                    throw new IllegalArgumentException();
            } 
            else
                throw new IllegalArgumentException();
        }
        else
            throw new IllegalArgumentException();
    }

    @Override
    public void run()
    {
        BuySellEntry buysell = (BuySellEntry) transaction.getTransaction().getCrossEntry();
        if (transaction.getTransaction() instanceof PortfolioTransaction)
        {        
            if (PortfolioTransaction.Type.BUY.equals(((PortfolioTransaction) transaction.getTransaction()).getType()))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.SELL);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.SELL);
            }
            else if (PortfolioTransaction.Type.SELL.equals(((PortfolioTransaction) transaction.getTransaction()).getType()))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.BUY);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.BUY);
            }
            else
                throw new IllegalArgumentException();
        }
        else if (transaction.getTransaction() instanceof AccountTransaction)
        {        
            if (AccountTransaction.Type.BUY.equals(((AccountTransaction) transaction.getTransaction()).getType()))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.SELL);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.SELL);
            }
            else if (AccountTransaction.Type.SELL.equals(((AccountTransaction) transaction.getTransaction()).getType()))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.BUY);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.BUY);
            }
            else
                throw new IllegalArgumentException();
        }
        else
            throw new IllegalArgumentException();

        client.markDirty();
    }
}
