package name.abuchen.portfolio.ui.util.htmlchart;

import java.util.Date;
import java.util.Locale;

import org.eclipse.swt.graphics.RGB;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
     * Derived classes must override the getter by returning a constant that
     * identifies the renderer to be used.
     * 
     * @return The name of the renderer used by Rickshaw (e.g. 'line', 'bar',
     *         etc.)
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

    @SuppressWarnings({ "unchecked", "nls" })
    private void buildSeriesData(JSONObject json)
    {
        int itemCount = dates.length;
        JSONArray jsonList = new JSONArray();
        JSONObject jsonDataPoint;
        for (int i = 0; i < itemCount; i++)
        {
            jsonDataPoint = new JSONObject();
            jsonDataPoint.put("x", dates[i].getTime() / 1000);
            jsonDataPoint.put("y", values[i]);
            jsonList.add(jsonDataPoint);
        }
        json.put("data", jsonList);
    }

    
    @SuppressWarnings({ "unchecked", "nls" })
    public JSONObject getJson()
    {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("color", "rgba(" + color.red + "," + color.green + "," + color.blue + ","
                        + String.format(Locale.US, "%3.2f", opacity) + ")");
        json.put("strokeWidth", strokeWidth);
        json.put("renderer", getRenderer());
        buildSeriesData(json);
        return json;
    }

}
