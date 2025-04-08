package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

public class RevertBuySellAction extends Action
{
    private final Client client;
    private final TransactionPair<?> transaction;

    public RevertBuySellAction(Client client, TransactionPair<?> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        Transaction tx = transaction.getTransaction();
        if (tx instanceof PortfolioTransaction)
        {
            PortfolioTransaction.Type type = ((PortfolioTransaction) tx).getType();

            if (type != PortfolioTransaction.Type.BUY && type != PortfolioTransaction.Type.SELL)
                throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (tx instanceof AccountTransaction)
        {
            AccountTransaction.Type type = ((AccountTransaction) tx).getType();

            if (type != AccountTransaction.Type.BUY && type != AccountTransaction.Type.SELL)
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
        BuySellEntry buysell = (BuySellEntry) transaction.getTransaction().getCrossEntry();

        PortfolioTransaction tx = buysell.getPortfolioTransaction();

        // when converting between buy and sell transactions, we keep the price
        // of the security the same, but add or subtract fees and taxes
        // depending on the new type of transaction

        Money grossAmount = tx.getUnit(Unit.Type.GROSS_VALUE).map(Unit::getAmount).orElse(tx.getGrossValue());

        Money feesAndTaxes = tx.getUnits().filter(u -> u.getType() == Unit.Type.FEE || u.getType() == Unit.Type.TAX) //
                        .map(Unit::getAmount).collect(MoneyCollectors.sum(tx.getCurrencyCode()));

        Type type = tx.getType();
        if (type == PortfolioTransaction.Type.BUY)
        {
            buysell.getAccountTransaction().setType(AccountTransaction.Type.SELL);
            tx.setType(PortfolioTransaction.Type.SELL);

            buysell.setMonetaryAmount(grossAmount.subtract(feesAndTaxes));
        }
        else if (type == PortfolioTransaction.Type.SELL)
        {
            buysell.getAccountTransaction().setType(AccountTransaction.Type.BUY);
            tx.setType(PortfolioTransaction.Type.BUY);

            buysell.setMonetaryAmount(grossAmount.add(feesAndTaxes));
        }
        else
        {
            throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
        }

        client.markDirty();
    }
}
