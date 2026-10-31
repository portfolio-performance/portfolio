package name.abuchen.portfolio.cli.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/** Older files are migrated and saved at core's current version. */
public class MigrationOnWriteTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private File historic(String resource, String target) throws IOException
    {
        var file = cli.clientFolder().resolve(target);
        try (var in = MigrationOnWriteTest.class.getResourceAsStream("historic/" + resource))
        {
            Files.write(file, in.readAllBytes());
        }
        return file.toFile();
    }

    private static List<String> names(Client client)
    {
        return client.getSecurities().stream().map(s -> s.getName()).toList();
    }

    @Test
    public void migratesAnOlderXmlFile() throws IOException
    {
        var file = cli.use(historic("client69.xml", "client.xml")).file();
        var original = Files.readAllBytes(file.toPath());

        var run = cli.apply(CliFixture.ADD_ACME);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("fileVersionBefore").getAsInt(), is(69));
        assertThat(run.meta().get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(run.warningCodes(), hasItem("file-migrated"));
        assertThat(Files.readAllBytes(Path.of(run.meta().get("backupPath").getAsString())), is(original));

        var reloaded = ClientFactory.load(file, null, new NullProgressMonitor());
        assertThat(reloaded.getFileVersionAfterRead(), is(Client.CURRENT_VERSION));
        assertThat(names(reloaded), hasItem("ACME"));
    }

    @Test
    public void migratesAnOlderBinaryFileAndKeepsItBinary() throws IOException
    {
        var file = cli.use(historic("client52.binary.portfolio", "client.portfolio")).file();

        var run = cli.apply(CliFixture.ADD_ACME);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("fileVersionBefore").getAsInt(), is(52));
        assertThat(run.meta().get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(run.warningCodes(), hasItem("file-migrated"));

        var info = cli.run("info");
        assertThat(info.data().get("format").getAsString(), is("binary"));
        assertThat(info.meta().get("fileVersion").getAsInt(), is(Client.CURRENT_VERSION));
    }

    @Test
    public void migratesAnOlderEncryptedFileWithTheLoadTimePassword() throws IOException
    {
        var file = cli.use(historic("client53.xml+pwd.portfolio", "client.portfolio")).file();

        var run = cli.applyWith(Map.of("PP_PASSWORD", "123456"), List.of(), CliFixture.ADD_ACME);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("fileVersionBefore").getAsInt(), is(53));
        assertThat(run.warningCodes(), hasItem("file-migrated"));

        var reloaded = ClientFactory.load(file, "123456".toCharArray(), new NullProgressMonitor());
        assertThat(reloaded.getFileVersionAfterRead(), is(Client.CURRENT_VERSION));
        assertThat(names(reloaded), hasItem("ACME"));
    }

    @Test
    public void aCurrentFileCarriesNoMigrationWarning() throws IOException
    {
        var run = cli.use(ClientFiles.xml(cli.clientFolder())).apply(CliFixture.ADD_ACME);

        assertThat(run.exitCode, is(0));
        assertThat(run.warningCodes(), not(hasItem("file-migrated")));
    }

    @Test
    public void aDryRunReportsThePendingMigrationAndWritesNothing() throws IOException
    {
        var file = cli.use(historic("client69.xml", "client.xml")).file();
        var before = Files.readAllBytes(file.toPath());
        var listingBefore = ClientFiles.listing(cli.clientFolder());

        var run = cli.applyWith(List.of("--dry-run"), CliFixture.ADD_ACME);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("dryRun").getAsBoolean(), is(true));
        assertThat(run.meta().get("fileVersionBefore").getAsInt(), is(69));
        assertThat(run.meta().get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(run.warningCodes(), hasItem("file-migrated"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
        assertThat(ClientFiles.listing(cli.clientFolder()), is(listingBefore));
    }
}
