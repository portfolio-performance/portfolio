package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

public class EncryptedFilesTest
{
    private static final String PASSWORD = "s3cr3t-Pa55word";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path dir()
    {
        return folder.getRoot().toPath();
    }

    private static JsonObject json(CliRun run)
    {
        return run.json().getAsJsonObject();
    }

    private static CliRun infoWithEnvPassword(File file, String password)
    {
        return CliRun.run(Map.of("PP_PASSWORD", password), "info", "--file", file.getPath());
    }

    private static void assertFormat(CliRun run, String format)
    {
        assertThat(run.stdout, run.exitCode, is(0));

        var data = json(run).getAsJsonObject("data");
        assertThat(data.get("format").getAsString(), is(format));
        assertThat(data.getAsJsonObject("counts").get("instruments").getAsInt(), is(2));
        assertThat(data.getAsJsonObject("counts").get("transactions").getAsInt(), is(2));
    }

    private static void assertPasswordHidden(CliRun run)
    {
        assertThat(run.stdout, not(containsString(PASSWORD)));
        assertThat(run.stderr, not(containsString(PASSWORD)));
    }

    @Test
    public void readsXmlAes256WithEnvironmentPassword() throws IOException
    {
        var run = infoWithEnvPassword(ClientFiles.xmlAes256(dir(), PASSWORD), PASSWORD);

        assertFormat(run, "xml-aes256");
        assertPasswordHidden(run);
    }

    @Test
    public void readsXmlAes128WithEnvironmentPassword() throws IOException
    {
        var run = infoWithEnvPassword(ClientFiles.xmlAes128(dir(), PASSWORD), PASSWORD);

        assertFormat(run, "xml-aes128");
        assertPasswordHidden(run);
    }

    @Test
    public void readsBinaryAes256WithEnvironmentPassword() throws IOException
    {
        var run = infoWithEnvPassword(ClientFiles.binaryAes256(dir(), PASSWORD), PASSWORD);

        assertFormat(run, "binary-aes256");
        assertPasswordHidden(run);
    }

    @Test
    public void readsPasswordFromStdin() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of(), PASSWORD + "\n", "info", "--file", file.getPath());

        assertFormat(run, "xml-aes256");
        assertPasswordHidden(run);
    }

    // stdin holds the password once, so a second read would find it empty and fail the command
    @Test
    public void aWriteReadsThePasswordFromStdinOnlyOnce() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of(), PASSWORD + "\n", "apply", "--file", file.getPath(), "--input",
                        addAcme().getPath());

        assertThat(run.stdout, run.exitCode, is(0));
        var reloaded = ClientFactory.load(file, PASSWORD.toCharArray(), new NullProgressMonitor());
        assertThat(reloaded.getSecurities().stream().map(s -> s.getName()).toList(), hasItem("ACME"));
    }

    @Test
    public void aDryRunReadsThePasswordFromStdinOnlyOnce() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);
        var before = Files.readAllBytes(file.toPath());

        var run = CliRun.runWithStdin(Map.of(), PASSWORD + "\n", "apply", "--file", file.getPath(), "--input",
                        addAcme().getPath(), "--dry-run");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
    }

    private File addAcme() throws IOException
    {
        var input = folder.newFile();
        Files.writeString(input.toPath(), WriteFormatsTest.ADD_ACME);
        return input;
    }

    @Test
    public void stdinPasswordMayEndWithCarriageReturn() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of(), PASSWORD + "\r\nignored\n", "info", "--file", file.getPath());

        assertFormat(run, "xml-aes256");
    }

    @Test
    public void stdinPasswordWithoutLineBreakIsAccepted() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of(), PASSWORD, "info", "--file", file.getPath());

        assertFormat(run, "xml-aes256");
    }

    @Test
    public void environmentPasswordWinsOverStdin() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of("PP_PASSWORD", PASSWORD), "wrong\n", "info", "--file", file.getPath());

        assertFormat(run, "xml-aes256");
    }

    @Test
    public void emptyEnvironmentPasswordFallsBackToStdin() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of("PP_PASSWORD", ""), PASSWORD + "\n", "info", "--file", file.getPath());

        assertFormat(run, "xml-aes256");
    }

    @Test
    public void unencryptedFilesNeverReadStdin() throws IOException
    {
        var run = CliRun.runWithStdin(Map.of(), "ignored\n", "info", "--file", ClientFiles.xml(dir()).getPath());

        assertThat(run.exitCode, is(0));
    }

    @Test
    public void missingPasswordIsReported() throws IOException
    {
        var run = CliRun.run("info", "--file", ClientFiles.xmlAes256(dir(), PASSWORD).getPath());

        assertThat(run.exitCode, is(1));
        assertThat(json(run).has("data"), is(false));
        assertThat(json(run).get("code").getAsString(), is("password-required"));
        assertPasswordHidden(run);
    }

    @Test
    public void emptyStdinLineIsAMissingPassword() throws IOException
    {
        var run = CliRun.runWithStdin(Map.of(), "\n", "info", "--file",
                        ClientFiles.xmlAes256(dir(), PASSWORD).getPath());

        assertThat(run.exitCode, is(1));
        assertThat(json(run).get("code").getAsString(), is("password-required"));
    }

    @Test
    public void wrongPasswordIsReported() throws IOException
    {
        var wrong = "not-the-" + PASSWORD.substring(4);

        for (var file : new File[] { ClientFiles.xmlAes256(dir(), PASSWORD), ClientFiles.xmlAes128(dir(), PASSWORD),
                        ClientFiles.binaryAes256(dir(), PASSWORD) })
        {
            var run = infoWithEnvPassword(file, wrong);

            assertThat(run.exitCode, is(1));
            assertThat(json(run).has("data"), is(false));
            assertThat(json(run).get("code").getAsString(), is("incorrect-password"));
            assertThat(run.stdout, not(containsString(wrong)));
            assertThat(run.stderr, not(containsString(wrong)));
        }
    }

    @Test
    public void wrongStdinPasswordIsReportedWithoutEchoingIt() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);

        var run = CliRun.runWithStdin(Map.of(), "hunter2-wrong\n", "info", "--file", file.getPath());

        assertThat(run.exitCode, is(1));
        assertThat(json(run).get("code").getAsString(), is("incorrect-password"));
        assertThat(run.stdout, not(containsString("hunter2-wrong")));
        assertThat(run.stderr, not(containsString("hunter2-wrong")));
    }

    @Test
    public void newerEncryptedFileIsRefused() throws IOException
    {
        var run = infoWithEnvPassword(ClientFiles.newerEncrypted(dir(), PASSWORD), PASSWORD);

        assertThat(run.exitCode, is(1));

        var problem = json(run);
        assertThat(problem.get("code").getAsString(), is("file-version-unsupported"));
        assertThat(problem.get("fileVersion").getAsInt(), is(ClientFiles.NEWER_ENCRYPTED_VERSION));
        assertThat(problem.get("maxSupportedVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertPasswordHidden(run);
    }

    @Test
    public void newerEncryptedFileWithWrongPasswordIsAWrongPassword() throws IOException
    {
        var run = infoWithEnvPassword(ClientFiles.newerEncrypted(dir(), PASSWORD), "wrong");

        assertThat(json(run).get("code").getAsString(), is("incorrect-password"));
    }

    @Test
    public void truncatedEncryptedFileIsUnreadable() throws IOException
    {
        var file = ClientFiles.xmlAes256(dir(), PASSWORD);
        Files.write(file.toPath(), Arrays.copyOf(Files.readAllBytes(file.toPath()), 20));

        var run = infoWithEnvPassword(file, PASSWORD);

        assertThat(run.exitCode, is(1));
        assertThat(json(run).get("code").getAsString(), is("file-unreadable"));
        assertPasswordHidden(run);
    }
}
