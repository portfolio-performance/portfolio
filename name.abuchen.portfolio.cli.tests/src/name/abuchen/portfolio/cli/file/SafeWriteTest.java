package name.abuchen.portfolio.cli.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * The hardening of the safe-write sequence, seen through <code>apply</code>:
 * where backups go and how many are kept, and what makes a write refuse.
 */
public class SafeWriteTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);
    private Path clientFolder;
    private File file;

    @Before
    public void setUp() throws IOException
    {
        clientFolder = cli.clientFolder();
        file = cli.use(ClientFiles.xml(clientFolder)).file();
    }

    private List<String> applyArguments(String... more) throws IOException
    {
        var batch = cli.batch(CliFixture.ADD_ACME);
        var args = new ArrayList<>(List.of("apply", "--file", file.getPath(), "--input", batch.toString()));
        args.addAll(List.of(more));
        return args;
    }

    private CliRun apply(String... more) throws IOException
    {
        return cli.applyWith(List.of(more), CliFixture.ADD_ACME);
    }

    private static List<String> names(Path root) throws IOException
    {
        try (Stream<Path> paths = Files.list(root))
        {
            return paths.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    /** CLI backups of client.xml, one per day of January 2026, oldest first. */
    private Path seedBackups(Path backups, int count) throws IOException
    {
        Files.createDirectories(backups);
        for (int day = 1; day <= count; day++)
            Files.writeString(backups.resolve("client.202601%02d-000000.pp-cli.xml".formatted(day)), "day " + day);
        return backups;
    }

    @Test
    public void backupDirOverridesWhereTheBackupGoes() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var elsewhere = folder.newFolder("elsewhere").toPath().resolve("deeper");

        var run = apply("--backup-dir", elsewhere.toString());

        assertThat(run.stdout, run.exitCode, is(0));
        var backup = elsewhere.resolve("client.20261016-120000.pp-cli.xml");
        assertThat(Files.readAllBytes(backup), is(original));
        assertThat(run.meta().get("backupPath").getAsString(), is(backup.toAbsolutePath().toString()));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aDryRunReportsTheBackupDirItWouldUseAndCreatesNothing() throws IOException
    {
        var elsewhere = folder.getRoot().toPath().resolve("elsewhere");

        var run = apply("--dry-run", "--backup-dir", elsewhere.toString());

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("backupPath").getAsString(),
                        is(elsewhere.resolve("client.20261016-120000.pp-cli.xml").toAbsolutePath().toString()));
        assertThat(Files.exists(elsewhere), is(false));
    }

    @Test
    public void keepsOnlyTheNewestTwentyBackupsByDefault() throws IOException
    {
        var backups = seedBackups(clientFolder.resolve("backups"), 25);

        var run = apply();

        assertThat(run.stdout, run.exitCode, is(0));
        var kept = names(backups);
        assertThat(kept.size(), is(20));
        assertThat(kept.contains("client.20261016-120000.pp-cli.xml"), is(true));
        // the 19 newest of the seeded ones: January 7 to 25
        assertThat(kept.contains("client.20260106-000000.pp-cli.xml"), is(false));
        assertThat(kept.contains("client.20260107-000000.pp-cli.xml"), is(true));
    }

    @Test
    public void keepOverridesTheNumberOfBackupsAndPrunesTheOldestByTheirTimestamp() throws IOException
    {
        var backups = seedBackups(clientFolder.resolve("backups"), 5);
        // the file system's modification time must not matter, only the name
        Files.setLastModifiedTime(backups.resolve("client.20260101-000000.pp-cli.xml"),
                        FileTime.fromMillis(System.currentTimeMillis()));

        var run = apply("--keep", "3");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(names(backups), is(List.of("client.20260104-000000.pp-cli.xml",
                        "client.20260105-000000.pp-cli.xml", "client.20261016-120000.pp-cli.xml")));
    }

    @Test
    public void keepZeroKeepsEveryBackup() throws IOException
    {
        var backups = seedBackups(clientFolder.resolve("backups"), 25);

        var run = apply("--keep", "0");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(names(backups).size(), is(26));
    }

    @Test
    public void pruningNeverTouchesFilesThatAreNotCliBackupsOfThisFile() throws IOException
    {
        var backups = seedBackups(clientFolder.resolve("backups"), 3);
        var bystanders = List.of("other.20250101-000000.pp-cli.xml", "client.20250101-000000.xml", "notes.txt");
        for (var name : bystanders)
            Files.writeString(backups.resolve(name), "keep me");
        var appBackup = clientFolder.resolve("client.xml.backup");
        Files.writeString(appBackup, "the app's own");

        var run = apply("--keep", "1");

        assertThat(run.stdout, run.exitCode, is(0));
        var kept = names(backups);
        assertThat(kept.containsAll(bystanders), is(true));
        assertThat(kept.contains("client.20261016-120000.pp-cli.xml"), is(true));
        assertThat(kept.contains("client.20260103-000000.pp-cli.xml"), is(false));
        assertThat(Files.readString(appBackup), is("the app's own"));
    }

    @Test
    public void aFailedWriteDoesNotPruneAnything() throws IOException
    {
        var backups = seedBackups(clientFolder.resolve("backups"), 5);
        var bad = "{\"operation\":\"instrument.add\",\"name\":\"Bad\",\"isin\":\"nope\"}";

        var run = cli.applyWith(List.of("--keep", "1"), bad);

        assertThat(run.exitCode, is(1));
        assertThat(names(backups).size(), is(5));
    }

    @Test
    public void everyFormatCanBeWrittenAndReadBackForTheConsistencyCheck() throws IOException
    {
        var password = "secret";
        var formats = new LinkedHashMap<String, File>();
        formats.put("xml-id-references", ClientFiles.xmlWithIdReferences(folder.newFolder("a").toPath()));
        formats.put("xml-zip", ClientFiles.xmlZip(folder.newFolder("b").toPath()));
        formats.put("binary", ClientFiles.binary(folder.newFolder("c").toPath()));
        formats.put("xml-aes256", ClientFiles.xmlAes256(folder.newFolder("d").toPath(), password));
        formats.put("xml-aes128", ClientFiles.xmlAes128(folder.newFolder("e").toPath(), password));
        formats.put("binary-aes256", ClientFiles.binaryAes256(folder.newFolder("f").toPath(), password));

        for (var format : formats.entrySet())
        {
            file = cli.use(format.getValue()).file();
            var instrument = ClientFactory.load(file, password.toCharArray(), new NullProgressMonitor())
                            .getSecurities().get(0);
            var update = "{\"operation\":\"instrument.update\",\"uuid\":\"" + instrument.getUUID()
                            + "\",\"note\":\"changed\"}";
            var run = cli.applyWith(Map.of(CommandContext.PASSWORD_VARIABLE, password), List.of(), update);

            assertThat(format.getKey() + ": " + run.stdout, run.exitCode, is(0));
            var saved = ClientFactory.load(file, password.toCharArray(), new NullProgressMonitor());
            assertThat(format.getKey(), saved.getSecurities().get(0).getNote(), is("changed"));
            assertThat(format.getKey(), FileFormat.isEncrypted(file) || FileFormat.detect(file).id()
                            .equals(format.getKey()), is(true));
            assertThat(format.getKey(), ClientFiles.listing(file.getParentFile().toPath()).stream()
                            .noneMatch(name -> name.contains("pp-cli.lock") || name.contains("pp-cli-tmp")), is(true));
        }
    }

    @Test
    public void keepMustBeAWholeNumberOfZeroOrMore() throws IOException
    {
        var run = apply("--keep", "-1");

        assertThat(run.exitCode, is(2));
        assertThat(run.code(), is("invalid-option-value"));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    private CliRun applyWith(WriteEnvironment environment, String osName, String... more) throws IOException
    {
        return CliRun.runWith(environment, osName, applyArguments(more).toArray(String[]::new));
    }

    @Test
    public void aFileChangedByTheAppBeforeTheRenameAbortsTheWriteAndLeavesNothingBehind() throws IOException
    {
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

        var run = applyWith(environment, "Linux");

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("file-changed"));
        assertThat(Files.readString(file.toPath()).endsWith("<!-- saved by the app -->"), is(true));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aFileThatOnlyGotANewModificationTimeAbortsTheWriteToo() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var environment = new WriteEnvironment()
        {
            @Override
            public void beforeRecheck(Path target)
            {
                try
                {
                    Files.setLastModifiedTime(target, FileTime.fromMillis(1_000_000_000_000L));
                }
                catch (IOException e)
                {
                    throw new AssertionError(e);
                }
            }
        };

        var run = applyWith(environment, "Linux");

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("file-changed"));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aFilesystemThatRefusesAnAtomicMoveGetsAPlainReplace() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var moves = new ArrayList<String>();
        var environment = new WriteEnvironment()
        {
            @Override
            public void move(Path source, Path target, boolean atomic) throws IOException
            {
                moves.add(atomic ? "atomic" : "replace");
                if (atomic)
                    throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "simulated");
                WriteEnvironment.super.move(source, target, atomic);
            }
        };

        var run = applyWith(environment, "Linux");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(moves, is(List.of("atomic", "replace")));
        assertThat(Files.readAllBytes(file.toPath()), is(not(original)));
        assertThat(ClientFiles.listing(clientFolder),
                        is(List.of("", "backups", "backups/client.20261016-120000.pp-cli.xml", "client.xml")));
    }

    @Test
    public void anyOtherFailureToMoveIsNotRetriedAsAPlainReplace() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var moves = new ArrayList<String>();
        var environment = new WriteEnvironment()
        {
            @Override
            public void move(Path source, Path target, boolean atomic) throws IOException
            {
                moves.add(atomic ? "atomic" : "replace");
                throw new AccessDeniedException(target.toString());
            }
        };

        var run = applyWith(environment, "Linux");

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("write-failed"));
        assertThat(moves, is(List.of("atomic")));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        // the move was tried, so the backup stays; only the temp file is gone
        assertThat(ClientFiles.listing(clientFolder),
                        is(List.of("", "backups", "backups/client.20261016-120000.pp-cli.xml", "client.xml")));
    }

    private static WriteEnvironment fileIsOpen(List<Path> probed)
    {
        return new WriteEnvironment()
        {
            @Override
            public boolean isOpenInApp(Path target)
            {
                probed.add(target);
                return true;
            }
        };
    }

    @Test
    public void aFileThatIsOpenOnMacOsIsRefusedWithoutForce() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());
        var probed = new ArrayList<Path>();

        var run = applyWith(fileIsOpen(probed), "Mac OS X");

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("file-open-in-app"));
        assertThat(run.problem().get("file").getAsString(), is(file.toPath().toAbsolutePath().toString()));
        assertThat(probed, is(List.of(file.toPath().toAbsolutePath())));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void forceWritesAnOpenFileAndWarns() throws IOException
    {
        var run = applyWith(fileIsOpen(new ArrayList<>()), "Mac OS X", "--force");

        assertThat(run.stdout, run.exitCode, is(0));
        var warnings = run.warnings();
        assertThat(warnings.size(), is(1));
        assertThat(warnings.get(0).getAsJsonObject().get("code").getAsString(), is("file-open-in-app"));
    }

    @Test
    public void aFileThatIsNotOpenOnMacOsIsWrittenWithoutAWarning() throws IOException
    {
        var run = applyWith(new WriteEnvironment()
        {
            @Override
            public boolean isOpenInApp(Path target)
            {
                return false;
            }
        }, "Mac OS X");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.warnings().size(), is(0));
    }

    @Test
    public void theOpenFileProbeIsMacOsOnly() throws IOException
    {
        var probed = new ArrayList<Path>();

        var run = applyWith(fileIsOpen(probed), "Windows 11");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(probed, is(List.of()));
    }

    @Test
    public void aDryRunDoesNotProbe() throws IOException
    {
        var probed = new ArrayList<Path>();

        var run = applyWith(fileIsOpen(probed), "Mac OS X", "--dry-run");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(probed, is(List.of()));
    }

    /** A deposit without a currency, which core's checker reports for the cash account. */
    private static void addCurrencylessDeposit(Client client, String date)
    {
        var deposit = new AccountTransaction(AccountTransaction.Type.DEPOSIT);
        deposit.setDateTime(LocalDate.parse(date).atStartOfDay());
        deposit.setAmount(1_00);
        deposit.setCurrencyCode(client.getAccounts().get(0).getCurrencyCode());
        client.getAccounts().get(0).addTransaction(deposit);
        deposit.setCurrencyCode(null);
    }

    private Client clientWithAConsistencyIssue() throws IOException
    {
        var client = ClientFiles.sampleClient();
        addCurrencylessDeposit(client, "2020-03-01");
        file = cli.save(client).file();
        return client;
    }

    private static final SafeWrite.Mutation<Object> INTRODUCE_AN_ISSUE = client -> {
        addCurrencylessDeposit(client, "2020-04-01");
        return null;
    };

    private SafeWrite.Settings settings(boolean dryRun)
    {
        return new SafeWrite.Settings(CliRun.CLOCK, dryRun, null, 20, false, "Linux", WriteEnvironment.SYSTEM);
    }

    private CliException refusedWrite(boolean dryRun, SafeWrite.Mutation<Object> change)
    {
        try (var opened = ClientLoader.open(file.getPath(), () -> null))
        {
            SafeWrite.run(opened, settings(dryRun), change);
        }
        catch (CliException e)
        {
            return e;
        }
        throw new AssertionError("the write was not refused");
    }

    @Test
    public void issuesTheFileAlreadyHadAreWarningsAndDoNotBlockTheWrite() throws IOException
    {
        clientWithAConsistencyIssue();

        var run = apply();

        assertThat(run.stdout, run.exitCode, is(0));
        var warnings = run.warnings();
        assertThat(warnings.size(), is(1));
        var warning = warnings.get(0).getAsJsonObject();
        assertThat(warning.get("code").getAsString(), is("consistency-issues-preexisting"));
        assertThat(warning.get("count").getAsInt(), is(1));
        var issue = warning.getAsJsonArray("issues").get(0).getAsJsonObject();
        assertThat(issue.get("check").getAsString(), is("TransactionMissingCurrencyIssue"));
        assertThat(issue.get("entity").getAsString(), startsWith("account:"));
        assertThat(issue.get("message").getAsString().isEmpty(), is(false));

        var saved = ClientFactory.load(file, null, new NullProgressMonitor());
        assertThat(saved.getSecurities().size(), is(3));
    }

    @Test
    public void aDryRunReportsThePreExistingIssuesToo() throws IOException
    {
        clientWithAConsistencyIssue();

        var run = apply("--dry-run");

        assertThat(run.stdout, run.exitCode, is(0));
        var warnings = run.warnings();
        assertThat(warnings.get(0).getAsJsonObject().get("code").getAsString(), is("consistency-issues-preexisting"));
    }

    @Test
    public void aFileWithoutIssuesGivesNoWarning() throws IOException
    {
        var run = apply();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.warnings().size(), is(0));
    }

    @Test
    public void theCheckersRepairsNeverReachTheWrittenFile() throws IOException
    {
        // a deposit with a security: core's checker repairs it by removing the security
        var client = ClientFiles.sampleClient();
        var deposit = client.getAccounts().get(0).getTransactions().stream()
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT).findFirst().orElseThrow();
        deposit.setSecurity(client.getSecurities().get(1));
        file = cli.save(client).file();

        var run = apply();

        assertThat(run.stdout, run.exitCode, is(0));
        var saved = ClientFactory.load(file, null, new NullProgressMonitor());
        var savedDeposit = saved.getAccounts().get(0).getTransactions().stream()
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT).findFirst().orElseThrow();
        assertThat(savedDeposit.getSecurity() == null, is(false));
    }

    @Test
    public void aChangeThatIntroducesAConsistencyIssueIsRefusedAndTheFileStaysUntouched() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());

        var refusal = refusedWrite(false, INTRODUCE_AN_ISSUE);

        assertThat(refusal.getCode(), is("consistency-check-failed"));
        var issues = (List<?>) refusal.toProblem().get("issues");
        assertThat(issues.size(), is(1));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aDryRunRefusesAChangeThatIntroducesAConsistencyIssueToo() throws IOException
    {
        var refusal = refusedWrite(true, INTRODUCE_AN_ISSUE);

        assertThat(refusal.getCode(), is("consistency-check-failed"));
    }

    @Test
    public void aFurtherIssueOfTheSameKindOnTheSameEntityIsStillNew() throws IOException
    {
        clientWithAConsistencyIssue();
        var original = Files.readAllBytes(file.toPath());

        var refusal = refusedWrite(false, INTRODUCE_AN_ISSUE);

        assertThat(refusal.getCode(), is("consistency-check-failed"));
        assertThat(((List<?>) refusal.toProblem().get("issues")).size(), is(1));
        assertThat(Files.readAllBytes(file.toPath()), is(original));
        assertThat(ClientFiles.listing(clientFolder), is(List.of("", "client.xml")));
    }

    @Test
    public void aChangeThatFixesAnIssueIsNotRefused() throws IOException
    {
        clientWithAConsistencyIssue();

        try (var opened = ClientLoader.open(file.getPath(), () -> null))
        {
            var outcome = SafeWrite.run(opened, settings(false), client -> {
                var account = client.getAccounts().get(0);
                account.getTransactions().stream().filter(t -> t.getCurrencyCode() == null).toList()
                                .forEach(t -> account.getTransactions().remove(t));
                return null;
            });
            assertThat(outcome.warnings().size(), is(0));
        }
        catch (CliException e)
        {
            throw new AssertionError(e.toProblem().toString());
        }
    }
}
