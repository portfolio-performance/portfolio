package name.abuchen.portfolio.online;

import java.time.Duration;

/**
 * Exception thrown by the quote feed provider to indicate that the rate limit
 * of the underlying has been exceeded.
 */
public class RateLimitExceededException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;

    public RateLimitExceededException(Duration retryAfter, String message)
    {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter()
    {
        return retryAfter;
    }
}
