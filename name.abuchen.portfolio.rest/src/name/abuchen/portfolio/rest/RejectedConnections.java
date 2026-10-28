package name.abuchen.portfolio.rest;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

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
        /** a header was sent, but under another scheme than {@code Bearer} - {@code Token …}, say */
        NO_BEARER_SCHEME,
        /** a bearer token was offered and it was not one of the accepted ones */
        INVALID_TOKEN
    }

    /** how much of a caller-supplied string is kept for display */
    private static final int MAX_DISPLAY_CHARS = 200;

    /** C0 and C1 control characters, plus the Unicode line and paragraph separators */
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cc}\\u2028\\u2029]"); //$NON-NLS-1$

    /**
     * Invisible format characters - bidi overrides and isolates, zero-width
     * characters - which would let a caller reorder how its label reads
     */
    private static final Pattern FORMAT_CHARACTERS = Pattern.compile("\\p{Cf}"); //$NON-NLS-1$

    /**
     * @param userAgent
     *            what the caller called itself, or null - the one field that
     *            says which connector this was
     * @param tokenPresented
     *            false when no bearer token was offered at all
     * @param reason
     *            which of the three refusals this was
     */
    public record Rejection(Instant when, String path, String userAgent, boolean tokenPresented, Reason reason)
    {
    }

    private static volatile Rejection last;

    private RejectedConnections()
    {
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
     * the preference page: bounded in length and with control characters - line
     * breaks, tabs, escape sequences - replaced by spaces, so a request cannot
     * break or garble a label.
     */
    private static String display(String value)
    {
        if (value == null)
            return null;

        var visible = FORMAT_CHARACTERS.matcher(value).replaceAll(""); //$NON-NLS-1$
        var cleaned = CONTROL_CHARACTERS.matcher(visible).replaceAll(" "); //$NON-NLS-1$
        return cleaned.length() <= MAX_DISPLAY_CHARS ? cleaned : cleaned.substring(0, MAX_DISPLAY_CHARS) + "\u2026"; //$NON-NLS-1$
    }
}
