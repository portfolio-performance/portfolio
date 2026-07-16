package name.abuchen.portfolio.model.ledger.configuration.rule;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;

/**
 * Describes a validation rule for Ledger entry configuration.
 * This is configuration metadata used by Ledger infrastructure. Normal transaction-editing
 * code should rely on creators, editors, converters, or validators that consume these rules.
 *
 * <p>
 * Rule objects are not persisted as standalone data. They describe which persisted
 * parameter type codes are required, optional, or repeatable for a configured entry shape.
 * </p>
 */
public final class LedgerParameterRule
{
    private final LedgerParameterType parameterType;
    private final LedgerRequirement requirement;
    private final boolean repeatable;

    private LedgerParameterRule(LedgerParameterType parameterType, LedgerRequirement requirement, boolean repeatable)
    {
        this.parameterType = Objects.requireNonNull(parameterType);
        this.requirement = Objects.requireNonNull(requirement);
        this.repeatable = repeatable;
    }

    public static LedgerParameterRule required(LedgerParameterType parameterType)
    {
        return of(parameterType, LedgerRequirement.REQUIRED, false);
    }

    public static LedgerParameterRule optional(LedgerParameterType parameterType)
    {
        return of(parameterType, LedgerRequirement.OPTIONAL, false);
    }

    public static LedgerParameterRule repeatable(LedgerParameterType parameterType, LedgerRequirement requirement)
    {
        return of(parameterType, requirement, true);
    }

    private static LedgerParameterRule of(LedgerParameterType parameterType, LedgerRequirement requirement,
                    boolean repeatable)
    {
        return new LedgerParameterRule(parameterType, requirement, repeatable);
    }

    public LedgerParameterType getParameterType()
    {
        return parameterType;
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

    public boolean isRepeatable()
    {
        return repeatable;
    }
}
