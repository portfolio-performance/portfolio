package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSet;
import name.abuchen.portfolio.util.Interval;

public class DashboardData
{
    public static final Object EMPTY_RESULT = new Object();

    private final Client client;
    private final IPreferenceStore preferences;
    private final IStylingEngine stylingEngine;
    private final ExchangeRateProviderFactory factory;
    private final CurrencyConverter converter;

    private final Map<Object, Object> cache = Collections.synchronizedMap(new HashMap<>());

    private List<ReportingPeriod> defaultReportingPeriods = new ArrayList<>();
    private ReportingPeriod defaultReportingPeriod;

    private DataSeriesSet dataSeriesSet;
    private DataSeriesCache dataSeriesCache;

    private Map<Widget, Object> resultCache = Collections.synchronizedMap(new HashMap<>());

    private Dashboard dashboard;

    @Inject
    public DashboardData(Client client, IPreferenceStore preferences, IStylingEngine stylingEngine,
                    ExchangeRateProviderFactory factory, DataSeriesCache dataSeriesCache)
    {
        this.client = client;
        this.preferences = preferences;
        this.stylingEngine = stylingEngine;
        this.factory = factory;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());

        this.dataSeriesSet = new DataSeriesSet(client, preferences, DataSeries.UseCase.RETURN_VOLATILITY);
        this.dataSeriesCache = dataSeriesCache;
    }

    public Client getClient()
    {
        return client;
    }

    public IPreferenceStore getPreferences()
    {
        return preferences;
    }

    public IStylingEngine getStylingEngine()
    {
        return stylingEngine;
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

    /**
     * Returns a specialized cache that computes {@link PerformanceIndex}
     * results if not present.
     */
    public DataSeriesCache getDataSeriesCache()
    {
        return dataSeriesCache;
    }

    /**
     * Returns a generic cache that can be used by widgets to share data for the
     * current dashboard.
     */
    public Map<Object, Object> getCache()
    {
        return cache;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public PerformanceIndex calculate(DataSeries dataSeries, Interval reportingPeriod)
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
}
