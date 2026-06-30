package name.abuchen.portfolio.model.ledger.nativeentry;

/**
 * Describes a validation or assembly issue for ledger-native entries.
 * This is internal native-entry infrastructure. It lets callers report precise failures
 * without guessing missing transaction facts.
 */
public enum LedgerNativeEntryAssemblyIssue
{
    ENTRY_TYPE_NOT_NATIVE,
    ENTRY_DEFINITION_MISSING,
    POSTING_TYPE_NOT_IN_ENTRY_DEFINITION,
    PARAMETER_NOT_IN_ENTRY_DEFINITION,
    PARAMETER_NOT_IN_POSTING_DEFINITION,
    PARAMETER_NOT_IN_POSTING_TYPE_DEFINITION,
    VALUE_KIND_MISMATCH,
    PARAMETER_CODE_NOT_ALLOWED,
    PROJECTION_ROLE_NOT_IN_ENTRY_DEFINITION,
    PROJECTION_TARGET_MISSING,
    REQUIRED_VALUE_MISSING,
    STRUCTURAL_VALIDATION_FAILED,
    NATIVE_DEFINITION_VALIDATION_FAILED
}
