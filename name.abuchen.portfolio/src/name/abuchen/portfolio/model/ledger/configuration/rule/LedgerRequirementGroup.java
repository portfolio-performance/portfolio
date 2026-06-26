package name.abuchen.portfolio.model.ledger.configuration.rule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Describes a validation rule for Ledger entry configuration.
 * This is configuration metadata used by Ledger infrastructure. Normal transaction-editing
 * code should rely on creators, editors, converters, or validators that consume these rules.
 *
 * <p>
 * Requirement groups are runtime configuration only. They describe alternative sets of
 * persisted facts that may satisfy a configured entry shape.
 * </p>
 */
public final class LedgerRequirementGroup
{
    private final String name;
    private final LedgerRequirement requirement;
    private final Set<LedgerPostingType> postingTypes;
    private final Set<LedgerParameterType> parameterTypes;

    private LedgerRequirementGroup(String name, LedgerRequirement requirement, Set<LedgerPostingType> postingTypes,
                    Set<LedgerParameterType> parameterTypes)
    {
        this.name = requireName(name);
        this.requirement = Objects.requireNonNull(requirement);
        this.postingTypes = copyPostingTypes(postingTypes);
        this.parameterTypes = copyParameterTypes(parameterTypes);

        if (this.postingTypes.isEmpty() && this.parameterTypes.isEmpty())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_025
                            .message("Ledger requirement group must contain postings or parameters")); //$NON-NLS-1$
    }

    public static LedgerRequirementGroup postingTypes(String name, LedgerRequirement requirement,
                    Set<LedgerPostingType> postingTypes)
    {
        return new LedgerRequirementGroup(name, requirement, postingTypes, EnumSet.noneOf(LedgerParameterType.class));
    }

    public static LedgerRequirementGroup parameterTypes(String name, LedgerRequirement requirement,
                    Set<LedgerParameterType> parameterTypes)
    {
        return new LedgerRequirementGroup(name, requirement, EnumSet.noneOf(LedgerPostingType.class), parameterTypes);
    }

    public String getName()
    {
        return name;
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

    public Set<LedgerPostingType> getPostingTypes()
    {
        return postingTypes;
    }

    public Set<LedgerParameterType> getParameterTypes()
    {
        return parameterTypes;
    }

    private static String requireName(String name)
    {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_026.message("Ledger requirement group name is required")); //$NON-NLS-1$

        return name;
    }

    private static Set<LedgerPostingType> copyPostingTypes(Set<LedgerPostingType> values)
    {
        Objects.requireNonNull(values);

        var copy = EnumSet.noneOf(LedgerPostingType.class);

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }

    private static Set<LedgerParameterType> copyParameterTypes(Set<LedgerParameterType> values)
    {
        Objects.requireNonNull(values);

        var copy = EnumSet.noneOf(LedgerParameterType.class);

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }
}
