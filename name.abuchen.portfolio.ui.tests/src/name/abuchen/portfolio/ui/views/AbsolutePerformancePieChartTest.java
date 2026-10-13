package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtchart.ICircularSeries;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.views.AbsolutePerformancePieChart.ChartValues;
import name.abuchen.portfolio.ui.views.AbsolutePerformancePieChart.Slice;
import name.abuchen.portfolio.ui.views.AbsolutePerformancePieChart.SliceType;

@SuppressWarnings("nls")
public class AbsolutePerformancePieChartTest
{
    private ChartValues values(long invested, long delta)
    {
        return new ChartValues(Money.of(CurrencyUnit.EUR, invested), Money.of(CurrencyUnit.EUR, delta),
                        Money.of(CurrencyUnit.EUR, invested + delta));
    }

    @Test
    public void testGainUsesCurrentValueAsPercentageBasis()
    {
        var values = values(100_00, 20_00);
        assertThat(values.slices(), is(List.of(new Slice(SliceType.INVESTED_CAPITAL, 100_00), new Slice(SliceType.GAIN, 20_00))));
        assertThat(values.percentageBasis(), is(120_00L));
        assertThat(values.isLoss(), is(false));
    }

    @Test
    public void testLossUsesInvestedCapitalAsPercentageBasis()
    {
        var values = values(100_00, -20_00);
        assertThat(values.slices(), is(List.of(new Slice(SliceType.REMAINING_VALUE, 80_00), new Slice(SliceType.LOSS, 20_00))));
        assertThat(values.percentageBasis(), is(100_00L));
        assertThat(values.delta(), is(Money.of(CurrencyUnit.EUR, -20_00)));
        assertThat(values.isLoss(), is(true));
    }

    @Test
    public void testBreakEvenHasOnlyCapitalSlice()
    {
        var values = values(100_00, 0);
        assertThat(values.slices(), is(List.of(new Slice(SliceType.INVESTED_CAPITAL, 100_00))));
        assertThat(values.percentageBasis(), is(100_00L));
    }

    @Test
    public void testZeroInvestedCapitalHasOnlyGainSlice()
    {
        var values = values(0, 20_00);
        assertThat(values.slices(), is(List.of(new Slice(SliceType.GAIN, 20_00))));
        assertThat(values.percentageBasis(), is(20_00L));
    }

    @Test
    public void testTotalLossHasOnlyLossSlice()
    {
        var values = values(100_00, -100_00);
        assertThat(values.slices(), is(List.of(new Slice(SliceType.LOSS, 100_00))));
        assertThat(values.percentageBasis(), is(100_00L));
        assertThat(values.currentValue(), is(Money.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testNegativeInvestedCapitalPreservesAmountsWithoutSlices()
    {
        var values = values(-100_00, 300_00);
        assertThat(values.slices().isEmpty(), is(true));
        assertThat(values.isEmpty(), is(false));
        assertThat(values.investedCapital(), is(Money.of(CurrencyUnit.EUR, -100_00)));
        assertThat(values.currentValue(), is(Money.of(CurrencyUnit.EUR, 200_00)));
    }

    @Test
    public void testNegativeCurrentValuePreservesAmountsWithoutSlices()
    {
        var values = values(100_00, -120_00);
        assertThat(values.slices().isEmpty(), is(true));
        assertThat(values.isEmpty(), is(false));
        assertThat(values.currentValue(), is(Money.of(CurrencyUnit.EUR, -20_00)));
        assertThat(values.delta(), is(Money.of(CurrencyUnit.EUR, -120_00)));
    }

    @Test
    public void testZeroAmountsHaveNoDataAndNoSlices()
    {
        var values = values(0, 0);
        assertThat(values.slices().isEmpty(), is(true));
        assertThat(values.isEmpty(), is(true));
    }

    @Test
    public void testHistoricalSnapshotExcludesLaterTransactions()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2024-01-01", 100_00).interest("2024-02-01", 20_00)
                        .interest("2024-03-01", 30_00).addTo(client);
        var date = LocalDate.of(2024, 2, 15);
        var snapshot = new ClientPerformanceSnapshot(client, new TestCurrencyConverter(), LocalDate.of(1900, 1, 1), date);
        var values = AbsolutePerformancePieChart.calculateValues(snapshot);

        assertThat(snapshot.getEndClientSnapshot().getTime(), is(date));
        assertThat(values.investedCapital(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(values.delta(), is(Money.of(CurrencyUnit.EUR, 20_00)));
        assertThat(values.currentValue(), is(Money.of(CurrencyUnit.EUR, 120_00)));
    }

    @Test
    public void testRefreshShowsLossThenSignedFallbackThenGain()
    {
        var shell = new Shell(Display.getDefault());
        try
        {
            shell.setSize(640, 480);
            shell.setLayout(new FillLayout());
            var chart = new AbsolutePerformancePieChart();
            var control = (Composite) chart.createControl(shell);
            shell.layout(true, true);
            var circularChart = controls(control).filter(CircularChart.class::isInstance)
                            .map(CircularChart.class::cast).findFirst().orElseThrow();

            chart.refresh(accountSnapshot(new AccountBuilder().deposit_("2024-01-01", 100_00)
                            .fees____("2024-02-01", 20_00)), "Selected account");
            var series = (ICircularSeries<?>) circularChart.getSeriesSet().getSeries()[0];
            assertThat(series.getNodeById("LOSS").getValue(), is(20d));
            assertThat(series.getNodeById("REMAINING_VALUE").getValue(), is(80d));
            var capitalAngle = series.getNodeById("REMAINING_VALUE").getAngleBounds();
            assertThat((capitalAngle.x + capitalAngle.y) % 360, is(90));
            assertThat(circularChart.getVisible(), is(true));
            assertThat(labelTexts(control).contains(Values.Money.format(Money.of(CurrencyUnit.EUR, -20_00))
                            + " (" + Values.Percent2.format(0.2) + ")"), is(true));
            assertThat(labelTexts(control).contains(Values.Money.format(Money.of(CurrencyUnit.EUR, 80_00))
                            + " (" + Values.Percent2.format(0.8) + ")"), is(true));
            assertThat(labelTexts(control).contains(Messages.LabelAbsolutePerformanceRemainingValue), is(true));
            assertThat(labelTexts(control).contains(Messages.LabelAbsolutePerformanceLoss), is(true));
            assertThat(labelTexts(control).contains(Messages.LabelAbsolutePerformanceInvestedCapitalBasis), is(false));

            chart.refresh(accountSnapshot(new AccountBuilder().deposit_("2024-01-01", 100_00)
                            .withdraw("2024-02-01", 200_00).interest("2024-03-01", 300_00)), "Selected account");
            assertThat(circularChart.getVisible(), is(false));
            assertThat(circularChart.getSeriesSet().getSeries().length, is(0));
            assertThat(labelTexts(control).contains(Messages.LabelAbsolutePerformanceUnavailable), is(true));
            assertThat(labelTexts(control).contains(Values.Money.format(Money.of(CurrencyUnit.EUR, -100_00))), is(true));
            assertThat(labelTexts(control).contains(Values.Money.format(Money.of(CurrencyUnit.EUR, 200_00))), is(true));
            assertThat(labelTexts(control).stream().noneMatch(text -> text.contains("%")), is(true));

            chart.refresh(accountSnapshot(new AccountBuilder().deposit_("2024-01-01", 100_00)
                            .interest("2024-02-01", 20_00)), "Selected account");
            assertThat(circularChart.getVisible(), is(true));
            series = (ICircularSeries<?>) circularChart.getSeriesSet().getSeries()[0];
            assertThat(series.getNodeById("GAIN").getValue(), is(20d));
            capitalAngle = series.getNodeById("INVESTED_CAPITAL").getAngleBounds();
            assertThat((capitalAngle.x + capitalAngle.y) % 360, is(90));
            assertThat(labelTexts(control).contains(Values.Money.format(Money.of(CurrencyUnit.EUR, 100_00))
                            + " (" + Values.Percent2.format(100d / 120d) + ")"), is(true));
            assertThat(labelTexts(control).contains(Values.Money.format(Money.of(CurrencyUnit.EUR, 20_00))
                            + " (" + Values.Percent2.format(20d / 120d) + ")"), is(true));
            assertThat(labelTexts(control).contains(Messages.LabelAbsolutePerformanceGain), is(true));
            assertThat(labelTexts(control).contains(Messages.LabelAbsolutePerformanceCurrentValueBasis), is(false));
        }
        finally
        {
            shell.dispose();
        }
    }

    @Test
    public void testShallowPaneKeepsChartFullHeight()
    {
        var shell = new Shell(Display.getDefault());
        try
        {
            shell.setSize(1740, 230);
            shell.setLayout(new FillLayout());
            var chart = new AbsolutePerformancePieChart();
            var control = (Composite) chart.createControl(shell);
            chart.refresh(accountSnapshot(new AccountBuilder().deposit_("2024-01-01", 100_00)
                            .interest("2024-02-01", 20_00)), "Selected account");
            shell.layout(true, true);
            var circularChart = controls(control).filter(CircularChart.class::isInstance)
                            .map(CircularChart.class::cast).findFirst().orElseThrow();

            assertThat(circularChart.getSize().y, is(control.getClientArea().height));
            assertTrue("The plot must remain readable in a shallow information pane",
                            circularChart.getPlotArea().getSize().y >= 100);
        }
        finally
        {
            shell.dispose();
        }
    }

    private ClientPerformanceSnapshot accountSnapshot(AccountBuilder account)
    {
        var client = new Client();
        account.addTo(client);
        return new ClientPerformanceSnapshot(client, new TestCurrencyConverter(), LocalDate.of(1900, 1, 1),
                        LocalDate.of(2024, 4, 1));
    }

    private Stream<Control> controls(Composite parent)
    {
        return Stream.of(parent.getChildren()).flatMap(control -> control instanceof Composite composite
                        ? Stream.concat(Stream.of(control), controls(composite)) : Stream.of(control));
    }

    private List<String> labelTexts(Composite parent)
    {
        return controls(parent).filter(Label.class::isInstance).map(Label.class::cast).map(Label::getText).toList();
    }

    @Test
    public void testIncludesPerformanceFromFullySoldSecurity()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        var account = new AccountBuilder().deposit_("2024-01-01", 100_00).addTo(client);
        new PortfolioBuilder(account).buy(security, "2024-01-02", Values.Share.factorize(1), 100_00)
                        .sell(security, "2024-02-01", Values.Share.factorize(1), 150_00).addTo(client);

        var snapshot = new ClientPerformanceSnapshot(client, new TestCurrencyConverter(), LocalDate.of(1900, 1, 1),
                        LocalDate.of(2024, 3, 1));

        assertThat(snapshot.getEndClientSnapshot().getJointPortfolio().getPositions().size(), is(0));
        assertThat(AbsolutePerformancePieChart.calculateInvestedCapital(snapshot), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(snapshot.getAbsoluteDelta(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 150_00)));
    }

    @Test
    public void testFilteredSecurityIncludesSoldHoldingsAndDividends()
    {
        var client = new Client();
        var security = new SecurityBuilder().addPrice("2024-03-01", Values.Quote.factorize(100)).addTo(client);
        var otherSecurity = new SecurityBuilder().addTo(client);
        var account = new AccountBuilder().deposit_("2024-01-01", 1000_00)
                        .dividend("2024-02-15", 10_00, security).addTo(client);
        new PortfolioBuilder(account).buy(security, "2024-01-02", Values.Share.factorize(2), 200_00)
                        .sell(security, "2024-02-01", Values.Share.factorize(1), 150_00)
                        .buy(otherSecurity, "2024-01-03", Values.Share.factorize(1), 300_00).addTo(client);

        var snapshot = new ClientPerformanceSnapshot(new ClientSecurityFilter(security).filter(client),
                        new TestCurrencyConverter(), LocalDate.of(1900, 1, 1), LocalDate.of(2024, 3, 1));

        assertThat(AbsolutePerformancePieChart.calculateInvestedCapital(snapshot), is(Money.of(CurrencyUnit.EUR, 40_00)));
        assertThat(snapshot.getAbsoluteDelta(), is(Money.of(CurrencyUnit.EUR, 60_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 100_00)));
    }
}
