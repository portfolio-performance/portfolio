package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyAccount;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyPortfolio;
import name.abuchen.portfolio.snapshot.filter.WithoutTaxesFilter;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CacheKey;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.util.Interval;

/**
 * Cache for calculation results of DataSeries.
 */
@Creatable
public class DataSeriesCache
{
    private final Client client;
    private final Map<CacheKey, PerformanceIndex> cache = Collections.synchronizedMap(new HashMap<>());
    private final Object cacheLock = new Object();

    private CurrencyConverter converter;

    private ClientFilter clientFilter = ClientFilter.NO_FILTER;
    private volatile Client filteredClient;
    private long generation;

    @Inject
    public DataSeriesCache(Client client, ExchangeRateProviderFactory factory)
    {
        this.client = client;
        this.filteredClient = client;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
    }

    public void clear()
    {
        // the base currency might have changed
        this.converter = this.converter.with(client.getBaseCurrency());

        // re-apply the filter so that changes to the client are reflected
        this.filteredClient = clientFilter.filter(client);

        synchronized (cacheLock)
        {
            generation++;
            cache.clear();
        }
    }

    /**
     * Sets the filter used to calculate the data series. Passing
     * {@link ClientFilter#NO_FILTER} resets to the unfiltered client.
     */
    public void setClientFilter(ClientFilter filter)
    {
        this.clientFilter = filter == null ? ClientFilter.NO_FILTER : filter;
        clear();
    }

    /**
     * Returns the client used for the calculations, i.e. the client with the
     * currently selected filter applied.
     */
    public Client getFilteredClient()
    {
        return filteredClient;
    }

    public PerformanceIndex lookup(DataSeries series, Interval reportingPeriod)
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

        long generationAtStart;
        synchronized (cacheLock)
        {
            generationAtStart = generation;
        }

        result = calculate(series, reportingPeriod);

        synchronized (cacheLock)
        {
            // A calculation started with an old filter must not repopulate the
            // cache after the cache has been cleared.
            if (generationAtStart == generation)
                cache.put(key, result);
        }

        return result;
    }

    private PerformanceIndex calculate(DataSeries series, Interval reportingPeriod)
    {
        List<Exception> warnings = new ArrayList<>();
        Portfolio portfolio;
        Account account;

        try
        {
            switch (series.getType())
            {
                case CLIENT:
                    return PerformanceIndex.forClient(filteredClient, converter, reportingPeriod, warnings);

                case CLIENT_PRETAX:
                    return PerformanceIndex.forClient(new WithoutTaxesFilter().filter(filteredClient), converter,
                                    reportingPeriod, warnings);

                case SECURITY:
                    return PerformanceIndex.forInvestment(filteredClient, converter,
                                    (Security) series.getInstance(), reportingPeriod, warnings);

                case SECURITY_BENCHMARK:
                    return PerformanceIndex.forSecurity(
                                    lookup(new DataSeries(DataSeries.Type.CLIENT, null, null, null), reportingPeriod),
                                    (Security) series.getInstance());

                case PORTFOLIO:
                    portfolio = findPortfolio(filteredClient, (Portfolio) series.getInstance());
                    if (portfolio == null)
                        return emptyIndex(reportingPeriod, warnings);
                    return PerformanceIndex.forPortfolio(filteredClient, converter, portfolio, reportingPeriod,
                                    warnings);

                case PORTFOLIO_PRETAX:
                    return calculatePortfolioPretax(series, reportingPeriod, warnings);

                case PORTFOLIO_PLUS_ACCOUNT:
                    portfolio = findPortfolio(filteredClient, (Portfolio) series.getInstance());
                    if (portfolio == null)
                        return emptyIndex(reportingPeriod, warnings);
                    return PerformanceIndex.forPortfolioPlusAccount(filteredClient, converter, portfolio,
                                    reportingPeriod, warnings);

                case PORTFOLIO_PLUS_ACCOUNT_PRETAX:
                    return calculatePortfolioPlusAccountPretax(series, reportingPeriod, warnings);

                case ACCOUNT:
                    account = findAccount(filteredClient, (Account) series.getInstance());
                    if (account == null)
                        return emptyIndex(reportingPeriod, warnings);
                    return PerformanceIndex.forAccount(filteredClient, converter, account, reportingPeriod, warnings);

                case ACCOUNT_PRETAX:
                    return calculateAccountPretax(series, reportingPeriod, warnings);

                case CLASSIFICATION:
                    Classification classification = (Classification) series.getInstance();
                    return PerformanceIndex.forClassification(filteredClient, converter, classification,
                                    reportingPeriod, warnings);

                case CLIENT_FILTER:
                    ClientFilterMenu.Item item = (ClientFilterMenu.Item) series.getInstance();
                    return PerformanceIndex.forClient(
                                    ClientFilterMenu.applyFilter(item.getFilter(), filteredClient), converter,
                                    reportingPeriod, warnings);

                case CLIENT_FILTER_PRETAX:
                    ClientFilterMenu.Item pretax = (ClientFilterMenu.Item) series.getInstance();
                    return PerformanceIndex.forClient(new WithoutTaxesFilter()
                                    .filter(ClientFilterMenu.applyFilter(pretax.getFilter(), filteredClient)),
                                    converter, reportingPeriod, warnings);

                case DERIVED_DATA_SERIES:
                    // redirect to the #lookup method to use the cached data, if
                    // available
                    var derivedDataSeries = (DerivedDataSeries) series.getInstance();
                    return lookup(derivedDataSeries.getBaseDataSeries(), reportingPeriod);

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

    private PerformanceIndex calculatePortfolioPretax(DataSeries series, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        Client pretaxClient = new WithoutTaxesFilter().filter(filteredClient);
        Portfolio portfolio = findPortfolio(pretaxClient, (Portfolio) series.getInstance());
        if (portfolio == null)
            return emptyIndex(reportingPeriod, warnings);

        return PerformanceIndex.forPortfolio(pretaxClient, converter, portfolio, reportingPeriod, warnings);
    }

    private PerformanceIndex calculatePortfolioPlusAccountPretax(DataSeries series, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        Client pretaxClient = new WithoutTaxesFilter().filter(filteredClient);
        Portfolio portfolio = findPortfolio(pretaxClient, (Portfolio) series.getInstance());
        if (portfolio == null)
            return emptyIndex(reportingPeriod, warnings);

        return PerformanceIndex.forPortfolioPlusAccount(pretaxClient, converter, portfolio, reportingPeriod, warnings);
    }

    private PerformanceIndex calculateAccountPretax(DataSeries series, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        Client pretaxClient = new WithoutTaxesFilter().filter(filteredClient);
        Account account = findAccount(pretaxClient, (Account) series.getInstance());
        if (account == null)
            return emptyIndex(reportingPeriod, warnings);

        return PerformanceIndex.forAccount(pretaxClient, converter, account, reportingPeriod, warnings);
    }

    private PerformanceIndex emptyIndex(Interval reportingPeriod, List<Exception> warnings)
    {
        Client emptyClient = new PortfolioClientFilter(Collections.emptyList(), Collections.emptyList())
                        .filter(filteredClient);
        return PerformanceIndex.forClient(emptyClient, converter, reportingPeriod, warnings);
    }

    /**
     * Returns the portfolio of the (possibly filtered) client matching the
     * given original portfolio, or {@code null} if it is not part of the
     * filtered client.
     */
    private static Portfolio findPortfolio(Client client, Portfolio portfolio)
    {
        return client.getPortfolios().stream().filter(p -> ReadOnlyPortfolio.unwrap(p).equals(portfolio)).findAny()
                        .orElse(null);
    }

    /**
     * Returns the account of the (possibly filtered) client matching the given
     * original account, or {@code null} if it is not part of the filtered
     * client.
     */
    private static Account findAccount(Client client, Account account)
    {
        return client.getAccounts().stream().filter(a -> ReadOnlyAccount.unwrap(a).equals(account)).findAny()
                        .orElse(null);
    }
}
