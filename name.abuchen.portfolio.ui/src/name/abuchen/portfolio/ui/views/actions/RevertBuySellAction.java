package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
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

        PortfolioTransaction tx = buysell.getPortfolioTransaction();

        // when converting between buy and sell transactions, we keep the price
        // of the security the same, but add or subtract fees and taxes
        // depending on the new type of transaction

        Money grossAmount = tx.getUnit(Unit.Type.GROSS_VALUE).map(Unit::getAmount).orElse(tx.getGrossValue());

        Money feesAndTaxes = tx.getUnits().filter(u -> u.getType() == Unit.Type.FEE || u.getType() == Unit.Type.TAX) //
                        .map(Unit::getAmount).collect(MoneyCollectors.sum(tx.getCurrencyCode()));

        if (tx.getType() == PortfolioTransaction.Type.BUY)
        {
            buysell.getAccountTransaction().setType(AccountTransaction.Type.SELL);
            tx.setType(PortfolioTransaction.Type.SELL);

            buysell.setMonetaryAmount(grossAmount.subtract(feesAndTaxes));
        }
        else if (tx.getType() == PortfolioTransaction.Type.SELL)
        {
            buysell.getAccountTransaction().setType(AccountTransaction.Type.BUY);
            tx.setType(PortfolioTransaction.Type.BUY);

            buysell.setMonetaryAmount(grossAmount.add(feesAndTaxes));
        }
        else
        {
            throw new IllegalArgumentException();
        }

        client.markDirty();
    }
}
