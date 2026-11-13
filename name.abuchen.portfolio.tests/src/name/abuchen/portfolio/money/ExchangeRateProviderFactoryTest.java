package name.abuchen.portfolio.money;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ExchangeRateProviderFactoryTest
{
    /** ARS is a known currency, but no built-in provider quotes it */
    @Test
    public void testAPairWithoutAnySeriesHasNone()
    {
        var factory = new ExchangeRateProviderFactory(new Client());

        assertThat(factory.hasTimeSeries("ARS", CurrencyUnit.EUR), is(false));
    }

    /** direct (EUR/USD), inverted (USD/EUR) and chained (GBP/USD via EUR) series all count */
    @Test
    public void testDirectInvertedAndChainedSeriesCount()
    {
        var factory = new ExchangeRateProviderFactory(new Client());

        assertThat(factory.hasTimeSeries(CurrencyUnit.EUR, CurrencyUnit.USD), is(true));
        assertThat(factory.hasTimeSeries(CurrencyUnit.USD, CurrencyUnit.EUR), is(true));
        assertThat(factory.hasTimeSeries("GBP", CurrencyUnit.USD), is(true));
    }

    /** an exchange-rate security is a series even before it holds a single price */
    @Test
    public void testAnExchangeRateSecurityWithoutPricesIsASeries()
    {
        var client = new Client();
        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(rates);

        assertThat(new ExchangeRateProviderFactory(client).hasTimeSeries("ARS", CurrencyUnit.EUR), is(true));
    }
}
