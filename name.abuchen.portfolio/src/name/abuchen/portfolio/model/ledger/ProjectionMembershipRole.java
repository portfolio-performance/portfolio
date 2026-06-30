package name.abuchen.portfolio.model.ledger;

/**
 * Defines how a posting participates in a compatibility projection.
 * Projection memberships are owned by {@link LedgerProjectionRef}; they do not move
 * compatibility identity or owner routing into {@link LedgerPosting}.
 */
public enum ProjectionMembershipRole
{
    PRIMARY,
    GROUP_ANCHOR,
    FEE_UNIT,
    TAX_UNIT,
    GROSS_VALUE_UNIT,
    FOREX_CONTEXT
}
