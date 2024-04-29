package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
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

    private int swtAntialias = SWT.ON;
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
        redraw();
    }

    private final void readChartConfig(Client client)
    {
        String pref = ReadOnlyClient.unwrap(client).getProperty(PREF_KEY);
        if (pref == null)
            return;

        chartConfig.clear();
        for (String key : pref.split(",")) //$NON-NLS-1$
        {
            chartConfig.add(ChartDetails.valueOf(key));
        }
    }

    public void updateChart(Portfolio portfolio, ExchangeRateProviderFactory exchangeRateProviderFactory)
    {
        this.portfolio = portfolio;
        this.exchangeRateProviderFactory = exchangeRateProviderFactory;
        getTitle().setText(portfolio.getName());
        updateChart();
    }

    public void updateChart()
    {
        try
        {
            suspendUpdate(true);

            for (ISeries s : getSeriesSet().getSeries())
                getSeriesSet().deleteSeries(s.getId());

            if (portfolio == null)
                return;

            List<PortfolioTransaction> tx = portfolio.getTransactions();

            if (tx.isEmpty())
                return;

            Collections.sort(tx, Transaction.BY_DATE);

            LocalDate now = LocalDate.now();
            LocalDate start = tx.get(0).getDateTime().toLocalDate();
            LocalDate end = tx.get(tx.size() - 1).getDateTime().toLocalDate();

            CurrencyConverter converter = new CurrencyConverterImpl(exchangeRateProviderFactory,
                            client.getBaseCurrency());

            if (now.isAfter(end))
                end = now;
            if (now.isBefore(start))
                start = now;

            int days = (int) ChronoUnit.DAYS.between(start, end) + 2;

            // Disable SWT antialias for more than 1000 records due to SWT
            // performance issue in Drawing
            swtAntialias = days > 1000 ? SWT.OFF : SWT.ON;

            LocalDate[] dates = new LocalDate[days];
            double[] values = new double[days];

            dates[0] = start.minusDays(1);
            values[0] = 0d;

            for (int ii = 1; ii < dates.length; ii++)
            {
                values[ii] = PortfolioSnapshot.create(portfolio, converter, start) //
                                .getValue().getAmount() / Values.Amount.divider();
                dates[ii] = start;
                start = start.plusDays(1);
            }

            ILineSeries lineSeries = addDateSeries(portfolio.getUUID(), dates, values, Colors.CASH,
                            portfolio.getName());
            lineSeries.setAntialias(swtAntialias);
            addChartCommon(dates, converter);
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

            updateChart();

        });

        action.setChecked(chartConfig.contains(detail));
        return action;
    }

    private void addChartCommon(LocalDate[] dates, CurrencyConverter converter)
    {
        if (chartConfig.contains(ChartDetails.ABSOLUTE_INVESTED_CAPITAL))
            addAbsoluteInvestedCapital(dates, converter);

        if (chartConfig.contains(ChartDetails.ABSOLUTE_DELTA))
            addAbsoluteDeltaAllRecords(dates, converter);

        if (chartConfig.contains(ChartDetails.TAXES_ACCUMULATED))
            addTaxes(dates, converter);

        if (chartConfig.contains(ChartDetails.FEES_ACCUMULATED))
            addFees(dates, converter);
    }

    private void addAbsoluteInvestedCapital(LocalDate[] dates, CurrencyConverter converter)
    {
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forPortfolio(client, converter, portfolio,
                        Interval.of(dates[0], dates[dates.length - 1]), warnings);
        double[] values;
        values = toDouble(index.calculateAbsoluteInvestedCapital(), Values.Amount.divider());
        String lineID = Messages.LabelAbsoluteInvestedCapital;

        ILineSeries lineSeries = addDateSeries(lineID, dates, values, colorAbsoluteInvestedCapital, lineID); // $NON-NLS-1$
        lineSeries.enableArea(true);
        lineSeries.setAntialias(swtAntialias);
    }

    private void addAbsoluteDeltaAllRecords(LocalDate[] dates, CurrencyConverter converter)
    {
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forPortfolio(client, converter, portfolio,
                        Interval.of(dates[0], dates[dates.length - 1]), warnings);
        double[] values;
        values = toDouble(index.calculateAbsoluteDelta(), Values.Amount.divider());
        String lineID = Messages.LabelAbsoluteDelta;

        ILineSeries lineSeries = addDateSeries(lineID, dates, values, colorAbsoluteDelta, lineID); // $NON-NLS-1$
        lineSeries.setAntialias(swtAntialias);
    }

    private void addTaxes(LocalDate[] dates, CurrencyConverter converter)
    {
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forPortfolio(client, converter, portfolio,
                        Interval.of(dates[0], dates[dates.length - 1]), warnings);
        double[] values;
        values = accumulateAndToDouble(index.getTaxes(), Values.Amount.divider());
        String lineID = Messages.LabelAccumulatedTaxes;

        ILineSeries lineSeries = addDateSeries(lineID, dates, values, colorTaxesAccumulated, lineID); // $NON-NLS-1$
        lineSeries.setAntialias(swtAntialias);
    }

    private void addFees(LocalDate[] dates, CurrencyConverter converter)
    {
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forPortfolio(client, converter, portfolio,
                        Interval.of(dates[0], dates[dates.length - 1]), warnings);
        double[] values;
        values = accumulateAndToDouble(index.getFees(), Values.Amount.divider());
        String lineID = Messages.LabelFeesAccumulated;

        ILineSeries lineSeries = addDateSeries(lineID, dates, values, colorFeesAccumulated, lineID); // $NON-NLS-1$
        lineSeries.setAntialias(swtAntialias);
    }

    private double[] toDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = input[ii] / divider;
        return answer;
    }

    private double[] accumulateAndToDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        long current = 0;
        for (int ii = 0; ii < answer.length; ii++)
        {
            current += input[ii];
            answer[ii] = current / divider;
        }
        return answer;
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
