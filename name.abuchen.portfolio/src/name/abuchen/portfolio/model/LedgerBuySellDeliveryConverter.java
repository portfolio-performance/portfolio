package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Exposes a model-level facade for ledger-backed buy/sell delivery conversion.
 * This class keeps existing model action code independent from the internal compatibility
 * package. The actual mutation is delegated to a Ledger compatibility converter.
 */
public final class LedgerBuySellDeliveryConverter
{
    private final Client client;
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellDeliveryConverter delegate;

    public LedgerBuySellDeliveryConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellDeliveryConverter(
                        this.client);
    }

    public boolean canConvert(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        return transaction.getTransaction() instanceof LedgerBackedTransaction
                        && (transaction.getTransaction().getType() == PortfolioTransaction.Type.BUY
                                        || transaction.getTransaction().getType() == PortfolioTransaction.Type.SELL);
    }

    public boolean canConvertDeliveryToBuySell(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        return transaction.getTransaction() instanceof LedgerBackedTransaction
                        && (transaction.getTransaction().getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                        || transaction.getTransaction()
                                                        .getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND);
    }

    public boolean canConvertSafely(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!canConvert(transaction)
                        || !(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            return false;

        var entry = ledgerTransaction.getLedgerEntry();
        var targetRole = transaction.getTransaction().getType() == PortfolioTransaction.Type.BUY
                        ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;

        return LedgerPlanReferenceSupport.refsFollowRoleChanges(client, entry,
                        LedgerPlanReferenceSupport.roleChange(ledgerTransaction.getLedgerProjectionRef().getUUID(),
                                        LedgerProjectionRole.PORTFOLIO, targetRole));
    }

    public boolean canConvertDeliveryToBuySellSafely(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!canConvertDeliveryToBuySell(transaction)
                        || !(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            return false;

        return LedgerPlanReferenceSupport.refsFollowRoleChanges(client, ledgerTransaction.getLedgerEntry(),
                        LedgerPlanReferenceSupport.roleChange(ledgerTransaction.getLedgerProjectionRef().getUUID(),
                                        ledgerTransaction.getLedgerProjectionRef().getRole(),
                                        LedgerProjectionRole.PORTFOLIO));
    }

    public PortfolioTransaction convertBuySellToDelivery(TransactionPair<PortfolioTransaction> transaction)
    {
        return delegate.convertBuySellToDelivery(transaction);
    }

    public BuySellEntry convertDeliveryToBuySell(TransactionPair<PortfolioTransaction> transaction)
    {
        return delegate.convertDeliveryToBuySell(transaction);
    }
}
