package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.ILegend;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;

import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.chart.TimelineSeriesModel;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;
import name.abuchen.portfolio.ui.views.dataseries.ColorWheel;
import name.abuchen.portfolio.ui.views.securitychart.SharesHeldChartSeries;
import name.abuchen.portfolio.util.FormatHelper;
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

    private static class ChartIntervalOrMessage
    {
        private final String message;
        private final ChartInterval interval;

        private ChartIntervalOrMessage(ChartInterval interval)
        {
            this.interval = interval;
            this.message = null;
        }

        private ChartIntervalOrMessage(String message)
        {
            this.interval = null;
            this.message = message;
        }

        public String getMessage()
        {
            return message;
        }

        public ChartInterval getInterval()
        {
            return interval;
        }
    }

    public enum IntervalOption
    {
        M1(Messages.SecurityTabChart1M, Messages.SecurityTabChart1MToolTip), //
        M2(Messages.SecurityTabChart2M, Messages.SecurityTabChart2MToolTip), //
        M6(Messages.SecurityTabChart6M, Messages.SecurityTabChart6MToolTip), //
        Y1(Messages.SecurityTabChart1Y, Messages.SecurityTabChart1YToolTip), //
        Y2(Messages.SecurityTabChart2Y, Messages.SecurityTabChart2YToolTip), //
        Y3(Messages.SecurityTabChart3Y, Messages.SecurityTabChart3YToolTip), //
        Y5(Messages.SecurityTabChart5Y, Messages.SecurityTabChart5YToolTip), //
        Y10(Messages.SecurityTabChart10Y, Messages.SecurityTabChart10YToolTip), //
        YTD(Messages.SecurityTabChartYTD, Messages.SecurityTabChartYTDToolTip), //
        H(Messages.SecurityTabChartHoldingPeriod, Messages.SecurityTabChartHoldingPeriodToolTip), //
        ALL(Messages.SecurityTabChartAll, Messages.SecurityTabChartAllToolTip);

        private final String label;
        private final String tooltip;

        private IntervalOption(String label, String tooltip)
        {
            this.label = label;
            this.tooltip = tooltip;
        }

        public String getLabel()
        {
            return label;
        }

        public String getTooltip()
        {
            return tooltip;
        }

        public ChartIntervalOrMessage getInterval(Client client, CurrencyConverter converter, Security security)
        {
            LocalDate now = LocalDate.now();

            switch (this)
            {
                case M1:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofMonths(1)), now));
                case M2:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofMonths(2)), now));
                case M6:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofMonths(6)), now));
                case Y1:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofYears(1)), now));
                case Y2:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofYears(2)), now));
                case Y3:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofYears(3)), now));
                case Y5:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofYears(5)), now));
                case Y10:
                    return new ChartIntervalOrMessage(new ChartInterval(now.minus(Period.ofYears(10)), now));
                case YTD:
                    return new ChartIntervalOrMessage(
                                    new ChartInterval(now.minus(Period.ofDays(now.getDayOfYear() - 1)), now));
                case H:
                    List<TransactionPair<?>> tx = security.getTransactions(client);
                    if (tx.isEmpty())
                        return new ChartIntervalOrMessage(Messages.SecuritiesChart_NoDataMessage_NoHoldings);

                    Collections.sort(tx, TransactionPair.BY_DATE);
                    boolean hasHoldings = ClientSnapshot.create(client, converter, LocalDate.now())
                                    .getPositionsByVehicle().containsKey(security);

                    return new ChartIntervalOrMessage(
                                    new ChartInterval(tx.get(0).getTransaction().getDateTime().toLocalDate(),
                                                    hasHoldings ? LocalDate.now()
                                                                    : tx.get(tx.size() - 1).getTransaction()
                                                                                    .getDateTime().toLocalDate()));
                case ALL:
                    List<SecurityPrice> prices = security.getPricesIncludingLatest();
                    if (prices.isEmpty())
                        return new ChartIntervalOrMessage(Messages.SecuritiesChart_NoDataMessage_NoPrices);
                    else
                        return new ChartIntervalOrMessage(new ChartInterval(prices.get(0).getDate(),
                                        prices.get(prices.size() - 1).getDate()));

                default:
                    throw new IllegalArgumentException("unsupported chart type " + this); //$NON-NLS-1$
            }
        }
    }

    private enum ChartDetails
    {
        SCALING_LINEAR(Messages.LabelChartDetailChartScalingLinear), //
        SCALING_LOG(Messages.LabelChartDetailChartScalingLog), //
        CLOSING(Messages.LabelChartDetailChartDevelopmentClosing), //
        PURCHASEPRICE(Messages.LabelChartDetailChartDevelopmentClosingFIFO), //
        PURCHASEPRICE_MA(Messages.LabelChartDetailChartDevelopmentClosingMovingAverage), //
        INVESTMENT(Messages.LabelChartDetailMarkerInvestments), //
        SHARES_HELD(Messages.ColumnSharesOwned), //
        DIVIDENDS(Messages.LabelChartDetailMarkerDividends), //
        EVENTS(Messages.LabelChartDetailMarkerEvents), //
        EXTREMES(Messages.LabelChartDetailMarkerHighLow), //
        FIFOPURCHASE(MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnPurchasePrice,
                        CostMethod.FIFO.getLabel())), //
        FLOATINGAVGPURCHASE(MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnPurchasePrice,
                        CostMethod.MOVING_AVERAGE.getLabel())), //
        BOLLINGERBANDS(Messages.LabelChartDetailIndicatorBollingerBands), //
        MACD(Messages.LabelChartDetailIndicatorMacd), //
        SMA_5DAYS(Messages.LabelChartDetailMovingAverage_5days), //
        SMA_20DAYS(Messages.LabelChartDetailMovingAverage_20days), //
        SMA_30DAYS(Messages.LabelChartDetailMovingAverage_30days), //
        SMA_38DAYS(Messages.LabelChartDetailMovingAverage_38days), //
        SMA_50DAYS(Messages.LabelChartDetailMovingAverage_50days), //
        SMA_90DAYS(Messages.LabelChartDetailMovingAverage_90days), //
        SMA_100DAYS(Messages.LabelChartDetailMovingAverage_100days), //
        SMA_200DAYS(Messages.LabelChartDetailMovingAverage_200days), //
        EMA_5DAYS(Messages.LabelChartDetailMovingAverage_5days), //
        EMA_20DAYS(Messages.LabelChartDetailMovingAverage_20days), //
        EMA_30DAYS(Messages.LabelChartDetailMovingAverage_30days), //
        EMA_38DAYS(Messages.LabelChartDetailMovingAverage_38days), //
        EMA_50DAYS(Messages.LabelChartDetailMovingAverage_50days), //
        EMA_90DAYS(Messages.LabelChartDetailMovingAverage_90days), //
        EMA_100DAYS(Messages.LabelChartDetailMovingAverage_100days), //
        EMA_200DAYS(Messages.LabelChartDetailMovingAverage_200days), //
        SHOW_MARKER_LINES(Messages.LabelChartDetailSettingsShowMarkerLines), //
        SHOW_DATA_DIVESTMENT_INVESTMENT_LABEL(Messages.LabelChartDetailSettingsShowDivestmentInvestmentDataLabel), //
        SHOW_DATA_DIVIDEND_LABEL(Messages.LabelChartDetailSettingsShowDividendDataLabel), //
        SHOW_DATA_EXTREMES_LABEL(Messages.LabelChartDetailSettingsShowExtremeDataLabel), //
        SHOW_MISSING_TRADING_DAYS(Messages.LabelChartDetailSettingsShowMissingTradingDays), //
        SHOW_LIMITS(Messages.LabelChartDetailSettingsShowLimits), //
        SHOW_PERCENTAGE_AXIS(Messages.LabelChartDetailSettingsShowPercentageAxis), //
        SHOW_MAIN_HORIZONTAL_LINES(Messages.LabelChartDetailSettingsShowHorizontalLinesMain), //
        SHOW_PERCENTAGE_HORIZONTAL_LINES(Messages.LabelChartDetailSettingsShowHorizontalLinesPercentage);

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

    /* testing */ static class ChartRange
    {
        public final int start;
        public final int size;

        public final LocalDate startDate;
        public final LocalDate endDate;

        public ChartRange(int start, int end, LocalDate startDate, LocalDate endDate)
        {
            this.start = start;
            this.size = end - start;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        /**
         * Maps the given {@link ChartInterval} to a range in the list of
         * security prices. Returns null if the interval does not intersect with
         * the list of prices.
         */
        public static ChartRange createFor(List<SecurityPrice> prices, ChartInterval chartInterval)
        {
            if (chartInterval == null)
                return null;

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

            return new ChartRange(start, end, prices.get(start).getDate(),
                            prices.get(Math.min(end, prices.size() - 1)).getDate());
        }
    }

    private Color colorQuote = Colors.getColor(77, 52, 235); // #4D34EB
    private Color colorQuoteAreaPositive = Colors.getColor(90, 114, 226); // #5A72E2
    private Color colorQuoteAreaNegative = Colors.getColor(226, 91, 90); // #E25B5A

    private Color colorEventPurchase = Colors.getColor(26, 173, 33); // #1AAD21
    private Color colorEventSale = Colors.getColor(255, 43, 48); // #FF2B30
    private Color colorEventDividend = Colors.getColor(128, 99, 168); // #8063A8

    private Color colorExtremeMarkerHigh = Colors.getColor(0, 147, 15); // #00930F
    private Color colorExtremeMarkerLow = Colors.getColor(168, 39, 42); // #A8272A

    private Color colorNonTradingDay = Colors.getColor(255, 137, 89); // #FF8959

    private Color colorSharesHeld = Colors.getColor(235, 201, 52); // #EBC934

    private static final Color colorFifoPurchasePrice = Colors.getColor(226, 122, 121); // #E27A79
    private static final Color colorMovingAveragePurchasePrice = Colors.getColor(150, 82, 81); // #965251
    private static final Color colorBollingerBands = Colors.getColor(201, 141, 68); // #C98D44
    private static final Color colorMACD = Colors.getColor(226, 155, 200); // #E29BC8

    private static final Color colorSMA1 = Colors.getColor(179, 107, 107); // #B36B6B
    private static final Color colorSMA2 = Colors.getColor(179, 167, 107); // #B3A76B
    private static final Color colorSMA3 = Colors.getColor(131, 179, 107); // #83B36B
    private static final Color colorSMA4 = Colors.getColor(107, 179, 143); // #6BB38F
    private static final Color colorSMA5 = Colors.getColor(107, 155, 179); // #6B9BB3
    private static final Color colorSMA6 = Colors.getColor(119, 107, 179); // #776BB3
    private static final Color colorSMA7 = Colors.getColor(179, 107, 179); // #B36BB3

    private static final Color colorEMA1 = Colors.getColor(200, 107, 107); // #C86B6B
    private static final Color colorEMA2 = Colors.getColor(200, 167, 107); // #C8A76B
    private static final Color colorEMA3 = Colors.getColor(131, 200, 107); // #83C86B
    private static final Color colorEMA4 = Colors.getColor(107, 200, 143); // #6BC88F
    private static final Color colorEMA5 = Colors.getColor(107, 155, 200); // #6B9BC8
    private static final Color colorEMA6 = Colors.getColor(119, 107, 200); // #776BC8
    private static final Color colorEMA7 = Colors.getColor(200, 107, 200); // #C86BB3

    private static final String PREF_KEY = "security-chart-details"; //$NON-NLS-1$

    private static final int MAX_SECURITIES_BENCHMARK = 10;

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d LLL"); //$NON-NLS-1$

    private Composite container;

    private Client client;
    private CurrencyConverter converter;
    private Security[] securities = new Security[0];

    private TimelineChart chart;

    /**
     * Calculates dynamically for each security the interval of security prices
     * to be shown.
     */
    private IntervalOption intervalOption = IntervalOption.Y2;

    /**
     * Flag to quickly disable/enable showing additional markings on chart. The
     * idea is that they are useful, but may make a chart very crowded. So, it's
     * nice idea to let users with one click see either fully annotated chart,
     * or pristine price chart.
     */
    private boolean showMarkings = true;

    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.INVESTMENT, ChartDetails.EVENTS,
                    ChartDetails.SCALING_LINEAR, ChartDetails.SHOW_MAIN_HORIZONTAL_LINES);

    private List<PaintListener> customPaintListeners = new ArrayList<>();
    private List<Transaction> customTooltipEvents = new ArrayList<>();

    private int swtAntialias = SWT.ON;

    private MessagePainter messagePainter = new MessagePainter();

    private String investmentMarkerLabelBuy = PortfolioTransaction.Type.BUY.toString();
    private String investmentMarkerLabelSell = PortfolioTransaction.Type.SELL.toString();

    public SecuritiesChart(Composite parent, Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;

        readChartConfig(client);

        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        chart = new TimelineChart(container);
        chart.getTitle().setText("..."); //$NON-NLS-1$
        chart.getTitle().setVisible(false);

        chart.addPlotPaintListener(event -> customPaintListeners.forEach(l -> l.paintControl(event)));
        chart.addPlotPaintListener(this.messagePainter);
        chart.getPlotArea().getControl().addDisposeListener(this.messagePainter);

        messagePainter.setMessage(Messages.SecuritiesChart_NoDataMessage_NoSecuritySelected);

        ILegend legend = chart.getLegend();
        legend.setPosition(SWT.BOTTOM);
        legend.setVisible(true);

        setupTooltip();
    }

    public IntervalOption getIntervalOption()
    {
        return intervalOption;
    }

    public void setIntervalOption(IntervalOption intervalOption)
    {
        this.intervalOption = intervalOption;
    }

    public void setQuoteColor(Color color)
    {
        this.colorQuote = color;
    }

    public void setQuoteAreaNegative(Color color)
    {
        this.colorQuoteAreaNegative = color;
    }

    public void setQuoteAreaPositive(Color color)
    {
        this.colorQuoteAreaPositive = color;
    }

    public void setPurchaseColor(Color color)
    {
        this.colorEventPurchase = color;
    }

    public void setSaleColor(Color color)
    {
        this.colorEventSale = color;
    }

    public void setDividendColor(Color color)
    {
        this.colorEventDividend = color;
    }

    public void setExtremeMarkerHighColor(Color color)
    {
        this.colorExtremeMarkerHigh = color;
    }

    public void setExtremeMarkerLowColor(Color color)
    {
        this.colorExtremeMarkerLow = color;
    }

    public void setNonTradingColor(Color color)
    {
        this.colorNonTradingDay = color;
    }

    public Color getSharesHeldColor()
    {
        return colorSharesHeld;
    }

    public void setSharesHeldColor(Color color)
    {
        this.colorSharesHeld = color;
    }

    public Client getClient()
    {
        return this.client;
    }

    public int getAntialias()
    {
        return this.swtAntialias;
    }

    private void setupTooltip()
    {
        TimelineChartToolTip toolTip = chart.getToolTip();

        toolTip.showToolTipOnlyForDatesInDataSeries(Messages.ColumnQuote);

        toolTip.setDefaultValueFormat(new DecimalFormat(Values.Quote.pattern()));
        toolTip.addSeriesExclude(Messages.LabelChartDetailChartDevelopment + "Positive"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailChartDevelopment + "Negative"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailChartDevelopment + "Zero"); //$NON-NLS-1$
        toolTip.addSeriesExclude(investmentMarkerLabelBuy);
        toolTip.addSeriesExclude(investmentMarkerLabelBuy + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(investmentMarkerLabelBuy + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(investmentMarkerLabelSell);
        toolTip.addSeriesExclude(investmentMarkerLabelSell + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(investmentMarkerLabelSell + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailMarkerDividends);
        toolTip.addSeriesExclude(Messages.LabelChartDetailMarkerDividends + "1"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailMarkerDividends + "2"); //$NON-NLS-1$
        toolTip.addSeriesExclude(Messages.LabelChartDetailIndicatorBollingerBands);

        int precision = FormatHelper.getCalculatedQuoteDisplayPrecision();
        DecimalFormat calculatedFormat = new DecimalFormat(Values.CalculatedQuote.pattern());
        calculatedFormat.setMinimumFractionDigits(precision);
        calculatedFormat.setMaximumFractionDigits(precision);
        for (String period : new String[] { Messages.LabelChartDetailMovingAverage_5days,
                        Messages.LabelChartDetailMovingAverage_20days, Messages.LabelChartDetailMovingAverage_30days,
                        Messages.LabelChartDetailMovingAverage_38days, Messages.LabelChartDetailMovingAverage_50days,
                        Messages.LabelChartDetailMovingAverage_100days,
                        Messages.LabelChartDetailMovingAverage_200days, })
        {
            toolTip.overrideValueFormat(String.format("%s (%s)", Messages.LabelChartDetailMovingAverageEMA, period), //$NON-NLS-1$
                            calculatedFormat);
            toolTip.overrideValueFormat(String.format("%s (%s)", Messages.LabelChartDetailMovingAverageSMA, period), //$NON-NLS-1$
                            calculatedFormat);
        }
        toolTip.overrideValueFormat(Messages.LabelChartDetailIndicatorBollingerBandsLower, calculatedFormat);
        toolTip.overrideValueFormat(Messages.LabelChartDetailIndicatorBollingerBandsUpper, calculatedFormat);
        toolTip.overrideValueFormat(MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnPurchasePrice,
                        CostMethod.FIFO.getLabel()), calculatedFormat);
        toolTip.overrideValueFormat(MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnPurchasePrice,
                        CostMethod.MOVING_AVERAGE.getLabel()), calculatedFormat);
        toolTip.overrideValueFormat(Messages.LabelChartDetailIndicatorMacd, calculatedFormat);
        toolTip.overrideValueFormat(Messages.LabelChartDetailIndicatorMacdSignal, calculatedFormat);

        toolTip.addExtraInfo((composite, focus) -> {
            if (focus instanceof LocalDate date)
            {
                Interval displayInterval = Interval.of(date.minusDays(5), date.plusDays(5));

                customTooltipEvents.stream() //
                                .filter(t -> displayInterval.contains(t.getDateTime())) //
                                .forEach(t -> {
                                    if (t instanceof AccountTransaction at)
                                        addDividendTooltip(composite, at);
                                    else if (t instanceof PortfolioTransaction pt)
                                        addInvestmentTooltip(composite, pt);
                                });
            }
        });
    }

    private void setupTooltipDisplayCalculatedQuote(String seriesLabel)
    {
        TimelineChartToolTip toolTip = chart.getToolTip();

        int precision = FormatHelper.getCalculatedQuoteDisplayPrecision();
        DecimalFormat calculatedFormat = new DecimalFormat(Values.CalculatedQuote.pattern());
        calculatedFormat.setMinimumFractionDigits(precision);
        calculatedFormat.setMaximumFractionDigits(precision);

        toolTip.overrideValueFormat(seriesLabel, calculatedFormat);
    }

    private void addInvestmentTooltip(Composite composite, PortfolioTransaction t)
    {
        Label label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipTransactionSummary, t.getType().toString(),
                        dateTimeFormatter.format(t.getDateTime().toLocalDate()), t.getMonetaryAmount().toString()));

        label = new Label(composite, SWT.NONE);
        label.setText(MessageFormat.format(Messages.LabelToolTipInvestmentDetails, Values.Share.format(t.getShares()),
                        Values.CalculatedQuote.format(
                                        t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())))));
    }

    private void addDividendTooltip(Composite composite, AccountTransaction t)
    {
        Label label = new Label(composite, SWT.NONE);
        String amount = t.getMonetaryAmount().toString();
        label.setText(MessageFormat.format(Messages.LabelToolTipTransactionSummary, t.getType().toString(),
                        dateTimeFormatter.format(t.getDateTime().toLocalDate()), amount));

        if (t.getShares() == 0L)
        {
            label = new Label(composite, SWT.NONE);
            label.setText("\u2211 " + t.getGrossValue().toString()); //$NON-NLS-1$
        }
        else
        {
            Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
            long gross = grossValue.isPresent() ? grossValue.get().getForex().getAmount() : t.getGrossValueAmount();
            String currency = grossValue.isPresent() ? grossValue.get().getForex().getCurrencyCode()
                            : t.getCurrencyCode();

            // gross value in either forex currency or transaction currency
            String grossAmount = Money.of(currency, gross).toString();

            // gross value in transaction currency
            String grossValueAmount = Money.of(t.getCurrencyCode(), t.getGrossValueAmount()).toString();

            // display gross value in transaction currency, different gross
            // value in security currency exists
            if (!grossValueAmount.equals(grossAmount))
            {
                label = new Label(composite, SWT.NONE);
                label.setText(MessageFormat.format(Messages.LabelToolTipDividendDetailsGross, grossValueAmount));
            }

            // display gross value, if different to net amount
            if (!grossAmount.equals(amount))
            {
                label = new Label(composite, SWT.NONE);
                label.setText(MessageFormat.format(Messages.LabelToolTipDividendDetailsGross, grossAmount));
            }

            // display dividend per share in security currency
            label = new Label(composite, SWT.NONE);
            label.setText(MessageFormat.format(Messages.LabelToolTipDividendDetails, Values.Share.format(t.getShares()),
                            currency, Values.Quote.format(Math.round(gross * Values.Share.divider()
                                            * Values.Quote.factorToMoney() / t.getShares()))));

        }
    }

    private void configureSeriesPainter(ILineSeries<Integer> series, LocalDate[] dates, double[] values, Color color,
                    int lineWidth, LineStyle lineStyle, boolean enableArea, boolean visibleInLegend)
    {
        if (lineWidth != 0)
            series.setLineWidth(lineWidth);

        series.setDataModel(new TimelineSeriesModel(dates, values));

        series.setLineStyle(lineStyle);
        series.enableArea(enableArea);
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

    public void addButtons(ToolBarManager toolBar)
    {
        chart.getChartToolsManager().addButtons(toolBar);

        SimpleAction actionHideMarkings = new SimpleAction(Messages.LabelHideMarkings, a -> {
            this.showMarkings = !this.showMarkings;
            a.setImageDescriptor(this.showMarkings ? Images.HIDDEN.descriptor() : Images.VISIBLE.descriptor());
            updateChart();
        });
        actionHideMarkings.setImageDescriptor(Images.HIDDEN.descriptor());
        toolBar.add(actionHideMarkings);

        toolBar.add(new Separator());

        List<Action> viewActions = new ArrayList<>();

        for (IntervalOption option : IntervalOption.values())
        {
            SimpleAction action = new SimpleAction(option.getLabel(), IAction.AS_CHECK_BOX, option.getTooltip(), a -> {
                this.intervalOption = option;
                updateChart();
                for (Action viewAction : viewActions)
                    viewAction.setChecked(a.equals(viewAction));
            });
            if (intervalOption == option)
                action.setChecked(true);
            viewActions.add(action);
            toolBar.add(action);
        }
        toolBar.add(new Separator());
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE, this::chartConfigAboutToShow));
    }

    private void chartConfigAboutToShow(IMenuManager manager)
    {
        MenuManager subMenuChartScaling = new MenuManager(Messages.LabelChartDetailChartScaling, null);
        MenuManager subMenuChartDevelopment = new MenuManager(Messages.LabelChartDetailChartDevelopment, null);
        MenuManager subMenuChartMarker = new MenuManager(Messages.LabelChartDetailMarker, null);
        MenuManager subMenuChartIndicator = new MenuManager(Messages.LabelChartDetailIndicator, null);
        MenuManager subMenuChartMovingAverage = new MenuManager(Messages.LabelChartDetailMovingAverage, null);
        MenuManager subMenuChartMovingAverageSMA = new MenuManager(Messages.LabelChartDetailMovingAverageSMA, null);
        MenuManager subMenuChartMovingAverageEMA = new MenuManager(Messages.LabelChartDetailMovingAverageEMA, null);
        MenuManager subMenuChartSettings = new MenuManager(Messages.LabelChartDetailSettings, null);

        subMenuChartScaling.add(addMenuAction(ChartDetails.SCALING_LINEAR));
        subMenuChartScaling.add(addMenuAction(ChartDetails.SCALING_LOG));
        subMenuChartDevelopment.add(addMenuAction(ChartDetails.CLOSING));
        subMenuChartDevelopment.add(addMenuAction(ChartDetails.PURCHASEPRICE));
        subMenuChartDevelopment.add(addMenuAction(ChartDetails.PURCHASEPRICE_MA));
        subMenuChartMarker.add(addMenuAction(ChartDetails.INVESTMENT));
        subMenuChartMarker.add(addMenuAction(ChartDetails.SHARES_HELD));
        subMenuChartMarker.add(addMenuAction(ChartDetails.DIVIDENDS));
        subMenuChartMarker.add(addMenuAction(ChartDetails.EVENTS));
        subMenuChartMarker.add(addMenuAction(ChartDetails.EXTREMES));
        subMenuChartMarker.add(addMenuAction(ChartDetails.FIFOPURCHASE));
        subMenuChartMarker.add(addMenuAction(ChartDetails.FLOATINGAVGPURCHASE));
        subMenuChartMarker.add(addMenuAction(ChartDetails.SHOW_LIMITS));
        subMenuChartIndicator.add(addMenuAction(ChartDetails.BOLLINGERBANDS));
        subMenuChartIndicator.add(addMenuAction(ChartDetails.MACD));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_5DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_20DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_30DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_38DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_50DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_90DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_100DAYS));
        subMenuChartMovingAverageSMA.add(addMenuAction(ChartDetails.SMA_200DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_5DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_20DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_30DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_38DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_50DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_90DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_100DAYS));
        subMenuChartMovingAverageEMA.add(addMenuAction(ChartDetails.EMA_200DAYS));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_MARKER_LINES));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_DATA_DIVIDEND_LABEL));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_DATA_EXTREMES_LABEL));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_DATA_DIVESTMENT_INVESTMENT_LABEL));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_MISSING_TRADING_DAYS));
        subMenuChartSettings.add(new Separator());
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_PERCENTAGE_AXIS));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_MAIN_HORIZONTAL_LINES));
        subMenuChartSettings.add(addMenuAction(ChartDetails.SHOW_PERCENTAGE_HORIZONTAL_LINES));
        manager.add(subMenuChartScaling);
        manager.add(subMenuChartDevelopment);
        manager.add(subMenuChartMarker);
        manager.add(subMenuChartIndicator);
        manager.add(subMenuChartMovingAverage);
        subMenuChartMovingAverage.add(subMenuChartMovingAverageSMA);
        subMenuChartMovingAverage.add(subMenuChartMovingAverageEMA);
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
                        chartConfig.remove(ChartDetails.PURCHASEPRICE_MA);
                        chartConfig.remove(ChartDetails.CLOSING);
                        break;
                    case CLOSING:
                        chartConfig.remove(ChartDetails.PURCHASEPRICE);
                        chartConfig.remove(ChartDetails.PURCHASEPRICE_MA);
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        break;
                    case PURCHASEPRICE:
                        chartConfig.remove(ChartDetails.CLOSING);
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        chartConfig.remove(ChartDetails.PURCHASEPRICE_MA);
                        break;
                    case PURCHASEPRICE_MA:
                        chartConfig.remove(ChartDetails.CLOSING);
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        chartConfig.remove(ChartDetails.PURCHASEPRICE);
                        break;
                    case SHOW_MAIN_HORIZONTAL_LINES:
                        chartConfig.remove(ChartDetails.SHOW_PERCENTAGE_HORIZONTAL_LINES);
                        break;
                    case SHOW_PERCENTAGE_HORIZONTAL_LINES:
                        chartConfig.remove(ChartDetails.SHOW_MAIN_HORIZONTAL_LINES);
                        break;
                    default:
                        break;
                }
            }

            if (!chartConfig.contains(ChartDetails.SCALING_LINEAR) && !chartConfig.contains(ChartDetails.SCALING_LOG))
                chartConfig.add(ChartDetails.SCALING_LINEAR);

            ReadOnlyClient.unwrap(client).setProperty(PREF_KEY, String.join(",", //$NON-NLS-1$
                            chartConfig.stream().map(ChartDetails::name).toList()));

            updateChart();

        });

        action.setChecked(chartConfig.contains(detail));
        return action;
    }

    public void updateChart(Client client, Security[] securities)
    {
        this.client = client;
        this.securities = securities;

        // update the tooltip format: for single security we show quotes, for
        // multiple securities we show percentages
        chart.getToolTip().setDefaultValueFormat(securities.length <= 1 ? new DecimalFormat(Values.Quote.pattern())
                        : new DecimalFormat(Values.Percent2.pattern()));

        updateChart();
    }

    public Control getControl()
    {
        return container;
    }

    private void clearChart()
    {
        // delete all line series (quotes + possibly moving average)
        ISeries<?>[] series = chart.getSeriesSet().getSeries();
        for (ISeries<?> s : series)
            chart.getSeriesSet().deleteSeries(s.getId());

        chart.clearMarkerLines();
        chart.clearNonTradingDayMarker();
        customPaintListeners.clear();
        customTooltipEvents.clear();
        chart.resetAxes();
        messagePainter.setMessage(null);
    }

    private String getTitleText()
    {
        if (securities.length == 0)
            return "..."; //$NON-NLS-1$

        return Arrays.stream(securities).map(Named::getName).collect(Collectors.joining(", ")); //$NON-NLS-1$
    }

    private void updateChart()
    {
        boolean isSingleSecurityMode = securities.length == 1;

        EnumSet<ChartDetails> chartConfigPainting = chartConfig;
        if (!isSingleSecurityMode)
        {
            chartConfigPainting = EnumSet.of(ChartDetails.SCALING_LINEAR);
        }

        chart.setRedraw(false);
        chart.suspendUpdate(true);

        ColorWheel colorWheel = new ColorWheel(securities.length);

        try
        {

            clearChart();

            chart.getTitle().setText(getTitleText());

            if (securities.length == 0)
            {
                messagePainter.setMessage(Messages.SecuritiesChart_NoDataMessage_NoSecuritySelected);
                return;
            }

            if (!isSingleSecurityMode)
                chart.getAxisSet().getYAxis(0).getTick().setFormat(new AxisTickPercentNumberFormat("+#.##%;-#.##%")); //$NON-NLS-1$
            else
                chart.getAxisSet().getYAxis(0).getTick().setFormat(null);

            if (securities.length > MAX_SECURITIES_BENCHMARK)
            {
                messagePainter.setMessage(
                                MessageFormat.format(Messages.SecuritiesChart_MaxSecuritiesReachedForBenchmarkHint,
                                                MAX_SECURITIES_BENCHMARK));
                return;
            }

            for (Security security : securities)
            {

                List<SecurityPrice> prices = security.getPricesIncludingLatest();
                if (isSingleSecurityMode && prices.isEmpty())
                {
                    messagePainter.setMessage(Messages.SecuritiesChart_NoDataMessage_NoPrices);
                    return;
                }

                ChartIntervalOrMessage chartIntervalOrMessage = intervalOption.getInterval(client, converter, security);
                if (chartIntervalOrMessage.getMessage() != null)
                {
                    if (isSingleSecurityMode)
                        messagePainter.setMessage(chartIntervalOrMessage.getMessage());

                    continue;
                }

                // determine the interval to be shown in the chart
                ChartInterval chartInterval = chartIntervalOrMessage.getInterval();
                ChartRange range = ChartRange.createFor(prices, chartInterval);
                if (range == null)
                {
                    if (isSingleSecurityMode)
                    {
                        messagePainter.setMessage(MessageFormat.format(
                                        Messages.SecuritiesChart_NoDataMessage_NoPricesInSelectedPeriod,
                                        intervalOption.getTooltip()));
                    }

                    continue;
                }

                // prepare value arrays
                LocalDate[] dates = new LocalDate[range.size];

                double[] values = new double[range.size];
                double[] valuesRelative = new double[range.size];
                double[] valuesRelativePositive = new double[range.size];
                double[] valuesRelativeNegative = new double[range.size];
                double[] valuesZeroLine = new double[range.size];
                Double firstQuote = null;
                Double referenceQuote = (prices.get(range.start).getValue() / Values.Quote.divider());

                // Disable SWT antialias for more than 1000 records due to SWT
                // performance issue in Drawing
                swtAntialias = range.size > 1000 ? SWT.OFF : SWT.ON;

                boolean showAreaRelativeToFirstQuote = chartConfigPainting.contains(ChartDetails.CLOSING)
                                || chartConfigPainting.contains(ChartDetails.PURCHASEPRICE)
                                || chartConfigPainting.contains(ChartDetails.PURCHASEPRICE_MA);

                CostMethod costMethod = null;
                if (chartConfigPainting.contains(ChartDetails.PURCHASEPRICE))
                    costMethod = CostMethod.FIFO;
                else if (chartConfigPainting.contains(ChartDetails.PURCHASEPRICE_MA))
                    costMethod = CostMethod.MOVING_AVERAGE;

                if (costMethod != null)
                {
                    Optional<Double> purchasePrice = getLatestPurchasePrice(security, costMethod);
                    if (purchasePrice.isPresent())
                        firstQuote = purchasePrice.get();
                    else
                        showAreaRelativeToFirstQuote = false;
                }
                else
                {
                    firstQuote = referenceQuote;
                }

                addChartMarkerBackground(chartInterval, range, security, chartConfigPainting);

                for (int ii = 0; ii < range.size; ii++)
                {
                    SecurityPrice p = prices.get(ii + range.start);
                    dates[ii] = p.getDate();
                    values[ii] = isSingleSecurityMode ? p.getValue() / Values.Quote.divider()
                                    : ((p.getValue() / Values.Quote.divider() / referenceQuote) - 1);
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

                if (showAreaRelativeToFirstQuote)
                {
                    @SuppressWarnings("unchecked")
                    ILineSeries<Integer> lineSeries2ndNegative = (ILineSeries<Integer>) chart.getSeriesSet()
                                    .createSeries(SeriesType.LINE,
                                                    Messages.LabelChartDetailChartDevelopmentClosing + "Negative"); //$NON-NLS-1$
                    lineSeries2ndNegative.setSymbolType(PlotSymbolType.NONE);
                    lineSeries2ndNegative.setYAxisId(1);
                    configureSeriesPainter(lineSeries2ndNegative, dates, valuesRelativeNegative, colorQuoteAreaNegative,
                                    1, LineStyle.SOLID, true, false);

                    @SuppressWarnings("unchecked")
                    ILineSeries<Integer> lineSeries2ndPositive = (ILineSeries<Integer>) chart.getSeriesSet()
                                    .createSeries(SeriesType.LINE,
                                                    Messages.LabelChartDetailChartDevelopmentClosing + "Positive"); //$NON-NLS-1$
                    lineSeries2ndPositive.setSymbolType(PlotSymbolType.NONE);
                    lineSeries2ndPositive.setYAxisId(1);
                    configureSeriesPainter(lineSeries2ndPositive, dates, valuesRelativePositive, colorQuoteAreaPositive,
                                    1, LineStyle.SOLID, true, false);
                }

                String seriesIdent = !isSingleSecurityMode ? security.getName() : Messages.ColumnQuote; // $NON-NLS-1$
                @SuppressWarnings("unchecked")
                ILineSeries<Integer> lineSeries = (ILineSeries<Integer>) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, seriesIdent);
                lineSeries.setSymbolType(PlotSymbolType.NONE);
                Color color = isSingleSecurityMode ? colorQuote : new Color(colorWheel.next());
                boolean enableArea = !showAreaRelativeToFirstQuote && isSingleSecurityMode;
                configureSeriesPainter(lineSeries, dates, values, color, 2, LineStyle.SOLID, enableArea,
                                !isSingleSecurityMode);

                chart.adjustRange();

                addChartMarkerForeground(chartInterval, security, chartConfigPainting);

                chart.adjustRange();

                IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
                IAxis yAxis2nd = chart.getAxisSet().getYAxis(1);
                IAxis yAxis3rd = chart.getAxisSet().getYAxis(2);

                if (firstQuote == null)
                    firstQuote = (prices.get(range.start).getValue() / Values.Quote.divider());

                yAxis2nd.setRange(new Range(yAxis1st.getRange().lower - firstQuote,
                                yAxis1st.getRange().upper - firstQuote));

                if (firstQuote != 0)
                {
                    yAxis3rd.setRange(new Range(yAxis1st.getRange().lower / firstQuote - 1,
                                    yAxis1st.getRange().upper / firstQuote - 1));
                }

                yAxis1st.enableLogScale(chartConfigPainting.contains(ChartDetails.SCALING_LOG));
                yAxis2nd.enableLogScale(chartConfigPainting.contains(ChartDetails.SCALING_LOG));

                yAxis1st.getTick().setVisible(true);
                // hide percentage axis in logarithmic mode
                yAxis3rd.getTick().setVisible(chartConfigPainting.contains(ChartDetails.SHOW_PERCENTAGE_AXIS)
                                && !chartConfigPainting.contains(ChartDetails.SCALING_LOG));

                // ensure that at least one set of horizontal lines is shown
                if (!chartConfigPainting.contains(ChartDetails.SHOW_MAIN_HORIZONTAL_LINES)
                                && !chartConfigPainting.contains(ChartDetails.SHOW_PERCENTAGE_HORIZONTAL_LINES))
                    chartConfigPainting.add(ChartDetails.SHOW_MAIN_HORIZONTAL_LINES);

                if (chartConfigPainting.contains(ChartDetails.SHOW_MAIN_HORIZONTAL_LINES)
                                || !yAxis3rd.getTick().isVisible())
                    yAxis1st.getGrid().setStyle(LineStyle.DOT);
                else
                    yAxis1st.getGrid().setStyle(LineStyle.NONE);

                if (chartConfigPainting.contains(ChartDetails.SHOW_PERCENTAGE_HORIZONTAL_LINES)
                                && yAxis3rd.getTick().isVisible())
                    yAxis3rd.getGrid().setStyle(LineStyle.DOT);
                else
                    yAxis3rd.getGrid().setStyle(LineStyle.NONE);

                if (chartConfigPainting.contains(ChartDetails.SHOW_MISSING_TRADING_DAYS))
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
        }
        finally
        {
            chart.suspendUpdate(false);
            chart.setRedraw(true);
            chart.redraw();
        }
    }

    private void addChartMarkerBackground(ChartInterval chartInterval, ChartRange range, Security security,
                    EnumSet<ChartDetails> chartConfig)
    {
        if (chartConfig.contains(ChartDetails.BOLLINGERBANDS))
            addBollingerBandsMarkerLines(chartInterval, 20, 2, security);

        if (chartConfig.contains(ChartDetails.MACD))
            addMacdMarkerLines(chartInterval, colorMACD, security);

        if (chartConfig.contains(ChartDetails.SMA_5DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_5days, 5, colorSMA1, security);

        if (chartConfig.contains(ChartDetails.SMA_20DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_20days, 20, colorSMA2, security);

        if (chartConfig.contains(ChartDetails.SMA_30DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_30days, 30, colorSMA3, security);

        if (chartConfig.contains(ChartDetails.SMA_38DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_38days, 38, colorSMA4, security);

        if (chartConfig.contains(ChartDetails.SMA_50DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_50days, 50, colorSMA4, security);

        if (chartConfig.contains(ChartDetails.SMA_90DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_90days, 90, colorSMA5, security);

        if (chartConfig.contains(ChartDetails.SMA_100DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_100days, 100, colorSMA6, security);

        if (chartConfig.contains(ChartDetails.SMA_200DAYS))
            addSMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageSMA,
                            Messages.LabelChartDetailMovingAverage_200days, 200, colorSMA7, security);

        if (chartConfig.contains(ChartDetails.EMA_5DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_5days, 5, colorEMA1, security);

        if (chartConfig.contains(ChartDetails.EMA_20DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_20days, 20, colorEMA2, security);

        if (chartConfig.contains(ChartDetails.EMA_30DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_30days, 30, colorEMA3, security);

        if (chartConfig.contains(ChartDetails.EMA_38DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_38days, 38, colorEMA4, security);

        if (chartConfig.contains(ChartDetails.EMA_50DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_50days, 50, colorEMA4, security);

        if (chartConfig.contains(ChartDetails.EMA_90DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_90days, 90, colorEMA5, security);

        if (chartConfig.contains(ChartDetails.EMA_100DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_100days, 100, colorEMA6, security);

        if (chartConfig.contains(ChartDetails.EMA_200DAYS))
            addEMAMarkerLines(chartInterval, Messages.LabelChartDetailMovingAverageEMA,
                            Messages.LabelChartDetailMovingAverage_200days, 200, colorEMA7, security);

        if (this.showMarkings && chartConfig.contains(ChartDetails.SHOW_LIMITS))
            addLimitLines(range, security);
    }

    private void addChartMarkerForeground(ChartInterval chartInterval, Security security,
                    EnumSet<ChartDetails> chartConfig)
    {
        if (!this.showMarkings)
            return;

        if (chartConfig.contains(ChartDetails.FIFOPURCHASE))
            addPurchasePrice(chartInterval, security, CostMethod.FIFO);

        if (chartConfig.contains(ChartDetails.FLOATINGAVGPURCHASE))
            addPurchasePrice(chartInterval, security, CostMethod.MOVING_AVERAGE);

        if (chartConfig.contains(ChartDetails.INVESTMENT))
            addInvestmentMarkerLines(chartInterval, security, chartConfig);

        if (chartConfig.contains(ChartDetails.SHARES_HELD))
            new SharesHeldChartSeries().configure(this, chart, chartInterval, security);

        if (chartConfig.contains(ChartDetails.DIVIDENDS))
            addDividendMarkerLines(chartInterval, security, chartConfig);

        if (chartConfig.contains(ChartDetails.EVENTS))
            addEventMarkerLines(chartInterval, security);

        if (chartConfig.contains(ChartDetails.EXTREMES))
            addExtremesMarkerLines(chartInterval, security, chartConfig);
    }

    private void addLimitLines(ChartRange range, Security security)
    {
        security.getAttributes().getMap().forEach((key, val) -> {
            // null OR not Limit Price --> ignore
            if (val == null || val.getClass() != LimitPrice.class)
                return;

            LimitPrice limitAttribute = (LimitPrice) val;

            // unwrap because ReadOnlyClient only contains/provides default
            // attributes
            Optional<AttributeType> attributeName = ReadOnlyClient.unwrap(client).getSettings().getAttributeTypes()
                            .filter(attr -> attr.getId().equals(key)).findFirst();

            // could not find name of limit attribute --> don't draw
            if (attributeName.isEmpty())
                return;

            String lineID = attributeName.get().getName() + " (" + limitAttribute.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$

            // horizontal line: only two points required
            LocalDate[] dates = new LocalDate[2];
            dates[0] = range.startDate;
            dates[1] = range.endDate;

            // both points with same y-value
            double[] values = new double[2];
            values[0] = values[1] = limitAttribute.getValue() / Values.Quote.divider();

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> lineSeriesLimit = (ILineSeries<Integer>) chart.getSeriesSet()
                            .createSeries(SeriesType.LINE, lineID);
            lineSeriesLimit.setDataModel(new TimelineSeriesModel(dates, values));
            lineSeriesLimit.setLineWidth(2);
            lineSeriesLimit.setLineStyle(LineStyle.DASH);
            lineSeriesLimit.enableArea(false);
            lineSeriesLimit.setSymbolType(PlotSymbolType.NONE);
            lineSeriesLimit.setAntialias(swtAntialias);
            lineSeriesLimit.setLineColor(Colors.ICON_ORANGE);
            lineSeriesLimit.setYAxisId(0);
            lineSeriesLimit.setVisibleInLegend(true);
        });
    }

    private void addSMAMarkerLines(ChartInterval chartInterval, String smaSeries, String smaDaysWording, int smaDays,
                    Color smaColor, Security security)
    {
        ChartLineSeriesAxes smaLines = new SimpleMovingAverage(smaDays, security, chartInterval).getSMA();
        if (smaLines == null || smaLines.getValues() == null || smaLines.getDates() == null)
            return;

        String lineID = smaSeries + " (" + smaDaysWording + ")"; //$NON-NLS-1$ //$NON-NLS-2$

        @SuppressWarnings("unchecked")
        ILineSeries<Integer> lineSeriesSMA = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        lineID);
        lineSeriesSMA.setDataModel(new TimelineSeriesModel(smaLines.getDates(), smaLines.getValues()));

        lineSeriesSMA.setLineWidth(2);
        lineSeriesSMA.enableArea(false);
        lineSeriesSMA.setSymbolType(PlotSymbolType.NONE);
        lineSeriesSMA.setAntialias(swtAntialias);
        lineSeriesSMA.setLineColor(smaColor);
        lineSeriesSMA.setYAxisId(0);
        lineSeriesSMA.setVisibleInLegend(true);
    }

    private void addEMAMarkerLines(ChartInterval chartInterval, String emaSeries, String emaDaysWording, int emaDays,
                    Color emaColor, Security security)
    {
        ChartLineSeriesAxes emaLines = new ExponentialMovingAverage(emaDays, security, chartInterval).getEMA();
        if (emaLines == null || emaLines.getValues() == null || emaLines.getDates() == null)
            return;

        String lineID = emaSeries + " (" + emaDaysWording + ")"; //$NON-NLS-1$ //$NON-NLS-2$

        @SuppressWarnings("unchecked")
        ILineSeries<Integer> lineSeriesEMA = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        lineID);
        lineSeriesEMA.setDataModel(new TimelineSeriesModel(emaLines.getDates(), emaLines.getValues()));
        lineSeriesEMA.setLineWidth(2);
        lineSeriesEMA.enableArea(false);
        lineSeriesEMA.setSymbolType(PlotSymbolType.NONE);
        lineSeriesEMA.setAntialias(swtAntialias);
        lineSeriesEMA.setLineColor(emaColor);
        lineSeriesEMA.setYAxisId(0);
        lineSeriesEMA.setVisibleInLegend(true);
    }

    private void addInvestmentMarkerLines(ChartInterval chartInterval, Security security,
                    EnumSet<ChartDetails> chartConfig)
    {
        List<PortfolioTransaction> purchase = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
                        .filter(t -> chartInterval.contains(t.getDateTime())) //
                        .sorted(Transaction.BY_DATE).toList();

        addInvestmentMarkers(purchase, investmentMarkerLabelBuy, colorEventPurchase, PlotSymbolType.TRIANGLE, security,
                        chartConfig);

        List<PortfolioTransaction> sales = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == security)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.SELL
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        .filter(t -> chartInterval.contains(t.getDateTime())) //
                        .sorted(Transaction.BY_DATE).toList();

        addInvestmentMarkers(sales, investmentMarkerLabelSell, colorEventSale, PlotSymbolType.INVERTED_TRIANGLE,
                        security, chartConfig);
    }

    private void addInvestmentMarkers(List<PortfolioTransaction> transactions, String seriesLabel, Color color,
                    PlotSymbolType symbol, Security security, EnumSet<ChartDetails> chartConfig)
    {
        if (transactions.isEmpty())
            return;

        customTooltipEvents.addAll(transactions);

        if (chartConfig.contains(ChartDetails.SHOW_MARKER_LINES))
        {
            var showLabels = chartConfig.contains(ChartDetails.SHOW_DATA_DIVESTMENT_INVESTMENT_LABEL);
            transactions.forEach(t -> {
                if (showLabels)
                {
                    String label = Values.Share.format(t.getType().isPurchase() ? t.getShares() : -t.getShares());
                    double value = t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode()))
                                    .getAmount() / Values.Quote.divider();
                    chart.addMarkerLine(t.getDateTime().toLocalDate(), color, label, value);
                }
                else
                    chart.addMarkerLine(t.getDateTime().toLocalDate(), color, null);
            });
        }
        else
        {
            LocalDate[] dates = transactions.stream().map(PortfolioTransaction::getDateTime).map(d -> d.toLocalDate())
                            .toArray(size -> new LocalDate[size]);

            double[] values = transactions.stream().mapToDouble(
                            t -> t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())).getAmount()
                                            / Values.Quote.divider())
                            .toArray();

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> border = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            seriesLabel + "2"); //$NON-NLS-1$
            border.setYAxisId(0);
            border.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            border.setSymbolType(symbol);
            border.setSymbolSize(7);

            configureSeriesPainter(border, dates, values, null, 0, LineStyle.NONE, false, false);

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> background = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            seriesLabel + "1"); //$NON-NLS-1$
            background.setYAxisId(0);
            background.setSymbolType(symbol);
            background.setSymbolSize(6);
            background.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            configureSeriesPainter(background, dates, values, null, 0, LineStyle.NONE, false, false);

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> inner = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            seriesLabel);
            inner.setYAxisId(0);
            inner.setSymbolType(symbol);
            inner.setSymbolSize(4);
            inner.setSymbolColor(color);
            configureSeriesPainter(inner, dates, values, color, 0, LineStyle.NONE, false, true);

            if (chartConfig.contains(ChartDetails.SHOW_DATA_DIVESTMENT_INVESTMENT_LABEL))
            {
                customPaintListeners.add(event -> {
                    Color defaultForeground = Colors.theme().defaultForeground();
                    event.gc.setForeground(defaultForeground);

                    int symbolSize = border.getSymbolSize();
                    int lastLabelEndX = Integer.MIN_VALUE;

                    IAxis xAxis = chart.getAxisSet().getXAxis(0);
                    IAxis yAxis = chart.getAxisSet().getYAxis(0);

                    for (int index = 0; index < dates.length; index++)
                    {
                        PortfolioTransaction t = transactions.get(index);
                        if (t == null)
                            continue;

                        int x = xAxis.getPixelCoordinate(dates[index].toEpochDay());
                        int y = yAxis.getPixelCoordinate(values[index]);

                        String label = Values.Share.format(t.getType().isPurchase() ? t.getShares() : -t.getShares());

                        Point textExtent = event.gc.textExtent(label);
                        int labelWidth = textExtent.x;
                        int halfLabelWidth = labelWidth / 2;

                        int labelStartX = x - halfLabelWidth;
                        int labelEndX = x + halfLabelWidth;

                        // Check if the label starts in a non-negative position
                        // and does not overlap with the previous label
                        if (labelStartX > lastLabelEndX && labelStartX >= 0)
                        {
                            event.gc.drawText(label, x - halfLabelWidth, y + symbolSize, true);

                            // Update the end position of the last drawn label
                            lastLabelEndX = labelEndX;
                        }
                    }
                });
            }
        }
    }

    private void addDividendMarkerLines(ChartInterval chartInterval, Security security,
                    EnumSet<ChartDetails> chartConfig)
    {
        List<AccountTransaction> dividends = client.getAccounts().stream().flatMap(a -> a.getTransactions().stream()) //
                        .filter(t -> t.getSecurity() == security) //
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS) //
                        .filter(t -> chartInterval.contains(t.getDateTime())) //
                        .sorted(Transaction.BY_DATE) //
                        .toList(); //

        if (dividends.isEmpty())
            return;

        customTooltipEvents.addAll(dividends);

        if (chartConfig.contains(ChartDetails.SHOW_MARKER_LINES))
        {
            var showLabels = chartConfig.contains(ChartDetails.SHOW_DATA_DIVIDEND_LABEL);
            dividends.forEach(t -> chart.addMarkerLine(t.getDateTime().toLocalDate(), colorEventDividend,
                            showLabels ? getDividendLabel(t) : null));
        }
        else
        {
            LocalDate[] dates = dividends.stream().map(AccountTransaction::getDateTime).map(d -> d.toLocalDate())
                            .toArray(size -> new LocalDate[size]);

            IAxis yAxis1st = chart.getAxisSet().getYAxis(0);

            // Factor should be between 0.84 (for wide range charts) and 1 (for
            // small range charts) and based on ratio of lower and upper range.
            // RangeRatio can be between 0 and 1 so parameters of linear
            // function "f = a * rangeRatio + b" are a = 0.16 and b = 0.84
            var rangeRatio = Math.max(yAxis1st.getRange().lower, 0) / yAxis1st.getRange().upper;
            double factor = 0.16 * rangeRatio + 0.84;
            double yAxis1stAxisPrice = Math.max(yAxis1st.getRange().lower * factor, 0.00001);

            double[] values = new double[dates.length];
            Arrays.fill(values, yAxis1stAxisPrice);

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> border = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailMarkerDividends + "2"); //$NON-NLS-1$
            border.setYAxisId(0);
            border.setSymbolType(PlotSymbolType.SQUARE);
            border.setSymbolSize(6);
            border.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            configureSeriesPainter(border, dates, values, null, 0, LineStyle.NONE, false, false);

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> background = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailMarkerDividends + "1"); //$NON-NLS-1$
            background.setYAxisId(0);
            background.setSymbolType(PlotSymbolType.SQUARE);
            background.setSymbolSize(5);
            background.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            configureSeriesPainter(background, dates, values, null, 0, LineStyle.NONE, false, false);

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> inner = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.LabelChartDetailMarkerDividends);
            inner.setYAxisId(0);
            inner.setSymbolType(PlotSymbolType.SQUARE);
            inner.setSymbolSize(3);
            inner.setSymbolColor(colorEventDividend);
            configureSeriesPainter(inner, dates, values, null, 0, LineStyle.NONE, false, true);

            if (chartConfig.contains(ChartDetails.SHOW_DATA_DIVIDEND_LABEL))
            {
                customPaintListeners.add(event -> {
                    Color defaultForeground = Colors.theme().defaultForeground();
                    event.gc.setForeground(defaultForeground);

                    // Three levels of the label
                    int[] labelExtendX = new int[3];

                    int symbolSize = border.getSymbolSize();
                    IAxis xAxis = chart.getAxisSet().getXAxis(0);
                    IAxis yAxis = chart.getAxisSet().getYAxis(0);

                    for (int index = 0; index < dates.length; index++)
                    {
                        int x = xAxis.getPixelCoordinate(dates[index].toEpochDay());
                        int y = yAxis.getPixelCoordinate(values[index]);

                        String label = getDividendLabel(dividends.get(index));

                        // Measure the label width and height using GC
                        Point labelSize = event.gc.stringExtent(label);
                        int labelWidth = labelSize.x;
                        int labelHeight = labelSize.y;
                        int halfLabelWidth = labelWidth / 2;
                        int halfLabelHeight = labelHeight / 2;

                        for (int level = 0; level < 3; level++)
                        {
                            // If the label has a free space and does not start
                            // in the negative, we output it.
                            if ((x - halfLabelWidth) - labelExtendX[level] > 0)
                            {
                                event.gc.drawText(label, x - halfLabelWidth,
                                                y - symbolSize - labelHeight * (level + 1) - halfLabelHeight, true);

                                labelExtendX[level] = x + halfLabelWidth;
                                break;
                            }
                        }
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

    private void addEventMarkerLines(ChartInterval chartInterval, Security security)
    {
        security.getEvents().stream() //
                        .filter(e -> chartInterval.contains(e.getDate())) //
                        .filter(e -> e.getType() != SecurityEvent.Type.DIVIDEND_PAYMENT) //
                        .forEach(e -> chart.addMarkerLine(e.getDate(),
                                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY), e.getDetails()));
    }

    private void addExtremesMarkerLines(ChartInterval chartInterval, Security security,
                    EnumSet<ChartDetails> chartConfig)
    {
        Optional<SecurityPrice> max = security.getPricesIncludingLatest().stream() //
                        .filter(p -> chartInterval.contains(p.getDate())) //
                        .max(Comparator.comparing(SecurityPrice::getValue));

        Optional<SecurityPrice> min = security.getPricesIncludingLatest().stream() //
                        .filter(p -> chartInterval.contains(p.getDate())) //
                        .min(Comparator.comparing(SecurityPrice::getValue));

        max.ifPresent(high -> addExtremeMarker(high, PlotSymbolType.DIAMOND, //
                        Messages.LabelChartDetailMarkerHigh, colorExtremeMarkerHigh, chartConfig));
        min.ifPresent(low -> addExtremeMarker(low, PlotSymbolType.DIAMOND, //
                        Messages.LabelChartDetailMarkerLow, colorExtremeMarkerLow, chartConfig));
    }

    private void addExtremeMarker(SecurityPrice price, PlotSymbolType plotSymbolType, String seriesLabel, Color color,
                    EnumSet<ChartDetails> chartConfig)
    {
        LocalDate eventDate = price.getDate();
        double value = price.getValue() / Values.Quote.divider();

        if (chartConfig.contains(ChartDetails.SHOW_MARKER_LINES))
        {
            if (chartConfig.contains(ChartDetails.SHOW_DATA_EXTREMES_LABEL))
            {
                String valueFormat = Values.Quote.format(price.getValue());
                chart.addMarkerLine(eventDate, color, valueFormat, value);
            }
            else
                chart.addMarkerLine(eventDate, color, null, value);
        }
        else
        {
            String valueFormat = Values.Quote.format(price.getValue());
            LocalDate zonedDate = eventDate;

            @SuppressWarnings("unchecked")
            ILineSeries<Integer> inner = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            seriesLabel);
            inner.setYAxisId(0);
            inner.setSymbolType(plotSymbolType);
            inner.setSymbolSize(6);
            inner.setSymbolColor(color);
            configureSeriesPainter(inner, new LocalDate[] { zonedDate }, new double[] { value }, color, 0,
                            LineStyle.NONE, false, true);

            if (chartConfig.contains(ChartDetails.SHOW_DATA_EXTREMES_LABEL))
            {
                customPaintListeners.add(event -> {
                    Color defaultForeground = Colors.theme().defaultForeground();
                    event.gc.setForeground(defaultForeground);

                    IAxis xAxis = chart.getAxisSet().getXAxis(0);
                    IAxis yAxis = chart.getAxisSet().getYAxis(0);

                    int x = xAxis.getPixelCoordinate(zonedDate.toEpochDay());
                    int y = yAxis.getPixelCoordinate(value);
                    Point textExtent = event.gc.textExtent(valueFormat);
                    int labelWidth = textExtent.x;
                    int halfLabelWidth = labelWidth / 2;

                    if (inner.getSymbolColor() == colorExtremeMarkerHigh)
                        y = y - textExtent.y - inner.getSymbolSize();
                    else
                        y = y + inner.getSymbolSize();

                    // If the label does not start in negative, then we print
                    // it.
                    if (x - halfLabelWidth >= 0)
                    {
                        event.gc.drawText(valueFormat, x - halfLabelWidth, y, true);
                    }
                });
            }
        }
    }

    private void addBollingerBandsMarkerLines(ChartInterval chartInterval, int bollingerBandsDays,
                    double bollingerBandsFactor, Security security)
    {
        BollingerBands bands = new BollingerBands(bollingerBandsDays, bollingerBandsFactor, security, chartInterval);

        ChartLineSeriesAxes lowerBand = bands.getLowerBand();
        if (lowerBand == null || lowerBand.getValues() == null || lowerBand.getDates() == null)
            return;

        @SuppressWarnings("unchecked")
        ILineSeries<Integer> lineSeriesBollingerBandsLowerBand = (ILineSeries<Integer>) chart.getSeriesSet()
                        .createSeries(SeriesType.LINE, Messages.LabelChartDetailIndicatorBollingerBandsLower);

        lineSeriesBollingerBandsLowerBand
                        .setDataModel(new TimelineSeriesModel(lowerBand.getDates(), lowerBand.getValues()));

        lineSeriesBollingerBandsLowerBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsLowerBand.setLineWidth(2);
        lineSeriesBollingerBandsLowerBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsLowerBand.setAntialias(swtAntialias);
        lineSeriesBollingerBandsLowerBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsLowerBand.setYAxisId(0);
        lineSeriesBollingerBandsLowerBand.setVisibleInLegend(false);

        ChartLineSeriesAxes middleBand = bands.getMiddleBand();
        @SuppressWarnings("unchecked")
        ILineSeries<Integer> lineSeriesBollingerBandsMiddleBand = (ILineSeries<Integer>) chart.getSeriesSet()
                        .createSeries(SeriesType.LINE, Messages.LabelChartDetailIndicatorBollingerBands);

        lineSeriesBollingerBandsMiddleBand
                        .setDataModel(new TimelineSeriesModel(middleBand.getDates(), middleBand.getValues()));

        lineSeriesBollingerBandsMiddleBand.setLineWidth(2);
        lineSeriesBollingerBandsMiddleBand.setLineStyle(LineStyle.DOT);
        lineSeriesBollingerBandsMiddleBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsMiddleBand.setAntialias(swtAntialias);
        lineSeriesBollingerBandsMiddleBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsMiddleBand.setYAxisId(0);
        lineSeriesBollingerBandsMiddleBand.setVisibleInLegend(true);

        ChartLineSeriesAxes upperBand = bands.getUpperBand();
        @SuppressWarnings("unchecked")
        ILineSeries<Integer> lineSeriesBollingerBandsUpperBand = (ILineSeries<Integer>) chart.getSeriesSet()
                        .createSeries(SeriesType.LINE, Messages.LabelChartDetailIndicatorBollingerBandsUpper);

        lineSeriesBollingerBandsUpperBand
                        .setDataModel(new TimelineSeriesModel(upperBand.getDates(), upperBand.getValues()));

        lineSeriesBollingerBandsUpperBand.setLineWidth(2);
        lineSeriesBollingerBandsUpperBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsUpperBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsUpperBand.setAntialias(swtAntialias);
        lineSeriesBollingerBandsUpperBand.setLineColor(colorBollingerBands);
        lineSeriesBollingerBandsUpperBand.setYAxisId(0);
        lineSeriesBollingerBandsUpperBand.setVisibleInLegend(false);
    }

    private void addMacdMarkerLines(ChartInterval chartInterval, Color color, Security security)
    {
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security, chartInterval);
        var macdLines = macd.getMacdLine();
        var signalLines = macd.getSignalLine();

        // macd and signal lines may exist separately set - avoid creating an
        // axis if both do not exist

        if (macdLines.isEmpty() && signalLines.isEmpty())
            return;

        Supplier<IAxis> yAxisFactory = () -> {
            int yAxisId = chart.getAxisSet().createYAxis();
            IAxis yAxis = chart.getAxisSet().getYAxis(yAxisId);
            yAxis.getTitle().setVisible(false);
            yAxis.getTick().setVisible(false);
            yAxis.getGrid().setStyle(LineStyle.NONE);
            yAxis.setPosition(Position.Primary);
            return yAxis;
        };

        int yAxisId = chart.getOrCreateAxis(ChartDetails.MACD, yAxisFactory).getId();

        if (macdLines.isPresent())
        {
            @SuppressWarnings("unchecked")
            ILineSeries<Integer> lineSeriesMacd = (ILineSeries<Integer>) chart.getSeriesSet()
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailIndicatorMacd);

            lineSeriesMacd.setDataModel(
                            new TimelineSeriesModel(macdLines.get().getDates(), macdLines.get().getValues()));

            lineSeriesMacd.setLineStyle(LineStyle.SOLID);
            lineSeriesMacd.setLineWidth(2);
            lineSeriesMacd.enableArea(false);
            lineSeriesMacd.setSymbolType(PlotSymbolType.NONE);
            lineSeriesMacd.setAntialias(swtAntialias);
            lineSeriesMacd.setLineColor(color);
            lineSeriesMacd.setYAxisId(yAxisId);
            lineSeriesMacd.setVisibleInLegend(true);
        }

        if (signalLines.isPresent())
        {
            @SuppressWarnings("unchecked")
            ILineSeries<Integer> lineSeriesSignal = (ILineSeries<Integer>) chart.getSeriesSet()
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailIndicatorMacdSignal);

            lineSeriesSignal.setDataModel(
                            new TimelineSeriesModel(signalLines.get().getDates(), signalLines.get().getValues()));

            lineSeriesSignal.setLineStyle(LineStyle.DOT);
            lineSeriesSignal.setLineWidth(2);
            lineSeriesSignal.enableArea(false);
            lineSeriesSignal.setSymbolType(PlotSymbolType.NONE);
            lineSeriesSignal.setAntialias(swtAntialias);
            lineSeriesSignal.setLineColor(color);
            lineSeriesSignal.setYAxisId(yAxisId);
            lineSeriesSignal.setVisibleInLegend(false);
        }
    }

    private void addPurchasePrice(ChartInterval chartInterval, Security security, CostMethod costMethod)
    {
        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security.getCurrencyCode() == null)
            return;

        // create a list of dates that are relevant for FIFO or moving average
        // purchase price changes (i.e. all purchase and sell events)

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
                        .toList();

        var costMethodLabel = costMethod.getLabel();
        Color color;
        switch (costMethod)
        {
            case FIFO:
                color = colorFifoPurchasePrice;
                break;
            case MOVING_AVERAGE:
                color = colorMovingAveragePurchasePrice;
                break;
            default:
                throw new IllegalArgumentException("unsupported cost method: " + costMethod); //$NON-NLS-1$
        }

        // calculate FIFO or moving average purchase price for each event -
        // separate lineSeries per holding period

        List<Double> values = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        int seriesCounter = 0;

        for (LocalDate eventDate : candidates)
        {
            Optional<Double> purchasePrice = getPurchasePrice(filteredClient, securityCurrency, eventDate, security,
                            costMethod);

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

                    createPriceLineSeries(values, dates, seriesCounter++, color, MessageFormat.format(
                                    Messages.LabelWithQualifier, Messages.ColumnPurchasePrice, costMethodLabel));
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
        getPurchasePrice(filteredClient, securityCurrency, chartInterval.getEnd(), security, costMethod)
                        .ifPresent(price -> {
                            dates.add(chartInterval.getEnd());
                            values.add(price);
                        });

        if (!dates.isEmpty())
            createPriceLineSeries(values, dates, seriesCounter, color, MessageFormat.format(Messages.LabelWithQualifier,
                            Messages.ColumnPurchasePrice, costMethodLabel));
    }

    private void createPriceLineSeries(List<Double> values, List<LocalDate> dates, int seriesCounter, Color color,
                    String label)
    {
        if (seriesCounter != 0)
            label += " " + MessageFormat.format(Messages.LabelChartDetailMarkerPurchasePeriodNo, //$NON-NLS-1$
                            seriesCounter + 1);

        @SuppressWarnings("unchecked")
        ILineSeries<Integer> series = (ILineSeries<Integer>) chart.getSeriesSet().createSeries(SeriesType.LINE, label);

        series.setSymbolType(PlotSymbolType.NONE);
        series.setYAxisId(0);
        series.enableStep(true);

        configureSeriesPainter(series, dates.toArray(new LocalDate[0]), Doubles.toArray(values), color, 2,
                        LineStyle.SOLID, false, seriesCounter == 0);

        setupTooltipDisplayCalculatedQuote(label);
    }

    private Optional<Double> getLatestPurchasePrice(Security security, CostMethod costMethod)
    {
        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security.getCurrencyCode() == null)
            return Optional.empty();

        return getPurchasePrice(new ClientSecurityFilter(security).filter(client),
                        converter.with(security.getCurrencyCode()), LocalDate.now(), security, costMethod);
    }

    private Optional<Double> getPurchasePrice(Client filteredClient, CurrencyConverter currencyConverter,
                    LocalDate date, Security security, CostMethod costMethod)
    {
        var r = LazySecurityPerformanceSnapshot
                        .create(filteredClient, currencyConverter, Interval.of(LocalDate.MIN, date))
                        .getRecord(security);

        if (r.isEmpty())
            return Optional.empty();

        Quote purchasePricePerShare;
        switch (costMethod)
        {
            case FIFO:
                purchasePricePerShare = r.get().getFifoCostPerSharesHeld().get();
                break;
            case MOVING_AVERAGE:
                purchasePricePerShare = r.get().getMovingAverageCostPerSharesHeld().get();
                break;
            default:
                throw new IllegalArgumentException("unsupported cost method: " + costMethod); //$NON-NLS-1$
        }

        return purchasePricePerShare.isZero() ? Optional.empty()
                        : Optional.of(purchasePricePerShare.getAmount() / Values.Quote.divider());
    }

    private static class MessagePainter implements PaintListener, DisposeListener
    {
        private String message;
        private Font font;

        private void setMessage(String message)
        {
            this.message = message;
        }

        @Override
        public void paintControl(PaintEvent e)
        {
            if (message == null)
                return;

            if (font == null)
                font = FontDescriptor.createFrom(e.gc.getFont()).increaseHeight(5).createFont(e.display);

            Font defaultFont = e.gc.getFont();
            e.gc.setFont(font);

            Point txtExtend = e.gc.textExtent(message);
            int posX = (e.width - txtExtend.x) / 2;
            int posY = (e.height - txtExtend.y) / 2;
            e.gc.setForeground(Colors.DARK_GRAY);
            e.gc.drawText(message, posX, posY);

            e.gc.setFont(defaultFont);
        }

        @Override
        public void widgetDisposed(DisposeEvent e)
        {
            if (font != null)
                font.dispose();
        }
    }
}
