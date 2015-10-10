package name.abuchen.portfolio.ui.util.chart;

import java.util.Date;

import org.eclipse.swt.graphics.Color;

public class TimelineHtmlChartSeriesLine extends TimelineHtmlChartSeries
{

    private String strokePattern;
    private Color strokeColor;

    public void TimelineHtmlChartSeries(String name, Date[] dates, double[] values, Color color, double transparency)
    {
        this.name = name;
        this.dates = dates;
        this.values = values;
        this.color = color;
        this.opacity = transparency;
    }

    @Override
    protected String getRenderer()
    {
        return "dottedline";
    }

    public String getStrokePattern()
    {
        return strokePattern;
    }

    public void setStrokePattern(String strokePattern)
    {
        this.strokePattern = strokePattern;
    }

    public Color getStrokeColor()
    {
        return strokeColor;
    }

    public void setStrokePattern(Color strokeColor)
    {
        this.strokeColor = strokeColor;
    }

    private void buildSeriesStrokePattern(StringBuilder buffer)
    {
        buffer.append("strokePattern:'").append(getRenderer()).append("'");
    };

    private void buildSeriesStrokeColor(StringBuilder buffer)
    {
        buffer.append("stroke:'rgba(").append(color.getRed()).append(",").append(color.getGreen()).append(",")
                        .append(color.getBlue()).append(",").append(String.format("%3.2f", opacity)).append(")'");
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
