package name.abuchen.portfolio.online;

public abstract class QuoteFeedException extends Exception
{
    private static final long serialVersionUID = 1L;

    protected QuoteFeedException()
    {
        super();
    }

    protected QuoteFeedException(String message)
    {
        super(message);
    }

    protected QuoteFeedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
