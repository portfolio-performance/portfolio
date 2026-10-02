package name.abuchen.portfolio.oauth;

/**
 * Exception thrown when token retrieval fails due to a network or I/O error
 * (e.g. no internet connection, DNS failure, firewall, proxy issues) rather
 * than an actual authentication failure. Callers should not interpret this as
 * "re-authentication required" and should not show a login dialog.
 */
public class AuthenticationNetworkException extends AuthenticationException
{
    private static final long serialVersionUID = 1L;

    public AuthenticationNetworkException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

