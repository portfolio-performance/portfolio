package name.abuchen.portfolio.ui.util.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class TimelineHtmlChart extends Composite
{
    private static class MarkerLine
    {
        private Date date;
        private RGB color;
        private String label;

        private MarkerLine(Date date, RGB color, String label)
        {
            this.date = date;
            this.color = color;
            this.label = label;
        }
    }

    private EmbeddedBrowser browser;
    private List<MarkerLine> markerLines = new ArrayList<MarkerLine>();
    private List<TimelineHtmlChartSeries> series = new ArrayList<TimelineHtmlChartSeries>();
    private String title;
    private String interpolation = "monotone";
    private String numberFormat = "#,##0.00";
    private String numberFormatLocale = "de";
    private boolean noLegend = false;
    private Double minY = Double.NaN;
    private Double maxY = Double.NaN;

    private final LocalResourceManager resources;
    private ChartContextMenu contextMenu;

    public TimelineHtmlChart(Composite parent)
    {
        super(parent, SWT.NONE);

        resources = new LocalResourceManager(JFaceResources.getResources(), this);
        browser = new EmbeddedBrowser("/META-INF/html/flare.html"); //$NON-NLS-1$
        browser.createControl(parent, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$

        // ZoomMouseWheelListener.attachTo(this);
        // MovePlotKeyListener.attachTo(this);
        // ZoomInAreaListener.attachTo(this);

        // this.contextMenu = new ChartContextMenu(this);
    }

    public void refreshChart()
    {

    }

    public void addMarkerLine(Date date, RGB color, String label)
    {
        this.markerLines.add(new MarkerLine(date, color, label));
        Collections.sort(this.markerLines, new Comparator<MarkerLine>()
        {
            @Override
            public int compare(MarkerLine ml1, MarkerLine ml2)
            {
                return ml1.date.compareTo(ml2.date);
            }
        });
    }

    public void clearMarkerLines()
    {
        this.markerLines.clear();
    }

    public void addSeries(TimelineHtmlChartSeries series)
    {
        this.series.add(series);
    }

    public void clearSeries()
    {
        this.series.clear();
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        public Object function(Object[] arguments)
        {
            try
            {
                StringBuilder buffer = new StringBuilder();
                buffer.append("{");
                buildTitle(buffer);
                buffer.append(",");
                buildInterpolation(buffer);
                buffer.append(",");
                buildNumberFormat(buffer);
                buffer.append(",");
                buildNumberFormatLocale(buffer);
                buffer.append(",");
                buildNoLegend(buffer);
                if (!Double.isNaN(minY))
                {
                    buffer.append(",");
                    buildMinY(buffer);
                }
                if (!Double.isNaN(maxY))
                {
                    buffer.append(",");
                    buildMaxY(buffer);
                }
                buffer.append(",");
                buildSeries(buffer);
                buffer.append("}");
                return buffer.toString();
            }
            catch (Throwable e)
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
            }
        }

        private void buildTitle(StringBuilder buffer)
        {
            buffer.append("title:'").append(StringEscapeUtils.escapeJson(title)).append("'");
        }

        private void buildInterpolation(StringBuilder buffer)
        {
            buffer.append("interpolation:'").append(interpolation).append("'");
        }

        private void buildNumberFormat(StringBuilder buffer)
        {
            buffer.append("numberFormat:'").append(numberFormat).append("'");
        }

        private void buildNumberFormatLocale(StringBuilder buffer)
        {
            buffer.append("numberFormatLocale:'").append(numberFormatLocale).append("'");
        }

        private void buildNoLegend(StringBuilder buffer)
        {
            buffer.append("noLegend:'").append(noLegend ? "true" : "false").append("'");
        }

        private void buildMinY(StringBuilder buffer)
        {
            buffer.append("minY:'").append(String.format("%.4f", minY)).append("'");
        }

        private void buildMaxY(StringBuilder buffer)
        {
            buffer.append("maxY:'").append(String.format("%.4f", maxY)).append("'");
        }

        private void buildSeries(StringBuilder buffer)
        {
            boolean isFirst = true;

            buffer.append("[");
            for (TimelineHtmlChartSeries s : series)
            {
                if (isFirst)
                    isFirst = false;
                else
                    buffer.append(",");

                s.buildSeries(buffer);
            }
            buffer.append("]");
        }

    }

    /*
     * public void exportMenuAboutToShow(IMenuManager manager, String label) {
     * this.contextMenu.exportMenuAboutToShow(manager, label); }
     */
}
