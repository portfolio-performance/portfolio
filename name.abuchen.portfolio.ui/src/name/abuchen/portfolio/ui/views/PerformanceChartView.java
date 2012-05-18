package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.snapshot.ClientIRRYield;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries;

public class PerformanceChartView extends AbstractHistoricView
{
    private TimelineChart chart;

    public PerformanceChartView()
    {
        super(5, 2);
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelInternalRateOfReturn;
    }

    @Override
    protected Composite buildBody(Composite parent)
    {
        chart = new TimelineChart(parent);
        chart.getTitle().setText(Messages.LabelInternalRateOfReturn);
        chart.getLegend().setVisible(true);
        chart.getLegend().setPosition(SWT.BOTTOM);

        // force layout, otherwise range calculation of chart does not work
        parent.layout();
        refreshChart();
        return chart;
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        refreshChart();
    }

    protected void refreshChart()
    {
        try
        {
            chart.suspendUpdate(true);

            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -getReportingYears());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Date startDate = cal.getTime();
            Date endDate = Dates.today();

            // TODO refactor
            // - re-use date array
            // - don't show flat line for not yet existing CPI values

            Date firstDataPoint = addYieldSeries(startDate, endDate);

            addCPISeries(startDate, endDate, firstDataPoint);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private Date addYieldSeries(Date startDate, Date endDate)
    {
        List<ClientIRRYield> yields = new ArrayList<ClientIRRYield>();
        Date firstDataPoint = null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DATE, -1);

        while (cal.getTimeInMillis() < endDate.getTime())
        {
            Date start = cal.getTime();

            cal.add(Calendar.DATE, 1);
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DATE, -1);
            if (cal.getTimeInMillis() > endDate.getTime())
                cal.setTime(endDate);

            ClientIRRYield yield = ClientIRRYield.create(getClient(), start, cal.getTime());
            yields.add(yield);

            if (yield.getIrr() != 0.0 && firstDataPoint == null)
                firstDataPoint = start;
        }

        // if file has no data points at all
        if (firstDataPoint == null)
            firstDataPoint = startDate;

        // dates
        Date[] dates = new Date[yields.size()];
        double[] irr = new double[yields.size()];
        double[] irrAccumulated = new double[yields.size()];

        double accumulated = 0;

        int index = 0;
        for (ClientIRRYield y : yields)
        {
            cal.setTime(y.getSnapshotStart().getTime());
            cal.add(Calendar.DATE, 15);
            dates[index] = cal.getTime();
            irr[index] = y.getIrr();

            accumulated += y.getIrr();
            irrAccumulated[index] = accumulated;

            index++;
        }

        IBarSeries barSeries = chart.addDateBarSeries(dates, irr, Messages.PerformanceChartLabelMonthly);
        barSeries.setBarPadding(50);
        barSeries.setBarColor(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));

        chart.addDateSeries(dates, irrAccumulated, Colors.IRR, Messages.PerformanceChartLabelAccumulatedIRR);
        return firstDataPoint;
    }

    private void addCPISeries(Date startDate, Date endDate, Date firstDataPoint)
    {
        List<ConsumerPriceIndex> rawData = getClient().getConsumerPriceIndeces();

        Calendar cal = Calendar.getInstance();

        ConsumerPriceIndex baseline = null;
        if (startDate.getTime() == firstDataPoint.getTime())
        {
            // set a baseline only if the first data point already contains data
            cal.setTime(startDate);
            cal.add(Calendar.MONTH, -1);
            baseline = lookup(cal, rawData);
        }
        else
        {
            // baseline must be set one month before first data point in order
            // to include the first month

            cal.setTime(firstDataPoint);
            cal.add(Calendar.MONTH, -1);
            firstDataPoint = cal.getTime();
        }

        List<Date> cpiDates = new ArrayList<Date>();
        List<Double> cpiSeries = new ArrayList<Double>();

        cal.setTime(startDate);
        while (cal.getTimeInMillis() < endDate.getTime())
        {
            ConsumerPriceIndex cpi = lookup(cal, rawData);

            if (cal.getTimeInMillis() < firstDataPoint.getTime())
            {
                // if no yields available, start with first yield
                cpiSeries.add(0d);
            }
            else if (cpi != null)
            {
                // if no data is available, start at first data point
                if (baseline == null)
                    baseline = cpi;
                cpiSeries.add((((double) cpi.getIndex() / (double) baseline.getIndex()) - 1) * 100);
            }
            else if (!cpiSeries.isEmpty())
            {
                cpiSeries.add(cpiSeries.get(cpiSeries.size() - 1));
            }
            else
            {
                cpiSeries.add(0d);
            }

            cal.set(Calendar.DAY_OF_MONTH, 15);
            cpiDates.add(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, 1);
        }

        double[] values = new double[cpiSeries.size()];
        for (int ii = 0; ii < values.length; ii++)
            values[ii] = cpiSeries.get(ii);

        chart.addDateSeries(cpiDates.toArray(new Date[0]), values, Colors.CPI, Messages.PerformanceChartLabelCPI);
    }

    private ConsumerPriceIndex lookup(Calendar cal, List<ConsumerPriceIndex> rates)
    {
        for (ConsumerPriceIndex r : rates)
            if (r.getYear() == cal.get(Calendar.YEAR) && r.getMonth() == cal.get(Calendar.MONTH))
                return r;
        return null;
    }

}
