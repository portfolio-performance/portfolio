package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.model.ClientFactory;

/** A write keeps the file in the format it has, with the password it was loaded with. */
@RunWith(Parameterized.class)
public class WriteFormatsTest
{
    static final String PASSWORD = "s3cr3t-Pa55word";
    static final String ADD_ACME = "{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"ACME\"}]}";

    @FunctionalInterface
    interface Fixture
    {
        File create(Path folder) throws IOException;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> formats()
    {
        return Arrays.asList(new Object[][] { //
                        { "xml", (Fixture) ClientFiles::xml }, //
                        { "xml-id-references", (Fixture) ClientFiles::xmlWithIdReferences }, //
                        { "xml-zip", (Fixture) ClientFiles::xmlZip }, //
                        { "binary", (Fixture) ClientFiles::binary }, //
                        { "xml-aes256", (Fixture) f -> ClientFiles.xmlAes256(f, PASSWORD) }, //
                        { "xml-aes128", (Fixture) f -> ClientFiles.xmlAes128(f, PASSWORD) }, //
                        { "binary-aes256", (Fixture) f -> ClientFiles.binaryAes256(f, PASSWORD) } });
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final String format;
    private final Fixture fixture;

    public WriteFormatsTest(String format, Fixture fixture)
    {
        this.format = format;
        this.fixture = fixture;
    }

    @Test
    public void keepsTheFormatAndReopensWithThePassword() throws IOException
    {
        var clientFolder = folder.newFolder("client").toPath();
        var file = fixture.create(clientFolder);
        var original = Files.readAllBytes(file.toPath());
        var input = folder.newFile("batch.json");
        Files.writeString(input.toPath(), ADD_ACME);

        var run = CliRun.run(Map.of("PP_PASSWORD", PASSWORD), "apply", "--file", file.getPath(), "--input",
                        input.getPath());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(format(file), is(format));

        var reloaded = ClientFactory.load(file, PASSWORD.toCharArray(), new NullProgressMonitor());
        assertThat(reloaded.getSecurities().stream().map(s -> s.getName()).toList(), hasItem("ACME"));
        assertThat(reloaded.getSecurities().size(), is(3));

        var backup = Path.of(run.json().getAsJsonObject().getAsJsonObject("meta").get("backupPath").getAsString());
        assertThat(Files.readAllBytes(backup), is(original));
        assertThat(format(backup.toFile()), is(format));
    }

    private static String format(File file)
    {
        var info = CliRun.run(Map.of("PP_PASSWORD", PASSWORD), "info", "--file", file.getPath());
        assertThat(info.stdout, info.exitCode, is(0));
        return info.json().getAsJsonObject().getAsJsonObject("data").get("format").getAsString();
    }
}
