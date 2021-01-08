package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;

public class RiskTest
{

    private LocalDate[] getDates(int size)
    {
        LocalDate[] dates = new LocalDate[size];
        for (int i = 0; i < size; i++)
            dates[i] = LocalDate.of(2015, 1, i + 1);
        return dates;
    }

    @Test
    public void testDrawdownNoDrawdown()
    {
        int size = 10;
        LocalDate[] dates = getDates(size);
        double[] values = new double[size];
        for (int i = 0; i < size; i++)
            values[i] = i;
        Drawdown drawdown = new Drawdown(values, dates, 0);

        double maxDrawdown = drawdown.getMaxDrawdown();
        long maxDrawdownDuration = drawdown.getMaxDrawdownDuration().getDays();

        // Every new value is a new peak, so there never is a drawdown and
        // therefore the magnitude is 0
        assertThat(maxDrawdown, is(0d));
        // Drawdown duration is the longest duration between peaks. Every value
        // is a peak, so 1 day is every time the duration. The fact that there
        // is never a drawdown does not negate the duration
        assertThat(maxDrawdownDuration, is(1l));
    }

    @Test
    public void testDrawdownSimplePeak()
    {
        Drawdown drawdown = new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(4), 0);

        double maxDrawdown = drawdown.getMaxDrawdown();
        long maxDrawdownDuration = drawdown.getMaxDrawdownDuration().getDays();

        // The drawdown is from 1.2 to -0.4 or 1.6, which is 4/3 from 1.2
        assertThat(maxDrawdown, closeTo(4d / 3, 0.1e-10));
        assertThat(maxDrawdownDuration, is(1l));
    }

    @Test
    public void testDrawdownMultiplePeaks()
    {
        Drawdown drawdown = new Drawdown(new double[] { 1, 1, -0.5, 1, 1, 2, 3, 4, 5, 6 }, getDates(10), 0);

        double maxDrawdown = drawdown.getMaxDrawdown();
        long maxDrawdownDuration = drawdown.getMaxDrawdownDuration().getDays();

        // the drawdown is from 2 to 0.5 which is 1.5 or 75% of 2
        assertThat(maxDrawdown, is(0.75d));
        // the first peak is the first 2. The second 2 is not a peak, the next
        // peak is the 3, which is 5 days later
        assertThat(maxDrawdownDuration, is(5l));
    }

   
    @Test(expected = IllegalArgumentException.class)
    public void testDrawdownInputArgumentsLengthNotTheSame()
    {
        new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(3), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDrawdownStartAtIllegalPosition()
    {
        new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(4), 4);
    }

    @Test
    public void testVolatility()
    {
        double[] delta = new double[] { 0.005, -1 / 300d, -0.005, 0.01, 0.01, -0.005 };
        Volatility volatility = new Volatility(delta, index -> true);
        // calculated reference values with excel
        assertThat(volatility.getStandardDeviation(), closeTo(0.017736692475, 0.1e-10));
        assertThat(volatility.getSemiDeviation(), closeTo(0.012188677034, 0.1e-10));
    }

    @Test
    public void testVolatilityWithSkip()
    {
        double[] delta = new double[] { 0, 0.005, -1 / 300d, -0.005, 0.01, 0.01, -0.005 };
        Volatility volatility = new Volatility(delta, index -> index > 0);
        assertThat(volatility.getStandardDeviation(), closeTo(0.017736692475, 0.1e-10));
        assertThat(volatility.getSemiDeviation(), closeTo(0.012188677034, 0.1e-10));
    }

    @Test
    public void testVolatilityWithConstantReturns()
    {
        double[] returns = new double[20];
        Arrays.fill(returns, 0.1);

        Volatility volatility = new Volatility(returns, index -> true);
        assertThat(volatility.getStandardDeviation(), closeTo(0d, 0.1e-10));
        assertThat(volatility.getSemiDeviation(), closeTo(0d, 0.1e-10));
    }

    @Test
    public void testVolatilityWithSingleValue()
    {
        double[] returns = new double[1];
        Arrays.fill(returns, 0.1);

        Volatility volatility = new Volatility(returns, index -> true);
        assertThat(volatility.getStandardDeviation(), is(0d));
        assertThat(volatility.getSemiDeviation(), is(0d));
    }

    @Test
    public void testVolatilityEmpty()
    {
        double[] returns = new double[0];
        Arrays.fill(returns, 0.1);

        Volatility volatility = new Volatility(returns, index -> true);
        assertThat(volatility.getStandardDeviation(), is(0d));
        assertThat(volatility.getSemiDeviation(), is(0d));
    }
}
