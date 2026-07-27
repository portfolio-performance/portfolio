package name.abuchen.portfolio.model.ledger;

/**
 * The business role a posting plays inside its entry: the cash or the instrument
 * that moves, the fee, tax, or gross value that qualifies the movement, or the
 * currency context it supplies.
 */
public enum LedgerPostingSemanticRole
{
    CASH,
    SECURITY,
    FEE,
    TAX,
    GROSS_VALUE,
    FOREX_CONTEXT
}
