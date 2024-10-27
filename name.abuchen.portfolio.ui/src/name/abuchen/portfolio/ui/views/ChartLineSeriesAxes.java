package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;

public class ChartLineSeriesAxes
{
    private LocalDate[] dates;
    private double[] values;

    public LocalDate[] getDates()
    {
        return dates;
    }

    public void setDates(LocalDate[] dates)
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
