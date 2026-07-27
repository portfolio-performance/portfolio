package name.abuchen.portfolio.model.ledger;

/**
 * Identifies which slot of an entry a primary posting fills, for example the
 * source or the target account of a cash transfer. LedgerRoleRules assigns
 * exactly one role to each primary posting an entry type requires; projecting an
 * entry onto the transaction model maps its postings by this role.
 */
public enum LedgerProjectionRole
{
    ACCOUNT,
    PORTFOLIO,
    SOURCE_ACCOUNT,
    TARGET_ACCOUNT,
    SOURCE_PORTFOLIO,
    TARGET_PORTFOLIO,
    DELIVERY_INBOUND,
    DELIVERY_OUTBOUND
}
