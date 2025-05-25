package name.abuchen.portfolio.oauth;

public class AuthenticationException extends Exception
{

    private static final long serialVersionUID = 1L;

    public AuthenticationException()
    {
        super();
    }

    public AuthenticationException(String message)
    {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
