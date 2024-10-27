package name.abuchen.portfolio.ui.views;

import static name.abuchen.portfolio.util.ArraysUtil.accumulateAndToDouble;
import static name.abuchen.portfolio.util.ArraysUtil.toDouble;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.ILegend;

import com.google.common.base.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.util.Interval;

public class PortfolioBalanceChart
{
    private static final String PREF_KEY = "portfolio-chart-details"; //$NON-NLS-1$

    private Client client;
    private Portfolio portfolio;
    private Composite container;
    private TimelineChart chart;
    private ExchangeRateProviderFactory exchangeRateProviderFactory;

    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.ABSOLUTE_INVESTED_CAPITAL,
                    ChartDetails.ABSOLUTE_DELTA);

    private Color colorAbsoluteInvestedCapital = Colors.getColor(235, 201, 52); // #EBC934
    private Color colorAbsoluteDelta = Colors.getColor(90, 114, 226); // #5A72E2
    private static final Color colorTaxesAccumulated = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    private static final Color colorFeesAccumulated = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    private Color colorDeltaAreaPositive = Colors.getColor(90, 114, 226); // #5A72E2
    private Color colorDeltaAreaNegative = Colors.getColor(226, 91, 90); // #E25B5A

    public PortfolioBalanceChart(Composite parent, Client client)
    {
        this.client = client;
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());
        chart = new TimelineChart(container);
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new ThousandsNumberFormat());
        chart.getToolTip().setDefaultValueFormat(new AmountNumberFormat());

        readChartConfig(client);

        ILegend legend = chart.getLegend();
        legend.setPosition(SWT.BOTTOM);
        legend.setVisible(true);

        setupTooltip();
    }

    public Control getControl()
    {
        return container;
    }

    public void redraw()
    {
        chart.redraw();
    }

    private final void readChartConfig(Client client)
    {
        String pref = ReadOnlyClient.unwrap(client).getProperty(PREF_KEY);
        if (pref == null)
            return;

        chartConfig.clear();
        for (String key : pref.split(",")) //$NON-NLS-1$
        {
            try
            {
                chartConfig.add(ChartDetails.valueOf(key));
            }
            catch (IllegalArgumentException ignore)
            {
                // ignore unknown configuration parameters
            }
        }
    }

    public void updateChart(Portfolio portfolio, ExchangeRateProviderFactory exchangeRateProviderFactory)
    {
        var isNewPortfolio = !Objects.equal(this.portfolio, portfolio);

        this.portfolio = portfolio;
        this.exchangeRateProviderFactory = exchangeRateProviderFactory;
        chart.getTitle().setText(portfolio.getName());

        // unless we are updating an existing portfolio (for example adding or
        // removing a data series), we clear the chart as to not show wrong data
        // series data
        if (isNewPortfolio)
            clearChart();

        computeAndUpdateChart();
    }

    private void clearChart()
    {
        try
        {
            chart.suspendUpdate(true);

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());
        }

        finally
        {
            chart.adjustRange();
            chart.suspendUpdate(false);
        }
    }

    private void computeAndUpdateChart()
    {
        new Job(PortfolioBalanceChart.class.getName())
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                computeChart();
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void computeChart()
    {
        List<PortfolioTransaction> tx = portfolio.getTransactions();

        if (tx.isEmpty())
            return;

        Collections.sort(tx, Transaction.BY_DATE);

        LocalDate now = LocalDate.now();
        LocalDate start = tx.get(0).getDateTime().toLocalDate();
        LocalDate end = tx.get(tx.size() - 1).getDateTime().toLocalDate();

        if (now.isAfter(end))
            end = now;
        if (now.isBefore(start))
            start = now;

        CurrencyConverter converter = new CurrencyConverterImpl(exchangeRateProviderFactory, client.getBaseCurrency());

        var warnings = new ArrayList<Exception>();

        var index = PerformanceIndex.forPortfolio(client, converter, portfolio, Interval.of(start, end), warnings);

        Display.getDefault().asyncExec(() -> updateChart(index));
    }

    private void updateChart(PerformanceIndex index)
    {
        try
        {
            chart.suspendUpdate(true);

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            int days = (int) ChronoUnit.DAYS.between(index.getDates()[0],
                            index.getDates()[index.getDates().length - 1]);

            // Disable SWT antialias for more than 1000 records due
            // to SWT performance issue in Drawing
            var swtAntialias = days > 1000 ? SWT.OFF : SWT.ON;

            // reverse the order

            var lineSeries = chart.addDateSeries(portfolio.getUUID(), index.getDates(),
                            toDouble(index.getTotals(), Values.Amount.divider()), Colors.CASH, portfolio.getName());
            lineSeries.setAntialias(swtAntialias);

            if (chartConfig.contains(ChartDetails.ABSOLUTE_INVESTED_CAPITAL))
                addAbsoluteInvestedCapital(index, swtAntialias);

            if (chartConfig.contains(ChartDetails.ABSOLUTE_DELTA))
                addAbsoluteDeltaAllRecords(index, swtAntialias);

            if (chartConfig.contains(ChartDetails.TAXES_ACCUMULATED))
                addTaxes(index, swtAntialias);

            if (chartConfig.contains(ChartDetails.FEES_ACCUMULATED))
                addFees(index, swtAntialias);

        }
        finally
        {
            chart.adjustRange();
            chart.suspendUpdate(false);
        }
    }

    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new ExportDropDown());
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE, this::chartConfigAboutToShow));
    }

    public void chartConfigAboutToShow(IMenuManager manager)
    {
        manager.add(addMenuAction(ChartDetails.ABSOLUTE_INVESTED_CAPITAL));
        manager.add(addMenuAction(ChartDetails.ABSOLUTE_DELTA));
        manager.add(addMenuAction(ChartDetails.TAXES_ACCUMULATED));
        manager.add(addMenuAction(ChartDetails.FEES_ACCUMULATED));
    }

    private Action addMenuAction(ChartDetails detail)
    {
        Action action = new SimpleAction(detail.toString(), a -> {
            boolean isActive = chartConfig.contains(detail);

            if (isActive)
                chartConfig.remove(detail);
            else
                chartConfig.add(detail);

            ReadOnlyClient.unwrap(client).setProperty(PREF_KEY, String.join(",", //$NON-NLS-1$
                            chartConfig.stream().map(ChartDetails::name).toList()));

            computeAndUpdateChart();
        });

        action.setChecked(chartConfig.contains(detail));
        return action;
    }

    private final class ExportDropDown extends DropDown implements IMenuListener
    {
        private ExportDropDown()
        {
            super(Messages.MenuExportData, Images.EXPORT, SWT.NONE);
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new SimpleAction(Messages.MenuExportChartData, a -> {
                TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                exporter.export(chart.getTitle().getText() + ".csv"); //$NON-NLS-1$
            }));

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, chart.getTitle().getText());
        }
    }

    private void addAbsoluteInvestedCapital(PerformanceIndex index, int swtAntialias)
    {
        double[] values = toDouble(index.calculateAbsoluteInvestedCapital(), Values.Amount.divider());
        String lineID = Messages.LabelAbsoluteInvestedCapital;

        var lineSeries = chart.addDateSeries(lineID, index.getDates(), values, colorAbsoluteInvestedCapital, lineID);
        lineSeries.enableArea(true);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addAbsoluteDeltaAllRecords(PerformanceIndex index, int swtAntialias)
    {
        double[] values = toDouble(index.calculateAbsoluteDelta(), Values.Amount.divider());

        double[] valuesRelativePositive = new double[values.length];
        double[] valuesRelativeNegative = new double[values.length];
        for (int ii = 0; ii < values.length; ii++)
        {
            if (values[ii] >= 0)
            {
                valuesRelativePositive[ii] = values[ii];
                valuesRelativeNegative[ii] = 0;
            }
            else
            {
                valuesRelativePositive[ii] = 0;
                valuesRelativeNegative[ii] = values[ii];
            }
        }
        String lineIDNeg = Messages.LabelAbsoluteDelta + "Negative"; //$NON-NLS-1$
        String lineIDPos = Messages.LabelAbsoluteDelta + "Positive"; //$NON-NLS-1$

        var lineSeries2ndNegative = chart.addDateSeries(lineIDNeg, index.getDates(), valuesRelativeNegative,
                        colorDeltaAreaNegative, lineIDNeg);
        lineSeries2ndNegative.setAntialias(swtAntialias);
        lineSeries2ndNegative.enableArea(true);
        lineSeries2ndNegative.setVisibleInLegend(false);
        lineSeries2ndNegative.setLineWidth(1);

        var lineSeries2ndPositive = chart.addDateSeries(lineIDPos, index.getDates(), valuesRelativePositive,
                        colorDeltaAreaPositive, lineIDPos);
        lineSeries2ndPositive.setAntialias(swtAntialias);
        lineSeries2ndPositive.enableArea(true);
        lineSeries2ndPositive.setVisibleInLegend(false);
        lineSeries2ndPositive.setLineWidth(1);

        String lineID = Messages.LabelAbsoluteDelta;

        var lineSeries = chart.addDateSeries(lineID, index.getDates(), values, colorAbsoluteDelta, lineID);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addTaxes(PerformanceIndex index, int swtAntialias)
    {
        double[] values = accumulateAndToDouble(index.getTaxes(), Values.Amount.divider());
        String lineID = Messages.LabelAccumulatedTaxes;

        var lineSeries = chart.addDateSeries(lineID, index.getDates(), values, colorTaxesAccumulated, lineID);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addFees(PerformanceIndex index, int swtAntialias)
    {
        double[] values = accumulateAndToDouble(index.getFees(), Values.Amount.divider());
        String lineID = Messages.LabelFeesAccumulated;

        var lineSeries = chart.addDateSeries(lineID, index.getDates(), values, colorFeesAccumulated, lineID);
        lineSeries.setAntialias(swtAntialias);
    }

    private enum ChartDetails
    {
        ABSOLUTE_INVESTED_CAPITAL(Messages.LabelAbsoluteInvestedCapital), //
        ABSOLUTE_DELTA(Messages.LabelAbsoluteDelta), //
        TAXES_ACCUMULATED(Messages.LabelAccumulatedTaxes), //
        FEES_ACCUMULATED(Messages.LabelFeesAccumulated);

        private final String label;

        private ChartDetails(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private void setupTooltip()
    {
        TimelineChartToolTip toolTip = chart.getToolTip();
        toolTip.addSeriesExclude(Messages.LabelAbsoluteDelta + "Positive"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelAbsoluteDelta + "Negative"); //$NON-NLS-1$
    }

    public void setAbsoluteDeltaColor(Color color)
    {
        this.colorAbsoluteDelta = color;
    }

    public void setAbsoluteInvestedCapitalColor(Color color)
    {
        this.colorAbsoluteInvestedCapital = color;
    }

    public void setDeltaAreaNegative(Color color)
    {
        this.colorDeltaAreaNegative = color;
    }

    public void setDeltaAreaPositive(Color color)
    {
        this.colorDeltaAreaPositive = color;
    }

}
