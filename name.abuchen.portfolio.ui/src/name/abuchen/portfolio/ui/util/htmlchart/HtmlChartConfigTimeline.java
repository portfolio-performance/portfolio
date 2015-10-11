package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.ArrayList;
import java.util.List;
// import name.abuchen.portfolio.ui.util.chart.ChartContextMenu;
import org.apache.commons.lang3.StringEscapeUtils;


public class HtmlChartConfigTimeline implements HtmlChartConfig
{
    private String title;
    private String interpolation = "monotone";
    private String numberFormat = "#,##0.00";
    private String numberFormatLocale = "de";
    private boolean noLegend = false;
    private Double minY = Double.NaN;
    private Double maxY = Double.NaN;
    private HtmlChartConfigTimelineVerticalMarkerList verticalMarker;
    private List<HtmlChartConfigTimelineSeries> series = new ArrayList<HtmlChartConfigTimelineSeries>();

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getInterpolation()
    {
        return interpolation;
    }

    public void setInterpolation(String interpolation)
    {
        this.interpolation = interpolation;
    }

    public String getNumberFormat()
    {
        return numberFormat;
    }

    public void setNumberFormat(String numberFormat)
    {
        this.numberFormat = numberFormat;
    }

    public String getNumberFormatLocale()
    {
        return numberFormatLocale;
    }

    public void setNumberFormatLocale(String numberFormatLocale)
    {
        this.numberFormatLocale = numberFormatLocale;
    }

    public boolean isNoLegend()
    {
        return noLegend;
    }

    public void setNoLegend(boolean noLegend)
    {
        this.noLegend = noLegend;
    }

    public Double getMinY()
    {
        return minY;
    }

    public void setMinY(Double minY)
    {
        this.minY = minY;
    }

    public Double getMaxY()
    {
        return maxY;
    }

    public void setMaxY(Double maxY)
    {
        this.maxY = maxY;
    }

    public HtmlChartConfigTimelineVerticalMarkerList getVerticalMarker()
    {
        return verticalMarker;
    }

    public void setVerticalMarker(HtmlChartConfigTimelineVerticalMarkerList verticalMarker)
    {
        this.verticalMarker = verticalMarker;
    }

    public List<HtmlChartConfigTimelineSeries> series()
    {
        return this.series;
    }

    private void buildJsonTitle(StringBuilder buffer)
    {
        buffer.append("title:'").append(StringEscapeUtils.escapeJson(title)).append("'");
    }

    private void buildJsonInterpolation(StringBuilder buffer)
    {
        buffer.append("interpolation:'").append(interpolation).append("'");
    }

    private void buildJsonNumberFormat(StringBuilder buffer)
    {
        buffer.append("numberFormat:'").append(numberFormat).append("'");
    }

    private void buildJsonNumberFormatLocale(StringBuilder buffer)
    {
        buffer.append("numberFormatLocale:'").append(numberFormatLocale).append("'");
    }

    private void buildJsonNoLegend(StringBuilder buffer)
    {
        buffer.append("noLegend:'").append(noLegend ? "true" : "false").append("'");
    }

    private void buildJsonMinY(StringBuilder buffer)
    {
        buffer.append("minY:'").append(String.format("%.4f", minY)).append("'");
    }

    private void buildJsonMaxY(StringBuilder buffer)
    {
        buffer.append("maxY:'").append(String.format("%.4f", maxY)).append("'");
    }

    private void buildJsonSeries(StringBuilder buffer)
    {
        boolean isFirst = true;

        buffer.append("[");
        for (HtmlChartConfigTimelineSeries s : series)
        {
            if (isFirst)
                isFirst = false;
            else
                buffer.append(",");

            s.buildSeries(buffer);
        }
        buffer.append("]");
    }

    private void buildJson(StringBuilder buffer)
    {
        buffer.append("{");
        buildJsonTitle(buffer);
        buffer.append(",");
        buildJsonInterpolation(buffer);
        buffer.append(",");
        buildJsonNumberFormat(buffer);
        buffer.append(",");
        buildJsonNumberFormatLocale(buffer);
        buffer.append(",");
        buildJsonNoLegend(buffer);
        
        if (verticalMarker != null) {
            buffer.append(",");
            verticalMarker.buildJson(buffer);
        }
        
        if (!Double.isNaN(minY))
        {
            buffer.append(",");
            buildJsonMinY(buffer);
        }
        if (!Double.isNaN(maxY))
        {
            buffer.append(",");
            buildJsonMaxY(buffer);
        }
        buffer.append(",");
        buildJsonSeries(buffer);
        buffer.append("}");        
    }

    public String getJson()
    {
        StringBuilder buffer = new StringBuilder();
        buildJson(buffer);
        return buffer.toString();
    }

    public String getHtmlPageUri()
    {
        return "/META-INF/html/line_chart.html";
    };

}
