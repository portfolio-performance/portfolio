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
public class ExchangeRateProviderGBXTest
{
    @Test
    public void testIt()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

        // default value EUR -> GBP is 0.72666
        ExchangeRateTimeSeries eur_gbx = factory.getTimeSeries("EUR", "GBX");
        assertThat(eur_gbx.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("72.666")));

        // inverse of default EUR -> GBP
        ExchangeRateTimeSeries gbx_eur = factory.getTimeSeries("GBX", "EUR");
        assertThat(gbx_eur.lookupRate(LocalDate.now()).get().getValue(),
                        comparesEqualTo(BigDecimal.ONE.divide(new BigDecimal("72.666"), 12, RoundingMode.HALF_DOWN)));

        // GBX -> GBP
        ExchangeRateTimeSeries gbx_gbp = factory.getTimeSeries("GBX", "GBP");
        assertThat(gbx_gbp.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("0.01")));

        // GBP -> GBX
        ExchangeRateTimeSeries gbp_gbx = factory.getTimeSeries("GBP", "GBX");
        assertThat(gbp_gbx.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal(100.0)));

        // GBX -> USD
        // default value EUR -> GBP is 0.72666
        // default value EUR -> USD is 1.0836
        double calculatedRate = 0.01d * (1 / 0.72666d) * 1.0836d;

        ExchangeRateTimeSeries gbx_usd = factory.getTimeSeries("GBX", "USD");
        assertThat(gbx_usd.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));

        // USD -> GBX
        calculatedRate = (1 / 1.0836d) * 0.72666d * 100;

        ExchangeRateTimeSeries usd_gbx = factory.getTimeSeries("USD", "GBX");
        assertThat(usd_gbx.lookupRate(LocalDate.now()).get().getValue().doubleValue(),
                        closeTo(calculatedRate, 0.00000001));

    }
}
