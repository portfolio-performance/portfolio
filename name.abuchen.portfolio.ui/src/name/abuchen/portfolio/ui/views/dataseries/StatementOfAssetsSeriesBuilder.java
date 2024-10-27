package name.abuchen.portfolio.ui.views.dataseries;

import static name.abuchen.portfolio.util.ArraysUtil.accumulateAndToDouble;
import static name.abuchen.portfolio.util.ArraysUtil.add;
import static name.abuchen.portfolio.util.ArraysUtil.toDouble;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.util.Interval;

public class StatementOfAssetsSeriesBuilder extends AbstractChartSeriesBuilder
{
    public StatementOfAssetsSeriesBuilder(TimelineChart chart, DataSeriesCache cache)
    {
        super(chart, cache);
    }

    public void build(DataSeries series, Interval reportingPeriod)
    {
        if (!series.isVisible())
            return;

        PerformanceIndex index = getCache().lookup(series, reportingPeriod);

        if (series.getType() == DataSeries.Type.CLIENT)
        {
            addClient(series, index);
        }
        else
        {
            var lineSeries = getChart().addDateSeries(series.getUUID(), index.getDates(),
                            toDouble(index.getTotals(), Values.Amount.divider()), series.getLabel());
            configure(series, lineSeries);
        }
    }

    private void addClient(DataSeries series, PerformanceIndex clientIndex)
    {
        double[] values;

        switch ((ClientDataSeries) series.getInstance())
        {
            case TOTALS:
                values = toDouble(clientIndex.getTotals(), Values.Amount.divider());
                break;
            case TRANSFERALS:
                values = toDouble(clientIndex.getTransferals(), Values.Amount.divider());
                break;
            case TRANSFERALS_ACCUMULATED:
                values = accumulateAndToDouble(clientIndex.getTransferals(), Values.Amount.divider());
                break;
            case INVESTED_CAPITAL:
                values = toDouble(clientIndex.calculateInvestedCapital(), Values.Amount.divider());
                break;
            case ABSOLUTE_INVESTED_CAPITAL:
                values = toDouble(clientIndex.calculateAbsoluteInvestedCapital(), Values.Amount.divider());
                break;
            case ABSOLUTE_DELTA:
                values = toDouble(clientIndex.calculateDelta(), Values.Amount.divider());
                break;
            case ABSOLUTE_DELTA_ALL_RECORDS:
                values = toDouble(clientIndex.calculateAbsoluteDelta(), Values.Amount.divider());
                break;
            case TAXES:
                values = toDouble(clientIndex.getTaxes(), Values.Amount.divider());
                break;
            case TAXES_ACCUMULATED:
                values = accumulateAndToDouble(clientIndex.getTaxes(), Values.Amount.divider());
                break;
            case DIVIDENDS:
                values = toDouble(clientIndex.getDividends(), Values.Amount.divider());
                break;
            case DIVIDENDS_ACCUMULATED:
                values = accumulateAndToDouble(clientIndex.getDividends(), Values.Amount.divider());
                break;
            case INTEREST:
                values = toDouble(clientIndex.getInterest(), Values.Amount.divider());
                break;
            case INTEREST_ACCUMULATED:
                values = accumulateAndToDouble(clientIndex.getInterest(), Values.Amount.divider());
                break;
            case INTEREST_CHARGE:
                values = toDouble(clientIndex.getInterestCharge(), Values.Amount.divider());
                break;
            case INTEREST_CHARGE_ACCUMULATED:
                values = accumulateAndToDouble(clientIndex.getInterestCharge(), Values.Amount.divider());
                break;
            case EARNINGS:
                values = toDouble(add(clientIndex.getDividends(), clientIndex.getInterest()), Values.Amount.divider());
                break;
            case EARNINGS_ACCUMULATED:
                values = accumulateAndToDouble(add(clientIndex.getDividends(), clientIndex.getInterest()),
                                Values.Amount.divider());
                break;
            case FEES:
                values = toDouble(clientIndex.getFees(), Values.Amount.divider());
                break;
            case FEES_ACCUMULATED:
                values = accumulateAndToDouble(clientIndex.getFees(), Values.Amount.divider());
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(series.getInstance()));
        }

        if (series.isLineChart())
        {
            var lineSeries = getChart().addDateSeries(series.getUUID(), clientIndex.getDates(), values,
                            series.getLabel());
            configure(series, lineSeries);
        }
        else
        {
            var barSeries = getChart().addDateBarSeries(series.getUUID(), clientIndex.getDates(), values,
                            series.getLabel());
            configure(series, barSeries);
        }
    }
}
