package name.abuchen.portfolio.ui.views.actions;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;

public class ConvertBuySellToDeliveryAction extends Action
{
    private final Client client;
    private final Collection<TransactionPair<PortfolioTransaction>> transactionList;

    public ConvertBuySellToDeliveryAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this(client, Arrays.asList(transaction));
    }

    public ConvertBuySellToDeliveryAction(Client client,
                    Collection<TransactionPair<PortfolioTransaction>> transactionList)
    {
        this.client = client;
        this.transactionList = transactionList;

        boolean allBuy = true;
        boolean allSell = true;

        for (TransactionPair<PortfolioTransaction> tx : transactionList)
        {
            if (tx.getTransaction().getType() != PortfolioTransaction.Type.BUY
                            && tx.getTransaction().getType() != PortfolioTransaction.Type.SELL)
                throw new IllegalArgumentException();

            allBuy &= tx.getTransaction().getType() == PortfolioTransaction.Type.BUY;
            allSell &= tx.getTransaction().getType() == PortfolioTransaction.Type.SELL;

        }

        if (allBuy)
            setText(Messages.MenuConvertToInboundDelivery);
        else if (allSell)
            setText(Messages.MenuConvertToOutboundDelivery);
        else
            setText(Messages.MenuConvertToDelivery);
    }

    @Override
    public void run()
    {
        for (TransactionPair<PortfolioTransaction> transaction : transactionList)
        {
            // delete existing transaction
            PortfolioTransaction buySellTransaction = transaction.getTransaction();
            transaction.getOwner().deleteTransaction(buySellTransaction, client);

            // create new delivery
            PortfolioTransaction delivery = new PortfolioTransaction();
            delivery.setType(buySellTransaction.getType() == PortfolioTransaction.Type.BUY
                            ? PortfolioTransaction.Type.DELIVERY_INBOUND
                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            delivery.setDateTime(buySellTransaction.getDateTime());
            delivery.setMonetaryAmount(buySellTransaction.getMonetaryAmount());
            delivery.setSecurity(buySellTransaction.getSecurity());
            delivery.setNote(buySellTransaction.getNote());
            delivery.setShares(buySellTransaction.getShares());

            buySellTransaction.getUnits().forEach(delivery::addUnit);

            transaction.getOwner().addTransaction(delivery);
        }

        client.markDirty();
    }
}
