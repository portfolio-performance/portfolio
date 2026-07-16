package name.abuchen.portfolio.model.ledger.configuration.rule;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;

/**
 * Describes a required component/detail relation between two native Ledger legs.
 */
public final class LedgerComponentRequirement
{
    public enum Relation
    {
        SAME_GROUP_KEY
    }

    private final String name;
    private final LedgerLegRole primaryLegRole;
    private final LedgerLegRole componentLegRole;
    private final Relation relation;

    private LedgerComponentRequirement(String name, LedgerLegRole primaryLegRole, LedgerLegRole componentLegRole,
                    Relation relation)
    {
        this.name = requireName(name);
        this.primaryLegRole = Objects.requireNonNull(primaryLegRole);
        this.componentLegRole = Objects.requireNonNull(componentLegRole);
        this.relation = Objects.requireNonNull(relation);
    }

    public static LedgerComponentRequirement sameGroupKey(String name, LedgerLegRole primaryLegRole,
                    LedgerLegRole componentLegRole)
    {
        return new LedgerComponentRequirement(name, primaryLegRole, componentLegRole, Relation.SAME_GROUP_KEY);
    }

    public String getName()
    {
        return name;
    }

    public LedgerLegRole getPrimaryLegRole()
    {
        return primaryLegRole;
    }

    public LedgerLegRole getComponentLegRole()
    {
        return componentLegRole;
    }

    public Relation getRelation()
    {
        return relation;
    }

    private static String requireName(String name)
    {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_023.message("Ledger component requirement name is required")); //$NON-NLS-1$

        return name;
    }
}
