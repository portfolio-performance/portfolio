package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Cache for calculation results of DataSeries.
 */
public class DataSeriesCache
{
    private static class CacheKey
    {
        private final Class<?> type;
        private final Object instance;
        private final ReportingPeriod reportingPeriod;

        public CacheKey(Class<?> type, Object instance, ReportingPeriod reportingPeriod)
        {
            this.type = Objects.requireNonNull(type);
            this.instance = Objects.requireNonNull(instance);
            this.reportingPeriod = Objects.requireNonNull(reportingPeriod);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(type, instance, reportingPeriod);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            CacheKey other = (CacheKey) obj;
            if (!type.equals(other.type))
                return false;
            if (!instance.equals(other.instance))
                return false;
            if (!reportingPeriod.equals(other.reportingPeriod))
                return false;
            return true;
        }
    }

    private final Client client;
    private final CurrencyConverter converter;

    private final Map<CacheKey, PerformanceIndex> cache = new HashMap<>();

    @Inject
    public DataSeriesCache(Client client, ExchangeRateProviderFactory factory)
    {
        this.client = client;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
    }

    public void clear()
    {
        this.cache.clear();
    }

    public PerformanceIndex lookup(DataSeries series, ReportingPeriod reportingPeriod)
    {
        Object instance = series.getInstance();
        if (series.getType() == Client.class)
            instance = Client.class;
        else if (series.isPortfolioPlus())
            instance = ((Portfolio) series.getInstance()).getUUID();
        else if (series.isBenchmark() && series.getInstance() instanceof Security)
            instance = ((Security) series.getInstance()).getUUID();

        CacheKey key = new CacheKey(series.getType(), instance, reportingPeriod);

        return cache.computeIfAbsent(key, k -> calculate(series, reportingPeriod));
    }

    private PerformanceIndex calculate(DataSeries series, ReportingPeriod reportingPeriod)
    {
        List<Exception> warnings = new ArrayList<>();

        try
        {
            if (series.getType() == Client.class)
            {
                return PerformanceIndex.forClient(client, converter, reportingPeriod, warnings);
            }
            else if (series.getType() == Security.class)
            {
                Security security = (Security) series.getInstance();

                return series.isBenchmark() ? PerformanceIndex.forSecurity(
                                lookup(new DataSeries(Client.class, null, null, null), reportingPeriod), security)
                                : PerformanceIndex.forInvestment(client, converter, security, reportingPeriod,
                                                warnings);
            }
            else if (series.getType() == Portfolio.class)
            {
                Portfolio portfolio = (Portfolio) series.getInstance();
                return series.isPortfolioPlus()
                                ? PerformanceIndex.forPortfolioPlusAccount(client, converter, portfolio,
                                                reportingPeriod, warnings)
                                : PerformanceIndex.forPortfolio(client, converter, portfolio, reportingPeriod,
                                                warnings);
            }
            else if (series.getType() == Account.class)
            {
                Account account = (Account) series.getInstance();
                return PerformanceIndex.forAccount(client, converter, account, reportingPeriod, warnings);
            }
            else if (series.getType() == Classification.class)
            {
                Classification classification = (Classification) series.getInstance();
                return PerformanceIndex.forClassification(client, converter, classification, reportingPeriod, warnings);
            }
            else if (series.getType() == ConsumerPriceIndex.class)
            {
                return PerformanceIndex.forConsumerPriceIndex(
                                lookup(new DataSeries(Client.class, null, null, null), reportingPeriod));
            }
            else
            {
                return null;
            }
        }
        finally
        {
            if (!warnings.isEmpty())
                PortfolioPlugin.log(warnings);
        }
    }
}
