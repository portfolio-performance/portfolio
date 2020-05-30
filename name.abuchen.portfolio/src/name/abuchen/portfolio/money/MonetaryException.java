package name.abuchen.portfolio.money;

public class MonetaryException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public MonetaryException()
    {
        super();
    }

    public MonetaryException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MonetaryException(String message)
    {
        super(message);
    }

    public MonetaryException(Throwable cause)
    {
        super(cause);
    }
}
