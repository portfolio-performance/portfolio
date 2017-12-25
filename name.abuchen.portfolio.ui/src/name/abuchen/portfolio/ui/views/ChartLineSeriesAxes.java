package name.abuchen.portfolio.ui.views;

import java.util.Date;

public class ChartLineSeriesAxes
{
    private Date[] dates;
    private double[] values;

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
}
