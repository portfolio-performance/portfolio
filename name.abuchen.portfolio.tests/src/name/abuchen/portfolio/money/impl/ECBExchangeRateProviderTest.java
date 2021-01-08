package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class ECBExchangeRateProviderTest
{
    @Test
    public void testLookup()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

        assertThat(factory.getTimeSeries("EUR", "CHF"), instanceOf(ExchangeRateTimeSeriesImpl.class));
        assertThat(factory.getTimeSeries("CHF", "EUR"), instanceOf(InverseExchangeRateTimeSeries.class));
        
        assertThat(factory.getTimeSeries("EUR", "XXX"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("XXX", "EUR"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("GBP", "XXX"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("XXX", "GBP"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("XZY", "XXX"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("VND", "EUR"), instanceOf(EmptyExchangeRateTimeSeries.class));

        ExchangeRateTimeSeries timeSeries = factory.getTimeSeries("CHF", "USD");
        assertThat(timeSeries, instanceOf(ChainedExchangeRateTimeSeries.class));
        assertThat(timeSeries.getBaseCurrency(), is("CHF"));
        assertThat(timeSeries.getTermCurrency(), is("USD"));
    }

}
