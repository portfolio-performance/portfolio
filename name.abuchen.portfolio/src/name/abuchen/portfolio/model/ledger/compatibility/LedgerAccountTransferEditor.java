package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Updates same-shape ledger-backed account transfer transactions.
 * This class is part of the Ledger compatibility layer. Contributor code should use it when
 * an existing UI or import path needs to edit Ledger truth safely.
 */
public final class LedgerAccountTransferEditor
{
    private final LedgerUnitPostingUpdater unitPostingUpdater = new LedgerUnitPostingUpdater();

    public void apply(LedgerBackedAccountTransaction transaction, LedgerAccountTransferEdit edit)
    {
        apply(transaction.getLedgerEntry(), edit);
    }

    public void apply(LedgerEntry entry, LedgerAccountTransferEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.CASH_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_009
                            .message("Unsupported account transfer edit for " + entry.getType())); //$NON-NLS-1$

        var sourceProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePostingUUID = sourceProjection.getPrimaryPosting().getUUID();
        var targetPostingUUID = targetProjection.getPrimaryPosting().getUUID();
        var sourceAccount = sourceProjection.getAccount();
        var targetAccount = targetProjection.getAccount();

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourceAccount, targetAccount, sourcePostingUUID, targetPostingUUID));
    }

    public void validate(LedgerEntry entry, LedgerAccountTransferEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.CASH_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_010
                            .message("Unsupported account transfer edit for " + entry.getType())); //$NON-NLS-1$

        var sourceProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePostingUUID = sourceProjection.getPrimaryPosting().getUUID();
        var targetPostingUUID = targetProjection.getPrimaryPosting().getUUID();
        var sourceAccount = sourceProjection.getAccount();
        var targetAccount = targetProjection.getAccount();

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourceAccount, targetAccount, sourcePostingUUID, targetPostingUUID));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerAccountTransferEdit edit,
                    name.abuchen.portfolio.model.Account sourceAccount,
                    name.abuchen.portfolio.model.Account targetAccount,
                    String sourcePostingUUID, String targetPostingUUID)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getSourcePosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, sourcePostingUUID));
        edit.getTargetPosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, targetPostingUUID));
        unitPostingUpdater.apply(editedEntry, edit.getUnits());
        ensureOwners(editedEntry, sourceAccount, targetAccount);
    }

    private void ensureOwners(LedgerEntry entry, name.abuchen.portfolio.model.Account source,
                    name.abuchen.portfolio.model.Account target)
    {
        var sourceProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.TARGET_ACCOUNT);

        if (sourceProjection.getAccount() != source || targetProjection.getAccount() != target)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_005
                            .message("Account transfer owner changes are not supported")); //$NON-NLS-1$
    }
}
