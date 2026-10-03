package name.abuchen.portfolio.ui.views.dataseries;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.IRRSeries;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.util.Interval;

public class PerformanceChartSeriesBuilder extends AbstractChartSeriesBuilder
{
    private final PerformanceMetric metric;

    public PerformanceChartSeriesBuilder(TimelineChart chart, DataSeriesCache cache)
    {
        this(chart, cache, PerformanceMetric.TTWROR);
    }

    public PerformanceChartSeriesBuilder(TimelineChart chart, DataSeriesCache cache, PerformanceMetric metric)
    {
        super(chart, cache);
        this.metric = metric;
    }

    public void build(DataSeries series, Interval reportingPeriod, Aggregation.Period aggregationPeriod)
    {
        if (!series.isVisible())
            return;

        PerformanceIndex index = getCache().lookup(series, reportingPeriod);

        if (series.getType() == DataSeries.Type.CLIENT)
        {
            addClient(series, index, aggregationPeriod);
        }
        else
        {
            addPerformanceSeries(series, index, aggregationPeriod);
        }
    }

    private void addPerformanceSeries(DataSeries series, PerformanceIndex index, Aggregation.Period aggregationPeriod)
    {
        if (metric == PerformanceMetric.IRR)
        {
            addIRRSeries(series, index, aggregationPeriod);
            return;
        }

        if (aggregationPeriod != null)
            index = Aggregation.aggregate(index, aggregationPeriod);

        var lineSeries = getChart().addDateSeries(series.getUUID(), index.getDates(), index.getAccumulatedPercentage(),
                        series.getLabel());
        configure(series, lineSeries);
    }

    private void addClient(DataSeries series, PerformanceIndex clientIndex, Aggregation.Period aggregationPeriod)
    {
        switch ((ClientDataSeries) series.getInstance())
        {
            case TOTALS:
                addPerformanceSeries(series, clientIndex, aggregationPeriod);
                break;
            case DELTA_PERCENTAGE:
                addDeltaSeries(series, clientIndex, aggregationPeriod);
                break;
            default:
                break;
        }
    }

    private void addIRRSeries(DataSeries series, PerformanceIndex index, Aggregation.Period aggregationPeriod)
    {
        LocalDate[] dates = IRRSeries.getDates(index);
        double[] values = IRRSeries.calculate(index);

        if (aggregationPeriod != null)
        {
            List<LocalDate> aggregatedDates = new ArrayList<>();
            List<Double> aggregatedValues = new ArrayList<>();

            LocalDate nextBoundary = aggregationPeriod.getStartDateFor(dates[0]).plus(aggregationPeriod.getPeriod());
            LocalDate kill = nextBoundary.minusDays(1);

            for (int ii = 0; ii < dates.length; ii++)
            {
                if (dates[ii].equals(kill) || ii == dates.length - 1)
                {
                    aggregatedDates.add(dates[ii]);
                    aggregatedValues.add(values[ii]);
                    nextBoundary = nextBoundary.plus(aggregationPeriod.getPeriod());
                    kill = nextBoundary.minusDays(1);
                }
            }

            dates = aggregatedDates.toArray(new LocalDate[0]);
            values = new double[aggregatedValues.size()];
            for (int ii = 0; ii < values.length; ii++)
                values[ii] = aggregatedValues.get(ii);
        }

        var lineSeries = getChart().addDateSeries(series.getUUID(), dates, values, series.getLabel());
        configure(series, lineSeries);
    }

    private void addDeltaSeries(DataSeries series, PerformanceIndex clientIndex, Aggregation.Period aggregationPeriod)
    {
        PerformanceIndex index = aggregationPeriod != null ? Aggregation.aggregate(clientIndex, aggregationPeriod)
                        : clientIndex;

        String aggregationPeriodLabel = aggregationPeriod != null ? aggregationPeriod.toString()
                        : Messages.LabelAggregationDaily;
        String seriesUUID = series.getUUID();

        double[] values = index.getDeltaPercentage();

        double[] positiveValues = new double[values.length];
        double[] negativeValues = new double[values.length];
        for (int ii = 0; ii < values.length; ii++)
        {
            if (values[ii] >= 0)
            {
                positiveValues[ii] = values[ii];
                negativeValues[ii] = 0;
            }
            else
            {
                positiveValues[ii] = 0;
                negativeValues[ii] = values[ii];
            }
        }

        String positiveUUID = seriesUUID + "Positive"; //$NON-NLS-1$
        String negativeUUID = seriesUUID + "Negative"; //$NON-NLS-1$

        Color positiveColor = Colors.getColor(series.getColor());
        Color negativeColor = Colors.getColor(series.getColorNegative());

        String positiveBarID = aggregationPeriodLabel + "Positive"; //$NON-NLS-1$
        String negativeBarID = aggregationPeriodLabel + "Negative"; //$NON-NLS-1$

        createHalfBarSeries(positiveUUID, index.getDates(), positiveValues, positiveColor, positiveBarID);
        createHalfBarSeries(negativeUUID, index.getDates(), negativeValues, negativeColor, negativeBarID);

        var barSeries = getChart().addDateBarSeries(seriesUUID, index.getDates(), values, positiveColor,
                        aggregationPeriodLabel);
        barSeries.setVisible(false);
        barSeries.setBarOverlay(true);

        series.setLabel(aggregationPeriodLabel);

        var toolTip = getChart().getToolTip();
        toolTip.addSeriesExclude(positiveUUID);
        toolTip.addSeriesExclude(negativeUUID);
        toolTip.setSecondaryTriangleColor(seriesUUID, negativeColor);
    }

    private void createHalfBarSeries(String uuid, LocalDate[] dates, double[] values, Color color, String barID)
    {
        var barSeries = getChart().addDateBarSeries(uuid, dates, values, color, barID);
        barSeries.setBarPadding(50);
        barSeries.setBarOverlay(true);
        barSeries.setVisibleInLegend(false);
    }
}
