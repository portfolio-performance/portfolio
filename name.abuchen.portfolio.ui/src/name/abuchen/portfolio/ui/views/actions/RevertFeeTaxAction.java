package name.abuchen.portfolio.ui.views.actions;


import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerAccountTypeToggleConverter;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertFeeTaxAction extends Action
{
    private final Client client;
    private final TransactionPair<AccountTransaction> transaction;

    public RevertFeeTaxAction(Client client, TransactionPair<AccountTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        AccountTransaction tx = transaction.getTransaction();
        Type type = tx.getType();
        if (type != Type.FEES && type != Type.FEES_REFUND && type != Type.TAXES && type != Type.TAX_REFUND)
            throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void run()
    {
        AccountTransaction tx = transaction.getTransaction();
        var converter = new LedgerAccountTypeToggleConverter(client);

        if (converter.canToggle(transaction))
        {
            converter.toggle(transaction);
            client.markDirty();
            return;
        }

        Type type = tx.getType();
        if (type == Type.FEES)
            tx.setType(Type.FEES_REFUND);
        else if (type == Type.FEES_REFUND)
            tx.setType(Type.FEES);
        else if (type == Type.TAXES)
            tx.setType(Type.TAX_REFUND);
        else if (type == Type.TAX_REFUND)
            tx.setType(Type.TAXES);
        else
            throw new IllegalArgumentException(
                            "unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$

        client.markDirty();
    }
}
