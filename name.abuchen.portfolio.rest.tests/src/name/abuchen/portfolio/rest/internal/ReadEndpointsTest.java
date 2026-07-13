package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
