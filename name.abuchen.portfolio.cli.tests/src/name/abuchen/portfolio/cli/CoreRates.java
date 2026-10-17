package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

/** Core's own currency converter over the same rates as an ECB cache file, as an oracle for the CLI's conversions. */
final class CoreRates
{
    private static final class TestProvider implements ExchangeRateProvider
    {
        @Override
        public String getName()
        {
            return "test";
        }

        @Override
        public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
        {
            return List.of();
        }
    }

    /** An independent oracle: the latest rate on or before the day, else the first, with core's weight. */
    private static final class TestSeries implements ExchangeRateTimeSeries
    {
        private final ExchangeRateProvider provider;
        private final String base;
        private final String term;
        private final List<ExchangeRate> rates;

        TestSeries(ExchangeRateProvider provider, String base, String term, List<ExchangeRate> rates)
        {
            this.provider = provider;
            this.base = base;
            this.term = term;
            this.rates = rates.stream().sorted((a, b) -> a.getTime().compareTo(b.getTime())).toList();
        }

        @Override
        public String getBaseCurrency()
        {
            return base;
        }

        @Override
        public String getTermCurrency()
        {
            return term;
        }

        @Override
        public Optional<ExchangeRateProvider> getProvider()
        {
            return Optional.of(provider);
        }

        @Override
        public List<ExchangeRate> getRates()
        {
            return rates;
        }

        @Override
        public Optional<ExchangeRate> lookupRate(LocalDate day)
        {
            return Optional.of(rates.stream().filter(r -> !r.getTime().isAfter(day)).reduce((a, b) -> b)
                            .orElse(rates.get(0)));
        }

        @Override
        public int getWeight()
        {
            return 2;
        }
    }

    private CoreRates()
    {
    }

    /** What core itself computes over the given rates. */
    static CurrencyConverter converter(String term, EcbCacheFiles.Series... series)
    {
        var provider = new TestProvider();

        var factory = new ExchangeRateProviderFactory(new Client())
        {
            @Override
            public List<ExchangeRateTimeSeries> getAvailableTimeSeries()
            {
                var all = new ArrayList<ExchangeRateTimeSeries>();
                for (var s : series)
                {
                    var rates = new ArrayList<ExchangeRate>();
                    s.rates().forEach((date, value) -> rates.add(new ExchangeRate(date, value)));
                    all.add(new TestSeries(provider, s.base(), s.term(), rates));
                }
                return all;
            }
        };
        return new CurrencyConverterImpl(factory, term);
    }
}
