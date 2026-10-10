package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

/** The investmentAccount/cashAccount filters of the report endpoints */
@SuppressWarnings("nls")
public class HoldingsFilterTest
{
    private ReportFixture f;

    @Before
    public void setUp()
    {
        f = new ReportFixture();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private JsonObject holdings(Map<String, String> filter) throws Exception
    {
        var query = new HashMap<>(filter);
        query.put("date", ReportFixture.DATE);
        var response = f.call("GET", "/holdings", query, null);
        assertThat(new String(response.body()), response.status(), is(200));
        return body(response);
    }

    private static BigDecimal total(JsonObject holdings)
    {
        return holdings.getAsJsonObject("totalAssets").get("value").getAsBigDecimal();
    }

    @Test
    public void testUnfilteredHoldingsCoverTheWholeFile() throws Exception
    {
        // 600 in shares, 500 + 500 cash, 0 on the USD account
        assertThat(total(holdings(Map.of())).compareTo(new BigDecimal("1600")), is(0));
    }

    @Test
    public void testCashAccountFilter() throws Exception
    {
        var holdings = holdings(Map.of("cashAccount", f.cash2.getUUID()));
        assertThat(total(holdings).compareTo(new BigDecimal("500")), is(0));

        var items = holdings.getAsJsonArray("items");
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("uuid").getAsString(), is(f.cash2.getUUID()));
    }

    @Test
    public void testInvestmentAndCashAccountFilterCombine() throws Exception
    {
        var holdings = holdings(Map.of("investmentAccount", f.broker.getUUID(), "cashAccount", f.cash.getUUID()));
        assertThat(total(holdings).compareTo(new BigDecimal("1100")), is(0));

        var items = holdings.getAsJsonArray("items");
        assertThat(items.size(), is(2));
        // sorted by name
        assertThat(items.get(0).getAsJsonObject().get("uuid").getAsString(), is(f.cash.getUUID()));
        assertThat(items.get(1).getAsJsonObject().get("uuid").getAsString(), is(f.eurSecurity.getUUID()));
    }

    @Test
    public void testCommaSeparatedList() throws Exception
    {
        var holdings = holdings(Map.of("cashAccount", f.cash.getUUID() + "," + f.cash2.getUUID()));
        assertThat(total(holdings).compareTo(new BigDecimal("1000")), is(0));
    }

    @Test
    public void testUnknownAccountIs400() throws Exception
    {
        var e = expectError(() -> f.call("GET", "/holdings", Map.of("investmentAccount", "nope"), null));
        assertThat(e.getStatus(), is(400));
        assertThat(e.getErrors().get(0).field(), is("investmentAccount"));
        assertThat(e.getErrors().get(0).code(), is("unknown-reference"));
    }

    @Test
    public void testPerformanceEndpointsAcceptTheFilter() throws Exception
    {
        var query = Map.of("openingDate", "2025-12-31", "closingDate", ReportFixture.DATE, "cashAccount",
                        f.cash2.getUUID());

        var performance = body(f.call("GET", "/performance", query, null));
        var closing = performance.getAsJsonObject("breakdown").getAsJsonObject("closingValue");
        assertThat(closing.get("value").getAsBigDecimal().compareTo(new BigDecimal("500")), is(0));

        assertThat(f.call("GET", "/performance/series", query, null).status(), is(200));
        assertThat(f.call("GET", "/performance/calendar", query, null).status(), is(200));

        var securities = body(f.call("GET", "/performance/securities",
                        Map.of("openingDate", "2025-12-31", "closingDate", ReportFixture.DATE, "cashAccount",
                                        f.cash2.getUUID()),
                        null));
        assertThat(securities.getAsJsonArray("items").size(), is(0));

        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    /* package */ interface Call
    {
        Object run() throws Exception;
    }

    /* package */ static ApiException expectError(Call call) throws Exception
    {
        try
        {
            call.run();
        }
        catch (ApiException e)
        {
            return e;
        }
        throw new AssertionError("expected ApiException");
    }
}
