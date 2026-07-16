package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the reporting class Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * These values are runtime configuration markers. They are not persisted in Ledger entries
 * and do not implement reporting behavior by themselves.
 * </p>
 */
public enum LedgerReportingClass
{
    CASH_DIVIDEND,
    SECURITIES_DISTRIBUTION,
    TOTAL_DISTRIBUTION_COMPONENT,
    FIXED_INCOME_COUPON,
    PRINCIPAL_REDEMPTION,
    CASH_COMPENSATION,
    RIGHTS_EVENT,
    SECURITY_REORGANIZATION,
    FEE,
    TAX,
    NONE,
    UNDEFINED
}
