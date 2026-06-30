package name.abuchen.portfolio.model.ledger.configuration.rule;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Describes a validation rule for Ledger entry configuration.
 * This is configuration metadata used by Ledger infrastructure. Normal transaction-editing
 * code should rely on creators, editors, converters, or validators that consume these rules.
 *
 * <p>
 * Rule objects are not persisted as standalone data. They describe which persisted
 * projection roles are expected and whether a projection must point to a posting or group.
 * </p>
 */
public final class LedgerProjectionRule
{
    private final LedgerProjectionRole role;
    private final LedgerRequirement requirement;
    private final boolean primaryPostingExpected;
    private final boolean postingGroupExpected;

    private LedgerProjectionRule(LedgerProjectionRole role, LedgerRequirement requirement,
                    boolean primaryPostingExpected, boolean postingGroupExpected)
    {
        this.role = Objects.requireNonNull(role);
        this.requirement = Objects.requireNonNull(requirement);
        this.primaryPostingExpected = primaryPostingExpected;
        this.postingGroupExpected = postingGroupExpected;
    }

    public static LedgerProjectionRule required(LedgerProjectionRole role, boolean primaryPostingExpected,
                    boolean postingGroupExpected)
    {
        return of(role, LedgerRequirement.REQUIRED, primaryPostingExpected, postingGroupExpected);
    }

    public static LedgerProjectionRule optional(LedgerProjectionRole role, boolean primaryPostingExpected,
                    boolean postingGroupExpected)
    {
        return of(role, LedgerRequirement.OPTIONAL, primaryPostingExpected, postingGroupExpected);
    }

    private static LedgerProjectionRule of(LedgerProjectionRole role, LedgerRequirement requirement,
                    boolean primaryPostingExpected, boolean postingGroupExpected)
    {
        return new LedgerProjectionRule(role, requirement, primaryPostingExpected, postingGroupExpected);
    }

    public LedgerProjectionRole getRole()
    {
        return role;
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

    public boolean isPrimaryPostingExpected()
    {
        return primaryPostingExpected;
    }

    public boolean isPostingGroupExpected()
    {
        return postingGroupExpected;
    }
}
