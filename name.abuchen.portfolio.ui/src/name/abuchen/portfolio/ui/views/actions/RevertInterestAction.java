package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
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

        AccountTransaction tx = transaction.getTransaction();
        Type type = tx.getType();
        if (type != AccountTransaction.Type.INTEREST
                        && type != AccountTransaction.Type.INTEREST_CHARGE)
            throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void run()
    {
        AccountTransaction tx = transaction.getTransaction();

        Type type = tx.getType();
        if (AccountTransaction.Type.INTEREST.equals(type))
            tx.setType(AccountTransaction.Type.INTEREST_CHARGE);
        else if (AccountTransaction.Type.INTEREST_CHARGE.equals(type))
            tx.setType(AccountTransaction.Type.INTEREST);
        else
            throw new IllegalArgumentException(
                            "unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$

        client.markDirty();
    }
}
