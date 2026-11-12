package name.abuchen.portfolio.cli.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.model.ClientFactory;

/** A write keeps the file in the format it has, with the password it was loaded with. */
@RunWith(Parameterized.class)
public class WriteFormatsTest
{
    static final String PASSWORD = "s3cr3t-Pa55word";
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

    private final CliFixture cli = new CliFixture(folder);

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
        var file = fixture.create(cli.clientFolder());
        var original = Files.readAllBytes(file.toPath());

        var run = cli.use(file).applyWith(Map.of("PP_PASSWORD", PASSWORD), List.of(), CliFixture.ADD_ACME);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(format(file), is(format));

        var reloaded = ClientFactory.load(file, PASSWORD.toCharArray(), new NullProgressMonitor());
        assertThat(reloaded.getSecurities().stream().map(s -> s.getName()).toList(), hasItem("ACME"));
        assertThat(reloaded.getSecurities().size(), is(3));

        var backup = Path.of(run.meta().get("backupPath").getAsString());
        assertThat(Files.readAllBytes(backup), is(original));
        assertThat(format(backup.toFile()), is(format));
    }

    private static String format(File file)
    {
        var info = CliRun.run(Map.of("PP_PASSWORD", PASSWORD), "info", "--file", file.getPath());
        assertThat(info.stdout, info.exitCode, is(0));
        return info.data().get("format").getAsString();
    }
}
