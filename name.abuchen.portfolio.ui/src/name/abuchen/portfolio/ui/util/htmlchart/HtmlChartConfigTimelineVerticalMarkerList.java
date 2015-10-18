package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.graphics.RGB;

/***
 * Structure holding the configuration and list of
 * {@link HtmlChartConfigTimelineVerticalMarker} for the {@link HtmlChart}.
 * The data is written to a StringBuffer as Json Object using the
 * {@code buildJson(StringBuilder buffer)} method.
 * 
 * @author fuchsst
 */
public class HtmlChartConfigTimelineVerticalMarkerList extends ArrayList<HtmlChartConfigTimelineVerticalMarker>
{
    private static final long serialVersionUID = -3502197888115471366L;

    private String name;
    private RGB color;
    private RGB labelColor;
    protected double opacity = 1;
    protected double labelOpacity = 1;
    private int strokeWidth;
    private boolean showLabel = true;

    public HtmlChartConfigTimelineVerticalMarkerList(String name, int strokeWidth)
    {
        this(name, strokeWidth, null, 1.0, null, 1.0, true);
    }

    public HtmlChartConfigTimelineVerticalMarkerList(String name, int strokeWidth, RGB color, double opacity, RGB labelColor,
                    double labelOpacity, boolean showLabel)
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

    public static long getSerialversionuid()
    {
        return serialVersionUID;
    }

    private void buildJsonName(StringBuilder buffer)
    {
        buffer.append("name:'").append(StringEscapeUtils.escapeJson(name)).append("'");
    };

    private void buildJsonStrokeWidth(StringBuilder buffer)
    {
        buffer.append("strokeWidth:").append(strokeWidth);
    }

    private void buildJsonColor(StringBuilder buffer)
    {
        buffer.append("color:'rgba(").append(color.red).append(",").append(color.green).append(",").append(color.blue)
                        .append(",").append(String.format(Locale.US, "%3.2f", opacity)).append(")'");
    };

    private void buildJsonLabelColor(StringBuilder buffer)
    {
        buffer.append("labelColor:'rgba(").append(labelColor.red).append(",").append(labelColor.green).append(",")
                        .append(labelColor.blue).append(",").append(String.format(Locale.US, "%3.2f", labelOpacity)).append(")'");
    };

    private void buildJsonShowLabel(StringBuilder buffer)
    {
        buffer.append("showLabel:").append(showLabel ? "true" : "false");
    }

    private void buildJsonVerticalMarker(StringBuilder buffer)
    {
        boolean isFirst = true;

        // sort marker by date
        Collections.sort(this, new Comparator<HtmlChartConfigTimelineVerticalMarker>()
        {
            @Override
            public int compare(HtmlChartConfigTimelineVerticalMarker ml1, HtmlChartConfigTimelineVerticalMarker ml2)
            {
                return ml1.getDate().compareTo(ml2.getDate());
            }
        });

        buffer.append("data : [");
        for (HtmlChartConfigTimelineVerticalMarker marker : this)
        {
            if (isFirst)
                isFirst = false;
            else
                buffer.append(",");

            marker.buildJson(buffer);
        }
        buffer.append("]");
    }

    public void buildJson(StringBuilder buffer)
    {
        buffer.append("verticalMarker : {");
        buildJsonName(buffer);
        buffer.append(",");
        buildJsonStrokeWidth(buffer);
        buffer.append(",");
        buildJsonVerticalMarker(buffer);
        buffer.append(",");
        buildJsonShowLabel(buffer);

        if (color != null)
        {
            buffer.append(",");
            buildJsonColor(buffer);
        }

        if (labelColor != null)
        {
            buffer.append(",");
            buildJsonLabelColor(buffer);
        }

        buffer.append("}");
    }

}
