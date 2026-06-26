package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;

/**
 * Carries the result of assembling ledger-native entries.
 * This is internal native-entry infrastructure. Contributor code should use higher-level
 * workflows before treating native entries as user-facing transactions.
 *
 * <p>
 * The result object is not persisted. Only the assembled {@link LedgerEntry} and its
 * projection refs become part of the Ledger model.
 * </p>
 */
public final class LedgerNativeEntryBuildResult
{
    private final LedgerEntry entry;
    private final LedgerStructuralValidator.ValidationResult validationResult;

    LedgerNativeEntryBuildResult(LedgerEntry entry, LedgerStructuralValidator.ValidationResult validationResult)
    {
        this.entry = Objects.requireNonNull(entry);
        this.validationResult = Objects.requireNonNull(validationResult);
    }

    public LedgerEntry getEntry()
    {
        return entry;
    }

    public List<LedgerProjectionRef> getProjectionRefs()
    {
        return entry.getProjectionRefs();
    }

    public LedgerStructuralValidator.ValidationResult getValidationResult()
    {
        return validationResult;
    }
}
