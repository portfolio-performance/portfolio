package name.abuchen.portfolio.math;

import java.util.Date;
import java.util.stream.DoubleStream;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class Risk
{

    public static class Drawdown
    {

        double[] values;
        Date[] dates;
        double peak, max;
        Duration duration;

        public Drawdown(double[] values, Date[] dates)
        {
            this.values = values;
            this.dates = dates;
            calculate();
        }

        private void calculate()
        {
            max = 0d;
            peak = values[0];
            Date peakDate = dates[0];
            duration = new Duration(new DateTime(peakDate), new DateTime(peakDate));
            Duration currentDuration;
            for (int i = 0; i < values.length; i++)
            {
                max = Math.max(max, (peak - values[i]));
                if (values[i] > peak)
                {
                    peak = values[i];
                    currentDuration = new Duration(new DateTime(peakDate), new DateTime(dates[i]));
                    peakDate = dates[i];
                    if (currentDuration.compareTo(duration) > 0)
                    {
                        duration = currentDuration;
                    }
                }
            }
        }

        public double getMagnitude()
        {
            return max;
        }

        public Duration getDuration()
        {
            return duration;
        }

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
