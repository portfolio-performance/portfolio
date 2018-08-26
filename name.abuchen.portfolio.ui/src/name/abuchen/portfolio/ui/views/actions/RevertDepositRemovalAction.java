package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertDepositRemovalAction extends Action
{
    private final Client client;
    private final TransactionPair<AccountTransaction> transaction;

    public RevertDepositRemovalAction(Client client, TransactionPair<AccountTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction().getType() != AccountTransaction.Type.DEPOSIT
                        && transaction.getTransaction().getType() != AccountTransaction.Type.REMOVAL)
            throw new IllegalArgumentException();
    }

    @Override
    public void run()
    {
        AccountTransaction accountTransaction = transaction.getTransaction();

        if (AccountTransaction.Type.DEPOSIT.equals(accountTransaction.getType()))
            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
        else if (AccountTransaction.Type.REMOVAL.equals(accountTransaction.getType()))
            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
        else
            throw new IllegalArgumentException();

        client.markDirty();
    }
}
