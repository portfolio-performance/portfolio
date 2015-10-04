package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.eclipse.swt.graphics.RGB;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/***
 * Structure holding the configuration and list of
 * {@link HtmlChartConfigTimelineVerticalMarker} for the {@link HtmlChart}. The
 * data is written to a StringBuffer as Json Object using the
 * {@code buildJson(StringBuilder buffer)} method.
 * 
 * @author fuchsst
 */
public class HtmlChartConfigTimelineVerticalMarkerList
{

    private String name;
    private RGB color;
    private RGB labelColor;
    private double opacity = 1;
    private double labelOpacity = 1;
    private int strokeWidth;
    private boolean showLabel = true;
    private ArrayList<HtmlChartConfigTimelineVerticalMarker> verticalMarker = new ArrayList<HtmlChartConfigTimelineVerticalMarker>();

    public HtmlChartConfigTimelineVerticalMarkerList(String name, int strokeWidth)
    {
        this(name, strokeWidth, null, 1.0, null, 1.0, true);
    }

    public HtmlChartConfigTimelineVerticalMarkerList(String name, int strokeWidth, RGB color, double opacity,
                    RGB labelColor, double labelOpacity, boolean showLabel)
    {
        super();
        this.name = name;
        this.color = color;
        this.opacity = opacity;
        this.labelColor = labelColor;
        this.labelOpacity = labelOpacity;
        this.strokeWidth = strokeWidth;
        this.showLabel = showLabel;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public RGB getColor()
    {
        return color;
    }

    public void setColor(RGB color)
    {
        this.color = color;
    }

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {
        if (opacity < 0)
            this.opacity = 0;
        else if (opacity > 1)
            this.opacity = 1;
        else
            this.opacity = opacity;
    }

    public RGB getLabelColor()
    {
        return labelColor;
    }

    public void setLabelColor(RGB labelColor)
    {
        this.labelColor = labelColor;
    }

    public double getLabelOpacity()
    {
        return this.labelOpacity;
    }

    public void setLabelOpacity(double labelOpacity)
    {
        if (labelOpacity < 0)
            this.labelOpacity = 0;
        else if (labelOpacity > 1)
            this.labelOpacity = 1;
        else
            this.labelOpacity = labelOpacity;
    }

    public int getStrokeWidth()
    {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth)
    {
        this.strokeWidth = strokeWidth;
    }

    public boolean isShowLabel()
    {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel)
    {
        this.showLabel = showLabel;
    }

    public void clear()
    {
        this.verticalMarker.clear();
    }

    public void addMarker(HtmlChartConfigTimelineVerticalMarker marker)
    {
        this.verticalMarker.add(marker);
    }

    @SuppressWarnings("unchecked")
    private void buildJsonVerticalMarker(JSONObject json)
    {
        // sort marker by date
        Collections.sort(this.verticalMarker, new Comparator<HtmlChartConfigTimelineVerticalMarker>()
        {
            @Override
            public int compare(HtmlChartConfigTimelineVerticalMarker ml1, HtmlChartConfigTimelineVerticalMarker ml2)
            {
                return ml1.getDate().compareTo(ml2.getDate());
            }
        });

        JSONArray jsonList = new JSONArray();
        for (HtmlChartConfigTimelineVerticalMarker marker : this.verticalMarker)
        {
            jsonList.add(marker.getJson());
        }
        json.put("data", jsonList);
    }

    @SuppressWarnings("unchecked")
    public JSONObject getJson()
    {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("strokeWidth", strokeWidth);
        json.put("showLabel", showLabel);

        if (color != null)
            json.put("color", "rgba(" + color.red + "," + color.green + "," + color.blue + ","
                            + String.format(Locale.US, "%3.2f", opacity) + ")");

        if (labelColor != null)
            json.put("labelColor", "rgba(" + labelColor.red + "," + labelColor.green + "," + labelColor.blue + ","
                            + String.format(Locale.US, "%3.2f", labelOpacity) + ")");

        buildJsonVerticalMarker(json);
        return json;
    }

}
