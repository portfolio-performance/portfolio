package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Updates same-shape ledger-backed delivery transaction transactions.
 * This class is part of the Ledger compatibility layer. Contributor code should use it when
 * an existing UI or import path needs to edit Ledger truth safely.
 */
public final class LedgerDeliveryTransactionEditor
{
    private final LedgerUnitPostingUpdater unitPostingUpdater = new LedgerUnitPostingUpdater();

    public void apply(LedgerBackedPortfolioTransaction transaction, LedgerDeliveryTransactionEdit edit)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(edit);

        var entry = transaction.getLedgerEntry();

        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_040
                            .message("Unsupported delivery edit for " + entry.getType())); //$NON-NLS-1$

        var role = transaction.getLedgerProjectionRole();
        var postingIndex = LedgerEntryEditSupport.postingIndex(entry,
                        transaction.getLedgerProjectionDescriptor().getPrimaryPosting());

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit, role, postingIndex));
    }

    public void validate(LedgerBackedPortfolioTransaction transaction, LedgerDeliveryTransactionEdit edit)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(edit);

        var entry = transaction.getLedgerEntry();

        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_041
                            .message("Unsupported delivery edit for " + entry.getType())); //$NON-NLS-1$

        var role = transaction.getLedgerProjectionRole();
        var postingIndex = LedgerEntryEditSupport.postingIndex(entry,
                        transaction.getLedgerProjectionDescriptor().getPrimaryPosting());

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit, role, postingIndex));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerDeliveryTransactionEdit edit,
                    name.abuchen.portfolio.model.ledger.LedgerProjectionRole role,
                    int postingIndex)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getPosting().applyTo(LedgerEntryEditSupport.postingAt(editedEntry, postingIndex));
        unitPostingUpdater.applyDirect(editedEntry, edit.getUnits());
        LedgerProjectionSupport.descriptor(editedEntry, role);
    }
}
