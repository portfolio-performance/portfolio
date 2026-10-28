package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;

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
     * One page of the instruments, in the file's order - of all of them, or of
     * the ones a query found - and no page when none was asked for.
     */
    @Test
    public void testOnePageOfTheInstruments()
    {
        var client = new Client();
        for (var name : List.of("ACME 1", "Other", "ACME 2", "ACME 3"))
            new SecurityBuilder().addTo(client).setName(name);

        var first = SecuritiesHandler.list(client, null, "1", "2").getAsJsonObject();
        assertThat(first.get("items").getAsJsonArray().size(), is(2));
        assertThat(first.get("items").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString(),
                        is("Other"));
        var page = first.getAsJsonObject("page");
        assertThat(page.get("offset").getAsInt(), is(1));
        assertThat(page.get("total").getAsInt(), is(4));
        assertThat(page.get("nextOffset").getAsInt(), is(3));

        // a query is paged among what it found
        var found = SecuritiesHandler.list(client, "acme", "2", null).getAsJsonObject();
        assertThat(found.get("items").getAsJsonArray().size(), is(1));
        assertThat(found.get("items").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString(),
                        is("ACME 3"));
        assertThat(found.getAsJsonObject("page").get("total").getAsInt(), is(3));
        assertThat(found.getAsJsonObject("page").get("nextOffset").isJsonNull(), is(true));

        assertThat(SecuritiesHandler.list(client, null).getAsJsonObject().has("page"), is(false));

        try
        {
            SecuritiesHandler.list(client, " ", "-1", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            // every problem in one answer
            assertThat(e.getErrors().size(), is(2));
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
