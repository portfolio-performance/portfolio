package name.abuchen.portfolio.online;

/**
 * Exception thrown by the feed provider if there is a configuration problem
 * with the instrument. For example, if the server returns a 404 (not found)
 * then the feed just does not support this instrument.
 */
public class FeedConfigurationException extends QuoteFeedException
{
    private static final long serialVersionUID = 1L;

    public FeedConfigurationException()
    {
        super();
    }
}
