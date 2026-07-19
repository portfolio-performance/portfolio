package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

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
 * error (UnknownHostException / AuthenticationNetworkException).
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

    private static AuthenticationNetworkException networkException()
    {
        return new AuthenticationNetworkException("Unable to connect to the server. Please check your internet connection.",
                        new UnknownHostException("auth.portfolio-performance.info"));
    }

    @Test
    public void networkAuthFailureMapsToNetworkConnectionException() throws Exception
    {
        var networkEx = networkException();
        var mockClient = Mockito.mock(OAuthClient.class);
        Mockito.when(mockClient.isAuthenticated()).thenReturn(true);
        Mockito.when(mockClient.getAPIAccessToken()).thenThrow(networkEx);

        var feed = new PortfolioPerformanceFeed(mockClient);

        var thrown = assertThrows(QuoteFeedException.class,
                        () -> feed.getHistoricalQuotes(nonSampleSecurity(), false));

        assertThat(thrown, instanceOf(NetworkConnectionException.class));
    }

    @Test
    public void networkConnectionExceptionPreservesOriginalMessage() throws Exception
    {
        var networkEx = networkException();
        var mockClient = Mockito.mock(OAuthClient.class);
        Mockito.when(mockClient.isAuthenticated()).thenReturn(true);
        Mockito.when(mockClient.getAPIAccessToken()).thenThrow(networkEx);

        var feed = new PortfolioPerformanceFeed(mockClient);

        var thrown = assertThrows(QuoteFeedException.class,
                        () -> feed.getHistoricalQuotes(nonSampleSecurity(), false));

        assertThat(thrown.getMessage(), is(networkEx.getMessage()));
    }

    @Test
    public void networkAuthFailureDoesNotThrowAuthenticationExpiredException() throws Exception
    {
        // AuthenticationExpiredException tells the UI to show "Authentication
        // expired – login again", which is wrong when the problem is that the
        // server cannot be reached.
        var networkEx = networkException();
        var mockClient = Mockito.mock(OAuthClient.class);
        Mockito.when(mockClient.isAuthenticated()).thenReturn(true);
        Mockito.when(mockClient.getAPIAccessToken()).thenThrow(networkEx);

        var feed = new PortfolioPerformanceFeed(mockClient);

        var thrown = assertThrows(QuoteFeedException.class,
                        () -> feed.getHistoricalQuotes(nonSampleSecurity(), false));

        assertThat("must NOT be AuthenticationExpiredException for a network failure",
                        thrown instanceof AuthenticationExpiredException, is(false));
    }
}

