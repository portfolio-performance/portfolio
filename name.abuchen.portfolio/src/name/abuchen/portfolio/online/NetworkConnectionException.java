package name.abuchen.portfolio.online;

/**
 * Exception thrown by a quote feed provider to indicate that the request
 * failed due to a network or I/O problem (e.g. no internet connection, DNS
 * failure, firewall, proxy issues). Unlike {@link AuthenticationExpiredException}
 * this does <em>not</em> mean that the user needs to log in again.
 */
public class NetworkConnectionException extends QuoteFeedException
{
    private static final long serialVersionUID = 1L;

    public NetworkConnectionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

