package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;

public class ConvertDeliveryToBuySellAction extends Action
{
    private final Client client;
    private final TransactionPair<PortfolioTransaction> transaction;

    public ConvertDeliveryToBuySellAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        if (transaction.getTransaction().getType() != PortfolioTransaction.Type.DELIVERY_INBOUND
                        && transaction.getTransaction().getType() != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException();

        setText(transaction.getTransaction().getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                        ? Messages.MenuConvertToBuy : Messages.MenuConvertToSell);
    }

    @Override
    public void run()
    {
        // delete existing transaction
        PortfolioTransaction deliveryTransaction = transaction.getTransaction();
        transaction.getOwner().deleteTransaction(deliveryTransaction, client);

        // create new buy / sell

        Portfolio portfolio = (Portfolio) transaction.getOwner();
        Account account = portfolio.getReferenceAccount();

        BuySellEntry entry = new BuySellEntry(portfolio, account);

        entry.setType(deliveryTransaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                        ? PortfolioTransaction.Type.BUY : PortfolioTransaction.Type.SELL);

        entry.setDate(deliveryTransaction.getDateTime());
        entry.setMonetaryAmount(deliveryTransaction.getMonetaryAmount());
        entry.setSecurity(deliveryTransaction.getSecurity());
        entry.setNote(deliveryTransaction.getNote());
        entry.setShares(deliveryTransaction.getShares());

        deliveryTransaction.getUnits().forEach(entry.getPortfolioTransaction()::addUnit);

        entry.insert();

        client.markDirty();
    }
}
