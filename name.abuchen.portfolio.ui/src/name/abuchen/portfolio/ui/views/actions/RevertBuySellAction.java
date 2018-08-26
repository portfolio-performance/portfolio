package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertBuySellAction extends Action
{
    private final Client client;
    private final TransactionPair<?> transaction;

    public RevertBuySellAction(Client client, TransactionPair<?> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction() instanceof PortfolioTransaction)
        {
            PortfolioTransaction.Type type = ((PortfolioTransaction) transaction.getTransaction()).getType();

            if (type != PortfolioTransaction.Type.BUY && type != PortfolioTransaction.Type.SELL)
                throw new IllegalArgumentException();
        }
        else if (transaction.getTransaction() instanceof AccountTransaction)
        {
            AccountTransaction.Type type = ((AccountTransaction) transaction.getTransaction()).getType();

            if (type != AccountTransaction.Type.BUY && type != AccountTransaction.Type.SELL)
                throw new IllegalArgumentException();
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void run()
    {
        BuySellEntry buysell = (BuySellEntry) transaction.getTransaction().getCrossEntry();

        if (transaction.getTransaction() instanceof PortfolioTransaction)
        {
            PortfolioTransaction.Type type = ((PortfolioTransaction) transaction.getTransaction()).getType();
            if (PortfolioTransaction.Type.BUY.equals(type))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.SELL);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.SELL);
            }
            else if (PortfolioTransaction.Type.SELL.equals(type))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.BUY);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.BUY);
            }
            else
            {
                throw new IllegalArgumentException();
            }
        }
        else if (transaction.getTransaction() instanceof AccountTransaction)
        {
            AccountTransaction.Type type = ((AccountTransaction) transaction.getTransaction()).getType();
            if (AccountTransaction.Type.BUY.equals(type))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.SELL);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.SELL);
            }
            else if (AccountTransaction.Type.SELL.equals(type))
            {
                buysell.getAccountTransaction().setType(AccountTransaction.Type.BUY);
                buysell.getPortfolioTransaction().setType(PortfolioTransaction.Type.BUY);
            }
            else
            {
                throw new IllegalArgumentException();
            }
        }
        else
        {
            throw new IllegalArgumentException();
        }

        client.markDirty();
    }
}
