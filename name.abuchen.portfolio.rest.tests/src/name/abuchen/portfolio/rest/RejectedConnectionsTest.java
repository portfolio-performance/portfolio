package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Test;

/**
 * The path and the user agent of a refused caller end up as a label on the
 * preference page, and both are whatever the caller chose to send.
 */
@SuppressWarnings("nls")
public class RejectedConnectionsTest
{
    @After
    public void tearDown()
    {
        RejectedConnections.clear();
    }

    @Test
    public void testControlCharactersCannotBreakTheLabel()
    {
        RejectedConnections.record("/v1/ping", "evil\r\nagent\t\u001b[31mred x", false,
                        RejectedConnections.Reason.NO_HEADER);

        var rejection = RejectedConnections.last().orElseThrow();
        assertThat(rejection.userAgent(), is("evil  agent  [31mred x"));
    }

    /** a bidi override would make the label read in an order the caller chose */
    @Test
    public void testFormatCharactersAreRemoved()
    {
        RejectedConnections.record("/v1/ping", "safe\u202Etxt.exe\u200B", false, RejectedConnections.Reason.NO_HEADER);

        assertThat(RejectedConnections.last().orElseThrow().userAgent(), is("safetxt.exe"));
    }

    @Test
    public void testALongValueIsCut()
    {
        RejectedConnections.record("/v1/ping", "a".repeat(500), false, RejectedConnections.Reason.NO_HEADER);

        assertThat(RejectedConnections.last().orElseThrow().userAgent().length(), is(201));
    }

    @Test
    public void testTheReasonIsKept()
    {
        RejectedConnections.record("/mcp", null, false, RejectedConnections.Reason.NO_BEARER_SCHEME);

        var rejection = RejectedConnections.last().orElseThrow();
        assertThat(rejection.reason(), is(RejectedConnections.Reason.NO_BEARER_SCHEME));
        assertThat(rejection.userAgent(), is((String) null));
    }
}
