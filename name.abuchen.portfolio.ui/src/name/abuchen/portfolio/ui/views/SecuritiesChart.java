package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;

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
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

/**
 * Chart of historical quotes for a given security
 */
public class SecuritiesChart
{
    private enum ChartDetails
    {
        CLOSING(Messages.LabelChartDetailClosingIndicator + SEPERATOR), //
        INVESTMENT(Messages.LabelChartDetailInvestments), //
        DIVIDENDS(Messages.LabelChartDetailDividends), //
        EVENTS(Messages.LabelChartDetailEvents + SEPERATOR), //
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
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailClosingIndicator);
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuBuy + "1"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuBuy + "2"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuSell + "1"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.SecurityMenuSell + "2"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailDividends); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailDividends + "1"); //$NON-NLS-1$
        chart.getToolTip().addSeriesExclude(Messages.LabelChartDetailDividends + "2"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

        ILegend legend = chart.getLegend();
        legend.setPosition(SWT.BOTTOM);
        legend.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
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
            double[] values2nd;
            double firstQuote;

            if (chartPeriod == null)
            {
                index = 0;
                dates = new LocalDate[prices.size()];
                values = new double[prices.size()];
                values2nd = new double[prices.size()];
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
                values2nd = new double[prices.size() - index];
            }

            SecurityPrice p2 = prices.get(index);
            firstQuote = (p2.getValue() / Values.Quote.divider());

            for (int ii = 0; index < prices.size(); index++, ii++)
            {
                SecurityPrice p = prices.get(index);
                dates[ii] = p.getDate();
                values[ii] = p.getValue() / Values.Quote.divider();
                values2nd[ii] = (p.getValue() / Values.Quote.divider()) - firstQuote;
            }

            ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                            Messages.ColumnQuote);
            lineSeries.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
            lineSeries.setLineWidth(2);
            if (!chartConfig.contains(ChartDetails.BOLLINGERBANDS))
                lineSeries.enableArea(!showAreaRelativeToFirstQuote);
            lineSeries.setSymbolType(PlotSymbolType.NONE);
            lineSeries.setYSeries(values);
            lineSeries.setAntialias(SWT.ON);
            lineSeries.setVisibleInLegend(false);

            if (showAreaRelativeToFirstQuote)
            {
                ILineSeries lineSeries2nd = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                                Messages.LabelChartDetailClosingIndicator);
                lineSeries2nd.setLineWidth(2);
                lineSeries2nd.setXDateSeries(TimelineChart.toJavaUtilDate(dates));
                if (!chartConfig.contains(ChartDetails.BOLLINGERBANDS))
                    lineSeries2nd.enableArea(true);
                lineSeries2nd.setSymbolType(PlotSymbolType.NONE);
                lineSeries2nd.setYSeries(values2nd);
                lineSeries2nd.setAntialias(SWT.ON);
                lineSeries2nd.setYAxisId(1);
                lineSeries2nd.setVisibleInLegend(false);
            }

            chart.adjustRange();

            addChartMarker();

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

    private void addChartMarker()
    {
        if (chartConfig.contains(ChartDetails.INVESTMENT))
            addInvestmentMarkerLines();

        if (chartConfig.contains(ChartDetails.DIVIDENDS))
            addDividendMarkerLines();

        if (chartConfig.contains(ChartDetails.EVENTS))
            addEventMarkerLines();

        if (chartConfig.contains(ChartDetails.SMA50))
            addSMAMarkerLines(50);

        if (chartConfig.contains(ChartDetails.SMA200))
            addSMAMarkerLines(200);

        if (chartConfig.contains(ChartDetails.BOLLINGERBANDS))
            addBollingerBandsMarkerLines(20, 2);
    }

    private void addSMAMarkerLines(int SMADays)
    {
        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(SMADays, this.security, chartPeriod).getSMA();
        if (SMALines == null || SMALines.getValues() == null || SMALines.getDates() == null)
            return;

        String lineID = SMADays == 200 ? Messages.LabelChartDetailSMA200 : Messages.LabelChartDetailSMA50;

        ILineSeries lineSeriesSMA = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, lineID);
        lineSeriesSMA.setXDateSeries(SMALines.getDates());
        lineSeriesSMA.setLineWidth(1);
        lineSeriesSMA.enableArea(false);
        lineSeriesSMA.setSymbolType(PlotSymbolType.NONE);
        lineSeriesSMA.setYSeries(SMALines.getValues());
        lineSeriesSMA.setAntialias(SWT.ON);
        lineSeriesSMA.setLineColor(
                        Display.getDefault().getSystemColor(SMADays == 200 ? SWT.COLOR_RED : SWT.COLOR_GREEN));
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
            lineSeriesBuy.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
            lineSeriesBuy.setVisibleInLegend(true);
            lineSeriesBuy.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
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
            lineSeriesSell.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
            lineSeriesSell.setVisibleInLegend(true);
            lineSeriesSell.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
        }
    }

    private void addDividendMarkerLines()
    {
        IAxis yAxis1st = chart.getAxisSet().getYAxis(0);
        Double LowerDividenPrice = yAxis1st.getRange().lower;

        Map<LocalDate, Double> mapDividend = new HashMap<LocalDate, Double>(); 
        for (Account account : this.client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getSecurity() == security && (chartPeriod == null || chartPeriod.isBefore(t.getDate())))
                {
                    if (t.getType() == AccountTransaction.Type.DIVIDENDS) {
                        mapDividend.put(t.getDate(), LowerDividenPrice);
                    }
                }
            }
        }

        if (!mapDividend.isEmpty()) {
            Map<LocalDate, Double> mapDividendTemp = new TreeMap(mapDividend);
            LocalDate[] datesDividend = mapDividendTemp.keySet().toArray(new LocalDate[mapDividendTemp.size()]);
            Double[] priceDividendTemp = mapDividendTemp.values().toArray(new Double[mapDividendTemp.size()]);
            double[] priceDividend = ArrayUtils.toPrimitive(priceDividendTemp);

            ILineSeries lineSeriesBorder = (ILineSeries) chart.getSeriesSet()  
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailDividends + "2"); //$NON-NLS-1$
            lineSeriesBorder.setLineStyle(LineStyle.NONE);
            lineSeriesBorder.setXDateSeries(TimelineChart.toJavaUtilDate(datesDividend));
            lineSeriesBorder.setYSeries(priceDividend);
            lineSeriesBorder.setYAxisId(0);
            lineSeriesBorder.setSymbolType(PlotSymbolType.SQUARE);
            lineSeriesBorder.setSymbolSize(7);
            lineSeriesBorder.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            lineSeriesBorder.setVisibleInLegend(false);

            ILineSeries lineSeriesBackground = (ILineSeries) chart.getSeriesSet()  
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailDividends + "1"); //$NON-NLS-1$
            lineSeriesBackground.setLineStyle(LineStyle.NONE);
            lineSeriesBackground.setXDateSeries(TimelineChart.toJavaUtilDate(datesDividend));
            lineSeriesBackground.setYSeries(priceDividend);
            lineSeriesBackground.setYAxisId(0);
            lineSeriesBackground.setSymbolType(PlotSymbolType.SQUARE);
            lineSeriesBackground.setSymbolSize(6);
            lineSeriesBackground.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            lineSeriesBackground.setVisibleInLegend(false);

            ILineSeries lineSeriesDividend = (ILineSeries) chart.getSeriesSet()  
                            .createSeries(SeriesType.LINE, Messages.LabelChartDetailDividends);
            lineSeriesDividend.setLineStyle(LineStyle.NONE);
            lineSeriesDividend.setXDateSeries(TimelineChart.toJavaUtilDate(datesDividend));
            lineSeriesDividend.setYSeries(priceDividend);
            lineSeriesDividend.setYAxisId(0);
            lineSeriesDividend.setSymbolType(PlotSymbolType.SQUARE);
            lineSeriesDividend.setSymbolSize(4);
            lineSeriesDividend.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA));
            lineSeriesDividend.setVisibleInLegend(true);
            lineSeriesDividend.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA));
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
        lineSeriesBollingerBandsLowerBand.setLineWidth(1);
        lineSeriesBollingerBandsLowerBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsLowerBand.enableArea(false);
        lineSeriesBollingerBandsLowerBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsLowerBand.setYSeries(bollingerBandsLowerBand.getValues());
        lineSeriesBollingerBandsLowerBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsLowerBand.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
        lineSeriesBollingerBandsLowerBand.setYAxisId(0);
        lineSeriesBollingerBandsLowerBand.setVisibleInLegend(true);

        ChartLineSeriesAxes bollingerBandsUpperBand = new BollingerBands(bollingerBandsDays, bollingerBandsFactor,
                        this.security, chartPeriod).getUpperBands();
        ILineSeries lineSeriesBollingerBandsUpperBand = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.LabelChartDetailBollingerBandsUpper);
        lineSeriesBollingerBandsUpperBand.setXDateSeries(bollingerBandsUpperBand.getDates());
        lineSeriesBollingerBandsUpperBand.setLineWidth(1);
        lineSeriesBollingerBandsUpperBand.setLineStyle(LineStyle.SOLID);
        lineSeriesBollingerBandsUpperBand.enableArea(false);
        lineSeriesBollingerBandsUpperBand.setSymbolType(PlotSymbolType.NONE);
        lineSeriesBollingerBandsUpperBand.setYSeries(bollingerBandsUpperBand.getValues());
        lineSeriesBollingerBandsUpperBand.setAntialias(SWT.ON);
        lineSeriesBollingerBandsUpperBand.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW));
        lineSeriesBollingerBandsUpperBand.setYAxisId(0);
        lineSeriesBollingerBandsUpperBand.setVisibleInLegend(true);

    }
}
