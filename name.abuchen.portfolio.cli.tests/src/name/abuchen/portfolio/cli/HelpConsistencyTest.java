package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * Every declared command and operation must be reachable through
 * <code>--help</code>, and every real response must conform to the schema that
 * is declared in code for it, and from which help is generated. A later ticket that adds a command gets a failure here
 * until it adds sample arguments and a correct schema.
 */
public class HelpConsistencyTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Arguments that make the command succeed against a sample file. */
    private static List<String> sampleArguments(String command, Path sampleFile, Security instrument, Path batch)
    {
        return switch (command)
        {
            case "info" -> List.of("info", "--file", sampleFile.toString());
            case "instruments", "investment-accounts", "cash-accounts" ->
                List.of(command, "--file", sampleFile.toString());
            case "prices" -> List.of("prices", "--file", sampleFile.toString(), "--instrument", instrument.getUUID());
            case "transactions" -> List.of("transactions", "--file", sampleFile.toString(), "--currency", "USD");
            case "holdings" -> List.of("holdings", "--file", sampleFile.toString(), "--date", "2026-10-01",
                            "--currency", "USD", "--include-zero");
            case "performance" -> List.of("performance", "--file", sampleFile.toString(), "--from", "2019-12-31",
                            "--to", "2026-10-01", "--currency", "USD", "--cost-method", "moving-average");
            case "taxonomies" -> List.of("taxonomies", "--file", sampleFile.toString());
            case "allocation" -> List.of("allocation", "--file", sampleFile.toString(), "--taxonomy", "Classes",
                            "--date", "2026-10-01", "--currency", "USD");
            case "apply" -> List.of("apply", "--file", sampleFile.toString(), "--input", batch.toString(),
                            "--dry-run");
            case "restore" -> List.of("restore", "--file", sampleFile.toString(), "--backup",
                            sampleFile.toString(), "--dry-run");
            default -> throw new AssertionError("Add sample arguments for command '" + command
                            + "' to HelpConsistencyTest.sampleArguments");
        };
    }

    @Test
    public void everyDeclaredCommandAndOperationIsReachableThroughHelp()
    {
        var global = CliRun.run("--help");
        assertThat(global.exitCode, is(0));

        for (var command : Commands.ALL)
        {
            assertThat(command.name(), global.stdout, containsString("  " + command.name() + " "));

            var help = CliRun.run(command.name(), "--help");
            assertThat(command.name(), help.exitCode, is(0));
            assertThat(command.name(), help.stdout, containsString("pp-cli " + command.name()));
        }

        var apply = CliRun.run("apply", "--help");
        for (var operation : Commands.OPERATIONS)
        {
            assertThat(operation.name(), apply.stdout, containsString("  " + operation.name() + " "));

            var help = CliRun.run("apply", "--help", operation.name());
            assertThat(operation.name(), help.exitCode, is(0));
            assertThat(operation.name(), help.stdout, containsString(operation.name()));
        }
    }

    @Test
    public void everyCommandsRealResponseConformsToItsDeclaredSchemas() throws IOException
    {
        // a stale price, so that prices emits a warning to check as well
        var client = ClientFiles.sampleClient();
        var instrument = client.getSecurities().get(0);
        instrument.addPrice(new SecurityPrice(LocalDate.parse("2026-01-02"), 100_000_000L));

        // a taxonomy with a nested classification and both kinds of assignment
        var taxonomy = TaxonomyFixtures.taxonomy(client, "Classes");
        var equity = TaxonomyFixtures.child(taxonomy.getRoot(), "Equity");
        equity.setKey("EQ");
        equity.setNote("Shares");
        var europe = TaxonomyFixtures.child(equity, "Europe");
        TaxonomyFixtures.assign(europe, instrument, 60);
        TaxonomyFixtures.assign(equity, client.getPortfolios().get(0).getReferenceAccount(), 100);
        TaxonomyFixtures.child(taxonomy.getRoot(), "Bonds");

        // one add and one update per entity kind, so that every kind of change record is checked
        var cash = client.getAccounts().get(0);
        var depot = client.getPortfolios().get(0);
        var other = client.getSecurities().get(1);
        var spareCash = new AccountBuilder().addTo(client);
        var spareDepot = new PortfolioBuilder(spareCash).addTo(client);
        var sampleFile = ClientFiles.xml(folder.getRoot().toPath(), client).toPath();
        var batch = folder.newFile("batch.json").toPath();
        var trade = "{\"operation\":\"transaction.add\",\"investmentAccount\":\"" + depot.getUUID()
                        + "\",\"currencyCode\":\"" + cash.getCurrencyCode() + "\",\"instrument\":\"" + other.getUUID()
                        + "\"";
        Files.writeString(batch, "{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"New\"},"
                        + "{\"operation\":\"taxonomy.assign\",\"taxonomy\":\"" + taxonomy.getId() + "\",\"instrument\":\""
                        + other.getUUID() + "\",\"assignments\":[{\"classification\":\"" + europe.getId()
                        + "\",\"weight\":0.25}]},"
                        + "{\"operation\":\"instrument.update\",\"uuid\":\"" + instrument.getUUID()
                        + "\",\"note\":null,\"retired\":true},"
                        + "{\"operation\":\"cash-account.add\",\"name\":\"Savings\",\"currencyCode\":\"USD\"},"
                        + "{\"operation\":\"cash-account.update\",\"uuid\":\"" + cash.getUUID()
                        + "\",\"note\":\"main\"},"
                        + "{\"operation\":\"investment-account.add\",\"name\":\"Second depot\","
                        + "\"referenceCashAccount\":\"" + cash.getUUID() + "\"},"
                        + "{\"operation\":\"investment-account.update\",\"uuid\":\"" + depot.getUUID()
                        + "\",\"retired\":true},"
                        + "{\"operation\":\"transaction.update\",\"uuid\":\"" + cash.getTransactions().get(0).getUUID()
                        + "\",\"note\":\"corrected\"},"
                        + "{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\""
                        + cash.getUUID() + "\",\"date\":\"2026-05-01\",\"amount\":250.00,\"currencyCode\":\""
                        + cash.getCurrencyCode() + "\"},"
                        + trade.replace("{\"operation\":\"transaction.add\"",
                                        "{\"operation\":\"transaction.add\",\"type\":\"BUY\",\"cashAccount\":\""
                                                        + cash.getUUID() + "\",\"date\":\"2026-05-02\",\"shares\":2,"
                                                        + "\"amount\":250.00")
                        + "},"
                        + trade.replace("{\"operation\":\"transaction.add\"",
                                        "{\"operation\":\"transaction.add\",\"type\":\"SELL\",\"cashAccount\":\""
                                                        + cash.getUUID() + "\",\"date\":\"2026-05-06\",\"shares\":1,"
                                                        + "\"amount\":120.00")
                        + "},"
                        + trade.replace("{\"operation\":\"transaction.add\"",
                                        "{\"operation\":\"transaction.add\",\"type\":\"DELIVERY_INBOUND\","
                                                        + "\"date\":\"2026-05-03\",\"shares\":1,\"amount\":100.00")
                        + "},"
                        + "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"security\",\"from\":\""
                        + depot.getUUID() + "\",\"to\":\"" + spareDepot.getUUID() + "\",\"instrument\":\""
                        + instrument.getUUID() + "\",\"date\":\"2026-05-04\",\"shares\":2,\"amount\":200.00,"
                        + "\"currencyCode\":\"" + instrument.getCurrencyCode() + "\"},"
                        + "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\""
                        + cash.getUUID() + "\",\"to\":\"" + spareCash.getUUID()
                        + "\",\"date\":\"2026-05-05\",\"amount\":50.00,\"currencyCode\":\""
                        + cash.getCurrencyCode() + "\"}]}");

        // a delete can only be applied once, so it is only previewed; a deposit leaves every holding alone
        Files.writeString(batch.resolveSibling("delete.json"), "{\"operations\":[{\"operation\":\"transaction.delete\","
                        + "\"uuid\":\"" + cash.getTransactions().get(0).getUUID() + "\"}]}");

        assertOperationInputsConform(batch);
        Files.copy(sampleFile, batch.resolveSibling("original.xml"));

        for (var command : Commands.ALL)
        {
            var name = command.name();

            assertConforms(command, sampleArguments(name, sampleFile, instrument, batch));
            for (var arguments : furtherArguments(name, sampleFile, client, batch))
                assertConforms(command, arguments);
        }
    }

    /** The operations an agent sends must conform to the input schema that help publishes for them. */
    private void assertOperationInputsConform(Path batch) throws IOException
    {
        var inputs = new HashMap<String, JsonObject>();
        Commands.OPERATIONS.forEach(o -> inputs.put(o.name(), ResponseContract.json(o.input())));

        var operations = JsonParser.parseString(Files.readString(batch)).getAsJsonObject()
                        .getAsJsonArray("operations");
        for (var element : operations)
        {
            var operation = element.getAsJsonObject();
            var name = operation.get("operation").getAsString();
            assertThat(name + " input " + operation, SchemaValidator.validate(operation, inputs.get(name)),
                            is(empty()));
        }
    }

    /** More ways to call a command whose response differs in shape. */
    private static List<List<String>> furtherArguments(String command, Path sampleFile, Client client, Path batch)
    {
        if (command.equals("apply"))
            return List.of(List.of("apply", "--file", sampleFile.toString(), "--input", batch.toString()),
                            List.of("apply", "--file", sampleFile.toString(), "--input", batch.toString(),
                                            "--backup-dir", batch.resolveSibling("elsewhere").toString(), "--keep",
                                            "1", "--force"),
                            List.of("apply", "--file", sampleFile.toString(), "--input",
                                            batch.resolveSibling("delete.json").toString(), "--dry-run"));

        if (command.equals("restore"))
            return List.of(List.of("restore", "--file", sampleFile.toString(), "--backup",
                            batch.resolveSibling("original.xml").toString()));

        if (command.equals("allocation"))
            return List.of(List.of("allocation", "--file", sampleFile.toString(), "--taxonomy", "Classes", "--date",
                            "2026-10-01"));

        if (!command.equals("performance"))
            return List.of();

        var base = List.of("performance", "--file", sampleFile.toString(), "--from", "2019-12-31", "--to",
                        "2026-10-01");
        var portfolio = client.getPortfolios().get(0);
        return List.of(with(base, "--by", "instrument"),
                        with(base, "--investment-account", portfolio.getUUID(), "--with-reference-cash-account"),
                        with(base, "--investment-account", portfolio.getUUID(), "--by", "instrument"),
                        with(base, "--cash-account", portfolio.getReferenceAccount().getUUID()),
                        with(base, "--instrument", client.getSecurities().get(0).getUUID()),
                        // the second instrument is quiet
                        with(base, "--instrument", client.getSecurities().get(1).getUUID()));
    }

    private static List<String> with(List<String> base, String... more)
    {
        var all = new ArrayList<>(base);
        all.addAll(List.of(more));
        return all;
    }

    /** The response is checked against the command's declared schemas by {@link CliRun} itself. */
    private void assertConforms(CommandSpec command, List<String> arguments)
    {
        var run = CliRun.run(arguments.toArray(String[]::new));
        assertThat(command.name() + " exit code, stdout: " + run.stdout, run.exitCode, is(0));
    }

    @Test
    public void theErrorCodesInHelpAreExactlyTheOnesTheToolFailsWith() throws IOException
    {
        var call = Pattern.compile("\\b(?:usage|failure)\\(\\s*\"([a-z0-9-]+)\"");
        var thrown = new TreeSet<String>();
        try (var sources = Files.walk(SourceTree.cliBundle("src")))
        {
            for (var source : sources.filter(p -> p.toString().endsWith(".java")).toList())
            {
                var matcher = call.matcher(Files.readString(source));
                while (matcher.find())
                    thrown.add(matcher.group(1));
            }
        }
        assertThat(thrown.contains("file-version-unsupported"), is(true));

        var declared = new TreeSet<String>();
        ErrorSpec.ALL.forEach(e -> declared.add(e.code()));
        assertThat(declared, is(thrown));
    }
}
