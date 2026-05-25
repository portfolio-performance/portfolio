package name.abuchen.portfolio.money;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CustomCurrency;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class CurrencyUnitResolverTest
{
    @Test
    public void testResolveCustomCurrencyFromClient()
    {
        Client client = new Client();
        client.addCustomCurrency(new CustomCurrency("BTC", "Bitcoin", "₿"));

        assertThat(CurrencyUnit.getInstance("BTC"), is(nullValue()));
        assertThat(client.getCustomCurrencies().size(), is(1));

        CurrencyUnit bitcoin = CurrencyUnitResolver.resolve(client, "BTC");

        assertThat(bitcoin, is(notNullValue()));
        assertThat(bitcoin.getCurrencyCode(), is("BTC"));
        assertThat(bitcoin.getDisplayName(), is("Bitcoin"));
        assertThat(bitcoin.getCurrencySymbol(), is("₿"));
    }

    @Test
    public void testUsedCurrenciesContainCustomCurrency()
    {
        Client client = new Client();
        client.addCustomCurrency(new CustomCurrency("BTC", "Bitcoin", "₿"));

        client.addSecurity(new Security("Bitcoin", "BTC"));

        CurrencyUnit bitcoin = CurrencyUnitResolver.resolve(client, "BTC");

        assertThat(bitcoin, is(notNullValue()));
        assertThat(client.getUsedCurrencies(), hasItem(bitcoin));
    }
}
