package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
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
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            CacheKey other = (CacheKey) obj;
            if (!type.equals(other.type))
                return false;
            if (!period.equals(other.period))
                return false;
            return true;
        }

    }

    private final Client client;
    private final CurrencyConverter converter;

    private final Map<CacheKey, Object> cache = new HashMap<>();

    private final List<ReportingPeriod> defaultReportingPeriods = new ArrayList<>();
    private ReportingPeriod defaultReportingPeriod;

    private DataSeriesSet dataSeriesSet;
    private DataSeriesCache dataSeriesCache;

    private Dashboard dashboard;

    @Inject
    public DashboardData(Client client, ExchangeRateProviderFactory factory, PortfolioPart part)
    {
        this.client = client;
        this.converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        this.defaultReportingPeriods.addAll(part.loadReportingPeriods());

        this.dataSeriesSet = new DataSeriesSet(client, DataSeries.UseCase.RETURN_VOLATILITY);
        this.dataSeriesCache = new DataSeriesCache(client, factory);
    }

    public Client getClient()
    {
        return client;
    }

    public Dashboard getDashboard()
    {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard)
    {
        this.dashboard = dashboard;
        this.defaultReportingPeriod = null;
    }

    public List<ReportingPeriod> getDefaultReportingPeriods()
    {
        return defaultReportingPeriods;
    }

    public void setDefaultReportingPeriod(ReportingPeriod reportingPeriod)
    {
        this.defaultReportingPeriod = reportingPeriod;

        defaultReportingPeriods.remove(reportingPeriod);
        defaultReportingPeriods.add(0, reportingPeriod);
    }

    public ReportingPeriod getDefaultReportingPeriod()
    {
        if (defaultReportingPeriod != null)
            return defaultReportingPeriod;

        String code = dashboard.getConfiguration().get(Dashboard.Config.REPORTING_PERIOD.name());

        try
        {
            if (code != null && !code.isEmpty())
                defaultReportingPeriod = ReportingPeriod.from(code);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }

        if (defaultReportingPeriod == null)
            defaultReportingPeriod = new ReportingPeriod.LastX(1, 0);

        return defaultReportingPeriod;
    }

    public DataSeriesSet getDataSeriesSet()
    {
        return dataSeriesSet;
    }

    public void clearCache()
    {
        cache.clear();
        dataSeriesCache.clear();
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

    public PerformanceIndex calculate(DataSeries dataSeries, ReportingPeriod reportingPeriod)
    {
        return dataSeriesCache.lookup(dataSeries, reportingPeriod);
    }
}
