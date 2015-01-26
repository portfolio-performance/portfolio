package name.abuchen.portfolio.math;

import java.util.Date;
import java.util.stream.DoubleStream;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public final class Risk
{

    public static double calculateMaxDrawdownMagnitude(double[] values)
    {
        double peak = Double.MIN_VALUE;
        double maxDD = 0;

        for (double value : values)
        {
            peak = Math.max(peak, value);
            // as the values are the accumulated percentages there is no need to
            // calculate a relation. We can simply reduce the peak
            maxDD = Math.max(maxDD, (peak - value));
        }
        return maxDD;
    }

    public static Duration calculateMaxDrawdownDuration(double[] values, Date[] dates)
    {
        double peak = values[0];
        Date peakDate = dates[0];
        Duration drawdownDuration = new Duration(new DateTime(dates[0]), new DateTime(dates[0]));
        Duration currentDuration;
        for (int i = 0; i < dates.length; i++)
        {
            if (values[i] > peak)
            {
                peak = values[i];
                currentDuration = new Duration(new DateTime(peakDate), new DateTime(dates[i]));
                peakDate = dates[i];
                if (currentDuration.compareTo(drawdownDuration) > 0)
                {
                    drawdownDuration = currentDuration;
                }
            }
        }
        return drawdownDuration;
    }

    private static double[] getReturns(double[] values)
    {
        double[] returns = new double[values.length - 1];
        for (int i = 0; i < returns.length; i++)
        {
            returns[i] = values[i + 1] - values[i];
        }
        return returns;
    }

    public static double calculateAverageVolatility(double[] values)
    {
        double[] vola = Risk.getReturns(values);
        double average = DoubleStream.of(vola).average().getAsDouble();
        double variance = 0d;
        for (int i = 0; i < vola.length; i++)
        {
            variance = Math.pow(vola[i] - average, 2) + variance;
        }
        variance = variance / vola.length;
        variance = Math.sqrt(variance);
        return variance;
    }

    public static double calculateSemiVolatility(double[] values)
    {
        double[] returns = Risk.getReturns(values);
        double averageReturn = DoubleStream.of(returns).average().getAsDouble();
        double semiVariance = 0;
        for (int i = 0; i < returns.length; i++)
        {
            if (returns[i] < averageReturn)
            {
                semiVariance = semiVariance + Math.pow(averageReturn - returns[i], 2);
            }
        }
        semiVariance = semiVariance / returns.length;
        return Math.sqrt(semiVariance);
    }

    public static double annualize(double risk, Date[] dates)
    {
        // annualization is obatined by multiplying with the square root of the
        // number of periods the risk was calculated with
        return risk * Math.sqrt(dates.length);
    }

}
