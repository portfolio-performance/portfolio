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
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
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
    private final DataSeriesCache parent;
    private final ClientFilter clientFilter;
    private final Map<CacheKey, PerformanceIndex> cache = Collections.synchronizedMap(new HashMap<>());

    private final Map<ClientFilter, DataSeriesCache> filteredCaches = new HashMap<>();

    private CurrencyConverter converter;
    private Client pretaxClient;

    @Inject
    public DataSeriesCache(Client client, ExchangeRateProviderFactory factory)
    {
        this.client = client;
        this.parent = null;
        this.clientFilter = ClientFilter.NO_FILTER;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
    }

    private DataSeriesCache(DataSeriesCache parent, ClientFilter filter)
    {
        this.parent = parent;
        this.clientFilter = filter;
        this.client = resolveFilter(filter, parent.client).filter(parent.client);
        this.converter = parent.converter;
    }

    /**
     * Shares a cache for equivalent account selections while keeping their
     * calculation results separate from the unfiltered client.
     */
    public synchronized DataSeriesCache filtered(ClientFilter filter)
    {
        if (filter == ClientFilter.NO_FILTER)
            return this;

        return filteredCaches.computeIfAbsent(filter, f -> new DataSeriesCache(this, f));
    }

    public synchronized void clear()
    {
        // the base currency might have changed
        this.converter = this.converter.with(client.getBaseCurrency());

        this.cache.clear();
        this.filteredCaches.clear();
        this.pretaxClient = null;
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

        result = calculate(series, reportingPeriod);
        cache.put(key, result);

        return result;
    }

    private PerformanceIndex calculate(DataSeries series, Interval reportingPeriod)
    {
        List<Exception> warnings = new ArrayList<>();

        try
        {
            switch (series.getType())
            {
                case CLIENT:
                    return PerformanceIndex.forClient(client, converter, reportingPeriod, warnings);

                case CLIENT_PRETAX:
                    return PerformanceIndex.forClient(getPretaxClient(), converter, reportingPeriod, warnings);

                case SECURITY:
                    return PerformanceIndex.forInvestment(client, converter, (Security) series.getInstance(),
                                    reportingPeriod, warnings);

                case SECURITY_BENCHMARK:
                    return PerformanceIndex.forSecurity(
                                    lookup(new DataSeries(DataSeries.Type.CLIENT, null, null, null), reportingPeriod),
                                    (Security) series.getInstance());

                case PORTFOLIO:
                    return calculateFiltered(new PortfolioClientFilter((Portfolio) series.getInstance()), false,
                                    reportingPeriod, warnings);

                case PORTFOLIO_PRETAX:
                    return calculatePortfolioPretax(series, reportingPeriod, warnings);

                case PORTFOLIO_PLUS_ACCOUNT:
                    Portfolio portfolio = (Portfolio) series.getInstance();
                    return calculateFiltered(new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount()),
                                    false, reportingPeriod, warnings);

                case PORTFOLIO_PLUS_ACCOUNT_PRETAX:
                    return calculatePortfolioPlusAccountPretax(series, reportingPeriod, warnings);

                case ACCOUNT:
                    return calculateFiltered(
                                    new PortfolioClientFilter(Collections.emptyList(),
                                                    List.of((Account) series.getInstance())),
                                    false, reportingPeriod, warnings);

                case ACCOUNT_PRETAX:
                    return calculateAccountPretax(series, reportingPeriod, warnings);

                case CLASSIFICATION:
                    Classification classification = (Classification) series.getInstance();
                    return PerformanceIndex.forClassification(client, converter, classification, reportingPeriod,
                                    warnings);

                case CLIENT_FILTER:
                    ClientFilterMenu.Item item = (ClientFilterMenu.Item) series.getInstance();
                    return calculateFiltered(item.getFilter(), false, reportingPeriod, warnings);

                case CLIENT_FILTER_PRETAX:
                    ClientFilterMenu.Item pretax = (ClientFilterMenu.Item) series.getInstance();
                    return calculateFiltered(pretax.getFilter(), true, reportingPeriod, warnings);

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

    // Account data series retain references to the original client. Resolve
    // those references against the filtered client so that a second filter
    // cannot restore transactions from excluded accounts.
    private static ClientFilter resolveFilter(ClientFilter filter, Client client)
    {
        if (!(client instanceof ReadOnlyClient) || !(filter instanceof PortfolioClientFilter portfolioFilter))
            return filter;

        var portfolios = new ArrayList<Portfolio>();
        var accounts = new ArrayList<Account>();
        var weights = new HashMap<Object, Integer>();
        for (var element : portfolioFilter.getAllElements())
        {
            Object resolved;
            if (element instanceof Portfolio portfolio)
            {
                resolved = client.getPortfolios().stream()
                                .filter(p -> ReadOnlyPortfolio.unwrap(p).equals(ReadOnlyPortfolio.unwrap(portfolio)))
                                .findAny().orElse(null);
                if (resolved != null)
                    portfolios.add((Portfolio) resolved);
            }
            else
            {
                resolved = client.getAccounts().stream().filter(
                                a -> ReadOnlyAccount.unwrap(a).equals(ReadOnlyAccount.unwrap((Account) element)))
                                .findAny().orElse(null);
                if (resolved != null)
                    accounts.add((Account) resolved);
            }
            if (resolved != null)
                weights.put(resolved, portfolioFilter.getWeight(element));
        }
        return new PortfolioClientFilter(portfolios, accounts, weights);
    }

    private synchronized Client getPretaxClient()
    {
        if (pretaxClient == null)
        {
            // Remove taxes before selecting accounts. A securities-only filter
            // turns refunds into a refund plus a withdrawal. Removing taxes
            // afterwards would leave an artificial deposit/withdrawal pair
            // that changes TTWROR despite its net cash flow being zero.
            if (parent == null)
            {
                pretaxClient = new WithoutTaxesFilter().filter(client);
            }
            else
            {
                Client source = parent.getPretaxClient();
                pretaxClient = resolveFilter(clientFilter, source).filter(source);
            }
        }
        return pretaxClient;
    }

    private PerformanceIndex calculateFiltered(ClientFilter filter, boolean pretax, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        Client source = pretax ? getPretaxClient() : client;
        Client filteredClient = resolveFilter(filter, source).filter(source);
        return PerformanceIndex.forClient(filteredClient, converter, reportingPeriod, warnings);
    }

    private PerformanceIndex calculatePortfolioPretax(DataSeries series, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        return calculateFiltered(new PortfolioClientFilter((Portfolio) series.getInstance()), true, reportingPeriod,
                        warnings);
    }

    private PerformanceIndex calculatePortfolioPlusAccountPretax(DataSeries series, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        Portfolio portfolio = (Portfolio) series.getInstance();
        return calculateFiltered(new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount()), true,
                        reportingPeriod, warnings);
    }

    private PerformanceIndex calculateAccountPretax(DataSeries series, Interval reportingPeriod,
                    List<Exception> warnings)
    {
        return calculateFiltered(
                        new PortfolioClientFilter(Collections.emptyList(), List.of((Account) series.getInstance())),
                        true, reportingPeriod, warnings);
    }
}
