package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

@SuppressWarnings("nls")
public class DryRunTest
{
    private TransactionFixture f;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private String usdBuy()
    {
        return "{'type':'buy','date':'2026-03-02','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'"
                        + f.cash.getUUID() + "','instrument':'" + f.usdSecurity.getUUID()
                        + "','shares':'10','quote':'100','fees':'5'}";
    }

    @Test
    public void testDryRunCreateReturnsFullPreviewAndChangesNothing()
    {
        var result = TransactionsHandler.create(f.context(true, "ref-1"), json(usdBuy()));
        var preview = result.entity();

        assertThat(result.changed(), is(false));
        assertThat(preview.get("dryRun").getAsBoolean(), is(true));
        assertThat(f.client.getAllTransactions().isEmpty(), is(true));
        assertThat(f.legCount(), is(0));
        assertThat(f.file.isDirty(), is(false));

        // the fully resolved transaction: both legs, rate, units, total
        assertThat(preview.get("type").getAsString(), is("buy"));
        assertThat(preview.get("uuid").getAsString().isEmpty(), is(false));
        var linked = preview.getAsJsonObject("linked");
        assertThat(linked.get("uuid").getAsString(), not(preview.get("uuid").getAsString()));
        assertThat(linked.getAsJsonObject("owner").get("uuid").getAsString(), is(f.cash.getUUID()));
        assertThat(preview.getAsJsonObject("owner").get("uuid").getAsString(), is(f.broker.getUUID()));
        assertThat(preview.getAsJsonObject("value").get("value").getAsBigDecimal().toPlainString(), is("905"));
        assertThat(preview.get("source").getAsString(), is("api:ref-1"));
        assertThat(preview.get("clientRef").getAsString(), is("ref-1"));

        var gross = StreamSupport.stream(preview.getAsJsonArray("units").spliterator(), false).map(JsonObject.class::cast)
                        .filter(u -> u.get("type").getAsString().equals("gross-value")).findFirst().orElseThrow();
        assertThat(gross.get("exchangeRate").getAsBigDecimal().toPlainString(), is("0.9"));
        assertThat(gross.getAsJsonObject("forex").get("value").getAsBigDecimal().toPlainString(), is("1000"));
    }

    @Test
    public void testDryRunValidatesLikeARealRun()
    {
        try
        {
            TransactionsHandler.create(f.context(true, null), json("{'type':'buy'}"));
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
        }
    }

    @Test
    public void testDryRunThroughRouterIs200() throws Exception
    {
        var response = f.call("POST", "/transactions", Map.of("dry_run", "true"), usdBuy());

        assertThat(response.status(), is(200));
        assertThat(response.headers().containsKey("Location"), is(false));
        assertThat(TransactionFixture.body(response).get("dryRun").getAsBoolean(), is(true));
        assertThat(f.legCount(), is(0));
        assertThat(f.file.isDirty(), is(false));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testDryRunDoesNotConsumeTheClientRef()
    {
        TransactionsHandler.create(f.context(true, "ref-2"), json(usdBuy()));
        var real = TransactionsHandler.create(f.context(false, "ref-2"), json(usdBuy()));

        assertThat(real.changed(), is(true));
        assertThat(real.entity().has("replayed"), is(false));
        assertThat(f.legCount(), is(2));
    }
}
