package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * The session store is bounded and displaces the session used least recently.
 * A client that only ever sends JSON is checked with {@code exists} and nothing
 * else, so that check has to count as a use.
 */
public class McpSessionsTest
{
    @Test
    public void testASessionInUseIsNotDisplaced()
    {
        var sessions = new McpSessions();
        var active = sessions.open();
        var idle = sessions.open();
        for (int ii = 2; ii < McpSessions.MAX_SESSIONS; ii++)
            sessions.open();

        // the active client sends a request, the idle one does not
        assertThat(sessions.exists(active), is(true));
        sessions.open();

        assertThat(sessions.exists(active), is(true));
        assertThat(sessions.exists(idle), is(false));
    }
}
