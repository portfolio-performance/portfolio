package name.abuchen.portfolio.model.ledger;

import java.time.Instant;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Provides shared support for safe edits on persisted Ledger entries.
 * This is internal Ledger mutation support. Contributors should normally use creators,
 * editors, converters, or deleters instead of calling it directly.
 */
public final class LedgerEntryEditSupport
{
    private LedgerEntryEditSupport()
    {
    }

    public static void applyValidated(LedgerEntry entry, EntryPatch patch)
    {
        validatePatch(entry, patch);
        patch.apply(entry);
        entry.setUpdatedAt(Instant.now());
    }

    public static void validatePatch(LedgerEntry entry, EntryPatch patch)
    {
        var candidate = LedgerModelCopy.copyEntry(entry);

        patch.apply(candidate);
        validate(candidate);
    }

    public static int postingIndex(LedgerEntry entry, LedgerPosting posting)
    {
        var index = entry.getPostings().indexOf(posting);

        if (index < 0)
            throw new IllegalArgumentException("Ledger posting does not belong to entry"); //$NON-NLS-1$

        return index;
    }

    public static LedgerPosting postingAt(LedgerEntry entry, int index)
    {
        if (index < 0 || index >= entry.getPostings().size())
            throw new IllegalArgumentException("Ledger posting index out of range: " + index); //$NON-NLS-1$

        return entry.getPostings().get(index);
    }

    public static LedgerPosting postingByUUID(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getUUID().equals(uuid)) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Ledger posting not found: " + uuid)); //$NON-NLS-1$
    }

    static LedgerPosting firstPosting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == type) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Ledger posting not found: " + type)); //$NON-NLS-1$
    }

    private static void validate(LedgerEntry entry)
    {
        var ledger = new Ledger();

        LedgerGraphWriter.addEntry(ledger, entry);

        var result = LedgerStructuralValidator.validate(ledger);

        if (!result.isOK())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_004.message("Invalid ledger edit: " + result.format())); //$NON-NLS-1$
    }

    @FunctionalInterface
    public interface EntryPatch
    {
        void apply(LedgerEntry entry);
    }
}
