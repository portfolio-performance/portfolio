package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

/** {@code PUT} and {@code DELETE /instruments/{uuid}/prices} */
@SuppressWarnings("nls")
public class SecurityPricesWriteTest
{
    private TransactionFixture f;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-01-05"), Values.Quote.factorize(10)));
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-01-06"), Values.Quote.factorize(11)));
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private String uuid()
    {
        return f.eurSecurity.getUUID();
    }

    private long priceAt(String date)
    {
        return f.eurSecurity.getPrices().stream().filter(p -> p.getDate().equals(LocalDate.parse(date)))
                        .findFirst().orElseThrow().getValue();
    }

    private ApiException upsertFails(String body)
    {
        try
        {
            SecurityPricesHandler.upsert(f.context(false, null), uuid(), json(body));
            Assert.fail("expected ApiException for " + body);
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    @Test
    public void testUpsertInsertsSortedAndOverwritesTheSameDate()
    {
        var result = SecurityPricesHandler.upsert(f.context(false, null), uuid(), json(
                        "{'items':[{'date':'2026-01-07','value':'12.5'},{'date':'2026-01-02','value':9},"
                                        + "{'date':'2026-01-06','value':'11.25'},{'date':'2026-01-05','value':'10'}]}"));

        assertThat(result.get("inserted").getAsInt(), is(2));
        assertThat(result.get("updated").getAsInt(), is(1));
        assertThat(result.get("unchanged").getAsInt(), is(1));

        assertThat(f.eurSecurity.getPrices().stream().map(p -> p.getDate().toString()).toList(),
                        contains("2026-01-02", "2026-01-05", "2026-01-06", "2026-01-07"));
        assertThat(priceAt("2026-01-06"), is(Values.Quote.factorize(11.25)));
        assertThat(priceAt("2026-01-07"), is(Values.Quote.factorize(12.5)));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testUpsertOfEqualPricesDoesNotMarkDirty()
    {
        var result = SecurityPricesHandler.upsert(f.context(false, null), uuid(),
                        json("{'items':[{'date':'2026-01-05','value':'10.00'}]}"));

        assertThat(result.get("unchanged").getAsInt(), is(1));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDryRunUpsertCountsButChangesNothing()
    {
        var result = SecurityPricesHandler.upsert(f.context(true, null), uuid(),
                        json("{'items':[{'date':'2026-01-05','value':'99'},{'date':'2026-02-01','value':'1'}]}"));

        assertThat(result.get("dryRun").getAsBoolean(), is(true));
        assertThat(result.get("inserted").getAsInt(), is(1));
        assertThat(result.get("updated").getAsInt(), is(1));
        assertThat(f.eurSecurity.getPrices().size(), is(2));
        assertThat(priceAt("2026-01-05"), is(Values.Quote.factorize(10)));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testInvalidPricesAreReportedPerItem()
    {
        var e = upsertFails("{'items':[{'date':'2026-01-10','value':'-1'},{'date':'2026-01-10','value':'1'},"
                        + "{'value':'1.123456789'},{'date':'x','value':'1','extra':true}],'more':1}");

        assertError(e, "items[0].value", "must-be-positive");
        assertError(e, "items[1].date", "invalid-value");
        assertError(e, "items[2].date", "required");
        assertError(e, "items[2].value", "invalid-value");
        assertError(e, "items[3].date", "invalid-value");
        assertError(e, "items[3].extra", "unknown-field");
        assertError(e, "more", "unknown-field");
        assertThat(f.eurSecurity.getPrices().size(), is(2));

        assertError(upsertFails("{}"), "items", "required");
        assertError(upsertFails("{'items':{}}"), "items", "invalid-type");
    }

    @Test
    public void testDeleteRange()
    {
        f.eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-01-07"), Values.Quote.factorize(12)));

        var result = SecurityPricesHandler.delete(f.context(false, null), uuid(), "2026-01-06", null);

        assertThat(result.get("removed").getAsInt(), is(2));
        assertThat(f.eurSecurity.getPrices().stream().map(p -> p.getDate().toString()).toList(),
                        contains("2026-01-05"));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDeleteWithoutBoundsRemovesAll()
    {
        var result = SecurityPricesHandler.delete(f.context(false, null), uuid(), null, null);

        assertThat(result.get("removed").getAsInt(), is(2));
        assertThat(f.eurSecurity.getPrices().isEmpty(), is(true));
    }

    @Test
    public void testDeleteOfNothingAndDryRunDoNotMarkDirty()
    {
        var none = SecurityPricesHandler.delete(f.context(false, null), uuid(), "2027-01-01", null);
        assertThat(none.get("removed").getAsInt(), is(0));

        var dry = SecurityPricesHandler.delete(f.context(true, null), uuid(), null, "2026-01-05");
        assertThat(dry.get("removed").getAsInt(), is(1));
        assertThat(dry.get("dryRun").getAsBoolean(), is(true));

        assertThat(f.eurSecurity.getPrices().size(), is(2));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDeleteRejectsAnInvertedRange()
    {
        try
        {
            SecurityPricesHandler.delete(f.context(false, null), uuid(), "2026-02-01", "2026-01-01");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).code(), is("invalid-range"));
        }
    }

    @Test
    public void testThroughRouterOnTheUIThread() throws Exception
    {
        var put = f.call("PUT", "/instruments/" + uuid() + "/prices", Map.of(),
                        "{'items':[{'date':'2026-03-01','value':'5'}]}");
        assertThat(put.status(), is(200));
        assertThat(body(put).get("inserted").getAsInt(), is(1));

        var delete = f.call("DELETE", "/instruments/" + uuid() + "/prices", Map.of("from", "2026-03-01"), null);
        assertThat(delete.status(), is(200));
        assertThat(body(delete).get("removed").getAsInt(), is(1));

        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
