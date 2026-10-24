package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Core logs status lines such as "Loaded ... with [XML]" to the console. The
 * CLI keeps them off stderr unless <code>--verbose</code> is given, so that a
 * harness merging stderr into stdout still sees one JSON document.
 */
public class QuietLoggingTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path file;

    @Before
    public void setUp() throws IOException
    {
        file = ClientFiles.xml(folder.getRoot().toPath()).toPath();
    }

    private static void assertSingleJsonDocument(CliRun run)
    {
        assertThat(run.stdout.endsWith("\n"), is(true));
        assertThat(run.stdout.trim().split("\n").length, is(1));
        run.json();
    }

    @Test
    public void infoWritesNothingToStderrByDefault()
    {
        var run = CliRun.run("info", "--file", file.toString());

        assertThat(run.exitCode, is(0));
        assertThat(run.stderr, is(""));
        assertSingleJsonDocument(run);
    }

    /**
     * Outside OSGi, core falls back to <code>System.err.println(status)</code>
     * for every log entry. The probe the apply command calls on macOS lets a
     * test do the same from inside a run.
     */
    private CliRun applyLoggingFromCore(String... more) throws IOException
    {
        var input = Files.writeString(folder.getRoot().toPath().resolve("batch.json"),
                        "{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"ACME\"}]}",
                        StandardCharsets.UTF_8);
        var logging = new WriteEnvironment()
        {
            @Override
            public boolean isOpenInApp(Path target)
            {
                System.err.println("Status INFO: name.abuchen.portfolio.model.ClientFactory code=0 Loaded x"); // NOSONAR
                System.err.println("Status OK: name.abuchen.portfolio code=0 fine"); // NOSONAR
                System.err.println("Status WARNING: name.abuchen.portfolio code=0 careful"); // NOSONAR
                System.err.println("Status ERROR: name.abuchen.portfolio code=0 broken"); // NOSONAR
                return false;
            }
        };

        var args = new ArrayList<>(List.of("apply", "--file", file.toString(), "--input", input.toString()));
        args.addAll(List.of(more));
        return CliRun.runWith(logging, "Mac OS X", args.toArray(String[]::new));
    }

    @Test
    public void coreInfoAndOkStatusLinesAreDroppedButWarningsAndErrorsStay() throws IOException
    {
        var run = applyLoggingFromCore();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.stderr.lines().toList(), is(List.of("Status WARNING: name.abuchen.portfolio code=0 careful",
                        "Status ERROR: name.abuchen.portfolio code=0 broken")));
        assertSingleJsonDocument(run);
    }

    @Test
    public void verboseKeepsEveryCoreStatusLine() throws IOException
    {
        var run = applyLoggingFromCore("--verbose");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.stderr, containsString("Status INFO: name.abuchen.portfolio.model.ClientFactory code=0 Loaded"));
        assertThat(run.stderr, containsString("Status OK: "));
        assertThat(run.stderr, containsString("Status WARNING: "));
        assertThat(run.stderr, containsString("Status ERROR: "));
        assertSingleJsonDocument(run);
    }

    @Test
    public void verboseRunOfInfoStillHoldsOnlyJsonOnStdout()
    {
        var run = CliRun.run("info", "--file", file.toString(), "--verbose");

        assertThat(run.exitCode, is(0));
        assertSingleJsonDocument(run);
    }

    @Test
    public void verboseDoesNotDisturbHelp()
    {
        var run = CliRun.run("info", "--verbose", "--help");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout.startsWith("pp-cli info"), is(true));
        assertThat(run.stderr, is(""));
    }

    @Test
    public void verboseIsASwitchWithoutValue()
    {
        var run = CliRun.run("info", "--file", file.toString(), "--verbose=true");

        assertThat(run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("invalid-option-value"));
    }

    @Test
    public void internalErrorStillWritesItsStackTraceToStderr() throws IOException
    {
        var input = Files.writeString(folder.getRoot().toPath().resolve("batch.json"),
                        "{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"ACME\"}]}",
                        StandardCharsets.UTF_8);
        var failingProbe = new WriteEnvironment()
        {
            @Override
            public boolean isOpenInApp(Path target)
            {
                throw new IllegalStateException("boom");
            }
        };

        var run = CliRun.runWith(failingProbe, "Mac OS X", "apply", "--file", file.toString(), "--input",
                        input.toString());

        assertThat(run.exitCode, is(1));
        assertThat(run.stderr, containsString("java.lang.IllegalStateException: boom"));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("internal-error"));
        assertSingleJsonDocument(run);
    }
}
