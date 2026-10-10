package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;

/** {@code POST}, {@code PATCH} and {@code DELETE /investment-accounts} */
@SuppressWarnings("nls")
public class PortfoliosWriteTest
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

    private Portfolio find(String uuid)
    {
        return f.client.getPortfolios().stream().filter(p -> p.getUUID().equals(uuid)).findFirst().orElse(null);
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
    public void testCreate()
    {
        var result = PortfoliosHandler.create(f.context(false, null), idempotency,
                        json("{'name':'Depot','referenceCashAccount':'" + f.usdCash.getUUID() + "','note':'n'}"));

        var portfolio = find(result.entity().get("uuid").getAsString());
        assertThat(portfolio.getName(), is("Depot"));
        assertThat(portfolio.getReferenceAccount(), is(f.usdCash));
        assertThat(portfolio.getNote(), is("n"));
        assertThat(result.entity().get("referenceCashAccount").getAsString(), is(f.usdCash.getUUID()));
        assertThat(result.entity().has("value"), is(true));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCreateValidation()
    {
        var count = f.client.getPortfolios().size();

        var e = fails(() -> PortfoliosHandler.create(f.context(false, null), idempotency, json("{'x':1}")));
        assertError(e, "name", "required");
        assertError(e, "referenceCashAccount", "required");
        assertError(e, "x", "unknown-field");

        assertError(fails(() -> PortfoliosHandler.create(f.context(false, null), idempotency,
                        json("{'name':'D','referenceCashAccount':'nope'}"))), "referenceCashAccount",
                        "unknown-reference");

        f.usdCash.setRetired(true);
        assertError(fails(() -> PortfoliosHandler.create(f.context(false, null), idempotency,
                        json("{'name':'D','referenceCashAccount':'" + f.usdCash.getUUID() + "'}"))),
                        "referenceCashAccount", "invalid-value");

        assertThat(f.client.getPortfolios().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testCreateDryRun()
    {
        var count = f.client.getPortfolios().size();

        var result = PortfoliosHandler.create(f.context(true, null), idempotency,
                        json("{'name':'D','referenceCashAccount':'" + f.cash.getUUID() + "'}"));

        assertThat(result.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.client.getPortfolios().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatch()
    {
        var result = PortfoliosHandler.patch(f.context(false, null), f.broker.getUUID(),
                        json("{'name':'Main','referenceCashAccount':'" + f.cash2.getUUID() + "','note':'x'}"));

        assertThat(result.changed(), is(true));
        assertThat(f.broker.getName(), is("Main"));
        assertThat(f.broker.getReferenceAccount(), is(f.cash2));
        assertThat(f.broker.getNote(), is("x"));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testKeepingARetiredReferenceAccountIsAllowed()
    {
        f.cash.setRetired(true);

        var result = PortfoliosHandler.patch(f.context(false, null), f.broker.getUUID(),
                        json("{'referenceCashAccount':'" + f.cash.getUUID() + "'}"));

        assertThat(result.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatchDryRun()
    {
        var preview = PortfoliosHandler.patch(f.context(true, null), f.broker.getUUID(),
                        json("{'referenceCashAccount':'" + f.cash2.getUUID() + "'}"));

        assertThat(preview.entity().get("referenceCashAccount").getAsString(), is(f.cash2.getUUID()));
        assertThat(preview.entity().get("uuid").getAsString(), is(f.broker.getUUID()));
        assertThat(f.broker.getReferenceAccount(), is(f.cash));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDelete()
    {
        var uuid = f.broker2.getUUID();

        var preview = PortfoliosHandler.delete(f.context(true, null), uuid);
        assertThat(preview.getAsJsonArray("removed").size(), is(1));
        assertThat(find(uuid), is(f.broker2));

        assertThat(PortfoliosHandler.delete(f.context(false, null), uuid), is(nullValue()));
        assertThat(find(uuid), is(nullValue()));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDeleteIsBlockedByTransactionsAndPlans()
    {
        f.create("{'type':'buy','date':'2026-03-02','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'"
                        + f.cash.getUUID() + "','instrument':'" + f.eurSecurity.getUUID()
                        + "','shares':'1','quote':'10'}");
        var plan = new InvestmentPlan("Plan");
        plan.setPortfolio(f.broker);
        f.client.addPlan(plan);

        var e = fails(() -> PortfoliosHandler.delete(f.context(false, null), f.broker.getUUID()));

        assertThat(e.getStatus(), is(409));
        assertThat(e.getType(), is("delete-blocked"));
        assertThat(e.getErrors().stream().map(ApiException.FieldError::field).toList(),
                        is(List.of("transactions", "plans")));
        assertThat(find(f.broker.getUUID()), is(f.broker));
    }

    @Test
    public void testThroughRouter() throws Exception
    {
        var created = f.call("POST", "/investment-accounts", Map.of(),
                        "{'name':'Routed','referenceCashAccount':'" + f.cash.getUUID() + "','clientRef':'p-1'}");
        assertThat(created.status(), is(201));
        var uuid = body(created).get("uuid").getAsString();
        assertThat(created.headers().get("Location"),
                        is("/v1/files/" + f.fileId() + "/investment-accounts/" + uuid));

        var replay = f.call("POST", "/investment-accounts", Map.of(),
                        "{'name':'Routed','referenceCashAccount':'" + f.cash.getUUID() + "','clientRef':'p-1'}");
        assertThat(replay.status(), is(200));
        assertThat(body(replay).get("uuid").getAsString(), is(uuid));

        var dry = f.call("PATCH", "/investment-accounts/" + uuid, Map.of("dry_run", "true"), "{'name':'X'}");
        assertThat(body(dry).get("dryRun").getAsBoolean(), is(true));
        assertThat(find(uuid).getName(), is("Routed"));

        assertThat(f.call("DELETE", "/investment-accounts/" + uuid, Map.of(), null).status(), is(204));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
