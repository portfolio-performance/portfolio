package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

/** Guards the guard that every successful CLI run in the suite passes through. */
public class ResponseContractTest
{
    private static final String INFO_DATA = """
                    {"fileVersion":68,"maxSupportedVersion":68,"format":"xml","baseCurrency":"EUR",
                     "counts":{"instruments":0,"investmentAccounts":0,"cashAccounts":0,"transactions":0,"taxonomies":0},
                     "pinnedUpstreamTag":"0.89.1"}""";

    private static final String META = "{\"fileVersion\":68,\"asOf\":\"2026-10-16\"}";

    private static List<String> violations(String stdout, String... args)
    {
        return ResponseContract.violations(List.of(args), 0, stdout);
    }

    private static String envelope(String data, String warnings)
    {
        return "{\"data\":" + data + ",\"meta\":" + META + ",\"warnings\":" + warnings + "}";
    }

    @Test
    public void acceptsAResponseThatMatchesTheCommandsDeclaration()
    {
        assertThat(violations(envelope(INFO_DATA, "[]"), "info", "--file", "x.xml"), is(empty()));
    }

    @Test
    public void reportsADataMemberTheCommandDoesNotDeclare()
    {
        var data = INFO_DATA.replace("\"format\":\"xml\"", "\"format\":\"xml\",\"surprise\":1");

        assertThat(violations(envelope(data, "[]"), "info"),
                        contains(containsString("undeclared property 'surprise'")));
    }

    @Test
    public void reportsAWarningCodeTheCommandDoesNotDeclare()
    {
        var stdout = envelope(INFO_DATA, "[{\"code\":\"made-up\",\"detail\":\"x\"}]");

        assertThat(violations(stdout, "info"), contains(containsString("made-up")));
    }

    @Test
    public void reportsAnEnvelopeWithMissingOrExtraMembers()
    {
        assertThat(violations("{\"data\":" + INFO_DATA + ",\"meta\":" + META + "}", "info"),
                        contains(containsString("envelope")));
    }

    @Test
    public void reportsMalformedWarnings()
    {
        assertThat(violations(envelope(INFO_DATA, "{}"), "info"), contains(containsString("warnings")));
        assertThat(violations(envelope(INFO_DATA, "[\"stale\"]"), "info"), contains(containsString("warning")));
        assertThat(violations(envelope(INFO_DATA, "[{\"code\":\"x\"}]"), "info"), contains(containsString("detail")));
    }

    @Test
    public void reportsAMetaMemberTheCommandDoesNotDeclare()
    {
        var stdout = envelope(INFO_DATA, "[]").replace("\"asOf\"", "\"extra\":true,\"asOf\"");

        assertThat(violations(stdout, "info"), contains(containsString("meta $: undeclared property 'extra'")));
    }

    @Test
    public void reportsOutputThatIsNotJson()
    {
        assertThat(violations("data: none", "info"), contains(containsString("not a JSON")));
    }

    @Test
    public void acceptsAFailureWithACodeTheCommandsHelpLists()
    {
        assertThat(ResponseContract.violations(List.of("info"), 1, "{\"code\":\"file-required\"}"), is(empty()));
        assertThat(ResponseContract.violations(List.of("info"), 2, "{\"code\":\"unknown-option\"}"), is(empty()));
    }

    @Test
    public void reportsAFailureWithACodeTheCommandsHelpDoesNotList()
    {
        assertThat(ResponseContract.violations(List.of("info"), 2, "{\"code\":\"period-required\"}"),
                        contains(containsString("period-required")));
    }

    @Test
    public void leavesHelpAndTablesAlone()
    {
        assertThat(violations("Usage: pp-cli info", "info", "--help"), is(empty()));
        assertThat(violations("Usage: pp-cli", new String[0]), is(empty()));
        assertThat(violations("NAME  ISIN", "instruments", "--format", "table"), is(empty()));
        assertThat(violations("NAME  ISIN", "instruments", "--format=table"), is(empty()));
    }
}
