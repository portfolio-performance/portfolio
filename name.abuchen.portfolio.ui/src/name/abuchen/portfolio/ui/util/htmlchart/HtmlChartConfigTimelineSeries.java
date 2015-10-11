package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.Date;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.graphics.RGB;

public abstract class HtmlChartConfigTimelineSeries
{
    protected Date[] dates;
    protected double[] values;
    protected RGB color;
    protected String name;
    protected double opacity = 1;
    protected int strokeWidth = 2;
    protected boolean noLegend = false;
    
    /**
     * Derived classes must override the getter by returning a constant that identifies the renderer to be used.
     * @return The name of the renderer used by Rickshaw (e.g. 'line', 'bar', etc.)
     */
    protected abstract String getRenderer();

    public Date[] getDates()
    {
        return dates;
    }

    public void setDates(Date[] dates)
    {
        this.dates = dates;
    }

    public double[] getValues()
    {
        return values;
    }

    public void setValues(double[] values)
    {
        this.values = values;
    }

    public int getStrokeWidth()
    {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth)
    {
        this.strokeWidth = strokeWidth;
    }

    public RGB getColor()
    {
        return this.color;
    }

    public void setColor(RGB color)
    {
        this.color = color;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public double getOpacity()
    {
        return this.opacity;
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

    private void buildSeriesName(StringBuilder buffer)
    {
        buffer.append("name:'").append(StringEscapeUtils.escapeJson(name)).append("'");
    }

    private void buildSeriesData(StringBuilder buffer)
    {
        int itemCount = dates.length;
        buffer.append("[");
        for (int i = 0; i < itemCount; i++)
        {
            if (i > 0 && itemCount > 1)
                buffer.append(",");
            buffer.append("{x:").append(dates[i].getTime()).append(",y:").append(String.format("%.4f", values[i]))
                            .append("}");

        }
        buffer.append("]");
    }

    private void buildSeriesColor(StringBuilder buffer)
    {
        buffer.append("color:'rgba(").append(color.red).append(",").append(color.green).append(",").append(color.blue)
                        .append(",").append(String.format("%3.2f", opacity)).append(")'");
    }

    private void buildSeriesStrokeWidth(StringBuilder buffer)
    {
        buffer.append("strokeWidth:").append(strokeWidth);
    }

    private void buildSeriesRenderer(StringBuilder buffer)
    {
        buffer.append("renderer:'").append(getRenderer()).append("'");
    };

    /**
     * Writes the configuration into the {@code buffer} as Json structure.
     * @param buffer An instantiated {@link java.lang.StringBuilder} where the Json stream is written to
     */
    public void buildSeries(StringBuilder buffer)
    {
        buildSeriesName(buffer);
        buffer.append(",");
        buildSeriesData(buffer);
        buffer.append(",");
        buildSeriesColor(buffer);
        buffer.append(",");
        buildSeriesStrokeWidth(buffer);
        buffer.append(",");
        buildSeriesRenderer(buffer);
    }

}
