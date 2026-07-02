package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;

/**
 * Tests helper rules used when generated bookings are converted.
 * Plan executions are linked by plan key and LedgerEntry metadata, so projection-ref
 * conversion helpers no longer rewrite or reject InvestmentPlan refs.
 */
@SuppressWarnings("nls")
public class LedgerInvestmentPlanRefSupportTest
{
    /**
     * Verifies that splitting a transfer does not rewrite legacy execution refs.
     * New plan linkage follows entry metadata, not projection UUID side mappings.
     */
    @Test
    public void testSplitUpdatesDoNotRewriteExecutionRefs()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(), null,
                                        LedgerProjectionRole.SOURCE_ACCOUNT));

        LedgerInvestmentPlanRefSupport.prepareAccountTransferSplitExecutionRefUpdates(fixture.client(),
                        fixture.transferEntry(), LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT,
                        fixture.removalEntry(), fixture.depositEntry()).apply();

        assertExecutionRef(plan, fixture.transferEntry().getUUID(), null, LedgerProjectionRole.SOURCE_ACCOUNT);
    }

    /**
     * Verifies that role-change validation no longer blocks conversions based on
     * projection-scoped plan identity.
     */
    @Test
    public void testRoleChangeHelpersIgnoreProjectionScopedPlanRefs()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(), null, null));

        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(
                        name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport
                                        .runtimeProjectionId(fixture.transferEntry(), LedgerProjectionRole.SOURCE_ACCOUNT),
                        LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT);

        LedgerInvestmentPlanRefSupport.requireCurrentRefsResolveUniquely(fixture.client(), fixture.transferEntry());
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(fixture.client(), fixture.transferEntry(),
                        roleChange);
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(fixture.client(), fixture.transferEntry(), roleChange);

        assertExecutionRef(plan, fixture.transferEntry().getUUID(), null, null);
    }

    private InvestmentPlan planWithRef(Client client, InvestmentPlan.LedgerExecutionRef ref)
    {
        var plan = new InvestmentPlan("Plan");

        plan.addLedgerExecutionRef(ref);
        client.addPlan(plan);

        return plan;
    }

    private void assertExecutionRef(InvestmentPlan plan, String entryUUID, String projectionUUID,
                    LedgerProjectionRole role)
    {
        var ref = plan.getLedgerExecutionRefs().get(0);

        assertThat(ref.getLedgerEntryUUID(), is(entryUUID));
        assertThat(ref.getProjectionUUID(), projectionUUID == null ? nullValue() : is(projectionUUID));
        assertThat(ref.getProjectionRole(), role == null ? nullValue() : is(role));
    }

    private Fixture fixture()
    {
        var client = new Client();
        var transferEntry = entry("transfer-entry", LedgerEntryType.CASH_TRANSFER);
        var removalEntry = entry("removal-entry", LedgerEntryType.REMOVAL);
        var depositEntry = entry("deposit-entry", LedgerEntryType.DEPOSIT);
        return new Fixture(client, transferEntry, removalEntry, depositEntry);
    }

    private LedgerEntry entry(String uuid, LedgerEntryType type)
    {
        var entry = new LedgerEntry(uuid);

        entry.setType(type);

        return entry;
    }

    private record Fixture(Client client, LedgerEntry transferEntry, LedgerEntry removalEntry, LedgerEntry depositEntry)
    {
    }
}
