package name.abuchen.portfolio.ui.views.dataseries;

import java.util.Arrays;

import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;

import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.PerformanceChartView;
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

            ILineSeries lineSeries = getChart().addDateSeries(series.getUUID(), index.getDates(), performanceIndexToDataSeries(index),
                            series.getLabel());
            lineSeries.setYAxisId(PerformanceChartView.TOTALS_YAXIS_INDEX);
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
                ILineSeries lineSeries = getChart().addDateSeries(series.getUUID(), index.getDates(), performanceIndexToDataSeries(index),
                                series.getLabel());
                configure(series, lineSeries);
                lineSeries.setYAxisId(PerformanceChartView.TOTALS_YAXIS_INDEX);
                break;
            case DELTA_PERCENTAGE:
                String aggreagtionPeriodLabel = aggregationPeriod != null ? aggregationPeriod.toString()
                                : Messages.LabelAggregationDaily;
                IBarSeries barSeries = getChart().addDateBarSeries(series.getUUID(), index.getDates(), index.getDeltaPercentage(),
                                aggreagtionPeriodLabel);
                barSeries.setYAxisId(PerformanceChartView.DELTA_PERCENTAGE_YAXIS_INDEX);
                // update label, e.g. 'daily' to 'weekly'
                series.setLabel(aggreagtionPeriodLabel);
                configure(series, barSeries);
                break;
            default:
                break;
        }
    }
    
    private double[] performanceIndexToDataSeries(PerformanceIndex index)
    {
        return Arrays.stream(index.getAccumulatedPercentage()).map(x -> x + 1d).toArray();
    }
}
