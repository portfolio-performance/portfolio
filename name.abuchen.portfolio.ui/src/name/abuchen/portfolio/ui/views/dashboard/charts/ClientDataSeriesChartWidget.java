package name.abuchen.portfolio.ui.views.dashboard.charts;

import static name.abuchen.portfolio.util.ArraysUtil.accumulateAndToDouble;
import static name.abuchen.portfolio.util.ArraysUtil.add;
import static name.abuchen.portfolio.util.ArraysUtil.toDouble;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartShowYAxisConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class ClientDataSeriesChartWidget extends WidgetDelegate<PerformanceIndex>
{
    private static final Color colorTotals = Colors.BLACK;
    private static final Color colorInvestedCapital = Colors.getColor(235, 201, 52); // #EBC934
    private static final Color colorAbsoluteInvestedCapital = Colors.getColor(235, 201, 52); // #EBC934
    private static final Color colorTransferals = Colors.DARK_GRAY;
    private static final Color colorTransferalsAccumulated = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
    private static final Color colorTaxes = Colors.RED;
    private static final Color colorTaxesAccumulated = Colors.RED;
    private static final Color colorAbsoluteDelta = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
    private static final Color colorAbsoluteDeltaAllRecord = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
    private static final Color colorDividends = Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA);
    private static final Color colorDividendsAccumulated = Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA);
    private static final Color colorInterest = Colors.DARK_GREEN;
    private static final Color colorInterestAccumulated = Colors.DARK_GREEN;
    private static final Color colorInterestCharge = Colors.DARK_GREEN;
    private static final Color colorInterestChargeAccumulated = Colors.DARK_GREEN;
    private static final Color colorEarnings = Colors.DARK_GREEN;
    private static final Color colorEarningsAccumulated = Colors.DARK_GREEN;
    private static final Color colorFees = Colors.GRAY;
    private static final Color colorFeesAccumulated = Colors.GRAY;

    private Label title;
    private TimelineChart chart;

    public enum ClientDataSeriesType
    {
        TOTALS(Messages.LabelTotalSum), //
        TRANSFERALS(Messages.LabelTransferals), //
        TRANSFERALS_ACCUMULATED(Messages.LabelAccumulatedTransferals), //
        INVESTED_CAPITAL(Messages.LabelInvestedCapital), //
        ABSOLUTE_INVESTED_CAPITAL(Messages.LabelAbsoluteInvestedCapital), //
        ABSOLUTE_DELTA(Messages.LabelDelta), //
        ABSOLUTE_DELTA_ALL_RECORDS(Messages.LabelAbsoluteDelta), //
        DIVIDENDS(Messages.LabelDividends), //
        DIVIDENDS_ACCUMULATED(Messages.LabelAccumulatedDividends), //
        INTEREST(Messages.LabelInterest), //
        INTEREST_ACCUMULATED(Messages.LabelAccumulatedInterest), //
        INTEREST_CHARGE(Messages.LabelInterestCharge), //
        INTEREST_CHARGE_ACCUMULATED(Messages.LabelAccumulatedInterestCharge), //
        EARNINGS(Messages.LabelEarnings), //
        EARNINGS_ACCUMULATED(Messages.LabelAccumulatedEarnings), //
        FEES(Messages.LabelFees), //
        FEES_ACCUMULATED(Messages.LabelFeesAccumulated), //
        TAXES(Messages.ColumnTaxes), //
        TAXES_ACCUMULATED(Messages.LabelAccumulatedTaxes); //

        private String label;

        private ClientDataSeriesType(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class AspectConfig extends EnumBasedConfig<ClientDataSeriesType>
    {
        public AspectConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelAspect, ClientDataSeriesType.class, Dashboard.Config.CLIENT_DATA_SERIES,
                            Policy.MULTIPLE);

            if (getValues().isEmpty())
            {
                getValues().add(ClientDataSeriesType.TOTALS);
                getValues().add(ClientDataSeriesType.INVESTED_CAPITAL);
                getValues().add(ClientDataSeriesType.ABSOLUTE_DELTA);
            }
        }
    }

    public ClientDataSeriesChartWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new DataSeriesConfig(this, false));
        addConfig(new AspectConfig(this));
        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ChartShowYAxisConfig(this, true));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new TimelineChart(container);
        chart.getTitle().setVisible(false);
        chart.getTitle().setText(title.getText());
        chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());
        chart.getToolTip().setDefaultValueFormat(new AmountNumberFormat());

        chart.getToolTip().reverseLabels(true);

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).span(2, 1).applyTo(chart);

        getDashboardData().getStylingEngine().style(chart);

        container.layout();

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<PerformanceIndex> getUpdateTask()
    {
        DataSeriesCache cache = getDashboardData().getDataSeriesCache();

        DataSeries serie = get(DataSeriesConfig.class).getDataSeries();
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

        return () -> cache.lookup(serie, interval);
    }

    @Override
    public void update(PerformanceIndex index)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        try
        {
            chart.suspendUpdate(true);

            get(ChartHeightConfig.class).updateGridData(chart, title.getParent());

            chart.getTitle().setText(title.getText());

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.getAxisSet().getYAxis(0).getTick().setFormat(new ThousandsNumberFormat());
            chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

            var metrics = get(AspectConfig.class).getValues();

            if (metrics.contains(ClientDataSeriesType.INVESTED_CAPITAL))
            {
                double[] values = toDouble(index.calculateInvestedCapital(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorInvestedCapital, Messages.LabelInvestedCapital, true);
            }

            if (metrics.contains(ClientDataSeriesType.ABSOLUTE_INVESTED_CAPITAL))
            {
                double[] values = toDouble(index.calculateAbsoluteInvestedCapital(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorAbsoluteInvestedCapital,
                                Messages.LabelAbsoluteInvestedCapital, true);
            }

            if (metrics.contains(ClientDataSeriesType.TRANSFERALS))
            {
                double[] values = toDouble(index.getTransferals(), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorTransferals, Messages.LabelTransferals);
            }

            if (metrics.contains(ClientDataSeriesType.TRANSFERALS_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(index.getTransferals(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorTransferalsAccumulated,
                                Messages.LabelAccumulatedTransferals, true);
            }

            if (metrics.contains(ClientDataSeriesType.TAXES))
            {
                double[] values = toDouble(index.getTaxes(), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorTaxes, Messages.ColumnTaxes);
            }

            if (metrics.contains(ClientDataSeriesType.TAXES_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(index.getTaxes(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorTaxesAccumulated, Messages.LabelAccumulatedTaxes, true);
            }

            if (metrics.contains(ClientDataSeriesType.ABSOLUTE_DELTA))
            {
                double[] values = toDouble(index.calculateDelta(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorAbsoluteDelta, Messages.LabelDelta);
            }

            if (metrics.contains(ClientDataSeriesType.ABSOLUTE_DELTA_ALL_RECORDS))
            {
                double[] values = toDouble(index.calculateAbsoluteDelta(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorAbsoluteDeltaAllRecord, Messages.LabelAbsoluteDelta);
            }

            if (metrics.contains(ClientDataSeriesType.DIVIDENDS))
            {
                double[] values = toDouble(index.getDividends(), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorDividends, Messages.LabelDividends);
            }

            if (metrics.contains(ClientDataSeriesType.DIVIDENDS_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(index.getDividends(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorDividendsAccumulated, Messages.LabelAccumulatedDividends);
            }

            if (metrics.contains(ClientDataSeriesType.INTEREST))
            {
                double[] values = toDouble(index.getInterest(), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorInterest, Messages.LabelInterest);
            }

            if (metrics.contains(ClientDataSeriesType.INTEREST_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(index.getInterest(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorInterestAccumulated, Messages.LabelAccumulatedInterest);
            }

            if (metrics.contains(ClientDataSeriesType.INTEREST_CHARGE))
            {
                double[] values = toDouble(index.getInterestCharge(), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorInterestCharge, Messages.LabelInterestCharge);
            }

            if (metrics.contains(ClientDataSeriesType.INTEREST_CHARGE_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(index.getInterestCharge(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorInterestChargeAccumulated,
                                Messages.LabelAccumulatedInterestCharge);
            }

            if (metrics.contains(ClientDataSeriesType.EARNINGS))
            {
                double[] values = toDouble(add(index.getDividends(), index.getInterest()), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorEarnings, Messages.LabelEarnings);
            }

            if (metrics.contains(ClientDataSeriesType.EARNINGS_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(add(index.getDividends(), index.getInterest()),
                                Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorEarningsAccumulated, Messages.LabelAccumulatedEarnings);
            }

            if (metrics.contains(ClientDataSeriesType.FEES))
            {
                double[] values = toDouble(index.getFees(), Values.Amount.divider());
                addBarSerie(values, index.getDates(), colorFees, Messages.LabelFees);
            }

            if (metrics.contains(ClientDataSeriesType.FEES_ACCUMULATED))
            {
                double[] values = accumulateAndToDouble(index.getFees(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorFeesAccumulated, Messages.LabelFeesAccumulated);
            }

            // Totals at the end to be plotted in front of all the other
            if (metrics.contains(ClientDataSeriesType.TOTALS))
            {
                double[] values = toDouble(index.getTotals(), Values.Amount.divider());
                addLineSerie(values, index.getDates(), colorTotals, Messages.LabelTotalSum);
            }

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void addLineSerie(double[] values, LocalDate[] dates, Color color, String label)
    {
        addLineSerie(values, dates, color, label, false);
    }

    private void addLineSerie(double[] values, LocalDate[] dates, Color color, String label, boolean showarea)
    {
        String lineID = get(DataSeriesConfig.class).getDataSeries().getUUID() + label;
        label = label + " (" + get(DataSeriesConfig.class).getDataSeries().getLabel() + ")"; //$NON-NLS-1$ //$NON-NLS-2$

        var lineSeries = chart.addDateSeries(lineID, dates, values, color, label);
        lineSeries.enableArea(showarea);
    }

    private void addBarSerie(double[] values, LocalDate[] dates, Color color, String label)
    {
        String lineID = get(DataSeriesConfig.class).getDataSeries().getUUID() + label;
        label = label + " (" + get(DataSeriesConfig.class).getDataSeries().getLabel() + ")"; //$NON-NLS-1$ //$NON-NLS-2$

        var barSeries = chart.addDateBarSeries(lineID, dates, values, label);
        barSeries.setBarColor(color);
    }
}
