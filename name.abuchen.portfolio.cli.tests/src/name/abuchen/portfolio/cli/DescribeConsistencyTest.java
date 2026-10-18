package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * Every declared command and operation must show up in <code>describe</code>,
 * and every real response must conform to the schema that <code>describe</code>
 * publishes for it. A later ticket that adds a command gets a failure here
 * until it adds sample arguments and a correct schema.
 */
public class DescribeConsistencyTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Arguments that make the command succeed against a sample file. */
    private static List<String> sampleArguments(String command, Path sampleFile, Security instrument)
    {
        return switch (command)
        {
            case "info" -> List.of("info", "--file", sampleFile.toString());
            case "describe" -> List.of("describe");
            case "instruments", "investment-accounts", "cash-accounts" ->
                List.of(command, "--file", sampleFile.toString());
            case "prices" -> List.of("prices", "--file", sampleFile.toString(), "--instrument", instrument.getUUID());
            case "transactions" -> List.of("transactions", "--file", sampleFile.toString(), "--currency", "USD");
            case "holdings" -> List.of("holdings", "--file", sampleFile.toString(), "--date", "2026-10-01",
                            "--currency", "USD", "--include-zero");
            case "performance" -> List.of("performance", "--file", sampleFile.toString(), "--from", "2019-12-31",
                            "--to", "2026-10-01", "--currency", "USD", "--cost-method", "moving-average");
            default -> throw new AssertionError("Add sample arguments for command '" + command
                            + "' to DescribeConsistencyTest.sampleArguments");
        };
    }

    private JsonObject describe()
    {
        var run = CliRun.run("describe");
        assertThat(run.exitCode, is(0));
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    @Test
    public void describeListsExactlyTheDeclaredCommandsAndOperations()
    {
        var data = describe();

        var described = new TreeSet<String>();
        data.getAsJsonArray("commands").forEach(e -> described.add(e.getAsJsonObject().get("name").getAsString()));
        var declared = new TreeSet<String>();
        Commands.ALL.forEach(c -> declared.add(c.name()));
        assertThat(described, is(declared));

        var describedOperations = new TreeSet<String>();
        data.getAsJsonArray("operations")
                        .forEach(e -> describedOperations.add(e.getAsJsonObject().get("name").getAsString()));
        var declaredOperations = new TreeSet<String>();
        Commands.OPERATIONS.forEach(o -> declaredOperations.add(o.name()));
        assertThat(describedOperations, is(declaredOperations));
    }

    @Test
    public void everyCommandsRealResponseConformsToItsDeclaredSchemas() throws IOException
    {
        // a stale price, so that prices emits a warning to check as well
        var client = ClientFiles.sampleClient();
        var instrument = client.getSecurities().get(0);
        instrument.addPrice(new SecurityPrice(LocalDate.parse("2026-01-02"), 100_000_000L));
        var sampleFile = ClientFiles.xml(folder.getRoot().toPath(), client).toPath();

        for (var element : describe().getAsJsonArray("commands"))
        {
            var command = element.getAsJsonObject();
            var name = command.get("name").getAsString();

            assertConforms(command, sampleArguments(name, sampleFile, instrument));
            for (var arguments : furtherArguments(name, sampleFile, client))
                assertConforms(command, arguments);
        }
    }

    /** More ways to call a command whose response differs in shape. */
    private static List<List<String>> furtherArguments(String command, Path sampleFile, Client client)
    {
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

    private void assertConforms(JsonObject command, List<String> arguments)
    {
        var name = command.get("name").getAsString();

        var run = CliRun.run(arguments.toArray(String[]::new));
        assertThat(name + " exit code, stdout: " + run.stdout, run.exitCode, is(0));

        var envelope = run.json().getAsJsonObject();
        assertThat(name + " envelope", envelope.keySet(), is(Set.of("data", "meta", "warnings")));

        var response = command.getAsJsonObject("response");
        assertThat(name + " data", SchemaValidator.validate(envelope.get("data"), response.getAsJsonObject("data")),
                        is(empty()));
        assertThat(name + " meta", SchemaValidator.validate(envelope.get("meta"), response.getAsJsonObject("meta")),
                        is(empty()));

        var declaredWarnings = new TreeSet<String>();
        command.getAsJsonArray("warnings")
                        .forEach(w -> declaredWarnings.add(w.getAsJsonObject().get("code").getAsString()));
        for (var warning : envelope.getAsJsonArray("warnings"))
        {
            var object = warning.getAsJsonObject();
            assertThat(name + " warning members", object.has("code") && object.has("detail"), is(true));
            assertThat(name + " warning " + object, declaredWarnings.contains(object.get("code").getAsString()),
                            is(true));
        }
    }

    @Test
    public void everyCommandsDeclaredOptionsAreDescribed()
    {
        var described = describe();

        for (var command : Commands.ALL)
        {
            var options = new TreeSet<String>();
            for (var spec : described.getAsJsonArray("commands"))
            {
                if (spec.getAsJsonObject().get("name").getAsString().equals(command.name()))
                    spec.getAsJsonObject().getAsJsonArray("options")
                                    .forEach(o -> options.add(o.getAsJsonObject().get("name").getAsString()));
            }

            var declared = new TreeSet<String>();
            command.options().forEach(o -> declared.add(o.name()));
            assertThat(command.name(), options, is(declared));
        }
    }
}
