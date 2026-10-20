package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/** Older files are migrated and saved at core's current version. */
public class MigrationOnWriteTest
{
    private static final String PASSWORD = "s3cr3t-Pa55word";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path clientFolder;
    private File input;

    @Before
    public void setUp() throws IOException
    {
        clientFolder = folder.newFolder("client").toPath();
        input = folder.newFile("batch.json");
        Files.writeString(input.toPath(), WriteFormatsTest.ADD_ACME);
    }

    private File historic(String resource, String target) throws IOException
    {
        var file = clientFolder.resolve(target);
        try (var in = MigrationOnWriteTest.class.getResourceAsStream("historic/" + resource))
        {
            Files.write(file, in.readAllBytes());
        }
        return file.toFile();
    }

    private CliRun apply(File file, String... more)
    {
        return applyWith(Map.of(), file, more);
    }

    private CliRun applyWith(Map<String, String> env, File file, String... more)
    {
        var args = new ArrayList<>(List.of("apply", "--file", file.getPath(), "--input", input.getPath()));
        args.addAll(List.of(more));
        return CliRun.run(env, args.toArray(String[]::new));
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    private static List<String> warningCodes(CliRun run)
    {
        var codes = new ArrayList<String>();
        var warnings = run.json().getAsJsonObject().getAsJsonArray("warnings");
        if (warnings != null)
            warnings.forEach(w -> codes.add(w.getAsJsonObject().get("code").getAsString()));
        return codes;
    }

    private static List<String> names(Client client)
    {
        return client.getSecurities().stream().map(s -> s.getName()).toList();
    }

    private List<String> listing() throws IOException
    {
        try (Stream<Path> paths = Files.walk(clientFolder))
        {
            return paths.map(p -> clientFolder.relativize(p).toString().replace('\\', '/')).sorted().toList();
        }
    }

    @Test
    public void passwordOnStdinIsReadOnceForAWrite() throws IOException
    {
        var file = ClientFiles.xmlAes256(clientFolder, PASSWORD);

        var run = CliRun.runWithStdin(Map.of(), PASSWORD + "\n", "apply", "--file", file.getPath(), "--input",
                        input.getPath());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(names(ClientFactory.load(file, PASSWORD.toCharArray(), new NullProgressMonitor())),
                        hasItem("ACME"));
    }

    @Test
    public void migratesAnOlderXmlFile() throws IOException
    {
        var file = historic("client69.xml", "client.xml");
        var original = Files.readAllBytes(file.toPath());

        var run = apply(file);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).get("fileVersionBefore").getAsInt(), is(69));
        assertThat(meta(run).get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(warningCodes(run), hasItem("file-migrated"));
        assertThat(Files.readAllBytes(Path.of(meta(run).get("backupPath").getAsString())), is(original));

        var reloaded = ClientFactory.load(file, null, new NullProgressMonitor());
        assertThat(reloaded.getFileVersionAfterRead(), is(Client.CURRENT_VERSION));
        assertThat(names(reloaded), hasItem("ACME"));
    }

    @Test
    public void migratesAnOlderBinaryFileAndKeepsItBinary() throws IOException
    {
        var file = historic("client52.binary.portfolio", "client.portfolio");

        var run = apply(file);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).get("fileVersionBefore").getAsInt(), is(52));
        assertThat(meta(run).get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(warningCodes(run), hasItem("file-migrated"));

        var info = CliRun.run("info", "--file", file.getPath());
        assertThat(info.json().getAsJsonObject().getAsJsonObject("data").get("format").getAsString(), is("binary"));
        assertThat(meta(info).get("fileVersion").getAsInt(), is(Client.CURRENT_VERSION));
    }

    @Test
    public void migratesAnOlderEncryptedFileWithTheLoadTimePassword() throws IOException
    {
        var file = historic("client53.xml+pwd.portfolio", "client.portfolio");

        var run = applyWith(Map.of("PP_PASSWORD", "123456"), file);

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).get("fileVersionBefore").getAsInt(), is(53));
        assertThat(warningCodes(run), hasItem("file-migrated"));

        var reloaded = ClientFactory.load(file, "123456".toCharArray(), new NullProgressMonitor());
        assertThat(reloaded.getFileVersionAfterRead(), is(Client.CURRENT_VERSION));
        assertThat(names(reloaded), hasItem("ACME"));
    }

    @Test
    public void aCurrentFileCarriesNoMigrationWarning() throws IOException
    {
        var run = apply(ClientFiles.xml(clientFolder));

        assertThat(run.exitCode, is(0));
        assertThat(warningCodes(run), not(hasItem("file-migrated")));
    }

    @Test
    public void aDryRunReportsThePendingMigrationAndWritesNothing() throws IOException
    {
        var file = historic("client69.xml", "client.xml");
        var before = Files.readAllBytes(file.toPath());
        var listingBefore = listing();

        var run = apply(file, "--dry-run");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).get("dryRun").getAsBoolean(), is(true));
        assertThat(meta(run).get("fileVersionBefore").getAsInt(), is(69));
        assertThat(meta(run).get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(warningCodes(run), hasItem("file-migrated"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
        assertThat(listing(), is(listingBefore));
    }
}
