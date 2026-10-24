package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonObject;

public class HelpTest
{
    /**
     * What an agent harness shows without truncating; describe, at about 20 KB,
     * was cut. Most help stays near 6 KB; performance, with its two levels of
     * figures, is the largest.
     */
    private static final int SIZE_LIMIT = 9 * 1024;

    @Test
    public void globalHelpListsEveryCommandWithASummaryAndEveryErrorCode()
    {
        var run = CliRun.run("--help");

        assertThat(run.exitCode, is(0));
        assertThat(run.stderr, is(""));
        for (var command : Commands.ALL)
        {
            assertThat(run.stdout, containsString("  " + command.name() + " "));
            assertThat(run.stdout, containsString(start(command.summary())));
        }
        for (var error : ErrorSpec.ALL)
            assertThat(run.stdout, containsString(error.code()));
    }

    @Test
    public void globalHelpStatesTheGlobalOptionsTheLimitsAndTheExitCodes()
    {
        var stdout = CliRun.run("--help").stdout;

        assertThat(stdout, containsString("--file"));
        assertThat(stdout, containsString("--format"));
        assertThat(stdout, containsString("--verbose"));
        assertThat(stdout, containsString("--help"));
        for (var limit : HelpText.KNOWN_LIMITS)
            assertThat(stdout, containsString(limit));
        assertThat(stdout, containsString("PP_PASSWORD"));
        assertThat(stdout, containsString("Exit code"));
    }

    @Test
    public void bareInvocationShowsTheGlobalHelp()
    {
        var run = CliRun.run();

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, is(CliRun.run("--help").stdout));
    }

    @Test
    public void helpIsPlainTextNotJson()
    {
        assertThat(CliRun.run("--help").stdout, not(startsWith("{")));
        assertThat(CliRun.run("info", "--help").stdout, not(startsWith("{")));
    }

    @Test
    public void commandHelpMentionsEveryOptionAndEveryTopLevelDataField()
    {
        for (var command : Commands.ALL)
        {
            var run = CliRun.run(command.name(), "--help");

            assertThat(command.name(), run.exitCode, is(0));
            assertThat(command.name(), run.stdout, containsString("pp-cli " + command.name()));
            for (var option : command.options())
                assertThat(command.name() + " --" + option.name(), run.stdout, containsString("--" + option.name()));
            for (var field : dataFields(command))
                assertThat(command.name() + " data." + field, run.stdout, containsString(field));
            for (var warning : command.warnings())
                assertThat(command.name() + " " + warning.code(), run.stdout, containsString(warning.code()));
        }
    }

    @Test
    public void commandHelpStatesTypesDefaultsAllowedValuesAndRequiredOptions()
    {
        var instruments = CliRun.run("instruments", "--help").stdout;
        assertThat(instruments, containsString("--limit <"));
        assertThat(instruments, containsString("default 200"));
        assertThat(instruments, containsString("1 to 1000"));
        assertThat(instruments, containsString("json, table"));

        var prices = CliRun.run("prices", "--help").stdout;
        assertThat(prices, containsString("(required"));
        assertThat(prices, containsString("yyyy-MM-dd"));

        var holdings = CliRun.run("holdings", "--help").stdout;
        assertThat(holdings, containsString("--include-zero"));
        assertThat(holdings, containsString("switch"));
    }

    @Test
    public void commandHelpIndentsNestedDataFieldsAndDescribesThem()
    {
        var stdout = CliRun.run("info", "--help").stdout;

        assertThat(stdout, containsString("\n  counts (object)"));
        assertThat(stdout, containsString("\n    instruments (integer) Instruments."));
    }

    @Test
    public void commandHelpListsOnlyTheErrorCodesTheCommandCanReturn()
    {
        var info = CliRun.run("info", "--help").stdout;
        assertThat(info, containsString("file-not-found"));
        assertThat(info, containsString("unknown-option"));
        assertThat(info, not(containsString("period-required")));
        assertThat(info, not(containsString("file-locked")));
        assertThat(info, not(containsString("command-required")));

        var performance = CliRun.run("performance", "--help").stdout;
        assertThat(performance, containsString("period-required"));
        assertThat(performance, containsString("reference-not-found"));
        assertThat(performance, not(containsString("file-locked")));

        var apply = CliRun.run("apply", "--help").stdout;
        assertThat(apply, containsString("file-locked"));
        assertThat(apply, containsString("validation-failed"));
        assertThat(apply, not(containsString("period-required")));

        var restore = CliRun.run("restore", "--help").stdout;
        assertThat(restore, containsString("backup-not-found"));
    }

    @Test
    public void everyErrorCodeIsReachableThroughSomeHelp()
    {
        var all = new StringBuilder(CliRun.run("--help").stdout);
        for (var command : Commands.ALL)
            all.append(CliRun.run(command.name(), "--help").stdout);

        for (var error : ErrorSpec.ALL)
            assertThat(error.code(), all.toString(), containsString(error.code()));

        var commands = Commands.ALL.stream().map(CommandSpec::name).toList();
        for (var error : ErrorSpec.ALL)
            assertThat(error.code(), commands.containsAll(error.commands()), is(true));
    }

    @Test
    public void applyHelpListsTheOperationsWithOneLineSummaries()
    {
        var run = CliRun.run("apply", "--help");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, containsString("pp-cli apply --help <operation>"));
        for (var operation : Commands.OPERATIONS)
        {
            assertThat(run.stdout, containsString("  " + operation.name() + " "));
            assertThat(run.stdout, containsString(start(operation.summary())));
        }
    }

    @Test
    public void applyHelpForAnOperationShowsItsInputSchema()
    {
        var run = CliRun.run("apply", "--help", "instrument.add");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, containsString("instrument.add"));
        assertThat(run.stdout, containsString("\n  operation (string"));
        assertThat(run.stdout, containsString("\n  name (string"));
        assertThat(run.stdout, containsString("\n  isin (string or null, optional"));
        assertThat(run.stdout, not(containsString("--file")));
    }

    @Test
    public void everyOperationHasInputHelpMentioningAllItsFields()
    {
        for (var operation : Commands.OPERATIONS)
        {
            var run = CliRun.run("apply", "--help", operation.name());

            assertThat(operation.name(), run.exitCode, is(0));
            for (var field : fields(operation.input().toJson()))
                assertThat(operation.name() + "." + field, run.stdout, containsString(field));
        }
    }

    @Test
    public void unknownOperationAfterApplyHelpIsAUsageError()
    {
        var run = CliRun.run("apply", "--help", "instrument.explode");

        assertThat(run.exitCode, is(2));
        var problem = run.json().getAsJsonObject();
        assertThat(problem.get("code").getAsString(), is("unknown-operation"));
        assertThat(problem.get("detail").getAsString(), containsString("instrument.add"));
    }

    @Test
    public void helpWinsOverEveryOtherArgumentAndNeedsNoFile()
    {
        var run = CliRun.run("transactions", "--limit", "abc", "--bogus", "--file", "/does/not/exist.xml", "--help");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, containsString("pp-cli transactions"));

        var global = CliRun.run("--file", "/does/not/exist.xml", "--help");
        assertThat(global.exitCode, is(0));
        assertThat(global.stdout, is(CliRun.run("--help").stdout));

        var apply = CliRun.run("apply", "--input", "missing.json", "--help");
        assertThat(apply.exitCode, is(0));
        assertThat(apply.stdout, containsString("pp-cli apply --help <operation>"));
    }

    @Test
    public void helpForAnUnknownCommandIsAUsageError()
    {
        var run = CliRun.run("frobnicate", "--help");

        assertThat(run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("unknown-command"));
    }

    @Test
    public void describeIsGone()
    {
        var run = CliRun.run("describe");

        assertThat(run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("unknown-command"));
        assertThat(CliRun.run("--help").stdout, not(containsString("describe")));
    }

    @Test
    public void noHelpOutputExceedsTheSizeLimit()
    {
        var outputs = new ArrayList<String>();
        outputs.add(CliRun.run("--help").stdout);
        for (var command : Commands.ALL)
            outputs.add(CliRun.run(command.name(), "--help").stdout);
        for (var operation : Commands.OPERATIONS)
            outputs.add(CliRun.run("apply", "--help", operation.name()).stdout);

        for (var output : outputs)
            assertThat(output.lines().findFirst().orElse(""), output.getBytes(StandardCharsets.UTF_8).length,
                            lessThanOrEqualTo(SIZE_LIMIT));
    }

    /** The start of a summary, which every one-line listing keeps. */
    private static String start(String summary)
    {
        var sentence = summary.indexOf(". ");
        return summary.substring(0, Math.min(40, sentence < 0 ? summary.length() : sentence + 1));
    }

    private static List<String> dataFields(CommandSpec command)
    {
        return fields(command.dataSchema().toJson());
    }

    private static List<String> fields(java.util.Map<String, Object> schema)
    {
        var json = new com.google.gson.Gson().toJsonTree(schema).getAsJsonObject();
        var names = new ArrayList<String>();
        if (json.has("properties"))
            names.addAll(((JsonObject) json.get("properties")).keySet());
        return names;
    }
}
