package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class ApplyCommandTest
{
    private static final String APPLE_ISIN = "US0378331005";
    private static final String SAP_ISIN = "DE0007164600";

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

    private static Security instrument(Client client, String name)
    {
        return client.getSecurities().stream().filter(s -> name.equals(s.getName())).findFirst().orElseThrow();
    }

    private static String add(String fields)
    {
        return "{\"operation\":\"instrument.add\"," + fields + "}";
    }

    private static String update(String uuid, String fields)
    {
        return "{\"operation\":\"instrument.update\",\"uuid\":\"" + uuid + "\"," + fields + "}";
    }

    /** The instrument that has transactions. */
    private Security tradedInstrument() throws IOException
    {
        return cli.reload().getSecurities().get(0);
    }

    /** The instrument that has none. */
    private Security idleInstrument() throws IOException
    {
        return cli.reload().getSecurities().get(1);
    }

    @Test
    public void addsAnInstrumentAndReportsTheCreatedRecord() throws IOException
    {
        var run = cli.apply(add("""
                        "name":"ACME Corp","currencyCode":"USD","isin":"%s","tickerSymbol":"ACME",
                        "wkn":"A12345","note":"new","retired":false""".formatted(APPLE_ISIN)));

        assertThat(run.stdout, run.exitCode, is(0));

        var created = run.created();
        assertThat(created.get("entity").getAsString(), is("instrument"));

        var security = instrument(cli.reload(), "ACME Corp");
        assertThat(created.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(created.getAsJsonObject("after").get("isin").getAsString(), is(APPLE_ISIN));
        assertThat(security.getCurrencyCode(), is("USD"));
        assertThat(security.getIsin(), is(APPLE_ISIN));
        assertThat(security.getTickerSymbol(), is("ACME"));
        assertThat(security.getWkn(), is("A12345"));
        assertThat(security.getNote(), is("new"));
        assertThat(security.isRetired(), is(false));
    }

    @Test
    public void anAddNeedsOnlyAName() throws IOException
    {
        var run = cli.apply(add("\"name\":\"Bare\""));

        assertThat(run.stdout, run.exitCode, is(0));
        var security = instrument(cli.reload(), "Bare");
        assertThat(security.getIsin() == null, is(true));
        assertThat(security.isRetired(), is(false));
    }

    @Test
    public void updatesOnlyThePresentFieldsAndReportsBeforeAndAfter() throws IOException
    {
        var uuid = idleInstrument().getUUID();
        cli.apply(update(uuid, "\"name\":\"Old\",\"currencyCode\":\"EUR\",\"note\":\"first\",\"wkn\":\"W1\""));

        var run = cli.apply(update(uuid, "\"name\":\"Renamed\",\"note\":null,\"retired\":true,\"wkn\":\"W1\""));

        assertThat(run.stdout, run.exitCode, is(0));

        var updated = run.updated();
        assertThat(updated.get("entity").getAsString(), is("instrument"));
        assertThat(updated.get("uuid").getAsString(), is(uuid));
        // wkn is unchanged and therefore not reported
        assertThat(updated.getAsJsonObject("before").keySet(), containsInAnyOrder("name", "note", "retired"));
        assertThat(updated.getAsJsonObject("before").get("name").getAsString(), is("Old"));
        assertThat(updated.getAsJsonObject("after").get("name").getAsString(), is("Renamed"));
        assertThat(updated.getAsJsonObject("after").get("note").isJsonNull(), is(true));
        assertThat(updated.getAsJsonObject("after").get("retired").getAsBoolean(), is(true));

        var security = cli.reload().getSecurities().get(1);
        assertThat(security.getName(), is("Renamed"));
        assertThat(security.getNote() == null, is(true));
        assertThat(security.getCurrencyCode(), is("EUR"));
        assertThat(security.getWkn(), is("W1"));
        assertThat(security.isRetired(), is(true));
    }

    @Test
    public void refusesAnInvalidIsinWithAFieldLevelError() throws IOException
    {
        var before = Files.readAllBytes(file.toPath());

        var run = cli.apply(add("\"name\":\"Bad\",\"isin\":\"US0378331006\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("validation-failed"));
        assertThat(run.failures().size(), is(1));
        assertThat(run.failure(0).get("operationIndex").getAsInt(), is(0));
        assertThat(run.failure(0).get("field").getAsString(), is("isin"));
        assertThat(run.failure(0).get("code").getAsString(), is("invalid-isin"));
        assertThat(run.failure(0).get("message").getAsString(), containsString("US0378331006"));
        assertThat(run.failure(0).get("check").getAsString(), is("Isin.isValid"));
        assertThat(run.failure(0).keySet(),
                        containsInAnyOrder("operationIndex", "field", "code", "message", "check"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
    }

    @Test
    public void refusesAnIsinThatAnotherInstrumentAlreadyHas() throws IOException
    {
        var holder = tradedInstrument();
        cli.apply(update(holder.getUUID(), "\"isin\":\"" + APPLE_ISIN + "\""));

        var run = cli.apply(add("\"name\":\"Twin\",\"isin\":\"" + APPLE_ISIN + "\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("field").getAsString(), is("isin"));
        assertThat(run.failure(0).get("code").getAsString(), is("duplicate-isin"));
        assertThat(run.failure(0).get("message").getAsString(), containsString(holder.getUUID()));
    }

    @Test
    public void refusesAnIsinThatAnEarlierAddOfTheSameBatchTook() throws IOException
    {
        var run = cli.apply(add("\"name\":\"First\",\"isin\":\"" + APPLE_ISIN + "\""),
                        add("\"name\":\"Second\",\"isin\":\"" + APPLE_ISIN + "\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.failures().size(), is(1));
        assertThat(run.failure(0).get("operationIndex").getAsInt(), is(1));
        assertThat(run.failure(0).get("code").getAsString(), is("duplicate-isin"));
    }

    @Test
    public void anUpdateMayKeepAnInstrumentsOwnIsin() throws IOException
    {
        var uuid = idleInstrument().getUUID();
        cli.apply(update(uuid, "\"isin\":\"" + APPLE_ISIN + "\""));

        var run = cli.apply(update(uuid, "\"isin\":\"" + APPLE_ISIN + "\",\"note\":\"same\""));

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void refusesACurrencyChangeOnceTheInstrumentHasTransactions() throws IOException
    {
        var traded = tradedInstrument();
        var other = traded.getCurrencyCode().equals("USD") ? "GBP" : "USD";
        var before = Files.readAllBytes(file.toPath());

        var run = cli.apply(update(traded.getUUID(), "\"currencyCode\":\"" + other + "\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("field").getAsString(), is("currencyCode"));
        assertThat(run.failure(0).get("code").getAsString(), is("currency-immutable"));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
    }

    @Test
    public void allowsRepeatingTheCurrencyOfAnInstrumentWithTransactions() throws IOException
    {
        var traded = tradedInstrument();

        var run = cli.apply(update(traded.getUUID(),
                        "\"currencyCode\":\"" + traded.getCurrencyCode() + "\",\"note\":\"kept\""));

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void allowsACurrencyChangeBeforeTheInstrumentHasTransactions() throws IOException
    {
        var idle = idleInstrument();
        var other = idle.getCurrencyCode().equals("USD") ? "GBP" : "USD";

        var run = cli.apply(update(idle.getUUID(), "\"currencyCode\":\"" + other + "\""));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(cli.reload().getSecurities().get(1).getCurrencyCode(), is(other));
    }

    @Test
    public void reportsEveryFailureOfABatchAtOnceAndLeavesTheFileUntouched() throws IOException
    {
        var before = Files.readAllBytes(file.toPath());
        var listingBefore = ClientFiles.listing(clientFolder);

        var run = cli.apply(add("\"name\":\"Fine\""), //
                        add("\"name\":\"Bad ISIN\",\"isin\":\"nope\""), //
                        add("\"currencyCode\":\"ZZZ\""), //
                        update("not-there", "\"name\":\"x\""), //
                        "{\"operation\":\"instrument.remove\"}", //
                        add("\"name\":\"Typo\",\"colour\":\"red\",\"retired\":\"yes\""));

        assertThat(run.exitCode, is(1));

        assertThat(run.failureKeys(), containsInAnyOrder("1:isin:invalid-isin", "2:currencyCode:invalid-currency",
                        "2:name:required", "3:uuid:reference-not-found", "4:operation:unknown-operation",
                        "5:colour:unknown-field", "5:retired:invalid-type"));

        assertThat(Files.readAllBytes(file.toPath()), is(before));
        assertThat(ClientFiles.listing(clientFolder), is(listingBefore));
    }

    @Test
    public void aLaterFailureAppliesNothingOfTheEarlierOperations() throws IOException
    {
        var run = cli.apply(add("\"name\":\"Would be added\""), add("\"name\":\"Bad\",\"isin\":\"nope\""));

        assertThat(run.exitCode, is(1));
        assertThat(cli.reload().getSecurities().size(), is(2));
    }

    @Test
    public void refusesTwoOperationsOnTheSameExistingInstrument() throws IOException
    {
        var uuid = idleInstrument().getUUID();

        var run = cli.apply(update(uuid, "\"note\":\"one\""), update(uuid, "\"note\":\"two\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.failures().size(), is(1));
        assertThat(run.failure(0).get("operationIndex").getAsInt(), is(1));
        assertThat(run.failure(0).get("field").getAsString(), is("uuid"));
        assertThat(run.failure(0).get("code").getAsString(), is("duplicate-target"));
        assertThat(cli.reload().getSecurities().get(1).getNote() == null, is(true));
    }

    @Test
    public void anInstrumentAddedInTheBatchCannotBeReferencedByUuid() throws IOException
    {
        var run = cli.apply(add("\"name\":\"New\""), update("00000000-0000-0000-0000-000000000000", "\"note\":\"x\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("code").getAsString(), is("reference-not-found"));
    }

    @Test
    public void aSuccessfulWriteLeavesABackupOfTheOriginalAndNoLockOrTempFile() throws IOException
    {
        var original = Files.readAllBytes(file.toPath());

        var run = cli.apply(add("\"name\":\"ACME\""));

        assertThat(run.stdout, run.exitCode, is(0));

        var backup = clientFolder.resolve("backups").resolve("client.20261016-120000.pp-cli.xml");
        assertThat(Files.readAllBytes(backup), is(original));
        assertThat(ClientFiles.listing(clientFolder),
                        is(List.of("", "backups", "backups/client.20261016-120000.pp-cli.xml", "client.xml")));
        assertThat(Files.readAllBytes(file.toPath()), is(not(original)));

        var meta = run.meta();
        assertThat(meta.get("backupPath").getAsString(), is(backup.toAbsolutePath().toString()));
        assertThat(meta.get("fileVersionBefore").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("fileVersionAfter").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("dryRun").getAsBoolean(), is(false));
    }

    @Test
    public void twoWritesInTheSameSecondKeepBothBackups() throws IOException
    {
        cli.apply(add("\"name\":\"One\""));
        var run = cli.apply(add("\"name\":\"Two\""));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(ClientFiles.listing(clientFolder.resolve("backups")),
                        is(List.of("", "client.20261016-120000-2.pp-cli.xml", "client.20261016-120000.pp-cli.xml")));
    }

    @Test
    public void aDryRunReportsTheSameChangesAndTheWouldBeBackupWithoutCreatingAnything() throws IOException
    {
        var uuid = idleInstrument().getUUID();
        var operation = update(uuid, "\"name\":\"Renamed\",\"isin\":\"" + SAP_ISIN + "\"");
        var before = Files.readAllBytes(file.toPath());
        var listingBefore = ClientFiles.listing(clientFolder);

        var dry = cli.applyWith(List.of("--dry-run"), operation);

        assertThat(dry.stdout, dry.exitCode, is(0));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
        assertThat(ClientFiles.listing(clientFolder), is(listingBefore));
        assertThat(dry.meta().get("dryRun").getAsBoolean(), is(true));
        assertThat(dry.meta().get("backupPath").getAsString(), is(clientFolder.resolve("backups")
                        .resolve("client.20261016-120000.pp-cli.xml").toAbsolutePath().toString()));

        var real = cli.apply(operation);

        assertThat(real.exitCode, is(0));
        assertThat(dry.data(), is(real.data()));
    }

    @Test
    public void aDryRunStillReportsValidationFailures() throws IOException
    {
        var run = cli.applyWith(List.of("--dry-run"), add("\"name\":\"Bad\",\"isin\":\"nope\""));

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("code").getAsString(), is("invalid-isin"));
    }

    @Test
    public void aLockFileMakesApplyFailFastWhileReadsStillWork() throws IOException
    {
        var lock = clientFolder.resolve(".client.xml.pp-cli.lock");
        Files.writeString(lock, "4711");
        var before = Files.readAllBytes(file.toPath());

        var run = cli.apply(add("\"name\":\"Blocked\""));

        assertThat(run.exitCode, is(1));
        var problem = run.problem();
        assertThat(problem.get("code").getAsString(), is("file-locked"));
        assertThat(problem.get("pid").getAsString(), is("4711"));
        assertThat(problem.get("lockFile").getAsString(), is(lock.toAbsolutePath().toString()));
        assertThat(Files.readAllBytes(file.toPath()), is(before));
        assertThat(ClientFiles.listing(clientFolder), contains("", ".client.xml.pp-cli.lock", "client.xml"));

        var read = cli.run("instruments");
        assertThat(read.exitCode, is(0));
    }

    @Test
    public void aFileNewerThanSupportedIsRefusedBeforeAnyLockBackupOrTempFile() throws IOException
    {
        file = cli.use(ClientFiles.newerXml(clientFolder)).file();
        var listingBefore = ClientFiles.listing(clientFolder);

        var run = cli.apply(add("\"name\":\"Never\""));

        assertThat(run.exitCode, is(1));
        var problem = run.problem();
        assertThat(problem.get("code").getAsString(), is("file-version-unsupported"));
        assertThat(problem.get("fileVersion").getAsInt(), is(ClientFiles.NEWER_VERSION));
        assertThat(ClientFiles.listing(clientFolder), is(listingBefore));
    }

    @Test
    public void readsTheBatchFromStdin() throws IOException
    {
        var run = CliRun.runWithStdin(Map.of(), "{\"operations\":[" + add("\"name\":\"Piped\"") + "]}", "apply",
                        "--file", file.getPath(), "--input", "-");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(instrument(cli.reload(), "Piped").getName(), is("Piped"));
    }

    @Test
    public void malformedInputIsAUsageError() throws IOException
    {
        for (var text : List.of("not json", "[]", "{}", "{\"operations\":[]}", "{\"operations\":{}}"))
        {
            var run = cli.run("apply", "--input", cli.input(text).toString());

            assertThat(text, run.exitCode, is(2));
            assertThat(text, run.code(), is("invalid-input"));
        }

        var missing = cli.run("apply", "--input", cli.inputFolder().resolve("absent.json").toString());
        assertThat(missing.exitCode, is(2));
        assertThat(missing.code(), is("input-unreadable"));
    }

    @Test
    public void inputIsRequired()
    {
        var run = cli.run("apply");

        assertThat(run.exitCode, is(2));
        assertThat(run.code(), is("option-required"));
    }
}
