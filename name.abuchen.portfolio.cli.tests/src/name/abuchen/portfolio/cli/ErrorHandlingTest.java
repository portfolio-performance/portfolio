package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;

public class ErrorHandlingTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path dir()
    {
        return folder.getRoot().toPath();
    }

    private static JsonObject problem(CliRun run)
    {
        var json = run.json().getAsJsonObject();
        assertThat(json.has("data"), is(false));
        return json;
    }

    @Test
    public void missingFileOptionIsAUsageError()
    {
        var run = CliRun.run("info");

        assertThat(run.exitCode, is(2));
        assertThat(problem(run).get("code").getAsString(), is("file-required"));
    }

    @Test
    public void missingFileIsReported()
    {
        var run = CliRun.run("info", "--file", dir().resolve("missing.xml").toString());

        assertThat(run.exitCode, is(1));
        assertThat(problem(run).get("code").getAsString(), is("file-not-found"));
    }

    @Test
    public void unreadableFileIsReported() throws IOException
    {
        var file = Files.writeString(dir().resolve("broken.xml"), "<client><nonsense/>");

        var run = CliRun.run("info", "--file", file.toString());

        assertThat(run.exitCode, is(1));
        assertThat(problem(run).get("code").getAsString(), is("file-unreadable"));
    }

    @Test
    public void unknownCommandIsAUsageError()
    {
        var run = CliRun.run("frobnicate");

        assertThat(run.exitCode, is(2));
        assertThat(problem(run).get("code").getAsString(), is("unknown-command"));
    }

    @Test
    public void unknownOptionIsAUsageError() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.xml(dir()).getPath(), "--bogus", "1");

        assertThat(run.exitCode, is(2));
        assertThat(problem(run).get("code").getAsString(), is("unknown-option"));
    }

    @Test
    public void optionWithoutValueIsAUsageError()
    {
        var run = CliRun.run("info", "--file");

        assertThat(run.exitCode, is(2));
        assertThat(problem(run).get("code").getAsString(), is("option-value-required"));
    }

    @Test
    public void passwordIsNotAnOption() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.xml(dir()).getPath(), "--password", "secret");

        assertThat(run.exitCode, is(2));
        assertThat(problem(run).get("code").getAsString(), is("unknown-option"));
        assertThat(run.stdout.contains("secret"), is(false));
        assertThat(run.stderr.contains("secret"), is(false));
    }

    @Test
    public void errorsCarryProblemJsonFields()
    {
        var run = CliRun.run("frobnicate");

        var problem = problem(run);
        assertThat(problem.get("title").getAsString().isEmpty(), is(false));
        assertThat(problem.get("detail").getAsString().isEmpty(), is(false));
        assertThat(problem.get("status").getAsInt(), is(400));
    }

    @Test
    public void newerXmlIsRefused() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.newerXml(dir()).getPath());

        assertVersionRefusal(run);
    }

    @Test
    public void newerBinaryIsRefused() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.newerBinary(dir()).getPath());

        assertVersionRefusal(run);
    }

    private static void assertVersionRefusal(CliRun run)
    {
        assertThat(run.exitCode, is(1));

        var problem = problem(run);
        assertThat(problem.get("code").getAsString(), is("file-version-unsupported"));
        assertThat(problem.get("fileVersion").getAsInt(), is(ClientFiles.NEWER_VERSION));
        assertThat(problem.get("maxSupportedVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(problem.get("fileVersion").getAsInt(), is(greaterThan(Client.CURRENT_VERSION)));
    }

    @Test
    public void errorsLeaveStdoutAsSingleJsonDocument()
    {
        var run = CliRun.run(Map.of(), "info", "--file", "nope.xml");

        assertThat(run.stdout.trim().split("\n").length, is(1));
        assertThat(run.exitCode, is(not(0)));
    }
}
