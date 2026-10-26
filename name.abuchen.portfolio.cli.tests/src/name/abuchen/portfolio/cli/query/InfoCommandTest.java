package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.model.Client;

public class InfoCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path dir()
    {
        return folder.getRoot().toPath();
    }

    @Test
    public void infoReturnsEnvelopeWithFileFacts() throws IOException
    {
        var file = ClientFiles.xml(dir());

        var run = CliRun.run("info", "--file", file.getPath());

        assertThat(run.exitCode, is(0));

        var envelope = run.json().getAsJsonObject();
        assertThat(envelope.keySet(), is(Set.of("data", "meta", "warnings")));
        assertThat(envelope.getAsJsonArray("warnings").size(), is(0));

        var data = envelope.getAsJsonObject("data");
        assertThat(data.get("fileVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(data.get("maxSupportedVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(data.get("format").getAsString(), is("xml"));
        assertThat(data.get("baseCurrency").getAsString(), is("EUR"));
        assertThat(data.get("pinnedUpstreamTag").getAsString(), is("0.89.0"));

        var counts = data.getAsJsonObject("counts");
        assertThat(counts.get("instruments").getAsInt(), is(2));
        assertThat(counts.get("investmentAccounts").getAsInt(), is(1));
        assertThat(counts.get("cashAccounts").getAsInt(), is(1));
        assertThat(counts.get("transactions").getAsInt(), is(2));

        var meta = envelope.getAsJsonObject("meta");
        assertThat(meta.get("fileVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("asOf").getAsString(), is("2026-10-16"));
    }

    @Test
    public void stdoutHoldsOnlyJson() throws IOException
    {
        var file = ClientFiles.xml(dir());

        var run = CliRun.run("info", "--file", file.getPath());

        assertThat(run.stdout.endsWith("\n"), is(true));
        assertThat(run.stdout.trim().split("\n").length, is(1));
        // core logs "Loaded ..." for every file; it must not reach stdout
        assertThat(run.stdout.contains("Loaded"), is(false));
    }

    @Test
    public void ppFileIsUsedWhenFileOptionIsAbsent() throws IOException
    {
        var file = ClientFiles.xml(dir());

        var run = CliRun.run(Map.of("PP_FILE", file.getPath()), "info");

        assertThat(run.exitCode, is(0));
        assertThat(run.data().get("format").getAsString(), is("xml"));
    }

    @Test
    public void fileOptionWinsOverPpFile() throws IOException
    {
        var file = ClientFiles.xml(dir());

        var run = CliRun.run(Map.of("PP_FILE", dir().resolve("missing.xml").toString()), "info", "--file",
                        file.getPath());

        assertThat(run.exitCode, is(0));
    }

    @Test
    public void blankFileOptionFallsBackToPpFile() throws IOException
    {
        var file = ClientFiles.xml(dir());

        var run = CliRun.run(Map.of("PP_FILE", file.getPath()), "info", "--file", "");

        assertThat(run.exitCode, is(0));
    }

    @Test
    public void detectsXmlWithIdReferences() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.xmlWithIdReferences(dir()).getPath());

        assertThat(run.exitCode, is(0));
        assertThat(run.data().get("format").getAsString(), is("xml-id-references"));
    }

    @Test
    public void detectsXmlZip() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.xmlZip(dir()).getPath());

        assertThat(run.exitCode, is(0));
        assertThat(run.data().get("format").getAsString(), is("xml-zip"));
    }

    @Test
    public void detectsBinary() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.binary(dir()).getPath());

        assertThat(run.exitCode, is(0));
        assertThat(run.data().get("format").getAsString(), is("binary"));
        assertThat(run.data().getAsJsonObject("counts").get("instruments").getAsInt(), is(2));
    }
}
