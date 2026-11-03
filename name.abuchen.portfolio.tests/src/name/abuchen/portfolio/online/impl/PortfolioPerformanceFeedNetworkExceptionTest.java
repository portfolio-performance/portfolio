package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.oauth.AuthenticationNetworkException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.AuthenticationExpiredException;
import name.abuchen.portfolio.online.NetworkConnectionException;
import name.abuchen.portfolio.online.QuoteFeedException;

/**
 * Regression tests that verify the downstream exception mapping inside
 * PortfolioPerformanceFeed when the token refresh call fails with a network
 * error (UnknownHostException, ConnectException, SocketTimeoutException).
 *
 * Before the fix the feed would silently swallow the real cause and re-throw
 * an AuthenticationExpiredException, misleading the user into thinking they
 * need to log in again.
 */
@SuppressWarnings("nls")
public class PortfolioPerformanceFeedNetworkExceptionTest
{
    /** A ticker that is not in the SAMPLE_SYMBOLS set – requires authentication. */
    private static Security nonSampleSecurity()
    {
        var security = new Security();
        security.setTickerSymbol("XYZ.NON-SAMPLE");
        return security;
    }

    private static PortfolioPerformanceFeed feedWithNetworkException(AuthenticationNetworkException ex)
                    throws Exception
    {
        var mockClient = Mockito.mock(OAuthClient.class);
        Mockito.when(mockClient.isAuthenticated()).thenReturn(true);
        Mockito.when(mockClient.getAPIAccessToken()).thenThrow(ex);
        return new PortfolioPerformanceFeed(mockClient);
    }

    // ---- UnknownHostException (DNS failure) ----

    @Test
    public void unknownHostMapsToNetworkConnectionException() throws Exception
    {
        var ex = new AuthenticationNetworkException("no route",
                        new UnknownHostException("auth.portfolio-performance.info"));

        var thrown = assertThrows(QuoteFeedException.class,
                        () -> feedWithNetworkException(ex).getHistoricalQuotes(nonSampleSecurity(), false));

        assertThat(thrown, instanceOf(NetworkConnectionException.class));
        assertThat(thrown instanceof AuthenticationExpiredException, is(false));
        assertThat(thrown.getMessage(), is(ex.getMessage()));
    }

    // ---- ConnectException (connection refused / firewall) ----

    @Test
    public void connectRefusedMapsToNetworkConnectionException() throws Exception
    {
        var ex = new AuthenticationNetworkException("no route",
                        new ConnectException("Connection refused"));

        var thrown = assertThrows(QuoteFeedException.class,
                        () -> feedWithNetworkException(ex).getHistoricalQuotes(nonSampleSecurity(), false));

        assertThat(thrown, instanceOf(NetworkConnectionException.class));
        assertThat(thrown instanceof AuthenticationExpiredException, is(false));
        assertThat(thrown.getMessage(), is(ex.getMessage()));
    }

    // ---- SocketTimeoutException (connection / read timeout) ----

    @Test
    public void socketTimeoutMapsToNetworkConnectionException() throws Exception
    {
        var ex = new AuthenticationNetworkException("no route",
                        new SocketTimeoutException("Read timed out"));

        var thrown = assertThrows(QuoteFeedException.class,
                        () -> feedWithNetworkException(ex).getHistoricalQuotes(nonSampleSecurity(), false));

        assertThat(thrown, instanceOf(NetworkConnectionException.class));
        assertThat(thrown instanceof AuthenticationExpiredException, is(false));
        assertThat(thrown.getMessage(), is(ex.getMessage()));
    }
}

