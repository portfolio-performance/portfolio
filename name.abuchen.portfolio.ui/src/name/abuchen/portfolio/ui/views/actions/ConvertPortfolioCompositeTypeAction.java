package name.abuchen.portfolio.ui.views.actions;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerPortfolioCompositeTypeConverter;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;

public class ConvertPortfolioCompositeTypeAction extends Action
{
    private final Client client;
    private final TransactionPair<PortfolioTransaction> transaction;

    public ConvertPortfolioCompositeTypeAction(Client client, TransactionPair<PortfolioTransaction> transaction)
    {
        this.client = client;
        this.transaction = transaction;

        var type = transaction.getTransaction().getType();

        if (type != PortfolioTransaction.Type.BUY && type != PortfolioTransaction.Type.SELL
                        && type != PortfolioTransaction.Type.DELIVERY_INBOUND
                        && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException("unsupported transaction type " + type); //$NON-NLS-1$
    }

    @Override
    public void run()
    {
        var converter = new LedgerPortfolioCompositeTypeConverter(client);

        if (converter.canConvertSafely(transaction))
        {
            converter.convert(transaction);
            client.markDirty();
            return;
        }

        if (converter.isLedgerBacked(transaction))
            throw new UnsupportedOperationException(
                            Messages.LedgerConvertPortfolioCompositeTypeActionUnsupportedLedgerBackedTransition);

        var type = transaction.getTransaction().getType();

        if (type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL)
        {
            new RevertBuySellAction(client, transaction).run();
            new ConvertBuySellToDeliveryAction(client, transaction).run();
        }
        else
        {
            new RevertDeliveryAction(client, transaction).run();
            new ConvertDeliveryToBuySellAction(client, transaction).run();
        }
    }
}
