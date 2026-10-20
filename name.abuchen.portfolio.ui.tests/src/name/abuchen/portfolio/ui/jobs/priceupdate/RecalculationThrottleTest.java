package name.abuchen.portfolio.ui.jobs.priceupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class RecalculationThrottleTest
{
    private static final long TICK = 300;
    private static final long TOLERANCE = TICK / 2;

    /**
     * Simulates the periodic check of the price update job (every 300 ms over
     * 30 seconds, new prices always available) and counts the
     * recalculations.
     */
    private static int simulate(long interval)
    {
        RecalculationThrottle throttle = new RecalculationThrottle(interval, TOLERANCE, 0);

        int count = 0;
        for (long now = 0; now < 30_000; now += TICK)
        {
            if (throttle.isDue(now))
            {
                throttle.recalculated(now);
                count++;
            }
        }
        return count;
    }

    @Test
    public void testIntervalIsRespected()
    {
        RecalculationThrottle throttle = new RecalculationThrottle(3000, TOLERANCE, 0);

        assertThat(throttle.isDue(0), is(false));
        assertThat(throttle.isDue(2700), is(false));
        assertThat(throttle.isDue(2850), is(true));

        throttle.recalculated(3000);

        assertThat(throttle.isDue(3300), is(false));
        assertThat(throttle.isDue(5700), is(false));
        assertThat(throttle.isDue(6000), is(true));
    }

    @Test
    public void testShortestIntervalRecalculatesOnEveryCheck()
    {
        // the timer is not exact: a check slightly before 300 ms counts
        RecalculationThrottle throttle = new RecalculationThrottle(300, TOLERANCE, 0);

        assertThat(throttle.isDue(290), is(true));
        throttle.recalculated(290);
        assertThat(throttle.isDue(580), is(true));

        // every check except the very first one at start
        assertThat(simulate(300), is(99));
    }

    @Test
    public void testDefaultIntervalLimitsRecalculations()
    {
        assertThat(simulate(3000), is(9));
        assertThat(simulate(10000), is(3));
    }

    @Test
    public void testOnlyAfterUpdateNeverRecalculatesDuringUpdate()
    {
        RecalculationThrottle throttle = new RecalculationThrottle(RecalculationThrottle.ONLY_AFTER_UPDATE, TOLERANCE,
                        0);

        assertThat(throttle.isDue(Long.MAX_VALUE / 2), is(false));
        assertThat(simulate(RecalculationThrottle.ONLY_AFTER_UPDATE), is(0));
    }
}
