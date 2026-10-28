package name.abuchen.portfolio.rest;

import java.text.MessageFormat;

import name.abuchen.portfolio.PortfolioLog;

/**
 * How much work the API takes on at once: calculations running, calculations
 * waiting for one of those slots, and HTTP requests served.
 * <p/>
 * The number of calculation slots can be raised with the system property
 * {@value #CALCULATION_SLOTS_PROPERTY} - in {@code PortfolioPerformance.ini}
 * after {@code -vmargs} - and is read once, when the server starts. There is
 * deliberately no preference page for it: two slots suit a personal API, and
 * every slot is a calculation that holds a whole file's daily valuation in
 * memory.
 */
public final class RestApiLimits
{
    public static final String CALCULATION_SLOTS_PROPERTY = "name.abuchen.portfolio.rest.calculationSlots"; //$NON-NLS-1$

    public static final int DEFAULT_CALCULATION_SLOTS = 2;
    public static final int MIN_CALCULATION_SLOTS = 1;
    public static final int MAX_CALCULATION_SLOTS = 8;

    /** how many calculations may wait for a slot, per slot */
    private static final int QUEUED_PER_SLOT = 2;

    /**
     * Requests served at once beyond the calculations: initialize, ping, the
     * cheap reads and a cancellation must still get through while every slot
     * is taken and the queue is full.
     */
    private static final int RESERVED_REQUESTS = 6;

    /** read once: the property cannot change while the application runs */
    private static final class Holder
    {
        private static final int SLOTS = resolve(System.getProperty(CALCULATION_SLOTS_PROPERTY),
                        Runtime.getRuntime().availableProcessors(), true);
    }

    private RestApiLimits()
    {
    }

    /** how many calculations run at once */
    public static int calculationSlots()
    {
        return Holder.SLOTS;
    }

    /** how many calculations may wait for a slot before the next one is refused */
    public static int calculationQueue()
    {
        return calculationQueue(calculationSlots());
    }

    /**
     * How many HTTP requests are served at once: every running and every
     * waiting calculation holds one, plus the reserve.
     */
    public static int maxConcurrentRequests()
    {
        return maxConcurrentRequests(calculationSlots());
    }

    /* package */ static int calculationQueue(int slots)
    {
        return QUEUED_PER_SLOT * slots;
    }

    /* package */ static int maxConcurrentRequests(int slots)
    {
        return slots + calculationQueue(slots) + RESERVED_REQUESTS;
    }

    /**
     * The slot count for a property value on a machine with that many
     * processors: the value if it is a number from 1 to 8, the default
     * otherwise - and never more than the processors less one, which stays
     * free for the user interface. Not private: a test drives it with values
     * no real machine is configured with.
     */
    /* package */ static int resolve(String value, int processors, boolean log)
    {
        var requested = DEFAULT_CALCULATION_SLOTS;

        if (value != null && !value.isBlank())
        {
            try
            {
                var parsed = Integer.parseInt(value.strip());
                if (parsed < MIN_CALCULATION_SLOTS || parsed > MAX_CALCULATION_SLOTS)
                    warn(log, MessageFormat.format("{0}={1} is outside {2} to {3}; using {4}", //$NON-NLS-1$
                                    CALCULATION_SLOTS_PROPERTY, value, MIN_CALCULATION_SLOTS, MAX_CALCULATION_SLOTS,
                                    parsed < MIN_CALCULATION_SLOTS ? MIN_CALCULATION_SLOTS : MAX_CALCULATION_SLOTS));
                requested = Math.clamp(parsed, MIN_CALCULATION_SLOTS, MAX_CALCULATION_SLOTS);
            }
            catch (NumberFormatException e)
            {
                warn(log, MessageFormat.format("{0}={1} is not a number; using {2}", //$NON-NLS-1$
                                CALCULATION_SLOTS_PROPERTY, value, DEFAULT_CALCULATION_SLOTS));
            }
        }

        var available = Math.max(MIN_CALCULATION_SLOTS, processors - 1);
        if (requested > available)
        {
            // the default is capped silently: a two-core machine was never
            // configured, so there is nothing to warn the user about
            if (value != null && !value.isBlank())
                warn(log, MessageFormat.format("{0}={1} exceeds the {2} processors less one; using {3}", //$NON-NLS-1$
                                CALCULATION_SLOTS_PROPERTY, value, processors, available));
            requested = available;
        }

        return requested;
    }

    private static void warn(boolean log, String message)
    {
        if (log)
            PortfolioLog.warning(message);
    }
}
