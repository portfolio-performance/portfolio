package name.abuchen.portfolio.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Test;

import name.abuchen.portfolio.rest.RejectedConnections;
import name.abuchen.portfolio.ui.Messages;

public class RestApiPreferencePageTest
{
    @Test
    public void testPerformOkWithoutOpeningPage()
    {
        // The preference dialog saves every page, including pages whose
        // controls have never been created because the user did not visit them.
        var page = new RestApiPreferencePage(null);

        assertTrue(page.performOk());
    }

    /**
     * Reported in review: each refusal gets its own message - a header in the
     * wrong scheme must not read as a missing token.
     */
    @Test
    public void testEachRefusalHasItsOwnMessage()
    {
        assertEquals(Messages.PrefMsgRestApiRejectedNoBearerScheme, RestApiPreferencePage
                        .messageFor(rejection(false, RejectedConnections.Reason.NO_BEARER_SCHEME)));
        assertEquals(Messages.PrefMsgRestApiRejectedNoToken,
                        RestApiPreferencePage.messageFor(rejection(false, RejectedConnections.Reason.NO_HEADER)));
        assertEquals(Messages.PrefMsgRestApiRejectedInvalidToken, RestApiPreferencePage
                        .messageFor(rejection(true, RejectedConnections.Reason.INVALID_TOKEN)));
    }

    private static RejectedConnections.Rejection rejection(boolean tokenPresented, RejectedConnections.Reason reason)
    {
        return new RejectedConnections.Rejection(Instant.parse("2026-07-20T10:00:00Z"), "/mcp", null, //$NON-NLS-1$ //$NON-NLS-2$
                        tokenPresented, reason);
    }
}
