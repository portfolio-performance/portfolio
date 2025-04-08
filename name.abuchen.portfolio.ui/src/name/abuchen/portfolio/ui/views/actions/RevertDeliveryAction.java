package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

public class RevertDeliveryAction extends Action
{
    private final Client client;
    private final TransactionPair<PortfolioTransaction> transaction;

    public RevertDeliveryAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        PortfolioTransaction tx = transaction.getTransaction();
        Type type = tx.getType();
        if (type != PortfolioTransaction.Type.DELIVERY_INBOUND
                        && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException(
                            "unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$

    }

    @Override
    public void run()
    {
        PortfolioTransaction tx = transaction.getTransaction();

        // when converting between inbound and outbound deliveries, we keep the
        // price of the security the same, but add or subtract fees and taxes
        // depending on the new type of transaction

        Money grossAmount = tx.getUnit(Unit.Type.GROSS_VALUE).map(Unit::getAmount).orElse(tx.getGrossValue());

        Money feesAndTaxes = tx.getUnits().filter(u -> u.getType() == Unit.Type.FEE || u.getType() == Unit.Type.TAX) //
                        .map(Unit::getAmount).collect(MoneyCollectors.sum(tx.getCurrencyCode()));

        Type type = tx.getType();
        if (PortfolioTransaction.Type.DELIVERY_INBOUND.equals(type))
        {
            tx.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            tx.setMonetaryAmount(grossAmount.subtract(feesAndTaxes));
        }
        else if (PortfolioTransaction.Type.DELIVERY_OUTBOUND.equals(type))
        {
            tx.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            tx.setMonetaryAmount(grossAmount.add(feesAndTaxes));
        }
        else
        {
            throw new IllegalArgumentException("unsupported transaction type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
        }

        client.markDirty();
    }
}
