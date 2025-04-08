package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.IntPredicate;

import name.abuchen.portfolio.util.Interval;

/**
 * The Risk class encapsulates financial risk metrics including drawdown and
 * volatility.
 * <p>
 * The Drawdown class calculates the maximum drawdown, the duration of the
 * drawdown, and the recovery time for a given series of asset values over time.
 * <p>
 * The Volatility class calculates standard deviation and semi-deviation of
 * asset returns, which are key measures of an asset's risk and return profile.
 */

public final class Risk
{

    /**
     * The Drawdown class calculates the drawdown metrics for a series of asset
     * values.
     * <p>
     * Drawdown represents the decline from a historical peak in some variable
     * (typically the value of an investment), and is usually quoted as a
     * percentage. This class provides methods to get the maximum drawdown, the
     * interval of the maximum drawdown, the maximum drawdown duration, and the
     * longest recovery time.
     */
    public static class Drawdown
    {
        private double maxDD;
        private Interval maxDDDuration;
        private Interval intervalMaxDD;
        private Interval recoveryTime;
        private double[] drawdownDataSerie;

        public Drawdown(double[] values, LocalDate[] dates, int startAt)
        {
            if (values.length != dates.length)
                throw new IllegalArgumentException("Values and dates mismatch: " + values.length + " != " + dates.length); //$NON-NLS-1$ //$NON-NLS-2$

            if (startAt >= values.length)
                throw new IllegalArgumentException("Unable to start at " + startAt + ": Values only contains "  + values.length + " elements"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
            drawdownDataSerie = new double[values.length];

            for (int ii = startAt; ii < values.length; ii++)
            {
                double value = values[ii] + 1;
                currentDrawdownDuration = Interval.of(lastPeakDate, dates[ii]);
                currentRecoveryTime = Interval.of(lastBottomDate, dates[ii]);

                if (value > peak)
                {
                    peak = value;
                    lastPeakDate = dates[ii];
                    drawdownDataSerie[ii] = 0;

                    if (currentRecoveryTime.isLongerThan(recoveryTime))
                        recoveryTime = currentRecoveryTime;

                    lastBottomDate = dates[ii];
                    bottom = value;
                }
                else
                {
                    double drawdown = (peak - value) / peak;
                    drawdownDataSerie[ii] = -drawdown;
                    if (drawdown > maxDD)
                    {
                        maxDD = drawdown;
                        intervalMaxDD = Interval.of(lastPeakDate, dates[ii]);
                    }

                    if (value < bottom)
                    {
                        bottom = value;
                        lastBottomDate = dates[ii];
                    }
                }

                if (currentDrawdownDuration != null && currentDrawdownDuration.isLongerThan(maxDDDuration))
                    maxDDDuration = currentDrawdownDuration;
            }

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

        public double[] getMaxDrawdownSerie()
        {
            return drawdownDataSerie;
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

                tempStandard += add;
                count++;

                if (logReturn < averageLogReturn)
                    tempSemi += add;
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

            return count == 0 ? 0 : sum / count;
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
