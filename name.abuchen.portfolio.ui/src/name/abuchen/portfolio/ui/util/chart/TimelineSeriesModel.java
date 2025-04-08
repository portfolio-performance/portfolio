package name.abuchen.portfolio.ui.util.chart;

import java.time.LocalDate;

import org.eclipse.swtchart.model.DoubleArraySeriesModel;

public class TimelineSeriesModel extends DoubleArraySeriesModel
{
    private final LocalDate[] xdata;

    public TimelineSeriesModel(LocalDate[] xSeries, double[] ySeries)
    {
        super(toEpochDay(xSeries), ySeries);

        xdata = xSeries;
    }

    public LocalDate[] getXDateSeries()
    {
        return xdata;
    }

    public static double[] toEpochDay(LocalDate[] dates)
    {
        double[] answer = new double[dates.length];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = dates[ii].toEpochDay();
        return answer;
    }
}
