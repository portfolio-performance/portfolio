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

        Drawdown drawdown = new Drawdown(values, dates);
        // Every new value is a new peak, so there never is a drawdown and
        // therefore the magnitude is 0
        assertThat(drawdown.getMaxDrawdown(), is(0d));
        // Drawdown duration is the longest duration between peaks. Every value
        // is a peak, so 1 day is every time the duration. The fact that there
        // is never a drawdown does not negate the duration
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(1l));

        drawdown = new Drawdown(new double[] { 1, 1, -0.5, 1, 1, 2, 3, 4, 5, 6 }, dates);
        // the drawdown is from 2 to 0.5 which is 1.5 or 75% of 2
        assertThat(drawdown.getMaxDrawdown(), is(0.75d));
        // the first peak is the first 2. The second 2 is not a peak, the next
        // peak is the 3, which is 5 days later
        assertThat(drawdown.getMaxDrawdownDuration().getDays(), is(5l));

        drawdown = new Drawdown(new double[] { 0, 0.1, 0.2, -1.4 }, getDates(4));
        // The drawdown is from 1.2 to -0.4 or 1.6, which is 4/3 from 1.2
        assertThat(drawdown.getMaxDrawdown(), closeTo(4d / 3, 0.1e-10));
    }

    @Test
    public void testVolatility()
    {
        double[] delta = new double[] { 0.5, -1 / 3d, -0.5, 1, 1, -0.5 };
        Volatility volatility = new Volatility(delta, index -> true);
        // returns are 0.5, -1/3, -0.5, 1, 1, -0.5 with an average of 7/36
        // the deviation from the average is 11/36 for 0.5, 19/36 for -1/3 and
        // so on each of these deviations is squared and the sum divided by the
        // number of returns (6) the resulting division is
        // root((121+1250+1682+361)/(1296*6)) or 3414/7776
        assertThat(volatility.getStandardDeviation(), closeTo(Math.sqrt(3414d / 7776), 0.1e-10));

        // for semi deviation, only the returns lower than the average are
        // counted so only the -1/3 and the two times -0.5
        // root((361+1250)/(1296*3)) or 1611/7776
        assertThat(volatility.getSemiDeviation(), closeTo(Math.sqrt(1611d / 7776), 0.1e-10));
    }

    @Test
    public void testVolatilityWithSkip()
    {
        double[] delta = new double[] { 0, 0.5, -1 / 3d, -0.5, 1, 1, -0.5 };
        Volatility volatility = new Volatility(delta, index -> index > 0);
        assertThat(volatility.getStandardDeviation(), closeTo(Math.sqrt(3414d / 7776), 0.1e-10));
        assertThat(volatility.getSemiDeviation(), closeTo(Math.sqrt(1611d / 7776), 0.1e-10));
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
