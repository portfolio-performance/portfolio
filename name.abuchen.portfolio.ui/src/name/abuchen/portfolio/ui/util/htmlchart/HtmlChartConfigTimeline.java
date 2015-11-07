package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class HtmlChartConfigTimeline implements HtmlChartConfig
{
    private static final double ZOOM_RATIO = 0.2;
    private static final double SCROLL_RATIO = 0.1;

    private String title;
    private String interpolation = "monotone";
    private String numberFormat = "#,##0.00";
    private String numberFormatLocale = "de";
    private boolean noLegend = false;
    private boolean useLogScale = false;
    private Date minX = null;
    private Date maxX = null;
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

    public boolean isUseLogScale()
    {
        return useLogScale;
    }

    public void setUseLogScale(boolean useLogScale)
    {
        this.useLogScale = useLogScale;
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

    public Date getMinX()
    {
        return minX;
    }

    public void setMinX(Date minX)
    {
        this.minX = minX;
    }

    public Date getMaxX()
    {
        return maxX;
    }

    public void setMaxX(Date maxX)
    {
        this.maxX = maxX;
    }

    public void resetX()
    {
        this.minX = null;
        this.maxX = null;
    }

    public void resetY()
    {
        this.minY = Double.NaN;
        this.maxY = Double.NaN;
    }

    public void resetXY()
    {
        this.resetX();
        this.resetY();
    }

    // sets minX, maxX based on the series data
    private void getXextendsFromData()
    {
        resetX();
        for (HtmlChartConfigTimelineSeries s : series)
        {
            for (Date d : s.dates)
            {
                if (minX == null || minX.after(d))
                    minX = d;
                if (maxX == null || maxX.before(d))
                    maxX = d;
            }
        }
    }

    // sets minY, maxY based on the series data
    private void getYextendsFromData()
    {
        resetY();
        for (HtmlChartConfigTimelineSeries s : series)
        {
            for (Double v : s.values)
            {
                if (minY == null || minY > v)
                    minY = v;
                if (maxY == null || maxY < v)
                    maxY = v;
            }
        }
    }

    public void zoomInX()
    {
        if (minX == null || maxX == null)
            getXextendsFromData();
        zoomInX(new Date((minX.getTime() + maxX.getTime()) / 2));
    };

    public void zoomInX(Date coordinate)
    {
        long lower = minX.getTime();
        long upper = maxX.getTime();
        if (useLogScale)
        {
            double digitMin = Math.log10(lower);
            double digitMax = Math.log10(upper);
            double digitCoordinate = Math.log10(coordinate.getTime());
            minX = new Date((long) Math.pow(10, digitMin + SCROLL_RATIO * (digitCoordinate - digitMin)));
            maxX = new Date((long) Math.pow(10, digitMax + SCROLL_RATIO * (digitCoordinate - digitMax)));
        }
        else
        {
            minX = new Date((long) (lower + ZOOM_RATIO * (coordinate.getTime() - lower)));
            maxX = new Date((long) (upper + ZOOM_RATIO * (coordinate.getTime() - upper)));
        }
    };

    public void zoomOutX()
    {
        if (minX == null || maxX == null)
            getXextendsFromData();
        zoomOutX(new Date((minX.getTime() + maxX.getTime()) / 2));
    }

    public void zoomOutX(Date coordinate)
    {
        long lower = minX.getTime();
        long upper = maxX.getTime();
        if (useLogScale)
        {
            double digitMin = Math.log10(lower);
            double digitMax = Math.log10(upper);
            double digitCoordinate = Math.log10(coordinate.getTime());
            minX = new Date((long) Math.pow(10, (digitMin - ZOOM_RATIO * digitCoordinate) / (1 - ZOOM_RATIO)));
            maxX = new Date((long) Math.pow(10, (digitMax - ZOOM_RATIO * digitCoordinate) / (1 - ZOOM_RATIO)));
        }
        else
        {
            minX = new Date((long) ((lower - ZOOM_RATIO * coordinate.getTime()) / (1 - ZOOM_RATIO)));
            maxX = new Date((long) ((upper - ZOOM_RATIO * coordinate.getTime()) / (1 - ZOOM_RATIO)));
        }
    }

    public void zoomInY()
    {
        if (minY == null || maxY == null)
            getYextendsFromData();
        zoomInY((minY + maxY) / 2);
    };

    public void zoomInY(Double coordinate)
    {
        if (useLogScale)
        {
            double digitMin = Math.log10(minY);
            double digitMax = Math.log10(maxY);
            double digitCoordinate = Math.log10(coordinate);
            minY = Math.pow(10, digitMin + SCROLL_RATIO * (digitCoordinate - digitMin));
            maxY = Math.pow(10, digitMax + SCROLL_RATIO * (digitCoordinate - digitMax));
        }
        else
        {
            minY = minY + ZOOM_RATIO * (coordinate - minY);
            maxY = maxY + ZOOM_RATIO * (coordinate - maxY);
        }
    };

    public void zoomOutY()
    {
        if (minY == null || maxY == null)
            getYextendsFromData();
        zoomOutY((minY + maxY) / 2);
    }

    public void zoomOutY(Double coordinate)
    {
        if (useLogScale)
        {
            double digitMin = Math.log10(minY);
            double digitMax = Math.log10(maxY);
            double digitCoordinate = Math.log10(coordinate);
            minY = Math.pow(10, (digitMin - ZOOM_RATIO * digitCoordinate) / (1 - ZOOM_RATIO));
            maxY = Math.pow(10, (digitMax - ZOOM_RATIO * digitCoordinate) / (1 - ZOOM_RATIO));
        }
        else
        {
            minY = (minY - ZOOM_RATIO * coordinate) / (1 - ZOOM_RATIO);
            maxY = (maxY - ZOOM_RATIO * coordinate) / (1 - ZOOM_RATIO);
        }
    }

    public void zoomInXY()
    {
        this.zoomInX();
        this.zoomInY();
    }

    public void zoomOutXY()
    {
        this.zoomOutX();
        this.zoomOutY();
    }

    public void scrollLeft()
    {
        if (minX == null || maxX == null)
            getXextendsFromData();

        if (useLogScale)
        {
            double digitMin = Math.log10(minX.getTime());
            double digitMax = Math.log10(maxX.getTime());
            minX = new Date((long) Math.pow(10, digitMin - (digitMax - digitMin) * SCROLL_RATIO));
            maxX = new Date((long) Math.pow(10, digitMax - (digitMax - digitMin) * SCROLL_RATIO));
        }
        else
        {
            long lower = minX.getTime();
            long upper = maxX.getTime();
            minX = new Date((long) (lower - (upper - lower) * SCROLL_RATIO));
            maxX = new Date((long) (upper - (upper - lower) * SCROLL_RATIO));
        }
    }

    public void scrollRight()
    {
        if (minX == null || maxX == null)
            getXextendsFromData();

        if (useLogScale)
        {
            double digitMin = Math.log10(minX.getTime());
            double digitMax = Math.log10(maxX.getTime());
            minX = new Date((long) Math.pow(10, digitMin + (digitMax - digitMin) * SCROLL_RATIO));
            maxX = new Date((long) Math.pow(10, digitMax + (digitMax - digitMin) * SCROLL_RATIO));
        }
        else
        {
            long lower = minX.getTime();
            long upper = maxX.getTime();
            minX = new Date((long) (lower + (upper - lower) * SCROLL_RATIO));
            maxX = new Date((long) (upper + (upper - lower) * SCROLL_RATIO));
        }
    }

    public void scrollUp()
    {
        if (minY == null || maxY == null)
            getYextendsFromData();

        if (useLogScale)
        {
            double digitMin = Math.log10(minY);
            double digitMax = Math.log10(maxY);
            minY = Math.pow(10, digitMin + (digitMax - digitMin) * SCROLL_RATIO);
            maxY = Math.pow(10, digitMax + (digitMax - digitMin) * SCROLL_RATIO);
        }
        else
        {
            double lower = minY;
            double upper = maxY;
            minY = lower + (upper - lower) * SCROLL_RATIO;
            maxY = upper + (upper - lower) * SCROLL_RATIO;
        }
    }

    public void scrollDown()
    {
        if (minY == null || maxY == null)
            getYextendsFromData();

        if (useLogScale)
        {
            double digitMin = Math.log10(minY);
            double digitMax = Math.log10(maxY);
            minY = Math.pow(10, digitMin - (digitMax - digitMin) * SCROLL_RATIO);
            maxY = Math.pow(10, digitMax - (digitMax - digitMin) * SCROLL_RATIO);
        }
        else
        {
            double lower = minY;
            double upper = maxY;
            minY = lower - (upper - lower) * SCROLL_RATIO;
            maxY = upper - (upper - lower) * SCROLL_RATIO;
        }
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

    @SuppressWarnings("unchecked")
    private void buildJsonSeries(JSONObject json)
    {
        JSONArray jsonList = new JSONArray();
        for (HtmlChartConfigTimelineSeries s : series)
        {
            jsonList.add(s.getJson());
        }
        json.put("series", jsonList);
    }

    @SuppressWarnings("unchecked")
    public JSONObject getJson()
    {
        JSONObject json = new JSONObject();
        json.put("title", title);
        json.put("interpolation", interpolation);
        json.put("numberFormat", numberFormat);
        json.put("numberFormatLocale", numberFormatLocale);
        json.put("noLegend", noLegend);

        if (verticalMarker != null)
            json.put("verticalMarker", verticalMarker.getJson());

        if (minX != null)
            json.put("minX", minX);

        if (maxX != null)
            json.put("maxX", maxX);

        if (!Double.isNaN(minY))
            json.put("minY", minY);

        if (!Double.isNaN(maxY))
            json.put("maxY", maxY);

        buildJsonSeries(json);
        return json;
    }

    public String getJsonString()
    {
        return this.getJson().toString();
    }

    public String getHtmlPageUri()
    {
        return "/META-INF/html/line_chart.html";
    };

}
