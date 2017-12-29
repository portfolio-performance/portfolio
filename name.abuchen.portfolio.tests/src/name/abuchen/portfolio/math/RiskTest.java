package name.abuchen.portfolio.math;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;

import org.junit.Test;

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
    public void testDrawdown()
    {
        int size = 10;
        LocalDate[] dates = getDates(size);
        double[] values = new double[size];
        for (int i = 0; i < size; i++)
            values[i] = i;

        Drawdown drawdown = new Drawdown(values, dates, 0);
        // Every new value is a new peak, so there never is a drawdown and
        // therefore the magnitude is 0
        assertThat(drawdown.getMaxDrawdown(), is(0d));
        // Drawdown duration is the longest duration between peaks. Every value
        // is a peak, so 1 day is every time the duration. The fact that there
        // is never a drawdown does not negate the duration
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(1l));

        drawdown = new Drawdown(new double[] { 1, 1, -0.5, 1, 1, 2, 3, 4, 5, 6 }, dates, 0);
        // the drawdown is from 2 to 0.5 which is 1.5 or 75% of 2
        assertThat(drawdown.getMaxDrawdown(), is(0.75d));
        // the first peak is the first 2. The second 2 is not a peak, the next
        // peak is the 3, which is 5 days later
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(5l));

        drawdown = new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(4), 0);
        // The drawdown is from 1.2 to -0.4 or 1.6, which is 4/3 from 1.2
        assertThat(drawdown.getMaxDrawdown(), closeTo(4d / 3, 0.1e-10));
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
}
