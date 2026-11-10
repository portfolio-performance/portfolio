package name.abuchen.portfolio.rest.spi;

import java.time.Instant;

/**
 * A pending request of a local program for API access, to be answered by the
 * user. The hosting application shows the request and reports the decision by
 * calling exactly one of the decision methods; late or repeated decisions are
 * silently ignored (the request may have expired meanwhile).
 */
public interface ApiAccessRequest
{
    /** the self-declared name of the requesting program, sanitized for display */
    String getClientName();

    /** when the request expires; the prompt should dismiss itself at this time */
    Instant getExpiresAt();

    /** grant access until the application quits */
    void allowForSession();

    /** grant access persistently */
    void allowAlways();

    void decline();
}
