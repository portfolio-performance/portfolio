package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.swtchart.IAxis;
import org.swtchart.ILegend;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;

import com.google.common.primitives.Doubles;
import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

/**
 * Chart of historical quotes for a given security
 */
public class SecuritiesChart
{
    /**
     * A <em>closed</em> interval that includes start and end date.
     * <p/>
     * We create a separate {@code ChartInterval} class because - for historical
     * reasons - the {@link Interval} is half-open, i.e. it does not include the
     * start date. When working with charts, however, it is easier to work with
     * a closed interval that includes both start and end date.
     */
    public static class ChartInterval
    {
        private final LocalDate start;
        private final LocalDate end;

        public ChartInterval(LocalDate start, LocalDate end)
        {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart()
        {
            return start;
        }

        public LocalDate getEnd()
        {
            return end;
        }

        public boolean contains(LocalDate other)
        {
            return !other.isBefore(start) && !other.isAfter(end);
        }

        public boolean contains(LocalDateTime other)
        {
            return contains(other.toLocalDate());
        }
    }

    private enum ChartDetails
    {
        SCALING_LINEAR(Messages.LabelChartDetailChartScalingLinear), //
        SCALING_LOG(Messages.LabelChartDetailChartScalingLog), //
        CLOSING(Messages.LabelChartDetailChartDevelopmentClosing), //
        PURCHASEPRICE(Messages.LabelChartDetailChartDevelopmentClosingFIFO), //
        INVESTMENT(Messages.LabelChartDetailMarkerInvestments), //
        DIVIDENDS(Messages.LabelChartDetailMarkerDividends), //
        EVENTS(Messages.LabelChartDetailMarkerSplits), //
        FIFOPURCHASE(Messages.LabelChartDetailMarkerPurchaseFIFO), //
        FLOATINGAVGPURCHASE(Messages.LabelChartDetailMarkerPurchaseMovingAverage), //
        BOLLINGERBANDS(Messages.LabelChartDetailIndicatorBollingerBands), //
        SMA_5DAYS(Messages.LabelChartDetailMovingAverage_5days), //
        SMA_20DAYS(Messages.LabelChartDetailMovingAverage_20days), //
        SMA_30DAYS(Messages.LabelChartDetailMovingAverage_30days), //
        SMA_38DAYS(Messages.LabelChartDetailMovingAverage_38days), //
        SMA_50DAYS(Messages.LabelChartDetailMovingAverage_50days), //
        SMA_90DAYS(Messages.LabelChartDetailMovingAverage_90days), //
        SMA_100DAYS(Messages.LabelChartDetailMovingAverage_100days), //
        SMA_200DAYS(Messages.LabelChartDetailMovingAverage_200days), //
        SHOW_MARKER_LINES(Messages.LabelChartDetailSettingsShowMarkerLines), //
        SHOW_DATA_LABELS(Messages.LabelChartDetailSettingsShowDataLabel), //
        SHOW_MISSING_TRADING_DAYS(Messages.LabelChartDetailSettingsShowMissingTradingDays);

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

    private static class CustomLayout extends Layout
    {
        private final TimelineChart chart;
        private final Composite buttons;

        public CustomLayout(TimelineChart chart, Composite buttons)
        {
            this.chart = chart;
            this.buttons = buttons;
        }

        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
        {
            return new Point(wHint, hHint);
        }

        @Override
        protected void layout(Composite composite, boolean flushCache)
        {
            Rectangle area = composite.getClientArea();

            Point size = buttons.computeSize(SWT.DEFAULT, area.height);

            buttons.setBounds(area.x + area.width - size.x, area.y, size.x, area.height);
            chart.setBounds(area.x, area.y, area.width - size.x, area.height);
        }
    }

    /* testing */ static class ChartRange
    {
        public final int start;
        public final int size;

        public ChartRange(int start, int end)
        {
            this.start = start;
            this.size = end - start;
        }

        /**
         * Maps the given {@link ChartInterval} to a range in the list of
         * security prices. Returns null if the interval does not intersect with
         * the list of prices.
         */
        public static ChartRange createFor(List<SecurityPrice> prices, ChartInterval chartInterval)
        {
            int start = Collections.binarySearch(prices, new SecurityPrice(chartInterval.getStart(), 0),
                            new SecurityPrice.ByDate());

            if (start < 0)
                start = -start - 1;

            if (start >= prices.size())
                return null;

            int end = Collections.binarySearch(prices, new SecurityPrice(chartInterval.getEnd(), 0),
                            new SecurityPrice.ByDate());

            if (end < 0)
                end = -end - 1;
            else
                end += 1; // include the entry that has been found

            if (end <= start)
                return null;

            return new ChartRange(start, end);
        }
    }

    private Color colorQuote = Colors.getColor(52, 70, 235);

    private Color colorEventPurchase = Colors.getColor(26, 173, 33);
    private Color colorEventSale = Colors.getColor(232, 51, 69);
    private Color colorEventDividend = Colors.getColor(128, 0, 128);

    private Color colorFifoPurchasePrice = Colors.getColor(226, 122, 121);
    private Color colorMovingAveragePurchasePrice = Colors.getColor(150, 82, 81);
    private Color colorBollingerBands = Colors.getColor(201, 141, 68);
    private Color colorSMA1 = Colors.getColor(179, 107, 107); // #B36B6B
    private Color colorSMA2 = Colors.getColor(179, 167, 107); // #B3A76B
    private Color colorSMA3 = Colors.getColor(131, 179, 107); // #83B36B
    private Color colorSMA4 = Colors.getColor(107, 179, 143); // #6BB38F
    private Color colorSMA5 = Colors.getColor(107, 155, 179); // #6B9BB3
    private Color colorSMA6 = Colors.getColor(119, 107, 179); // #776BB3
    private Color colorSMA7 = Colors.getColor(179, 107, 179); // #B36BB3

    private Color colorAreaPositive = Colors.getColor(90, 114, 226);
    private Color colorAreaNegative = Colors.getColor(226, 91, 90);

    private Color colorNonTradingDay = Colors.getColor(255, 137, 89);

    private static final String PREF_KEY = "security-chart-details"; //$NON-NLS-1$

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d LLL"); //$NON-NLS-1$

    private Menu contextMenu;

    private Client client;
    private CurrencyConverter converter;
    private Security security;

    private TimelineChart chart;

    /**
     * Calculates dynamically for each security the interval of security prices
     * to be shown.
     */
    private Function<Security, ChartInterval> chartIntervalFunction;

    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.INVESTMENT, ChartDetails.EVENTS,
                    ChartDetails.SCALING_LINEAR);

    private List<PaintListener> customPaintListeners = new ArrayList<>();
    private List<PaintListener> customBehindPaintListener = new ArrayList<>();
    private List<Transaction> customTooltipEvents = new ArrayList<>();

    private int swtAntialias = SWT.ON;

    public SecuritiesChart(Composite parent, Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;

        readChartConfig(client);

        chart = new TimelineChart(parent);
        chart.getTitle().setText("..."); //$NON-NLS-1$

        chart.getPlotArea().addPaintListener(event -> customPaintListeners.forEach(l -> l.paintControl(event)));
        chart.getPlotArea().addPaintListener(event -> customBehindPaintListener.forEach(l -> l.paintControl(event)));

        setupTooltip();

        ILegend legend = chart.getLegend();
        legend.setPosition(SWT.BOTTOM);
        legend.setVisible(true);

        Composite buttons = new Composite(parent, SWT.NONE);
        buttons.setBackground(Colors.WHITE);
        RowLayoutFactory.fillDefaults().type(SWT.VERTICAL).spacing(2).fill(true).wrap(true).applyTo(buttons);

        addConfigButton(buttons);

        Function<TemporalAmount, ChartInterval> nowMinus = temporalAmount -> {
            LocalDate now = LocalDate.now();
            return new ChartInterval(now.minus(temporalAmount), now);
        };

        addButton(buttons, Messages.SecurityTabChart1M, s -> nowMinus.apply(Period.ofMonths(1)));
        addButton(buttons, Messages.SecurityTabChart2M, s -> nowMinus.apply(Period.ofMonths(2)));
        addButton(buttons, Messages.SecurityTabChart6M, s -> nowMinus.apply(Period.ofMonths(6)));
        addButton(buttons, Messages.SecurityTabChart1Y, s -> nowMinus.apply(Period.ofYears(1)));
        addButton(buttons, Messages.SecurityTabChart2Y, s -> nowMinus.apply(Period.ofYears(2)));
        addButton(buttons, Messages.SecurityTabChart3Y, s -> nowMinus.apply(Period.ofYears(3)));
        addButton(buttons, Messages.SecurityTabChart5Y, s -> nowMinus.apply(Period.ofYears(5)));
        addButton(buttons, Messages.SecurityTabChart10Y, s -> nowMinus.apply(Period.ofYears(10)));
        addButton(buttons, Messages.SecurityTabChartYTD,
                        s -> nowMinus.apply(Period.ofDays(LocalDate.now().getDayOfYear() - 1)));

        addButton(buttons, Messages.SecurityTabChartHoldingPeriod, s -> {
            List<TransactionPair<?>> tx = s.getTransactions(client);
            if (tx.isEmpty())
                return null;

            Collections.sort(tx, new TransactionPair.ByDate());
            boolean hasHoldings = ClientSnapshot.create(client, converter, LocalDate.now()).getPositionsByVehicle()
                            .containsKey(s);

            return new ChartInterval(tx.get(0).getTransaction().getDateTime().toLocalDate(),
                            hasHoldings ? LocalDate.now()
                                            : tx.get(tx.size() - 1).getTransaction().getDateTime().toLocalDate());

        });

        addButton(buttons, Messages.SecurityTabChartAll, s -> {
            List<SecurityPrice> prices = s.getPricesIncludingLatest();
            return prices.isEmpty() ? null
                            : new ChartInterval(prices.get(0).getDate(), prices.get(prices.size() - 1).getDate());
        });

        parent.setLayout(new CustomLayout(chart, buttons));
    }

    private void setupTooltip()
    {
        TimelineChartToolTip toolTip = chart.getToolTip();

        toolTip.showToolTipOnlyForDatesInDataSeries(Messages.ColumnQuote);

        toolTip.setValueFormat(new DecimalFormat(Values.Quote.pattern()));
        toolTip.addSeriesExclude(Messages.LabelChartDetailChartDevelopment + "Positive"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailChartDevelopment + "Negative"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailChartDevelopment + "Zero"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuBuy);
        toolTip.addSeriesExclude(Messages.SecurityMenuBuy + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuBuy + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuSell);
        toolTip.addSeriesExclude(Messages.SecurityMenuSell + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.SecurityMenuSell + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailMarkerDividends);
        toolTip.addSeriesExclude(Messages.LabelChartDetailMarkerDividends + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailMarkerDividends + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailIndicatorBollingerBands);

        toolTip.addExtraInfo((composite, focus) -> {
            if (focus instanceof Date)
            {
                Instant instant = ((Date) focus).toInstant();
                ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
                LocalDate date = zdt.toLocalDate();

                Interval displayInterval = Interval.of(date.minusDays(5), date.plusDays(5));

                customTooltipEvents.stream() //
                                .filter(t -> displayInterval.contains(t.getDateTime())) //
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
                        dateTimeFormatter.format(t.getDateTime().toLocalDate()), t.getMonetaryAmount().toString()));

        label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipInvestmentDetails, Values.Share.format(t.getShares()),
                        Values.Quote.format(
                                        t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())))));
    }

    private void addDividendTooltip(Composite composite, AccountTransaction t)
    {
        Label label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipTransactionSummary, t.getType().toString(),
                        dateTimeFormatter.format(t.getDateTime().toLocalDate()), t.getMonetaryAmount().toString()));

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
        series.setAntialias(swtAntialias);

        if (color != null)
            series.setLineColor(color);
        series.setVisibleInLegend(visibleInLegend);
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
                // do not print exception to the log as it confuses users. The
                // old SMA200 label has been renamed, nothing we can change
                // anymore
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
        MenuManager subMenuChartScaling = new MenuManager(Messages.LabelChartDetailChartScaling, null);
        MenuManager subMenuChartDevelopment = new MenuManager(Messages.LabelChartDetailChartDevelopment, null);
        MenuManager subMenuChartMarker = new MenuManager(Messages.LabelChartDetailMarker, null);
        MenuManager subMenuChartIndicator = new MenuManager(Messages.LabelChartDetailIndicator, null);
        MenuManager subMenuChartMovingAverage = new MenuManager(Messages.LabelChartDetailMovingAverage, null);
        MenuManager subMenuChartSettings = new MenuManager(Messages.LabelChartDetailSettings, null);

        subMenuChartScaling.add(addMenuAction(ChartDetails.SCALING_LINEAR));
        subMenuChartScaling.add(addMenuAction(ChartDetails.SCALING_LOG));
        subMenuChartDevelopment.add(addMenuAction(ChartDetails.CLOSING));
        subMenuChartDevelopment.add(addMenuAction(ChartDetails.PURCHASEPRICE));
        subMenuChartMarker.add(addMenuAction(ChartDetails.INVESTMENT));
        subMenuChartMarker.add(addMenuAction(ChartDetails.DIVIDENDS));
        subMenuChartMarker.add(addMenuAction(ChartDetails.EVENTS));
        subMenuChartMarker.add(addMenuAction(ChartDetails.FIFOPURCHASE));
        subMenuChartMarker.add(addMenuAction(ChartDetails.FLOATINGAVGPURCHASE));
        subMenuChartIndicator.add(addMenuAction(ChartDetails.BOLLINGERBANDS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_5DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_20DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_30DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_38DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_50DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_90DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_100DAYS));
        subMenuChartMovingAverage.add(addMenuAction(ChartDetails.SMA_200DAYS));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_MARKER_LINES));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_DATA_LABELS));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_MISSING_TRADING_DAYS));
        manager.add(subMenuChartScaling);
        manager.add(subMenuChartDevelopment);
        manager.add(subMenuChartMarker);
        manager.add(subMenuChartIndicator);
        manager.add(subMenuChartMovingAverage);
        manager.add(subMenuChartSettings);
    }

    private Action addMenuAction(ChartDetails detail)
    {
        Action action = new SimpleAction(detail.toString(), a -> {
            boolean isActive = chartConfig.contains(detail);

            if (isActive)
                chartConfig.remove(detail);
            else
                chartConfig.add(detail);

            if (!isActive)
            {
                switch (detail)
                {
                    case SCALING_LINEAR:
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        break;
                    case SCALING_LOG:
                        chartConfig.remove(ChartDetails.SCALING_LINEAR);
                        chartConfig.remove(ChartDetails.PURCHASEPRICE);
                        chartConfig.remove(ChartDetails.CLOSING);
                        break;
                    case CLOSING:
                        chartConfig.remove(ChartDetails.PURCHASEPRICE);
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        break;
                    case PURCHASEPRICE:
                        chartConfig.remove(ChartDetails.CLOSING);
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        break;
                    default:
                        break;
                }
            }
            if (!chartConfig.contains(ChartDetails.SCALING_LINEAR) && !chartConfig.contains(ChartDetails.SCALING_LOG))
                chartConfig.add(ChartDetails.SCALING_LINEAR);

            ReadOnlyClient.unwrap(client).setProperty(PREF_KEY, String.join(",", //$NON-NLS-1$
                            chartConfig.stream().map(ChartDetails::name).collect(Collectors.toList())));

            updateChart();

        });

        action.setChecked(chartConfig.contains(detail));
        return (action);
    }

    private void addButton(Composite buttons, String label, Function<Security, ChartInterval> interval)
    {
        Button b = new Button(buttons, SWT.FLAT);
        b.setText(label);
        b.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            chartIntervalFunction = interval;
            updateChart();
        }));
    }

    public void setClient(Client client)
    {
        this.client = client;
        updateChart();
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
            chart.clearNonTradingDayMarker();
            customPaintListeners.clear();
            customBehindPaintListener.clear();
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

            // determine the interval to be shown in the chart

            ChartInterval chartInterval = null;
            if (chartIntervalFunction != null)
                chartInterval = chartIntervalFunction.apply(security);
            if (chartInterval == null)
                chartInterval = new ChartInterval(LocalDate.now().minusYears(2), LocalDate.now());

            // determine index range for given interval in prices list

            List<SecurityPrice> prices = security.getPricesIncludingLatest();

            ChartRange range = ChartRange.createFor(prices, chartInterval);
            if (range == null)
            {
                chart.redraw();
                return;
            }

            // prepare value arrays

            LocalDate[] dates = new LocalDate[range.size];

            double[] values = new double[range.size];
            double[] valuesRelative = new double[range.size];
            double[] valuesRelativePositive = new double[range.size];
            double[] valuesRelativeNegative = new double[range.size];
            double[] valuesZeroLine = new double[range.size];
            double firstQuote = 0;

            // Disable SWT antialias for more than 1000 records due to SWT
            // performance issue in Drawing
            swtAntialias = range.size > 1000 ? SWT.OFF : SWT.ON;

            if (!chartConfig.contains(ChartDetails.PURCHASEPRICE))
            {
                SecurityPrice p2 = prices.get(range.start);
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

            addChartMarkerBackground(chartInterval);

            for (int ii = 0; ii < range.size; ii++)
            {
                SecurityPrice p = prices.get(ii + range.start);
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
                                Messages.LabelChartDetailChartDevelopmentClosing + "Negative"); //$NON-NLS-1$
                lineSeries2ndNegative.setSymbolType(PlotSymbolType.NONE);
                lineSeries2ndNegative.setYAxisId(1);
                configureSeriesPainter(lineSeries2ndNegative, javaDates, valuesRelativeNegative, colorAreaNegative, 1,
                                LineStyle.SOLID, true, false);

                ILineSeries lineSeries2ndPositive = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailChartDevelopmentClosing + "Positive"); //$NON-NLS-1$
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

            addChartMarkerForeground(chartInterval);

            chart.adjustRange();

            IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
            IAxis yAxis2nd = chart.getAxisSet().getYAxis(1);
            yAxis2nd.setRange(
                            new Range(yAxis1st.getRange().lower - firstQuote, yAxis1st.getRange().upper - firstQuote));

            yAxis1st.enableLogScale(chartConfig.contains(ChartDetails.SCALING_LOG));
            yAxis2nd.enableLogScale(chartConfig.contains(ChartDetails.SCALING_LOG));

            yAxis1st.getTick().setVisible(true);

            if (chartConfig.contains(ChartDetails.SHOW_MISSING_TRADING_DAYS))
            {
                TradeCalendar tradeCalendar = TradeCalendarManager.getInstance(security);
                List<LocalDate> calendarDates = new ArrayList<>();
                for (LocalDate calendarDate = dates[0]; calendarDate
                                .isBefore(dates[dates.length - 1]); calendarDate = calendarDate.plusDays(1))
                    calendarDates.add(calendarDate);
                for (LocalDate pricingDate : dates)
                    calendarDates.remove(pricingDate);
                for (LocalDate targetDate : calendarDates)
                {
                    if (!tradeCalendar.isHoliday(targetDate))
                        chart.addNonTradingDayMarker(targetDate, colorNonTradingDay);
                }
            }
        }
        finally
        {
            chart.setRedraw(true);
            chart.redraw();
        }
    }

    private void addChartMarkerBackground(ChartInterval chartInterval)
    {
        if (chartConfig.contains(ChartDetails.BOLLINGERBANDS))
            addBollingerBandsMarkerLines(chartInterval, 20, 2);

        if (chartConfig.contains(ChartDetails.SMA_5DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_5days, 5, colorSMA1);

        if (chartConfig.contains(ChartDetails.SMA_20DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_20days, 20, colorSMA2);

        if (chartConfig.contains(ChartDetails.SMA_30DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_30days, 30, colorSMA3);

        if (chartConfig.contains(ChartDetails.SMA_38DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_38days, 38, colorSMA4);

        if (chartConfig.contains(ChartDetails.SMA_50DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_50days, 50, colorSMA4);

        if (chartConfig.contains(ChartDetails.SMA_90DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_90days, 90, colorSMA5);

        if (chartConfig.contains(ChartDetails.SMA_100DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_100days, 100, colorSMA6);

        if (chartConfig.contains(ChartDetails.SMA_200DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverage,
                            Messages.LabelChartDetailMovingAverage_200days, 200, colorSMA7);

    }

    private void addChartMarkerForeground(ChartInterval chartInterval)
    {
        if (chartConfig.contains(ChartDetails.FIFOPURCHASE))
            addFIFOPurchasePrice(chartInterval);

        if (chartConfig.contains(ChartDetails.FLOATINGAVGPURCHASE))
            addMovingAveragePurchasePrice(chartInterval);

        if (chartConfig.contains(ChartDetails.INVESTMENT))
            addInvestmentMarkerLines(chartInterval);

        if (chartConfig.contains(ChartDetails.DIVIDENDS))
            addDividendMarkerLines(chartInterval);

        if (chartConfig.contains(ChartDetails.EVENTS))
            addEventMarkerLines(chartInterval);
    }

    private void addSMAMarkerLines(ChartInterval chartInterval, String smaSeries, String smaDaysWording, int smaDays,
                    Color smaColor)
    {
        ChartLineSeriesAxes smaLines = new SimpleMovingAverage(smaDays, this.security, chartInterval).getSMA();
        if (smaLines == null || smaLines.getValues() == null || smaLines.getDates() == null)
            return;

        @SuppressWarnings("nls")
        String lineID = smaSeries + " (" + smaDaysWording + ")";

        ILineSeries lineSeriesSMA = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, lineID);
        lineSeriesSMA.setXDateSeries(smaLines.getDates());
        lineSeriesSMA.setLineWidth(2);
        lineSeriesSMA.enableArea(false);
        lineSeriesSMA.setSymbolType(PlotSymbolType.NONE);
        lineSeriesSMA.setYSeries(smaLines.getValues());
        lineSeriesSMA.setAntialias(swtAntialias);
        lineSeriesSMA.setLineColor(smaColor);
        lineSeriesSMA.setYAxisId(0);
        lineSeriesSMA.setVisibleInLegend(true);
    }

    private void addInvestmentMarkerLines(ChartInterval chartInterval)
    {
        List<PortfolioTransaction> purchase = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .filter(t -> chartInterval.contains(t.getDateTime())) //
                        .sorted(new Transaction.ByDate()).collect(Collectors.toList());

        addInvestmentMarkers(purchase, Messages.SecurityMenuBuy, colorEventPurchase);

        List<PortfolioTransaction> sales = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.SELL
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        .filter(t -> chartInterval.contains(t.getDateTime())) //
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
                chart.addMarkerLine(t.getDateTime().toLocalDate(), color, label, value);
            });
        }
        else
        {
            Date[] dates = transactions.stream().map(PortfolioTransaction::getDateTime)
                            .map(d -> Date.from(d.atZone(ZoneId.systemDefault()).toInstant()))
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

    private void addDividendMarkerLines(ChartInterval chartInterval)
    {
        List<AccountTransaction> dividends = client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .filter(t -> chartInterval.contains(t.getDateTime())).sorted(new Transaction.ByDate())
                        .collect(Collectors.toList());

        if (dividends.isEmpty())
            return;

        customTooltipEvents.addAll(dividends);

        if (chartConfig.contains(ChartDetails.SHOW_MARKER_LINES))
        {
            dividends.forEach(t -> chart.addMarkerLine(t.getDateTime().toLocalDate(), colorEventDividend,
                            getDividendLabel(t)));
        }
        else
        {
            Date[] dates = dividends.stream().map(AccountTransaction::getDateTime)
                            .map(d -> Date.from(d.atZone(ZoneId.systemDefault()).toInstant()))
                            .collect(Collectors.toList()).toArray(new Date[0]);

            IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
            double yAxis1stAxisPrice = yAxis1st.getRange().lower;

            double[] values = new double[dates.length];
            Arrays.fill(values, yAxis1stAxisPrice);

            ILineSeries border = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailMarkerDividends + "2"); //$NON-NLS-1$
            border.setYAxisId(0);
            border.setSymbolType(PlotSymbolType.SQUARE);
            border.setSymbolSize(6);
            border.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            configureSeriesPainter(border, dates, values, null, 0, LineStyle.NONE, false, false);

            ILineSeries background = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailMarkerDividends + "1"); //$NON-NLS-1$
            background.setYAxisId(0);
            background.setSymbolType(PlotSymbolType.SQUARE);
            background.setSymbolSize(5);
            background.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            configureSeriesPainter(background, dates, values, null, 0, LineStyle.NONE, false, false);

            ILineSeries inner = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailMarkerDividends);
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

    private void addEventMarkerLines(ChartInterval chartInterval)
    {
        security.getEvents().stream() //
                        .filter(e -> chartInterval.contains(e.getDate())) //
                        .forEach(e -> chart.addMarkerLine(e.getDate(),
                                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY), e.getDetails()));
    }

    private void addBollingerBandsMarkerLines(ChartInterval chartInterval, int bollingerBandsDays,
                    double bollingerBandsFactor)
    {
        BollingerBands bands = new BollingerBands(bollingerBandsDays, bollingerBandsFactor, this.security,
                        chartInterval);

        ChartLineSeriesAxes lowerBand = bands.getLowerBand();
        if (lowerBand == null || lowerBand.getValues() == null || lowerBand.getDates() == null)
            return;

        ILineSeries lineSeriesBollingerBandsLowerBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailIndicatorBollingerBandsLower);
        lineSeriesBollingerBandsLowerBand.setXDateSeries(lowerBand.getDates());
        lineSeriesBollingerBandsLowerBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsLowerBand.setLineWidth(2);
        lineSeriesBollingerBandsLowerBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsLowerBand.setYSeries(lowerBand.getValues());
        lineSeriesBollingerBandsLowerBand.setAntialias(swtAntialias);
        lineSeriesBollingerBandsLowerBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsLowerBand.setYAxisId(0);
        lineSeriesBollingerBandsLowerBand.setVisibleInLegend(false);

        ChartLineSeriesAxes middleBand = bands.getMiddleBand();
        ILineSeries lineSeriesBollingerBandsMiddleBand = (ILineSeries) chart.getSeriesSet()
                        .createSeries(SeriesType.LINE, Messages.LabelChartDetailIndicatorBollingerBands);
        lineSeriesBollingerBandsMiddleBand.setXDateSeries(middleBand.getDates());
        lineSeriesBollingerBandsMiddleBand.setLineWidth(2);
        lineSeriesBollingerBandsMiddleBand.setLineStyle(LineStyle.DOT);
        lineSeriesBollingerBandsMiddleBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsMiddleBand.setYSeries(middleBand.getValues());
        lineSeriesBollingerBandsMiddleBand.setAntialias(swtAntialias);
        lineSeriesBollingerBandsMiddleBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsMiddleBand.setYAxisId(0);
        lineSeriesBollingerBandsMiddleBand.setVisibleInLegend(true);

        ChartLineSeriesAxes upperBand = bands.getUpperBand();
        ILineSeries lineSeriesBollingerBandsUpperBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailIndicatorBollingerBandsUpper);
        lineSeriesBollingerBandsUpperBand.setXDateSeries(upperBand.getDates());
        lineSeriesBollingerBandsUpperBand.setLineWidth(2);
        lineSeriesBollingerBandsUpperBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsUpperBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsUpperBand.setYSeries(upperBand.getValues());
        lineSeriesBollingerBandsUpperBand.setAntialias(swtAntialias);
        lineSeriesBollingerBandsUpperBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsUpperBand.setYAxisId(0);
        lineSeriesBollingerBandsUpperBand.setVisibleInLegend(false);
    }

    private void addFIFOPurchasePrice(ChartInterval chartInterval)
    {
        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security.getCurrencyCode() == null)
            return;

        // create a list of dates that are relevant for FIFO purchase price
        // changes (i.e. all purchase and sell events)

        Client filteredClient = new ClientSecurityFilter(security).filter(client);
        CurrencyConverter securityCurrency = converter.with(security.getCurrencyCode());

        List<LocalDate> candidates = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getSecurity().equals(security))
                        .filter(t -> !(t.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                        || t.getType() == PortfolioTransaction.Type.TRANSFER_OUT))
                        .filter(t -> !t.getDateTime().toLocalDate().isAfter(chartInterval.getEnd()))
                        .map(t -> chartInterval.contains(t.getDateTime()) ? t.getDateTime().toLocalDate()
                                        : chartInterval.getStart())
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

        getPurchasePrice(filteredClient, securityCurrency, chartInterval.getEnd()).ifPresent(price -> {
            dates.add(chartInterval.getEnd());
            values.add(price);
        });

        if (!dates.isEmpty())
            createFIFOPurchaseLineSeries(values, dates, seriesCounter);
    }

    private void createFIFOPurchaseLineSeries(List<Double> values, List<LocalDate> dates, int seriesCounter)
    {
        String label = seriesCounter == 0 ? Messages.LabelChartDetailMarkerPurchaseFIFO
                        : MessageFormat.format(Messages.LabelChartDetailMarkerPurchaseFIFOHoldingPeriod,
                                        seriesCounter + 1);

        ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, label);

        series.setSymbolType(PlotSymbolType.NONE);
        series.setYAxisId(0);
        series.enableStep(true);

        configureSeriesPainter(series, TimelineChart.toJavaUtilDate(dates.toArray(new LocalDate[0])),
                        Doubles.toArray(values), colorFifoPurchasePrice, 2,
                        LineStyle.SOLID, false, seriesCounter == 0);
    }

    private void addMovingAveragePurchasePrice(ChartInterval chartInterval)
    {
        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security.getCurrencyCode() == null)
            return;

        // create a list of dates that are relevant for floating avg purchase
        // price
        // changes (i.e. all purchase and sell events)

        Client filteredClient = new ClientSecurityFilter(security).filter(client);
        CurrencyConverter securityCurrency = converter.with(security.getCurrencyCode());

        List<LocalDate> candidates = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getSecurity().equals(security))
                        .filter(t -> !(t.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                        || t.getType() == PortfolioTransaction.Type.TRANSFER_OUT))
                        .filter(t -> !t.getDateTime().toLocalDate().isAfter(chartInterval.getEnd()))
                        .map(t -> chartInterval.contains(t.getDateTime()) ? t.getDateTime().toLocalDate()
                                        : chartInterval.getStart())
                        .distinct() //
                        .sorted() //
                        .collect(Collectors.toList());

        // calculate floating avg purchase price for each event - separate
        // lineSeries
        // per holding period

        List<Double> values = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        int seriesCounter = 0;

        for (LocalDate eventDate : candidates)
        {
            Optional<Double> purchasePrice = getMovingAveragePurchasePrice(filteredClient, securityCurrency, eventDate);

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

                    createMovingAveragePurchaseLineSeries(values, dates, seriesCounter++);

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

        getMovingAveragePurchasePrice(filteredClient, securityCurrency, chartInterval.getEnd()).ifPresent(price -> {
            dates.add(chartInterval.getEnd());
            values.add(price);
        });

        if (!dates.isEmpty())
            createMovingAveragePurchaseLineSeries(values, dates, seriesCounter);
    }

    private void createMovingAveragePurchaseLineSeries(List<Double> values, List<LocalDate> dates, int seriesCounter)
    {
        String label = seriesCounter == 0 ? Messages.LabelChartDetailMarkerPurchaseMovingAverage
                        : MessageFormat.format(Messages.LabelChartDetailMarkerPurchaseMovingAverageHoldingPeriod,
                                        seriesCounter + 1);

        ILineSeries series = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, label);

        series.setSymbolType(PlotSymbolType.NONE);
        series.setYAxisId(0);
        series.enableStep(true);

        configureSeriesPainter(series, TimelineChart.toJavaUtilDate(dates.toArray(new LocalDate[0])),
                        Doubles.toArray(values), colorMovingAveragePurchasePrice, 2,
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
        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, currencyConverter, date);
        AssetPosition position = snapshot.getPositionsByVehicle().get(security);
        if (position == null)
            return Optional.empty();

        Money purchasePrice = position.getPosition().getFIFOPurchasePrice();
        if (!purchasePrice.isZero())
            return Optional.of(purchasePrice.getAmount() / Values.Amount.divider());
        else
            return Optional.empty();
    }

    private Optional<Double> getMovingAveragePurchasePrice(Client filteredClient, CurrencyConverter currencyConverter,
                    LocalDate date)
    {
        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, currencyConverter, date);
        AssetPosition position = snapshot.getPositionsByVehicle().get(security);
        if (position == null)
            return Optional.empty();

        Money purchasePrice = position.getPosition().getMovingAveragePurchasePrice();
        if (!purchasePrice.isZero())
            return Optional.of(purchasePrice.getAmount() / Values.Amount.divider());
        else
            return Optional.empty();
    }

}
