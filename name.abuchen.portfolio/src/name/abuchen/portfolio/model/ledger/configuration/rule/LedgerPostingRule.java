package name.abuchen.portfolio.model.ledger.configuration.rule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Describes a validation rule for Ledger entry configuration.
 * This is configuration metadata used by Ledger infrastructure. Normal transaction-editing
 * code should rely on creators, editors, converters, or validators that consume these rules.
 *
 * <p>
 * Rule objects are not persisted as standalone data. They describe which persisted posting
 * type codes and parameter type codes are expected for a configured entry shape.
 * </p>
 */
public final class LedgerPostingRule
{
    private final LedgerPostingType postingType;
    private final LedgerRequirement requirement;
    private final Set<LedgerParameterType> requiredParameterTypes;
    private final Set<LedgerParameterType> optionalParameterTypes;
    private final Set<LedgerParameterType> repeatableParameterTypes;

    private LedgerPostingRule(LedgerPostingType postingType, LedgerRequirement requirement,
                    Set<LedgerParameterType> requiredParameterTypes, Set<LedgerParameterType> optionalParameterTypes,
                    Set<LedgerParameterType> repeatableParameterTypes)
    {
        this.postingType = Objects.requireNonNull(postingType);
        this.requirement = Objects.requireNonNull(requirement);
        this.requiredParameterTypes = copyOf(requiredParameterTypes);
        this.optionalParameterTypes = copyOf(optionalParameterTypes);
        this.repeatableParameterTypes = copyOf(repeatableParameterTypes);
    }

    public static LedgerPostingRule required(LedgerPostingType postingType,
                    Set<LedgerParameterType> requiredParameterTypes, Set<LedgerParameterType> optionalParameterTypes)
    {
        return of(postingType, LedgerRequirement.REQUIRED, requiredParameterTypes, optionalParameterTypes,
                        EnumSet.noneOf(LedgerParameterType.class));
    }

    public static LedgerPostingRule optional(LedgerPostingType postingType,
                    Set<LedgerParameterType> requiredParameterTypes, Set<LedgerParameterType> optionalParameterTypes)
    {
        return of(postingType, LedgerRequirement.OPTIONAL, requiredParameterTypes, optionalParameterTypes,
                        EnumSet.noneOf(LedgerParameterType.class));
    }

    private static LedgerPostingRule of(LedgerPostingType postingType, LedgerRequirement requirement,
                    Set<LedgerParameterType> requiredParameterTypes, Set<LedgerParameterType> optionalParameterTypes,
                    Set<LedgerParameterType> repeatableParameterTypes)
    {
        return new LedgerPostingRule(postingType, requirement, requiredParameterTypes, optionalParameterTypes,
                        repeatableParameterTypes);
    }

    public LedgerPostingType getPostingType()
    {
        return postingType;
    }

    public LedgerRequirement getRequirement()
    {
        return requirement;
    }

    public boolean isRequired()
    {
        return requirement == LedgerRequirement.REQUIRED;
    }

    public boolean isOptional()
    {
        return requirement == LedgerRequirement.OPTIONAL;
    }

    public Set<LedgerParameterType> getRequiredParameterTypes()
    {
        return requiredParameterTypes;
    }

    public Set<LedgerParameterType> getOptionalParameterTypes()
    {
        return optionalParameterTypes;
    }

    public Set<LedgerParameterType> getRepeatableParameterTypes()
    {
        return repeatableParameterTypes;
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
