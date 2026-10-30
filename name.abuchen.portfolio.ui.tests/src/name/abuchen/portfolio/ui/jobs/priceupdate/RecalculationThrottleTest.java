package name.abuchen.portfolio.ui.jobs.priceupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class RecalculationThrottleTest
{
    private static final long TICK = 300;

    /**
     * Simulates the periodic check of the price update job (every 300 ms over
     * 30 seconds, new prices always available) with recalculations of the
     * given duration and counts the recalculations.
     */
    private static int simulate(long duration)
    {
        RecalculationThrottle throttle = new RecalculationThrottle(0);

        int count = 0;
        for (long now = 0; now < 30_000; now += TICK)
        {
            if (throttle.isDue(now))
            {
                throttle.recalculationStarted(now);
                throttle.recalculationFinished(now + duration);
                count++;
            }
        }
        return count;
    }

    @Test
    public void testFirstRecalculationIsImmediatelyDue()
    {
        RecalculationThrottle throttle = new RecalculationThrottle(0);

        // the user sees the first arrived prices right away
        assertThat(throttle.isDue(0), is(true));
    }

    @Test
    public void testRecalculationInFlightIsNeverDue()
    {
        RecalculationThrottle throttle = new RecalculationThrottle(0);

        throttle.recalculationStarted(1000);

        // even after the minimum interval, no second recalculation while the
        // first one is still running
        assertThat(throttle.isDue(1300), is(false));
        assertThat(throttle.isDue(10_000), is(false));

        // duration of 1 second: the minimum interval of 3 seconds applies
        throttle.recalculationFinished(2_000);
        assertThat(throttle.isDue(4_999), is(false));
        assertThat(throttle.isDue(5_000), is(true));
    }

    @Test
    public void testFastRecalculationUsesMinimumInterval()
    {
        RecalculationThrottle throttle = new RecalculationThrottle(0);

        throttle.recalculationStarted(0);
        throttle.recalculationFinished(200);

        assertThat(throttle.isDue(3199), is(false));
        assertThat(throttle.isDue(3200), is(true));
    }

    @Test
    public void testSlowRecalculationExtendsTheInterval()
    {
        // a recalculation of 4 seconds delays the next one until
        // 4000 + 2 × 4000 = 12000, i.e. the UI thread spends at most a third
        // of its time recalculating
        RecalculationThrottle throttle = new RecalculationThrottle(0);

        throttle.recalculationStarted(0);
        throttle.recalculationFinished(4000);

        assertThat(throttle.isDue(11_999), is(false));
        assertThat(throttle.isDue(12_000), is(true));
    }

    @Test
    public void testFastRecalculationsRefreshEveryMinimumInterval()
    {
        // at t = 0, 3000, 6000, ... up to t = 27000
        assertThat(simulate(0), is(10));
    }

    @Test
    public void testSlowRecalculationsReduceTheRate()
    {
        // 2 seconds duration: due at t = 0, 6000, 12000, 18000 and 24000
        assertThat(simulate(2_000), is(5));

        // 10 seconds duration: due at t = 0, then blocked until t = 30000
        assertThat(simulate(10_000), is(1));
    }
}
