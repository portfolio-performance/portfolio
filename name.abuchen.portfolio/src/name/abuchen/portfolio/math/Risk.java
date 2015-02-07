package name.abuchen.portfolio.math;

import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

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
            for (int i = 0; i < values.length; i++)
            {
                value = values[i] + 1;
                currentDrawdownDuration = new Duration(new DateTime(lastPeakDate), new DateTime(dates[i]));
                if (value > peak)
                {
                    peak = value;
                    lastPeakDate = dates[i];
                    if (currentDrawdownDuration.isLongerThan(lastDrawdownDuration))
                    {
                        lastDrawdownDuration = currentDrawdownDuration;
                    }
                }
                else
                {
                    if (peak == 0d)
                    {
                        drawdown = peak - value;
                    }
                    else
                    {
                        drawdown = (peak - value) / peak;
                    }
                    if (drawdown > max)
                    {
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
        private final double stdDeviation;
        private final double semiDeviation;

        public Volatility(Date[] dates, double[] returns, int skip, Predicate<Date> filter)
        {
            Objects.requireNonNull(returns);

            double averageReturn = average(dates, returns, skip, filter);
            double tempStandard = 0;
            int countStandard = 0;
            double tempSemi = 0;
            int countSemi = 0;

            for (int ii = skip; ii < returns.length; ii++)
            {
                if (!filter.test(dates[ii]))
                    continue;

                double add = Math.pow(returns[ii] - averageReturn, 2);

                tempStandard = tempStandard + add;
                countStandard++;

                if (returns[ii] < averageReturn)
                {
                    tempSemi = tempSemi + add;
                    countSemi++;
                }
            }

            stdDeviation = Math.sqrt(tempStandard / countStandard);
            semiDeviation = Math.sqrt(tempSemi / countSemi);
        }

        private double average(Date[] dates, double[] returns, int skip, Predicate<Date> filter)
        {
            double sum = 0;
            int count = 0;

            for (int ii = skip; ii < returns.length; ii++)
            {
                if (!filter.test(dates[ii]))
                    continue;

                sum += returns[ii];
                count++;
            }

            return sum / count;
        }

        public double getStandardDeviation()
        {
            return stdDeviation;
        }

        public double getSemiDeviation()
        {
            return semiDeviation;
        }
    }
}
