package name.abuchen.portfolio.math;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.util.Interval;

public class RiskTest
{
    // Tolerance for floating-point comparisons
    private static final double TOLERANCE = 0.1e-10;

    // Single return value used in tests
    private static final double SINGLE_RETURN_VALUE = 0.1;

    // Empty returns array
    private static final double[] EMPTY_RETURNS = new double[0];

    private LocalDate[] getDates(int count)
    {
        LocalDate[] dates = new LocalDate[count];
        LocalDate startDate = LocalDate.of(2015, 1, 1);
        for (int i = 0; i < count; i++)
        {
            dates[i] = startDate.plusDays(i);
        }
        return dates;
    }

    /**
     * Tests the Drawdown calculation when there is no drawdown. 
     * Scenario: All values are increasing sequentially, hence no drawdown should occur.
     * Expects: Maximum drawdown to be 0 and duration of the drawdown to be 1
     * day.
     */
    @Test
    public void testDrawdownNoDrawdown()
    {
        LocalDate[] dates = getDates(10);
        double[] values = new double[10];
        for (int i = 0; i < values.length; i++)
            values[i] = i;

        Drawdown drawdown = new Drawdown(values, dates, 0);

        assertThat(drawdown.getMaxDrawdown(), is(0d));
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(1L));
    }
    

    /**
     * Tests the Drawdown calculation with a simple peak.
     * Scenario: Values rise to a peak and then drop, before recovering.
     * Expects: Maximum drawdown to be calculated correctly with specified tolerance and duration of 1 day.
     */
    @Test
    public void testDrawdownSimplePeak()
    {
        Drawdown drawdown = new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(4), 0);

        assertThat(drawdown.getMaxDrawdown(), closeTo(4d / 3, TOLERANCE));
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(1L));
    }

    /**
     * Tests the Drawdown calculation with multiple peaks.
     * Scenario: Values include multiple peaks and valleys.
     * Expects: Maximum drawdown to be correctly calculated and the duration to reflect the longest duration of drawdown.
     */
    @Test
    public void testDrawdownMultiplePeaks()
    {
        Drawdown drawdown = new Drawdown(new double[] { 1, 1, -0.5, 1, 1, 2, 3, 4, 5, 6 }, getDates(10), 0);

        assertThat(drawdown.getMaxDrawdown(), is(0.75d));
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(5L));
    }

    /**
     * Tests the Drawdown constructor with mismatched lengths of values and dates arrays.
     * Scenario: The lengths of the values and dates arrays do not match.
     * Expects: IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDrawdownInputArgumentsLengthNotTheSame()
    {
        new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(3), 0);
    }

    /**
     * Tests the Drawdown constructor with an invalid start index.
     * Scenario: The start index is out of the bounds of the values array.
     * Expects: IllegalArgumentException to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDrawdownStartAtIllegalPosition()
    {
        new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(4), 4);
    }

    /**
     * Tests the Volatility calculation with a set of returns.
     * Scenario: A defined set of return values is used.
     * Expects: Standard deviation and semi-deviation to be calculated within the specified tolerance.
     */
    @Test
    public void testVolatility()
    {
        double[] delta = { 0.005, -1 / 300d, -0.005, 0.01, 0.01, -0.005 };
        Volatility volatility = new Volatility(delta, index -> true);

        assertThat(volatility.getStandardDeviation(), closeTo(0.017736692475, TOLERANCE));
        assertThat(volatility.getSemiDeviation(), closeTo(0.012188677034, TOLERANCE));
    }

    /**
     * Tests the Volatility calculation while skipping the first return value.
     * Scenario: The first return value is skipped in the volatility calculation.
     * Expects: Volatility to be consistent with calculations that include the first value, within specified tolerance.
     */
    @Test
    public void testVolatilityWithSkip()
    {
        double[] delta = { 0, 0.005, -1 / 300d, -0.005, 0.01, 0.01, -0.005 };
        Volatility volatility = new Volatility(delta, index -> index > 0);

        assertThat(volatility.getStandardDeviation(), closeTo(0.017736692475, TOLERANCE));
        assertThat(volatility.getSemiDeviation(), closeTo(0.012188677034, TOLERANCE));
    }

    /**
     * Tests the Volatility calculation with constant returns.
     * Scenario: All return values are constant.
     * Expects: Standard deviation and semi-deviation to be zero.
     */
    @Test
    public void testVolatilityWithConstantReturns()
    {
        double[] returns = new double[20];
        Arrays.fill(returns, SINGLE_RETURN_VALUE);

        Volatility volatility = new Volatility(returns, index -> true);

        assertThat(volatility.getStandardDeviation(), closeTo(0d, TOLERANCE));
        assertThat(volatility.getSemiDeviation(), closeTo(0d, TOLERANCE));
    }

    /**
     * Tests the Volatility calculation with a single return value.
     * Scenario: Only one return value is provided.
     * Expects: Standard deviation and semi-deviation to be zero.
     */
    @Test
    public void testVolatilityWithSingleValue()
    {
        Volatility volatility = new Volatility(new double[] { SINGLE_RETURN_VALUE }, index -> true);

        assertThat(volatility.getStandardDeviation(), is(0d));
        assertThat(volatility.getSemiDeviation(), is(0d));
    }

    /**
     * Tests the Volatility calculation with an empty returns array.
     * Scenario: The returns array is empty.
     * Expects: Standard deviation and semi-deviation to be zero.
     */
    @Test
    public void testVolatilityEmpty()
    {
        Volatility volatility = new Volatility(EMPTY_RETURNS, index -> true);

        assertThat(volatility.getStandardDeviation(), is(0d));
        assertThat(volatility.getSemiDeviation(), is(0d));
    }

    /**
     * Tests the calculation of the recovery time based on a predefined set of values and dates.
     * Scenario: Given values and corresponding dates, calculates the longest recovery time.
     * Expects: The actual recovery time to match the expected interval.
     */
    @Test
    public void testDrawdownRecoveryTimeCalculation()
    {
        double[] values = { 100, 120, 110, 80, 85, 90, 95, 100, 105, 110, 115, 120 };
        LocalDate[] dates = getDates(12);

        Drawdown drawdown = new Drawdown(values, dates, 0);

        // Recovery starts after the bottom on 1/4/2015
        // Recovery ends on 1/12/2015
        Interval expectedRecoveryTime = Interval.of(LocalDate.of(2015, 1, 4), LocalDate.of(2015, 1, 12));

        assertThat(drawdown.getLongestRecoveryTime(), is(expectedRecoveryTime));
    }

}
