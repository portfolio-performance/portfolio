package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.rest.internal.ApiException;

/**
 * The refusals a model reads: the prose says who has to act, the trailer says
 * the same as data - and the two must agree about whether trying again helps.
 */
@SuppressWarnings("nls")
public class McpExplainTest
{
    private static String explain(ApiException problem)
    {
        return McpExplain.explainProblem(problem, McpExplain.Context.of("pp_list_holdings"));
    }

    /** never started, so never executed: safe to send again, after the wait it asked for */
    @Test
    public void testUiBusyIsRetryable()
    {
        var text = explain(ApiException.uiBusy(10_000));

        assertThat(text, containsString("nothing was changed"));
        assertThat(text, containsString("retryable: yes · reasonCode: ui-busy · retryAfterSeconds: 5"));
    }

    /** already running and going to finish: sending it again would apply a write twice */
    @Test
    public void testUiRequestUnderwayIsNotRetryable()
    {
        var text = explain(ApiException.uiRequestUnderway(10_000));

        assertThat(text, containsString("do not send the same call again"));
        assertThat(text, containsString("retryable: no · reasonCode: ui-request-underway"));
        assertThat(text, not(containsString("retryAfterSeconds")));
    }

    /** the same question takes just as long again, so only a smaller one helps */
    @Test
    public void testCalculationTimeoutAsksForASmallerQuestion()
    {
        var text = explain(ApiException.calculationTimeout(120));

        assertThat(text, containsString("within 120 seconds"));
        assertThat(text, containsString("shorter interval"));
        assertThat(text, containsString("retryable: no · reasonCode: calculation-timeout"));
    }

    @Test
    public void testServerBusyIsRetryable()
    {
        var text = explain(ApiException.serverBusy());

        assertThat(text, containsString("too many requests at once"));
        assertThat(text, containsString("retryable: yes · reasonCode: server-busy · retryAfterSeconds: 1"));
    }

    /** one address per file, as the tool descriptions say - never "this or that" */
    @Test
    public void testTheFileListOffersOnlyTheUseValue()
    {
        var text = McpExplain.describeFiles(List.of(new McpExplain.FileSummary("9a2d7c30", "depot.xml", "main", "main"),
                        new McpExplain.FileSummary("5f3c1e2a", "other.xml", null, "5f3c1e2a")));

        assertThat(text, containsString("- depot.xml — use `main`"));
        assertThat(text, containsString("- other.xml — use `5f3c1e2a`"));
        assertThat(text, not(containsString("9a2d7c30")));
    }
}
