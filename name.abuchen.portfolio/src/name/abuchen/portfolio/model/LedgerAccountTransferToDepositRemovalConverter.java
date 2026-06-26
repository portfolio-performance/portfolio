package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Exposes a model-level facade for ledger-backed account transfer splitting.
 * This class keeps existing model action code independent from the internal compatibility
 * package. The actual mutation is delegated to a Ledger compatibility converter.
 */
public final class LedgerAccountTransferToDepositRemovalConverter
{
    private final name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferToDepositRemovalConverter delegate;

    public LedgerAccountTransferToDepositRemovalConverter(Client client)
    {
        this.delegate = new name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferToDepositRemovalConverter(
                        Objects.requireNonNull(client));
    }

    public boolean canSplit(AccountTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        return transfer.getSourceTransaction() instanceof LedgerBackedTransaction
                        && transfer.getTargetTransaction() instanceof LedgerBackedTransaction;
    }

    public SplitResult split(AccountTransferEntry transfer)
    {
        return delegate.split(transfer);
    }

    public record SplitResult(AccountTransaction removal, AccountTransaction deposit)
    {
    }
}
