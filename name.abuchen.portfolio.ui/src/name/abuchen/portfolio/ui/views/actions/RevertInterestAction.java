package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertInterestAction<T extends Transaction> extends Action
{
    private final Client client;
    private final TransactionPair<T> transaction;

    public RevertInterestAction(Client client, TransactionPair<T> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction instanceof TransactionPair<?> && transaction.getTransaction() instanceof AccountTransaction)
        {
            // we have made sure transaction is of class TransactionPair<AccountTransaction> so the cast is type safe
            @SuppressWarnings("unchecked")
            TransactionPair<AccountTransaction> accountTransaction = (TransactionPair<AccountTransaction>) transaction;  
            if (accountTransaction.getTransaction().getType() != AccountTransaction.Type.INTEREST
                            && accountTransaction.getTransaction().getType() != AccountTransaction.Type.INTEREST_CHARGE)
                throw new IllegalArgumentException();
        }
        else
            throw new IllegalArgumentException();
    }

    @Override
    public void run()
    {
        AccountTransaction accountTransaction = (AccountTransaction) transaction.getTransaction(); 
        
        if (accountTransaction instanceof AccountTransaction)
            if (AccountTransaction.Type.INTEREST.equals(accountTransaction.getType()))
                accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
            else if (AccountTransaction.Type.INTEREST_CHARGE.equals(accountTransaction.getType()))
                accountTransaction.setType(AccountTransaction.Type.INTEREST);
            else
                throw new IllegalArgumentException();
        else
            throw new IllegalArgumentException();

        client.markDirty();
    }
}
