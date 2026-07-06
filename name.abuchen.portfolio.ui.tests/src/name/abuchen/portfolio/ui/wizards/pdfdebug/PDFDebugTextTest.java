package name.abuchen.portfolio.ui.wizards.pdfdebug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Test;

@SuppressWarnings("nls")
public class PDFDebugTextTest
{
    @Test
    public void testBuildStructure()
    {
        var result = PDFDebugText.build("1.8.x-test", "HELLO 123");

        assertThat(result, startsWith("```\nPDFBox Version: 1.8.x-test\n"));
        assertThat(result, containsString("Portfolio Performance Version: "));
        assertThat(result, containsString("System: "));
        assertThat(result, containsString("-----------------------------------------\n"));
        assertThat(result, endsWith("\nHELLO 123\n```"));
    }
}
