package name.abuchen.portfolio.model.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import name.abuchen.portfolio.model.ExchangeRate;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class InverseExchangeRateTimeSeriesTest
{

    @Test
    public void testInverseLookupOfExchangeRate()
    {
        ExchangeRateTimeSeriesImpl source = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        source.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1 * Values.ExchangeRate.factor()));
        source.addRate(new ExchangeRate(Dates.date("2014-12-02"), 2 * Values.ExchangeRate.factor()));
        source.addRate(new ExchangeRate(Dates.date("2014-12-03"), 3 * Values.ExchangeRate.factor()));

        InverseExchangeRateTimeSeries inverse = new InverseExchangeRateTimeSeries(source);

        assertThat(inverse.lookupRate(Dates.date("2014-11-30")).get().getValue(), is(1_0000L));
        assertThat(inverse.lookupRate(Dates.date("2014-12-01")).get().getValue(), is(1_0000L));
        assertThat(inverse.lookupRate(Dates.date("2014-12-02")).get().getValue(), is(5000L));
        assertThat(inverse.lookupRate(Dates.date("2014-12-03")).get().getValue(), is(3333L));
        assertThat(inverse.lookupRate(Dates.date("2014-12-04")).get().getValue(), is(3333L));

        assertThat(inverse.getBaseCurrency(), is(source.getTermCurrency()));
        assertThat(inverse.getTermCurrency(), is(source.getBaseCurrency()));
    }

    @Test
    public void testIfNoRatesExist()
    {
        ExchangeRateTimeSeriesImpl source = new ExchangeRateTimeSeriesImpl();
        InverseExchangeRateTimeSeries inverse = new InverseExchangeRateTimeSeries(source);

        assertThat(inverse.lookupRate(Dates.date("2014-11-30")).isPresent(), is(false));
    }

}
