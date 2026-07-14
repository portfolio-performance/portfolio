package name.abuchen.portfolio.model.ledger;

/**
 * The direction in which a primary posting moves value.
 * <p>
 * INBOUND and OUTBOUND separate the two legs of an entry that would otherwise
 * look alike, for example the debited and the credited account of a cash
 * transfer. NEUTRAL is used where the entry type alone already determines the
 * movement, for example a deposit.
 */
public enum LedgerPostingDirection
{
    INBOUND,
    OUTBOUND,
    NEUTRAL
}
