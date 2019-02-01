package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;

public class ConvertBuySellToDeliveryAction extends Action
{
    private final Client client;
    private final TransactionPair<PortfolioTransaction> transaction;

    public ConvertBuySellToDeliveryAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction().getType() != PortfolioTransaction.Type.BUY
                        && transaction.getTransaction().getType() != PortfolioTransaction.Type.SELL)
            throw new IllegalArgumentException();

        setText(transaction.getTransaction().getType() == PortfolioTransaction.Type.BUY
                        ? Messages.MenuConvertToInboundDelivery : Messages.MenuConvertToOutboundDelivery);
    }

    @Override
    public void run()
    {
        // delete existing transaction
        PortfolioTransaction buySellTransaction = transaction.getTransaction();
        transaction.getOwner().deleteTransaction(buySellTransaction, client);

        // create new delivery
        PortfolioTransaction delivery = new PortfolioTransaction();
        delivery.setType(buySellTransaction.getType() == PortfolioTransaction.Type.BUY
                        ? PortfolioTransaction.Type.DELIVERY_INBOUND : PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        delivery.setDateTime(buySellTransaction.getDateTime());
        delivery.setMonetaryAmount(buySellTransaction.getMonetaryAmount());
        delivery.setSecurity(buySellTransaction.getSecurity());
        delivery.setNote(buySellTransaction.getNote());
        delivery.setShares(buySellTransaction.getShares());

        buySellTransaction.getUnits().forEach(delivery::addUnit);

        transaction.getOwner().addTransaction(delivery);

        client.markDirty();
    }
}
