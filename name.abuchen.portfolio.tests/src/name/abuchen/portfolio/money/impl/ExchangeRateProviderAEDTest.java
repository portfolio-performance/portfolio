package name.abuchen.portfolio.money.impl;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class ExchangeRateProviderAEDTest
{
    @Test
    public void testIt()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

        ExchangeRateTimeSeries usd_aed = factory.getTimeSeries("USD", "AED");
        assertThat(usd_aed.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("3.6725")));

        ExchangeRateTimeSeries aed_usd = factory.getTimeSeries("AED", "USD");
        assertThat(aed_usd.lookupRate(LocalDate.now()).get().getValue(),
                        comparesEqualTo(BigDecimal.ONE.divide(new BigDecimal("3.6725"), 10, RoundingMode.HALF_DOWN)));

        // EUR -> USD -> AED
        // default value EUR -> USD is 1.0836
        double calculatedRate = 1.0836d * 3.6725d;

        ExchangeRateTimeSeries eur_aed = factory.getTimeSeries("EUR", "AED");
        assertThat(eur_aed.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));
    }
}
