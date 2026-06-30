package name.abuchen.portfolio.model.ledger.projection;

import java.util.List;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.ledger.LedgerEntry;

/**
 * Represents a runtime legacy cross-entry view backed by a Ledger entry.
 * This class belongs to projection support. Cross-entry views are compatibility objects and
 * must not become a second persisted transaction truth.
 */
final class LedgerBackedCrossEntry implements CrossEntry
{
    private final LedgerEntry entry;
    private final List<Transaction> transactions;

    LedgerBackedCrossEntry(LedgerEntry entry, List<Transaction> transactions)
    {
        this.entry = entry;
        this.transactions = List.copyOf(transactions);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        projection(t);
    }

    @Override
    public TransactionOwner<? extends Transaction> getOwner(Transaction t)
    {
        return projection(t).owner();
    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        var projection = projection(t);

        return transactions.stream() //
                        .filter(transaction -> transaction != projection.transaction()) //
                        .findFirst().orElse(null);
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        var crossTransaction = getCrossTransaction(t);

        return crossTransaction != null ? projection(crossTransaction).owner() : null;
    }

    @Override
    public void insert()
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public String getSource()
    {
        return entry.getSource();
    }

    @Override
    public void setSource(String source)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    private Projection projection(Transaction transaction)
    {
        for (var candidate : transactions)
        {
            if (candidate == transaction || candidate.getUUID().equals(transaction.getUUID()))
            {
                var ledgerBacked = (LedgerBackedTransaction) candidate;
                var projectionRef = ledgerBacked.getLedgerProjectionRef();

                if (candidate instanceof LedgerBackedAccountTransaction)
                    return new Projection(candidate, projectionRef.getAccount());

                if (candidate instanceof LedgerBackedPortfolioTransaction)
                    return new Projection(candidate, projectionRef.getPortfolio());
            }
        }

        throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_068
                        .message("Transaction does not belong to this Ledger cross entry: " + transaction.getUUID())); //$NON-NLS-1$
    }

    private record Projection(Transaction transaction, TransactionOwner<? extends Transaction> owner)
    {
    }
}
