package name.abuchen.portfolio.ui.jobs.priceupdate;

/**
 * Decides whether the views are recalculated while a price update is running.
 * <p/>
 * With many securities, new prices arrive continuously. Each recalculation of
 * the active view (for example the statement of assets with performance
 * columns per category) can take longer than the progress interval of the
 * price update. Without throttling, the user interface would recalculate
 * without a break until the price update has finished.
 * <p/>
 * The throttle works adaptively instead of with a configured interval: while a
 * recalculation is in flight, the next one is never due. Once it has finished,
 * the next recalculation is due at <i>finished + max(MINIMUM_INTERVAL, FACTOR
 * × duration)</i>. With FACTOR = 2 the UI thread spends at most about a third
 * of its time recalculating: small files keep refreshing every
 * MINIMUM_INTERVAL, while a file whose recalculation takes longer stays
 * responsive in between without anyone tuning a preference.
 */
/* package */ final class RecalculationThrottle
{
    /**
     * Minimum time in milliseconds between two recalculations of the views.
     */
    /* package */ static final long MINIMUM_INTERVAL = 3000L;

    /**
     * A recalculation that took <i>duration</i> milliseconds blocks the next
     * one for FACTOR × duration milliseconds.
     */
    private static final double FACTOR = 2.0;

    private long nextDue;
    private boolean inFlight;
    private long startedAt;

    /**
     * @param start
     *            start of the price update in milliseconds. The very first
     *            recalculation is immediately due, so the user sees the first
     *            arrived prices right away.
     */
    /* package */ RecalculationThrottle(long start)
    {
        this.nextDue = start;
    }

    /**
     * Returns true if the views may be recalculated at the given time. Never
     * due while a recalculation is in flight.
     */
    /* package */ synchronized boolean isDue(long now)
    {
        return !inFlight && now >= nextDue;
    }

    /* package */ synchronized void recalculationStarted(long now)
    {
        this.inFlight = true;
        this.startedAt = now;
    }

    /**
     * Called once the recalculation has finished. The longer it took, the
     * longer the next recalculation is delayed.
     * <p/>
     * This method is called on the UI thread, while {@link #isDue(long)} and
     * {@link #recalculationStarted(long)} are called on the scheduler thread
     * of the price update job. The methods are synchronized so that the state
     * changes as one unit and the scheduler always sees the updated deadline.
     */
    /* package */ synchronized void recalculationFinished(long now)
    {
        this.inFlight = false;

        long duration = now - this.startedAt;
        this.nextDue = now + Math.max(MINIMUM_INTERVAL, (long) (duration * FACTOR));
    }
}
