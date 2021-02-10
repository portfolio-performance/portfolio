package name.abuchen.portfolio.money.impl;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class ExchangeRateProviderZACTest
{
    @Test
    public void testIt()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

        // default value EUR -> ZAR is 17.8953
        ExchangeRateTimeSeries eur_zar = factory.getTimeSeries("EUR", "ZAR");
        assertThat(eur_zar.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("17.8953")));

        // inverse of default EUR -> ZAR
        ExchangeRateTimeSeries zar_eur = factory.getTimeSeries("ZAR", "EUR");
        assertThat(zar_eur.lookupRate(LocalDate.now()).get().getValue(),
                        comparesEqualTo(BigDecimal.ONE.divide(new BigDecimal("17.8953"), 12, RoundingMode.HALF_DOWN)));

        // ZAR -> ZAC
        ExchangeRateTimeSeries zar_zac = factory.getTimeSeries("ZAR", "ZAC");
        assertThat(zar_zac.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("0.01")));

        // ZAC -> ZAR
        ExchangeRateTimeSeries zac_zar = factory.getTimeSeries("ZAC", "ZAR");
        assertThat(zac_zar.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal(100.0)));

        // ZAR -> USD
        // default value EUR -> ZAR is 17.8953
        // default value EUR -> USD is 1.0836
        double calculatedRate = 0.01d * (1 / 17.8953d) * 1.0836d;

        ExchangeRateTimeSeries zar_usd = factory.getTimeSeries("ZAR", "USD");
        assertThat(zar_usd.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));

        // USD -> ZAR
        calculatedRate = (1 / 1.0836d) * 17.8953d * 100;

        ExchangeRateTimeSeries usd_zar = factory.getTimeSeries("USD", "ZAR");
        assertThat(usd_zar.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));

    }
}