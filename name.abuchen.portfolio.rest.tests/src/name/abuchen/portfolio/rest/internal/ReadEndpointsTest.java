package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ReadEndpointsTest
{
    @Test
    public void testSecuritiesListAndGet()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");
        security.setIsin("DE0001234567");

        var list = SecuritiesHandler.list(client).getAsJsonObject();
        assertThat(list.get("items").getAsJsonArray().size(), is(1));

        var single = SecuritiesHandler.get(client, security.getUUID()).getAsJsonObject();
        assertThat(single.get("name").getAsString(), is("ACME"));
        assertThat(single.get("isin").getAsString(), is("DE0001234567"));
        assertThat(single.get("uuid").getAsString(), is(security.getUUID()));
    }

    /**
     * `query` finds by part of the name, or by the whole ISIN, WKN or ticker -
     * ignoring case and surrounding whitespace. A large file is more than one
     * answer can carry, and this is how a name becomes a uuid without it.
     */
    @Test
    public void testInstrumentQuery()
    {
        var client = new Client();
        var acme = new SecurityBuilder().addTo(client);
        acme.setName("ACME Corp.");
        acme.setIsin("DE0005557508");
        acme.setWkn("555750");
        acme.setTickerSymbol("ACM.DE");
        var other = new SecurityBuilder().addTo(client);
        other.setName("Other Inc.");
        other.setIsin("US0378331005");
        other.setWkn("865985");
        other.setTickerSymbol("OTH.F");

        for (var query : List.of("acme", " corp ", "de0005557508", "555750", "acm.de"))
        {
            var items = SecuritiesHandler.list(client, query).getAsJsonObject().get("items").getAsJsonArray();
            assertThat(query, items.size(), is(1));
            assertThat(items.get(0).getAsJsonObject().get("uuid").getAsString(), is(acme.getUUID()));
        }

        // an ISIN is matched whole, not as part of one
        assertThat(SecuritiesHandler.list(client, "DE000555").getAsJsonObject().get("items").getAsJsonArray().size(),
                        is(0));
        // nothing found is an empty list, not an error
        assertThat(SecuritiesHandler.list(client, "nothing").getAsJsonObject().get("items").getAsJsonArray().size(),
                        is(0));
        // without a query, everything
        assertThat(SecuritiesHandler.list(client, null).getAsJsonObject().get("items").getAsJsonArray().size(), is(2));

        try
        {
            SecuritiesHandler.list(client, "   ");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("query"));
        }
    }

    /**
     * The instruments page by uuid: the pages walked with nextCursor are
     * disjoint and hold every instrument once, each item carries its cursor,
     * and no page is given when none was asked for.
     */
    @Test
    public void testThePagesOfTheInstrumentsHoldEveryInstrumentOnce()
    {
        var client = new Client();
        for (var name : List.of("ACME 1", "Other", "ACME 2", "ACME 3", "Fifth"))
            new SecurityBuilder().addTo(client).setName(name);

        var seen = new ArrayList<String>();
        String cursor = "";
        while (cursor != null)
        {
            var response = SecuritiesHandler.list(client, null, cursor, "2").getAsJsonObject();
            var items = response.get("items").getAsJsonArray();
            assertThat(items.size() <= 2, is(true));
            items.forEach(item -> {
                assertThat(item.getAsJsonObject().has("cursor"), is(true));
                seen.add(item.getAsJsonObject().get("uuid").getAsString());
            });
            assertThat(response.getAsJsonObject("page").get("total").getAsInt(), is(5));
            var next = response.getAsJsonObject("page").get("nextCursor");
            cursor = next.isJsonNull() ? null : next.getAsString();
        }

        var expected = client.getSecurities().stream().map(Security::getUUID).sorted().toList();
        assertThat(seen, is(expected));

        assertThat(SecuritiesHandler.list(client, null).getAsJsonObject().has("page"), is(false));
        assertThat(SecuritiesHandler.list(client, null).getAsJsonObject().get("items").getAsJsonArray().get(0)
                        .getAsJsonObject().has("cursor"), is(false));
    }

    /**
     * An instrument removed between two pages shifts nothing: the next page
     * continues after the last item seen, where an offset would skip one.
     */
    @Test
    public void testAnInstrumentRemovedBetweenPagesSkipsNothing()
    {
        var client = new Client();
        for (var name : List.of("A", "B", "C", "D"))
            new SecurityBuilder().addTo(client).setName(name);
        var ordered = client.getSecurities().stream().sorted(Comparator.comparing(Security::getUUID)).toList();

        var first = SecuritiesHandler.list(client, null, "", "2").getAsJsonObject();
        var cursor = first.getAsJsonObject("page").get("nextCursor").getAsString();

        client.removeSecurity(ordered.get(0));

        var second = SecuritiesHandler.list(client, null, cursor, "2").getAsJsonObject();
        var uuids = new ArrayList<String>();
        second.get("items").getAsJsonArray()
                        .forEach(item -> uuids.add(item.getAsJsonObject().get("uuid").getAsString()));
        assertThat(uuids, is(List.of(ordered.get(2).getUUID(), ordered.get(3).getUUID())));
        assertThat(second.getAsJsonObject("page").get("nextCursor").isJsonNull(), is(true));
    }

    /** a query is paged among what it found */
    @Test
    public void testAQueryIsPagedAmongWhatItFound()
    {
        var client = new Client();
        for (var name : List.of("ACME 1", "Other", "ACME 2", "ACME 3"))
            new SecurityBuilder().addTo(client).setName(name);

        var found = SecuritiesHandler.list(client, "acme", null, "2").getAsJsonObject();
        assertThat(found.get("items").getAsJsonArray().size(), is(2));
        assertThat(found.getAsJsonObject("page").get("total").getAsInt(), is(3));

        var rest = SecuritiesHandler.list(client, "acme",
                        found.getAsJsonObject("page").get("nextCursor").getAsString(), "2").getAsJsonObject();
        assertThat(rest.get("items").getAsJsonArray().size(), is(1));
        assertThat(rest.getAsJsonObject("page").get("nextCursor").isJsonNull(), is(true));
    }

    @Test
    public void testAMalformedCursorIsRefusedWithTheOtherProblems()
    {
        var client = new Client();
        new SecurityBuilder().addTo(client).setName("A");

        try
        {
            SecuritiesHandler.list(client, " ", "-1", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            // every problem in one answer
            assertThat(e.getErrors().size(), is(2));
            assertThat(e.getErrors().get(1).field(), is("cursor"));
        }

        // a trade cursor is no instrument cursor
        try
        {
            SecuritiesHandler.list(client, null,
                            Paging.encode(Paging.Kind.TRADES, Paging.scopeHash("file=|query="), "x"), null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("cursor"));
        }
    }

    /** a blank value is no value: omitted on instruments and accounts as an unset one is */
    @Test
    public void testABlankNoteIsOmitted()
    {
        var client = new Client();
        var blank = new SecurityBuilder().addTo(client);
        blank.setNote("  ");
        var set = new SecurityBuilder().addTo(client);
        set.setNote("kept");

        var items = SecuritiesHandler.list(client, null).getAsJsonObject().get("items").getAsJsonArray();
        assertThat(items.get(0).getAsJsonObject().has("note"), is(false));
        assertThat(items.get(1).getAsJsonObject().get("note").getAsString(), is("kept"));

        // the codes as well, while a set value goes out as stored
        var codes = new SecurityBuilder().addTo(client);
        codes.setIsin("");
        codes.setWkn(" ");
        codes.setTickerSymbol("");
        var spaced = new SecurityBuilder().addTo(client);
        spaced.setWkn("A19PCG ");
        items = SecuritiesHandler.list(client, null).getAsJsonObject().get("items").getAsJsonArray();
        var blankCodes = items.get(2).getAsJsonObject();
        assertThat(blankCodes.has("isin"), is(false));
        assertThat(blankCodes.has("wkn"), is(false));
        assertThat(blankCodes.has("tickerSymbol"), is(false));
        assertThat(items.get(3).getAsJsonObject().get("wkn").getAsString(), is("A19PCG "));

        var account = new name.abuchen.portfolio.model.Account();
        account.setName("Giro");
        account.setCurrencyCode("EUR");
        account.setNote("");
        assertThat(EntityJson.toJson(account).has("note"), is(false));
    }

    /**
     * Reported in review: a cursor is good only for the list it came from - the
     * same file and the same query (ignoring case, as the query does). One of
     * another file or query is refused, not answered with a page of a
     * different list.
     */
    @Test
    public void testACursorOfAnotherFileOrQueryIsRefused()
    {
        var client = new Client();
        for (var name : List.of("ACME 1", "ACME 2", "ACME 3"))
            new SecurityBuilder().addTo(client).setName(name);

        var cursor = SecuritiesHandler.list(client, "file-a", "acme", "", "1").getAsJsonObject()
                        .getAsJsonObject("page").get("nextCursor").getAsString();

        // the same list: also with the query in another case
        assertThat(SecuritiesHandler.list(client, "file-a", "ACME", cursor, "1").getAsJsonObject().get("items")
                        .getAsJsonArray().size(), is(1));

        for (var other : List.of(new String[] { "file-b", "acme" }, new String[] { "file-a", "acme 1" },
                        new String[] { "file-a", null }))
        {
            try
            {
                SecuritiesHandler.list(client, other[0], other[1], cursor, "1");
                Assert.fail("expected ApiException for " + other[0] + ", " + other[1]);
            }
            catch (ApiException e)
            {
                assertThat(e.getErrors().get(0).field(), is("cursor"));
                assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            }
        }
    }

    /**
     * Reported in review: a cursor this API did not write is refused - bytes
     * that are no UTF-8, or a key that no instrument could have.
     */
    @Test
    public void testACursorThatIsNoUtf8OrNoKeyIsRefused()
    {
        var client = new Client();
        new SecurityBuilder().addTo(client).setName("ACME");

        var prefix = ("v1:i:" + Paging.scopeHash("file=|query=") + ":").getBytes(StandardCharsets.UTF_8);
        var notUtf8 = Arrays.copyOf(prefix, prefix.length + 2);
        notUtf8[prefix.length] = (byte) 0xC3;
        notUtf8[prefix.length + 1] = (byte) 0x28;
        var withControl = ("v1:i:" + Paging.scopeHash("file=|query=") + ":a\u0000b").getBytes(StandardCharsets.UTF_8);

        for (var bytes : List.of(notUtf8, withControl, prefix))
        {
            try
            {
                SecuritiesHandler.list(client, null, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
                                null);
                Assert.fail("expected ApiException");
            }
            catch (ApiException e)
            {
                assertThat(e.getErrors().get(0).field(), is("cursor"));
                assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            }
        }
    }

    /**
     * The retired flag is not part of the API until the naming ("retired" in the
     * model, "deactivated" in the UI) is settled.
     */
    @Test
    public void testRetiredFlagIsNotExposed()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setRetired(true);

        var single = SecuritiesHandler.get(client, security.getUUID()).getAsJsonObject();
        assertThat(single.has("isRetired"), is(false));

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), JsonParser.parseString("{\"isRetired\":false}") //
                            .getAsJsonObject());
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).code(), is("unknown-field"));
            assertThat(security.isRetired(), is(true)); // nothing applied
        }
    }

    @Test
    public void testUnknownSecurityUuidIs404()
    {
        try
        {
            SecuritiesHandler.get(new Client(), "no-such-uuid");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testAccountsAndPortfolios()
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);
        new PortfolioBuilder(account).addTo(client);

        assertThat(AccountsHandler.list(client).getAsJsonObject().get("items").getAsJsonArray().size(), is(1));

        var portfolios = PortfoliosHandler.list(client).getAsJsonObject().get("items").getAsJsonArray();
        assertThat(portfolios.size(), is(1));
        assertThat(portfolios.get(0).getAsJsonObject().get("referenceCashAccount").getAsString(),
                        is(account.getUUID()));
    }
}
