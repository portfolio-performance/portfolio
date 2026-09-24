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
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

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
     * The retired flag is exposed and writable under the model's name,
     * {@code retired} (the UI says "deactivated").
     */
    @Test
    public void testRetiredFlagIsExposedAndWritable()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setRetired(true);

        var single = SecuritiesHandler.get(client, security.getUUID()).getAsJsonObject();
        assertThat(single.get("retired").getAsBoolean(), is(true));
        assertThat(single.has("isRetired"), is(false));

        SecuritiesHandler.patch(client, security.getUUID(), JsonParser.parseString("{\"retired\":false}") //
                        .getAsJsonObject());
        assertThat(security.isRetired(), is(false));

        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), JsonParser.parseString("{\"retired\":\"yes\"}") //
                            .getAsJsonObject());
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).code(), is("invalid-type"));
            assertThat(security.isRetired(), is(false)); // nothing applied
        }
    }

    @Test
    public void testRetiredFlagOfAccounts()
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);
        var portfolio = new PortfolioBuilder(account).addTo(client);
        account.setRetired(true);
        var factory = new ExchangeRateProviderFactory(client);

        assertThat(AccountsHandler.get(client, factory, account.getUUID(), null).getAsJsonObject().get("retired")
                        .getAsBoolean(), is(true));
        assertThat(PortfoliosHandler.get(client, factory, portfolio.getUUID(), null, null).getAsJsonObject()
                        .get("retired").getAsBoolean(), is(false));
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
        var factory = new ExchangeRateProviderFactory(client);

        var accounts = AccountsHandler.list(client, factory, null).getAsJsonObject().get("items").getAsJsonArray();
        assertThat(accounts.size(), is(1));
        assertThat(accounts.get(0).getAsJsonObject().has("balance"), is(true));

        var portfolios = PortfoliosHandler.list(client, factory, null, null).getAsJsonObject().get("items")
                        .getAsJsonArray();
        assertThat(portfolios.size(), is(1));
        assertThat(portfolios.get(0).getAsJsonObject().has("value"), is(true));
        assertThat(portfolios.get(0).getAsJsonObject().get("referenceCashAccount").getAsString(),
                        is(account.getUUID()));
    }
}
