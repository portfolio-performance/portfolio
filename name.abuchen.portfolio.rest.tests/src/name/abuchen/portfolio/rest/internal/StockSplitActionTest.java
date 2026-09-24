package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class StockSplitActionTest
{
    private TransactionFixture f;
    private IdempotencyIndex idempotency;
    private Transaction before;
    private Transaction after;
    private Transaction dividend;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
        idempotency = new IdempotencyIndex();

        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-01-01"), Values.Quote.factorize(60)));
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-05-29"), Values.Quote.factorize(80)));
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-06-01"), Values.Quote.factorize(41)));

        var buy = "{'type':'buy','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'"
                        + f.cash.getUUID() + "','instrument':'" + f.eurSecurity.getUUID() + "','quote':'50',";
        before = f.find(f.create(buy + "'date':'2026-01-10','shares':'10'}").get("uuid").getAsString())
                        .getTransaction();
        after = f.find(f.create(buy + "'date':'2026-07-01','shares':'5'}").get("uuid").getAsString())
                        .getTransaction();
        dividend = f.find(f.create("{'type':'dividends','cashAccount':'" + f.cash.getUUID() + "','instrument':'"
                        + f.eurSecurity.getUUID() + "','date':'2026-03-01','shares':'10','grossValue':'30'}")
                        .get("uuid").getAsString()).getTransaction();

        f.file.setDirty(false);
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private JsonObject split(String body, boolean dryRun, String clientRef)
    {
        return StockSplitAction.apply(f.context(dryRun, clientRef), idempotency, f.eurSecurity.getUUID(), json(body));
    }

    private long price(String date)
    {
        return f.eurSecurity.getPrices().stream().filter(p -> p.getDate().equals(LocalDate.parse(date)))
                        .findFirst().orElseThrow().getValue();
    }

    @Test
    public void testSplitAdjustsSharesAndPricesBeforeTheExDate()
    {
        var result = split("{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}", false, null);

        assertThat(before.getShares(), is(shares("20")));
        assertThat(dividend.getShares(), is(shares("20")));
        assertThat(after.getShares(), is(shares("5")));

        assertThat(price("2026-01-01"), is(Values.Quote.factorize(30)));
        assertThat(price("2026-05-29"), is(Values.Quote.factorize(40)));
        assertThat(price("2026-06-01"), is(Values.Quote.factorize(41)));

        var event = f.eurSecurity.getEvents().get(0);
        assertThat(event.getType(), is(SecurityEvent.Type.STOCK_SPLIT));
        assertThat(event.getDate(), is(LocalDate.parse("2026-06-01")));
        assertThat(event.getDetails(), is("2:1"));

        assertThat(result.get("transactionsAffected").getAsInt(), is(2));
        assertThat(result.get("pricesAffected").getAsInt(), is(2));
        assertThat(result.getAsJsonObject("event").get("details").getAsString(), is("2:1"));
        var first = result.getAsJsonArray("transactions").get(0).getAsJsonObject();
        assertThat(first.get("uuid").getAsString(), is(before.getUUID()));
        assertThat(first.get("sharesBefore").getAsBigDecimal().compareTo(BigDecimal.TEN), is(0));
        assertThat(first.get("sharesAfter").getAsBigDecimal().compareTo(new BigDecimal("20")), is(0));
        assertThat(result.getAsJsonObject("lastPrice").get("date").getAsString(), is("2026-05-29"));
        assertThat(result.getAsJsonObject("lastPrice").get("after").getAsBigDecimal()
                        .compareTo(new BigDecimal("40")), is(0));
        assertThat(result.has("dryRun"), is(false));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDryRunChangesNothing()
    {
        var result = split("{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}", true, null);

        assertThat(result.get("dryRun").getAsBoolean(), is(true));
        assertThat(result.get("transactionsAffected").getAsInt(), is(2));
        assertThat(before.getShares(), is(shares("10")));
        assertThat(price("2026-01-01"), is(Values.Quote.factorize(60)));
        assertThat(f.eurSecurity.getEvents().isEmpty(), is(true));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testOptionsAndReverseSplit()
    {
        split("{'exDate':'2026-06-01','newShares':'1','oldShares':'4','adjustTransactions':false}", false, null);

        assertThat(before.getShares(), is(shares("10")));
        assertThat(price("2026-01-01"), is(Values.Quote.factorize(240)));
        assertThat(f.eurSecurity.getEvents().get(0).getDetails(), is("1:4"));
    }

    @Test
    public void testValidation()
    {
        try
        {
            split("{'newShares':'0','oldShares':'-1','adjustPrices':'yes','foo':1}", false, null);
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertError(e, "exDate", "required");
            assertError(e, "newShares", "must-be-positive");
            assertError(e, "oldShares", "must-be-positive");
            assertError(e, "adjustPrices", "invalid-type");
            assertError(e, "foo", "unknown-field");
        }

        try
        {
            split("{'exDate':'2026-06-01','newShares':'3','oldShares':'3.0'}", false, null);
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertError(e, "newShares", "invalid-value");
        }
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testReplayWithClientRefDoesNotSplitTwice()
    {
        split("{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}", false, "split-1");
        var replay = split("{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}", false, "split-1");

        assertThat(replay.get("replayed").getAsBoolean(), is(true));
        assertThat(before.getShares(), is(shares("20")));
        assertThat(f.eurSecurity.getEvents().size(), is(1));
    }

    @Test
    public void testThroughRouter() throws Exception
    {
        var response = f.call("POST", "/instruments/" + f.eurSecurity.getUUID() + "/actions/split",
                        Map.of("dry_run", "true"), "{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}");
        assertThat(response.status(), is(200));
        assertThat(body(response).get("dryRun").getAsBoolean(), is(true));

        f.host().setUserEditing(true);
        try
        {
            f.call("POST", "/instruments/" + f.eurSecurity.getUUID() + "/actions/split", Map.of(),
                            "{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}");
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
        f.host().setUserEditing(false);

        response = f.call("POST", "/instruments/" + f.eurSecurity.getUUID() + "/actions/split", Map.of(),
                        "{'exDate':'2026-06-01','newShares':'2','oldShares':'1'}");
        assertThat(response.status(), is(200));
        assertThat(before.getShares(), is(shares("20")));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
