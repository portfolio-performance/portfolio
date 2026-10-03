package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class GeneratePlanTest
{
    private TransactionFixture f;
    private InvestmentPlan plan;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();

        var start = LocalDate.now().minusMonths(2).minusDays(3);
        InvestmentPlansHandler.create(f.context(false, null), new IdempotencyIndex(),
                        json("{'name':'ETF','kind':'purchase','instrument':'" + f.eurSecurity.getUUID()
                                        + "','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'"
                                        + f.cash.getUUID() + "','start':'" + start
                                        + "','intervalMonths':1,'amount':'100'}"));
        plan = f.client.getPlans().get(0);
        f.file.setDirty(false);
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private void addPrice()
    {
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2000-01-03"), Values.Quote.factorize(20)));
    }

    @Test
    public void testDryRunListsTheDueDates()
    {
        addPrice();

        var result = InvestmentPlansHandler.generate(f.context(true, null), "ETF");

        var dates = plan.getDatesOfTransactionsToBeGenerated();
        assertThat(dates.size() >= 3, is(true));
        assertThat(result.get("dryRun").getAsBoolean(), is(true));
        assertThat(result.get("count").getAsInt(), is(dates.size()));
        assertThat(result.getAsJsonArray("dates").get(0).getAsString(), is(dates.get(0).toString()));
        assertThat(plan.getTransactions().isEmpty(), is(true));
        assertThat(f.broker.getTransactions().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testGeneratesTheDueTransactions()
    {
        addPrice();
        var dates = plan.getDatesOfTransactionsToBeGenerated();

        var result = InvestmentPlansHandler.generate(f.context(false, null), "ETF");

        assertThat(result.get("count").getAsInt(), is(dates.size()));
        assertThat(plan.getTransactions().size(), is(dates.size()));
        assertThat(f.broker.getTransactions().size(), is(dates.size()));
        assertThat(f.cash.getTransactions().size(), is(dates.size()));
        assertThat(f.broker.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.BUY));

        var first = result.getAsJsonArray("transactions").get(0).getAsJsonObject();
        assertThat(first.get("type").getAsString(), is("buy"));
        assertThat(first.getAsJsonObject("linked").getAsJsonObject("owner").get("uuid").getAsString(),
                        is(f.cash.getUUID()));
        assertThat(f.file.isDirty(), is(true));

        // nothing is due any more: no change, the file stays clean
        f.file.setDirty(false);
        result = InvestmentPlansHandler.generate(f.context(false, null), "ETF");
        assertThat(result.get("count").getAsInt(), is(0));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testMissingPriceGeneratesNothing()
    {
        try
        {
            InvestmentPlansHandler.generate(f.context(false, null), "ETF");
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(409));
            assertThat(e.getType(), is("missing-price"));
            assertThat(e.getDetail().contains("EUR Share"), is(true));
        }
        assertThat(plan.getTransactions().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testThroughRouter() throws Exception
    {
        addPrice();

        var response = f.call("POST", "/investment-plans/ETF/actions/generate", Map.of("dry_run", "true"), null);
        assertThat(response.status(), is(200));
        assertThat(plan.getTransactions().isEmpty(), is(true));

        f.host().setUserEditing(true);
        try
        {
            f.call("POST", "/investment-plans/ETF/actions/generate", Map.of(), null);
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
        f.host().setUserEditing(false);

        response = f.call("POST", "/investment-plans/ETF/actions/generate", Map.of(), null);
        assertThat(body(response).get("count").getAsInt(), is(plan.getTransactions().size()));
        assertThat(plan.getTransactions().isEmpty(), is(false));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));

        try
        {
            f.call("POST", "/investment-plans/nope/actions/generate", Map.of(), null);
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }
}
