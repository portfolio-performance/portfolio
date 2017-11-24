package name.abuchen.portfolio.ui.views;

import java.lang.Double;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.swtchart.ILegend;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.IAxis;
import org.swtchart.Range;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.util.ColorConversion;

/**
 * Chart of historical quotes for a given security
 */
public class SecuritiesChart
{
    private enum ChartDetails
    {
        COMPACTVIEW(Messages.LabelChartDetailCompactView),
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

    private static final String SEPERATOR = "---"; //$NON-NLS-1$
    private static final String PREF_KEY = "security-chart-details"; //$NON-NLS-1$

    private Menu contextMenu;

    private Client client;
    private CurrencyConverter converter;
    private Security security;

    private TimelineChart chart;
    private LocalDate chartPeriod = LocalDate.now().minusYears(2);
    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.INVESTMENT, ChartDetails.EVENTS);

    public SecuritiesChart(Composite parent, Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;

        readChartConfig(client);

        chart = new TimelineChart(parent);
        chart.getTitle().setText("..."); //$NON-NLS-1$
        chart.getToolTip().setValueFormat(new DecimalFormat(Values.Quote.pattern()));
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailClosingIndicator+"Positive"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailClosingIndicator+"Negative"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailClosingIndicator+"Zero"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuBuy + "1"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuBuy + "2"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuSell + "1"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuSell + "2"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailDividends); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailDividends + "1"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailDividends + "2"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailBollingerBands); //$NON-NLS-1$
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

    private static final class JSColors
    {
        private static final int SIZE = 11;
        private static final float STEP = 360.0f / (float) SIZE;

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        public int[] byIndex(int colorIndex, float setSaturation)
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (colorIndex / SIZE)));
            return ColorConversion.toRGB(ColorConversion.toHex((HUE + (STEP * colorIndex)) % 360f, setSaturation, brightness));
        }
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

            if (security == null || security.getPrices().isEmpty())
            {
                chart.getTitle().setText(security == null ? "..." : security.getName()); //$NON-NLS-1$
                chart.redraw();
                return;
            }

            chart.getTitle().setText(security.getName());

            boolean showAreaRelativeToFirstQuote = chartConfig.contains(ChartDetails.CLOSING);

            List<SecurityPrice> prices = security.getPricesIncludingLatest();

            int index;
            LocalDate[] dates;

            double[] values;
            double[] valuesRelative;
            double[] valuesRelativePositive;
            double[] valuesRelativeNegative;
            double[] valuesZeroLine;
            double firstQuote;

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
                firstQuote = getLatestPurchasePrice();
                if (Double.isInfinite(firstQuote) || firstQuote == 0) showAreaRelativeToFirstQuote = false;
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
            JSColors colors = new JSColors();
            int[] rgbColor;
            if (showAreaRelativeToFirstQuote)
            {
                ILineSeries lineSeries2ndPositive = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailClosingIndicator+"Positive");
                lineSeries2ndPositive.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
                lineSeries2ndPositive.enableArea(true);
                lineSeries2ndPositive.setSymbolType(PlotSymbolType.NONE);
                lineSeries2ndPositive.setYSeries(valuesRelativePositive);
                lineSeries2ndPositive.setAntialias(SWT.ON);
                lineSeries2ndPositive.setYAxisId(1);
                lineSeries2ndPositive.setVisibleInLegend(false);
                rgbColor = colors.byIndex(10, 0.6f);
                lineSeries2ndPositive.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));

                ILineSeries lineSeries2ndNegative = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailClosingIndicator+"Negative");
                lineSeries2ndNegative.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
                lineSeries2ndNegative.enableArea(true);
                lineSeries2ndNegative.setSymbolType(PlotSymbolType.NONE);
                lineSeries2ndNegative.setYSeries(valuesRelativeNegative);
                lineSeries2ndNegative.setAntialias(SWT.ON);
                lineSeries2ndNegative.setYAxisId(1);
                lineSeries2ndNegative.setVisibleInLegend(false);
                rgbColor = colors.byIndex(3, 0.6f);
                lineSeries2ndNegative.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));

                ILineSeries lineSeries2ndZero = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailClosingIndicator+"Zero");
                lineSeries2ndZero.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
                lineSeries2ndZero.enableArea(true);
                lineSeries2ndZero.setSymbolType(PlotSymbolType.NONE);
                lineSeries2ndZero.setYSeries(valuesZeroLine);
                lineSeries2ndZero.setAntialias(SWT.ON);
                lineSeries2ndZero.setYAxisId(1);
                lineSeries2ndZero.setVisibleInLegend(false);
                lineSeries2ndZero.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }

            ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.ColumnQuote);
            lineSeries.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
            lineSeries.setLineWidth(2);
            lineSeries.enableArea(!showAreaRelativeToFirstQuote);
            lineSeries.setSymbolType(PlotSymbolType.NONE);
            lineSeries.setYSeries(values);
            lineSeries.setAntialias(SWT.ON);
            lineSeries.setVisibleInLegend(false);
            rgbColor = colors.byIndex(10, 0.464f);
            lineSeries.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));

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
    private void addSMAMarkerLines(int SMADays)
    {
        JSColors colors = new JSColors();
        int[] rgbColor;
        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(SMADays, this.security, chartPeriod).getSMA();
        if (SMALines == null || SMALines.getValues() == null || SMALines.getDates() == null)
            return;

        String lineID = SMADays == 200 ? Messages.LabelChartDetailSMA200 : Messages.LabelChartDetailSMA50;

        ILineSeries lineSeriesSMA = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, lineID);
        lineSeriesSMA.setXDateSeries(SMALines.getDates());
        lineSeriesSMA.setLineWidth(2);
        lineSeriesSMA.enableArea(false);
        lineSeriesSMA.setSymbolType(PlotSymbolType.NONE);
        lineSeriesSMA.setYSeries(SMALines.getValues());
        lineSeriesSMA.setAntialias(SWT.ON);
        rgbColor = colors.byIndex(SMADays == 200 ? 7:0, 0.464f);
        lineSeriesSMA.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));
        lineSeriesSMA.setYAxisId(0);
        lineSeriesSMA.setVisibleInLegend(true);

    }

    private void addInvestmentMarkerLines()
    {
        List<LocalDate> mapDatesBuyTemp = new ArrayList<> ();
        List<LocalDate> mapDatesSellTemp = new ArrayList<> ();
        List<Double> mapPriceBuyTemp = new ArrayList<> ();
        List<Double> mapPriceSellTemp = new ArrayList<> ();
        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getSecurity() == security && (chartPeriod == null || chartPeriod.isBefore(t.getDate())))
                {
                    if((t.getType() != name.abuchen.portfolio.model.PortfolioTransaction.Type.TRANSFER_IN) &&  
                                    (t.getType() != name.abuchen.portfolio.model.PortfolioTransaction.Type.TRANSFER_OUT))
                    {
                        if (!chartConfig.contains(ChartDetails.COMPACTVIEW))
                        {
                            String label = Values.Share.format(t.getType().isPurchase() ? t.getShares() : -t.getShares());  
                            Color color = Display.getDefault().getSystemColor(  
                                            t.getType().isPurchase() ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);  
                            double value = t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode()))  
                                            .getAmount() / Values.Quote.divider();  
                            chart.addMarkerLine(t.getDate(), color, label, value);
                        }
                        else
                        {
                            if (t.getType().isPurchase()) {
                                mapDatesBuyTemp.add(t.getDate());
                                mapPriceBuyTemp.add(t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode()))
                                                .getAmount() / Values.Quote.divider());
                                }
                            else {
                                mapDatesSellTemp.add(t.getDate());
                                mapPriceSellTemp.add(t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode()))
                                                .getAmount() / Values.Quote.divider());
                            }
                        }

                    }
                }
            }
        }
        if (chartConfig.contains(ChartDetails.COMPACTVIEW))
        {
            if (!mapDatesBuyTemp.isEmpty()) {
                LocalDate[] mapDatesBuy;
                mapDatesBuy = new LocalDate[mapDatesBuyTemp.size()];
                mapDatesBuy = mapDatesBuyTemp.toArray(mapDatesBuy);
                double[] mapPriceBuy = ArrayUtils.toPrimitive(mapPriceBuyTemp.toArray(new Double[mapPriceBuyTemp.size()]));

                ILineSeries lineSeriesBuyBorder= (ILineSeries) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, Messages.SecurityMenuBuy + "2"); //$NON-NLS-1$
                lineSeriesBuyBorder.setLineStyle(LineStyle.NONE);
                lineSeriesBuyBorder.setXDateSeries(TimelineChart.toJavaUtilDate(mapDatesBuy));
                lineSeriesBuyBorder.setYSeries(mapPriceBuy);
                lineSeriesBuyBorder.setYAxisId(0);
                lineSeriesBuyBorder.setSymbolType(PlotSymbolType.DIAMOND);
                lineSeriesBuyBorder.setSymbolSize(7);
                lineSeriesBuyBorder.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                lineSeriesBuyBorder.setVisibleInLegend(false);

                ILineSeries lineSeriesBuyBackground = (ILineSeries) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, Messages.SecurityMenuBuy + "1"); //$NON-NLS-1$
                lineSeriesBuyBackground.setLineStyle(LineStyle.NONE);
                lineSeriesBuyBackground.setXDateSeries(TimelineChart.toJavaUtilDate(mapDatesBuy));
                lineSeriesBuyBackground.setYSeries(mapPriceBuy);
                lineSeriesBuyBackground.setYAxisId(0);
                lineSeriesBuyBackground.setSymbolType(PlotSymbolType.DIAMOND);
                lineSeriesBuyBackground.setSymbolSize(6);
                lineSeriesBuyBackground.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
                lineSeriesBuyBackground.setVisibleInLegend(false);

                ILineSeries lineSeriesBuy = (ILineSeries) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, Messages.SecurityMenuBuy);
                lineSeriesBuy.setLineStyle(LineStyle.NONE);
                lineSeriesBuy.setXDateSeries(TimelineChart.toJavaUtilDate(mapDatesBuy));
                lineSeriesBuy.setYSeries(mapPriceBuy);
                lineSeriesBuy.setYAxisId(0);
                lineSeriesBuy.setSymbolType(PlotSymbolType.DIAMOND);
                lineSeriesBuy.setSymbolSize(4);
                lineSeriesBuy.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
                lineSeriesBuy.setVisibleInLegend(true);
            }

            if (!mapDatesSellTemp.isEmpty()) {
                LocalDate[] mapDatesSell;
                mapDatesSell = new LocalDate[mapDatesSellTemp.size()];
                mapDatesSell = mapDatesSellTemp.toArray(mapDatesSell);
                double[] mapPriceSell = ArrayUtils.toPrimitive(mapPriceSellTemp.toArray(new Double[mapPriceSellTemp.size()]));

                ILineSeries lineSeriesSellBorder = (ILineSeries) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, Messages.SecurityMenuSell + "2"); //$NON-NLS-1$
                lineSeriesSellBorder.setLineStyle(LineStyle.NONE);
                lineSeriesSellBorder.setXDateSeries(TimelineChart.toJavaUtilDate(mapDatesSell));
                lineSeriesSellBorder.setYSeries(mapPriceSell);
                lineSeriesSellBorder.setYAxisId(0);
                lineSeriesSellBorder.setSymbolType(PlotSymbolType.DIAMOND);
                lineSeriesSellBorder.setSymbolSize(7);
                lineSeriesSellBorder.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                lineSeriesSellBorder.setVisibleInLegend(false);

                ILineSeries lineSeriesSellBackground = (ILineSeries) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, Messages.SecurityMenuSell + "1"); //$NON-NLS-1$
                lineSeriesSellBackground.setLineStyle(LineStyle.NONE);
                lineSeriesSellBackground.setXDateSeries(TimelineChart.toJavaUtilDate(mapDatesSell));
                lineSeriesSellBackground.setYSeries(mapPriceSell);
                lineSeriesSellBackground.setYAxisId(0);
                lineSeriesSellBackground.setSymbolType(PlotSymbolType.DIAMOND);
                lineSeriesSellBackground.setSymbolSize(6);
                lineSeriesSellBackground.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
                lineSeriesSellBackground.setVisibleInLegend(false);

                ILineSeries lineSeriesSell = (ILineSeries) chart.getSeriesSet()
                                .createSeries(SeriesType.LINE, Messages.SecurityMenuSell);
                lineSeriesSell.setLineStyle(LineStyle.NONE);
                lineSeriesSell.setXDateSeries(TimelineChart.toJavaUtilDate(mapDatesSell));
                lineSeriesSell.setYSeries(mapPriceSell);
                lineSeriesSell.setYAxisId(0);
                lineSeriesSell.setSymbolType(PlotSymbolType.DIAMOND);
                lineSeriesSell.setSymbolSize(4);
                lineSeriesSell.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                lineSeriesSell.setVisibleInLegend(true);
            }
        }
    }

    private void addDividendMarkerLines()
    {
        IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
        Double yAxis1stAxisPrice = yAxis1st.getRange().lower;

        Color color = Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA); 
        List<LocalDate> dividendDate = new ArrayList<> ();
        List<Double> dividendAxisValue = new ArrayList<> ();
        for (Account account : this.client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getSecurity() == security && (chartPeriod == null || chartPeriod.isBefore(t.getDate())))
                {
                    if (t.getType() == AccountTransaction.Type.DIVIDENDS) {
                        dividendDate.add(t.getDate());
                        dividendAxisValue.add(yAxis1stAxisPrice);
                        if (!chartConfig.contains(ChartDetails.COMPACTVIEW))
                        {
                            if (t.getShares() == 0L)  
                            {
                                chart.addMarkerLine(t.getDate(), color, "\u2211 " + t.getGrossValue().toString()); //$NON-NLS-1$
                            }
                            else
                            {
                                Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
                                long gross = grossValue.isPresent() ? grossValue.get().getForex().getAmount()
                                                : t.getGrossValueAmount();

                                long perShare = Math.round(gross * Values.Share.divider() * Values.Quote.factorToMoney()
                                                / t.getShares());

                                chart.addMarkerLine(t.getDate(), color, Values.Quote.format(perShare));
                            }
                        }
                    }
                }
            }
        }

        if (chartConfig.contains(ChartDetails.COMPACTVIEW) && !dividendDate.isEmpty()) {
            LocalDate[] dividendDateTemp;
            dividendDateTemp = new LocalDate[dividendDate.size()];
            dividendDateTemp = dividendDate.toArray(dividendDateTemp);
            double[] dividendAxisValueTemp = ArrayUtils.toPrimitive(dividendAxisValue.toArray(new Double[dividendAxisValue.size()]));

            ILineSeries lineSeriesBorder = (ILineSeries) chart.getSeriesSet()  
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailDividends + "2"); //$NON-NLS-1$
            lineSeriesBorder.setLineStyle(LineStyle.NONE);
            lineSeriesBorder.setXDateSeries(TimelineChart.toJavaUtilDate(dividendDateTemp));
            lineSeriesBorder.setYSeries(dividendAxisValueTemp);
            lineSeriesBorder.setYAxisId(0);
            lineSeriesBorder.setSymbolType(PlotSymbolType.SQUARE);
            lineSeriesBorder.setSymbolSize(6);
            lineSeriesBorder.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            lineSeriesBorder.setVisibleInLegend(false);

            ILineSeries lineSeriesBackground = (ILineSeries) chart.getSeriesSet()  
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailDividends + "1"); //$NON-NLS-1$
            lineSeriesBackground.setLineStyle(LineStyle.NONE);
            lineSeriesBackground.setXDateSeries(TimelineChart.toJavaUtilDate(dividendDateTemp));
            lineSeriesBackground.setYSeries(dividendAxisValueTemp);
            lineSeriesBackground.setYAxisId(0);
            lineSeriesBackground.setSymbolType(PlotSymbolType.SQUARE);
            lineSeriesBackground.setSymbolSize(5);
            lineSeriesBackground.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            lineSeriesBackground.setVisibleInLegend(false);

            ILineSeries lineSeriesDividend = (ILineSeries) chart.getSeriesSet()  
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailDividends);
            lineSeriesDividend.setLineStyle(LineStyle.NONE);
            lineSeriesDividend.setXDateSeries(TimelineChart.toJavaUtilDate(dividendDateTemp));
            lineSeriesDividend.setYSeries(dividendAxisValueTemp);
            lineSeriesDividend.setYAxisId(0);
            lineSeriesDividend.setSymbolType(PlotSymbolType.SQUARE);
            lineSeriesDividend.setSymbolSize(3);
            lineSeriesDividend.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA));
            lineSeriesDividend.setVisibleInLegend(true);
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

        JSColors colors = new JSColors();
        int[] rgbColor;
        rgbColor = colors.byIndex(4, 0.464f);

        ILineSeries lineSeriesBollingerBandsLowerBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailBollingerBandsLower);
        lineSeriesBollingerBandsLowerBand.setXDateSeries(bollingerBandsLowerBand.getDates());
        lineSeriesBollingerBandsLowerBand.setLineWidth(2);
        lineSeriesBollingerBandsLowerBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsLowerBand.enableArea(false);
        lineSeriesBollingerBandsLowerBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsLowerBand.setYSeries(bollingerBandsLowerBand.getValues());
        lineSeriesBollingerBandsLowerBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsLowerBand.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));
        lineSeriesBollingerBandsLowerBand.setYAxisId(0);
        lineSeriesBollingerBandsLowerBand.setVisibleInLegend(false);

        ChartLineSeriesAxes bollingerBandsMiddleBand = new BollingerBands(bollingerBandsDays, bollingerBandsFactor,
                        this.security, chartPeriod).getMiddleBands();
        ILineSeries lineSeriesBollingerBandsMiddleBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailBollingerBands);
        lineSeriesBollingerBandsMiddleBand.setXDateSeries(bollingerBandsMiddleBand.getDates());
        lineSeriesBollingerBandsMiddleBand.setLineWidth(2);
        lineSeriesBollingerBandsMiddleBand.setLineStyle(LineStyle.DOT);
        lineSeriesBollingerBandsMiddleBand.enableArea(false);
        lineSeriesBollingerBandsMiddleBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsMiddleBand.setYSeries(bollingerBandsMiddleBand.getValues());
        lineSeriesBollingerBandsMiddleBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsMiddleBand.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));
        lineSeriesBollingerBandsMiddleBand.setYAxisId(0);
        lineSeriesBollingerBandsMiddleBand.setVisibleInLegend(true);

        ChartLineSeriesAxes bollingerBandsUpperBand = new BollingerBands(bollingerBandsDays, bollingerBandsFactor,
                        this.security, chartPeriod).getUpperBands();
        ILineSeries lineSeriesBollingerBandsUpperBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailBollingerBandsUpper);
        lineSeriesBollingerBandsUpperBand.setXDateSeries(bollingerBandsUpperBand.getDates());
        lineSeriesBollingerBandsUpperBand.setLineWidth(2);
        lineSeriesBollingerBandsUpperBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsUpperBand.enableArea(false);
        lineSeriesBollingerBandsUpperBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsUpperBand.setYSeries(bollingerBandsUpperBand.getValues());
        lineSeriesBollingerBandsUpperBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsUpperBand.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));
        lineSeriesBollingerBandsUpperBand.setYAxisId(0);
        lineSeriesBollingerBandsUpperBand.setVisibleInLegend(false);
    }

    private void addFIFOPurchasePrice()
    {
        Map<LocalDate, Double> purchaseDeltaValueMapTemp = new HashMap<LocalDate, Double>(); 
        Map<LocalDate, Double> purchaseShareMapTemp = new HashMap<LocalDate, Double>();
        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getSecurity() == security)
                {
                    if((t.getType() != name.abuchen.portfolio.model.PortfolioTransaction.Type.TRANSFER_IN) &&  
                                    (t.getType() != name.abuchen.portfolio.model.PortfolioTransaction.Type.TRANSFER_OUT))
                    {
                        double share = t.getShares() / Values.Share.divider();
                        share = t.getType().isPurchase() ? share : -share;
                        double amount = (t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())).getAmount() / Values.Quote.divider()) * share;
                        if (purchaseDeltaValueMapTemp.containsKey(t.getDate())) {
                            // retrieve existing share and values
                            double getAmount = purchaseDeltaValueMapTemp.get(t.getDate());
                            double getShare = purchaseShareMapTemp.get(t.getDate());

                            // add additional share and value
                            purchaseDeltaValueMapTemp.put(t.getDate(), getAmount + amount);
                            if (getShare >= 0) purchaseShareMapTemp.put(t.getDate(), getShare + (t.getType().isPurchase() ? share : -share));
                            else purchaseShareMapTemp.put(t.getDate(), getShare - (t.getType().isPurchase() ? share : -share));
                        }
                        else
                        {
                            // add initial share and value
                            purchaseDeltaValueMapTemp.put(t.getDate(), amount);
                            purchaseShareMapTemp.put(t.getDate(), share);
                        }
                    }
                }
            }
        }

        if (!purchaseDeltaValueMapTemp.isEmpty()) {
            NavigableMap<LocalDate, Double> purchaseDeltaValueMap = new TreeMap(purchaseDeltaValueMapTemp);
            NavigableMap<LocalDate, Double> purchaseShareMap = new TreeMap(purchaseShareMapTemp);

            int lineSeriesCounter = 0;
            double fifoValue = 0;
            double fifoShare = 0;
            JSColors colors = new JSColors();
            int[] rgbColor;
            rgbColor = colors.byIndex(3, 0.464f);
            for (Map.Entry<LocalDate, Double> e : purchaseDeltaValueMap.entrySet()) {
                Map.Entry<LocalDate, Double> next = purchaseDeltaValueMap.higherEntry(e.getKey()); // next

                LocalDate startDate = e.getKey();
                fifoValue = fifoValue + e.getValue(); 
                fifoShare = fifoShare + purchaseShareMap.get(e.getKey());
                double fifoValuePerShare = fifoValue / fifoShare;
                if (fifoShare == 0) {
                    fifoValue = 0;
                    continue;
                }
                if (fifoValue < 0) fifoValue=-fifoValue;
                int daysBetween = (int)ChronoUnit.DAYS.between(e.getKey(), next == null ? LocalDate.now() : next.getKey());
                if (daysBetween==0) continue;
                List<LocalDate> datesChartTemp = new ArrayList<> ();
                List<Double> valuesChartTemp = new ArrayList<> ();
                for (int relevantDays=0; relevantDays < daysBetween; relevantDays++) {
                    if ((chartPeriod == null) || (startDate.isAfter(chartPeriod) || startDate.isEqual(chartPeriod)))
                    {
                        datesChartTemp.add(startDate);
                        valuesChartTemp.add(fifoValuePerShare);
                    }
                    startDate = startDate.plusDays(1);
                }
                if (!datesChartTemp.isEmpty())
                {
                    LocalDate[] datesChart;
                    datesChart = new LocalDate[datesChartTemp.size()];
                    datesChart = datesChartTemp.toArray(datesChart);
                    double[] valuesChart = ArrayUtils.toPrimitive(valuesChartTemp.toArray(new Double[valuesChartTemp.size()]));
                    lineSeriesCounter++;
                    ILineSeries FIFOlineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                    Messages.LabelChartDetailFIFOpurchase + (lineSeriesCounter == 1 ? "" : " (" + lineSeriesCounter + ")"));
                    FIFOlineSeries.setXDateSeries(TimelineChart.toJavaUtilDate(datesChart));
                    FIFOlineSeries.setYSeries(valuesChart);
                    FIFOlineSeries.setLineWidth(2);
                    FIFOlineSeries.setSymbolType(PlotSymbolType.NONE);
                    FIFOlineSeries.setLineColor(new Color(Display.getDefault(), new RGB(rgbColor[0], rgbColor[1], rgbColor[2])));
                    FIFOlineSeries.setYAxisId(0);
                    FIFOlineSeries.setVisibleInLegend(lineSeriesCounter == 1 ? true : false);
                }
            }
        }
    }

    private double getLatestPurchasePrice()
    {
        Map<LocalDate, Double> purchaseDeltaValueMapTemp = new HashMap<LocalDate, Double>(); 
        Map<LocalDate, Double> purchaseShareMapTemp = new HashMap<LocalDate, Double>();
        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getSecurity() == security)
                {
                    if((t.getType() != name.abuchen.portfolio.model.PortfolioTransaction.Type.TRANSFER_IN) &&  
                                    (t.getType() != name.abuchen.portfolio.model.PortfolioTransaction.Type.TRANSFER_OUT))
                    {
                        double share = t.getShares() / Values.Share.divider();
                        share = t.getType().isPurchase() ? share : -share;
                        double amount = (t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())).getAmount() / Values.Quote.divider()) * share;
                        if (purchaseDeltaValueMapTemp.containsKey(t.getDate())) {
                            // retrieve existing share and values
                            double getAmount = purchaseDeltaValueMapTemp.get(t.getDate());
                            double getShare = purchaseShareMapTemp.get(t.getDate());

                            // add additional share and value
                            purchaseDeltaValueMapTemp.put(t.getDate(), getAmount + amount);
                            if (getShare >= 0) purchaseShareMapTemp.put(t.getDate(), getShare + (t.getType().isPurchase() ? share : -share));
                            else purchaseShareMapTemp.put(t.getDate(), getShare - (t.getType().isPurchase() ? share : -share));
                        }
                        else
                        {
                            // add initial share and value
                            purchaseDeltaValueMapTemp.put(t.getDate(), amount);
                            purchaseShareMapTemp.put(t.getDate(), share);
                        }
                    }
                }
            }
        }
        double fifoValue = 0;
        double fifoShare = 0;
        double fifoValuePerShare = 0;
        if (!purchaseDeltaValueMapTemp.isEmpty()) {
            NavigableMap<LocalDate, Double> purchaseDeltaValueMap = new TreeMap(purchaseDeltaValueMapTemp);
            NavigableMap<LocalDate, Double> purchaseShareMap = new TreeMap(purchaseShareMapTemp);

            for (Map.Entry<LocalDate, Double> e : purchaseDeltaValueMap.entrySet()) {
                fifoValue = fifoValue + e.getValue(); 
                fifoShare = fifoShare + purchaseShareMap.get(e.getKey());
                fifoValuePerShare = Double.isInfinite(fifoValue / fifoShare) ? 0 : fifoValue / fifoShare;
                if (fifoShare == 0) {
                    fifoValue = 0;
                    continue;
                }
                if (fifoValue < 0) fifoValue=-fifoValue;
            }
        }

        return (fifoValuePerShare);
    }
}