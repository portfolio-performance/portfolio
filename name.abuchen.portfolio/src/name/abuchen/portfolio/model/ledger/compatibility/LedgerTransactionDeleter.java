package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Deletes ledger-backed transactions through their persisted Ledger entry.
 * This class is part of the Ledger compatibility layer. Contributor code should use it
 * instead of removing only a runtime projection from an owner list.
 */
public final class LedgerTransactionDeleter
{
    private final Client client;
    private final LedgerMutationContext mutationContext;

    public LedgerTransactionDeleter(Client client)
    {
        this.client = Objects.requireNonNull(client);
        this.mutationContext = new LedgerMutationContext(client);
    }

    LedgerTransactionDeleter(LedgerMutationContext mutationContext)
    {
        this.client = null;
        this.mutationContext = Objects.requireNonNull(mutationContext);
    }

    public void delete(LedgerBackedTransaction transaction)
    {
        Objects.requireNonNull(transaction);

        delete(transaction.getLedgerEntry());
    }

    public void delete(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        mutationContext.removeEntry(entry);

        if (client != null)
            client.getPlans().forEach(plan -> plan.removeLedgerExecutionRefs(entry));
    }
}
