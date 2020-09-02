package name.abuchen.portfolio.util;

/**
 * Exception thrown by the quote feed provider to indicate that the rate limit
 * of the underlying has been exceeded.
 */
public class RateLimitExceededException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException(String message)
    {
        super(message);
    }
}
