package name.abuchen.portfolio.rest;

import java.time.Instant;
import java.util.Optional;

/**
 * The last connection the server turned away for want of a valid token, for the
 * preference page to show. A client whose token is wrong never becomes an entry
 * in the client list, and the MCP client measured for ADR 0005 reports nothing
 * at all on failure, so without this the misconfiguration is invisible from
 * both ends. No token material is kept - only whether one was offered.
 * <p/>
 * Only an actual refusal belongs here: a {@code /v1} route answering 401 does,
 * the MCP endpoint's token-free handshake and a {@code /.well-known/*} probe do
 * not. A warning about a request that worked is worse than none, because the
 * user cannot act on it.
 * <p/>
 * Static for the same reason {@link RestApiWorkspace#getClientStore()} is: it
 * outlives a server restart and the preference page holds no reference to it.
 */
public final class RejectedConnections
{
    /**
     * @param userAgent
     *            what the caller called itself, or null - the one field that
     *            says which connector this was
     * @param tokenPresented
     *            false when no bearer token was offered at all
     */
    public record Rejection(Instant when, String path, String userAgent, boolean tokenPresented)
    {
    }

    private static volatile Rejection last;

    private RejectedConnections()
    {
    }

    public static void record(String path, String userAgent, boolean tokenPresented)
    {
        last = new Rejection(Instant.now(), path, userAgent, tokenPresented);
    }

    public static Optional<Rejection> last()
    {
        return Optional.ofNullable(last);
    }

    /** for tests, which must not see one another's refusals */
    public static void clear()
    {
        last = null;
    }
}
