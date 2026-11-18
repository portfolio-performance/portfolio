package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Watchlist;

@SuppressWarnings("nls")
public class InstrumentFilterTest
{
    private Client client;
    private Security apple;
    private Security bond;

    @Before
    public void setUp()
    {
        client = new Client();
        apple = new SecurityBuilder().addTo(client);
        apple.setName("Apple Inc.");
        apple.setIsin("US0378331005");
        apple.setCurrencyCode("USD");
        bond = new SecurityBuilder().addTo(client);
        bond.setName("Euro Bond");
        bond.setCurrencyCode("EUR");

        var attribute = new AttributeType("watch");
        attribute.setName("Watch");
        attribute.setTarget(Security.class);
        attribute.setType(Boolean.class);
        attribute.setConverter(AttributeType.BooleanConverter.class);
        client.getSettings().addAttributeType(attribute);
        apple.getAttributes().put(attribute, Boolean.TRUE);
        bond.getAttributes().put(attribute, Boolean.FALSE);

        var watchlist = new Watchlist();
        watchlist.setName("Core stocks");
        watchlist.addSecurity(apple);
        client.addWatchlist(watchlist);

        var taxonomy = new Taxonomy("Sectors");
        var root = new Classification("root", "Sectors");
        var technology = new Classification(root, "technology", "Technology");
        root.addChild(technology);
        taxonomy.setRootNode(root);
        technology.addAssignment(new Classification.Assignment(apple));
        client.addTaxonomy(taxonomy);
    }

    @Test
    public void filtersFixedAndCustomAttributesTogether()
    {
        var result = SecuritiesHandler.list(client, "{\"currencyCode\":\"USD\",\"attributes\":{\"watch\":true}}");

        assertThat(result.getAsJsonObject().getAsJsonArray("items").size(), is(1));
        assertThat(result.getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().get("uuid").getAsString(),
                        is(apple.getUUID()));
    }

    @Test
    public void supportsAlternativesAndWatchlistMembership()
    {
        var result = SecuritiesHandler.list(client, "{\"name\":[\"Apple Inc.\",\"Euro Bond\"],\"watchlist\":\"Core stocks\"}");

        assertThat(result.getAsJsonObject().getAsJsonArray("items").size(), is(1));
        assertThat(result.getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().get("name").getAsString(),
                        is("Apple Inc."));
    }

    @Test
    public void filtersByTaxonomyClassification()
    {
        var result = SecuritiesHandler.list(client,
                        "{\"classification\":{\"name\":\"Technology\",\"taxonomy\":\"Sectors\"}}");

        assertThat(result.getAsJsonObject().getAsJsonArray("items").size(), is(1));
        assertThat(result.getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().get("uuid").getAsString(),
                        is(apple.getUUID()));
    }

    @Test
    public void rejectsMalformedAndUnknownFilters()
    {
        assertBadFilter("not-json", "invalid-type");
        assertBadFilter("{\"sector\":\"Technology\"}", "unknown-field");
        assertBadFilter("{\"attributes\":{\"missing\":true}}", "unknown-attribute");
    }

    private void assertBadFilter(String filter, String code)
    {
        try
        {
            SecuritiesHandler.list(client, filter);
            throw new AssertionError("expected invalid filter");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).code(), is(code));
        }
    }
}
