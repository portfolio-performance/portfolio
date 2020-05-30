package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.IntPredicate;

import name.abuchen.portfolio.util.Interval;

public final class Risk
{

    public static class Drawdown
    {
        private double maxDD;
        private Interval maxDDDuration;
        private Interval intervalMaxDD;
        private Interval recoveryTime;

        public Drawdown(double[] values, LocalDate[] dates, int startAt)
        {
            if (values.length != dates.length)
                throw new IllegalArgumentException();

            if (startAt >= values.length)
                throw new IllegalArgumentException();

            double peak = values[startAt] + 1;
            double bottom = values[startAt] + 1;
            LocalDate lastPeakDate = dates[startAt];
            LocalDate lastBottomDate = dates[startAt];

            maxDD = 0;
            intervalMaxDD = Interval.of(lastPeakDate, lastPeakDate);
            maxDDDuration = Interval.of(lastPeakDate, lastPeakDate);
            recoveryTime = Interval.of(lastBottomDate, lastPeakDate);
            Interval currentDrawdownDuration = null;
            Interval currentRecoveryTime = null;

            for (int ii = startAt; ii < values.length; ii++)
            {
                double value = values[ii] + 1;
                currentDrawdownDuration = Interval.of(lastPeakDate, dates[ii]);
                currentRecoveryTime = Interval.of(lastBottomDate, dates[ii]);

                if (value > peak)
                {
                    peak = value;
                    lastPeakDate = dates[ii];

                    if (currentDrawdownDuration.isLongerThan(maxDDDuration))
                        maxDDDuration = currentDrawdownDuration;

                    if (currentRecoveryTime.isLongerThan(recoveryTime))
                        recoveryTime = currentRecoveryTime;
                    // Reset the recovery time calculation, as the recovery is
                    // now complete
                    lastBottomDate = dates[ii];
                    bottom = value;
                }
                else
                {
                    double drawdown = (peak - value) / peak;
                    if (drawdown > maxDD)
                    {
                        maxDD = drawdown;
                        intervalMaxDD = Interval.of(lastPeakDate, dates[ii]);
                    }
                }
                if (value < bottom)
                {
                    bottom = value;
                    lastBottomDate = dates[ii];
                }
            }

            // check if current drawdown duration is longer than the max
            // drawdown duration currently calculated --> use it because it is
            // the longest duration even if we do not know how much longer it
            // will get

            if (currentDrawdownDuration != null && currentDrawdownDuration.isLongerThan(maxDDDuration))
                maxDDDuration = currentDrawdownDuration;

            if (currentRecoveryTime != null && currentRecoveryTime.isLongerThan(recoveryTime))
                recoveryTime = currentRecoveryTime;
        }

        public Interval getLongestRecoveryTime()
        {
            return recoveryTime;
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
    }

    public static class Volatility
    {
        private final double stdDeviation;
        private final double semiDeviation;

        public Volatility(double[] returns, IntPredicate filter)
        {
            Objects.requireNonNull(returns);

            double tempStandard = 0;
            double tempSemi = 0;
            int count = 0;

            double averageLogReturn = logAverage(returns, filter);

            for (int ii = 0; ii < returns.length; ii++)
            {
                if (!filter.test(ii))
                    continue;

                double logReturn = Math.log(1 + returns[ii]);
                double add = Math.pow(logReturn - averageLogReturn, 2);

                tempStandard = tempStandard + add;
                count++;

                if (logReturn < averageLogReturn)
                    tempSemi = tempSemi + add;
            }

            if (count <= 1)
            {
                stdDeviation = 0d;
                semiDeviation = 0d;
            }
            else
            {
                stdDeviation = Math.sqrt(tempStandard / (count - 1) * count);
                semiDeviation = Math.sqrt(tempSemi / (count - 1) * count);
            }
        }

        private double logAverage(double[] returns, IntPredicate filter)
        {
            double sum = 0;
            int count = 0;

            for (int ii = 0; ii < returns.length; ii++)
            {
                if (!filter.test(ii))
                    continue;

                sum += Math.log(1 + returns[ii]);
                count++;
            }

            if (count == 0)
                return 0;

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
    {
    }
}
