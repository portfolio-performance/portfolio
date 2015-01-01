package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.impl.ChainedExchangeRateTimeSeries;
import name.abuchen.portfolio.money.impl.ExchangeRateTimeSeriesImpl;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class ChainedExchangeRateTimeSeriesTest
{

    @Test
    public void testChainedLookupOfExchangeRate()
    {
        ExchangeRateTimeSeriesImpl first = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        first.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1 * Values.ExchangeRate.factor()));
        first.addRate(new ExchangeRate(Dates.date("2014-12-02"), 2 * Values.ExchangeRate.factor()));
        first.addRate(new ExchangeRate(Dates.date("2014-12-03"), 3 * Values.ExchangeRate.factor()));

        ExchangeRateTimeSeriesImpl second = new ExchangeRateTimeSeriesImpl(null, "USD", "CHF");
        second.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1 * Values.ExchangeRate.factor()));
        second.addRate(new ExchangeRate(Dates.date("2014-12-02"), 2 * Values.ExchangeRate.factor()));
        second.addRate(new ExchangeRate(Dates.date("2014-12-03"), 3 * Values.ExchangeRate.factor()));

        ChainedExchangeRateTimeSeries chained = new ChainedExchangeRateTimeSeries(first, second);

        assertThat(chained.lookupRate(Dates.date("2014-11-30")).get().getValue(), is(1_0000L));
        assertThat(chained.lookupRate(Dates.date("2014-12-01")).get().getValue(), is(1_0000L));
        assertThat(chained.lookupRate(Dates.date("2014-12-02")).get().getValue(), is(4_0000L));
        assertThat(chained.lookupRate(Dates.date("2014-12-03")).get().getValue(), is(9_0000L));
        assertThat(chained.lookupRate(Dates.date("2014-12-04")).get().getValue(), is(9_0000L));

        assertThat(chained.getBaseCurrency(), is("EUR"));
        assertThat(chained.getTermCurrency(), is("CHF"));
    }

    @Test
    public void testIfNoRatesExist()
    {
        ExchangeRateTimeSeriesImpl first = new ExchangeRateTimeSeriesImpl();
        ExchangeRateTimeSeriesImpl second = new ExchangeRateTimeSeriesImpl();
        ChainedExchangeRateTimeSeries chained = new ChainedExchangeRateTimeSeries(first, second);

        assertThat(chained.lookupRate(Dates.date("2014-11-30")).isPresent(), is(false));
    }

    @Test
    public void testIfSomeRatesDoNotExist()
    {
        ExchangeRateTimeSeriesImpl first = new ExchangeRateTimeSeriesImpl();
        first.addRate(new ExchangeRate(Dates.date("2014-11-30"), 1 * Values.ExchangeRate.factor()));
        ExchangeRateTimeSeriesImpl second = new ExchangeRateTimeSeriesImpl();
        ChainedExchangeRateTimeSeries chained = new ChainedExchangeRateTimeSeries(first, second);

        assertThat(chained.lookupRate(Dates.date("2014-11-30")).isPresent(), is(false));
    }

}
