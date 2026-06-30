package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Exposes a model-level facade for ledger-backed buy/sell reversal.
 * This class keeps existing model action code independent from the internal compatibility
 * package. The actual mutation is delegated to a Ledger compatibility converter.
 */
public final class LedgerBuySellReversalConverter
{
    private final Client client;
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellReversalConverter delegate;

    public LedgerBuySellReversalConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellReversalConverter(
                        this.client);
    }

    public boolean canReverse(BuySellEntry entry)
    {
        Objects.requireNonNull(entry);

        return entry.getAccountTransaction() instanceof LedgerBackedTransaction
                        && entry.getPortfolioTransaction() instanceof LedgerBackedTransaction;
    }

    public boolean canReverseSafely(BuySellEntry entry)
    {
        Objects.requireNonNull(entry);

        return delegate.canReverseSafely(entry);
    }

    public BuySellEntry reverse(BuySellEntry entry)
    {
        return delegate.reverse(entry);
    }

}
