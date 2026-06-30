package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Exposes a model-level facade for ledger-backed delivery direction changes.
 * This class keeps existing model action code independent from the internal compatibility
 * package. The actual mutation is delegated to a Ledger compatibility converter.
 */
public final class LedgerDeliveryDirectionConverter
{
    private final Client client;
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryDirectionConverter delegate;

    public LedgerDeliveryDirectionConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryDirectionConverter(
                        this.client);
    }

    public boolean canReverse(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        return transaction.getTransaction() instanceof LedgerBackedTransaction
                        && (transaction.getTransaction().getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                        || transaction.getTransaction()
                                                        .getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND);
    }

    public boolean canReverseSafely(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!canReverse(transaction)
                        || !(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            return false;

        var sourceRole = ledgerTransaction.getLedgerProjectionRef().getRole();
        var targetRole = transaction.getTransaction().getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                        ? LedgerProjectionRole.DELIVERY_OUTBOUND
                        : LedgerProjectionRole.DELIVERY_INBOUND;

        return LedgerPlanReferenceSupport.refsFollowRoleChanges(client, ledgerTransaction.getLedgerEntry(),
                        LedgerPlanReferenceSupport.roleChange(ledgerTransaction.getLedgerProjectionRef().getUUID(),
                                        sourceRole, targetRole));
    }

    public PortfolioTransaction reverse(TransactionPair<PortfolioTransaction> transaction)
    {
        return delegate.reverse(transaction);
    }
}
