package name.abuchen.portfolio.oauth;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.UnknownHostException;

import org.junit.Test;

/**
 * Regression tests for the AuthenticationNetworkException contract.
 *
 * Verifies that a plain I/O failure (specifically UnknownHostException) is
 * wrapped in an AuthenticationNetworkException that:
 *   - is a subtype of AuthenticationException (existing callers still compile)
 *   - preserves the original message and cause
 *   - can be caught separately from the broader AuthenticationException so
 *     callers can distinguish network failures from real auth failures
 */
@SuppressWarnings("nls")
public class AuthenticationNetworkExceptionTest
{
    @Test
    public void preservesMessageAndCause()
    {
        var cause = new UnknownHostException("auth.portfolio-performance.info");
        var ex = new AuthenticationNetworkException("unable to connect", cause);

        assertThat(ex.getMessage(), is("unable to connect"));
        assertThat(ex.getCause(), is(cause));
    }

    @Test
    public void isSubtypeOfAuthenticationException()
    {
        var ex = new AuthenticationNetworkException("msg", new UnknownHostException("host"));

        assertThat(ex, instanceOf(AuthenticationException.class));
    }

    @Test
    public void canBeCaughtBeforeBroaderAuthenticationException()
    {
        // If a narrow catch of AuthenticationNetworkException is placed before
        // AuthenticationException, the network exception must land there – not
        // in the broader catch – so callers can suppress the login dialog.
        var ex = new AuthenticationNetworkException("network failure",
                        new UnknownHostException("auth.portfolio-performance.info"));

        boolean caughtAsNetwork = false;
        boolean caughtAsAuth = false;

        try
        {
            throw ex;
        }
        catch (AuthenticationNetworkException e)
        {
            caughtAsNetwork = true;
        }
        catch (AuthenticationException e) // NOSONAR
        {
            caughtAsAuth = true;
        }

        assertThat("must be caught as AuthenticationNetworkException", caughtAsNetwork, is(true));
        assertThat("must NOT fall through to AuthenticationException", caughtAsAuth, is(false));
    }
}

