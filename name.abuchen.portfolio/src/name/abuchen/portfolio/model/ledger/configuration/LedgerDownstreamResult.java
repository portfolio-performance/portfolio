package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the downstream result Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * These values are runtime configuration markers. They are not persisted in Ledger entries;
 * persisted files store the facts from which downstream results may later be derived.
 * </p>
 */
public enum LedgerDownstreamResult
{
    FIFO_RESULT,
    COST_BASIS_RESULT,
    TAX_RESULT,
    PERFORMANCE_RESULT,
    REPORT_RESULT,
    SNAPSHOT_RESULT
}
