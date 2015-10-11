package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.Date;
import org.eclipse.swt.graphics.RGB;

/**
 * Configuration for timeline chart series rendered as area (based on the own
 * extended Rickshaw class 'dottedarea', which also supports stroke patterns (if
 * in addition to the area also a line on the area edge is drawn').
 * 
 * @author fuchsst
 */
public class HtmlChartConfigTimelineSeriesArea extends HtmlChartConfigTimelineSeries
{

    private String strokePattern;
    private RGB strokeColor;
    private Double strokeOpacity;

    /**
     * @param name
     *            Name of the series (e.g. used as legend label)
     * @param dates
     *            array of dates (X-axis data points)
     * @param values
     *            array of values (Y-axis data points)
     * @param color
     *            area color
     * @param opacity
     *            level of opacity, where 1=opaque and 0=completely transparent
     */
    public HtmlChartConfigTimelineSeriesArea(String name, Date[] dates, double[] values, RGB color, double opacity)
    {
        this.name = name;
        this.dates = dates;
        this.values = values;
        this.color = color;
        this.opacity = opacity;
        this.strokeOpacity = opacity;
    }

    @Override
    protected String getRenderer()
    {
        return "dottedarea";
    }

    /**
     * Pattern of line. Empty if solid line
     * 
     * @return String of comma separated numbers, where each represents the
     *         section length in pixel of line/gap switches
     */
    public String getStrokePattern()
    {
        return this.strokePattern;
    }

    /**
     * Pattern of line. Leave empty for solid line
     * 
     * @param strokePattern
     *            String of comma separated numbers, where each represents the
     *            section length in pixel of line/gap switches (e.g. '4, 2' =
     *            '---- ---- ---- '...)
     */
    public void setStrokePattern(String strokePattern)
    {
        this.strokePattern = strokePattern;
    }

    /**
     * Color of the line on the area border. If null, no line will be drawn
     * 
     * @return The {@link RGB} color of the area border
     */
    public RGB getStrokeColor()
    {
        return this.strokeColor;
    }

    /**
     * Color of the line on the area border. Leave null, if no line should be
     * drawn
     * 
     * @param strokeColor
     *            The {@link RGB} color of the area border
     */
    public void setStrokePattern(RGB strokeColor)
    {
        this.strokeColor = strokeColor;
    }

    /**
     * Level of opacity of the area border, where 1=opaque and 0=completely
     * transparent
     * 
     * @return Value between 0 (transparent) and 1 (opaque)
     */
    public Double getStrokeOpacity()
    {
        return this.strokeOpacity;
    }

    /**
     * Level of opacity of the area border, where 1=opaque and 0=completely
     * transparent
     * 
     * @param strokeOpacity
     *            Value between 0 (transparent) and 1 (opaque)
     */
    public void setStrokeOpacity(double strokeOpacity)
    {
        if (strokeOpacity < 0)
            this.strokeOpacity = 0.0;
        else if (strokeOpacity > 1)
            this.strokeOpacity = 1.0;
        else
            this.strokeOpacity = strokeOpacity;
    }

    private void buildSeriesStrokePattern(StringBuilder buffer)
    {
        buffer.append("strokePattern:'").append(strokePattern).append("'");
    };

    private void buildSeriesStrokeColor(StringBuilder buffer)
    {
        buffer.append("stroke:'rgba(").append(strokeColor.red).append(",").append(strokeColor.green).append(",")
                        .append(strokeColor.blue).append(",").append(String.format("%3.2f", strokeOpacity))
                        .append(")'");
    };

    public void buildSeries(StringBuilder buffer)
    {
        super.buildSeries(buffer);
        if (strokePattern != null && !strokePattern.isEmpty())
        {
            buffer.append(",");
            buildSeriesStrokePattern(buffer);
        }
        if (strokeColor != null)
        {
            buffer.append(",");
            buildSeriesStrokeColor(buffer);
        }
    }

}
