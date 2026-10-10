package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.amount;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.InvestmentPlan;

/** Investment plans, addressed by name */
@SuppressWarnings("nls")
public class InvestmentPlansTest
{
    private TransactionFixture f;
    private IdempotencyIndex idempotency;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
        idempotency = new IdempotencyIndex();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private String purchase(String name)
    {
        return "{'name':'" + name + "','kind':'purchase','instrument':'" + f.eurSecurity.getUUID()
                        + "','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'" + f.cash.getUUID()
                        + "','start':'2026-01-15','intervalMonths':3,'amount':'250','fees':'1.50',"
                        + "'autoGenerate':true,'note':'n'}";
    }

    private InvestmentPlan plan(String name)
    {
        return f.client.getPlans().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    private MasterDataWrites.WriteResult create(String body)
    {
        return InvestmentPlansHandler.create(f.context(false, null), idempotency, json(body));
    }

    private static ApiException fails(Runnable runnable)
    {
        try
        {
            runnable.run();
            Assert.fail("expected ApiException");
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    @Test
    public void testCreatePurchasePlan()
    {
        var result = create(purchase("ETF"));

        var plan = plan("ETF");
        assertThat(plan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));
        assertThat(plan.getSecurity(), is(f.eurSecurity));
        assertThat(plan.getPortfolio(), is(f.broker));
        assertThat(plan.getAccount(), is(f.cash));
        assertThat(plan.getStart(), is(LocalDate.parse("2026-01-15")));
        assertThat(plan.getInterval(), is(3));
        assertThat(plan.getAmount(), is(amount("250")));
        assertThat(plan.getFees(), is(amount("1.50")));
        assertThat(plan.isAutoGenerate(), is(true));
        assertThat(plan.getNote(), is("n"));
        assertThat(f.file.isDirty(), is(true));

        var json = result.entity();
        assertThat(json.get("kind").getAsString(), is("purchase"));
        assertThat(json.get("intervalMonths").getAsInt(), is(3));
        assertThat(json.getAsJsonObject("amount").get("value").getAsBigDecimal().compareTo(
                        new java.math.BigDecimal("250")), is(0));
        assertThat(json.get("nextTransactionDate").getAsString().isEmpty(), is(false));
    }

    @Test
    public void testCreateWeeklyDepositPlanWithDefaults()
    {
        create("{'name':'Savings','kind':'deposit','cashAccount':'" + f.usdCash.getUUID()
                        + "','intervalWeeks':2,'amount':'100'}");

        var plan = plan("Savings");
        assertThat(plan.getInterval(), is(InvestmentPlan.WEEKS_THRESHOLD + 2));
        assertThat(plan.getStart(), is(LocalDate.now()));
        assertThat(plan.getSecurity(), is(nullValue()));
        assertThat(InvestmentPlansHandler.get(f.client, "Savings").getAsJsonObject().get("intervalWeeks").getAsInt(),
                        is(2));
    }

    @Test
    public void testCreateValidation()
    {
        var e = fails(() -> create("{'kind':'saving','intervalMonths':1,'intervalWeeks':1,'fees':'-1'}"));
        assertError(e, "name", "required");
        assertError(e, "amount", "required");
        assertError(e, "kind", "invalid-value");
        assertError(e, "intervalWeeks", "invalid-value");
        assertError(e, "fees", "must-be-positive");

        e = fails(() -> create("{'name':'P','kind':'purchase','amount':'10','fees':'6','taxes':'4'}"));
        assertError(e, "instrument", "required");
        assertError(e, "investmentAccount", "required");
        assertError(e, "amount", "invalid-value");

        e = fails(() -> create("{'name':'D','kind':'deposit','amount':'10','fees':'1','instrument':'"
                        + f.eurSecurity.getUUID() + "'}"));
        assertError(e, "cashAccount", "required");
        assertError(e, "fees", "not-allowed-for-type");
        assertError(e, "instrument", "not-allowed-for-type");

        e = fails(() -> create("{'name':'I','kind':'interest','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'10','taxes':'2','fees':'1','intervalMonths':100}"));
        assertError(e, "intervalMonths", "invalid-value");

        e = fails(() -> create("{'name':'I','kind':'interest','cashAccount':'nope','amount':'0'}"));
        assertError(e, "cashAccount", "unknown-reference");
        assertError(e, "amount", "must-be-positive");

        assertThat(f.client.getPlans(), is(empty()));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testInterestPlanMayHaveTaxes()
    {
        create("{'name':'I','kind':'interest','cashAccount':'" + f.cash.getUUID() + "','amount':'10','taxes':'2'}");

        assertThat(plan("I").getTaxes(), is(amount("2")));
    }

    @Test
    public void testNameMustBeUnique()
    {
        create(purchase("ETF"));

        assertError(fails(() -> create(purchase("ETF"))), "name", "already-exists");
        assertThat(f.client.getPlans().size(), is(1));
    }

    @Test
    public void testCreateDryRun()
    {
        var result = InvestmentPlansHandler.create(f.context(true, null), idempotency, json(purchase("ETF")));

        assertThat(result.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.client.getPlans(), is(empty()));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatch()
    {
        create(purchase("ETF"));
        f.file.setDirty(false);

        var result = InvestmentPlansHandler.patch(f.context(false, null), "ETF",
                        json("{'name':'ETF monthly','intervalMonths':1,'cashAccount':null,'fees':null,'note':null}"));

        assertThat(result.changed(), is(true));
        var plan = plan("ETF monthly");
        assertThat(plan.getInterval(), is(1));
        assertThat(plan.getAccount(), is(nullValue()));
        assertThat(plan.getFees(), is(0L));
        assertThat(plan.getNote(), is(nullValue()));
        assertThat(result.entity().has("cashAccount"), is(false));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testPatchRulesDryRunAndNoOp()
    {
        create(purchase("ETF"));
        f.file.setDirty(false);

        assertError(fails(() -> InvestmentPlansHandler.patch(f.context(false, null), "ETF",
                        json("{'kind':'deposit'}"))), "kind", "not-allowed-for-type");
        assertError(fails(() -> InvestmentPlansHandler.patch(f.context(false, null), "ETF",
                        json("{'instrument':null}"))), "instrument", "required");

        var dry = InvestmentPlansHandler.patch(f.context(true, null), "ETF", json("{'amount':'500'}"));
        assertThat(dry.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(plan("ETF").getAmount(), is(amount("250")));

        var noop = InvestmentPlansHandler.patch(f.context(false, null), "ETF",
                        json("{'kind':'purchase','amount':'250.00','start':'2026-01-15'}"));
        assertThat(noop.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDeleteKeepsTransactions()
    {
        create(purchase("ETF"));
        var deposit = f.create("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'10'}");
        var transaction = f.find(deposit.get("uuid").getAsString()).getTransaction();
        plan("ETF").getTransactions().add(transaction);
        f.file.setDirty(false);

        var preview = InvestmentPlansHandler.delete(f.context(true, null), "ETF");
        assertThat(preview.getAsJsonArray("removed").get(0).getAsJsonObject().get("transactionCount").getAsInt(),
                        is(1));
        assertThat(f.client.getPlans().size(), is(1));
        assertThat(f.file.isDirty(), is(false));

        InvestmentPlansHandler.delete(f.context(false, null), "ETF");
        assertThat(f.client.getPlans(), is(empty()));
        assertThat(f.cash.getTransactions().contains(transaction), is(true));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testThroughRouter() throws Exception
    {
        var created = f.call("POST", "/investment-plans", Map.of(),
                        purchase("My Plan").replace("{'name'", "{'clientRef':'plan-1','name'"));
        assertThat(created.status(), is(201));
        assertThat(created.headers().get("Location"), is("/v1/files/" + f.fileId() + "/investment-plans/My%20Plan"));

        var replay = f.call("POST", "/investment-plans", Map.of(),
                        purchase("Other").replace("{'name'", "{'clientRef':'plan-1','name'"));
        assertThat(replay.status(), is(200));
        assertThat(body(replay).get("name").getAsString(), is("My Plan"));

        var patched = f.call("PATCH", "/investment-plans/My%20Plan", Map.of(), "{'autoGenerate':false}");
        assertThat(body(patched).get("autoGenerate").getAsBoolean(), is(false));

        var list = f.call("GET", "/investment-plans", Map.of(), null);
        assertThat(body(list).getAsJsonArray("items").size(), is(1));

        assertThat(f.call("DELETE", "/investment-plans/My%20Plan", Map.of(), null).status(), is(204));
        assertThat(f.client.getPlans(), is(empty()));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
