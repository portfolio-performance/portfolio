package name.abuchen.portfolio.ui.views.actions;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;

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
    private final Collection<TransactionPair<PortfolioTransaction>> transactionList;

    public ConvertDeliveryToBuySellAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this(client, Arrays.asList(transaction));
    }

    public ConvertDeliveryToBuySellAction(Client client,
                    Collection<TransactionPair<PortfolioTransaction>> transactionList)
    {
        this.client = client;
        this.transactionList = transactionList;

        boolean allInbound = true;
        boolean allOutbound = true;

        for (TransactionPair<PortfolioTransaction> tx : transactionList)
        {
            if (tx.getTransaction().getType() != PortfolioTransaction.Type.DELIVERY_INBOUND
                            && tx.getTransaction().getType() != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                throw new IllegalArgumentException();

            allInbound &= tx.getTransaction().getType() == PortfolioTransaction.Type.DELIVERY_INBOUND;
            allOutbound &= tx.getTransaction().getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
        }

        if (allInbound)
            setText(Messages.MenuConvertToBuy);
        else if (allOutbound)
            setText(Messages.MenuConvertToSell);
        else
            setText(Messages.MenuConvertToBuySell);
    }

    @Override
    public void run()
    {
        for (TransactionPair<PortfolioTransaction> transaction : transactionList)
        {
            Portfolio portfolio = (Portfolio) transaction.getOwner();
            Account account = portfolio.getReferenceAccount();
            PortfolioTransaction deliveryTransaction = transaction.getTransaction();

            // check if the transaction currency fits to the reference account
            // (and if not, fail fast)

            if (!deliveryTransaction.getCurrencyCode().equals(account.getCurrencyCode()))
                throw new IllegalArgumentException(MessageFormat.format(
                                Messages.MsgErrorConvertToBuySellCurrencyMismatch,
                                deliveryTransaction.getCurrencyCode(), account.getCurrencyCode(), account.getName()));

            // delete existing transaction
            transaction.getOwner().deleteTransaction(deliveryTransaction, client);

            // create new buy / sell
            BuySellEntry entry = new BuySellEntry(portfolio, account);

            entry.setType(deliveryTransaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                            ? PortfolioTransaction.Type.BUY
                            : PortfolioTransaction.Type.SELL);

            entry.setDate(deliveryTransaction.getDateTime());
            entry.setMonetaryAmount(deliveryTransaction.getMonetaryAmount());
            entry.setSecurity(deliveryTransaction.getSecurity());
            entry.setNote(deliveryTransaction.getNote());
            entry.setShares(deliveryTransaction.getShares());

            deliveryTransaction.getUnits().forEach(entry.getPortfolioTransaction()::addUnit);

            entry.insert();
        }

        client.markDirty();
    }
}
