package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.util.chart.AbstractChartToolTip;
import name.abuchen.portfolio.ui.util.chart.ChartUtil;
import name.abuchen.portfolio.ui.util.chart.ScatterChart;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.util.TextUtil;

/**
 * Bubble chart with one bubble per security held at the end of the reporting
 * period: the market value on the x axis, the return on the y axis. The area
 * of a bubble is proportional to the weight of the security in the holdings.
 */
public class HoldingsWeightReturnWidget extends WidgetDelegate<HoldingsWeightReturnWidget.Result>
{
    public enum Metric
    {
        TTWROR(Messages.LabelPerformanceTTWROR, LazySecurityPerformanceRecord::getTrueTimeWeightedRateOfReturn), //
        IRR(Messages.LabelPerformanceIRR, LazySecurityPerformanceRecord::getIrr), //
        CAPITAL_GAINS_FIFO(Messages.ColumnCapitalGainsPercent,
                        r -> r.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO)), //
        CAPITAL_GAINS_MOVING_AVERAGE(Messages.ColumnCapitalGainsMovingAveragePercent,
                        r -> r.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE));

        private final String label;
        private final Function<LazySecurityPerformanceRecord, Double> valueFunction;

        private Metric(String label, Function<LazySecurityPerformanceRecord, Double> valueFunction)
        {
            this.label = label;
            this.valueFunction = valueFunction;
        }

        public Double valueOf(LazySecurityPerformanceRecord rcrd)
        {
            return valueFunction.apply(rcrd);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    public static class MetricConfig extends EnumBasedConfig<Metric>
    {
        public MetricConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelMetric, Metric.class, Dashboard.Config.METRIC, Policy.EXACTLY_ONE);
        }
    }

    public record Bubble(Security security, Money marketValue, double weight, double value)
    {
    }

    /**
     * The bubbles together with the metric they were calculated with.
     */
    public record Result(Metric metric, List<Bubble> bubbles)
    {
    }

    private record PlacedBubble(Bubble bubble, int x, int y, int radius)
    {
        boolean contains(int px, int py)
        {
            long dx = (long) px - x;
            long dy = (long) py - y;
            return dx * dx + dy * dy <= (long) radius * radius;
        }
    }

    private static final String SERIES_ID = "bubbles"; //$NON-NLS-1$

    /** share of the axis range reserved on each side for the bubbles */
    private static final double MARGIN_RATIO = 0.15;

    /** radius of the biggest bubble relative to the plot area */
    private static final double MAX_RADIUS_RATIO = 0.12;

    private static final int MIN_RADIUS = 3;
    private static final int FILL_ALPHA = 150;
    private static final int LABEL_PADDING = 4;
    private static final int SHORT_NAME_LENGTH = 3;

    @Inject
    private AbstractFinanceView view;

    private Label title;
    private ScatterChart chart;

    /** bubbles sorted by weight, biggest first (painted first) */
    private List<Bubble> bubbles = List.of();

    /** metric of the displayed bubbles */
    private Metric metric = Metric.TTWROR;

    public HoldingsWeightReturnWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new DataSeriesConfig(this, false));
        addConfig(new ReportingPeriodConfig(this));
        addConfig(new MetricConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        var container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new ScatterChart(container);
        chart.getTitle().setVisible(false);

        // the default tooltip picks the series closest to the mouse; the
        // bubbles are hit-tested by their painted area instead
        chart.getToolTip().setActive(false);
        new BubbleToolTip(chart);

        // the axis titles are used as labels in the tooltip only
        var xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setText(Messages.ColumnMarketValue);
        xAxis.getTitle().setVisible(false);
        xAxis.getTick().setFormat(new ThousandsNumberFormat());

        var yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.getTick().setFormat(new AxisTickPercentNumberFormat("0.#%")); //$NON-NLS-1$

        chart.getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                paintBubbles(e);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return false;
            }
        });

        var plotArea = chart.getPlotArea().getControl();
        plotArea.addListener(SWT.MouseMove, this::updateHoverText);
        plotArea.addListener(SWT.MouseExit, e -> plotArea.setToolTipText(null));
        plotArea.addListener(SWT.MouseDoubleClick, this::openInformationPane);

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).applyTo(chart);

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
    public Supplier<Result> getUpdateTask()
    {
        return () -> {
            var selectedMetric = get(MetricConfig.class).getValue();
            var interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            var index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(), interval);

            var snapshot = index.getClientPerformanceSnapshot().orElseThrow(IllegalArgumentException::new);
            var client = snapshot.getClient();
            var converter = getDashboardData().getCurrencyConverter();

            // reuse the valuations at start and end of the reporting period
            var records = LazySecurityPerformanceSnapshot
                            .create(client, converter, interval, snapshot.getStartClientSnapshot(),
                                            snapshot.getEndClientSnapshot())
                            .getRecords().stream() //
                            .filter(r -> r.getMarketValue().isPositive()) //
                            .toList();

            double total = records.stream().mapToDouble(r -> r.getMarketValue().getAmount()).sum();

            var answer = new ArrayList<Bubble>();
            for (var rcrd : records)
            {
                var value = selectedMetric.valueOf(rcrd);
                if (value == null || value.isNaN() || value.isInfinite())
                    continue;

                var marketValue = rcrd.getMarketValue();
                answer.add(new Bubble(rcrd.getSecurity(), marketValue, marketValue.getAmount() / total, value));
            }

            answer.sort(Comparator.comparingDouble(Bubble::weight).reversed());
            return new Result(selectedMetric, answer);
        };
    }

    @Override
    public void update(Result data)
    {
        var currentMetric = get(MetricConfig.class).getValue();

        // the dashboard applies the results of background calculations
        // without checking whether they are still current. If the metric has
        // been changed in the meantime, recalculate with the current metric
        // so that the bubbles and the labels always match.
        if (data != null && data.metric() != currentMetric)
        {
            update();
            return;
        }

        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        this.bubbles = data != null ? data.bubbles() : List.of();
        this.metric = data != null ? data.metric() : currentMetric;

        try
        {
            chart.suspendUpdate(true);

            get(ChartHeightConfig.class).updateGridData(chart, title.getParent());

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.getAxisSet().getYAxis(0).getTitle().setText(metric.toString());

            if (!bubbles.isEmpty())
            {
                var xSeries = bubbles.stream().mapToDouble(b -> b.marketValue().getAmount() / Values.Amount.divider())
                                .toArray();
                var ySeries = bubbles.stream().mapToDouble(Bubble::value).toArray();

                // invisible series: it only defines the range of the axes,
                // the bubbles are painted by the custom paint listener
                var series = chart.addScatterSeries(SERIES_ID, xSeries, ySeries, getWidget().getLabel());
                series.setSymbolType(PlotSymbolType.NONE);

                chart.getAxisSet().adjustRange();
                ChartUtil.addMargins(chart, MARGIN_RATIO);
            }
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private List<PlacedBubble> placeBubbles()
    {
        if (bubbles.isEmpty())
            return List.of();

        var size = chart.getPlotArea().getControl().getSize();
        double maxRadius = MAX_RADIUS_RATIO * Math.min(size.x, size.y);
        double maxWeight = bubbles.get(0).weight();

        var xAxis = chart.getAxisSet().getXAxis(0);
        var yAxis = chart.getAxisSet().getYAxis(0);

        var placed = new ArrayList<PlacedBubble>(bubbles.size());
        for (var bubble : bubbles)
        {
            // the area (not the radius) is proportional to the weight
            int radius = (int) Math.max(MIN_RADIUS, Math.round(maxRadius * Math.sqrt(bubble.weight() / maxWeight)));
            int x = xAxis.getPixelCoordinate(bubble.marketValue().getAmount() / Values.Amount.divider());
            int y = yAxis.getPixelCoordinate(bubble.value());
            placed.add(new PlacedBubble(bubble, x, y, radius));
        }
        return placed;
    }

    private Bubble findBubbleAt(int x, int y)
    {
        // the smaller bubbles are painted last, i.e. on top
        var placed = placeBubbles();
        for (int ii = placed.size() - 1; ii >= 0; ii--)
        {
            if (placed.get(ii).contains(x, y))
                return placed.get(ii).bubble();
        }
        return null;
    }

    private void paintBubbles(PaintEvent e)
    {
        var placed = placeBubbles();
        if (placed.isEmpty())
            return;

        var gc = e.gc;
        var oldAlpha = gc.getAlpha();
        var oldAntialias = gc.getAntialias();
        var oldBackground = gc.getBackground();
        var oldForeground = gc.getForeground();

        var positive = ValueColorScheme.current().positiveForeground();
        var negative = ValueColorScheme.current().negativeForeground();
        var textColor = chart.getAxisSet().getXAxis(0).getTick().getForeground();

        gc.setAntialias(SWT.ON);

        for (var p : placed)
        {
            var color = p.bubble().value() >= 0 ? positive : negative;
            int diameter = 2 * p.radius();
            int left = p.x() - p.radius();
            int top = p.y() - p.radius();

            gc.setAlpha(FILL_ALPHA);
            gc.setBackground(color);
            gc.fillOval(left, top, diameter, diameter);

            gc.setAlpha(255);
            gc.setForeground(color);
            gc.drawOval(left, top, diameter, diameter);

            // label only if it fits into the bubble
            var label = shortLabel(p.bubble().security());
            var extent = gc.textExtent(label);
            if (extent.x + LABEL_PADDING <= diameter && extent.y <= diameter)
            {
                gc.setForeground(textColor);
                gc.drawText(label, p.x() - extent.x / 2, p.y() - extent.y / 2, true);
            }
        }

        gc.setAlpha(oldAlpha);
        gc.setAntialias(oldAntialias);
        gc.setBackground(oldBackground);
        gc.setForeground(oldForeground);
    }

    private static String shortLabel(Security security)
    {
        var ticker = security.getTickerSymbolWithoutStockMarket();
        if (ticker != null && !ticker.isBlank())
            return ticker;

        var name = security.getName() != null ? security.getName().strip() : ""; //$NON-NLS-1$
        if (name.length() <= SHORT_NAME_LENGTH + 1)
            return name;

        return name.substring(0, SHORT_NAME_LENGTH) + "."; //$NON-NLS-1$
    }

    private void updateHoverText(Event event)
    {
        var bubble = findBubbleAt(event.x, event.y);
        var text = bubble != null ? TextUtil.tooltip(bubble.security().getName()) : null;

        var control = (Control) event.widget;
        if (!Objects.equals(text, control.getToolTipText()))
            control.setToolTipText(text);
    }

    private void openInformationPane(Event event)
    {
        if (view == null || event.button != 1)
            return;

        var bubble = findBubbleAt(event.x, event.y);
        if (bubble == null)
            return;

        view.setInformationPaneInput(bubble.security());
        view.showPane();
    }

    private final class BubbleToolTip extends AbstractChartToolTip
    {
        private BubbleToolTip(ScatterChart chart)
        {
            super(chart);
        }

        @Override
        protected Object getFocusObjectAt(Event event)
        {
            return findBubbleAt(event.x, event.y);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            var bubble = (Bubble) getFocusedObject();

            var container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

            var heading = new Label(container, SWT.NONE);
            heading.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            heading.setText(TextUtil.tooltip(bubble.security().getName()));
            GridDataFactory.fillDefaults().span(2, 1).applyTo(heading);

            addRow(container, Messages.ColumnMarketValue, Values.Money.format(bubble.marketValue()));
            addRow(container, Messages.ColumnShareInPercent, Values.Percent2.format(bubble.weight()));
            addRow(container, metric.toString(), Values.Percent2.format(bubble.value()));
        }

        private void addRow(Composite container, String label, String value)
        {
            var left = new Label(container, SWT.NONE);
            left.setText(TextUtil.tooltip(label));

            var right = new Label(container, SWT.RIGHT);
            right.setText(value);
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
        }
    }
}
