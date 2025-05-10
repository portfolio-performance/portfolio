package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.Interval;

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

    static class ChartIntervalOrMessage
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

    enum ChartDetails
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
        FIFOPURCHASE(Messages.LabelChartDetailMarkerPurchaseFIFO), //
        FLOATINGAVGPURCHASE(Messages.LabelChartDetailMarkerPurchaseMovingAverage), //
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

    private boolean showMarkings = true;

    private static final String PREF_KEY = "security-chart-details"; //$NON-NLS-1$

    private Composite container;

    private Client client;

    private SecuritiesChartPainter chartpainter;

    /**
     * Calculates dynamically for each security the interval of security prices
     * to be shown.
     */
    private IntervalOption intervalOption = IntervalOption.Y2;

    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.INVESTMENT, ChartDetails.EVENTS,
                    ChartDetails.SCALING_LINEAR, ChartDetails.SHOW_MAIN_HORIZONTAL_LINES);

    private Security[] securities;

    public SecuritiesChart(Composite parent, Client client, CurrencyConverter converter)
    {
        this.client = client;
        readChartConfig(client);

        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        this.chartpainter = new SecuritiesChartPainter(this, container, converter, client);
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
        this.chartpainter.setQuoteColor(color);
    }

    public void setQuoteAreaNegative(Color color)
    {
        this.chartpainter.setQuoteAreaNegative(color);
    }

    public void setQuoteAreaPositive(Color color)
    {
        this.chartpainter.setQuoteAreaPositive(color);
    }

    public void setPurchaseColor(Color color)
    {
        this.chartpainter.setPurchaseColor(color);
    }

    public void setSaleColor(Color color)
    {
        this.chartpainter.setSaleColor(color);
    }

    public void setDividendColor(Color color)
    {
        this.chartpainter.setDividendColor(color);
    }

    public void setExtremeMarkerHighColor(Color color)
    {
        this.chartpainter.setExtremeMarkerHighColor(color);
    }

    public void setExtremeMarkerLowColor(Color color)
    {
        this.chartpainter.setExtremeMarkerLowColor(color);
    }

    public void setNonTradingColor(Color color)
    {
        this.chartpainter.setNonTradingColor(color);
    }

    public Color getSharesHeldColor()
    {
        return chartpainter.getSharesHeldColor();
    }

    public void setSharesHeldColor(Color color)
    {
        this.chartpainter.setSharesHeldColor(color);
    }

    public Client getClient()
    {
        return this.client;
    }

    public int getAntialias()
    {
        return this.chartpainter.swtAntialias;
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

    private void updateChart()
    {
        chartpainter.updateChart(chartConfig, securities, showMarkings, intervalOption);
    }

    public void addButtons(ToolBarManager toolBar)
    {
        chartpainter.chart.getChartToolsManager().addButtons(toolBar);

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
        updateChart();
    }

    public Control getControl()
    {
        return container;
    }








}
