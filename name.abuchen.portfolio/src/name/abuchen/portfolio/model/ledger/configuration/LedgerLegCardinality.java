package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines how often a configured Ledger leg may appear inside one native entry.
 * This is Java-only configuration metadata. Persisted files store concrete
 * postings and semantic projection facts, not leg cardinality rules.
 */
public enum LedgerLegCardinality
{
    EXACTLY_ONE,
    AT_LEAST_ONE,
    OPTIONAL,
    REPEATABLE
}
