package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class HtmlChartConfigTimeline implements HtmlChartConfig
{
    private String title;
    private String interpolation = "monotone"; //$NON-NLS-1$
    private String numberFormat = "#,##0.00"; //$NON-NLS-1$
    private String numberFormatLocale = "de"; //$NON-NLS-1$
    private boolean showLegend = true;
    private boolean useLogScale = false;
    private boolean allowZoom = true;
    private HtmlChartConfigTimelineVerticalMarkerList verticalMarker;
    private List<HtmlChartConfigTimelineSeries> series = new ArrayList<>();

    @Override
    public String getTitle()
    {
        return title;
    }

    public HtmlChartConfigTimeline setTitle(String title)
    {
        this.title = title;
        return this;
    }

    public String getInterpolation()
    {
        return interpolation;
    }

    public HtmlChartConfigTimeline setInterpolation(String interpolation)
    {
        this.interpolation = interpolation;
        return this;
    }

    public String getNumberFormat()
    {
        return numberFormat;
    }

    public HtmlChartConfigTimeline setNumberFormat(String numberFormat)
    {
        this.numberFormat = numberFormat;
        return this;
    }

    public String getNumberFormatLocale()
    {
        return numberFormatLocale;
    }

    public HtmlChartConfigTimeline setNumberFormatLocale(String numberFormatLocale)
    {
        this.numberFormatLocale = numberFormatLocale;
        return this;
    }

    public boolean isShowLegend()
    {
        return showLegend;
    }

    public HtmlChartConfigTimeline setShowLegend(boolean showLegend)
    {
        this.showLegend = showLegend;
        return this;
    }

    public boolean isUseLogScale()
    {
        return useLogScale;
    }

    public HtmlChartConfigTimeline setUseLogScale(boolean useLogScale)
    {
        this.useLogScale = useLogScale;
        return this;
    }

    public boolean isAllowZoom()
    {
        return allowZoom;
    }

    public HtmlChartConfigTimeline setAllowZoom(boolean allowZoom)
    {
        this.allowZoom = allowZoom;
        return this;
    }

    public HtmlChartConfigTimelineVerticalMarkerList getVerticalMarker()
    {
        return verticalMarker;
    }

    public HtmlChartConfigTimeline setVerticalMarker(HtmlChartConfigTimelineVerticalMarkerList verticalMarker)
    {
        this.verticalMarker = verticalMarker;
        return this;
    }

    public List<HtmlChartConfigTimelineSeries> series()
    {
        return this.series;
    }

    @SuppressWarnings("unchecked")
    private void buildJsonSeries(JSONObject json)
    {
        JSONArray jsonList = new JSONArray();
        for (HtmlChartConfigTimelineSeries s : series)
        {
            jsonList.add(s.getJson());
        }
        json.put("series", jsonList); //$NON-NLS-1$
    }

    @Override
    @SuppressWarnings({ "unchecked", "nls" })
    public JSONObject getJson()
    {
        JSONObject json = new JSONObject();
        json.put("title", title);
        json.put("interpolation", interpolation);
        json.put("numberFormat", numberFormat);
        json.put("numberFormatLocale", numberFormatLocale);
        json.put("showLegend", showLegend);
        json.put("allowZoom", allowZoom);

        if (verticalMarker != null)
            json.put("verticalMarker", verticalMarker.getJson());

        buildJsonSeries(json);

        return json;
    }

    @Override
    public String getJsonString()
    {
        return this.getJson().toString();
    }

    @Override
    public String getHtmlPageUri()
    {
        return "/META-INF/html/line_chart.html"; //$NON-NLS-1$
    }
}
