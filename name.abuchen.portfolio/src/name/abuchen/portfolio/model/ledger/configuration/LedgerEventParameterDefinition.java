package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Describes Ledger configuration for entries, postings, parameters, or native shapes.
 * This metadata is used by Ledger validation and assembly infrastructure. It is not a
 * normal transaction-editing API.
 *
 * <p>
 * This class is runtime-only configuration. It lists parameter types that may describe an
 * event; the persisted file stores concrete {@code LedgerParameter} values instead.
 * </p>
 */
public final class LedgerEventParameterDefinition
{
    private static final Set<LedgerParameterType> PARAMETER_TYPES = Collections
                    .unmodifiableSet(EnumSet.of(LedgerParameterType.CORPORATE_ACTION_KIND,
                                    LedgerParameterType.CORPORATE_ACTION_SUBTYPE,
                                    LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.EVENT_STAGE,
                                    LedgerParameterType.EX_DATE, LedgerParameterType.RECORD_DATE,
                                    LedgerParameterType.PAYMENT_DATE, LedgerParameterType.EFFECTIVE_DATE,
                                    LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.ELECTION_DEADLINE));

    private LedgerEventParameterDefinition()
    {
    }

    public static Set<LedgerParameterType> getParameterTypes()
    {
        return PARAMETER_TYPES;
    }

    public static boolean supportsParameterType(LedgerParameterType parameterType)
    {
        return PARAMETER_TYPES.contains(parameterType);
    }
}
