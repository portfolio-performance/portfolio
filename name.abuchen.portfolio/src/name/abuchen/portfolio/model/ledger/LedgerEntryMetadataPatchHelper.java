package name.abuchen.portfolio.model.ledger;

import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Applies safe metadata edits to persisted Ledger entries.
 * This is internal Ledger mutation support. Contributor code should normally use
 * higher-level Ledger editors or compatibility write paths.
 */
public final class LedgerEntryMetadataPatchHelper
{
    private LedgerEntryMetadataPatchHelper()
    {
    }

    public static void apply(LedgerEntry entry, LedgerEntryMetadataPatch patch)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(patch);

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> applyDirect(editedEntry, patch));
    }

    public static void setDateTime(LedgerEntry entry, LocalDateTime dateTime)
    {
        Objects.requireNonNull(entry);

        LedgerEntryEditSupport.applyValidated(entry, editedEntry -> editedEntry.setDateTime(dateTime));
    }

    public static void setNote(LedgerEntry entry, String note)
    {
        apply(entry, LedgerEntryMetadataPatch.builder().note(note).build());
    }

    public static void setSource(LedgerEntry entry, String source)
    {
        apply(entry, LedgerEntryMetadataPatch.builder().source(source).build());
    }

    private static void applyDirect(LedgerEntry entry, LedgerEntryMetadataPatch patch)
    {
        applyDateTime(entry, patch.getDateTime());
        applyNote(entry, patch.getNote());
        applySource(entry, patch.getSource());
    }

    private static void applyDateTime(LedgerEntry entry, LedgerFieldEdit<?> edit)
    {
        if (edit.isOmitted())
            return;

        if (edit.isClear())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_005.message("Ledger entry date/time cannot be cleared")); //$NON-NLS-1$

        entry.setDateTime((java.time.LocalDateTime) edit.getValue());
    }

    private static void applyNote(LedgerEntry entry, LedgerFieldEdit<String> edit)
    {
        if (edit.isOmitted())
            return;

        entry.setNote(edit.isClear() ? null : edit.getValue());
    }

    private static void applySource(LedgerEntry entry, LedgerFieldEdit<String> edit)
    {
        if (edit.isOmitted())
            return;

        entry.setSource(edit.isClear() ? null : edit.getValue());
    }
}
