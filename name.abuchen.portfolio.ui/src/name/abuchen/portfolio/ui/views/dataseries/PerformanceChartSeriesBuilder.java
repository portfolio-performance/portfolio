package name.abuchen.portfolio.ui.views.dataseries;

import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;

import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator.ClientDataSeries;

public class PerformanceChartSeriesBuilder extends AbstractChartSeriesBuilder
{
    public PerformanceChartSeriesBuilder(TimelineChart chart, DataSeriesCache cache)
    {
        super(chart, cache);
    }

    public void build(DataSeries series, ReportingPeriod reportingPeriod, Aggregation.Period aggregationPeriod)
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
            case ACCUMULATED:
                ILineSeries lineSeries = getChart().addDateSeries(index.getDates(), index.getAccumulatedPercentage(),
                                series.getLabel());
                configure(series, lineSeries);
                break;
            case DELTA_PERCENTAGE:
                IBarSeries barSeries = getChart().addDateBarSeries(index.getDates(), index.getDeltaPercentage(),
                                aggregationPeriod != null ? aggregationPeriod.toString() : series.getLabel());
                configure(series, barSeries);
                break;
            default:
                break;
        }
    }
}
