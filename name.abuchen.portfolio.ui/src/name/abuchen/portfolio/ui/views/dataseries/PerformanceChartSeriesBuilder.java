package name.abuchen.portfolio.ui.views.dataseries;

import java.time.LocalDate;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.util.Interval;

public class PerformanceChartSeriesBuilder extends AbstractChartSeriesBuilder
{
    public PerformanceChartSeriesBuilder(TimelineChart chart, DataSeriesCache cache)
    {
        super(chart, cache);
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
            if (aggregationPeriod != null)
                index = Aggregation.aggregate(index, aggregationPeriod);

            var lineSeries = getChart().addDateSeries(series.getUUID(), index.getDates(),
                            index.getAccumulatedPercentage(), series.getLabel());
            configure(series, lineSeries);
        }

    }

    private void addClient(DataSeries series, PerformanceIndex clientIndex, Aggregation.Period aggregationPeriod)
    {
        PerformanceIndex index = aggregationPeriod != null ? Aggregation.aggregate(clientIndex, aggregationPeriod)
                        : clientIndex;

        switch ((ClientDataSeries) series.getInstance())
        {
            case TOTALS:
                var lineSeries = getChart().addDateSeries(series.getUUID(), index.getDates(),
                                index.getAccumulatedPercentage(), series.getLabel());
                configure(series, lineSeries);
                break;
            case DELTA_PERCENTAGE:
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

                // add main bar series (only for tooltip and legend)
                var barSeries = getChart().addDateBarSeries(seriesUUID, index.getDates(), values, positiveColor,
                                aggregationPeriodLabel);
                barSeries.setVisible(false);
                barSeries.setBarOverlay(true);

                // update label, e.g. 'daily' to 'weekly'
                series.setLabel(aggregationPeriodLabel);

                var toolTip = getChart().getToolTip();
                toolTip.addSeriesExclude(positiveUUID);
                toolTip.addSeriesExclude(negativeUUID);
                toolTip.setSecondaryTriangleColor(seriesUUID, negativeColor);

                break;
            default:
                break;
        }
    }

    private void createHalfBarSeries(String uuid, LocalDate[] dates, double[] values, Color color, String barID)
    {
        var barSeries = getChart().addDateBarSeries(uuid, dates, values, color, barID);
        barSeries.setBarPadding(50);
        barSeries.setBarOverlay(true);
        barSeries.setVisibleInLegend(false);
    }
}
