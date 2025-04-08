package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import name.abuchen.portfolio.money.ExchangeRate;

import org.junit.Test;

@SuppressWarnings("nls")
public class ExchangeRateTimeSeriesImplTest
{
    @Test
    public void testLookupOfExchangeRate()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-02"), BigDecimal.valueOf(2)));
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-03"), BigDecimal.valueOf(3)));

        assertThat(series.lookupRate(LocalDate.parse("2014-11-30")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-01")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-02")).get().getValue(), is(BigDecimal.valueOf(2)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-03")).get().getValue(), is(BigDecimal.valueOf(3)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-04")).get().getValue(), is(BigDecimal.valueOf(3)));
    }

    @Test
    public void testAddingOfExchangeRates()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(2)));

        assertProperties(series);
    }

    @Test
    public void testCreationFromTemplate()
    {
        ExchangeRateTimeSeriesImpl template = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        template.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        template.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(2)));

        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl(template);

        assertProperties(series);
    }

    private void assertProperties(ExchangeRateTimeSeriesImpl series)
    {
        assertThat(series.getBaseCurrency(), is("EUR"));
        assertThat(series.getTermCurrency(), is("USD"));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-01")).get().getValue(), is(BigDecimal.valueOf(2)));
        assertThat(series.getLatest().get(), is(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(2))));
    }

    @Test
    public void testLookupOfExchangeRateWithGaps()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-03"), BigDecimal.valueOf(3)));

        assertThat(series.lookupRate(LocalDate.parse("2014-11-30")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-01")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-02")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-03")).get().getValue(), is(BigDecimal.valueOf(3)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-04")).get().getValue(), is(BigDecimal.valueOf(3)));
    }

    @Test
    public void testLookupOfExchangeRateWithNoRates()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();

        assertThat(series.lookupRate(LocalDate.parse("2014-12-01")).isPresent(), is(false));
    }

    @Test
    public void testLookupOfExchangeRateWithOneRateOnly()
    {
        ExchangeRateTimeSeriesImpl series = new ExchangeRateTimeSeriesImpl();
        series.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));

        assertThat(series.lookupRate(LocalDate.parse("2014-11-30")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-01")).get().getValue(), is(BigDecimal.valueOf(1)));
        assertThat(series.lookupRate(LocalDate.parse("2014-12-02")).get().getValue(), is(BigDecimal.valueOf(1)));
    }

}
