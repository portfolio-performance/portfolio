package name.abuchen.portfolio.model.ledger;

/**
 * Defines the role of a runtime projection for a Ledger entry.
 * This is internal Ledger model metadata used by projection and compatibility code.
 * Contributor code should not invent roles outside the configured Ledger write paths.
 *
 * <p>
 * Protobuf stores projection roles through the fixed {@code PLedgerProjectionRole} enum and
 * explicit reader/writer mappings. New roles need matching persistence mapping and must not
 * change the meaning of existing stored role values.
 * </p>
 */
public enum LedgerProjectionRole
{
    ACCOUNT,
    PORTFOLIO,
    SOURCE_ACCOUNT,
    TARGET_ACCOUNT,
    SOURCE_PORTFOLIO,
    TARGET_PORTFOLIO,
    DELIVERY,
    DELIVERY_INBOUND,
    DELIVERY_OUTBOUND,
    CASH_COMPENSATION,
    OLD_SECURITY_LEG,
    NEW_SECURITY_LEG
}
