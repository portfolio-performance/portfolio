package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import name.abuchen.portfolio.money.ExchangeRate;

import org.junit.Test;

@SuppressWarnings("nls")
public class ChainedExchangeRateTimeSeriesTest
{

    @Test
    public void testChainedLookupOfExchangeRate()
    {
        ExchangeRateTimeSeriesImpl first = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        first.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        first.addRate(new ExchangeRate(LocalDate.parse("2014-12-02"), BigDecimal.valueOf(2)));
        first.addRate(new ExchangeRate(LocalDate.parse("2014-12-03"), BigDecimal.valueOf(3)));

        ExchangeRateTimeSeriesImpl second = new ExchangeRateTimeSeriesImpl(null, "USD", "CHF");
        second.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        second.addRate(new ExchangeRate(LocalDate.parse("2014-12-02"), BigDecimal.valueOf(2)));
        second.addRate(new ExchangeRate(LocalDate.parse("2014-12-03"), BigDecimal.valueOf(3)));

        ChainedExchangeRateTimeSeries chained = new ChainedExchangeRateTimeSeries(first, second);

        assertThat(chained.lookupRate(LocalDate.parse("2014-11-30")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(chained.lookupRate(LocalDate.parse("2014-12-01")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(chained.lookupRate(LocalDate.parse("2014-12-02")).get().getValue(), is(BigDecimal.valueOf(4)));
        assertThat(chained.lookupRate(LocalDate.parse("2014-12-03")).get().getValue(), is(BigDecimal.valueOf(9)));
        assertThat(chained.lookupRate(LocalDate.parse("2014-12-04")).get().getValue(), is(BigDecimal.valueOf(9)));

        assertThat(chained.getBaseCurrency(), is("EUR"));
        assertThat(chained.getTermCurrency(), is("CHF"));
    }

    @Test
    public void testIfNoRatesExist()
    {
        ExchangeRateTimeSeriesImpl first = new ExchangeRateTimeSeriesImpl();
        ExchangeRateTimeSeriesImpl second = new ExchangeRateTimeSeriesImpl();
        ChainedExchangeRateTimeSeries chained = new ChainedExchangeRateTimeSeries(first, second);

        assertThat(chained.lookupRate(LocalDate.parse("2014-11-30")).isPresent(), is(false));
    }

    @Test
    public void testIfSomeRatesDoNotExist()
    {
        ExchangeRateTimeSeriesImpl first = new ExchangeRateTimeSeriesImpl();
        first.addRate(new ExchangeRate(LocalDate.parse("2014-11-30"), BigDecimal.valueOf(1)));
        ExchangeRateTimeSeriesImpl second = new ExchangeRateTimeSeriesImpl();
        ChainedExchangeRateTimeSeries chained = new ChainedExchangeRateTimeSeries(first, second);

        assertThat(chained.lookupRate(LocalDate.parse("2014-11-30")).isPresent(), is(false));
    }

}
