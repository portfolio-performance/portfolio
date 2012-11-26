package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.snapshot.ClientIRRYield;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;

public class PerformanceChartView extends AbstractHistoricView
{
    private static final int NUM_OF_COLORS = 10;
    private static final LineStyle[] LINE_STYLES = new LineStyle[] { LineStyle.SOLID, LineStyle.DOT, LineStyle.DASH,
                    LineStyle.DASHDOT, LineStyle.DASHDOTDOT };

    private SecurityPicker picker;

    private ColorWheel colorWheel;
    private TimelineChart chart;

    @Override
    protected String getTitle()
    {
        return Messages.LabelInternalRateOfReturn;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        addExportButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addExportButton(ToolBar toolBar)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                exporter.setDateFormat(new SimpleDateFormat("yyyy-MM-01")); //$NON-NLS-1$
                exporter.setValueFormat(new DecimalFormat("0.##########")); //$NON-NLS-1$
                exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_EXPORT));
        export.setToolTipText(Messages.MenuExportData);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action config = new Action()
        {
            @Override
            public void run()
            {
                picker.showMenu(getClientEditor().getSite().getShell());
            }
        };
        config.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_CONFIG));
        config.setToolTipText(Messages.MenuConfigureChart);

        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        picker = new SecurityPicker(PerformanceChartView.class.getSimpleName(), parent, getClientEditor());
        picker.setListener(new SecurityPicker.SecurityListener()
        {
            @Override
            public void onAddition(Security[] securities)
            {
                refreshChart();
            }

            @Override
            public void onRemoval(Security[] securities)
            {
                refreshChart();
            }
        });

        colorWheel = new ColorWheel(parent, NUM_OF_COLORS);

        chart = new TimelineChart(parent);
        chart.getTitle().setVisible(false);
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

            for (Security security : picker.getSelectedSecurities())
                addSecuritySeries(startDate, endDate, firstDataPoint, security);

            chart.getSeriesSet().bringToFront(Messages.PerformanceChartLabelCPI);
            chart.getSeriesSet().bringToFront(Messages.PerformanceChartLabelAccumulatedIRR);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private Date addYieldSeries(final Date startDate, final Date endDate)
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
        {
            firstDataPoint = startDate;
        }
        else
        {
            cal.setTime(firstDataPoint);
            cal.add(Calendar.DATE, 1);
            firstDataPoint = cal.getTime();
        }

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

    private void addCPISeries(final Date startDate, final Date endDate, Date firstDataPoint)
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

        chart.addDateSeries(cpiDates.toArray(new Date[0]), values, Colors.CPI, Messages.PerformanceChartLabelCPI) //
                        .setLineStyle(LineStyle.DASHDOTDOT);
    }

    private ConsumerPriceIndex lookup(Calendar cal, List<ConsumerPriceIndex> rates)
    {
        for (ConsumerPriceIndex r : rates)
            if (r.getYear() == cal.get(Calendar.YEAR) && r.getMonth() == cal.get(Calendar.MONTH))
                return r;
        return null;
    }

    private void addSecuritySeries(Date startDate, Date endDate, Date firstDataPoint, Security security)
    {
        Calendar cal = Calendar.getInstance();

        SecurityPrice baseline = null;
        if (startDate.getTime() == firstDataPoint.getTime())
        {
            cal.setTime(startDate);
            cal.add(Calendar.MONTH, -1);
            baseline = security.getSecurityPrice(cal.getTime());
        }
        else
        {
            cal.setTime(firstDataPoint);
            cal.add(Calendar.MONTH, -1);
            firstDataPoint = cal.getTime();
        }

        List<Date> dates = new ArrayList<Date>();
        List<Double> series = new ArrayList<Double>();

        cal.setTime(startDate);
        while (cal.getTimeInMillis() < endDate.getTime())
        {
            // get last day of month
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DATE, -1);
            SecurityPrice price = security.getSecurityPrice(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, 1);

            if (cal.getTimeInMillis() < firstDataPoint.getTime())
            {
                series.add(0d);
            }
            else if (price != null)
            {
                if (baseline == null)
                    baseline = price;
                series.add((((double) price.getValue() / (double) baseline.getValue()) - 1) * 100);
            }
            else if (!series.isEmpty())
            {
                series.add(series.get(series.size() - 1));
            }
            else
            {
                series.add(0d);
            }

            cal.set(Calendar.DAY_OF_MONTH, 15);
            dates.add(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, 1);
        }

        double[] values = new double[series.size()];
        for (int ii = 0; ii < values.length; ii++)
            values[ii] = series.get(ii);

        int index = getClient().getSecurities().indexOf(security);

        chart.addDateSeries(dates.toArray(new Date[0]), //
                        values, //
                        colorWheel.getSegment(index).getColor(), security.getName()) //
                        .setLineStyle(LINE_STYLES[(index / NUM_OF_COLORS) % LINE_STYLES.length]);
    }

}
