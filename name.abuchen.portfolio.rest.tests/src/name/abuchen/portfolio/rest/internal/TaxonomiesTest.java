package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;

@SuppressWarnings("nls")
public class TaxonomiesTest
{
    private static JsonArray items(Client client)
    {
        return TaxonomiesHandler.list(client).getAsJsonObject().get("items").getAsJsonArray();
    }

    private static Client clientWithTaxonomy()
    {
        var client = new Client();

        var taxonomy = new Taxonomy("Asset Allocation");
        var root = new Classification(taxonomy.getId(), "Asset Allocation");
        taxonomy.setRootNode(root);

        var equity = new Classification(root, "equity", "Equity");
        equity.setColor("#89afee");
        root.addChild(equity);
        equity.addChild(new Classification(equity, "europe", "Europe"));
        equity.addChild(new Classification(equity, "us", "North America"));

        root.addChild(new Classification(root, "cash", "Cash"));

        client.addTaxonomy(taxonomy);
        return client;
    }

    @Test
    public void testTheTaxonomyIsReportedWithItsIdAndName()
    {
        var list = items(clientWithTaxonomy());

        assertThat(list.size(), is(1));
        var taxonomy = list.get(0).getAsJsonObject();
        assertThat(taxonomy.get("id").getAsString(), is(not(emptyString())));
        assertThat(taxonomy.get("name").getAsString(), is("Asset Allocation"));
    }

    /**
     * The root names the taxonomy, not a category - and a holding's classification
     * paths skip it too, so the two only line up if it is left out here as well.
     */
    @Test
    public void testTheRootIsNotReportedAsACategory()
    {
        var categories = items(clientWithTaxonomy()).get(0).getAsJsonObject().get("categories").getAsJsonArray();

        assertThat(categories.size(), is(2));
        assertThat(categories.get(0).getAsJsonObject().get("name").getAsString(), is("Equity"));
        assertThat(categories.get(1).getAsJsonObject().get("name").getAsString(), is("Cash"));
    }

    @Test
    public void testTheTreeIsNested()
    {
        var categories = items(clientWithTaxonomy()).get(0).getAsJsonObject().get("categories").getAsJsonArray();
        var children = categories.get(0).getAsJsonObject().get("children").getAsJsonArray();

        assertThat(children.size(), is(2));
        assertThat(children.get(0).getAsJsonObject().get("name").getAsString(), is("Europe"));
    }

    /** A leaf omits the key rather than carrying an empty array on every leaf. */
    @Test
    public void testALeafHasNoChildrenKey()
    {
        var categories = items(clientWithTaxonomy()).get(0).getAsJsonObject().get("categories").getAsJsonArray();

        assertThat(categories.get(1).getAsJsonObject().get("children"), is(nullValue()));
    }

    /**
     * The application assigns every category a color when it is created, so one is
     * always reported - the field is optional in the schema for the de-serialized
     * case, not because a category normally lacks one.
     */
    @Test
    public void testTheColorIsReported()
    {
        var categories = items(clientWithTaxonomy()).get(0).getAsJsonObject().get("categories").getAsJsonArray();

        assertThat(categories.get(0).getAsJsonObject().get("color").getAsString(), is("#89afee"));
        assertThat(categories.get(1).getAsJsonObject().get("color").getAsString(), is(not(emptyString())));
    }

    @Test
    public void testAFileWithoutTaxonomiesReportsAnEmptyList()
    {
        assertThat(items(new Client()).size(), is(0));
    }

    /**
     * The ids a holding's classifications are keyed by have to be the ids reported
     * here, or a client cannot join the two.
     */
    @Test
    public void testTheIdMatchesTheOneUsedOnHoldings()
    {
        var client = clientWithTaxonomy();
        var reported = items(client).get(0).getAsJsonObject().get("id").getAsString();

        assertThat(reported, is(client.getTaxonomies().get(0).getId()));
    }

    @Test
    public void testEveryTaxonomyIsListed()
    {
        var client = clientWithTaxonomy();
        var second = new Taxonomy("Regions");
        second.setRootNode(new Classification(second.getId(), "Regions"));
        client.addTaxonomy(second);

        var list = items(client);
        assertThat(list.size(), is(2));

        var empty = (JsonObject) list.get(1);
        assertThat(empty.get("name").getAsString(), is("Regions"));
        assertThat(empty.get("categories").getAsJsonArray().size(), is(0));
    }
}
