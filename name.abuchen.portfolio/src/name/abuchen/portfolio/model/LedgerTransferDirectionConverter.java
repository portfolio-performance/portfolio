package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Exposes a model-level facade for ledger-backed transfer direction changes.
 * This class keeps existing model action code independent from the internal compatibility
 * package. The actual mutation is delegated to a Ledger compatibility converter.
 */
public final class LedgerTransferDirectionConverter
{
    private final Client client;
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerTransferDirectionConverter delegate;

    public LedgerTransferDirectionConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerTransferDirectionConverter(
                        this.client);
    }

    public boolean canReverse(AccountTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        return transfer.getSourceTransaction() instanceof LedgerBackedTransaction
                        && transfer.getTargetTransaction() instanceof LedgerBackedTransaction;
    }

    public boolean canReverse(PortfolioTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        return transfer.getSourceTransaction() instanceof LedgerBackedTransaction
                        && transfer.getTargetTransaction() instanceof LedgerBackedTransaction;
    }

    public boolean canReverseSafely(AccountTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        if (!(transfer.getSourceTransaction() instanceof LedgerBackedAccountTransaction sourceTransaction)
                        || !(transfer.getTargetTransaction() instanceof LedgerBackedAccountTransaction targetTransaction)
                        || sourceTransaction.getLedgerEntry() != targetTransaction.getLedgerEntry())
            return false;

        var entry = sourceTransaction.getLedgerEntry();
        return LedgerPlanReferenceSupport.refsFollowRoleChanges(client, entry,
                        LedgerPlanReferenceSupport.roleChange(
                                        LedgerPlanReferenceSupport.projectionUUID(entry,
                                                        LedgerProjectionRole.SOURCE_ACCOUNT),
                                        LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT),
                        LedgerPlanReferenceSupport.roleChange(
                                        LedgerPlanReferenceSupport.projectionUUID(entry,
                                                        LedgerProjectionRole.TARGET_ACCOUNT),
                                        LedgerProjectionRole.TARGET_ACCOUNT, LedgerProjectionRole.SOURCE_ACCOUNT));
    }

    public boolean canReverseSafely(PortfolioTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        if (!(transfer.getSourceTransaction() instanceof LedgerBackedPortfolioTransaction sourceTransaction)
                        || !(transfer.getTargetTransaction() instanceof LedgerBackedPortfolioTransaction targetTransaction)
                        || sourceTransaction.getLedgerEntry() != targetTransaction.getLedgerEntry())
            return false;

        var entry = sourceTransaction.getLedgerEntry();
        return LedgerPlanReferenceSupport.refsFollowRoleChanges(client, entry,
                        LedgerPlanReferenceSupport.roleChange(
                                        LedgerPlanReferenceSupport.projectionUUID(entry,
                                                        LedgerProjectionRole.SOURCE_PORTFOLIO),
                                        LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerProjectionRole.TARGET_PORTFOLIO),
                        LedgerPlanReferenceSupport.roleChange(
                                        LedgerPlanReferenceSupport.projectionUUID(entry,
                                                        LedgerProjectionRole.TARGET_PORTFOLIO),
                                        LedgerProjectionRole.TARGET_PORTFOLIO, LedgerProjectionRole.SOURCE_PORTFOLIO));
    }

    public AccountTransferEntry reverse(AccountTransferEntry transfer)
    {
        return delegate.reverse(transfer);
    }

    public PortfolioTransferEntry reverse(PortfolioTransferEntry transfer)
    {
        return delegate.reverse(transfer);
    }
}
