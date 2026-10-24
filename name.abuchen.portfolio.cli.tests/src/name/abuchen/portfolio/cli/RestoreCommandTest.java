package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;

/**
 * <code>restore --backup</code> rolls a client file back through the same
 * safe-write sequence as <code>apply</code>.
 */
public class RestoreCommandTest
{
    private static final String APPLY_BACKUP = "client.20261016-120000.pp-cli.xml";
    private static final String RESTORE_BACKUP = "client.20261016-120000-2.pp-cli.xml";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path clientFolder;
    private File file;
    private Set<String> originalSecurities;

    @Before
    public void setUp() throws IOException
    {
        clientFolder = folder.newFolder("client").toPath();
        file = ClientFiles.xml(clientFolder);
        originalSecurities = securities(file, null);
    }

    private static Set<String> securities(File file, char[] password) throws IOException
    {
        Client client = ClientFactory.load(file, password, new NullProgressMonitor());
        return client.getSecurities().stream().map(Security::getUUID).collect(Collectors.toSet());
    }

    private CliRun apply(File target) throws IOException
    {
        var input = Files.createTempFile(folder.getRoot().toPath(), "batch", ".json");
        Files.writeString(input, "{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"ACME\"}]}",
                        StandardCharsets.UTF_8);
        var run = CliRun.run("apply", "--file", target.getPath(), "--input", input.toString());
        assertThat(run.stdout, run.exitCode, is(0));
        return run;
    }

    private CliRun restore(String... more)
    {
        return restore(file, more);
    }

    private static CliRun restore(File target, String... more)
    {
        var args = new ArrayList<>(List.of("restore", "--file", target.getPath()));
        args.addAll(List.of(more));
        return CliRun.run(args.toArray(String[]::new));
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    private static String code(CliRun run)
    {
        return run.json().getAsJsonObject().get("code").getAsString();
    }

    private static List<String> listing(Path root) throws IOException
    {
        try (Stream<Path> paths = Files.walk(root))
        {
            return paths.map(p -> root.relativize(p).toString().replace('\\', '/')).sorted().toList();
        }
    }

    @Test
    public void bringsTheClientFileBackToItsStateBeforeApply() throws IOException
    {
        var applied = apply(file);
        assertThat(securities(file, null).size(), is(originalSecurities.size() + 1));

        var run = restore("--backup", meta(applied).get("backupPath").getAsString());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(securities(file, null), is(originalSecurities));
        assertThat(meta(run).get("dryRun").getAsBoolean(), is(false));
    }

    @Test
    public void backsUpTheFileItReplacesAndReportsThatBackup() throws IOException
    {
        var applied = apply(file);
        var afterApply = securities(file, null);

        var run = restore("--backup", meta(applied).get("backupPath").getAsString());

        assertThat(run.stdout, run.exitCode, is(0));
        var backup = clientFolder.resolve("backups").resolve(RESTORE_BACKUP);
        assertThat(meta(run).get("backupPath").getAsString(), is(backup.toAbsolutePath().toString()));
        assertThat(securities(backup.toFile(), null), is(afterApply));
    }

    @Test
    public void aRestoreCanItselfBeUndone() throws IOException
    {
        var applied = apply(file);
        var afterApply = securities(file, null);
        var restored = restore("--backup", meta(applied).get("backupPath").getAsString());

        var undone = restore("--backup", meta(restored).get("backupPath").getAsString());

        assertThat(undone.stdout, undone.exitCode, is(0));
        assertThat(securities(file, null), is(afterApply));
    }

    @Test
    public void aMissingBackupIsRefusedWithoutSideEffects() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());

        var run = restore("--backup", clientFolder.resolve("backups").resolve("nope.xml").toString());

        assertThat(run.exitCode, is(1));
        assertThat(code(run), is("backup-not-found"));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aBackupNewerThanSupportedIsRefusedWithoutSideEffects() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var newer = ClientFiles.newerXml(folder.newFolder("newer").toPath());

        var run = restore("--backup", newer.getPath());

        assertThat(run.exitCode, is(1));
        assertThat(code(run), is("file-version-unsupported"));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aHeldLockAbortsTheRestore() throws IOException
    {
        var applied = apply(file);
        var afterApply = Files.readAllBytes(file.toPath());
        var lock = Files.writeString(clientFolder.resolve(".client.xml.pp-cli.lock"), "4711");

        var run = restore("--backup", meta(applied).get("backupPath").getAsString());

        assertThat(run.exitCode, is(1));
        assertThat(code(run), is("file-locked"));
        assertThat(Files.readAllBytes(file.toPath()), is(afterApply));
        assertThat(Files.exists(lock), is(true));
        assertThat(listing(clientFolder), is(List.of("", ".client.xml.pp-cli.lock", "backups",
                        "backups/" + APPLY_BACKUP, "client.xml")));
    }

    @Test
    public void aFileChangedBeforeTheRenameAbortsTheRestore() throws IOException
    {
        var applied = apply(file);
        var environment = new WriteEnvironment()
        {
            @Override
            public void beforeRecheck(Path target)
            {
                try
                {
                    Files.writeString(target, Files.readString(target) + "\n<!-- saved by the app -->");
                }
                catch (IOException e)
                {
                    throw new AssertionError(e);
                }
            }
        };

        var run = CliRun.runWith(environment, "Linux", "restore", "--file", file.getPath(), "--backup",
                        meta(applied).get("backupPath").getAsString());

        assertThat(run.exitCode, is(1));
        assertThat(code(run), is("file-changed"));
        assertThat(Files.readString(file.toPath()).endsWith("<!-- saved by the app -->"), is(true));
        assertThat(listing(clientFolder), is(List.of("", "backups", "backups/" + APPLY_BACKUP, "client.xml")));
    }

    @Test
    public void aDryRunReportsTheWouldBeBackupAndCreatesNothing() throws IOException
    {
        var applied = apply(file);
        var afterApply = Files.readAllBytes(file.toPath());
        var before = listing(clientFolder);

        var run = restore("--backup", meta(applied).get("backupPath").getAsString(), "--dry-run");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(meta(run).get("dryRun").getAsBoolean(), is(true));
        assertThat(meta(run).get("backupPath").getAsString(),
                        is(clientFolder.resolve("backups").resolve(RESTORE_BACKUP).toAbsolutePath().toString()));
        assertThat(Files.readAllBytes(file.toPath()), is(afterApply));
        assertThat(listing(clientFolder), is(before));
    }

    @Test
    public void restoresAnEncryptedFileWithTheSamePassword() throws IOException
    {
        var password = "secret";
        var encrypted = ClientFiles.xmlAes256(folder.newFolder("encrypted").toPath(), password);
        var original = securities(encrypted, password.toCharArray());
        var input = Files.createTempFile(folder.getRoot().toPath(), "batch", ".json");
        Files.writeString(input, "{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"ACME\"}]}");
        var applied = CliRun.run(Map.of(CommandContext.PASSWORD_VARIABLE, password), "apply", "--file",
                        encrypted.getPath(), "--input", input.toString());
        assertThat(applied.stdout, applied.exitCode, is(0));

        var run = CliRun.run(Map.of(CommandContext.PASSWORD_VARIABLE, password), "restore", "--file",
                        encrypted.getPath(), "--backup", meta(applied).get("backupPath").getAsString());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(securities(encrypted, password.toCharArray()), is(original));
    }
}
