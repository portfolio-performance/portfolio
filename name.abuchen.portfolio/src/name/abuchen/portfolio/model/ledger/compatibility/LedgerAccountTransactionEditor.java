package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.EnumSet;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerFieldEdit;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

/**
 * Updates same-shape ledger-backed account transaction transactions.
 * This class is part of the Ledger compatibility layer. Contributor code should use it when
 * an existing UI or import path needs to edit Ledger truth safely.
 */
public final class LedgerAccountTransactionEditor
{
    private static final EnumSet<LedgerEntryType> ACCOUNT_ONLY_TYPES = EnumSet.of(LedgerEntryType.DEPOSIT,
                    LedgerEntryType.REMOVAL, LedgerEntryType.INTEREST, LedgerEntryType.INTEREST_CHARGE,
                    LedgerEntryType.FEES, LedgerEntryType.FEES_REFUND, LedgerEntryType.TAXES,
                    LedgerEntryType.TAX_REFUND, LedgerEntryType.DIVIDENDS);

    private final LedgerUnitPostingUpdater unitPostingUpdater = new LedgerUnitPostingUpdater();

    public void apply(LedgerBackedAccountTransaction transaction, LedgerAccountTransactionEdit edit)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(edit);

        var entry = transaction.getLedgerEntry();

        if (!ACCOUNT_ONLY_TYPES.contains(entry.getType()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_007
                            .message("Unsupported account transaction edit for " + entry.getType())); //$NON-NLS-1$

        var projectionUUID = transaction.getLedgerProjectionRef().getUUID();
        var postingUUID = LedgerProjectionSupport.primaryPosting(entry, transaction.getLedgerProjectionRef()).getUUID();

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyEdit(editedEntry, edit, projectionUUID,
                        postingUUID));
    }

    public void validate(LedgerBackedAccountTransaction transaction, LedgerAccountTransactionEdit edit)
    {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(edit);

        var entry = transaction.getLedgerEntry();

        if (!ACCOUNT_ONLY_TYPES.contains(entry.getType()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_008
                            .message("Unsupported account transaction edit for " + entry.getType())); //$NON-NLS-1$

        var projectionUUID = transaction.getLedgerProjectionRef().getUUID();
        var postingUUID = LedgerProjectionSupport.primaryPosting(entry, transaction.getLedgerProjectionRef()).getUUID();

        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyEdit(editedEntry, edit, projectionUUID,
                        postingUUID));
    }

    private void applyEdit(LedgerEntry editedEntry, LedgerAccountTransactionEdit edit, String projectionUUID,
                    String postingUUID)
    {
        LedgerEntryMetadataPatchHelper.apply(editedEntry, edit.getMetadata());
        edit.getPosting().applyTo(LedgerEntryEditSupport.postingByUUID(editedEntry, postingUUID));
        applyExDate(LedgerEntryEditSupport.postingByUUID(editedEntry, postingUUID), edit.getExDate());
        unitPostingUpdater.apply(editedEntry, edit.getUnits());
        ensureProjectionExists(editedEntry, projectionUUID);
    }

    private void applyExDate(LedgerPosting posting, LedgerFieldEdit<java.time.LocalDateTime> edit)
    {
        if (edit.isOmitted())
            return;

        posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE) //
                        .toList().forEach(posting::removeParameter);

        if (edit.isSet())
            posting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                            edit.getValue()));
    }

    private void ensureProjectionExists(LedgerEntry entry, String projectionUUID)
    {
        if (entry.getProjectionRefs().stream().noneMatch(projection -> projection.getUUID().equals(projectionUUID)))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_004
                            .message("Projection was removed by edit: " + projectionUUID)); //$NON-NLS-1$
    }
}
