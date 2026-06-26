package name.abuchen.portfolio.model.ledger.nativeentry;

/**
 * Reports failed ledger-native entry assembly.
 * This is internal native-entry infrastructure. It carries diagnostics instead of applying
 * partial Ledger mutations.
 */
public final class LedgerNativeEntryAssemblyException extends IllegalArgumentException
{
    private static final long serialVersionUID = 1L;

    private final LedgerNativeEntryAssemblyIssue issue;

    LedgerNativeEntryAssemblyException(LedgerNativeEntryAssemblyIssue issue, String message)
    {
        super(message);
        this.issue = issue;
    }

    public LedgerNativeEntryAssemblyIssue getIssue()
    {
        return issue;
    }
}
