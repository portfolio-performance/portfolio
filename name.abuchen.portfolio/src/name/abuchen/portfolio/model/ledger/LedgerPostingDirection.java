package name.abuchen.portfolio.model.ledger;

/**
 * Describes the semantic movement direction of a Ledger posting.
 * It is additive metadata for future projection derivation and does not replace
 * derived descriptors in the current materialization path.
 */
public enum LedgerPostingDirection
{
    INBOUND,
    OUTBOUND,
    NEUTRAL
}
