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
        Duration drawdownDuration, lastPeakDuration;

        public Drawdown(double[] values, Date[] dates)
        {
            this.values = values;
            this.dates = dates;
            max = 0d;
            peak = values[0];
            Date peakDate = dates[0];
            drawdownDuration = new Duration(new DateTime(peakDate), new DateTime(peakDate));
            Duration currentDuration;
            for (int i = 0; i < values.length; i++)
            {
                max = Math.max(max, (peak - values[i]));
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
            lastPeakDuration = new Duration(new DateTime(peakDate), new DateTime(dates[dates.length - 1]));
        }

        public Duration getDurationSinceLastPeak()
        {
            return lastPeakDuration;
        }

        public double getMagnitude()
        {
            return max;
        }

        public Duration getDuration()
        {
            return drawdownDuration;
        }

    }

    public static class Volatility
    {

        double[] values;
        double[] returns;
        double average;
        double standard;
        double semi;

        public Volatility(double[] values)
        {
            this.values = values;
            returns = getReturns(values);
            average = DoubleStream.of(returns).average().getAsDouble();
            standard = 0d;
            semi = 0d;
            for (int i = 0; i < returns.length; i++)
            {
                standard = standard + Math.pow(returns[i] - average, 2);
                if (returns[i] < average)
                {
                    semi = semi + Math.pow(returns[i] - average, 2);
                }
            }
            standard = Math.sqrt(standard / returns.length);
            semi = Math.sqrt(semi / returns.length);
        }

        public double getStandardDeviation()
        {
            return standard;
        }

        public double getSemiDeviation()
        {
            return semi;
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

    public static double annualize(double risk, Date[] dates)
    {
        // annualization is obatined by multiplying with the square root of the
        // number of periods the risk was calculated with
        return risk * Math.sqrt(dates.length);
    }

}
