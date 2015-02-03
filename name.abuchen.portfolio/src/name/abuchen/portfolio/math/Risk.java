package name.abuchen.portfolio.math;

import java.util.Arrays;
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
        double peak, max, drawdown;
        Duration currentDrawdownDuration, lastDrawdownDuration;
        Date lastPeakDate;

        public Drawdown(double[] values, Date[] dates)
        {
            this.values = values;
            this.dates = dates;
            peak = values[0] + 1;
            max = 0d;
            lastDrawdownDuration = Duration.ZERO;
            lastPeakDate = dates[0];
            double value;
            for (int i = 0;i<values.length;i++) {
                value = values[i] + 1;
                currentDrawdownDuration = new Duration(new DateTime(lastPeakDate), new DateTime(dates[i]));
                if (value > peak) {
                    peak = value;
                    lastPeakDate = dates[i];
                    if (currentDrawdownDuration.isLongerThan(lastDrawdownDuration)) {
                        lastDrawdownDuration = currentDrawdownDuration;
                    }
                } else {
                    if (peak == 0d) {
                        drawdown = peak - value;
                    } else {
                        drawdown = (peak - value) / peak;
                    }
                    if (drawdown > max) {
                        max = drawdown;
                    }
                }
            }
        }

        public Duration getDurationSinceLastPeak()
        {
            return currentDrawdownDuration;
        }

        public double getMagnitude()
        {
            return max;
        }

        public Duration getDuration()
        {
            return lastDrawdownDuration;
        }

    }

    public static class Volatility
    {

        double[] values;
        double[] returns;
        double averageReturn;
        double standard;
        double semi;

        public Volatility(double[] values)
        {
            this.values = values;
            returns = getReturns(values);
            averageReturn = DoubleStream.of(returns).average().getAsDouble();
            standard = 0d;
            semi = 0d;
            double add;
            for (int i = 0; i < returns.length; i++)
            {
                add = Math.pow(returns[i] - averageReturn, 2);
                standard = standard + add;
                if (returns[i] < averageReturn)
                {
                    semi = semi + Math.pow(returns[i] - averageReturn, 2);
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
            if (values[i] == 0) {
                returns[i] = values[i+1];
            } else {
                returns[i] = (values[i + 1] - values[i]) / (values[i] + 1);
            }
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
