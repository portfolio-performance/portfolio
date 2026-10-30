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
 * <p/>
 * Likewise {@value #MCP_MAX_RESULT_CHARS_PROPERTY}: how many characters one MCP
 * tool result may hold. The default suits Claude Desktop; a client that keeps
 * less - Claude Code, or another agent - would otherwise cut a result blindly
 * where the server would have sent a page and said where the next one starts.
 */
public final class RestApiLimits
{
    public static final String CALCULATION_SLOTS_PROPERTY = "name.abuchen.portfolio.rest.calculationSlots"; //$NON-NLS-1$

    public static final String MCP_MAX_RESULT_CHARS_PROPERTY = "name.abuchen.portfolio.rest.mcpMaxResultChars"; //$NON-NLS-1$

    /**
     * Under Claude Desktop's measured 150,000 truncation point, roughly 25,000
     * tokens, and enough for a personal portfolio on every collection.
     */
    public static final int DEFAULT_MCP_MAX_RESULT_CHARS = 100_000;
    /** below this, guidance and one page of a list no longer fit together */
    public static final int MIN_MCP_MAX_RESULT_CHARS = 10_000;
    /** Claude Desktop cuts what is larger, and so does every client measured so far */
    public static final int MAX_MCP_MAX_RESULT_CHARS = 150_000;

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

        private static final int MCP_MAX_RESULT_CHARS = resolveMaxResultChars(
                        System.getProperty(MCP_MAX_RESULT_CHARS_PROPERTY), true);
    }

    private RestApiLimits()
    {
    }

    /** how many calculations run at once */
    public static int calculationSlots()
    {
        return Holder.SLOTS;
    }

    /** how many characters one MCP tool result may hold */
    public static int mcpMaxResultChars()
    {
        return Holder.MCP_MAX_RESULT_CHARS;
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

    /**
     * The result size for a property value: the value if it is a number from
     * {@value #MIN_MCP_MAX_RESULT_CHARS} to {@value #MAX_MCP_MAX_RESULT_CHARS},
     * the nearer bound when it is outside, the default when it is no number.
     * Not private: a test drives it.
     */
    /* package */ static int resolveMaxResultChars(String value, boolean log)
    {
        if (value == null || value.isBlank())
            return DEFAULT_MCP_MAX_RESULT_CHARS;

        try
        {
            var parsed = Integer.parseInt(value.strip().replace("_", "")); //$NON-NLS-1$ //$NON-NLS-2$
            var bounded = Math.clamp(parsed, MIN_MCP_MAX_RESULT_CHARS, MAX_MCP_MAX_RESULT_CHARS);
            if (bounded != parsed)
                warn(log, MessageFormat.format("{0}={1} is outside {2} to {3}; using {4}", //$NON-NLS-1$
                                MCP_MAX_RESULT_CHARS_PROPERTY, value, String.valueOf(MIN_MCP_MAX_RESULT_CHARS),
                                String.valueOf(MAX_MCP_MAX_RESULT_CHARS), String.valueOf(bounded)));
            return bounded;
        }
        catch (NumberFormatException e)
        {
            warn(log, MessageFormat.format("{0}={1} is not a number; using {2}", //$NON-NLS-1$
                            MCP_MAX_RESULT_CHARS_PROPERTY, value, String.valueOf(DEFAULT_MCP_MAX_RESULT_CHARS)));
            return DEFAULT_MCP_MAX_RESULT_CHARS;
        }
    }

    private static void warn(boolean log, String message)
    {
        if (log)
            PortfolioLog.warning(message);
    }
}
