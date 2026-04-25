package name.abuchen.portfolio.ui.views;

import java.lang.reflect.Field;
import java.time.LocalDate;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ColorGradientDefinitions;
import name.abuchen.portfolio.ui.util.ColorSourceTracker;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DataSeriesColors;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.util.chart.BarChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.payments.PaymentsPalette;

public class ColorArchitectureDebugView extends AbstractFinanceView
{
    private static final String STATUS_CSS_BACKED = "CSS-backed"; //$NON-NLS-1$
    private static final String STATUS_FALLBACK = "fallback"; //$NON-NLS-1$
    private static final String STATUS_HARD_CODED = "hard-coded"; //$NON-NLS-1$
    private static final String STATUS_ALGORITHMIC = "algorithmic"; //$NON-NLS-1$
    private static final String STATUS_MIXED = "mixed"; //$NON-NLS-1$

    private static final class ColorEntry
    {
        private final String domain;
        private final String property;
        private final Color color;
        private final String status;
        private final String note;

        private ColorEntry(String domain, String property, Color color, String status, String note)
        {
            this.domain = domain;
            this.property = property;
            this.color = color;
            this.status = status;
            this.note = note;
        }
    }

    private static final class SecuritiesChartSnapshot
    {
        private Color quoteColor;
        private Color quoteAreaPositiveColor;
        private Color quoteAreaNegativeColor;
        private Color purchaseEventColor;
        private Color saleEventColor;
        private Color dividendEventColor;
        private Color extremeMarkerHighColor;
        private Color extremeMarkerLowColor;
        private Color nonTradingColor;
        private Color sharesHeldColor;
    }

    private static final class PortfolioBalanceChartSnapshot
    {
        private Color totalsColor;
        private Color investedCapitalColor;
        private Color absoluteDeltaColor;
        private Color taxesAccumulatedColor;
        private Color feesAccumulatedColor;
        private Color deltaAreaPositiveColor;
        private Color deltaAreaNegativeColor;
    }

    @Override
    protected String getDefaultTitle()
    {
        return "Color Architecture Debug"; //$NON-NLS-1$
    }

    @Override
    protected Control createBody(Composite parent)
    {
        var scrolled = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        var content = new Composite(scrolled, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).spacing(10, 10).applyTo(content);
        scrolled.setContent(content);

        createInfoBlock(content);

        var hidden = new Composite(content, SWT.NONE);
        hidden.setVisible(false);
        GridDataFactory.fillDefaults().exclude(true).hint(0, 0).applyTo(hidden);
        hidden.setLayout(new FillLayout());

        var securitiesSnapshot = buildSecuritiesChartSnapshot(hidden);
        var portfolioSnapshot = buildPortfolioBalanceChartSnapshot(hidden);

        createThemeSection(content);
        createValueColorSchemeSection(content);
        createDataSeriesSection(content);
        createPaymentsSection(content);
        createGradientSection(content);
        createSecuritiesChartSection(content, securitiesSnapshot);
        createPortfolioBalanceSection(content, portfolioSnapshot);
        createRenderPreviewSection(content, securitiesSnapshot, portfolioSnapshot);

        scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        content.addListener(SWT.Resize, e -> scrolled.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT)));

        return scrolled;
    }

    private void createInfoBlock(Composite parent)
    {
        var section = createSection(parent, "Debug-Zugriff"); //$NON-NLS-1$

        var text = new Label(section, SWT.WRAP);
        text.setText("Diese View zeigt aktuelle Farbwerte und trennt bei instrumentierten Domänen zwischen CSS-backed und fallback. " //$NON-NLS-1$
                        + "SecuritiesChart und PortfolioBalanceChart werden intern als gestylte Testinstanzen aufgebaut. " //$NON-NLS-1$
                        + "Reststellen werden bewusst als hard-coded, algorithmic oder mixed markiert."); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(text);

        var launch = new Label(section, SWT.WRAP);
        launch.setText("VM-Argument für Eclipse Debug-Run: -Dname.abuchen.portfolio.debug=yes"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(launch);
    }

    private void createThemeSection(Composite parent)
    {
        var section = createSection(parent, "CustomColors"); //$NON-NLS-1$
        var table = createColorTable(section);

        var theme = Colors.theme();
        addTrackedColorRow(table, "CustomColors", "default-foreground", theme.defaultForeground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "default-background", theme.defaultBackground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "chip-background", theme.chipBackground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "warning-background", theme.warningBackground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "red-background", theme.redBackground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "green-background", theme.greenBackground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "red-foreground", theme.redForeground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "green-foreground", theme.greenForeground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "gray-foreground", theme.grayForeground(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "CustomColors", "hyperlink", theme.hyperlink(), "Colors.Theme"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private void createValueColorSchemeSection(Composite parent)
    {
        var section = createSection(parent, "ValueColorScheme"); //$NON-NLS-1$
        var table = createColorTable(section);

        for (var scheme : ValueColorScheme.getAvailableSchemes())
        {
            String domain = "ValueColorScheme." + scheme.getIdentifier(); //$NON-NLS-1$
            String note = scheme == ValueColorScheme.current() ? "aktives Scheme" : "verfügbares Scheme"; //$NON-NLS-1$ //$NON-NLS-2$

            addTrackedColorRow(table, domain, "positive-foreground", scheme.positiveForeground(), note); //$NON-NLS-1$
            addTrackedColorRow(table, domain, "negative-foreground", scheme.negativeForeground(), note); //$NON-NLS-1$
        }
    }

    private void createDataSeriesSection(Composite parent)
    {
        var section = createSection(parent, "DataSeries"); //$NON-NLS-1$
        var table = createColorTable(section);

        var colors = DataSeriesColors.instance();

        addTrackedColorRow(table, "DataSeries", "totals-color", colors.totalsColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "invested-capital-color", colors.investedCapitalColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "absolute-invested-capital-color", //$NON-NLS-1$ //$NON-NLS-2$
                        colors.absoluteInvestedCapitalColor(), null);
        addTrackedColorRow(table, "DataSeries", "transferals-color", colors.transferalsColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "transferals-accumulated-color", colors.transferalsAccumulatedColor(), //$NON-NLS-1$ //$NON-NLS-2$
                        null);
        addTrackedColorRow(table, "DataSeries", "taxes-color", colors.taxesColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "taxes-accumulated-color", colors.taxesAccumulatedColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "absolute-delta-color", colors.absoluteDeltaColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "absolute-delta-all-record-color", colors.absoluteDeltaAllRecordColor(), //$NON-NLS-1$ //$NON-NLS-2$
                        null);
        addTrackedColorRow(table, "DataSeries", "dividends-color", colors.dividendsColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "dividends-accumulated-color", colors.dividendsAccumulatedColor(), //$NON-NLS-1$ //$NON-NLS-2$
                        null);
        addTrackedColorRow(table, "DataSeries", "interest-color", colors.interestColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "interest-accumulated-color", colors.interestAccumulatedColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "interest-charge-color", colors.interestChargeColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "interest-charge-accumulated-color", //$NON-NLS-1$ //$NON-NLS-2$
                        colors.interestChargeAccumulatedColor(), null);
        addTrackedColorRow(table, "DataSeries", "earnings-color", colors.earningsColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "earnings-accumulated-color", colors.earningsAccumulatedColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "fees-color", colors.feesColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "fees-accumulated-color", colors.feesAccumulatedColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "performance-entire-portfolio-color", //$NON-NLS-1$ //$NON-NLS-2$
                        colors.performanceEntirePortfolioColor(), null);
        addTrackedColorRow(table, "DataSeries", "performance-positive-color", colors.performancePositiveColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
        addTrackedColorRow(table, "DataSeries", "performance-negative-color", colors.performanceNegativeColor(), null); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void createPaymentsSection(Composite parent)
    {
        var section = createSection(parent, "PaymentsPalette"); //$NON-NLS-1$
        var table = createColorTable(section);

        var palette = PaymentsPalette.instance();
        for (int ii = 0; ii < palette.size(); ii++)
        {
            addTrackedColorRow(table, "PaymentsPalette", "color-" + ii, palette.get(ii), "Palette index " + ii); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    private void createGradientSection(Composite parent)
    {
        var section = createSection(parent, "ColorGradientDefinitions"); //$NON-NLS-1$
        var table = createGradientTable(section);

        addGradientRow(table, ColorGradientDefinitions.redToGreen(),
                        isGradientCssBacked(ColorGradientDefinitions.redToGreen()));
        addGradientRow(table, ColorGradientDefinitions.orangeToBlue(),
                        isGradientCssBacked(ColorGradientDefinitions.orangeToBlue()));
        addGradientRow(table, ColorGradientDefinitions.greenYellowRed(),
                        isGradientCssBacked(ColorGradientDefinitions.greenYellowRed()));
        addGradientRow(table, ColorGradientDefinitions.greenWhiteRed(),
                        isGradientCssBacked(ColorGradientDefinitions.greenWhiteRed()));
        addGradientRow(table, ColorGradientDefinitions.yellowWhiteBlack(),
                        isGradientCssBacked(ColorGradientDefinitions.yellowWhiteBlack()));
    }

    private void createSecuritiesChartSection(Composite parent, SecuritiesChartSnapshot snapshot)
    {
        var section = createSection(parent, "SecuritiesChart"); //$NON-NLS-1$
        var table = createColorTable(section);

        addTrackedColorRow(table, "SecuritiesChart", "quote-color", snapshot.quoteColor, "gestylte Dummy-Instanz"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addTrackedColorRow(table, "SecuritiesChart", "quote-area-positive-color", snapshot.quoteAreaPositiveColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "quote-area-negative-color", snapshot.quoteAreaNegativeColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "purchase-event-color", snapshot.purchaseEventColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "sale-event-color", snapshot.saleEventColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "dividend-event-color", snapshot.dividendEventColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "extreme-marker-high-color", snapshot.extremeMarkerHighColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "extreme-marker-low-color", snapshot.extremeMarkerLowColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "non-trading-color", snapshot.nonTradingColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "SecuritiesChart", "shares-held-color", snapshot.sharesHeldColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
    }

    private void createPortfolioBalanceSection(Composite parent, PortfolioBalanceChartSnapshot snapshot)
    {
        var section = createSection(parent, "PortfolioBalanceChart"); //$NON-NLS-1$
        var table = createColorTable(section);

        addTrackedColorRow(table, "PortfolioBalanceChart", "totals-color", snapshot.totalsColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "PortfolioBalanceChart", "invested-capital-color", snapshot.investedCapitalColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "PortfolioBalanceChart", "absolute-delta-color", snapshot.absoluteDeltaColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "PortfolioBalanceChart", "taxes-accumulated-color", snapshot.taxesAccumulatedColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "PortfolioBalanceChart", "fees-accumulated-color", snapshot.feesAccumulatedColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "PortfolioBalanceChart", "delta-area-positive-color", snapshot.deltaAreaPositiveColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
        addTrackedColorRow(table, "PortfolioBalanceChart", "delta-area-negative-color", snapshot.deltaAreaNegativeColor, //$NON-NLS-1$ //$NON-NLS-2$
                        "gestylte Dummy-Instanz"); //$NON-NLS-1$
    }

    private void createRenderPreviewSection(Composite parent, SecuritiesChartSnapshot securitiesSnapshot,
                    PortfolioBalanceChartSnapshot portfolioSnapshot)
    {
        var section = createSection(parent, "Echte Render-Beispiele aus CSS-/Domain-Farben"); //$NON-NLS-1$

        var lineTitle = new Label(section, SWT.NONE);
        lineTitle.setText("SecuritiesChart-nahe LineSeries / Marker / Event-Farben"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(lineTitle);

        var lineChart = new TimelineChart(section);
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 220).applyTo(lineChart);

        LocalDate[] dates = new LocalDate[] { //
                        LocalDate.now().minusDays(6), //
                        LocalDate.now().minusDays(5), //
                        LocalDate.now().minusDays(4), //
                        LocalDate.now().minusDays(3), //
                        LocalDate.now().minusDays(2), //
                        LocalDate.now().minusDays(1), //
                        LocalDate.now() };

        double[] quoteValues = new double[] { 100, 103, 102, 106, 108, 107, 111 };
        double[] holdingsValues = new double[] { 10, 10, 11, 11, 12, 12, 12 };

        lineChart.addDateSeries("debug-quote", dates, quoteValues, //$NON-NLS-1$
                        cssOrDomainFallback("SecuritiesChart", "quote-color", securitiesSnapshot.quoteColor), "Quote"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        lineChart.addDateSeries("debug-shares-held", dates, holdingsValues, //$NON-NLS-1$
                        cssOrDomainFallback("SecuritiesChart", "shares-held-color", securitiesSnapshot.sharesHeldColor), //$NON-NLS-1$ //$NON-NLS-2$
                        "Shares Held"); //$NON-NLS-1$

        lineChart.addMarkerLine(dates[1],
                        cssOrDomainFallback("SecuritiesChart", "purchase-event-color", //$NON-NLS-1$ //$NON-NLS-2$
                                        securitiesSnapshot.purchaseEventColor),
                        "BUY"); //$NON-NLS-1$
        lineChart.addMarkerLine(dates[3],
                        cssOrDomainFallback("SecuritiesChart", "dividend-event-color", //$NON-NLS-1$ //$NON-NLS-2$
                                        snapshotFallback(securitiesSnapshot.dividendEventColor,
                                                        PaymentsPalette.instance().get(0))), // $NON-NLS-1$
                        "DIV"); //$NON-NLS-1$
        lineChart.addMarkerLine(dates[5],
                        cssOrDomainFallback("SecuritiesChart", "sale-event-color", securitiesSnapshot.saleEventColor), //$NON-NLS-1$ //$NON-NLS-2$
                        "SELL"); //$NON-NLS-1$

        lineChart.addNonTradingDayMarker(dates[2], cssOrDomainFallback("SecuritiesChart", "non-trading-color", //$NON-NLS-1$ //$NON-NLS-2$
                        securitiesSnapshot.nonTradingColor));
        lineChart.adjustRange();

        var barTitle = new Label(section, SWT.NONE);
        barTitle.setText("PortfolioBalanceChart-nahe BarSeries-Farben"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(barTitle);

        var barChart = new BarChart(section, "Debug"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 220).applyTo(barChart);

        barChart.setCategories(java.util.List.of("A", "B", "C", "D")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        barChart.addSeries("totals", "Totals", new double[] { 50, 60, 55, 70 }, //$NON-NLS-1$ //$NON-NLS-2$
                        cssOrDomainFallback("PortfolioBalanceChart", "totals-color", portfolioSnapshot.totalsColor), //$NON-NLS-1$ //$NON-NLS-2$
                        true);
        barChart.addSeries("delta", "Absolute Delta", new double[] { 5, -3, 4, -2 }, //$NON-NLS-1$ //$NON-NLS-2$
                        cssOrDomainFallback("PortfolioBalanceChart", "absolute-delta-color", //$NON-NLS-1$ //$NON-NLS-2$
                                        portfolioSnapshot.absoluteDeltaColor),
                        true);
        barChart.adjustRange();

        var paletteTitle = new Label(section, SWT.NONE);
        paletteTitle.setText("PaymentsPalette aus Runtime-Palette"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(paletteTitle);

        var paletteComposite = new Composite(section, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(paletteComposite);
        GridLayoutFactory.fillDefaults().numColumns(15).spacing(4, 4).margins(0, 0).applyTo(paletteComposite);

        for (int ii = 0; ii < PaymentsPalette.instance().size(); ii++)
            createSwatch(paletteComposite, PaymentsPalette.instance().get(ii));

        var gradientTitle = new Label(section, SWT.NONE);
        gradientTitle.setText("Gradients aus ColorGradientDefinitions"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(gradientTitle);

        createGradientPreview(section, ColorGradientDefinitions.redToGreen());
        createGradientPreview(section, ColorGradientDefinitions.orangeToBlue());
        createGradientPreview(section, ColorGradientDefinitions.greenYellowRed());
        createGradientPreview(section, ColorGradientDefinitions.greenWhiteRed());
        createGradientPreview(section, ColorGradientDefinitions.yellowWhiteBlack());
    }

    private Color cssOrDomainFallback(String domain, String property, Color color)
    {
        if (color != null)
            return color;

        return Colors.theme().defaultForeground();
    }

    private Color snapshotFallback(Color preferred, Color fallback)
    {
        return preferred != null ? preferred : fallback;
    }

    private SecuritiesChartSnapshot buildSecuritiesChartSnapshot(Composite parent)
    {
        var snapshot = new SecuritiesChartSnapshot();

        try
        {
            var factory = getFromContext(ExchangeRateProviderFactory.class);
            if (factory == null || getClient() == null)
                return snapshot;

            var chart = new SecuritiesChart(parent, getClient(),
                            new CurrencyConverterImpl(factory, getClient().getBaseCurrency()));
            getStylingEngine().style(chart.getControl());
            getStylingEngine().style(chart);

            snapshot.quoteColor = readColorField(chart, "colorQuote"); //$NON-NLS-1$
            snapshot.quoteAreaPositiveColor = readColorField(chart, "colorQuoteAreaPositive"); //$NON-NLS-1$
            snapshot.quoteAreaNegativeColor = readColorField(chart, "colorQuoteAreaNegative"); //$NON-NLS-1$
            snapshot.purchaseEventColor = readColorField(chart, "colorEventPurchase"); //$NON-NLS-1$
            snapshot.saleEventColor = readColorField(chart, "colorEventSale"); //$NON-NLS-1$
            snapshot.dividendEventColor = readColorField(chart, "colorEventDividend"); //$NON-NLS-1$
            snapshot.extremeMarkerHighColor = readColorField(chart, "colorExtremeMarkerHigh"); //$NON-NLS-1$
            snapshot.extremeMarkerLowColor = readColorField(chart, "colorExtremeMarkerLow"); //$NON-NLS-1$
            snapshot.nonTradingColor = readColorField(chart, "colorNonTradingDay"); //$NON-NLS-1$
            snapshot.sharesHeldColor = chart.getSharesHeldColor();
        }
        catch (RuntimeException e)
        {
            // Debug-View soll bei Teilfehlern trotzdem öffnen
        }

        return snapshot;
    }

    private PortfolioBalanceChartSnapshot buildPortfolioBalanceChartSnapshot(Composite parent)
    {
        var snapshot = new PortfolioBalanceChartSnapshot();

        try
        {
            if (getClient() == null)
                return snapshot;

            var chart = new PortfolioBalanceChart(parent, getClient());
            getStylingEngine().style(chart.getControl());
            getStylingEngine().style(chart);

            snapshot.totalsColor = readColorField(chart, "colorTotals"); //$NON-NLS-1$
            snapshot.investedCapitalColor = readColorField(chart, "colorAbsoluteInvestedCapital"); //$NON-NLS-1$
            snapshot.absoluteDeltaColor = readColorField(chart, "colorAbsoluteDelta"); //$NON-NLS-1$
            snapshot.taxesAccumulatedColor = readColorField(chart, "colorTaxesAccumulated"); //$NON-NLS-1$
            snapshot.feesAccumulatedColor = readColorField(chart, "colorFeesAccumulated"); //$NON-NLS-1$
            snapshot.deltaAreaPositiveColor = readColorField(chart, "colorDeltaAreaPositive"); //$NON-NLS-1$
            snapshot.deltaAreaNegativeColor = readColorField(chart, "colorDeltaAreaNegative"); //$NON-NLS-1$
        }
        catch (RuntimeException e)
        {
            // Debug-View soll bei Teilfehlern trotzdem öffnen
        }

        return snapshot;
    }

    private Composite createSection(Composite parent, String title)
    {
        var section = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(section);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(6, 6).applyTo(section);

        var label = new Label(section, SWT.NONE);
        label.setText(title);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

        return section;
    }

    private Composite createColorTable(Composite parent)
    {
        var table = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        GridLayoutFactory.fillDefaults().numColumns(5).spacing(10, 6).margins(0, 0).applyTo(table);

        createHeader(table, "Domain"); //$NON-NLS-1$
        createHeader(table, "Property"); //$NON-NLS-1$
        createHeader(table, "Aktueller Wert"); //$NON-NLS-1$
        createHeader(table, "Farbbox"); //$NON-NLS-1$
        createHeader(table, "Status"); //$NON-NLS-1$

        return table;
    }

    private Composite createGradientTable(Composite parent)
    {
        var table = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        GridLayoutFactory.fillDefaults().numColumns(5).spacing(10, 6).margins(0, 0).applyTo(table);

        createHeader(table, "Domain"); //$NON-NLS-1$
        createHeader(table, "Property"); //$NON-NLS-1$
        createHeader(table, "Aktueller Wert"); //$NON-NLS-1$
        createHeader(table, "Vorschau"); //$NON-NLS-1$
        createHeader(table, "Status"); //$NON-NLS-1$

        return table;
    }

    private void createHeader(Composite parent, String text)
    {
        var label = new Label(parent, SWT.NONE);
        label.setText(text);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
    }

    private void addTrackedColorRow(Composite parent, String domain, String property, Color color, String note)
    {
        addColorRow(parent, new ColorEntry(domain, property, color, trackedStatus(domain, property), note));
    }

    private void addColorRow(Composite parent, ColorEntry entry)
    {
        var domain = new Label(parent, SWT.WRAP);
        domain.setText(entry.domain);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(domain);

        var property = new Label(parent, SWT.WRAP);
        property.setText(entry.property);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(property);

        var value = new Label(parent, SWT.WRAP);
        value.setText(toHex(entry.color) + (entry.note != null ? " | " + entry.note : "")); //$NON-NLS-1$ //$NON-NLS-2$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(value);

        createSwatch(parent, entry.color);

        var status = new Label(parent, SWT.WRAP);
        status.setText(entry.status);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(status);
    }

    private void addGradientRow(Composite parent, ColorGradientDefinitions.Definition definition, String status)
    {
        String domain = "ColorGradientDefinitions." + definition.getCssClass(); //$NON-NLS-1$

        var domainLabel = new Label(parent, SWT.WRAP);
        domainLabel.setText(domain);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(domainLabel);

        var propertyLabel = new Label(parent, SWT.WRAP);
        propertyLabel.setText("color-0 .. color-n"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(propertyLabel);

        var valueLabel = new Label(parent, SWT.WRAP);
        valueLabel.setText(sampleGradientText(definition));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(valueLabel);

        var preview = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(preview);
        GridLayoutFactory.fillDefaults().numColumns(9).spacing(1, 1).margins(0, 0).applyTo(preview);

        for (int ii = 0; ii < 9; ii++)
            createSwatch(preview, definition.getGradient().getColorAt(ii / 8f));

        var statusLabel = new Label(parent, SWT.WRAP);
        statusLabel.setText(status);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(statusLabel);
    }

    private void createGradientPreview(Composite parent, ColorGradientDefinitions.Definition definition)
    {
        var row = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(row);
        GridLayoutFactory.fillDefaults().numColumns(10).spacing(2, 2).margins(0, 0).applyTo(row);

        var name = new Label(row, SWT.NONE);
        name.setText(definition.getCssClass());
        GridDataFactory.fillDefaults().hint(160, SWT.DEFAULT).applyTo(name);

        for (int ii = 0; ii < 9; ii++)
            createSwatch(row, definition.getGradient().getColorAt(ii / 8f));
    }

    private void addRestRow(Composite parent, String name, String status, String note)
    {
        var nameLabel = new Label(parent, SWT.WRAP);
        nameLabel.setText(name);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(nameLabel);

        var statusLabel = new Label(parent, SWT.WRAP);
        statusLabel.setText(status);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(statusLabel);

        var noteLabel = new Label(parent, SWT.WRAP);
        noteLabel.setText(note);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(noteLabel);
    }

    private void createSwatch(Composite parent, Color color)
    {
        Color swatchColor = notNull(color, Colors.LIGHT_GRAY);

        var swatch = new Canvas(parent, SWT.BORDER);
        GridDataFactory.fillDefaults().hint(26, 14).applyTo(swatch);

        swatch.addPaintListener((PaintEvent e) -> {
            var area = swatch.getClientArea();
            e.gc.setBackground(swatchColor);
            e.gc.fillRectangle(area);
        });
    }

    private String sampleGradientText(ColorGradientDefinitions.Definition definition)
    {
        var start = definition.getGradient().getColorAt(0f);
        var mid = definition.getGradient().getColorAt(0.5f);
        var end = definition.getGradient().getColorAt(1f);

        return toHex(start) + " … " + toHex(mid) + " … " + toHex(end); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String trackedStatus(String domain, String property)
    {
        return ColorSourceTracker.isCssApplied(domain, property) ? STATUS_CSS_BACKED : STATUS_FALLBACK;
    }

    private String isGradientCssBacked(ColorGradientDefinitions.Definition definition)
    {
        String domain = "ColorGradientDefinitions." + definition.getCssClass(); //$NON-NLS-1$
        return ColorSourceTracker.isCssApplied(domain, "color-0") ? STATUS_CSS_BACKED : STATUS_FALLBACK; //$NON-NLS-1$
    }

    private String toHex(Color color)
    {
        return color != null ? Colors.toHex(color) : "<null>"; //$NON-NLS-1$
    }

    private Color notNull(Color color, Color fallback)
    {
        return color != null ? color : fallback;
    }

    private Color readColorField(Object target, String fieldName)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Color) field.get(target);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }
}
