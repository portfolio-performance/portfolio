package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;

/**
 * Tests helper rules used when generated transfer bookings are split.
 * These tests make sure plan references are moved only when the source or target side is unambiguous.
 */
@SuppressWarnings("nls")
public class LedgerInvestmentPlanRefSupportTest
{
    /**
     * Verifies that a plan reference known as the transfer source follows the new removal booking.
     * The planned update must point to the removal side before the converter applies the split.
     */
    @Test
    public void testSourceRoleOnlyExecutionRefMapsToRemovalProjectionUpdate()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(), null,
                                        LedgerProjectionRole.SOURCE_ACCOUNT));

        var updates = prepareSplitUpdates(fixture);

        assertExecutionRef(plan, fixture.transferEntry().getUUID(), null, LedgerProjectionRole.SOURCE_ACCOUNT);

        updates.apply();

        assertExecutionRef(plan, fixture.removalEntry().getUUID(), fixture.removalProjection().getUUID(),
                        LedgerProjectionRole.ACCOUNT);
    }

    /**
     * Verifies that a plan reference known as the transfer target follows the new deposit booking.
     * The planned update must point to the deposit side before the converter applies the split.
     */
    @Test
    public void testTargetRoleOnlyExecutionRefMapsToDepositProjectionUpdate()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(), null,
                                        LedgerProjectionRole.TARGET_ACCOUNT));

        var updates = prepareSplitUpdates(fixture);

        assertExecutionRef(plan, fixture.transferEntry().getUUID(), null, LedgerProjectionRole.TARGET_ACCOUNT);

        updates.apply();

        assertExecutionRef(plan, fixture.depositEntry().getUUID(), fixture.depositProjection().getUUID(),
                        LedgerProjectionRole.ACCOUNT);
    }

    /**
     * Verifies that a plan reference to the old source-side booking continues as a removal.
     * The prepared update must replace the old transfer projection with the new removal projection.
     */
    @Test
    public void testSourceProjectionExecutionRefMapsToRemovalProjectionUpdate()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(),
                                        fixture.sourceProjection().getUUID(), LedgerProjectionRole.SOURCE_ACCOUNT));

        prepareSplitUpdates(fixture).apply();

        assertExecutionRef(plan, fixture.removalEntry().getUUID(), fixture.removalProjection().getUUID(),
                        LedgerProjectionRole.ACCOUNT);
    }

    /**
     * Verifies that an unspecific plan reference blocks a transfer split.
     * Without a source or target side, the generated booking cannot be continued as either removal or deposit.
     */
    @Test
    public void testEntryOnlyExecutionRefRejectsSplitMapping()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(), null, null));

        var exception = assertThrows(UnsupportedOperationException.class, () -> prepareSplitUpdates(fixture));

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_045
                        .message("Ledger plan reference cannot be mapped to a split transfer side")));
        assertExecutionRef(plan, fixture.transferEntry().getUUID(), null, null);
    }

    /**
     * Verifies that a contradictory plan reference is rejected before the split can proceed.
     * A reference may not point to the source-side booking and be treated as the target side.
     */
    @Test
    public void testConflictingProjectionRoleRejectsSplitMapping()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(),
                                        fixture.sourceProjection().getUUID(), LedgerProjectionRole.TARGET_ACCOUNT));

        var exception = assertThrows(UnsupportedOperationException.class, () -> prepareSplitUpdates(fixture));

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_045
                        .message("Ledger plan reference cannot be mapped to a split transfer side")));
        assertExecutionRef(plan, fixture.transferEntry().getUUID(), fixture.sourceProjection().getUUID(),
                        LedgerProjectionRole.TARGET_ACCOUNT);
    }

    /**
     * Verifies that an ambiguous split target is rejected before any plan reference is changed.
     * The code must not choose between multiple possible removal bookings.
     */
    @Test
    public void testAmbiguousSplitTargetProjectionRejectsBeforeExecutionRefUpdate()
    {
        var fixture = fixture();
        var plan = planWithRef(fixture.client(),
                        new InvestmentPlan.LedgerExecutionRef(fixture.transferEntry().getUUID(),
                                        fixture.sourceProjection().getUUID(), LedgerProjectionRole.SOURCE_ACCOUNT));

        fixture.removalEntry().addProjectionRef(accountProjection("second-removal-projection"));

        var exception = assertThrows(UnsupportedOperationException.class, () -> prepareSplitUpdates(fixture));

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_PROJ_041
                        .message("Ledger plan reference cannot be mapped to a split transfer side")));
        assertExecutionRef(plan, fixture.transferEntry().getUUID(), fixture.sourceProjection().getUUID(),
                        LedgerProjectionRole.SOURCE_ACCOUNT);
    }

    private LedgerInvestmentPlanRefSupport.SplitExecutionRefUpdates prepareSplitUpdates(Fixture fixture)
    {
        return LedgerInvestmentPlanRefSupport.prepareAccountTransferSplitExecutionRefUpdates(fixture.client(),
                        fixture.transferEntry(), fixture.sourceProjection(), fixture.targetProjection(),
                        fixture.removalEntry(), fixture.depositEntry());
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
        var sourceProjection = projection("source-projection", LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = projection("target-projection", LedgerProjectionRole.TARGET_ACCOUNT);
        var removalProjection = accountProjection("removal-projection");
        var depositProjection = accountProjection("deposit-projection");

        transferEntry.addProjectionRef(sourceProjection);
        transferEntry.addProjectionRef(targetProjection);
        removalEntry.addProjectionRef(removalProjection);
        depositEntry.addProjectionRef(depositProjection);

        return new Fixture(client, transferEntry, sourceProjection, targetProjection, removalEntry, removalProjection,
                        depositEntry, depositProjection);
    }

    private LedgerEntry entry(String uuid, LedgerEntryType type)
    {
        var entry = new LedgerEntry(uuid);

        entry.setType(type);

        return entry;
    }

    private LedgerProjectionRef accountProjection(String uuid)
    {
        return projection(uuid, LedgerProjectionRole.ACCOUNT);
    }

    private LedgerProjectionRef projection(String uuid, LedgerProjectionRole role)
    {
        var projection = new LedgerProjectionRef(uuid);

        projection.setRole(role);

        return projection;
    }

    private record Fixture(Client client, LedgerEntry transferEntry, LedgerProjectionRef sourceProjection,
                    LedgerProjectionRef targetProjection, LedgerEntry removalEntry,
                    LedgerProjectionRef removalProjection, LedgerEntry depositEntry,
                    LedgerProjectionRef depositProjection)
    {
    }
}
