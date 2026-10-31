package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.file.WriteEnvironment;
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

    private final CliFixture cli = new CliFixture(folder);
    private Path clientFolder;
    private File file;
    private Set<String> originalSecurities;

    @Before
    public void setUp() throws IOException
    {
        clientFolder = cli.clientFolder();
        file = cli.use(ClientFiles.xml(clientFolder)).file();
        originalSecurities = securities(file, null);
    }

    private static Set<String> securities(File file, char[] password) throws IOException
    {
        Client client = ClientFactory.load(file, password, new NullProgressMonitor());
        return client.getSecurities().stream().map(Security::getUUID).collect(Collectors.toSet());
    }

    /** Adds an instrument, so that there is a backup to restore. */
    private CliRun apply() throws IOException
    {
        var run = cli.apply(CliFixture.ADD_ACME);
        assertThat(run.stdout, run.exitCode, is(0));
        return run;
    }

    @Test
    public void bringsTheClientFileBackToItsStateBeforeApply() throws IOException
    {
        var applied = apply();
        assertThat(securities(file, null).size(), is(originalSecurities.size() + 1));

        var run = cli.run("restore", "--backup", applied.meta().get("backupPath").getAsString());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(securities(file, null), is(originalSecurities));
    }

    @Test
    public void backsUpTheFileItReplacesAndReportsThatBackup() throws IOException
    {
        var applied = apply();
        var afterApply = securities(file, null);

        var run = cli.run("restore", "--backup", applied.meta().get("backupPath").getAsString());

        assertThat(run.stdout, run.exitCode, is(0));
        var backup = clientFolder.resolve("backups").resolve(RESTORE_BACKUP);
        assertThat(run.meta().get("backupPath").getAsString(), is(backup.toAbsolutePath().toString()));
        assertThat(securities(backup.toFile(), null), is(afterApply));
    }

    @Test
    public void aRestoreCanItselfBeUndone() throws IOException
    {
        var applied = apply();
        var afterApply = securities(file, null);
        var restored = cli.run("restore", "--backup", applied.meta().get("backupPath").getAsString());

        var undone = cli.run("restore", "--backup", restored.meta().get("backupPath").getAsString());

        assertThat(undone.stdout, undone.exitCode, is(0));
        assertThat(securities(file, null), is(afterApply));
    }

    @Test
    public void aMissingBackupIsRefusedWithoutSideEffects() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());

        var run = cli.run("restore", "--backup", clientFolder.resolve("backups").resolve("nope.xml").toString());

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("backup-not-found"));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aBackupNewerThanSupportedIsRefusedWithoutSideEffects() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var newer = ClientFiles.newerXml(folder.newFolder("newer").toPath());

        var run = cli.run("restore", "--backup", newer.getPath());

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("file-version-unsupported"));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aHeldLockAbortsTheRestore() throws IOException
    {
        var applied = apply();
        var afterApply = Files.readAllBytes(file.toPath());
        var lock = Files.writeString(clientFolder.resolve(".client.xml.pp-cli.lock"), "4711");

        var run = cli.run("restore", "--backup", applied.meta().get("backupPath").getAsString());

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("file-locked"));
        assertThat(Files.readAllBytes(file.toPath()), is(afterApply));
        assertThat(Files.exists(lock), is(true));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", ".client.xml.pp-cli.lock", "backups",
                        "backups/" + APPLY_BACKUP, "client.xml")));
    }

    @Test
    public void aFileChangedBeforeTheRenameAbortsTheRestore() throws IOException
    {
        var applied = apply();
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
                        applied.meta().get("backupPath").getAsString());

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("file-changed"));
        assertThat(Files.readString(file.toPath()).endsWith("<!-- saved by the app -->"), is(true));
        assertThat(ClientFiles.listing(clientFolder),
                        is(List.of("", "backups", "backups/" + APPLY_BACKUP, "client.xml")));
    }

    @Test
    public void aDryRunReportsTheWouldBeBackupAndCreatesNothing() throws IOException
    {
        var applied = apply();
        var afterApply = Files.readAllBytes(file.toPath());
        var before = ClientFiles.listing(clientFolder);

        var run = cli.run("restore", "--backup", applied.meta().get("backupPath").getAsString(), "--dry-run");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("backupPath").getAsString(),
                        is(clientFolder.resolve("backups").resolve(RESTORE_BACKUP).toAbsolutePath().toString()));
        assertThat(Files.readAllBytes(file.toPath()), is(afterApply));
        assertThat(ClientFiles.listing(clientFolder), is(before));
    }

    @Test
    public void restoresAnEncryptedFileWithTheSamePassword() throws IOException
    {
        var password = "secret";
        var encrypted = ClientFiles.xmlAes256(folder.newFolder("encrypted").toPath(), password);
        var original = securities(encrypted, password.toCharArray());
        var env = Map.of(CommandContext.PASSWORD_VARIABLE, password);
        var applied = cli.use(encrypted).applyWith(env, List.of(), CliFixture.ADD_ACME);
        assertThat(applied.stdout, applied.exitCode, is(0));

        var run = cli.run(env, "restore", "--backup", applied.meta().get("backupPath").getAsString());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(securities(encrypted, password.toCharArray()), is(original));
    }
}
