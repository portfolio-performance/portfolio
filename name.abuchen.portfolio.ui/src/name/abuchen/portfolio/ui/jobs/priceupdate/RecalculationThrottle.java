package name.abuchen.portfolio.ui.jobs.priceupdate;

/**
 * Decides whether the views are recalculated while a price update is running.
 * <p/>
 * With many securities, new prices arrive continuously. Each recalculation of
 * the active view (for example the statement of assets with performance
 * columns per category) can take longer than the progress interval of the
 * price update. Without throttling, the user interface would recalculate
 * without a break until the price update has finished.
 */
/* package */ final class RecalculationThrottle
{
    /**
     * Preference value: recalculate the views only after the price update has
     * finished.
     */
    /* package */ static final int ONLY_AFTER_UPDATE = 0;

    private final long interval;
    private final long tolerance;
    private long lastRecalculation;

    /**
     * @param interval
     *            minimum time between two recalculations in milliseconds or
     *            {@link #ONLY_AFTER_UPDATE}
     * @param tolerance
     *            tolerance in milliseconds because the check runs periodically
     *            and the timer is not exact
     * @param start
     *            start of the price update in milliseconds
     */
    /* package */ RecalculationThrottle(long interval, long tolerance, long start)
    {
        this.interval = interval;
        this.tolerance = tolerance;
        this.lastRecalculation = start;
    }

    /**
     * Returns true if the views may be recalculated at the given time.
     */
    /* package */ boolean isDue(long now)
    {
        if (interval <= ONLY_AFTER_UPDATE)
            return false;

        return now - lastRecalculation + tolerance >= interval;
    }

    /* package */ void recalculated(long now)
    {
        this.lastRecalculation = now;
    }
}
