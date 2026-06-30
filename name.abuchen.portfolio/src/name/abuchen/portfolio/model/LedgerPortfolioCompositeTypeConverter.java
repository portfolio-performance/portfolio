package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;

/**
 * Exposes a model-level facade for composite ledger-backed portfolio type conversions.
 * The actual mutation is delegated to the Ledger compatibility converter.
 */
public final class LedgerPortfolioCompositeTypeConverter
{
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioCompositeTypeConverter delegate;

    public LedgerPortfolioCompositeTypeConverter(Client client)
    {
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioCompositeTypeConverter(
                        Objects.requireNonNull(client));
    }

    public boolean isLedgerBacked(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        return transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction;
    }

    public boolean canConvertSafely(TransactionPair<PortfolioTransaction> transaction)
    {
        return delegate.canConvertSafely(transaction);
    }

    public PortfolioTransaction convert(TransactionPair<PortfolioTransaction> transaction)
    {
        return delegate.convert(transaction);
    }
}
