package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.ConfigurationSet.WellKnownConfigurationSets;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSet;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class DashboardAccountFilterTest
{
    private final Interval interval = Interval.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));
    private Client client;
    private DashboardData data;
    private Portfolio first;
    private Portfolio second;
    private Security stock;
    private DataSeries stocks;
    private DataSeries crypto;

    @Before
    public void setup()
    {
        client = new Client();
        stock = new SecurityBuilder().addPrice("2024-01-01", Values.Quote.factorize(100))
                        .addPrice("2024-01-03", Values.Quote.factorize(110)).addTo(client);
        var coin = new SecurityBuilder().addPrice("2024-01-01", Values.Quote.factorize(200))
                        .addPrice("2024-01-03", Values.Quote.factorize(220)).addTo(client);
        first = new PortfolioBuilder().buy(stock, "2024-01-01", Values.Share.factorize(2), 200_00)
                        .buy(coin, "2024-01-01", Values.Share.factorize(3), 600_00).addTo(client);
        second = new PortfolioBuilder().buy(stock, "2024-01-01", Values.Share.factorize(7), 700_00)
                        .buy(coin, "2024-01-01", Values.Share.factorize(5), 1000_00).addTo(client);

        var taxonomy = new Taxonomy("Asset Classes");
        var root = new Classification("root", "Asset Classes");
        taxonomy.setRootNode(root);
        var stockClass = new Classification(root, "stocks", "Stocks");
        root.addChild(stockClass);
        stockClass.addAssignment(new Assignment(stock));
        var cryptoClass = new Classification(root, "crypto", "Crypto");
        root.addChild(cryptoClass);
        cryptoClass.addAssignment(new Assignment(coin));
        client.addTaxonomy(taxonomy);

        var factory = new ExchangeRateProviderFactory(client);
        data = new DashboardData(client, new PreferenceStore(), null, factory, new DataSeriesCache(client, factory));
        data.setDefaultReportingPeriod(new ReportingPeriod.FromXtoY(interval));
        stocks = series(stockClass);
        crypto = series(cryptoClass);
    }

    private DataSeries series(Object instance)
    {
        return data.getDataSeriesSet().getAvailableSeries().stream().filter(
                        s -> s.getInstance() == instance && !s.isBenchmark() && !s.getType().name().endsWith("PRETAX"))
                        .findFirst().orElseThrow();
    }

    private Widget widget(Portfolio portfolio)
    {
        var widget = WidgetFactory.TOTAL_SUM.constructWidget();
        widget.getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(), portfolio.getUUID());
        return widget;
    }

    private long total(PerformanceIndex index)
    {
        return index.getTotals()[index.getTotals().length - 1];
    }

    @Test
    public void testAccountAndAssetClassAreIntersected()
    {
        var widget = widget(first);
        var stockIndex = data.calculate(stocks, interval, widget);
        assertThat(total(stockIndex), is(220_00L));
        assertThat(total(data.calculate(crypto, interval, widget)), is(660_00L));
        assertThat(stockIndex.getFinalAccumulatedPercentage(), closeTo(0.1, 0.000001));
        assertThat(Double.isFinite(stockIndex.getPerformanceIRR()), is(true));
    }

    @Test
    public void testSameSecurityIsRestrictedToSelectedAccount()
    {
        var securitySeries = series(stock);
        assertThat(total(data.calculate(securitySeries, interval, widget(first))), is(220_00L));
        assertThat(total(data.calculate(securitySeries, interval, widget(second))), is(770_00L));
    }

    @Test
    public void testCachesSeparateAccountsAndUnfilteredResults()
    {
        var firstWidget = widget(first);
        var secondWidget = widget(second);
        var firstIndex = data.calculate(stocks, interval, firstWidget);
        assertThat(total(data.calculate(stocks, interval, secondWidget)), is(770_00L));
        assertThat(total(data.calculate(stocks, interval)), is(990_00L));
        assertSame(firstIndex, data.calculate(stocks, interval, firstWidget));
        data.clearCache();
        assertThat(total(data.calculate(stocks, interval, firstWidget)), is(220_00L));
    }

    @Test
    public void testPortfolioSeriesUsesFilteredTransactions()
    {
        assertThat(total(data.calculate(series(first), interval, widget(first))), is(880_00L));
        assertThat(total(data.calculate(series(second), interval, widget(first))), is(0L));
    }

    @Test
    public void testSavedSelectionAndMissingFilterFallback()
    {
        var saved = widget(first).copy();
        assertThat(total(data.calculate(stocks, interval, saved)), is(220_00L));
        saved.getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(), "deleted-filter");
        assertThat(total(data.calculate(stocks, interval, saved)), is(990_00L));
        saved.getConfiguration().remove(Dashboard.Config.CLIENT_FILTER.name());
        assertSame(data.calculate(stocks, interval), data.calculate(stocks, interval, saved));
    }

    @Test
    public void testTransfersAndCacheInvalidation()
    {
        var widget = widget(first);
        assertThat(total(data.calculate(stocks, interval, widget)), is(220_00L));
        var transfer = new PortfolioTransferEntry(first, second);
        transfer.setDate(LocalDateTime.parse("2024-01-02T00:00"));
        transfer.setSecurity(stock);
        transfer.setCurrencyCode("EUR");
        transfer.setShares(Values.Share.factorize(1));
        transfer.setAmount(100_00L);
        transfer.insert();
        data.clearCache();
        assertThat(total(data.calculate(stocks, interval, widget)), is(110_00L));
        assertThat(total(data.calculate(stocks, interval, widget(second))), is(880_00L));
        assertThat(total(data.calculate(stocks, interval)), is(990_00L));
    }

    @Test
    public void testWeightedCustomAccountFilter()
    {
        client.getSettings().getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_DEFINITIONS)
                        .add(new Configuration("half-first", "Half of first account",
                                        first.getUUID() + ":" + Classification.ONE_HUNDRED_PERCENT / 2));
        var widget = widget(first);
        widget.getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(), "half-first");
        assertThat(total(data.calculate(stocks, interval, widget)), is(110_00L));
        assertThat(total(data.calculate(crypto, interval, widget)), is(330_00L));
    }

    @Test
    public void testCustomDataSeriesDoesNotRestoreExcludedAccountTransactions()
    {
        client.getSettings().getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_DEFINITIONS)
                        .add(new Configuration("both", "Both accounts", first.getUUID() + "," + second.getUUID()));
        var customSeries = new DataSeriesSet(client, new PreferenceStore(), DataSeries.UseCase.RETURN_VOLATILITY)
                        .getAvailableSeries().stream().filter(s -> s.getType() == DataSeries.Type.CLIENT_FILTER)
                        .findFirst().orElseThrow();
        assertThat(total(data.calculate(customSeries, interval, widget(first))), is(880_00L));
    }

    @Test
    public void testPartialTaxonomyAssignmentIsPreserved()
    {
        ((Classification) stocks.getInstance()).getAssignments().get(0)
                        .setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        assertThat(total(data.calculate(stocks, interval, widget(first))), is(110_00L));
        assertThat(total(data.calculate(stocks, interval, widget(second))), is(385_00L));
    }

    @Test
    public void testStockOnlyAccountWithDividendsAndFees()
    {
        var portfolio = new PortfolioBuilder().buy(stock, "2024-01-01", Values.Share.factorize(2), 200_00)
                        .addTo(client);
        var account = portfolio.getReferenceAccount();
        var date = LocalDateTime.parse("2024-01-02T00:00");
        account.addTransaction(new AccountTransaction(date, "EUR", 5_00, stock, AccountTransaction.Type.DIVIDENDS));
        account.addTransaction(new AccountTransaction(date, "EUR", 1_00, stock, AccountTransaction.Type.FEES));
        account.addTransaction(new AccountTransaction(date, "EUR", 20, stock, AccountTransaction.Type.FEES_REFUND));
        var factory = new ExchangeRateProviderFactory(client);
        data = new DashboardData(client, new PreferenceStore(), null, factory, new DataSeriesCache(client, factory));
        var category = data.calculate(stocks, interval, widget(portfolio));
        var accountIndex = data.calculate(series(portfolio), interval);
        assertThat(total(category), is(total(accountIndex)));
        assertArrayEquals(accountIndex.calculateInvestedCapital(), category.calculateInvestedCapital());
        assertArrayEquals(accountIndex.calculateDelta(), category.calculateDelta());
        assertThat(category.getPerformanceIRR(), closeTo(accountIndex.getPerformanceIRR(), 0.000001));
        assertThat(category.getFinalAccumulatedPercentage(),
                        closeTo(accountIndex.getFinalAccumulatedPercentage(), 0.000001));
    }

    @Test
    public void testTaxonomyExcludesStandaloneTaxesAndRefunds()
    {
        stock.addPrice(new SecurityPrice(LocalDate.of(2024, 1, 2), Values.Quote.factorize(105)));
        var portfolio = new PortfolioBuilder().buy(stock, "2024-01-01", Values.Share.factorize(2), 200_00)
                        .addTo(client);
        var account = portfolio.getReferenceAccount();
        var date = LocalDateTime.parse("2024-01-02T00:00");
        account.addTransaction(new AccountTransaction(date, "EUR", 1_00, stock, AccountTransaction.Type.TAXES));
        account.addTransaction(new AccountTransaction(date, "EUR", 8_00, stock, AccountTransaction.Type.TAX_REFUND));
        var factory = new ExchangeRateProviderFactory(client);
        data = new DashboardData(client, new PreferenceStore(), null, factory, new DataSeriesCache(client, factory));
        var category = data.calculate(stocks, interval, widget(portfolio));
        var accountIndex = data.calculate(series(portfolio), interval);
        assertThat(total(category), is(220_00L));
        assertThat(total(accountIndex), is(220_00L));
        assertThat(category.calculateInvestedCapital()[2], is(200_00L));
        assertThat(accountIndex.calculateInvestedCapital()[2], is(193_00L));
        assertThat(category.calculateDelta()[2], is(20_00L));
        assertThat(accountIndex.calculateDelta()[2], is(27_00L));
        var pretaxSeries = data.getDataSeriesSet().getAvailableSeries().stream()
                        .filter(ds -> ds.getInstance() == portfolio && ds.getType() == DataSeries.Type.PORTFOLIO_PRETAX)
                        .findFirst().orElseThrow();
        var pretaxIndex = data.calculate(pretaxSeries, interval);
        assertArrayEquals(pretaxIndex.calculateInvestedCapital(), category.calculateInvestedCapital());
        assertArrayEquals(pretaxIndex.calculateDelta(), category.calculateDelta());
        assertThat(category.getPerformanceIRR(), closeTo(pretaxIndex.getPerformanceIRR(), 0.000001));
        assertArrayEquals(category.getDeltaPercentage(), pretaxIndex.getDeltaPercentage(), 0.000001);
        assertThat(category.getFinalAccumulatedPercentage(),
                        closeTo(pretaxIndex.getFinalAccumulatedPercentage(), 0.000001));
        var scopedPretaxIndex = data.calculate(pretaxSeries, interval, widget(portfolio));
        assertArrayEquals(category.getDeltaPercentage(), scopedPretaxIndex.getDeltaPercentage(), 0.000001);
    }

    private Object indicatorValue(WidgetFactory type)
    {
        var widget = type.constructWidget();
        widget.getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), stocks.getUUID());
        widget.getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(), first.getUUID());
        return type.constructDelegate(widget, data).getUpdateTask().get();
    }

    @Test
    public void testPerformanceIndicatorsUseTheCombinedFilter()
    {
        assertThat((Double) indicatorValue(WidgetFactory.TTWROR), closeTo(0.1, 0.000001));
        assertThat(Double.isFinite((Double) indicatorValue(WidgetFactory.IRR)), is(true));
        assertThat(indicatorValue(WidgetFactory.ABSOLUTE_CHANGE), is(Money.of("EUR", 20_00)));
        assertThat(indicatorValue(WidgetFactory.DELTA), is(Money.of("EUR", 20_00)));
    }

    @Test
    public void testIndicatorHasBothAccountAndDataSeriesConfiguration()
    {
        var widget = widget(first);
        widget.getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), stocks.getUUID());
        var delegate = WidgetFactory.TOTAL_SUM.constructDelegate(widget, data);
        assertThat(delegate.get(ClientFilterConfig.class).getSelectedItem().getId(), is(first.getUUID()));
        assertThat(delegate.get(DataSeriesConfig.class).getDataSeries(), is(stocks));
        assertThat(delegate.getUpdateTask().get(), is(Money.of("EUR", 220_00)));
    }
}
