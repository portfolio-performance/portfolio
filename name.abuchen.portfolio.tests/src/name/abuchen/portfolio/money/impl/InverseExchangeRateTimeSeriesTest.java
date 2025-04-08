package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import name.abuchen.portfolio.money.ExchangeRate;

import org.junit.Test;

@SuppressWarnings("nls")
public class InverseExchangeRateTimeSeriesTest
{

    @Test
    public void testInverseLookupOfExchangeRate()
    {
        ExchangeRateTimeSeriesImpl source = new ExchangeRateTimeSeriesImpl(null, "EUR", "USD");
        source.addRate(new ExchangeRate(LocalDate.parse("2014-12-01"), BigDecimal.valueOf(1)));
        source.addRate(new ExchangeRate(LocalDate.parse("2014-12-02"), BigDecimal.valueOf(2)));
        source.addRate(new ExchangeRate(LocalDate.parse("2014-12-03"), BigDecimal.valueOf(3)));

        InverseExchangeRateTimeSeries inverse = new InverseExchangeRateTimeSeries(source);

        assertThat(inverse.lookupRate(LocalDate.parse("2014-11-30")).get().getValue(), is(new BigDecimal(1).setScale(10)));
        assertThat(inverse.lookupRate(LocalDate.parse("2014-12-01")).get().getValue(), is(new BigDecimal(1).setScale(10)));
        assertThat(inverse.lookupRate(LocalDate.parse("2014-12-02")).get().getValue(),
                        is(new BigDecimal(0.500).setScale(10)));
        assertThat(inverse.lookupRate(LocalDate.parse("2014-12-03")).get().getValue(), is(BigDecimal.valueOf(0.3333333333)));
        assertThat(inverse.lookupRate(LocalDate.parse("2014-12-04")).get().getValue(), is(BigDecimal.valueOf(0.3333333333)));

        assertThat(inverse.getBaseCurrency(), is(source.getTermCurrency()));
        assertThat(inverse.getTermCurrency(), is(source.getBaseCurrency()));
    }

    @Test
    public void testIfNoRatesExist()
    {
        ExchangeRateTimeSeriesImpl source = new ExchangeRateTimeSeriesImpl();
        InverseExchangeRateTimeSeries inverse = new InverseExchangeRateTimeSeries(source);

        assertThat(inverse.lookupRate(LocalDate.parse("2014-11-30")).isPresent(), is(false));
    }

}
