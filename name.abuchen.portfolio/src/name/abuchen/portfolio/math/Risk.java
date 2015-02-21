package name.abuchen.portfolio.math;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

import name.abuchen.portfolio.util.Interval;

public final class Risk
{

    public static class Drawdown
    {
        private double maxDD;
        private Interval maxDDDuration;
        private Interval intervalMaxDD;
        private Interval intervalSinceLastPeak;
        
        public Drawdown(double[] values, Date[] dates)
        {
            double peak = values[0] + 1;
            Instant lastPeakDate = dates[0].toInstant();

            maxDDDuration = Interval.of(lastPeakDate, lastPeakDate);
            Interval currentDrawdownDuration;

            for (int ii = 0; ii < values.length; ii++)
            {
                double value = values[ii] + 1;
                currentDrawdownDuration = Interval.of(lastPeakDate, dates[ii].toInstant());

                if (value > peak)
                {
                    peak = value;
                    lastPeakDate = dates[ii].toInstant();

                    if (currentDrawdownDuration.isLongerThan(maxDDDuration))
                        maxDDDuration = currentDrawdownDuration;
                }
                else
                {
                    double drawdown = (peak - value) / peak;
                    if (drawdown > maxDD)
                    {
                        maxDD = drawdown;
                        intervalMaxDD = Interval.of(lastPeakDate, dates[ii].toInstant());
                    }
                }
            }
            intervalSinceLastPeak = Interval.of(lastPeakDate, dates[dates.length-1].toInstant());
        }

        public double getMaxDrawdown()
        {
            return maxDD;
        }

        public Interval getIntervalOfMaxDrawdown()
        {
            return intervalMaxDD;
        }

        public Interval getMaxDrawdownDuration()
        {
            return maxDDDuration;
        }
        
        public Interval getIntervalSinceLastPeak() {
            return intervalSinceLastPeak;
        }
        
        public boolean isInMaxDrawdownDuration() {
            return intervalSinceLastPeak.isLongerThan(intervalMaxDD);
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
            double tempSemi = 0;
            int count = 0;

            for (int ii = skip; ii < returns.length; ii++)
            {
                if (!filter.test(dates[ii]))
                    continue;

                double add = Math.pow(returns[ii] - averageReturn, 2);

                tempStandard = tempStandard + add;
                count++;

                if (returns[ii] < averageReturn)
                    tempSemi = tempSemi + add;
            }

            stdDeviation = Math.sqrt(tempStandard / count);
            semiDeviation = Math.sqrt(tempSemi / count);
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

        public double getExpectedSemiDeviation()
        {
            return stdDeviation / Math.sqrt(2);
        }

        public String getNormalizedSemiDeviationComparison()
        {
            double expectedSemiDeviation = getExpectedSemiDeviation();
            if (expectedSemiDeviation > semiDeviation)
                return ">"; //$NON-NLS-1$
            else if (expectedSemiDeviation < semiDeviation)
                return "<"; //$NON-NLS-1$
            return "="; //$NON-NLS-1$
        }
    }

    private Risk()
    {}
}
