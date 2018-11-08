package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyAccount;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyPortfolio;
import name.abuchen.portfolio.snapshot.filter.WithoutTaxesFilter;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;

/**
 * Cache for calculation results of DataSeries.
 */
public class DataSeriesCache
{
    private static class CacheKey
    {
        private final String uuid;
        private final ReportingPeriod reportingPeriod;

        CacheKey(String uuid, ReportingPeriod reportingPeriod)
        {
            this.uuid = Objects.requireNonNull(uuid);
            this.reportingPeriod = Objects.requireNonNull(reportingPeriod);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(uuid, reportingPeriod);
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
            if (!uuid.equals(other.uuid))
                return false;
            if (!reportingPeriod.equals(other.reportingPeriod))
                return false;
            return true;
        }
    }

    private final Client client;
    private final CurrencyConverter converter;

    private final Map<CacheKey, PerformanceIndex> cache = Collections.synchronizedMap(new HashMap<>());

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
        // Every data series is cached separately except the for the client. The
        // client data series are created out of the same PerformanceIndex
        // instance, e.g. accumulated and delta performance.
        String uuid = series.getType() == DataSeries.Type.CLIENT ? "$client$" : series.getUUID(); //$NON-NLS-1$

        CacheKey key = new CacheKey(uuid, reportingPeriod);

        // #computeIfAbsent leads to a ConcurrentMapModificdation b/c #calculate
        // might call #lookup to calculate other cache entries
        PerformanceIndex result = cache.get(key);
        if (result != null)
            return result;

        result = calculate(series, reportingPeriod);
        cache.put(key, result);

        return result;
    }

    private PerformanceIndex calculate(DataSeries series, ReportingPeriod reportingPeriod)
    {
        List<Exception> warnings = new ArrayList<>();

        try
        {
            switch (series.getType())
            {
                case CLIENT:
                    return PerformanceIndex.forClient(client, converter, reportingPeriod, warnings);

                case CLIENT_PRETAX:
                    return PerformanceIndex.forClient(new WithoutTaxesFilter().filter(client), converter,
                                    reportingPeriod, warnings);

                case SECURITY:
                    return PerformanceIndex.forInvestment(client, converter, (Security) series.getInstance(),
                                    reportingPeriod, warnings);

                case SECURITY_BENCHMARK:
                    return PerformanceIndex.forSecurity(
                                    lookup(new DataSeries(DataSeries.Type.CLIENT, null, null, null), reportingPeriod),
                                    (Security) series.getInstance());

                case PORTFOLIO:
                    return PerformanceIndex.forPortfolio(client, converter, (Portfolio) series.getInstance(),
                                    reportingPeriod, warnings);

                case PORTFOLIO_PRETAX:
                    return calculatePortfolioPretax(series, reportingPeriod, warnings);

                case PORTFOLIO_PLUS_ACCOUNT:
                    return PerformanceIndex.forPortfolioPlusAccount(client, converter, (Portfolio) series.getInstance(),
                                    reportingPeriod, warnings);

                case PORTFOLIO_PLUS_ACCOUNT_PRETAX:
                    return calculatePortfolioPlusAccountPretax(series, reportingPeriod, warnings);

                case ACCOUNT:
                    Account account = (Account) series.getInstance();
                    return PerformanceIndex.forAccount(client, converter, account, reportingPeriod, warnings);

                case ACCOUNT_PRETAX:
                    return calculateAccountPretax(series, reportingPeriod, warnings);

                case CLASSIFICATION:
                    Classification classification = (Classification) series.getInstance();
                    return PerformanceIndex.forClassification(client, converter, classification, reportingPeriod,
                                    warnings);

                case CONSUMER_PRICE_INDEX:
                    return PerformanceIndex.forConsumerPriceIndex(
                                    lookup(new DataSeries(DataSeries.Type.CLIENT, null, null, null), reportingPeriod));

                case CLIENT_FILTER:
                    ClientFilterMenu.Item item = (ClientFilterMenu.Item) series.getInstance();
                    return PerformanceIndex.forClient(item.getFilter().filter(client), converter, reportingPeriod,
                                    warnings);

                case CLIENT_FILTER_PRETAX:
                    ClientFilterMenu.Item pretax = (ClientFilterMenu.Item) series.getInstance();
                    return PerformanceIndex.forClient(
                                    new WithoutTaxesFilter().filter(pretax.getFilter().filter(client)), converter,
                                    reportingPeriod, warnings);

                default:
                    throw new IllegalArgumentException(series.getType().name());
            }
        }
        finally
        {
            if (!warnings.isEmpty())
                PortfolioPlugin.log(warnings);
        }
    }

    private PerformanceIndex calculatePortfolioPretax(DataSeries series, ReportingPeriod reportingPeriod,
                    List<Exception> warnings)
    {
        Client filteredClient = new WithoutTaxesFilter().filter(client);
        Portfolio portfolio = filteredClient.getPortfolios().stream()
                        .filter(p -> ((ReadOnlyPortfolio) p).getSource().equals(series.getInstance())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        return PerformanceIndex.forPortfolio(filteredClient, converter, portfolio, reportingPeriod, warnings);
    }

    private PerformanceIndex calculatePortfolioPlusAccountPretax(DataSeries series, ReportingPeriod reportingPeriod,
                    List<Exception> warnings)
    {
        Client filteredClient = new WithoutTaxesFilter().filter(client);
        Portfolio portfolio = filteredClient.getPortfolios().stream()
                        .filter(p -> ((ReadOnlyPortfolio) p).getSource().equals(series.getInstance())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        return PerformanceIndex.forPortfolioPlusAccount(client, converter, portfolio, reportingPeriod, warnings);
    }

    private PerformanceIndex calculateAccountPretax(DataSeries series, ReportingPeriod reportingPeriod,
                    List<Exception> warnings)
    {
        Client filteredClient = new WithoutTaxesFilter().filter(client);
        Account account = filteredClient.getAccounts().stream()
                        .filter(a -> ((ReadOnlyAccount) a).getSource().equals(series.getInstance())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        return PerformanceIndex.forAccount(client, converter, account, reportingPeriod, warnings);
    }
}
