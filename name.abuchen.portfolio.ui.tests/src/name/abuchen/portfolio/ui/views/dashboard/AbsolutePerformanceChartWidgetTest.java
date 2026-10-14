package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;

@SuppressWarnings("nls")
public class AbsolutePerformanceChartWidgetTest
{
    private DashboardData dashboardData(Client client)
    {
        var factory = new ExchangeRateProviderFactory(client);
        return new DashboardData(client, new PreferenceStore(), mock(IStylingEngine.class), factory,
                        new DataSeriesCache(client, factory));
    }

    @Test
    public void testUsesConfiguredReportingPeriod()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2024-01-01", 100_00).interest("2024-01-15", 10_00)
                        .interest("2024-02-01", 20_00).interest("2024-03-01", 30_00).addTo(client);
        var widget = WidgetFactory.ABSOLUTE_PERFORMANCE_CHART.constructWidget();
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                        new ReportingPeriod.FromXtoY(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 15)).getCode());
        var delegate = (AbsolutePerformanceChartWidget) WidgetFactory.ABSOLUTE_PERFORMANCE_CHART
                        .constructDelegate(widget, dashboardData(client));

        var snapshot = delegate.getUpdateTask().get();
        assertThat(snapshot.getStartClientSnapshot().getTime(), is(LocalDate.of(2024, 1, 31)));
        assertThat(snapshot.getEndClientSnapshot().getTime(), is(LocalDate.of(2024, 2, 15)));
        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 110_00)));
        assertThat(snapshot.getAbsoluteDelta(), is(Money.of(CurrencyUnit.EUR, 20_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 130_00)));
        assertThat(delegate.get(ChartHeightConfig.class).getPixel(), is(210));
    }

    @Test
    public void testSelectedPortfolioIncludesClosedTradesAndExcludesOtherHoldings()
    {
        var client = new Client();
        var security = new SecurityBuilder().addPrice("2024-03-01", Values.Quote.factorize(100)).addTo(client);
        var account = new AccountBuilder().deposit_("2024-01-01", 1000_00).addTo(client);
        var selectedPortfolio = new PortfolioBuilder(account)
                        .buy(security, "2024-01-02", Values.Share.factorize(1), 100_00)
                        .sell(security, "2024-02-01", Values.Share.factorize(1), 150_00).addTo(client);
        new PortfolioBuilder(account).buy(security, "2024-01-03", Values.Share.factorize(3), 300_00).addTo(client);
        var widget = WidgetFactory.ABSOLUTE_PERFORMANCE_CHART.constructWidget();
        var data = dashboardData(client);
        var selectedSeries = data.getDataSeriesSet().getAvailableSeries().stream()
                        .filter(series -> series.getType() == DataSeries.Type.PORTFOLIO
                                        && series.getInstance() == selectedPortfolio)
                        .findFirst().orElseThrow();
        widget.getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), selectedSeries.getUUID());
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                        new ReportingPeriod.FromXtoY(LocalDate.of(2023, 12, 31), LocalDate.of(2024, 3, 1)).getCode());
        var delegate = new AbsolutePerformanceChartWidget(widget, data);

        var snapshot = delegate.getUpdateTask().get();
        assertThat(snapshot.getAbsoluteDelta(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.TRANSFERS), is(Money.of(CurrencyUnit.EUR, -50_00)));
        assertThat(delegate.get(DataSeriesConfig.class).getDataSeries(), is(selectedSeries));
    }

    @Test
    public void testAccountDataSeriesExcludesOtherAccounts()
    {
        var client = new Client();
        var selectedAccount = new AccountBuilder().deposit_("2024-01-01", 100_00)
                        .interest("2024-02-01", 20_00).addTo(client);
        new AccountBuilder().deposit_("2024-01-01", 500_00).interest("2024-02-01", 200_00).addTo(client);
        var delegate = delegateForSeries(client, DataSeries.Type.ACCOUNT, selectedAccount);

        var snapshot = delegate.getUpdateTask().get();
        assertThat(snapshot.getAbsoluteDelta(), is(Money.of(CurrencyUnit.EUR, 20_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 120_00)));
        assertThat(delegate.getUpdateTask().get(), is(snapshot));
    }

    @Test
    public void testSecurityDataSeriesIncludesSalesAndDividends()
    {
        var client = new Client();
        var security = new SecurityBuilder().addPrice("2024-03-01", Values.Quote.factorize(100)).addTo(client);
        var otherSecurity = new SecurityBuilder().addTo(client);
        var account = new AccountBuilder().deposit_("2024-01-01", 1000_00)
                        .dividend("2024-02-15", 10_00, security).addTo(client);
        new PortfolioBuilder(account).buy(security, "2024-01-02", Values.Share.factorize(2), 200_00)
                        .sell(security, "2024-02-01", Values.Share.factorize(1), 150_00)
                        .buy(otherSecurity, "2024-01-03", Values.Share.factorize(1), 300_00).addTo(client);
        var delegate = delegateForSeries(client, DataSeries.Type.SECURITY, security);

        var snapshot = delegate.getUpdateTask().get();
        assertThat(snapshot.getAbsoluteDelta(), is(Money.of(CurrencyUnit.EUR, 60_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(snapshot.getValue(CategoryType.TRANSFERS), is(Money.of(CurrencyUnit.EUR, 40_00)));
    }

    private AbsolutePerformanceChartWidget delegateForSeries(Client client, DataSeries.Type type, Object instance)
    {
        var data = dashboardData(client);
        var series = data.getDataSeriesSet().getAvailableSeries().stream()
                        .filter(candidate -> candidate.getType() == type && candidate.getInstance() == instance)
                        .findFirst().orElseThrow();
        var widget = WidgetFactory.ABSOLUTE_PERFORMANCE_CHART.constructWidget();
        widget.getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), series.getUUID());
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                        new ReportingPeriod.FromXtoY(LocalDate.of(2023, 12, 31), LocalDate.of(2024, 3, 1)).getCode());
        return new AbsolutePerformanceChartWidget(widget, data);
    }
}
