package name.abuchen.portfolio.money.impl;

import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class GBXExchangeRateProviderTest
{
    @Test
    public void testIt()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory();

        // default value EUR -> GBP is 0.72666
        ExchangeRateTimeSeries eur_gbx = factory.getTimeSeries("EUR", "GBX");
        assertThat(eur_gbx.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("72.666")));

        // inverse of default EUR -> GBP
        ExchangeRateTimeSeries gbx_eur = factory.getTimeSeries("GBX", "EUR");
        assertThat(gbx_eur.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(
                        BigDecimal.ONE.divide(new BigDecimal("72.666"), 12, BigDecimal.ROUND_HALF_DOWN)));

        // GBX -> GBP
        ExchangeRateTimeSeries gbx_gbp = factory.getTimeSeries("GBX", "GBP");
        assertThat(gbx_gbp.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("0.01")));

        // GBP -> GBX
        ExchangeRateTimeSeries gbp_gbx = factory.getTimeSeries("GBP", "GBX");
        assertThat(gbp_gbx.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal(100.0)));
    }
}
