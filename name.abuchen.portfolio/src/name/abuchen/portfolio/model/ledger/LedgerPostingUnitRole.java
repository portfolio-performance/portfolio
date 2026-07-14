package name.abuchen.portfolio.model.ledger;

/**
 * How a posting contributes to the value of its entry: as the primary movement,
 * or as a fee, tax, gross value, or forex component of it.
 */
public enum LedgerPostingUnitRole
{
    PRIMARY,
    FEE,
    TAX,
    GROSS_VALUE,
    FOREX_CONTEXT
}
