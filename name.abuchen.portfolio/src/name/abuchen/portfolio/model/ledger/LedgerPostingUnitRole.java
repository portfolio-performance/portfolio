package name.abuchen.portfolio.model.ledger;

/**
 * Describes how a posting contributes to a generated compatibility transaction
 * unit once projections are derived from posting semantics.
 */
public enum LedgerPostingUnitRole
{
    PRIMARY,
    FEE,
    TAX,
    GROSS_VALUE,
    FOREX_CONTEXT
}
