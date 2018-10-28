package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertAccountAction extends Action
{
    private final Client client;
    private final TransactionPair<AccountTransaction> transaction;

    public RevertAccountAction(Client client, TransactionPair<AccountTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction().getType() != AccountTransaction.Type.DEPOSIT
                        && transaction.getTransaction().getType() != AccountTransaction.Type.DIVIDENDS
                        && transaction.getTransaction().getType() != AccountTransaction.Type.FEES
                        && transaction.getTransaction().getType() != AccountTransaction.Type.FEES_REFUND
                        && transaction.getTransaction().getType() != AccountTransaction.Type.INTEREST
                        && transaction.getTransaction().getType() != AccountTransaction.Type.INTEREST_CHARGE
                        && transaction.getTransaction().getType() != AccountTransaction.Type.REMOVAL
                        && transaction.getTransaction().getType() != AccountTransaction.Type.TAXES
                        && transaction.getTransaction().getType() != AccountTransaction.Type.TAX_REFUND
                        )
            throw new IllegalArgumentException();
    }

    @Override
    public void run()
    {
        AccountTransaction accountTransaction = transaction.getTransaction();

        accountTransaction.setType(accountTransaction.getType().getSibling());

        client.markDirty();
    }
}
