package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
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

        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePostingUUID = LedgerProjectionSupport.primaryPosting(entry, sourceProjection).getUUID();
        var targetPostingUUID = LedgerProjectionSupport.primaryPosting(entry, targetProjection).getUUID();
        var sourceAccount = sourceProjection.getAccount();
        var targetAccount = targetProjection.getAccount();

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourceProjection.getUUID(), sourceAccount, targetProjection.getUUID(), targetAccount,
                        sourcePostingUUID, targetPostingUUID));
    }

    public void validate(LedgerEntry entry, LedgerAccountTransferEdit edit)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(edit);

        if (entry.getType() != LedgerEntryType.CASH_TRANSFER)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_010
                            .message("Unsupported account transfer edit for " + entry.getType())); //$NON-NLS-1$

        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePostingUUID = LedgerProjectionSupport.primaryPosting(entry, sourceProjection).getUUID();
        var targetPostingUUID = LedgerProjectionSupport.primaryPosting(entry, targetProjection).getUUID();
        var sourceAccount = sourceProjection.getAccount();
        var targetAccount = targetProjection.getAccount();

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit,
                        sourceProjection.getUUID(), sourceAccount, targetProjection.getUUID(), targetAccount,
                        sourcePostingUUID, targetPostingUUID));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerAccountTransferEdit edit, String sourceProjectionUUID,
                    name.abuchen.portfolio.model.Account sourceAccount, String targetProjectionUUID,
                    name.abuchen.portfolio.model.Account targetAccount, String sourcePostingUUID,
                    String targetPostingUUID)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getSourcePosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, sourcePostingUUID));
        edit.getTargetPosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, targetPostingUUID));
        unitPostingUpdater.apply(editedEntry, edit.getUnits());
        ensureOwners(editedEntry, sourceProjectionUUID, sourceAccount, targetProjectionUUID, targetAccount);
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream() //
                        .filter(projection -> projection.getRole() == role) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Projection not found: " + role)); //$NON-NLS-1$
    }

    private void ensureOwners(LedgerEntry entry, String sourceProjectionUUID, name.abuchen.portfolio.model.Account source,
                    String targetProjectionUUID, name.abuchen.portfolio.model.Account target)
    {
        var sourceProjection = entry.getProjectionRefs().stream()
                        .filter(projection -> projection.getUUID().equals(sourceProjectionUUID)).findFirst()
                        .orElseThrow();
        var targetProjection = entry.getProjectionRefs().stream()
                        .filter(projection -> projection.getUUID().equals(targetProjectionUUID)).findFirst()
                        .orElseThrow();

        if (sourceProjection.getAccount() != source || targetProjection.getAccount() != target)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_005
                            .message("Account transfer owner changes are not supported")); //$NON-NLS-1$
    }
}
