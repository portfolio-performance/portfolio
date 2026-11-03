package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;

public class AbsolutePerformancePieChart
{
    enum SliceType
    {
        INVESTED_CAPITAL, REMAINING_VALUE, GAIN, LOSS
    }

    record Slice(SliceType type, long amount)
    {
    }

    record ChartValues(Money investedCapital, Money delta, Money currentValue)
    {
        boolean isLoss()
        {
            return delta.isNegative();
        }

        long percentageBasis()
        {
            return isLoss() ? investedCapital.getAmount() : currentValue.getAmount();
        }

        boolean isEmpty()
        {
            return investedCapital.isZero() && delta.isZero() && currentValue.isZero();
        }

        List<Slice> slices()
        {
            if (investedCapital.isNegative() || currentValue.isNegative() || percentageBasis() <= 0)
                return List.of();

            var slices = new ArrayList<Slice>();
            long capital = isLoss() ? currentValue.getAmount() : investedCapital.getAmount();
            if (capital > 0)
                slices.add(new Slice(isLoss() ? SliceType.REMAINING_VALUE : SliceType.INVESTED_CAPITAL, capital));
            if (!delta.isZero())
                slices.add(new Slice(isLoss() ? SliceType.LOSS : SliceType.GAIN,
                                isLoss() ? -delta.getAmount() : delta.getAmount()));
            return List.copyOf(slices);
        }
    }

    private CircularChart chart;
    private Composite container;
    private Composite chartContainer;
    private StackLayout chartLayout;
    private Label status;
    private Label heading;
    private Label currentValueLabel;
    private Label currentValue;
    private Label investedCapital;
    private Label delta;
    private Label deltaLabel;
    private ClientPerformanceSnapshot snapshot;
    private ChartValues values;
    private String inputLabel;

    public Control createControl(Composite parent)
    {
        return createControl(parent, false);
    }

    public Control createControl(Composite parent, boolean summaryBelowChart)
    {
        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(summaryBelowChart ? 1 : 2)
                        .spacing(summaryBelowChart ? 0 : 20, 5).applyTo(container);

        chartContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chartContainer);
        chartLayout = new StackLayout();
        chartContainer.setLayout(chartLayout);
        status = new Label(chartContainer, SWT.WRAP | SWT.CENTER);

        chart = new CircularChart(chartContainer, SeriesType.DOUGHNUT, this::getNodeLabel);
        chart.addLabelPainter(new RenderLabelsCenteredInPie(chart, this::getNodeLabel));
        chart.getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent event)
            {
                paintCurrentValue(event);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return false;
            }
        });
        chart.getToolTip().setToolTipBuilder((tooltip, node) -> {
            var data = new Composite(tooltip, SWT.NONE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);
            var slice = getSlice(node);
            if (slice != null)
            {
                new Label(data, SWT.NONE).setText(getSliceLabel(slice.type()));
                new Label(data, SWT.NONE).setText(formatPercentage(slice));
                long signedAmount = slice.type() == SliceType.LOSS ? -slice.amount() : slice.amount();
                addTooltipLine(data, Values.Money.format(Money.of(values.currentValue().getCurrencyCode(), signedAmount)));
            }
            else
            {
                addTooltipLine(data, inputLabel != null ? inputLabel : Messages.LabelTotalSum);
                addTooltipLine(data, Messages.LabelAbsolutePerformanceCurrentValue + ": " //$NON-NLS-1$
                                + Values.Money.format(values.currentValue()));
            }
            addTooltipLine(data, getPercentageBasisLabel());
        });

        var summary = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 20, 0, 0).applyTo(summary);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(summary);

        heading = new Label(summary, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(heading);
        heading.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);

        currentValueLabel = new Label(summary, SWT.NONE);
        currentValue = addSummaryAmount(summary);
        new Label(summary, SWT.NONE).setText(Messages.LabelAbsolutePerformanceInvestedCapital);
        investedCapital = addSummaryAmount(summary);
        deltaLabel = new Label(summary, SWT.NONE);
        delta = addSummaryAmount(summary);

        updateChart();
        return container;
    }

    public void setChartHeight(int height)
    {
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, height).grab(true, false).applyTo(chartContainer);
        container.layout(true, true);
    }

    private Label addSummaryAmount(Composite parent)
    {
        var amount = new Label(parent, SWT.RIGHT);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(amount);
        return amount;
    }

    private void addTooltipLine(Composite parent, String text)
    {
        var label = new Label(parent, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
        label.setText(text);
    }

    public void refresh(ClientPerformanceSnapshot snapshot, String label)
    {
        this.snapshot = snapshot;
        this.inputLabel = label;
        this.values = snapshot != null ? calculateValues(snapshot) : null;
        if (chart != null)
            updateChart();
    }

    static Money calculateInvestedCapital(ClientPerformanceSnapshot snapshot)
    {
        return snapshot.getValue(CategoryType.INITIAL_VALUE, CategoryType.TRANSFERS);
    }

    static ChartValues calculateValues(ClientPerformanceSnapshot snapshot)
    {
        return new ChartValues(calculateInvestedCapital(snapshot), snapshot.getAbsoluteDelta(),
                        snapshot.getValue(CategoryType.FINAL_VALUE));
    }

    private void updateChart()
    {
        currentValueLabel.setText(Messages.LabelAbsolutePerformanceCurrentValue);
        deltaLabel.setText(Messages.LabelAbsolutePerformanceGainLoss);
        for (var series : chart.getSeriesSet().getSeries())
            chart.getSeriesSet().deleteSeries(series.getId());

        if (values == null)
        {
            heading.setText(inputLabel != null ? inputLabel : Messages.LabelTotalSum);
            currentValue.setText(""); //$NON-NLS-1$
            investedCapital.setText(""); //$NON-NLS-1$
            delta.setText(""); //$NON-NLS-1$
            showStatus(Messages.LabelAbsolutePerformanceNoData);
            return;
        }

        heading.setText(String.format(Messages.LabelAbsolutePerformanceAsOf,
                        inputLabel != null ? inputLabel : Messages.LabelTotalSum,
                        Values.Date.format(snapshot.getEndClientSnapshot().getTime())));
        currentValue.setText(Values.Money.format(values.currentValue()));
        investedCapital.setText(Values.Money.format(values.investedCapital()));
        delta.setText(Values.Money.format(values.delta()));
        delta.setForeground(values.isLoss() ? Colors.theme().redForeground()
                        : values.delta().isZero() ? Colors.theme().defaultForeground() : Colors.theme().greenForeground());

        // SWT draws arcs counterclockwise. Reverse the slices so capital is
        // first when reading clockwise from 12 o'clock.
        var slices = values.slices().reversed();
        if (slices.isEmpty())
        {
            showStatus(values.isEmpty() ? Messages.LabelAbsolutePerformanceNoData
                            : Messages.LabelAbsolutePerformanceUnavailable);
            return;
        }

        if (values.isLoss())
        {
            currentValueLabel.setText(Messages.LabelAbsolutePerformanceRemainingValue);
            currentValue.setText(formatSummaryAmount(values.currentValue(), values.currentValue().getAmount()));
            deltaLabel.setText(Messages.LabelAbsolutePerformanceLoss);
        }
        else
        {
            investedCapital.setText(formatSummaryAmount(values.investedCapital(), values.investedCapital().getAmount()));
            deltaLabel.setText(Messages.LabelAbsolutePerformanceGain);
        }
        delta.setText(formatSummaryAmount(values.delta(),
                        values.isLoss() ? -values.delta().getAmount() : values.delta().getAmount()));

        var series = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        Messages.ColumnAbsolutePerformance_MenuLabel);
        series.setSeries(slices.stream().map(slice -> slice.type().name()).toArray(String[]::new),
                        slices.stream().mapToDouble(slice -> slice.amount() / Values.Amount.divider()).toArray());
        series.setSliceColor(Colors.WHITE);
        series.setColor(slices.stream().map(slice -> getSliceColor(slice.type())).toArray(Color[]::new));
        series.getRootNode().setAngleBounds(new Point(90, 360));
        chart.updateAngleBounds();
        chartLayout.topControl = chart;
        chart.redraw();
        chartContainer.layout();
        container.layout(true, true);
    }

    private void showStatus(String text)
    {
        status.setText(text);
        chartLayout.topControl = status;
        chartContainer.layout();
        container.layout(true, true);
    }

    private String getPercentageBasisLabel()
    {
        return values.isLoss() ? Messages.LabelAbsolutePerformanceInvestedCapitalBasis
                        : Messages.LabelAbsolutePerformanceCurrentValueBasis;
    }

    private Slice getSlice(Node node)
    {
        if (values == null)
            return null;
        return values.slices().stream().filter(slice -> slice.type().name().equals(node.getId())).findFirst().orElse(null);
    }

    private String formatPercentage(Slice slice)
    {
        return Values.Percent2.format(slice.amount() / (double) values.percentageBasis());
    }

    private String formatSummaryAmount(Money amount, long sliceAmount)
    {
        return Values.Money.format(amount) + " (" //$NON-NLS-1$
                        + Values.Percent2.format(sliceAmount / (double) values.percentageBasis()) + ")"; //$NON-NLS-1$
    }

    private String getNodeLabel(Node node)
    {
        var slice = getSlice(node);
        return slice != null ? formatPercentage(slice) : null;
    }

    private String getSliceLabel(SliceType type)
    {
        return switch (type)
        {
            case INVESTED_CAPITAL -> Messages.LabelAbsolutePerformanceInvestedCapital;
            case REMAINING_VALUE -> Messages.LabelAbsolutePerformanceRemainingValue;
            case GAIN -> Messages.LabelAbsolutePerformanceGain;
            case LOSS -> Messages.LabelAbsolutePerformanceLoss;
        };
    }

    private Color getSliceColor(SliceType type)
    {
        return switch (type)
        {
            case INVESTED_CAPITAL, REMAINING_VALUE -> Colors.getColor(70, 130, 180);
            case GAIN -> Colors.getColor(88, 168, 121);
            case LOSS -> Colors.getColor(217, 107, 107);
        };
    }

    private void paintCurrentValue(PaintEvent event)
    {
        if (values == null || values.slices().isEmpty())
            return;
        var xAxis = chart.getAxisSet().getXAxis(0);
        var yAxis = chart.getAxisSet().getYAxis(0);
        int x = xAxis.getPixelCoordinate(0);
        int y = yAxis.getPixelCoordinate(0);
        int available = (int) (Math.min(Math.abs(xAxis.getPixelCoordinate(1) - x),
                        Math.abs(yAxis.getPixelCoordinate(1) - y)) * Math.sqrt(2) * 0.9);
        String text = Messages.LabelAbsolutePerformanceCurrentValue + "\n" + Values.Money.format(values.currentValue()); //$NON-NLS-1$
        var size = event.gc.textExtent(text);
        if (size.x > available || size.y > available)
        {
            text = Values.Money.format(values.currentValue());
            size = event.gc.textExtent(text);
        }
        if (size.x <= available && size.y <= available)
        {
            var foreground = event.gc.getForeground();
            event.gc.setForeground(Colors.theme().defaultForeground());
            event.gc.drawText(text, x - size.x / 2, y - size.y / 2, true);
            event.gc.setForeground(foreground);
        }
    }
}
