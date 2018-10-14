package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSet;

public class DashboardData
{
    private static final class CacheKey
    {
        private Class<?> type;
        private ReportingPeriod period;

        public CacheKey(Class<?> type, ReportingPeriod period)
        {
            this.type = Objects.requireNonNull(type);
            this.period = Objects.requireNonNull(period);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(type, period);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (!(obj instanceof CacheKey))
                return false;

            CacheKey other = (CacheKey) obj;
            return type.equals(other.type) && period.equals(other.period);
        }
    }

    public static final Object EMPTY_RESULT = new Object();

    private final Client client;
    private final IPreferenceStore preferences;
    private final ExchangeRateProviderFactory factory;
    private final CurrencyConverter converter;

    private final Map<CacheKey, Object> cache = Collections.synchronizedMap(new HashMap<>());

    private List<ReportingPeriod> defaultReportingPeriods = new ArrayList<>();
    private ReportingPeriod defaultReportingPeriod;

    private DataSeriesSet dataSeriesSet;
    private DataSeriesCache dataSeriesCache;

    private Map<Widget, Object> resultCache = Collections.synchronizedMap(new HashMap<>());

    private Dashboard dashboard;

    @Inject
    private MDirtyable dirtyable;

    @Inject
    public DashboardData(Client client, IPreferenceStore preferences, ExchangeRateProviderFactory factory)
    {
        this.client = client;
        this.preferences = preferences;
        this.factory = factory;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());

        this.dataSeriesSet = new DataSeriesSet(client, preferences, DataSeries.UseCase.RETURN_VOLATILITY);
        this.dataSeriesCache = new DataSeriesCache(client, factory);
    }

    public Client getClient()
    {
        return client;
    }

    public IPreferenceStore getPreferences()
    {
        return preferences;
    }

    public Dashboard getDashboard()
    {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard)
    {
        this.dashboard = dashboard;
    }

    public void setDefaultReportingPeriods(List<ReportingPeriod> defaultReportingPeriods)
    {
        this.defaultReportingPeriods = defaultReportingPeriods;
    }

    public List<ReportingPeriod> getDefaultReportingPeriods()
    {
        return defaultReportingPeriods;
    }

    public void setDefaultReportingPeriod(ReportingPeriod reportingPeriod)
    {
        this.defaultReportingPeriod = reportingPeriod;
    }

    public ReportingPeriod getDefaultReportingPeriod()
    {
        return defaultReportingPeriod;
    }

    public ExchangeRateProviderFactory getExchangeRateProviderFactory()
    {
        return factory;
    }

    public DataSeriesSet getDataSeriesSet()
    {
        return dataSeriesSet;
    }

    public synchronized void clearCache()
    {
        cache.clear();
        dataSeriesCache.clear();

        clearResultCache();
    }

    public <T> T calculate(Class<T> type, ReportingPeriod period)
    {
        CacheKey key = new CacheKey(type, period);
        return type.cast(cache.computeIfAbsent(key, k -> doCalculate(type, period)));
    }

    private Object doCalculate(Class<?> type, ReportingPeriod period)
    {
        if (type.equals(ClientPerformanceSnapshot.class))
        {
            return new ClientPerformanceSnapshot(client, converter, period);
        }
        else if (type.equals(PerformanceIndex.class))
        {
            return PerformanceIndex.forClient(client, converter, period, new ArrayList<Exception>());
        }
        else
        {
            return null;
        }
    }

    public DataSeriesCache getDataSeriesCache()
    {
        return dataSeriesCache;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public PerformanceIndex calculate(DataSeries dataSeries, ReportingPeriod reportingPeriod)
    {
        return dataSeriesCache.lookup(dataSeries, reportingPeriod);
    }

    public synchronized Map<Widget, Object> getResultCache()
    {
        return resultCache;
    }

    public synchronized void clearResultCache()
    {
        // create a new cache map in order to make sure that old (possibly
        // still running) tasks do not write into the new cache
        resultCache = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Marks the file as dirty <b>without</b> triggering an update.
     */
    public void markDirty()
    {
        dirtyable.setDirty(true);
    }
}
