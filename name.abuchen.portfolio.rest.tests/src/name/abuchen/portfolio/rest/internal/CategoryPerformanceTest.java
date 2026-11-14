package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

/**
 * One class of a taxonomy reported in the same terms as the whole file: the
 * class is what the report covers. The fixture keeps this checkable by hand -
 * two positions, one of them classified, so the class must see 1200 of the
 * file's 1800 and nothing else.
 */
@SuppressWarnings("nls")
public class CategoryPerformanceTest
{
    private static final double EPS = 1e-9;

    private static JsonObject report(Client client, String taxonomy, String klass)
    {
        return CategoryPerformanceHandler.get(client, new ExchangeRateProviderFactory(client), taxonomy, klass,
                        "2024-01-01", "2024-12-31", null, null);
    }

    /** ACME bought for 1000 and worth 1200, Other bought for 500 and worth 600 */
    private static Client twoPositions(Client client)
    {
        var cash = new AccountBuilder("EUR").deposit_("2023-06-01", Values.Amount.factorize(1500)).addTo(client);

        var acme = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(120)) //
                        .addTo(client);
        acme.setName("ACME");

        var other = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(50)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(60)) //
                        .addTo(client);
        other.setName("Other");

        new PortfolioBuilder(cash) //
                        .buy(acme, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .buy(other, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .addTo(client);

        return client;
    }

    /** the same file with ACME in the class "equity" and Other left unclassified */
    private static Taxonomy withClasses(Client client)
    {
        var taxonomy = new TaxonomyBuilder() //
                        .addClassification("equity") //
                        .addClassification("bonds") //
                        .addTo(client);
        taxonomy.getClassificationById("equity") //
                        .addAssignment(new Classification.Assignment(client.getSecurities().get(0),
                                        Classification.ONE_HUNDRED_PERCENT));
        return taxonomy;
    }

    private static double value(JsonObject report, String field)
    {
        return report.get("breakdown").getAsJsonObject().get(field).getAsJsonObject().get("value").getAsDouble();
    }

    /** the class reports its own 1000 to 1200 while the file reports 1500 to 1800 */
    @Test
    public void testAClassSeesOnlyWhatItHolds()
    {
        var client = twoPositions(new Client());
        withClasses(client);

        var json = report(client, null, "equity");
        assertThat(value(json, "openingValue"), is(closeTo(1000d, EPS)));
        assertThat(value(json, "closingValue"), is(closeTo(1200d, EPS)));
        assertThat(json.get("ttwror").getAsDouble(), is(closeTo(0.2d, EPS)));

        var whole = PerformanceHandler.list(client, new ExchangeRateProviderFactory(client), "2024-01-01",
                        "2024-12-31", null, null).getAsJsonObject();
        assertThat(value(whole, "closingValue"), is(closeTo(1800d, EPS)));
    }

    /** the report names the class it is about, since its figures say nothing of their own */
    @Test
    public void testTheReportNamesItsClass()
    {
        var client = twoPositions(new Client());
        var taxonomy = withClasses(client);

        var category = report(client, taxonomy.getId(), "equity").get("category").getAsJsonObject();
        assertThat(category.get("id").getAsString(), is("equity"));
        assertThat(category.get("name").getAsString(), is("equity"));
        assertThat(category.get("assignedCount").getAsInt(), is(1));
        assertThat(category.get("taxonomy").getAsJsonObject().get("id").getAsString(), is(taxonomy.getId()));
        assertThat(category.has("parentId"), is(false));
    }

    /** a class holding nothing has no capital to measure a return on: null, not 0 % */
    @Test
    public void testAClassOfNothingHasNoReturn()
    {
        var client = twoPositions(new Client());
        withClasses(client);

        var json = report(client, null, "bonds");
        assertThat(value(json, "closingValue"), is(closeTo(0d, EPS)));
        assertThat(json.get("ttwror").isJsonNull(), is(true));
        assertThat(json.get("irr").isJsonNull(), is(true));
        assertThat(json.get("category").getAsJsonObject().get("assignedCount").getAsInt(), is(0));
    }

    /** the id of a class no taxonomy of the file defines is refused */
    @Test
    public void testAClassOfNoUseIsRefused()
    {
        var client = twoPositions(new Client());
        withClasses(client);

        try
        {
            report(client, null, "nope");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("class"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            return;
        }
        throw new AssertionError("a class of no taxonomy was accepted");
    }

    /** and without a class there is no report: the whole file has its own tool */
    @Test
    public void testWithoutAClassThereIsNothingToReportOn()
    {
        var client = twoPositions(new Client());
        withClasses(client);

        try
        {
            report(client, null, null);
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("class"));
            assertThat(e.getErrors().get(0).code(), is("required"));
            return;
        }
        throw new AssertionError("a report over no class at all was accepted");
    }
}
