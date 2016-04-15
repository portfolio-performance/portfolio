package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class ECBExchangeRateProviderTest
{
    @Test
    public void testLookup()
    {
        ECBData data = new ECBData();
        data.addSeries(new ExchangeRateTimeSeriesImpl(null, "EUR", "CHF"));
        data.addSeries(new ExchangeRateTimeSeriesImpl(null, "EUR", "USD"));
        data.addSeries(new ExchangeRateTimeSeriesImpl(null, "EUR", "GBP"));
        ECBExchangeRateProvider provider = new ECBExchangeRateProvider(data);

        assertThat(provider.getTimeSeries("EUR", "CHF"), instanceOf(ExchangeRateTimeSeriesImpl.class));
        assertThat(provider.getTimeSeries("CHF", "EUR"), instanceOf(InverseExchangeRateTimeSeries.class));
        assertThat(provider.getTimeSeries("EUR", "XXX"), is(nullValue()));
        assertThat(provider.getTimeSeries("XXX", "EUR"), is(nullValue()));
        assertThat(provider.getTimeSeries("GBP", "XXX"), is(nullValue()));
        assertThat(provider.getTimeSeries("XXX", "GBP"), is(nullValue()));
        assertThat(provider.getTimeSeries("XZY", "XXX"), is(nullValue()));

        ExchangeRateTimeSeries timeSeries = provider.getTimeSeries("CHF", "USD");
        assertThat(timeSeries, instanceOf(ChainedExchangeRateTimeSeries.class));
        assertThat(timeSeries.getBaseCurrency(), is("CHF"));
        assertThat(timeSeries.getTermCurrency(), is("USD"));
    }

}
