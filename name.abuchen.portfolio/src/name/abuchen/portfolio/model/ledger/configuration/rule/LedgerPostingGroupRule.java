package name.abuchen.portfolio.model.ledger.configuration.rule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Describes a validation rule for Ledger entry configuration.
 * This is configuration metadata used by Ledger infrastructure. Normal transaction-editing
 * code should rely on creators, editors, converters, or validators that consume these rules.
 *
 * <p>
 * Rule objects are not persisted as standalone data. They describe how persisted postings
 * and projections may be grouped for a configured entry shape.
 * </p>
 */
public final class LedgerPostingGroupRule
{
    private final String name;
    private final LedgerRequirement requirement;
    private final Set<LedgerPostingType> postingTypes;
    private final Set<LedgerProjectionRole> projectionRoles;
    private final boolean groupAnchorExpected;

    private LedgerPostingGroupRule(String name, LedgerRequirement requirement, Set<LedgerPostingType> postingTypes,
                    Set<LedgerProjectionRole> projectionRoles, boolean groupAnchorExpected)
    {
        this.name = requireName(name);
        this.requirement = Objects.requireNonNull(requirement);
        this.postingTypes = copyPostingTypes(postingTypes);
        this.projectionRoles = copyProjectionRoles(projectionRoles);
        this.groupAnchorExpected = groupAnchorExpected;
    }

    public static LedgerPostingGroupRule of(String name, LedgerRequirement requirement,
                    Set<LedgerPostingType> postingTypes, Set<LedgerProjectionRole> projectionRoles,
                    boolean groupAnchorExpected)
    {
        return new LedgerPostingGroupRule(name, requirement, postingTypes, projectionRoles, groupAnchorExpected);
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

    public Set<LedgerProjectionRole> getProjectionRoles()
    {
        return projectionRoles;
    }

    public boolean isGroupAnchorExpected()
    {
        return groupAnchorExpected;
    }

    private static String requireName(String name)
    {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_024.message("Ledger posting group rule name is required")); //$NON-NLS-1$

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

    private static Set<LedgerProjectionRole> copyProjectionRoles(Set<LedgerProjectionRole> values)
    {
        Objects.requireNonNull(values);

        var copy = EnumSet.noneOf(LedgerProjectionRole.class);

        for (var value : values)
            copy.add(Objects.requireNonNull(value));

        return Collections.unmodifiableSet(copy);
    }
}
