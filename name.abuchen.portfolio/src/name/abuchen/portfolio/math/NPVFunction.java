package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.math.NewtonGoalSeek.Function;
import name.abuchen.portfolio.util.Dates;

@SuppressWarnings("nls")
/* package */class NPVFunction implements Function
{
    private final int[] days;
    private final double[] values;

    public NPVFunction(List<LocalDate> dates, List<Double> values)
    {
        if (dates == null || values == null)
            throw new NullPointerException("dates and/or values are null");
        if (dates.size() != values.size())
            throw new UnsupportedOperationException("dates and values must have equal size.");
        if (dates.isEmpty() || values.isEmpty())
            throw new UnsupportedOperationException("at least one data point must be provided");

        this.days = new int[dates.size()];
        for (int ii = 0; ii < dates.size(); ii++)
            this.days[ii] = Dates.daysBetween(dates.get(0), dates.get(ii));

        this.values = new double[values.size()];
        for (int ii = 0; ii < values.size(); ii++)
            this.values[ii] = values.get(ii);
    }

    @Override
    public double compute(double rate)
    {
        double answer = 0;

        for (int ii = 0; ii < days.length; ii++)
        {
            answer += values[ii] / Math.pow(rate, days[ii] / 365.0);
        }

        return answer;
    }

}
