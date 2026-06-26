package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Describes Ledger configuration for entries, postings, parameters, or native shapes.
 * This metadata is used by Ledger validation and assembly infrastructure. It is not a
 * normal transaction-editing API.
 *
 * <p>
 * This definition is not persisted as a standalone object. It defines which parameter
 * types are meaningful for postings that are persisted with a stable posting type code.
 * </p>
 */
public final class LedgerPostingTypeDefinition
{
    private final LedgerPostingType postingType;
    private final Set<LedgerParameterType> componentParameterTypes;

    private LedgerPostingTypeDefinition(LedgerPostingType postingType, Set<LedgerParameterType> componentParameterTypes)
    {
        this.postingType = Objects.requireNonNull(postingType);
        this.componentParameterTypes = copyOf(componentParameterTypes);
    }

    static LedgerPostingTypeDefinition of(LedgerPostingType postingType,
                    Set<LedgerParameterType> componentParameterTypes)
    {
        return new LedgerPostingTypeDefinition(postingType, componentParameterTypes);
    }

    public LedgerPostingType getPostingType()
    {
        return postingType;
    }

    public Set<LedgerParameterType> getComponentParameterTypes()
    {
        return componentParameterTypes;
    }

    public boolean supportsParameterType(LedgerParameterType parameterType)
    {
        return componentParameterTypes.contains(parameterType);
    }

    private static Set<LedgerParameterType> copyOf(Set<LedgerParameterType> values)
    {
        Objects.requireNonNull(values);

        var copy = EnumSet.noneOf(LedgerParameterType.class);

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }
}
