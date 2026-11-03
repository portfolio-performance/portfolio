package name.abuchen.portfolio.util;

/**
 * Time limits for the JDK's {@code com.sun.net.httpserver}. The JDK reads them
 * from system properties once, when the first {@code HttpServer} of the
 * process is created, and applies them to every server from then on. So every
 * code path that creates one calls {@link #apply()} first - whichever starts
 * first, the limits hold for all. A value given on the command line wins.
 */
public final class HttpServerDefaults
{
    /**
     * Receiving a request - its head and its body - may take this many seconds.
     * Without it, a client that sends half a request head and stops keeps its
     * socket and a thread for good: nothing above the JDK sees the request
     * before the head is complete.
     */
    private static final String MAX_REQUEST_SECONDS = "60"; //$NON-NLS-1$

    /**
     * Open connections per server. A connection is handed to a thread before
     * its request head is read, so no bound above the JDK counts one that sends
     * half a head and waits; this one does. Far above what a local client
     * opens - a few, kept alive - and above the requests served at once.
     */
    private static final String MAX_CONNECTIONS = "100"; //$NON-NLS-1$

    /**
     * Sending a response may take this many seconds. Once a streamed response
     * has started, closing the exchange no longer closes the connection - it
     * writes the last chunk, which blocks like any other write to a client that
     * stopped reading - so this is the one thing that frees that writer. Far
     * above what any request may legitimately take: 10 s for the UI thread, 30
     * s in the queue, 120 s for a calculation.
     */
    private static final String MAX_RESPONSE_SECONDS = "600"; //$NON-NLS-1$

    private HttpServerDefaults()
    {
    }

    public static synchronized void apply()
    {
        setIfUnset("sun.net.httpserver.maxReqTime", MAX_REQUEST_SECONDS); //$NON-NLS-1$
        setIfUnset("jdk.httpserver.maxConnections", MAX_CONNECTIONS); //$NON-NLS-1$
        setIfUnset("sun.net.httpserver.maxRspTime", MAX_RESPONSE_SECONDS); //$NON-NLS-1$
    }

    private static void setIfUnset(String key, String value)
    {
        if (System.getProperty(key) == null)
            System.setProperty(key, value);
    }
}
