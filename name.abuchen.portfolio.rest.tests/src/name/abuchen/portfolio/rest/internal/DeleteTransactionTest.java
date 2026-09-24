package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class DeleteTransactionTest
{
    private TransactionFixture f;

    private BuySellEntry buy;
    private String buyUuid;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();

        buyUuid = f.create("{'type':'buy','date':'2026-03-02','investmentAccount':'" + f.broker.getUUID()
                        + "','cashAccount':'" + f.cash.getUUID() + "','instrument':'" + f.eurSecurity.getUUID()
                        + "','shares':'10','quote':'100'}").get("uuid").getAsString();
        buy = (BuySellEntry) f.find(buyUuid).getTransaction().getCrossEntry();
        f.file.setDirty(false);
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    @Test
    public void testDeleteRemovesBothLegs()
    {
        var preview = TransactionsHandler.delete(f.context(false, null), buyUuid);

        assertThat(preview, nullValue());
        assertThat(f.broker.getTransactions().isEmpty(), is(true));
        assertThat(f.cash.getTransactions().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDeleteByTheOtherLeg()
    {
        TransactionsHandler.delete(f.context(false, null), buy.getAccountTransaction().getUUID());
        assertThat(f.legCount(), is(0));
    }

    @Test
    public void testDeleteRemovesPlanLink()
    {
        var plan = new InvestmentPlan("plan");
        plan.setSecurity(f.eurSecurity);
        plan.setPortfolio(f.broker);
        plan.setAccount(f.cash);
        plan.setStart(LocalDate.of(2026, 1, 1));
        plan.getTransactions().add(buy.getPortfolioTransaction());
        f.client.addPlan(plan);

        TransactionsHandler.delete(f.context(false, null), buyUuid);

        assertThat(plan.getTransactions().isEmpty(), is(true));
    }

    @Test
    public void testDeleteTransferRemovesBothLegs()
    {
        var uuid = f.create("{'type':'cash-transfer','date':'2026-05-01','fromCashAccount':'" + f.cash.getUUID()
                        + "','toCashAccount':'" + f.cash2.getUUID() + "','amount':'5'}").get("uuid").getAsString();

        TransactionsHandler.delete(f.context(false, null), uuid);

        assertThat(f.cash2.getTransactions().isEmpty(), is(true));
        assertThat(f.cash.getTransactions().size(), is(1)); // the buy's cash leg
    }

    @Test
    public void testDryRunReportsBothLegsAndDeletesNothing()
    {
        var preview = TransactionsHandler.delete(f.context(true, null), buyUuid);

        assertThat(preview.get("dryRun").getAsBoolean(), is(true));
        var removed = new ArrayList<String>();
        preview.getAsJsonArray("removed").forEach(e -> removed.add(e.getAsJsonObject().get("uuid").getAsString()));
        assertThat(removed, containsInAnyOrder(buyUuid, buy.getAccountTransaction().getUUID()));

        assertThat(f.legCount(), is(2));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDeleteThroughRouter() throws Exception
    {
        var dry = f.call("DELETE", "/transactions/" + buyUuid, Map.of("dry_run", "true"), null);
        assertThat(dry.status(), is(200));
        assertThat(f.legCount(), is(2));

        var real = f.call("DELETE", "/transactions/" + buyUuid, Map.of(), null);
        assertThat(real.status(), is(204));
        assertThat(f.legCount(), is(0));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));

        // gone: 404 afterwards, for either leg
        for (Transaction tx : new Transaction[] { buy.getPortfolioTransaction(), buy.getAccountTransaction() })
        {
            try
            {
                f.call("GET", "/transactions/" + tx.getUUID(), Map.of(), null);
                org.junit.Assert.fail("expected ApiException");
            }
            catch (ApiException e)
            {
                assertThat(e.getStatus(), is(404));
            }
        }
    }

    @Test
    public void testDeleteIs423WhileUserEditing() throws Exception
    {
        f.host().setUserEditing(true);
        try
        {
            f.call("DELETE", "/transactions/" + buyUuid, Map.of(), null);
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
        assertThat(f.legCount(), is(2));
    }

    @Test
    public void testDeleteUnknownIs404()
    {
        try
        {
            TransactionsHandler.delete(f.context(false, null), "no-such-uuid");
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
        assertThat(f.file.isDirty(), is(false));
    }
}
