package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertTransferAction extends Action
{
    private final Client client;
    private final TransactionPair<?> transaction;

    public RevertTransferAction(Client client, TransactionPair<?> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        Transaction tx = transaction.getTransaction();
        if (tx instanceof PortfolioTransaction)
        {
            PortfolioTransaction.Type type = ((PortfolioTransaction) transaction.getTransaction()).getType();

            if (type != PortfolioTransaction.Type.TRANSFER_IN && type != PortfolioTransaction.Type.TRANSFER_OUT)
                throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (tx instanceof AccountTransaction)
        {
            AccountTransaction.Type type = ((AccountTransaction) transaction.getTransaction()).getType();

            if (type != AccountTransaction.Type.TRANSFER_IN && type != AccountTransaction.Type.TRANSFER_OUT)
                throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            throw new IllegalArgumentException("unsupported transaction " + tx); //$NON-NLS-1$
        }
    }

    @Override
    public void run()
    {
        Transaction tx = transaction.getTransaction();
        CrossEntry e = tx.getCrossEntry();
        if (e instanceof PortfolioTransferEntry)
        {
            PortfolioTransferEntry entry = (PortfolioTransferEntry) transaction.getTransaction().getCrossEntry();

            Portfolio oldSource = entry.getSourcePortfolio();
            entry.setSourcePortfolio((Portfolio) entry.getTargetPortfolio());
            entry.setTargetPortfolio(oldSource);
            entry.getSourceTransaction().setType(PortfolioTransaction.Type.TRANSFER_IN);
            entry.getTargetTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);

            PortfolioTransaction oldSourceTransaction = entry.getSourceTransaction();
            entry.setSourceTransaction(entry.getTargetTransaction());
            entry.setTargetTransaction(oldSourceTransaction);
        }
        else if (e instanceof AccountTransferEntry)
        {
            AccountTransferEntry entry = (AccountTransferEntry) transaction.getTransaction().getCrossEntry();

            Account oldSource = entry.getSourceAccount();
            entry.setSourceAccount(entry.getTargetAccount());
            entry.setTargetAccount(oldSource);
            entry.getSourceTransaction().setType(AccountTransaction.Type.TRANSFER_IN);
            entry.getTargetTransaction().setType(AccountTransaction.Type.TRANSFER_OUT);

            AccountTransaction oldSourceTransaction = entry.getSourceTransaction();
            entry.setSourceTransaction(entry.getTargetTransaction());
            entry.setTargetTransaction(oldSourceTransaction);
        }
        else
        {
            throw new IllegalArgumentException("unsupported entry type " + e + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
        }

        client.markDirty();
    }
}
