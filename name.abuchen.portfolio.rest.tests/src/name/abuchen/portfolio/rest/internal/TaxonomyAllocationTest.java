package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Taxonomy;

@SuppressWarnings("nls")
public class TaxonomyAllocationTest
{
    private ReportFixture f;
    private Taxonomy taxonomy;

    @Before
    public void setUp()
    {
        f = new ReportFixture();

        taxonomy = new Taxonomy("Asset Classes");
        var root = new Classification(UUID.randomUUID().toString(), "Asset Classes");
        taxonomy.setRootNode(root);

        var stocks = new Classification(root, "stocks", "Stocks");
        stocks.setWeight(60 * 100);
        root.addChild(stocks);
        stocks.addAssignment(new Classification.Assignment(f.eurSecurity, Classification.ONE_HUNDRED_PERCENT));

        var cash = new Classification(root, "cash", "Cash");
        cash.setWeight(40 * 100);
        root.addChild(cash);
        cash.addAssignment(new Classification.Assignment(f.cash, Classification.ONE_HUNDRED_PERCENT / 2));

        f.client.addTaxonomy(taxonomy);
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private JsonObject allocation(Map<String, String> query) throws Exception
    {
        var response = f.call("GET", "/taxonomies/" + taxonomy.getId() + "/allocation", query, null);
        assertThat(new String(response.body()), response.status(), is(200));
        return body(response);
    }

    private static void assertValue(JsonObject json, String field, String expected)
    {
        assertThat(json + " " + field,
                        json.getAsJsonObject(field).get("value").getAsBigDecimal().compareTo(new BigDecimal(expected)),
                        is(0));
    }

    @Test
    public void testValuesPerCategoryAndUnassigned() throws Exception
    {
        var json = allocation(Map.of("date", ReportFixture.DATE));

        assertThat(json.getAsJsonObject("taxonomy").get("id").getAsString(), is(taxonomy.getId()));
        assertThat(json.get("currency").getAsString(), is("EUR"));
        assertValue(json, "totalAssets", "1600");

        var categories = json.getAsJsonArray("categories");
        assertThat(categories.size(), is(2));

        // shares: 10 x 60
        var stocksJson = categories.get(0).getAsJsonObject();
        assertThat(stocksJson.get("id").getAsString(), is("stocks"));
        assertValue(stocksJson, "value", "600");
        assertThat(stocksJson.get("weight").getAsBigDecimal().compareTo(new BigDecimal("60")), is(0));
        assertThat(stocksJson.get("share").getAsBigDecimal().compareTo(new BigDecimal("0.375")), is(0));

        // half of "Cash" (500)
        var cashJson = categories.get(1).getAsJsonObject();
        assertValue(cashJson, "value", "250");
        var assignment = cashJson.getAsJsonArray("assignments").get(0).getAsJsonObject();
        assertThat(assignment.get("vehicle").getAsString(), is(f.cash.getUUID()));
        assertValue(assignment, "value", "250");

        // the other half of "Cash" plus "Cash 2"
        assertValue(json.getAsJsonObject("unassigned"), "value", "750");

        // targets: the classified 850 split 60:40
        assertValue(stocksJson, "targetValue", "510");
        assertValue(stocksJson, "deviation", "90");
        assertValue(cashJson, "targetValue", "340");
    }

    @Test
    public void testFilterNarrowsTheSnapshot() throws Exception
    {
        var json = allocation(Map.of("date", ReportFixture.DATE, "cashAccount", f.cash2.getUUID()));
        assertValue(json, "totalAssets", "500");
        assertValue(json.getAsJsonArray("categories").get(0).getAsJsonObject(), "value", "0");
        assertValue(json.getAsJsonObject("unassigned"), "value", "500");
    }

    @Test
    public void testUnknownTaxonomyIs404() throws Exception
    {
        var e = HoldingsFilterTest.expectError(() -> f.call("GET", "/taxonomies/nope/allocation", Map.of(), null));
        assertThat(e.getStatus(), is(404));
    }

    @Test
    public void testInvalidParametersAre400() throws Exception
    {
        var e = HoldingsFilterTest.expectError(() -> f.call("GET", "/taxonomies/" + taxonomy.getId() + "/allocation",
                        Map.of("date", "x", "currency", "XYZ"), null));
        assertThat(e.getStatus(), is(400));
        assertThat(e.getErrors().size(), is(2));
    }
}
