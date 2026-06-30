package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the performance treatment Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * These values are runtime configuration markers. They are not persisted in Ledger entries
 * and do not implement performance calculation by themselves.
 * </p>
 */
public enum LedgerPerformanceTreatment
{
    EXTERNAL_CASH_FLOW,
    INTERNAL_RECLASSIFICATION,
    SECURITY_DISTRIBUTION,
    INCOME_DISTRIBUTION,
    PRINCIPAL_RETURN,
    COST_BASIS_REALLOCATION,
    PERFORMANCE_NEUTRAL,
    REALIZED_GAIN_RELEVANT,
    VALUATION_ONLY,
    UNDEFINED
}
