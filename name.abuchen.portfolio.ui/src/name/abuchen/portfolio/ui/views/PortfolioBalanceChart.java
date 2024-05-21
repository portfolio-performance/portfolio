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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.ILegend;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

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
import name.abuchen.portfolio.util.Interval;

public class PortfolioBalanceChart extends TimelineChart // NOSONAR
{
    private static final String PREF_KEY = "portfolio-chart-details"; //$NON-NLS-1$

    private Client client;
    private Portfolio portfolio;
    private ExchangeRateProviderFactory exchangeRateProviderFactory;

    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.ABSOLUTE_INVESTED_CAPITAL,
                    ChartDetails.ABSOLUTE_DELTA);

    private static final Color colorAbsoluteInvestedCapital = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    private static final Color colorAbsoluteDelta = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    private static final Color colorTaxesAccumulated = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    private static final Color colorFeesAccumulated = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

    public PortfolioBalanceChart(Composite parent, Client client)
    {
        super(parent);
        this.client = client;

        getTitle().setVisible(false);

        readChartConfig(client);

        ILegend legend = getLegend();
        legend.setPosition(SWT.BOTTOM);
        legend.setVisible(true);
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
        getTitle().setText(portfolio.getName());

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
            suspendUpdate(true);

            for (ISeries s : getSeriesSet().getSeries())
                getSeriesSet().deleteSeries(s.getId());
        }

        finally
        {
            adjustRange();
            suspendUpdate(false);
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
            suspendUpdate(true);

            for (ISeries s : getSeriesSet().getSeries())
                getSeriesSet().deleteSeries(s.getId());

            int days = (int) ChronoUnit.DAYS.between(index.getDates()[0],
                            index.getDates()[index.getDates().length - 1]);

            // Disable SWT antialias for more than 1000 records due
            // to SWT performance issue in Drawing
            var swtAntialias = days > 1000 ? SWT.OFF : SWT.ON;

            // reverse the order

            ILineSeries lineSeries = addDateSeries(portfolio.getUUID(), index.getDates(),
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
            adjustRange();
            suspendUpdate(false);
        }
    }

    public void addButtons(ToolBarManager toolBar)
    {
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

    private void addAbsoluteInvestedCapital(PerformanceIndex index, int swtAntialias)
    {
        double[] values = toDouble(index.calculateAbsoluteInvestedCapital(), Values.Amount.divider());
        String lineID = Messages.LabelAbsoluteInvestedCapital;

        ILineSeries lineSeries = addDateSeries(lineID, index.getDates(), values, colorAbsoluteInvestedCapital, lineID);
        lineSeries.enableArea(true);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addAbsoluteDeltaAllRecords(PerformanceIndex index, int swtAntialias)
    {
        double[] values = toDouble(index.calculateAbsoluteDelta(), Values.Amount.divider());
        String lineID = Messages.LabelAbsoluteDelta;

        ILineSeries lineSeries = addDateSeries(lineID, index.getDates(), values, colorAbsoluteDelta, lineID);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addTaxes(PerformanceIndex index, int swtAntialias)
    {
        double[] values = accumulateAndToDouble(index.getTaxes(), Values.Amount.divider());
        String lineID = Messages.LabelAccumulatedTaxes;

        ILineSeries lineSeries = addDateSeries(lineID, index.getDates(), values, colorTaxesAccumulated, lineID);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addFees(PerformanceIndex index, int swtAntialias)
    {
        double[] values = accumulateAndToDouble(index.getFees(), Values.Amount.divider());
        String lineID = Messages.LabelFeesAccumulated;

        ILineSeries lineSeries = addDateSeries(lineID, index.getDates(), values, colorFeesAccumulated, lineID);
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
}
