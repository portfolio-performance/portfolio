package name.abuchen.portfolio.online;

/**
 * Exception thrown by the quote feed provider to indicate that the rate limit
 * of the underlying has been exceeded.
 */
public class RateLimitExceededException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException()
    {
        super();
    }

    public RateLimitExceededException(String message)
    {
        super(message);
    }
}
