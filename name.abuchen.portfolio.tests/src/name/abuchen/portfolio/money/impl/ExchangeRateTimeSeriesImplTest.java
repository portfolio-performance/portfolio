package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.impl.ExchangeRateTimeSeriesImpl;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class ExchangeRateTimeSeriesImplTest
{
    @Test
    public void testLookupOfExchangeRate()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();
        series.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1));
        series.addRate(new ExchangeRate(Dates.date("2014-12-02"), 2));
        series.addRate(new ExchangeRate(Dates.date("2014-12-03"), 3));

        assertThat(series.lookupRate(Dates.date("2014-11-30")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-01")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-02")).get().getValue(), is(2L));
        assertThat(series.lookupRate(Dates.date("2014-12-03")).get().getValue(), is(3L));
        assertThat(series.lookupRate(Dates.date("2014-12-04")).get().getValue(), is(3L));
    }

    @Test
    public void testAddingOfExchangeRates()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        series.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1));
        series.addRate(new ExchangeRate(Dates.date("2014-12-01"), 2));

        assertProperties(series);
    }

    @Test
    public void testCreationFromTemplate()
    {
        ExchangeRateTimeSeriesImpl template = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        template.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1));
        template.addRate(new ExchangeRate(Dates.date("2014-12-01"), 2));

        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl(template);

        assertProperties(series);
    }

    private void assertProperties(ExchangeRateTimeSeriesImpl series)
    {
        assertThat(series.getBaseCurrency(), is("EUR"));
        assertThat(series.getTermCurrency(), is("USD"));
        assertThat(series.lookupRate(Dates.date("2014-12-01")).get().getValue(), is(2L));
        assertThat(series.getLatest().get(), is(new ExchangeRate(Dates.date("2014-12-01"), 2)));
    }

    @Test
    public void testLookupOfExchangeRateWithGaps()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();
        series.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1));
        series.addRate(new ExchangeRate(Dates.date("2014-12-03"), 3));

        assertThat(series.lookupRate(Dates.date("2014-11-30")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-01")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-02")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-03")).get().getValue(), is(3L));
        assertThat(series.lookupRate(Dates.date("2014-12-04")).get().getValue(), is(3L));
    }

    @Test
    public void testLookupOfExchangeRateWithNoRates()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();

        assertThat(series.lookupRate(Dates.date("2014-12-01")).isPresent(), is(false));
    }

    @Test
    public void testLookupOfExchangeRateWithOneRateOnly()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();
        series.addRate(new ExchangeRate(Dates.date("2014-12-01"), 1));

        assertThat(series.lookupRate(Dates.date("2014-11-30")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-01")).get().getValue(), is(1L));
        assertThat(series.lookupRate(Dates.date("2014-12-02")).get().getValue(), is(1L));
    }

}
