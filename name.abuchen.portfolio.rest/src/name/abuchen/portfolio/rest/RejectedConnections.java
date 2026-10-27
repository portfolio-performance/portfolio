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
    /** why the caller was refused, which the preference page words differently */
    public enum Reason
    {
        /** no {@code Authorization} header at all */
        NO_HEADER,
        /** a header was sent but did not carry the bearer scheme - usually a lowercase {@code bearer} */
        NO_BEARER_SCHEME,
        /** a bearer token was offered and it was not one of the accepted ones */
        INVALID_TOKEN
    }

    /** how much of a caller-supplied string is kept for display */
    private static final int MAX_DISPLAY_CHARS = 200;

    /**
     * @param userAgent
     *            what the caller called itself, or null - the one field that
     *            says which connector this was
     * @param tokenPresented
     *            false when no bearer token was offered at all
     * @param reason
     *            which of the three refusals this was; {@link #NO_HEADER} is what
     *            a caller with a header in the wrong shape looks like from here
     */
    public record Rejection(Instant when, String path, String userAgent, boolean tokenPresented, Reason reason)
    {
    }

    private static volatile Rejection last;

    private RejectedConnections()
    {
    }

    public static void record(String path, String userAgent, boolean tokenPresented)
    {
        record(path, userAgent, tokenPresented, tokenPresented ? Reason.INVALID_TOKEN : Reason.NO_HEADER);
    }

    public static void record(String path, String userAgent, boolean tokenPresented, Reason reason)
    {
        last = new Rejection(Instant.now(), display(path), display(userAgent), tokenPresented, reason);
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

    /**
     * The path and the {@code User-Agent} are attacker-controlled text shown on
     * the preference page: bounded in length and stripped of control characters,
     * so a request cannot put a line break into a label.
     */
    private static String display(String value)
    {
        if (value == null)
            return null;

        var cleaned = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        return cleaned.length() <= MAX_DISPLAY_CHARS ? cleaned : cleaned.substring(0, MAX_DISPLAY_CHARS) + "\u2026"; //$NON-NLS-1$
    }
}
