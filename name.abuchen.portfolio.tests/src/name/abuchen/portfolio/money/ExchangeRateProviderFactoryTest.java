package name.abuchen.portfolio.money;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.impl.ExchangeRateTimeSeriesImpl;
import name.abuchen.portfolio.money.impl.SecurityBasedExchangeRateProvider;

@SuppressWarnings("nls")
public class ExchangeRateProviderFactoryTest
{
    /**
     * A provider that quotes a fixed set of currencies against the euro.
     * <p/>
     * The built-in providers are found by a {@code ServiceLoader}, whose
     * registration lives in {@code META-INF/services} of the bundle: a launch
     * that runs against the output folders - any plain JUnit launch, Infinitest
     * among them - finds none of them, and then no currency pair has a series
     * at all. The tests name their own providers to say what they measure.
     */
    private static ExchangeRateProvider quoting(String... termCurrencies)
    {
        return new ExchangeRateProvider()
        {
            @Override
            public String getName()
            {
                return "test";
            }

            @Override
            public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
            {
                return List.of(termCurrencies).stream() //
                                .map(term -> (ExchangeRateTimeSeries) new FixedExchangeRateTimeSeries(this, 1,
                                                BigDecimal.ONE, CurrencyUnit.EUR, term))
                                .toList();
            }
        };
    }

    /** a currency no provider of this test quotes has no series */
    @Test
    public void testAPairWithoutAnySeriesHasNone()
    {
        var factory = new ExchangeRateProviderFactory(new Client(), List.of());

        assertThat(factory.hasTimeSeries("ARS", CurrencyUnit.EUR), is(false));
    }

    /** direct (EUR/USD), inverted (USD/EUR) and chained (GBP/USD via EUR) series all count */
    @Test
    public void testDirectInvertedAndChainedSeriesCount()
    {
        var factory = new ExchangeRateProviderFactory(new Client(), List.of(quoting(CurrencyUnit.USD, "GBP")));

        assertThat(factory.hasTimeSeries(CurrencyUnit.EUR, CurrencyUnit.USD), is(true));
        assertThat(factory.hasTimeSeries(CurrencyUnit.USD, CurrencyUnit.EUR), is(true));
        assertThat(factory.hasTimeSeries("GBP", CurrencyUnit.USD), is(true));
    }

    /** an exchange-rate security is a series even before it holds a single price */
    @Test
    public void testAnExchangeRateSecurityWithoutPricesIsASeries()
    {
        var client = new Client();
        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(rates);

        var factory = new ExchangeRateProviderFactory(client, List.of(new SecurityBasedExchangeRateProvider()));

        assertThat(factory.hasTimeSeries("ARS", CurrencyUnit.EUR), is(true));
    }

    @Test
    public void testAsOfLookupDoesNotUseFutureRates()
    {
        var january = LocalDate.of(2023, 1, 2);
        var february = LocalDate.of(2023, 2, 1);
        var provider = new ExchangeRateProvider()
        {
            @Override
            public String getName()
            {
                return "test";
            }

            @Override
            public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
            {
                var usd = new ExchangeRateTimeSeriesImpl(this, CurrencyUnit.EUR, CurrencyUnit.USD);
                usd.addRate(new ExchangeRate(january, BigDecimal.ONE));
                var gbp = new ExchangeRateTimeSeriesImpl(this, CurrencyUnit.EUR, "GBP");
                gbp.addRate(new ExchangeRate(february, new BigDecimal("0.8")));
                return List.of(usd, gbp);
            }
        };
        var factory = new ExchangeRateProviderFactory(new Client(), List.of(provider));
        var direct = new CurrencyConverterImpl(factory, "GBP");
        var inverse = new CurrencyConverterImpl(factory, CurrencyUnit.EUR);
        var chained = new CurrencyConverterImpl(factory, CurrencyUnit.USD);

        assertThat(direct.findRate(january, CurrencyUnit.EUR).isEmpty(), is(true));
        assertThat(inverse.findRate(january, "GBP").isEmpty(), is(true));
        assertThat(chained.findRate(january, "GBP").isEmpty(), is(true));
        assertThat(chained.findRate(february, "GBP").orElseThrow().getValue(), is(new BigDecimal("1.2500000000")));
        assertThat(direct.getRate(january, CurrencyUnit.EUR).getTime(), is(february));
        assertThat(chained.getRate(january, "GBP").getValue(), is(new BigDecimal("1.2500000000")));
    }
}
