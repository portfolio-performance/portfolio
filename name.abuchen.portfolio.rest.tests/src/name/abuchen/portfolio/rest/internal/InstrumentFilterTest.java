package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Watchlist;

/**
 * Narrowing an instrument list: one custom attribute, one watchlist, one class.
 * What matters here is that a value is found by exactly the text the API reports
 * it in, and that a question about something the file does not have is refused
 * instead of answered with an empty list.
 */
@SuppressWarnings("nls")
public class InstrumentFilterTest
{
    private static final String FILE = "f1";

    private static JsonElement list(Client client, String query, SecuritiesHandler.Filters filters, String cursor,
                    String limit)
    {
        return SecuritiesHandler.list(client, FILE, query, filters, cursor, limit);
    }

    private static SecuritiesHandler.Filters attributes(String id, String value)
    {
        return new SecuritiesHandler.Filters(null, null, Map.of(id, value));
    }

    private static int count(JsonElement answer)
    {
        return answer.getAsJsonObject().getAsJsonArray("items").size();
    }

    private static String nameOfFirst(JsonElement answer)
    {
        return answer.getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().get("name").getAsString();
    }

    private static Security named(Client client, String name)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        return security;
    }

    /** an attribute of no type the API knows, as an older or newer file can hold one */
    private static AttributeType booleanAttribute(Client client, String id)
    {
        var type = new AttributeType(id);
        type.setName(id);
        type.setTarget(Security.class);
        type.setType(Boolean.class);
        type.setConverter(AttributeType.BooleanConverter.class);
        client.getSettings().addAttributeType(type);
        return type;
    }

    /** the first error of a refused request, whose code is the part a client acts on */
    private static ApiException.FieldError expectRejection(Runnable call)
    {
        try
        {
            call.run();
            Assert.fail("expected an ApiException");
            return null;
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            return e.getErrors().get(0);
        }
    }

    @Test
    public void testPercentAttributeIsFoundByTheTextTheApiReports()
    {
        var client = new Client();
        var cheap = named(client, "Cheap Fund");
        named(client, "Costly Fund");
        cheap.getAttributes().put(client.getSettings().getAttributeTypes().filter(t -> "ter".equals(t.getId()))
                        .findFirst().orElseThrow(), Double.valueOf(0.008d));

        assertThat(count(list(client, null, attributes("ter", "0.008"), null, null)), is(1));
        assertThat(nameOfFirst(list(client, null, attributes("ter", "0.008"), null, null)), is("Cheap Fund"));

        // 0.8 per cent read back as a fraction, so the percent sign is not the
        // value - and a value the file holds nowhere answers with nothing
        assertThat(count(list(client, null, attributes("ter", "0.0079"), null, null)), is(0));
    }

    @Test
    public void testAmountIsFoundByTheTextTheApiReportsNotByItsScaledStore()
    {
        var client = new Client();
        var big = named(client, "Big House");
        var aum = client.getSettings().getAttributeTypes().filter(t -> "aum".equals(t.getId())).findFirst()
                        .orElseThrow();
        // stored scaled by two, reported as 13
        big.getAttributes().put(aum, Long.valueOf(1300L));

        assertThat(nameOfFirst(list(client, null, attributes("aum", "13"), null, null)), is("Big House"));
        assertThat(count(list(client, null, attributes("aum", "13.00"), null, null)), is(1));

        // the number as the file stores it is not what a caller can read back
        assertThat(count(list(client, null, attributes("aum", "1300"), null, null)), is(0));
    }

    @Test
    public void testTextAttributeIsMatchedExactlyIncludingCase()
    {
        var client = new Client();
        var fund = named(client, "iShares World");
        fund.getAttributes().put(
                        client.getSettings().getAttributeTypes().filter(t -> "vendor".equals(t.getId())).findFirst()
                                        .orElseThrow(), "iShares");

        assertThat(count(list(client, null, attributes("vendor", "iShares"), null, null)), is(1));
        assertThat(count(list(client, null, attributes("vendor", "ishares"), null, null)), is(0));
    }

    @Test
    public void testEmptyParameterAsksForInstrumentsHoldingNoValue()
    {
        var client = new Client();
        var withVendor = named(client, "With Vendor");
        named(client, "Without Vendor");
        withVendor.getAttributes().put(
                        client.getSettings().getAttributeTypes().filter(t -> "vendor".equals(t.getId())).findFirst()
                                        .orElseThrow(), "iShares");

        var answer = list(client, null, attributes("vendor", ""), null, null);

        assertThat(count(answer), is(1));
        assertThat(nameOfFirst(answer), is("Without Vendor"));
    }

    @Test
    public void testBooleanAttributeAcceptsBothSpellingsOfTheWords()
    {
        var client = new Client();
        var reviewed = booleanAttribute(client, "reviewed");
        var yes = named(client, "Reviewed Fund");
        var no = named(client, "Overlooked Fund");
        yes.getAttributes().put(reviewed, Boolean.TRUE);
        no.getAttributes().put(reviewed, Boolean.FALSE);

        assertThat(nameOfFirst(list(client, null, attributes("reviewed", "true"), null, null)), is("Reviewed Fund"));
        assertThat(nameOfFirst(list(client, null, attributes("reviewed", "TRUE"), null, null)), is("Reviewed Fund"));
        assertThat(nameOfFirst(list(client, null, attributes("reviewed", "false"), null, null)),
                        is("Overlooked Fund"));
    }

    @Test
    public void testAttributeValueOfTheWrongShapeIsRefused()
    {
        var client = new Client();
        booleanAttribute(client, "reviewed");

        var error = expectRejection(() -> list(client, null, attributes("reviewed", "yes"), null, null));
        assertThat(error.field(), is("attribute:reviewed"));
        assertThat(error.code(), is("invalid-value"));

        var number = expectRejection(() -> list(client, null, attributes("ter", "cheap"), null, null));
        assertThat(number.field(), is("attribute:ter"));
        assertThat(number.code(), is("invalid-value"));
    }

    @Test
    public void testUnknownAttributeIdIsRefused()
    {
        var client = new Client();

        var error = expectRejection(() -> list(client, null, attributes("nope", "1"), null, null));
        assertThat(error.field(), is("attribute:nope"));
        assertThat(error.code(), is("unknown-attribute"));
    }

    @Test
    public void testAttributeOfAnotherKindIsRefused()
    {
        var client = new Client();
        var accountAttribute = new AttributeType("broker");
        accountAttribute.setName("Broker");
        accountAttribute.setTarget(name.abuchen.portfolio.model.Account.class);
        accountAttribute.setType(String.class);
        accountAttribute.setConverter(AttributeType.StringConverter.class);
        client.getSettings().addAttributeType(accountAttribute);

        var error = expectRejection(() -> list(client, null, attributes("broker", "Flatex"), null, null));
        assertThat(error.field(), is("attribute:broker"));
        assertThat(error.code(), is("attribute-not-applicable"));
    }

    @Test
    public void testImageAttributeCannotBeFilteredOnBecauseItsValueNeverTravelsOut()
    {
        var client = new Client();

        var error = expectRejection(() -> list(client, null, attributes("logo", "anything"), null, null));
        assertThat(error.field(), is("attribute:logo"));
        assertThat(error.code(), is("unsupported-attribute-type"));
    }

    @Test
    public void testWatchlistMembershipNarrowsTheList()
    {
        var client = new Client();
        var held = named(client, "Held In Core");
        named(client, "Not In Core");
        var core = new Watchlist();
        core.setName("Core");
        core.addSecurity(held);
        client.addWatchlist(core);

        var answer = list(client, null, new SecuritiesHandler.Filters("Core", null, Map.of()), null, null);

        assertThat(count(answer), is(1));
        assertThat(nameOfFirst(answer), is("Held In Core"));
    }

    @Test
    public void testEveryWatchlistOfTheNameCounts()
    {
        var client = new Client();
        named(client, "Outside");

        for (var name : new String[] { "First Twin", "Second Twin" })
        {
            var list = new Watchlist();
            list.setName("Twins");
            list.addSecurity(named(client, name));
            client.addWatchlist(list);
        }
        named(client, "Outside");

        // a file the application wrote can hold two lists under one name; the
        // narrowing means either of them, not whichever came first
        assertThat(count(list(client, null, new SecuritiesHandler.Filters("Twins", null, Map.of()), null, null)),
                        is(2));
    }

    @Test
    public void testUnknownWatchlistIsRefusedAndNamesTheListsTheFileHas()
    {
        var client = new Client();
        var core = new Watchlist();
        core.setName("Core");
        client.addWatchlist(core);

        var error = expectRejection(() -> list(client, null, new SecuritiesHandler.Filters("Corey", null, Map.of()),
                        null, null));
        assertThat(error.field(), is("watchlist"));
        assertThat(error.code(), is("invalid-value"));
        assertThat(error.message().contains("Core"), is(true));
    }

    @Test
    public void testClassificationIsFoundById()
    {
        var client = new Client();
        var technology = named(client, "Technology Fund");
        named(client, "Unassigned Bond");

        var taxonomy = new Taxonomy("Sectors");
        var root = new Classification("root", "Sectors");
        var tech = new Classification(root, "tech", "Technology");
        root.addChild(tech);
        taxonomy.setRootNode(root);
        tech.addAssignment(new Classification.Assignment(technology));
        client.addTaxonomy(taxonomy);

        var answer = list(client, null, new SecuritiesHandler.Filters(null, "tech", Map.of()), null, null);

        assertThat(count(answer), is(1));
        assertThat(nameOfFirst(answer), is("Technology Fund"));
    }

    @Test
    public void testUnknownClassificationIsRefused()
    {
        var client = new Client();

        var error = expectRejection(() -> list(client, null, new SecuritiesHandler.Filters(null, "nope", Map.of()),
                        null, null));
        assertThat(error.field(), is("classification"));
        assertThat(error.code(), is("invalid-value"));
    }

    @Test
    public void testEveryCriterionGivenHasToHold()
    {
        var client = new Client();
        var inBoth = named(client, "In Both");
        var onlyWatchlist = named(client, "Only Watchlist");
        var vendor = client.getSettings().getAttributeTypes().filter(t -> "vendor".equals(t.getId())).findFirst()
                        .orElseThrow();

        var taxonomy = new Taxonomy("Sectors");
        var root = new Classification("root", "Sectors");
        var tech = new Classification(root, "tech", "Technology");
        root.addChild(tech);
        taxonomy.setRootNode(root);
        tech.addAssignment(new Classification.Assignment(inBoth));
        tech.addAssignment(new Classification.Assignment(onlyWatchlist));
        client.addTaxonomy(taxonomy);

        for (var security : new Security[] { inBoth, onlyWatchlist })
        {
            var watchlist = new Watchlist();
            watchlist.setName("Core");
            watchlist.addSecurity(security);
            client.addWatchlist(watchlist);
        }
        inBoth.getAttributes().put(vendor, "iShares");

        var filters = new SecuritiesHandler.Filters("Core", "tech", Map.of("vendor", "iShares"));
        assertThat(count(list(client, null, filters, null, null)), is(1));
        assertThat(nameOfFirst(list(client, null, filters, null, null)), is("In Both"));

        // together with the free-text query, which keeps its own meaning
        assertThat(count(list(client, "Only", filters, null, null)), is(0));
        assertThat(count(list(client, "Both", filters, null, null)), is(1));
    }

    @Test
    public void testAPageCountsTheNarrowedListAndNothingElse()
    {
        var client = new Client();
        for (var name : new String[] { "Alpha", "Alps", "Beta" })
            named(client, name);
        var vendor = client.getSettings().getAttributeTypes().filter(t -> "vendor".equals(t.getId())).findFirst()
                        .orElseThrow();
        client.getSecurities().forEach(security -> {
            if (!"Beta".equals(security.getName()))
                security.getAttributes().put(vendor, "iShares");
        });

        var page = list(client, null, attributes("vendor", "iShares"), "", "1").getAsJsonObject();

        assertThat(page.getAsJsonObject("page").get("total").getAsInt(), is(2));
        assertThat(page.getAsJsonArray("items").size(), is(1));
    }

    @Test
    public void testACursorIsGoodOnlyForTheNarrowingItCameWith()
    {
        var client = new Client();
        var vendor = client.getSettings().getAttributeTypes().filter(t -> "vendor".equals(t.getId())).findFirst()
                        .orElseThrow();
        for (var name : new String[] { "Alpha", "Alps", "Beta", "Bravo" })
        {
            var security = named(client, name);
            if (name.startsWith("A"))
                security.getAttributes().put(vendor, "iShares");
        }

        var firstPage = list(client, null, SecuritiesHandler.Filters.NONE, "", "1").getAsJsonObject();
        var cursor = firstPage.getAsJsonObject("page").get("nextCursor").getAsString();

        // the same file, the same limit, one attribute more: continued where it
        // left off, the walk would skip everything the criterion left out
        var error = expectRejection(() -> list(client, null, attributes("vendor", "iShares"), cursor, "1"));
        assertThat(error.field(), is("cursor"));
        assertThat(error.code(), is("invalid-value"));

        // and the narrowed list pages over its own two items, counted on its own
        var narrowed = list(client, null, attributes("vendor", "iShares"), "", "1").getAsJsonObject()
                        .getAsJsonObject("page");
        assertThat(narrowed.get("total").getAsInt(), is(2));
    }

    @Test
    public void testFiltersAreReadOutOfTheQueryParameters()
    {
        var request = new Request("GET", "/v1/files/x/instruments", Map.of(), Map.of( //
                        "query", "apple", //
                        "watchlist", "Core", //
                        "classification", "tech", //
                        "attribute:ter", "0.008", //
                        "attribute:vendor", ""), new byte[0]);

        var filters = SecuritiesHandler.filtersOf(request);

        assertThat(filters.watchlist(), is("Core"));
        assertThat(filters.classification(), is("tech"));
        assertThat(filters.attributes().size(), is(2));
        assertThat(filters.attributes().get("ter"), is("0.008"));
        assertThat(filters.attributes().get("vendor"), is(""));
    }

    @Test
    public void testAnInstrumentWithoutAnyOfTheCriteriaStillAnswersWhenNothingIsNarrowed()
    {
        var client = new Client();
        named(client, "Alone");

        assertThat(count(list(client, null, SecuritiesHandler.Filters.NONE, null, null)), is(1));
        assertThat(count(list(client, null, new SecuritiesHandler.Filters(null, null, Map.of()), null, null)), is(1));
    }
}
