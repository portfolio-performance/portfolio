package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertInterestAction extends Action
{
    private final Client client;
    private final TransactionPair<AccountTransaction> transaction;

    public RevertInterestAction(Client client, TransactionPair<AccountTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction().getType() != AccountTransaction.Type.INTEREST
                        && transaction.getTransaction().getType() != AccountTransaction.Type.INTEREST_CHARGE)
            throw new IllegalArgumentException();
    }

    @Override
    public void run()
    {
        AccountTransaction accountTransaction = transaction.getTransaction();

        if (AccountTransaction.Type.INTEREST.equals(accountTransaction.getType()))
            accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
        else if (AccountTransaction.Type.INTEREST_CHARGE.equals(accountTransaction.getType()))
            accountTransaction.setType(AccountTransaction.Type.INTEREST);
        else
            throw new IllegalArgumentException();

        client.markDirty();
    }
}
