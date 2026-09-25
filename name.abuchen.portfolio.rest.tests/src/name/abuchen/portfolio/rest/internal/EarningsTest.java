package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class EarningsTest
{
    private ReportFixture f;

    @Before
    public void setUp()
    {
        f = new ReportFixture();
        f.addEarnings();
        // a USD dividend on the USD account
        f.create("{'type':'dividends','cashAccount':'" + f.usdCash.getUUID() + "','instrument':'"
                        + f.usdSecurity.getUUID() + "','date':'2026-05-01','grossValue':'10'}");
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    @Test
    public void testListsEarningsWithTotalsPerCurrency() throws Exception
    {
        var json = body(f.call("GET", "/earnings", Map.of(), null));

        // newest first; the deposits and the buy are not earnings
        var items = json.getAsJsonArray("items");
        assertThat(items.size(), is(3));
        assertThat(items.get(0).getAsJsonObject().get("type").getAsString(), is("dividends"));
        assertThat(items.get(1).getAsJsonObject().get("type").getAsString(), is("interest"));
        assertThat(items.get(2).getAsJsonObject().get("type").getAsString(), is("dividends"));

        var totals = json.getAsJsonArray("totals");
        assertThat(totals.size(), is(2));

        var eur = totals.get(0).getAsJsonObject();
        assertThat(eur.get("currency").getAsString(), is("EUR"));
        assertThat(eur.get("value").getAsBigDecimal().compareTo(new BigDecimal("27")), is(0));
        assertThat(eur.get("dividends").getAsBigDecimal().compareTo(new BigDecimal("25")), is(0));
        assertThat(eur.get("interest").getAsBigDecimal().compareTo(new BigDecimal("2")), is(0));
        assertThat(eur.get("taxes").getAsBigDecimal().compareTo(new BigDecimal("5")), is(0));
        assertThat(eur.get("count").getAsInt(), is(2));

        var usd = totals.get(1).getAsJsonObject();
        assertThat(usd.get("currency").getAsString(), is("USD"));
        assertThat(usd.get("value").getAsBigDecimal().compareTo(new BigDecimal("10")), is(0));
    }

    @Test
    public void testFilters() throws Exception
    {
        var json = body(f.call("GET", "/earnings",
                        Map.of("from", "2026-02-01", "to", "2026-03-31", "instrument", f.eurSecurity.getUUID()),
                        null));
        assertThat(json.getAsJsonArray("items").size(), is(1));

        json = body(f.call("GET", "/earnings", Map.of("cashAccount", f.usdCash.getUUID()), null));
        assertThat(json.getAsJsonArray("items").size(), is(1));
        assertThat(json.getAsJsonArray("totals").get(0).getAsJsonObject().get("currency").getAsString(), is("USD"));

        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testNoEarnings() throws Exception
    {
        var json = body(f.call("GET", "/earnings", Map.of("to", "2026-01-31"), null));
        assertThat(json.getAsJsonArray("items").size(), is(0));
        assertThat(json.getAsJsonArray("totals").size(), is(0));
    }
}
