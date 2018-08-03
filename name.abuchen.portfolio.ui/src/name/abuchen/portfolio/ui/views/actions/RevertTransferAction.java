package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertTransferAction<T extends Transaction> extends Action
{
    private final Client client;
    private final TransactionPair<T> transaction;

    public RevertTransferAction(Client client, TransactionPair<T> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction instanceof TransactionPair<?>)
        {        
            if (transaction.getTransaction() instanceof PortfolioTransaction)
            {            
                // we have made sure transaction is of class TransactionPair<PortfolioTransaction> so the cast is type safe
                @SuppressWarnings("unchecked")
                TransactionPair<PortfolioTransaction> portfolioTransaction = (TransactionPair<PortfolioTransaction>) transaction;
                if (portfolioTransaction.getTransaction().getType() != PortfolioTransaction.Type.TRANSFER_IN
                        && portfolioTransaction.getTransaction().getType() != PortfolioTransaction.Type.TRANSFER_OUT)
                    throw new IllegalArgumentException();
            }
            else if (transaction.getTransaction() instanceof AccountTransaction)
            {
                // we have made sure transaction is of class TransactionPair<AccountTransaction> so the cast is type safe
                @SuppressWarnings("unchecked")
                TransactionPair<AccountTransaction> accountTransaction = (TransactionPair<AccountTransaction>) transaction;
                if (accountTransaction.getTransaction().getType() != AccountTransaction.Type.TRANSFER_IN
                        && accountTransaction.getTransaction().getType() != AccountTransaction.Type.TRANSFER_OUT)
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
        if (transaction.getTransaction().getCrossEntry() instanceof PortfolioTransferEntry)
        {        
            PortfolioTransferEntry entry = (PortfolioTransferEntry) transaction.getTransaction().getCrossEntry(); 
            Portfolio temp = (Portfolio) entry.getSourcePortfolio();
            entry.setSourcePortfolio((Portfolio) entry.getTargetPortfolio());
            entry.setTargetPortfolio(temp);
            entry.getSourceTransaction().setType(PortfolioTransaction.Type.TRANSFER_IN);
            entry.getTargetTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
            PortfolioTransaction transactionTemp = entry.getSourceTransaction();
            entry.setSourceTransaction(entry.getTargetTransaction());
            entry.setTargetTransaction(transactionTemp);
        }
        else if (transaction.getTransaction().getCrossEntry() instanceof AccountTransferEntry)
        {        
            AccountTransferEntry entry = (AccountTransferEntry) transaction.getTransaction().getCrossEntry(); 
            Account temp = (Account) entry.getSourceAccount();
            entry.setSourceAccount((Account) entry.getTargetAccount());
            entry.setTargetAccount(temp);
            entry.getSourceTransaction().setType(AccountTransaction.Type.TRANSFER_IN);
            entry.getTargetTransaction().setType(AccountTransaction.Type.TRANSFER_OUT);
            AccountTransaction transactionTemp = entry.getSourceTransaction();
            entry.setSourceTransaction(entry.getTargetTransaction());
            entry.setTargetTransaction(transactionTemp);
        }
        else
            throw new IllegalArgumentException();

        client.markDirty();
    }
}
