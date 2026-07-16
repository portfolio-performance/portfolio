package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Describes Ledger configuration for entries, postings, parameters, or native shapes.
 * This metadata is used by Ledger validation and assembly infrastructure. It is not a
 * normal transaction-editing API.
 *
 * <p>
 * Native entry shapes are not persisted as Ledger facts. They describe assembly and
 * validation policy for entry definitions; persisted files store the resulting entry type
 * id, postings, parameters, and projections.
 * </p>
 */
public enum LedgerNativeEntryShape
{
    SINGLE_INSTRUMENT,
    DUAL_INSTRUMENT,
    INSTRUMENT_PLUS_ACCOUNT,
    DUAL_INSTRUMENT_PLUS_ACCOUNT,
    UNDEFINED
}
