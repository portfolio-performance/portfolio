package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Exposes a model-level facade for ledger-backed account type toggles.
 * This class keeps existing model action code independent from the internal compatibility
 * package. The actual mutation is delegated to a Ledger compatibility converter.
 */
public final class LedgerAccountTypeToggleConverter
{
    private final Client client;
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTypeToggleConverter delegate;

    public LedgerAccountTypeToggleConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTypeToggleConverter(
                        this.client);
    }

    public boolean canToggle(TransactionPair<AccountTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedTransaction))
            return false;

        return switch (transaction.getTransaction().getType())
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND -> true;
            default -> false;
        };
    }

    public boolean canToggleSafely(TransactionPair<AccountTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        return canToggle(transaction) && transaction.getTransaction() instanceof LedgerBackedAccountTransaction ledgerTransaction
                        && LedgerPlanReferenceSupport.currentRefsResolveUniquely(client, ledgerTransaction.getLedgerEntry());
    }

    public AccountTransaction toggle(TransactionPair<AccountTransaction> transaction)
    {
        return delegate.toggle(transaction);
    }
}
