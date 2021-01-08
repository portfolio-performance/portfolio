package name.abuchen.portfolio.money.impl;

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
public class ExchangeRateProviderILATest
{
    @Test
    public void testIt()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

        // default value EUR -> ILS is 422.10
        ExchangeRateTimeSeries eur_ILA = factory.getTimeSeries("EUR", "ILA");
        assertThat(eur_ILA.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("422.1")));

        // inverse of default EUR -> ILS
        ExchangeRateTimeSeries ILA_eur = factory.getTimeSeries("ILA", "EUR");
        assertThat(ILA_eur.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(
                        BigDecimal.ONE.divide(new BigDecimal("422.099999949897"), 12, RoundingMode.HALF_DOWN)));

        // ILA -> ILS
        ExchangeRateTimeSeries ILA_ILS = factory.getTimeSeries("ILA", "ILS");
        assertThat(ILA_ILS.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal("0.01")));

        // ILS -> ILA
        ExchangeRateTimeSeries ILS_ILA = factory.getTimeSeries("ILS", "ILA");
        assertThat(ILS_ILA.lookupRate(LocalDate.now()).get().getValue(), comparesEqualTo(new BigDecimal(100.0)));

    }
}
