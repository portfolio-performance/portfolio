package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Names functional business legs inside a native Ledger entry.
 * A leg role is not persisted directly. It lets Java configuration distinguish
 * business sides that can share the same posting type, such as the old and new
 * security sides of a spin-off.
 */
public enum LedgerLegRole
{
    SOURCE_SECURITY_LEG,
    TARGET_SECURITY_LEG,
    RECEIVED_SECURITY_LEG,
    DISTRIBUTED_RIGHT_LEG,
    DISTRIBUTED_SECURITY_LEG,
    SOURCE_BOND_LEG,
    CASH_LEG,
    CASH_COMPENSATION_LEG,
    ACCRUED_INTEREST_LEG,
    PRINCIPAL_REDEMPTION_LEG,
    FEE_LEG,
    TAX_LEG,
    FOREX_CONTEXT_LEG
}
