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
        ExchangeRateTimeSeries zar_zac = factory.getTimeSeries("GBX", "GBP");
        assertThat(zar_zac.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("0.01")));

        // inverse of default ZAR -> ZAC
        ExchangeRateTimeSeries zac_zar = factory.getTimeSeries("ZAC", "ZAR");
        assertThat(zac_zar.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal(100.0)));

        // ZAC -> EUR
        // default value EUR -> ZAR is 17.8953
        // default value EUR -> USD is 1.2104
        double calculatedRate = 0.01d * (1 / 17.8953d) * 1.2104d;

        ExchangeRateTimeSeries zac_eur = factory.getTimeSeries("ZAC", "EUR");
        assertThat(zac_eur.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));

        // EUR -> ZAC
        calculatedRate = (1 / 1.2104d) * 17.8953 * 100;

        ExchangeRateTimeSeries eur_zac = factory.getTimeSeries("EUR", "ZAC");
        assertThat(eur_zac.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));
    }
}
