package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.Date;
import org.eclipse.swt.graphics.RGB;

/**
 * Configuration for timeline chart series rendered as line (based on the own
 * extended Rickshaw class 'dottedline', which also supports stroke patterns').
 * 
 * @author fuchsst
 */
public class HtmlChartConfigTimelineSeriesLine extends HtmlChartConfigTimelineSeries
{

    private String strokePattern;

    /**
     * @param name
     *            Name of the series (e.g. used as legend label)
     * @param dates
     *            array of dates (X-axis data points)
     * @param values
     *            array of values (Y-axis data points)
     * @param color
     *            line color
     * @param opacity
     *            level of opacity, where 1=opaque and 0=completely transparent
     */
    public HtmlChartConfigTimelineSeriesLine(String name, Date[] dates, double[] values, RGB color, double opacity)
    {
        this.name = name;
        this.dates = dates;
        this.values = values;
        this.color = color;
        this.opacity = opacity;
    }

    @Override
    protected String getRenderer()
    {
        return "dottedline";
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

    private void buildSeriesStrokePattern(StringBuilder buffer)
    {
        buffer.append("strokePattern:'").append(strokePattern).append("'");
    };

    public void buildSeries(StringBuilder buffer)
    {
        super.buildSeries(buffer);
        if (strokePattern != null && !strokePattern.isEmpty())
        {
            buffer.append(",");
            buildSeriesStrokePattern(buffer);
        }
    }

}
