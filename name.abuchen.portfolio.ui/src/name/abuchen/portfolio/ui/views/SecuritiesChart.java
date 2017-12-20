package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.swtchart.IAxis;
import org.swtchart.ILegend;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.util.Interval;

/**
 * Chart of historical quotes for a given security
 */
public class SecuritiesChart
{
    private enum ChartDetails
    {
        SHOW_MARKER_LINES(Messages.LabelChartDetailShowMarkerLines), //
        SHOW_DATA_LABELS(Messages.LabelChartDetailShowDataLabel + SEPERATOR), //
        CLOSING(Messages.LabelChartDetailClosingIndicator), //
        PURCHASEPRICE(Messages.LabelChartDetailPurchaseIndicator + SEPERATOR), //
        INVESTMENT(Messages.LabelChartDetailInvestments), //
        DIVIDENDS(Messages.LabelChartDetailDividends), //
        EVENTS(Messages.LabelChartDetailEvents), //
        FIFOPURCHASE(Messages.LabelChartDetailFIFOpurchase + SEPERATOR), //
        SMA50(Messages.LabelChartDetailSMA50), //
        SMA200(Messages.LabelChartDetailSMA200), //
        BOLLINGERBANDS(Messages.LabelChartDetailBollingerBands);

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

    private Color colorQuote = Colors.getColor(52, 70, 235);

    private Color colorEventPurchase = Colors.getColor(26, 173, 33);
    private Color colorEventSale = Colors.getColor(232, 51, 69);
    private Color colorEventDividend = Colors.getColor(128, 0, 128);

    private Color colorFifoPurchasePrice = Colors.getColor(226, 122, 121);
    private Color colorBollingerBands = Colors.getColor(201, 141, 68);
    private Color colorSMA50 = Colors.getColor(102, 171, 29);
    private Color colorSMA200 = Colors.getColor(96, 104, 110);

    private Color colorAreaPositive = Colors.getColor(90, 114, 226);
    private Color colorAreaNegative = Colors.getColor(226, 91, 90);

    private static final String SEPERATOR = "---"; //$NON-NLS-1$
    private static final String PREF_KEY = "security-chart-details"; //$NON-NLS-1$

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d LLL"); //$NON-NLS-1$

    private Menu contextMenu;

    private Client client;
    private CurrencyConverter converter;
    private Security security;

    private TimelineChart chart;
    private LocalDate chartPeriod = LocalDate.now().minusYears(2);
    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.INVESTMENT, ChartDetails.EVENTS);

    private List<PaintListener> customPaintListeners = new ArrayList<>();
    private List<Transaction> customTooltipEvents = new ArrayList<>();

    public SecuritiesChart(Composite parent, Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;

        readChartConfig(client);

        chart = new TimelineChart(parent);
        chart.getTitle().setText("..."); //$NON-NLS-1$

        chart.getPlotArea().addPaintListener(event -> customPaintListeners.forEach(l -> l.paintControl(event)));

        setupTooltip();

        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

        ILegend legend = chart.getLegend();
        legend.setPosition(SWT.BOTTOM);
        legend.setVisible(true);

        Composite buttons = new Composite(parent, SWT.NONE);
        buttons.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridDataFactory.fillDefaults().grab(false, true).applyTo(buttons);
        RowLayoutFactory.fillDefaults().type(SWT.VERTICAL).spacing(2).fill(true).applyTo(buttons);

        addConfigButton(buttons);

        addButton(buttons, Messages.SecurityTabChart1M, Period.ofMonths(1));
        addButton(buttons, Messages.SecurityTabChart2M, Period.ofMonths(2));
        addButton(buttons, Messages.SecurityTabChart6M, Period.ofMonths(6));
        addButton(buttons, Messages.SecurityTabChart1Y, Period.ofYears(1));
        addButton(buttons, Messages.SecurityTabChart2Y, Period.ofYears(2));
        addButton(buttons, Messages.SecurityTabChart3Y, Period.ofYears(3));
        addButton(buttons, Messages.SecurityTabChart5Y, Period.ofYears(5));
        addButton(buttons, Messages.SecurityTabChart10Y, Period.ofYears(10));
        addButton(buttons, Messages.SecurityTabChartYTD, Period.ofDays(LocalDate.now().getDayOfYear() - 1));

        Button button = new Button(buttons, SWT.FLAT);
        button.setText(Messages.SecurityTabChartAll);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                chartPeriod = null;
                updateChart();
            }
        });
    }

    private void setupTooltip()
    {
        TimelineChartToolTip toolTip = chart.getToolTip();

        toolTip.setValueFormat(new DecimalFormat(Values.Quote.pattern()));
        toolTip.addSeriesExclude(Messages.LabelChartDetailClosingIndicator + "Positive"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailClosingIndicator + "Negative"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailClosingIndicator + "Zero"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuBuy);
        toolTip.addSeriesExclude(Messages.SecurityMenuBuy + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuBuy + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuSell);
        toolTip.addSeriesExclude(Messages.SecurityMenuSell + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuSell + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailDividends);
        toolTip.addSeriesExclude(Messages.LabelChartDetailDividends + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailDividends + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailBollingerBands);

        toolTip.addExtraInfo((composite, focus) -> {
            if (focus instanceof Date)
            {
                Instant instant = ((Date) focus).toInstant();
                ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
                LocalDate date = zdt.toLocalDate();

                Interval displayInterval = Interval.of(date.minusDays(5), date.plusDays(5));

                customTooltipEvents.stream() //
                                .filter(t -> displayInterval.contains(t.getDate())) //
                                .forEach(t -> {
                                    if (t instanceof AccountTransaction)
                                        addDividendTooltip(composite, (AccountTransaction) t);
                                    else if (t instanceof PortfolioTransaction)
                                        addInvestmentTooltip(composite, (PortfolioTransaction) t);
                                });
            }
        });
    }

    private void addInvestmentTooltip(Composite composite, PortfolioTransaction t)
    {
        Label label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipTransactionSummary, t.getType().toString(),
                        dateTimeFormatter.format(t.getDate()), t.getMonetaryAmount().toString()));

        label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipInvestmentDetails, Values.Share.format(t.getShares()),
                        Values.Quote.format(
                                        t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())))));
    }

    private void addDividendTooltip(Composite composite, AccountTransaction t)
    {
        Label label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipTransactionSummary, t.getType().toString(),
                        dateTimeFormatter.format(t.getDate()), t.getMonetaryAmount().toString()));

        if (t.getShares() == 0L)
        {
            label = new Label(composite, SWT.NONE);
            label.setText("\u2211 " + t.getGrossValue().toString()); //$NON-NLS-1$
        }
        else
        {
            Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
            long gross = grossValue.isPresent() ? grossValue.get().getForex().getAmount() : t.getGrossValueAmount();

            label = new Label(composite, SWT.NONE);
            label.setText(MessageFormat.format(Messages.LabelToolTipDividendDetails, Values.Share.format(t.getShares()),
                            Values.Quote.format(Math.round(gross * Values.Share.divider() * Values.Quote.factorToMoney()
                                            / t.getShares()))));
        }
    }

    private void configureSeriesPainter(ILineSeries series, Date[] dates, double[] values, Color color, int lineWidth,
                    LineStyle lineStyle, boolean enableArea, boolean visibleInLegend)
    {
        if (lineWidth != 0)
            series.setLineWidth(lineWidth);
        series.setLineStyle(lineStyle);
        series.setXDateSeries(dates);
        series.enableArea(enableArea);
        series.setYSeries(values);
        series.setAntialias(SWT.ON);

        if (color != null)
            series.setLineColor(color);
        series.setVisibleInLegend(visibleInLegend);
    }

    private final void readChartConfig(Client client)
    {
        String pref = client.getProperty(PREF_KEY);
        if (pref == null)
            return;

        chartConfig.clear();
        for (String key : pref.split(",")) //$NON-NLS-1$
        {
            try
            {
                chartConfig.add(ChartDetails.valueOf(key));
            }
            catch (IllegalArgumentException e)
            {
                PortfolioPlugin.log(e);
            }
        }
    }

    private void addConfigButton(Composite buttons)
    {
        Button b = new Button(buttons, SWT.FLAT);
        b.setImage(Images.CONFIG.image());
        b.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (contextMenu == null)
                {
                    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
                    menuMgr.setRemoveAllWhenShown(true);
                    menuMgr.addMenuListener(SecuritiesChart.this::chartConfigAboutToShow);

                    contextMenu = menuMgr.createContextMenu(buttons.getShell());

                    buttons.addDisposeListener(event -> contextMenu.dispose());
                }

                contextMenu.setVisible(true);
            }
        });
    }

    private void chartConfigAboutToShow(IMenuManager manager)
    {
        for (ChartDetails detail : ChartDetails.values())
        {
            String buttonDescription = detail.toString().replaceAll(SEPERATOR, ""); //$NON-NLS-1$

            Action action = new SimpleAction(buttonDescription, a -> {
                boolean isActive = chartConfig.contains(detail);

                if (isActive)
                    chartConfig.remove(detail);
                else
                    chartConfig.add(detail);

                client.setProperty(PREF_KEY, String.join(",", //$NON-NLS-1$
                                chartConfig.stream().map(ChartDetails::name).collect(Collectors.toList())));

                updateChart();
            });

            action.setChecked(chartConfig.contains(detail));
            manager.add(action);
            if (detail.toString().endsWith(SEPERATOR))
                manager.add(new Separator());
        }
    }

    private void addButton(Composite buttons, String label, TemporalAmount amountToAdd)
    {
        Button b = new Button(buttons, SWT.FLAT);
        b.setText(label);
        b.addSelectionListener(new ChartPeriodSelectionListener()
        {
            @Override
            protected LocalDate startAt()
            {
                return LocalDate.now().minus(amountToAdd);
            }
        });
    }

    /**
     * ChartPeriodSelectionListener handles the selection of the time which
     * should be displayed in the chart
     */
    private abstract class ChartPeriodSelectionListener implements SelectionListener
    {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
            chartPeriod = startAt();
            updateChart();
        }

        protected abstract LocalDate startAt();

        @Override
        public void widgetDefaultSelected(SelectionEvent e)
        {
            // not used
        }
    }

    public void updateChart(Security security)
    {
        this.security = security;
        updateChart();
    }

    private void updateChart()
    {
        chart.setRedraw(false);

        try
        {
            // delete all line series (quotes + possibly moving average)
            ISeries[] series = chart.getSeriesSet().getSeries();
            for (ISeries s : series)
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.clearMarkerLines();
            customPaintListeners.clear();
            customTooltipEvents.clear();

            if (security == null || security.getPrices().isEmpty())
            {
                chart.getTitle().setText(security == null ? "..." : security.getName()); //$NON-NLS-1$
                chart.redraw();
                return;
            }

            chart.getTitle().setText(security.getName());

            boolean showAreaRelativeToFirstQuote = chartConfig.contains(ChartDetails.CLOSING)
                            || chartConfig.contains(ChartDetails.PURCHASEPRICE);

            List<SecurityPrice> prices = security.getPricesIncludingLatest();

            int index;
            LocalDate[] dates;

            double[] values;
            double[] valuesRelative;
            double[] valuesRelativePositive;
            double[] valuesRelativeNegative;
            double[] valuesZeroLine;
            double firstQuote = 0;

            if (chartPeriod == null)
            {
                index = 0;
                dates = new LocalDate[prices.size()];
                values = new double[prices.size()];
                valuesRelative = new double[prices.size()];
                valuesRelativePositive = new double[prices.size()];
                valuesRelativeNegative = new double[prices.size()];
                valuesZeroLine = new double[prices.size()];
            }
            else
            {
                index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(chartPeriod, 0),
                                new SecurityPrice.ByDate()));

                if (index >= prices.size())
                {
                    // no data available
                    chart.redraw();
                    return;
                }

                dates = new LocalDate[prices.size() - index];
                values = new double[prices.size() - index];
                valuesRelative = new double[prices.size() - index];
                valuesRelativePositive = new double[prices.size() - index];
                valuesRelativeNegative = new double[prices.size() - index];
                valuesZeroLine = new double[prices.size() - index];
            }

            SecurityPrice p2 = prices.get(index);

            if (!chartConfig.contains(ChartDetails.PURCHASEPRICE))
            {
                firstQuote = (p2.getValue() / Values.Quote.divider());
            }
            else
            {
                Optional<Double> purchasePrice = getLatestPurchasePrice();

                if (purchasePrice.isPresent())
                    firstQuote = purchasePrice.get();
                else
                    showAreaRelativeToFirstQuote = false;
            }

            addChartMarkerBackground();

            for (int ii = 0; index < prices.size(); index++, ii++)
            {
                SecurityPrice p = prices.get(index);
                dates[ii] = p.getDate();
                values[ii] = p.getValue() / Values.Quote.divider();
                if (showAreaRelativeToFirstQuote)
                {
                    valuesRelative[ii] = (p.getValue() / Values.Quote.divider()) - firstQuote;
                    valuesZeroLine[ii] = 0;
                    if (valuesRelative[ii] >= 0)
                    {
                        valuesRelativePositive[ii] = valuesRelative[ii];
                        valuesRelativeNegative[ii] = 0;
                    }
                    else
                    {
                        valuesRelativePositive[ii] = 0;
                        valuesRelativeNegative[ii] = valuesRelative[ii];
                    }
                }
            }

            Date[] javaDates = TimelineChart.toJavaUtilDate(dates);

            if (showAreaRelativeToFirstQuote)
            {

                ILineSeries lineSeries2ndNegative = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailClosingIndicator + "Negative"); //$NON-NLS-1$
                lineSeries2ndNegative.setSymbolType(PlotSymbolType.NONE);
                lineSeries2ndNegative.setYAxisId(1);
                configureSeriesPainter(lineSeries2ndNegative, javaDates, valuesRelativeNegative, colorAreaNegative, 1,
                                LineStyle.SOLID, true, false);

                ILineSeries lineSeries2ndPositive = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailClosingIndicator + "Positive"); //$NON-NLS-1$
                lineSeries2ndPositive.setSymbolType(PlotSymbolType.NONE);
                lineSeries2ndPositive.setYAxisId(1);
                configureSeriesPainter(lineSeries2ndPositive, javaDates, valuesRelativePositive, colorAreaPositive, 1,
                                LineStyle.SOLID, true, false);
            }

            ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.ColumnQuote);
            lineSeries.setSymbolType(PlotSymbolType.NONE);
            configureSeriesPainter(lineSeries, javaDates, values, colorQuote, 2, LineStyle.SOLID,
                            !showAreaRelativeToFirstQuote, false);

            chart.adjustRange();

            addChartMarkerForeground();

            chart.adjustRange();

            IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
            IAxis yAxis2nd = chart.getAxisSet().getYAxis(1);
            yAxis2nd.setRange(
                            new Range(yAxis1st.getRange().lower - firstQuote, yAxis1st.getRange().upper - firstQuote));

        }
        finally
        {
            chart.setRedraw(true);
            chart.redraw();
        }
    }

    private void addChartMarkerBackground()
    {
        if (chartConfig.contains(ChartDetails.BOLLINGERBANDS))
            addBollingerBandsMarkerLines(20, 2);

        if (chartConfig.contains(ChartDetails.SMA50))
            addSMAMarkerLines(50);

        if (chartConfig.contains(ChartDetails.SMA200))
            addSMAMarkerLines(200);
    }

    private void addChartMarkerForeground()
    {
        if (chartConfig.contains(ChartDetails.FIFOPURCHASE))
            addFIFOPurchasePrice();

        if (chartConfig.contains(ChartDetails.INVESTMENT))
            addInvestmentMarkerLines();

        if (chartConfig.contains(ChartDetails.DIVIDENDS))
            addDividendMarkerLines();

        if (chartConfig.contains(ChartDetails.EVENTS))
            addEventMarkerLines();
    }

    private void addSMAMarkerLines(int smaDays)
    {
        ChartLineSeriesAxes smaLines = new SimpleMovingAverage(smaDays, this.security, chartPeriod).getSMA();
        if (smaLines == null || smaLines.getValues() == null || smaLines.getDates() == null)
            return;

        String lineID = smaDays == 200 ? Messages.LabelChartDetailSMA200 : Messages.LabelChartDetailSMA50;

        ILineSeries lineSeriesSMA = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, lineID);
        lineSeriesSMA.setXDateSeries(smaLines.getDates());
        lineSeriesSMA.setLineWidth(2);
        lineSeriesSMA.enableArea(false);
        lineSeriesSMA.setSymbolType(PlotSymbolType.NONE);
        lineSeriesSMA.setYSeries(smaLines.getValues());
        lineSeriesSMA.setAntialias(SWT.ON);
        lineSeriesSMA.setLineColor(smaDays == 200 ? colorSMA200 : colorSMA50);
        lineSeriesSMA.setYAxisId(0);
        lineSeriesSMA.setVisibleInLegend(true);
    }

    private void addInvestmentMarkerLines()
    {
        List<PortfolioTransaction> purchase = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .filter(t -> chartPeriod == null || chartPeriod.isBefore(t.getDate()))
                        .sorted(new Transaction.ByDate()).collect(Collectors.toList());

        addInvestmentMarkers(purchase, Messages.SecurityMenuBuy, colorEventPurchase);

        List<PortfolioTransaction> sales = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.SELL
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        .filter(t -> chartPeriod == null || chartPeriod.isBefore(t.getDate()))
                        .sorted(new Transaction.ByDate()).collect(Collectors.toList());

        addInvestmentMarkers(sales, Messages.SecurityMenuSell, colorEventSale);
    }

    private void addInvestmentMarkers(List<PortfolioTransaction> transactions, String seriesLabel, Color color)
    {
        if (transactions.isEmpty())
            return;

        customTooltipEvents.addAll(transactions);

        if (chartConfig.contains(ChartDetails.SHOW_MARKER_LINES))
        {
            transactions.forEach(t -> {
                String label = Values.Share.format(t.getType().isPurchase() ? t.getShares() : -t.getShares());
                double value = t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())).getAmount()
                                / Values.Quote.divider();
                chart.addMarkerLine(t.getDate(), color, label, value);
            });
        }
        else
        {
            Date[] dates = transactions.stream().map(PortfolioTransaction::getDate)
                            .map(d -> Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                            .collect(Collectors.toList()).toArray(new Date[0]);

            double[] values = transactions.stream().mapToDouble(
                            t -> t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())).getAmount()
                                            / Values.Quote.divider())
                            .toArray();

            ILineSeries border = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesLabel + "2"); //$NON-NLS-1$
            border.setYAxisId(0);
            border.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            border.setSymbolType(PlotSymbolType.DIAMOND);
            border.setSymbolSize(7);

            configureSeriesPainter(border, dates, values, null, 0, LineStyle.NONE, false, false);

            ILineSeries background = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            seriesLabel + "1"); //$NON-NLS-1$
            background.setYAxisId(0);
            background.setSymbolType(PlotSymbolType.DIAMOND);
            background.setSymbolSize(6);
            background.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            configureSeriesPainter(background, dates, values, null, 0, LineStyle.NONE, false, false);

            ILineSeries inner = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, seriesLabel);
            inner.setYAxisId(0);
            inner.setSymbolType(PlotSymbolType.DIAMOND);
            inner.setSymbolSize(4);
            inner.setSymbolColor(color);
            configureSeriesPainter(inner, dates, values, color, 0, LineStyle.NONE, false, true);

            if (chartConfig.contains(ChartDetails.SHOW_DATA_LABELS))
            {
                customPaintListeners.add(event -> {
                    IAxis xAxis = chart.getAxisSet().getXAxis(0);
                    IAxis yAxis = chart.getAxisSet().getYAxis(0);

                    for (int index = 0; index < dates.length; index++)
                    {
                        int x = xAxis.getPixelCoordinate(dates[index].getTime());
                        int y = yAxis.getPixelCoordinate(values[index]);

                        PortfolioTransaction t = transactions.get(index);
                        String label = Values.Share.format(t.getType().isPurchase() ? t.getShares() : -t.getShares());
                        Point textExtent = event.gc.textExtent(label);

                        event.gc.setForeground(Colors.BLACK);
                        event.gc.drawText(label, x - (textExtent.x / 2), y + 10, true);
                    }
                });
            }
        }
    }

    private void addDividendMarkerLines()
    {
        List<AccountTransaction> dividends = client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .filter(t -> chartPeriod == null || chartPeriod.isBefore(t.getDate()))
                        .sorted(new Transaction.ByDate()).collect(Collectors.toList());

        if (dividends.isEmpty())
            return;

        customTooltipEvents.addAll(dividends);

        if (chartConfig.contains(ChartDetails.SHOW_MARKER_LINES))
        {
            dividends.forEach(t -> chart.addMarkerLine(t.getDate(), colorEventDividend, getDividendLabel(t)));
        }
        else
        {
            Date[] dates = dividends.stream().map(AccountTransaction::getDate)
                            .map(d -> Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                            .collect(Collectors.toList()).toArray(new Date[0]);

            IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
            double yAxis1stAxisPrice = yAxis1st.getRange().lower;

            double[] values = new double[dates.length];
            Arrays.fill(values, yAxis1stAxisPrice);

            ILineSeries border = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailDividends + "2"); //$NON-NLS-1$
            border.setYAxisId(0);
            border.setSymbolType(PlotSymbolType.SQUARE);
            border.setSymbolSize(6);
            border.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            configureSeriesPainter(border, dates, values, null, 0, LineStyle.NONE, false, false);

            ILineSeries background = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailDividends + "1"); //$NON-NLS-1$
            background.setYAxisId(0);
            background.setSymbolType(PlotSymbolType.SQUARE);
            background.setSymbolSize(5);
            background.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            configureSeriesPainter(background, dates, values, null, 0, LineStyle.NONE, false, false);

            ILineSeries inner = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailDividends);
            inner.setYAxisId(0);
            inner.setSymbolType(PlotSymbolType.SQUARE);
            inner.setSymbolSize(3);
            inner.setSymbolColor(colorEventDividend);
            configureSeriesPainter(inner, dates, values, null, 0, LineStyle.NONE, false, true);

            if (chartConfig.contains(ChartDetails.SHOW_DATA_LABELS))
            {
                customPaintListeners.add(event -> {
                    IAxis xAxis = chart.getAxisSet().getXAxis(0);
                    IAxis yAxis = chart.getAxisSet().getYAxis(0);

                    for (int index = 0; index < dates.length; index++)
                    {
                        int x = xAxis.getPixelCoordinate(dates[index].getTime());
                        int y = yAxis.getPixelCoordinate(values[index]);

                        String label = getDividendLabel(dividends.get(index));
                        Point textExtent = event.gc.textExtent(label);

                        event.gc.setForeground(Colors.BLACK);
                        event.gc.drawText(label, x - (textExtent.x / 2), y - 22, true);
                    }
                });
            }
        }
    }

    private String getDividendLabel(AccountTransaction t)
    {
        if (t.getShares() == 0L)
        {
            return "\u2211 " + t.getGrossValue().toString(); //$NON-NLS-1$
        }
        else
        {
            Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
            long gross = grossValue.isPresent() ? grossValue.get().getForex().getAmount() : t.getGrossValueAmount();

            long perShare = Math.round(gross * Values.Share.divider() * Values.Quote.factorToMoney() / t.getShares());

            return Values.Quote.format(perShare);
        }
    }

    private void addEventMarkerLines()
    {
        security.getEvents().stream() //
                        .filter(e -> chartPeriod == null || chartPeriod.isBefore(e.getDate())) //
                        .forEach(e -> chart.addMarkerLine(e.getDate(),
                                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY), e.getDetails()));
    }

    private void addBollingerBandsMarkerLines(int bollingerBandsDays, double bollingerBandsFactor)
    {
        ChartLineSeriesAxes bollingerBandsLowerBand = new BollingerBands(bollingerBandsDays, bollingerBandsFactor,
                        this.security, chartPeriod).getLowerBands();
        if (bollingerBandsLowerBand == null || bollingerBandsLowerBand.getValues() == null
                        || bollingerBandsLowerBand.getDates() == null)
            return;

        ILineSeries lineSeriesBollingerBandsLowerBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailBollingerBandsLower);
        lineSeriesBollingerBandsLowerBand.setXDateSeries(bollingerBandsLowerBand.getDates());
        lineSeriesBollingerBandsLowerBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsLowerBand.setLineWidth(2);
        lineSeriesBollingerBandsLowerBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsLowerBand.setYSeries(bollingerBandsLowerBand.getValues());
        lineSeriesBollingerBandsLowerBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsLowerBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsLowerBand.setYAxisId(0);
        lineSeriesBollingerBandsLowerBand.setVisibleInLegend(false);

        ChartLineSeriesAxes bollingerBandsMiddleBand = new BollingerBands(bollingerBandsDays, bollingerBandsFactor,
                        this.security, chartPeriod).getMiddleBands();
        ILineSeries lineSeriesBollingerBandsMiddleBand = (ILineSeries) chart.getSeriesSet()
                        .createSeries(SeriesType.LINE, Messages.LabelChartDetailBollingerBands);
        lineSeriesBollingerBandsMiddleBand.setXDateSeries(bollingerBandsMiddleBand.getDates());
        lineSeriesBollingerBandsMiddleBand.setLineWidth(2);
        lineSeriesBollingerBandsMiddleBand.setLineStyle(LineStyle.DOT);
        lineSeriesBollingerBandsMiddleBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsMiddleBand.setYSeries(bollingerBandsMiddleBand.getValues());
        lineSeriesBollingerBandsMiddleBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsMiddleBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsMiddleBand.setYAxisId(0);
        lineSeriesBollingerBandsMiddleBand.setVisibleInLegend(true);

        ChartLineSeriesAxes bollingerBandsUpperBand = new BollingerBands(bollingerBandsDays, bollingerBandsFactor,
                        this.security, chartPeriod).getUpperBands();
        ILineSeries lineSeriesBollingerBandsUpperBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailBollingerBandsUpper);
        lineSeriesBollingerBandsUpperBand.setXDateSeries(bollingerBandsUpperBand.getDates());
        lineSeriesBollingerBandsUpperBand.setLineWidth(2);
        lineSeriesBollingerBandsUpperBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsUpperBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsUpperBand.setYSeries(bollingerBandsUpperBand.getValues());
        lineSeriesBollingerBandsUpperBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsUpperBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsUpperBand.setYAxisId(0);
        lineSeriesBollingerBandsUpperBand.setVisibleInLegend(false);
    }

    private void addFIFOPurchasePrice()
    {
        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security.getCurrencyCode() == null)
            return;

        // create a list of dates that are relevant for FIFO purchase price
        // changes (i.e. all purchase and sell events)

        Client filteredClient = new ClientSecurityFilter(security).filter(client);
        CurrencyConverter securityCurrency = converter.with(security.getCurrencyCode());

        LocalDate today = LocalDate.now();

        List<LocalDate> candidates = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getSecurity().equals(security))
                        .filter(t -> !(t.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                        || t.getType() == PortfolioTransaction.Type.TRANSFER_OUT))
                        .filter(t -> t.getDate().isBefore(today))
                        .map(t -> (chartPeriod == null || t.getDate().isAfter(chartPeriod)) ? t.getDate() : chartPeriod)
                        .distinct() //
                        .sorted() //
                        .collect(Collectors.toList());

        // calculate FIFO purchase price for each event - separate lineSeries
        // per holding period

        List<Double> values = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        int seriesCounter = 0;

        for (LocalDate eventDate : candidates)
        {
            Optional<Double> purchasePrice = getPurchasePrice(filteredClient, securityCurrency, eventDate);

            if (purchasePrice.isPresent())
            {
                dates.add(eventDate);
                values.add(purchasePrice.get());
            }
            else
            {
                if (!dates.isEmpty())
                {
                    // add previous value if the data series ends here (no more
                    // future events)

                    dates.add(eventDate);
                    values.add(values.get(values.size() - 1));

                    createFIFOPurchaseLineSeries(values, dates, seriesCounter++);

                    values.clear();
                    dates.clear();
                }
                else if (dates.isEmpty())
                {
                    // if no holding period exists, then do not add the event at
                    // all
                }
            }
        }

        // add today if needed

        getPurchasePrice(filteredClient, securityCurrency, today).ifPresent(price -> {
            dates.add(today);
            values.add(price);
        });

        if (!dates.isEmpty())
            createFIFOPurchaseLineSeries(values, dates, seriesCounter);
    }

    private void createFIFOPurchaseLineSeries(List<Double> values, List<LocalDate> dates, int seriesCounter)
    {
        String label = seriesCounter == 0 ? Messages.LabelChartDetailFIFOpurchase
                        : MessageFormat.format(Messages.LabelChartDetailFIFOpurchaseHoldingPeriod, seriesCounter + 1);

        ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, label);

        series.setSymbolType(PlotSymbolType.NONE);
        series.setYAxisId(0);
        series.enableStep(true);

        configureSeriesPainter(series, TimelineChart.toJavaUtilDate(dates.toArray(new LocalDate[0])),
                        ArrayUtils.toPrimitive(values.toArray(new Double[0])), colorFifoPurchasePrice, 2,
                        LineStyle.SOLID, false, seriesCounter == 0);
    }

    private Optional<Double> getLatestPurchasePrice()
    {
        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security.getCurrencyCode() == null)
            return Optional.empty();

        return getPurchasePrice(new ClientSecurityFilter(security).filter(client),
                        converter.with(security.getCurrencyCode()), LocalDate.now());
    }

    private Optional<Double> getPurchasePrice(Client filteredClient, CurrencyConverter currencyConverter,
                    LocalDate date)
    {
        ClientSnapshot snapshot = ClientSnapshot.createEndOfDay(filteredClient, currencyConverter, date);
        AssetPosition position = snapshot.getPositionsByVehicle().get(security);
        if (position == null)
            return Optional.empty();

        Money purchasePrice = position.getPosition().getFIFOPurchasePrice();
        if (!purchasePrice.isZero())
            return Optional.of(purchasePrice.getAmount() / Values.Amount.divider());
        else
            return Optional.empty();
    }
}
