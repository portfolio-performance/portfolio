package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;

/**
 * Watchlists as the file keeps them: named baskets whose contents are optional.
 * They are the one thing in a file without an identifier, so the name is both
 * what a reader sees and all it can quote back.
 */
@SuppressWarnings("nls")
public class WatchlistsHandlerTest
{
    private static final String FILE = "C:/portfolio/Richard.portfolio";

    private static JsonObject list(Client client)
    {
        return WatchlistsHandler.list(client, FILE, null, null, null, null);
    }

    /** one page of the lists, as a caller that walks them asks for it */
    private static JsonObject page(Client client, String cursor, String limit)
    {
        return WatchlistsHandler.list(client, FILE, null, null, cursor, limit);
    }

    private static Security security(Client client, String name)
    {
        var security = new Security();
        security.setName(name);
        security.setCurrencyCode("EUR");
        client.addSecurity(security);
        return security;
    }

    private static Watchlist watchlist(Client client, String name, Security... securities)
    {
        var watchlist = new Watchlist();
        watchlist.setName(name);
        for (var security : securities)
            watchlist.addSecurity(security);
        client.addWatchlist(watchlist);
        return watchlist;
    }

    private static JsonObject byName(JsonArray items, String name)
    {
        for (var element : items)
        {
            var json = element.getAsJsonObject();
            if (json.get("name").getAsString().equals(name))
                return json;
        }
        throw new AssertionError("no watchlist " + name);
    }

    @Test
    public void testFileWithoutWatchlistsSaysSo()
    {
        var json = list(new Client());
        assertThat(json.get("items").getAsJsonArray().size(), is(0));
        assertThat(json.get("empty").getAsString(), is("this file has no watchlists"));
    }

    /** by default a list only says how full it is, because a working one holds hundreds */
    @Test
    public void testCountsWithoutContents()
    {
        var client = new Client();
        var one = security(client, "ACME");
        var two = security(client, "Other");
        watchlist(client, "Beobachtung", one, two);
        watchlist(client, "Leerstand");

        var items = list(client).get("items").getAsJsonArray();
        assertThat(items.size(), is(2));
        assertThat(byName(items, "Beobachtung").get("instrumentCount").getAsInt(), is(2));
        assertThat(byName(items, "Beobachtung").has("instruments"), is(false));
        assertThat(byName(items, "Leerstand").get("instrumentCount").getAsInt(), is(0));
    }

    @Test
    public void testContentsOnRequest()
    {
        var client = new Client();
        var one = security(client, "ACME");
        watchlist(client, "Beobachtung", one);

        var json = WatchlistsHandler.list(client, FILE, null, "true", null, null);
        var instruments = byName(json.get("items").getAsJsonArray(), "Beobachtung").get("instruments")
                        .getAsJsonArray();
        assertThat(instruments.size(), is(1));
        assertThat(instruments.get(0).getAsJsonObject().get("name").getAsString(), is("ACME"));
        assertThat(instruments.get(0).getAsJsonObject().get("uuid").getAsString(), is(one.getUUID()));
    }

    /** where a name is kept, without pulling every list of the file along */
    @Test
    public void testNarrowedToOneInstrument()
    {
        var client = new Client();
        var acme = security(client, "ACME");
        var other = security(client, "Other");
        watchlist(client, "Beobachtung", acme, other);
        watchlist(client, "Nur Anderes", other);
        watchlist(client, "Leerstand");

        var items = WatchlistsHandler.list(client, FILE, acme.getUUID(), null, null, null).get("items")
                        .getAsJsonArray();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("name").getAsString(), is("Beobachtung"));
    }

    /** an instrument of this file that no list holds is an empty answer, not a refusal */
    @Test
    public void testInstrumentOfNoList()
    {
        var client = new Client();
        var acme = security(client, "ACME");
        security(client, "Other");
        watchlist(client, "Beobachtung", acme);

        var other = client.getSecurities().get(1);
        var json = WatchlistsHandler.list(client, FILE, other.getUUID(), null, null, null);
        assertThat(json.get("items").getAsJsonArray().size(), is(0));
        assertThat(json.get("empty").getAsString(), is("no watchlist of this file holds this instrument"));
    }

    /** and a uuid of no instrument at all is refused, pointing at the list that has it */
    @Test
    public void testInstrumentOfNoFileIsRefused()
    {
        var client = new Client();
        watchlist(client, "Beobachtung");

        try
        {
            WatchlistsHandler.list(client, FILE, "f3e06d98-c176-4ee7-9fa7-c3c130d2f50f", null, null, null);
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("instrument"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            assertThat(e.getErrors().get(0).message(),
                            is("no instrument of this file has this uuid - see pp_list_instruments"));
            return;
        }
        throw new AssertionError("a uuid of no instrument was accepted");
    }

    @Test
    public void testAFlagOfNoUseIsRefused()
    {
        try
        {
            WatchlistsHandler.list(new Client(), FILE, null, "maybe", null, null);
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("withInstruments"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            return;
        }
        throw new AssertionError("a flag of no use was accepted");
    }

    /** without paging the answer stays what it was: the lists, in the file's order, no page */
    @Test
    public void testWithoutPagingNothingIsAdded()
    {
        var client = new Client();
        watchlist(client, "Beobachtung");
        watchlist(client, "Tech");

        var json = list(client);
        assertThat(json.has("page"), is(false));

        var items = json.get("items").getAsJsonArray();
        assertThat(items.size(), is(2));
        assertThat(items.get(0).getAsJsonObject().has("cursor"), is(false));
    }

    @Test
    public void testAPageHoldsTheListsAskedForAndNamesTheNext()
    {
        var client = new Client();
        watchlist(client, "Beobachtung");
        watchlist(client, "Tech");
        watchlist(client, "Rohstoffe");

        var json = page(client, null, "1");
        var page = json.get("page").getAsJsonObject();
        assertThat(page.get("limit").getAsInt(), is(1));
        assertThat(page.get("total").getAsInt(), is(3));
        assertThat(page.get("nextCursor").isJsonNull(), is(false));

        var items = json.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("name").getAsString(), is("Beobachtung"));
        assertThat(items.get(0).getAsJsonObject().has("cursor"), is(true));
    }

    /**
     * The walk holds every list exactly once, and - because the key carries the
     * list's place in the file rather than its name - in the order the
     * application shows them, not in an order of keys.
     */
    @Test
    public void testAWalkThroughThePagesHoldsEveryListOnce()
    {
        var client = new Client();
        watchlist(client, "Beobachtung");
        watchlist(client, "Tech");
        watchlist(client, "Rohstoffe");
        watchlist(client, "Leerstand");

        var seen = new ArrayList<String>();
        var pages = 0;
        String cursor = null;
        boolean anotherPage = true;

        while (anotherPage)
        {
            var json = page(client, cursor, "3");
            assertThat(json.get("page").getAsJsonObject().get("total").getAsInt(), is(4));

            json.get("items").getAsJsonArray()
                            .forEach(item -> seen.add(item.getAsJsonObject().get("name").getAsString()));

            var next = json.get("page").getAsJsonObject().get("nextCursor");
            anotherPage = !next.isJsonNull();
            cursor = anotherPage ? next.getAsString() : null;
            pages++;
        }

        assertThat(pages, is(2));
        assertThat(seen, contains("Beobachtung", "Tech", "Rohstoffe", "Leerstand"));
    }

    /** a page ends between lists and never inside one: what it shows of a list is all of it */
    @Test
    public void testAPageCarriesAWholeListWithItsInstruments()
    {
        var client = new Client();
        watchlist(client, "Beobachtung", security(client, "ACME"), security(client, "Other"),
                        security(client, "Third"));
        watchlist(client, "Andere", security(client, "ACME"));

        var items = WatchlistsHandler.list(client, FILE, null, "true", null, "1").get("items").getAsJsonArray();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("name").getAsString(), is("Beobachtung"));
        assertThat(items.get(0).getAsJsonObject().get("instrumentCount").getAsInt(), is(3));
        assertThat(items.get(0).getAsJsonObject().get("instruments").getAsJsonArray().size(), is(3));
    }

    /**
     * Two lists of one name - what a hand-edited file can hold and this API has
     * no second way of telling apart - are two items of the walk, not one that a
     * page would skip: a key of only the name would carry both and hide the
     * second from every page after it.
     */
    @Test
    public void testTwoListsOfOneNameAreToldApart()
    {
        var client = new Client();
        watchlist(client, "Tech");
        watchlist(client, "Tech");

        var items = new ArrayList<JsonObject>();
        String cursor = null;
        boolean anotherPage = true;

        while (anotherPage)
        {
            var json = page(client, cursor, "1");
            json.get("items").getAsJsonArray().forEach(item -> items.add(item.getAsJsonObject()));

            var next = json.get("page").getAsJsonObject().get("nextCursor");
            anotherPage = !next.isJsonNull();
            cursor = anotherPage ? next.getAsString() : null;
        }

        assertThat(items.size(), is(2));
        assertThat(items.get(0).get("cursor").getAsString(), is(not(items.get(1).get("cursor").getAsString())));
    }

    /** a cursor of another list is refused, not answered with a page of these lists */
    @Test
    public void testACursorOfAnotherListIsRefused()
    {
        var client = new Client();
        watchlist(client, "Beobachtung");

        var foreign = Paging.encode(Paging.Kind.HOLDINGS, Paging.scopeHash(scopeOf(FILE)), "000|Beobachtung");
        assertCursorRefused(client, foreign);
    }

    /**
     * and so is a cursor of these very lists taken for another file: watchlist
     * keys are small places and common names, so one file's cursor would quietly
     * skip lists of the next.
     */
    @Test
    public void testACursorOfAnotherFileIsRefused()
    {
        var client = new Client();
        watchlist(client, "Beobachtung");

        var otherFile = Paging.encode(Paging.Kind.WATCHLISTS,
                        Paging.scopeHash(scopeOf("C:/portfolio/Andere.portfolio")), "000|Beobachtung");
        assertCursorRefused(client, otherFile);
    }

    private static String scopeOf(String fileId)
    {
        return "watchlists|file=" + fileId + "|instrument="; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static void assertCursorRefused(Client client, String cursor)
    {
        try
        {
            page(client, cursor, "1");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("cursor"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            return;
        }
        throw new AssertionError("a cursor of another list was accepted");
    }
}
