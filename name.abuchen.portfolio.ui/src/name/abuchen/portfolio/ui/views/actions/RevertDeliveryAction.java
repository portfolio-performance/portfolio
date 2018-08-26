package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;

public class RevertDeliveryAction extends Action
{
    private final Client client;
    private final TransactionPair<PortfolioTransaction> transaction;

    public RevertDeliveryAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction().getType() != PortfolioTransaction.Type.DELIVERY_INBOUND
                        && transaction.getTransaction().getType() != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException();

    }

    @Override
    public void run()
    {
        PortfolioTransaction deliveryTransaction = transaction.getTransaction();

        if (deliveryTransaction instanceof PortfolioTransaction)
        {
            if (PortfolioTransaction.Type.DELIVERY_INBOUND.equals(deliveryTransaction.getType()))
                deliveryTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            else if (PortfolioTransaction.Type.DELIVERY_OUTBOUND.equals(deliveryTransaction.getType()))
                deliveryTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            else
                throw new IllegalArgumentException();
        }
        else
        {
            throw new IllegalArgumentException();
        }

        client.markDirty();
    }
}
