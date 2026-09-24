package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.money.CurrencyUnit;

/** {@code POST}, {@code PATCH} and {@code DELETE /cash-accounts} */
@SuppressWarnings("nls")
public class AccountsWriteTest
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

    private Account find(String uuid)
    {
        return f.client.getAccounts().stream().filter(a -> a.getUUID().equals(uuid)).findFirst().orElse(null);
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
        var result = AccountsHandler.create(f.context(false, null), idempotency,
                        json("{'name':'Savings','currencyCode':'USD','note':'n'}"));

        assertThat(result.changed(), is(true));
        var account = find(result.entity().get("uuid").getAsString());
        assertThat(account.getName(), is("Savings"));
        assertThat(account.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(account.getNote(), is("n"));
        assertThat(result.entity().getAsJsonObject("balance").get("currency").getAsString(), is("USD"));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCreateDefaultsToBaseCurrency()
    {
        var entity = AccountsHandler.create(f.context(false, null), idempotency, json("{'name':'Plain'}")).entity();

        assertThat(find(entity.get("uuid").getAsString()).getCurrencyCode(), is(f.client.getBaseCurrency()));
    }

    @Test
    public void testCreateValidation()
    {
        var count = f.client.getAccounts().size();

        var e = fails(() -> AccountsHandler.create(f.context(false, null), idempotency,
                        json("{'currencyCode':'XYZ','balance':1}")));

        assertError(e, "name", "required");
        assertError(e, "currencyCode", "unknown-currency");
        assertError(e, "balance", "unknown-field");
        assertThat(f.client.getAccounts().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testCreateDryRun()
    {
        var count = f.client.getAccounts().size();

        var result = AccountsHandler.create(f.context(true, null), idempotency, json("{'name':'Preview'}"));

        assertThat(result.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.client.getAccounts().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatch()
    {
        var type = new AttributeType("iban");
        type.setName("IBAN");
        type.setType(String.class);
        type.setTarget(Account.class);
        type.setConverter(AttributeType.StringConverter.class);
        f.client.getSettings().addAttributeType(type);

        var result = AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(),
                        json("{'name':'Dollars','currencyCode':'CHF','note':'x','attributes':{'iban':'CH00'}}"));

        assertThat(result.changed(), is(true));
        assertThat(f.usdCash.getName(), is("Dollars"));
        assertThat(f.usdCash.getCurrencyCode(), is("CHF"));
        assertThat(f.usdCash.getNote(), is("x"));
        assertThat(f.usdCash.getAttributes().get(type), is("CH00"));
        assertThat(result.entity().getAsJsonObject("attributes").get("iban").getAsString(), is("CH00"));
        assertThat(f.file.isDirty(), is(true));

        AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(), json("{'note':null}"));
        assertThat(f.usdCash.getNote(), is(nullValue()));
    }

    @Test
    public void testCurrencyIsLockedByTransactions()
    {
        f.create("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.usdCash.getUUID()
                        + "','amount':'10'}");
        f.file.setDirty(false);

        var e = fails(() -> AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(),
                        json("{'currencyCode':'EUR'}")));

        assertError(e, "currencyCode", "locked-by-transactions");
        assertThat(f.usdCash.getCurrencyCode(), is(CurrencyUnit.USD));

        // the same currency is not a change
        AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(), json("{'currencyCode':'USD'}"));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatchDryRunAndNoOp()
    {
        var preview = AccountsHandler.patch(f.context(true, null), f.usdCash.getUUID(), json("{'name':'New'}"));

        assertThat(preview.entity().get("name").getAsString(), is("New"));
        assertThat(preview.entity().get("uuid").getAsString(), is(f.usdCash.getUUID()));
        assertThat(preview.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.usdCash.getName(), is("USD Cash"));

        var noop = AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(), json("{'name':'USD Cash'}"));
        assertThat(noop.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));

        assertError(fails(() -> AccountsHandler.patch(f.context(false, null), f.usdCash.getUUID(),
                        json("{'name':null}"))), "name", "required");
    }

    @Test
    public void testDelete()
    {
        var uuid = f.usdCash.getUUID();

        var preview = AccountsHandler.delete(f.context(true, null), uuid);
        assertThat(preview.getAsJsonArray("removed").size(), is(1));
        assertThat(find(uuid), is(f.usdCash));
        assertThat(f.file.isDirty(), is(false));

        assertThat(AccountsHandler.delete(f.context(false, null), uuid), is(nullValue()));
        assertThat(find(uuid), is(nullValue()));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDeleteIsBlockedByTransactionsPlansAndInvestmentAccounts()
    {
        f.create("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID() + "','amount':'10'}");
        var plan = new InvestmentPlan("Plan");
        plan.setAccount(f.cash);
        f.client.addPlan(plan);

        var e = fails(() -> AccountsHandler.delete(f.context(false, null), f.cash.getUUID()));

        assertThat(e.getStatus(), is(409));
        assertThat(e.getType(), is("delete-blocked"));
        assertThat(e.getErrors().stream().map(ApiException.FieldError::field).toList(),
                        is(java.util.List.of("transactions", "plans", "investmentAccounts")));
        assertThat(find(f.cash.getUUID()), is(f.cash));
    }

    @Test
    public void testThroughRouter() throws Exception
    {
        var created = f.call("POST", "/cash-accounts", Map.of(), "{'name':'Routed','clientRef':'acc-1'}");
        assertThat(created.status(), is(201));
        var uuid = body(created).get("uuid").getAsString();
        assertThat(created.headers().get("Location"), is("/v1/files/" + f.fileId() + "/cash-accounts/" + uuid));

        var replay = f.call("POST", "/cash-accounts", Map.of(), "{'name':'Other','clientRef':'acc-1'}");
        assertThat(replay.status(), is(200));
        assertThat(body(replay).get("replayed").getAsBoolean(), is(true));
        assertThat(body(replay).get("uuid").getAsString(), is(uuid));

        var patched = f.call("PATCH", "/cash-accounts/" + uuid, Map.of(), "{'name':'Renamed'}");
        assertThat(body(patched).get("name").getAsString(), is("Renamed"));

        assertThat(f.call("DELETE", "/cash-accounts/" + uuid, Map.of(), null).status(), is(204));
        assertThat(find(uuid), is(nullValue()));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
