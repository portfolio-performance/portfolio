package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/**
 * The <code>ClientFile.write</code> facet at its own interface: the write
 * meta, the warnings of the write and the codes of a refused one. The
 * safe-write sequence itself is SafeWriteTest's. Today is 2026-10-16, 12:00.
 */
public class ClientFileWriteTest
{
    private static final Facet<ClientFile.Writer> WRITE = ClientFile.write("Preview.");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path clientFolder;
    private File file;

    @Before
    public void setUp() throws IOException
    {
        clientFolder = new CliFixture(folder).clientFolder();
        file = ClientFiles.xml(clientFolder);
    }

    private static Object addAcme(Client client)
    {
        var security = new Security("ACME", client.getBaseCurrency());
        client.addSecurity(security);
        return security.getUUID();
    }

    private FacetHarness.Result<ClientFile.Writer> write(String... more)
    {
        var args = new ArrayList<>(List.of("--file", file.getPath()));
        args.addAll(List.of(more));
        return FacetHarness.run(WRITE, args, null, writer -> writer.write(ClientFileWriteTest::addAcme));
    }

    @Test
    public void aWriteReportsBothFileVersionsAndTheBackupItWrote()
    {
        var result = write();

        var backup = clientFolder.resolve("backups").resolve("client.20261016-120000.pp-cli.xml");
        var meta = result.meta();
        assertThat(meta.get("fileVersionBefore").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("asOf").getAsString(), is("2026-10-16"));
        assertThat(meta.get("dryRun").getAsBoolean(), is(false));
        assertThat(meta.get("backupPath").getAsString(), is(backup.toAbsolutePath().toString()));
        assertThat(Files.exists(backup), is(true));
        assertThat(result.warnings(), is(List.of()));
    }

    @Test
    public void theMetaKeepsThePublishedOrder()
    {
        var members = new ArrayList<>(write().meta().keySet());

        assertThat(members, is(List.of("fileVersionBefore", "fileVersionAfter", "asOf", "dryRun", "backupPath")));
    }

    @Test
    public void aDryRunReportsTheWouldBeBackupAndWritesNothing() throws IOException
    {
        var before = ClientFiles.listing(clientFolder);

        var result = write("--dry-run");

        assertThat(result.meta().get("dryRun").getAsBoolean(), is(true));
        assertThat(result.meta().get("backupPath").getAsString(), is(clientFolder.resolve("backups")
                        .resolve("client.20261016-120000.pp-cli.xml").toAbsolutePath().toString()));
        assertThat(ClientFiles.listing(clientFolder), is(before));
    }

    @Test
    public void theBackupDirOptionMovesTheBackup()
    {
        var elsewhere = folder.getRoot().toPath().resolve("elsewhere");

        var result = write("--backup-dir", elsewhere.toString());

        assertThat(result.meta().get("backupPath").getAsString(),
                        is(elsewhere.resolve("client.20261016-120000.pp-cli.xml").toAbsolutePath().toString()));
    }

    @Test
    public void forwardsTheWarningsOfTheWrite() throws IOException
    {
        var client = ClientFiles.sampleClient();
        var deposit = new AccountTransaction(AccountTransaction.Type.DEPOSIT);
        deposit.setDateTime(LocalDate.parse("2020-03-01").atStartOfDay());
        deposit.setAmount(1_00);
        deposit.setCurrencyCode(client.getAccounts().get(0).getCurrencyCode());
        client.getAccounts().get(0).addTransaction(deposit);
        deposit.setCurrencyCode(null);
        file = new CliFixture(folder).save(client).file();

        var result = write();

        assertThat(result.warningCodes(), is(List.of("consistency-issues-preexisting")));
    }

    @Test
    public void aRefusedWriteFailsWithItsCode() throws IOException
    {
        Files.writeString(clientFolder.resolve(".client.xml.pp-cli.lock"), "4711");

        assertThat(write().code(), is("file-locked"));
    }

    @Test
    public void theWriteOptionsAreCheckedBeforeTheFileIsOpened()
    {
        var result = FacetHarness.run(WRITE, List.of("--keep", "-1"), null,
                        writer -> writer.write(ClientFileWriteTest::addAcme));

        assertThat(result.code(), is("invalid-option-value"));
    }

    @Test
    public void aWriteWithoutAFileFailsWithFileRequired()
    {
        var written = FacetHarness.run(WRITE, List.of(), null, writer -> writer.write(ClientFileWriteTest::addAcme));
        assertThat(written.code(), is("file-required"));
    }
}
