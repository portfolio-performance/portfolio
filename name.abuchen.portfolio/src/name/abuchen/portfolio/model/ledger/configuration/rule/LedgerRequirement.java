package name.abuchen.portfolio.model.ledger.configuration.rule;

/**
 * Describes a validation rule for Ledger entry configuration.
 * This is configuration metadata used by Ledger infrastructure. Normal transaction-editing
 * code should rely on creators, editors, converters, or validators that consume these rules.
 *
 * <p>
 * Requirement values are runtime configuration only. Persisted files store the actual
 * Ledger facts; these values describe whether a configured fact is expected.
 * </p>
 */
public enum LedgerRequirement
{
    REQUIRED,
    OPTIONAL
}
