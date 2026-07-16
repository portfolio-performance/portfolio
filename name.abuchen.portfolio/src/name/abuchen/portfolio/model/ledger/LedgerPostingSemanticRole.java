package name.abuchen.portfolio.model.ledger;

/**
 * Names the business role a posting plays inside an entry.
 * This vocabulary is intentionally separate from persisted projection identity.
 */
public enum LedgerPostingSemanticRole
{
    CASH,
    SECURITY,
    RIGHT,
    BOND,
    CASH_COMPENSATION,
    ACCRUED_INTEREST,
    PRINCIPAL_REDEMPTION,
    FEE,
    TAX,
    GROSS_VALUE,
    FOREX_CONTEXT
}
