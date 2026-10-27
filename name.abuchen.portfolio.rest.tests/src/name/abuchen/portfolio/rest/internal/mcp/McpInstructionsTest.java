package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

@SuppressWarnings("nls")
public class McpInstructionsTest
{
    /** Claude Code truncates at exactly 2,048 characters, so the length is not a matter of taste */
    @Test
    public void testItFitsTheOneClientKnownToReadIt()
    {
        assertThat(McpInstructions.INSTRUCTIONS.length() <= McpInstructions.MAX_CHARS, is(true));
    }

    /** ordered by what goes wrong when the truncation cuts it */
    @Test
    public void testTheMostCostlyMisreadingsComeFirst()
    {
        var text = McpInstructions.INSTRUCTIONS;

        assertThat(text.indexOf("not saved to disk") < text.indexOf("fractions"), is(true));
        assertThat(text.indexOf("fractions") < text.indexOf("Vocabulary"), is(true));
    }

    /** the page it names has to be the page that exists */
    @Test
    public void testItPointsAtThePreferencePageByItsRealName()
    {
        assertThat(McpInstructions.INSTRUCTIONS, containsString("Preferences → MCP Server & REST API"));
    }
}
