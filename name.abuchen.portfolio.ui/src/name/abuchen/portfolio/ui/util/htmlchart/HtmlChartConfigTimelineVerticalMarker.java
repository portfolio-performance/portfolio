package name.abuchen.portfolio.ui.util.htmlchart;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import org.eclipse.swt.graphics.RGB;
import org.json.simple.JSONObject;

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
    private LocalDate date;
    private RGB color;
    private RGB labelColor;
    protected double opacity = 1;
    protected double labelOpacity = 1;
    private String strokePattern;

    public HtmlChartConfigTimelineVerticalMarker(LocalDate date, String label)
    {
        this(date, label, null, Double.NaN, null, Double.NaN, null);
    }

    public HtmlChartConfigTimelineVerticalMarker(LocalDate date, String label, RGB color, Double opacity,
                    RGB labelColor,
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

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
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

    @SuppressWarnings("unchecked")
    public JSONObject getJson()
    {
        JSONObject json = new JSONObject();

        json.put("x", date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000);
        json.put("label", label);

        if (color != null)
            json.put("color", "rgba(" + color.red + "," + color.green + "," + color.blue + ","
                            + String.format(Locale.US, "%3.2f", opacity) + ")");

        if (labelColor != null)
            json.put("labelColor", "rgba(" + labelColor.red + "," + labelColor.green + "," + labelColor.blue + ","
                            + String.format(Locale.US, "%3.2f", labelOpacity) + ")");

        if (strokePattern != null && !strokePattern.isEmpty())
            json.put("strokePattern", strokePattern);

        return json;
    }

}
