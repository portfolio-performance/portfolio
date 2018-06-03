package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class ECBExchangeRateProviderTest
{
    @Test
    public void testLookup()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(null);

        assertThat(factory.getTimeSeries("EUR", "CHF"), instanceOf(ExchangeRateTimeSeriesImpl.class));
        assertThat(factory.getTimeSeries("CHF", "EUR"), instanceOf(InverseExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("EUR", "XXX"), is(nullValue()));
        assertThat(factory.getTimeSeries("XXX", "EUR"), is(nullValue()));
        assertThat(factory.getTimeSeries("GBP", "XXX"), is(nullValue()));
        assertThat(factory.getTimeSeries("XXX", "GBP"), is(nullValue()));
        assertThat(factory.getTimeSeries("XZY", "XXX"), is(nullValue()));
        assertThat(factory.getTimeSeries("VND", "EUR"), is(nullValue()));

        ExchangeRateTimeSeries timeSeries = factory.getTimeSeries("CHF", "USD");
        assertThat(timeSeries, instanceOf(ChainedExchangeRateTimeSeries.class));
        assertThat(timeSeries.getBaseCurrency(), is("CHF"));
        assertThat(timeSeries.getTermCurrency(), is("USD"));
    }

}
