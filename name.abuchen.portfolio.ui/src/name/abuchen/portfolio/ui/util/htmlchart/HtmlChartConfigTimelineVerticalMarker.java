package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.Date;
import java.util.Locale;
import java.util.function.DoubleToLongFunction;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.graphics.RGB;

/**
 * Structure holding the configuration of a single Vertical Marker for the
 * {@link HtmlChart}. The data is written to a StringBuffer as Json Object using
 * the {@code buildJson(StringBuilder buffer)} method
 * 
 * @author fuchsst
 */
public class HtmlChartConfigTimelineVerticalMarker
{

    private String label;
    private Date date;
    private RGB color;
    private RGB labelColor;
    protected double opacity = 1;
    protected double labelOpacity = 1;
    private String strokePattern;

    public HtmlChartConfigTimelineVerticalMarker(Date date, String label)
    {
        this(date, label, null, Double.NaN, null, Double.NaN, null);
    }

    public HtmlChartConfigTimelineVerticalMarker(Date date, String label, RGB color, Double opacity, RGB labelColor,
                    Double labelOpacity, String strokePattern)
    {
        this.date = date;
        this.color = color;
        this.opacity = opacity;
        this.label = label;
        this.labelColor = labelColor;
        this.labelOpacity = labelOpacity;
        this.strokePattern = strokePattern;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public RGB getColor()
    {
        return color;
    }

    public void setColor(RGB color)
    {
        this.color = color;
    }

    public RGB getLabelColor()
    {
        return labelColor;
    }

    public void setLabelColor(RGB labelColor)
    {
        this.labelColor = labelColor;
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

    public double getLabelOpacity()
    {
        return labelOpacity;
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

    public String getStrokePattern()
    {
        return strokePattern;
    }

    public void setStrokePattern(String strokePattern)
    {
        this.strokePattern = strokePattern;
    }

    private void buildJsonLabel(StringBuilder buffer)
    {
        buffer.append("label:'").append(StringEscapeUtils.escapeJson(label)).append("'");
    };

    private void buildJsonDate(StringBuilder buffer)
    {
        buffer.append("x:").append(date.getTime() / 1000);
    };

    private void buildJsonColor(StringBuilder buffer)
    {
        buffer.append("color:'rgba(").append(color.red).append(",").append(color.green).append(",").append(color.blue)
                        .append(",").append(String.format(Locale.US, "%3.2f", opacity)).append(")'");
    };

    private void buildJsonSeriesStrokePattern(StringBuilder buffer)
    {
        buffer.append("strokePattern:'").append(strokePattern).append("'");
    };

    private void buildJsonLabelColor(StringBuilder buffer)
    {
        buffer.append("labelColor:'rgba(").append(labelColor.red).append(",").append(labelColor.green).append(",")
                        .append(labelColor.blue).append(",").append(String.format(Locale.US, "%3.2f", labelOpacity))
                        .append(")'");
    };

    public void buildJson(StringBuilder buffer)
    {
        buffer.append("{");
        buildJsonDate(buffer);
        buffer.append(",");
        buildJsonLabel(buffer);

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
        if (strokePattern != null && !strokePattern.isEmpty())
        {
            buffer.append(",");
            buildJsonSeriesStrokePattern(buffer);
        }

        buffer.append("}");
    }

}
