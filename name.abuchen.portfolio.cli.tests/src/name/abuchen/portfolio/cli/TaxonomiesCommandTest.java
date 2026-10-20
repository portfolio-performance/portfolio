package name.abuchen.portfolio.cli;

import static name.abuchen.portfolio.cli.TaxonomyFixtures.assign;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.child;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.percent;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.taxonomy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/**
 * <code>taxonomies</code>: the taxonomies of the file and their classification
 * trees, flattened in pre-order.
 */
public class TaxonomiesCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client client;

    @Before
    public void setUp()
    {
        client = new Client();
    }

    private CliRun run(String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of("taxonomies", "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.run(args.toArray(String[]::new));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static List<JsonObject> objects(JsonArray array)
    {
        return StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsJsonObject).toList();
    }

    private static List<String> names(JsonArray classifications)
    {
        return objects(classifications).stream().map(c -> c.get("name").getAsString()).toList();
    }

    private static JsonObject classification(JsonObject taxonomy, String name)
    {
        return objects(taxonomy.getAsJsonArray("classifications")).stream()
                        .filter(c -> c.get("name").getAsString().equals(name)).findFirst().orElseThrow();
    }

    @Test
    public void listsEveryTaxonomyWithItsClassificationsInPreOrder() throws IOException
    {
        var regions = taxonomy(client, "Regions");
        var europe = child(regions.getRoot(), "Europe", 0);
        child(europe, "Germany", 0);
        child(europe, "France", 1);
        child(regions.getRoot(), "America", 1);
        taxonomy(client, "Asset classes");

        var run = run();
        assertThat(run.stdout, run.exitCode, is(0));

        var items = objects(data(run).getAsJsonArray("items"));
        assertThat(data(run).get("total").getAsInt(), is(2));

        // file order, not alphabetical
        assertThat(items.stream().map(i -> i.get("name").getAsString()).toList(), is(List.of("Regions", "Asset classes")));
        assertThat(items.get(0).get("uuid").getAsString(), is(regions.getId()));

        var tree = items.get(0).getAsJsonArray("classifications");
        assertThat(names(tree), is(List.of("Europe", "Germany", "France", "America")));
        assertThat(items.get(1).getAsJsonArray("classifications").size(), is(0));
    }

    @Test
    public void classificationsCarryTheirParentPathAndTargetWeight() throws IOException
    {
        var regions = taxonomy(client, "Regions");
        var europe = child(regions.getRoot(), "Europe", 0);
        europe.setWeight(percent(60));
        var germany = child(europe, "Germany", 0);
        germany.setWeight(percent(33.5));

        var tree = objects(data(run()).getAsJsonArray("items").get(0).getAsJsonObject()
                        .getAsJsonArray("classifications"));

        var top = tree.get(0);
        assertThat(top.get("uuid").getAsString(), is(europe.getId()));
        assertThat(top.has("parent"), is(false));
        assertThat(top.getAsJsonArray("path").size(), is(0));
        assertThat(top.get("targetWeight").getAsBigDecimal().compareTo(new BigDecimal("0.6")), is(0));
        assertThat(top.has("color"), is(false));
        assertThat(top.has("rank"), is(false));

        var nested = tree.get(1);
        assertThat(nested.get("parent").getAsString(), is(europe.getId()));
        assertThat(nested.getAsJsonArray("path").size(), is(1));
        assertThat(nested.getAsJsonArray("path").get(0).getAsString(), is("Europe"));
        assertThat(nested.get("targetWeight").getAsBigDecimal().compareTo(new BigDecimal("0.335")), is(0));
    }

    @Test
    public void siblingsAreOrderedByRankThenNameThenUuid() throws IOException
    {
        var taxonomy = taxonomy(client, "Order");
        child(taxonomy.getRoot(), "zeta", 0);
        child(taxonomy.getRoot(), "beta", 1);
        child(taxonomy.getRoot(), "Alpha", 1);
        var first = child(taxonomy.getRoot(), "same", 2);
        var second = child(taxonomy.getRoot(), "SAME", 2);

        var tree = data(run()).getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonArray("classifications");

        assertThat(names(tree).subList(0, 3), is(List.of("zeta", "Alpha", "beta")));

        var tied = objects(tree).subList(3, 5).stream().map(c -> c.get("uuid").getAsString()).toList();
        var expected = new ArrayList<>(List.of(first, second));
        expected.sort(Comparator.comparing(Classification::getId));
        assertThat(tied, is(expected.stream().map(Classification::getId).toList()));
    }

    @Test
    public void classificationsListTheirDirectAssignmentsOnly() throws IOException
    {
        Security alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");
        Security retired = new SecurityBuilder().addTo(client);
        retired.setName("Retired");
        retired.setRetired(true);
        Account cash = new AccountBuilder().addTo(client);
        cash.setName("Cash");

        var taxonomy = taxonomy(client, "Classes");
        var equity = child(taxonomy.getRoot(), "Equity");
        var europe = child(equity, "Europe");
        assign(equity, alpha, 25);
        assign(europe, alpha, 75);
        assign(equity, retired);
        assign(europe, cash);
        child(taxonomy.getRoot(), "Bonds");

        var item = data(run()).getAsJsonArray("items").get(0).getAsJsonObject();

        var direct = objects(classification(item, "Equity").getAsJsonArray("assignments"));
        assertThat(direct.stream().map(a -> a.get("name").getAsString()).toList(), is(List.of("Alpha", "Retired")));
        assertThat(direct.get(0).get("instrument").getAsString(), is(alpha.getUUID()));
        assertThat(direct.get(0).get("weight").getAsBigDecimal().compareTo(new BigDecimal("0.25")), is(0));
        assertThat(direct.get(1).get("instrument").getAsString(), is(retired.getUUID()));

        var nested = objects(classification(item, "Europe").getAsJsonArray("assignments"));
        assertThat(nested.stream().map(a -> a.get("name").getAsString()).toList(), is(List.of("Alpha", "Cash")));
        assertThat(nested.get(1).get("cashAccount").getAsString(), is(cash.getUUID()));
        assertThat(nested.get(1).has("instrument"), is(false));

        assertThat(classification(item, "Bonds").getAsJsonArray("assignments").size(), is(0));
    }

    @Test
    public void optionalFieldsAppearOnlyWhenSet() throws IOException
    {
        var taxonomy = taxonomy(client, "Fields");
        var keyed = child(taxonomy.getRoot(), "Keyed");
        keyed.setKey("K1");
        keyed.setNote("A note");
        child(taxonomy.getRoot(), "Plain");

        var item = data(run()).getAsJsonArray("items").get(0).getAsJsonObject();

        assertThat(classification(item, "Keyed").get("key").getAsString(), is("K1"));
        assertThat(classification(item, "Keyed").get("note").getAsString(), is("A note"));
        assertThat(classification(item, "Plain").has("key"), is(false));
        assertThat(classification(item, "Plain").has("note"), is(false));
        assertThat(classification(item, "Plain").get("targetWeight").getAsBigDecimal().signum() >= 0, is(true));
    }

    @Test
    public void pagesOverTaxonomiesLikeTheOtherLists() throws IOException
    {
        taxonomy(client, "One");
        taxonomy(client, "Two");
        taxonomy(client, "Three");

        var run = run("--limit", "2");
        var data = data(run);
        assertThat(data.get("total").getAsInt(), is(3));
        assertThat(data.getAsJsonArray("items").size(), is(2));
        assertThat(data.get("nextOffset").getAsInt(), is(2));

        var last = data(run("--limit", "2", "--offset", "2"));
        assertThat(names(last.getAsJsonArray("items")), is(List.of("Three")));
        assertThat(last.has("nextOffset"), is(false));
    }
}
