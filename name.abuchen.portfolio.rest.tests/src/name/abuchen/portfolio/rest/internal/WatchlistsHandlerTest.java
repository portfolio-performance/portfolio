package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    private static JsonObject list(Client client)
    {
        return WatchlistsHandler.list(client, null, null);
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

        var json = WatchlistsHandler.list(client, null, "true");
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

        var items = WatchlistsHandler.list(client, acme.getUUID(), null).get("items").getAsJsonArray();
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
        var json = WatchlistsHandler.list(client, other.getUUID(), null);
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
            WatchlistsHandler.list(client, "f3e06d98-c176-4ee7-9fa7-c3c130d2f50f", null);
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
            WatchlistsHandler.list(new Client(), null, "maybe");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("withInstruments"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            return;
        }
        throw new AssertionError("a flag of no use was accepted");
    }
}
