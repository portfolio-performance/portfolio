package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;

/**
 * Contributor-facing query facade over one native Corporate Action entry.
 */
public final class LedgerCorporateActionView
{
    private final LedgerEntry entry;

    private LedgerCorporateActionView(LedgerEntry entry)
    {
        this.entry = Objects.requireNonNull(entry);

        if (entry.getType() != LedgerEntryType.CORPORATE_ACTION)
            throw new IllegalArgumentException("Ledger entry is not a Corporate Action: " + entry.getType()); //$NON-NLS-1$
    }

    public static LedgerCorporateActionView of(LedgerEntry entry)
    {
        return new LedgerCorporateActionView(entry);
    }

    public LedgerEntry entry()
    {
        return entry;
    }

    public LedgerCorporateActionLegQuery legs()
    {
        return new LedgerCorporateActionLegQuery(entry);
    }

    public LedgerCorporateActionLegQuery legs(LedgerLegRole role)
    {
        return legs().withRole(role);
    }

    public LedgerCorporateActionLegQuery securityContexts()
    {
        return legs(LedgerLegRole.SECURITY_CONTEXT_LEG);
    }

    public LedgerCorporateActionLegQuery securityIn()
    {
        return legs(LedgerLegRole.TARGET_SECURITY_LEG);
    }

    public LedgerCorporateActionLegQuery rightsIn()
    {
        return legs(LedgerLegRole.DISTRIBUTED_RIGHT_LEG);
    }

    public LedgerCorporateActionLegQuery securityOut()
    {
        return legs(LedgerLegRole.SOURCE_SECURITY_LEG);
    }

    public LedgerCorporateActionLegQuery cash()
    {
        return legs(LedgerLegRole.CASH_LEG);
    }

    public LedgerCorporateActionLegQuery cashCompensation()
    {
        return legs(LedgerLegRole.CASH_COMPENSATION_LEG);
    }

    public LedgerCorporateActionLegQuery fees()
    {
        return legs(LedgerLegRole.FEE_LEG);
    }

    public LedgerCorporateActionLegQuery taxes()
    {
        return legs(LedgerLegRole.TAX_LEG);
    }

    public LedgerCorporateActionLegQuery principal()
    {
        return legs(LedgerLegRole.PRINCIPAL_REDEMPTION_LEG);
    }

    public LedgerCorporateActionLegQuery accruedInterest()
    {
        return legs(LedgerLegRole.ACCRUED_INTEREST_LEG);
    }
}
