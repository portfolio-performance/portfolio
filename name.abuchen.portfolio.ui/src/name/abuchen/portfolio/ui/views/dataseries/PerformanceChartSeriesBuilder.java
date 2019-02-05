package name.abuchen.portfolio.ui.views.dataseries;

import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;

import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
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
        PerformanceIndex index = getCache().lookup(series, reportingPeriod);

        if (series.getType() == DataSeries.Type.CLIENT)
        {
            addClient(series, index, aggregationPeriod);
        }
        else if (series.getType() == DataSeries.Type.CONSUMER_PRICE_INDEX)
        {
            if (index.getDates().length > 0
                            && (aggregationPeriod == null || aggregationPeriod != Aggregation.Period.YEARLY))
            {
                ILineSeries lineSeries = getChart().addDateSeries(index.getDates(), index.getAccumulatedPercentage(),
                                series.getLabel());
                configure(series, lineSeries);
            }
        }
        else
        {
            if (aggregationPeriod != null)
                index = Aggregation.aggregate(index, aggregationPeriod);

            ILineSeries lineSeries = getChart().addDateSeries(index.getDates(), index.getAccumulatedPercentage(),
                            series.getLabel());
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
                ILineSeries lineSeries = getChart().addDateSeries(index.getDates(), index.getAccumulatedPercentage(),
                                series.getLabel());
                configure(series, lineSeries);
                break;
            case DELTA_PERCENTAGE:
                String aggreagtionPeriodLabel = aggregationPeriod != null ? aggregationPeriod.toString()
                                : Messages.LabelAggregationDaily;
                IBarSeries barSeries = getChart().addDateBarSeries(index.getDates(), index.getDeltaPercentage(),
                                aggreagtionPeriodLabel);
                // update label, e.g. 'daily' to 'weekly'
                series.setLabel(aggreagtionPeriodLabel);
                configure(series, barSeries);
                break;
            default:
                break;
        }
    }
}
