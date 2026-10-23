package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class FormatTableTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CliRun run(Client client, String command, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of(command, "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.run(args.toArray(String[]::new));
    }

    private static Security instrument(Client client, String name)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        security.setCurrencyCode(null);
        return security;
    }

    @Test
    public void instrumentsRenderAsAnAlignedTable() throws IOException
    {
        var client = new Client();
        var acme = instrument(client, "ACME Corp");
        acme.setCurrencyCode("USD");
        acme.setIsin("US0000000001");
        acme.setWkn("A12345");
        acme.setTickerSymbol("ACME");
        acme.setNote("not shown");
        acme.setRetired(true);
        var bare = instrument(client, "Bare");
        bare.setIsin(null);
        bare.setWkn(null);
        bare.setTickerSymbol(null);

        var run = run(client, "instruments", "--format", "table");

        assertThat(run.exitCode, is(0));
        assertThat(run.stderr, is(""));
        assertThat(run.stdout, is(String.join("\n",
                        "UUID                                   Name        Currency   ISIN           WKN      Ticker   Retired",
                        "-------------------------------------------------------------------------------------------------------",
                        "%s   ACME Corp   USD        US0000000001   A12345   ACME     yes".formatted(acme.getUUID()),
                        "%s   Bare".formatted(bare.getUUID()), "")));
    }

    @Test
    public void investmentAccountsRenderAsATable() throws IOException
    {
        var client = new Client();
        var cash = new AccountBuilder().addTo(client);
        var depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Depot");
        depot.setNote("not shown");

        var run = run(client, "investment-accounts", "--format", "table");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, is(String.join("\n",
                        "UUID                                   Name    Reference cash account                 Retired",
                        "----------------------------------------------------------------------------------------------",
                        "%s   Depot   %s".formatted(depot.getUUID(), cash.getUUID()), "")));
    }

    @Test
    public void cashAccountsRenderAsATable() throws IOException
    {
        var client = new Client();
        var savings = new AccountBuilder().addTo(client);
        savings.setName("Savings");
        savings.setCurrencyCode("EUR");
        savings.setRetired(true);

        var run = run(client, "cash-accounts", "--format", "table");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, is(String.join("\n",
                        "UUID                                   Name      Currency   Retired",
                        "--------------------------------------------------------------------",
                        "%s   Savings   EUR        yes".formatted(savings.getUUID()), "")));
    }

    @Test
    public void anEmptyListPrintsTheHeaderAndSeparatorOnly() throws IOException
    {
        var run = run(new Client(), "instruments", "--format", "table");

        assertThat(run.exitCode, is(0));
        assertThat(run.stdout, is(String.join("\n", "UUID   Name   Currency   ISIN   WKN   Ticker   Retired",
                        "-------------------------------------------------------", "")));
    }

    @Test
    public void aLongNameIsCutAtFortyCharactersButAUuidNever() throws IOException
    {
        var client = new Client();
        var security = instrument(client, "A".repeat(50));
        security.setIsin(null);
        security.setWkn(null);
        security.setTickerSymbol(null);

        var run = run(client, "instruments", "--format", "table");

        assertThat(run.stdout.lines().skip(2).toList(),
                        is(List.of(security.getUUID() + "   " + "A".repeat(39) + "\u2026")));
    }

    @Test
    public void aLongValueInAnUncappedColumnStaysOnOneLine() throws IOException
    {
        var client = new Client();
        var security = instrument(client, "Long");
        security.setIsin("I".repeat(100));
        security.setWkn(null);
        security.setTickerSymbol(null);

        var run = run(client, "instruments", "--format", "table");

        assertThat(run.stdout.lines().count(), is(3L));
        assertThat(run.stdout.lines().skip(2).findFirst().orElseThrow().endsWith("I".repeat(100)), is(true));
    }

    @Test
    public void newlinesAndTabsInAValueBecomeASpace() throws IOException
    {
        var client = new Client();
        var security = instrument(client, "one\ntwo\r\nthree\tfour");
        security.setIsin(null);
        security.setWkn(null);
        security.setTickerSymbol(null);

        var run = run(client, "instruments", "--format", "table");

        assertThat(run.stdout.lines().skip(2).toList(),
                        is(List.of(security.getUUID() + "   one two three four")));
    }

    @Test
    public void aPartialPageAddsAFooterOnStderrAndKeepsStdoutATable() throws IOException
    {
        var client = new Client();
        for (var name : List.of("a", "b", "c"))
            instrument(client, name);

        var first = run(client, "instruments", "--format", "table", "--limit", "2");
        var last = run(client, "instruments", "--format", "table", "--limit", "2", "--offset", "2");
        var whole = run(client, "instruments", "--format", "table");

        assertThat(first.stdout.lines().count(), is(4L));
        assertThat(first.stderr.lines().toList(), is(List.of("showing 2 of 3; next page: --offset 2")));
        assertThat(last.stdout.lines().count(), is(3L));
        assertThat(last.stderr.lines().toList(), is(List.of("showing 1 of 3")));
        assertThat(whole.stderr, is(""));
    }

    @Test
    public void jsonStaysTheDefaultAndTheOnlyOtherChoiceIsExplicitJson() throws IOException
    {
        var client = new Client();
        instrument(client, "ACME");

        var implicit = run(client, "instruments");
        var explicit = run(client, "instruments", "--format", "json");

        assertThat(explicit.exitCode, is(0));
        assertThat(explicit.stdout, is(implicit.stdout));
        assertThat(implicit.json().getAsJsonObject().keySet(), is(Set.of("data", "meta", "warnings")));
    }

    @Test
    public void aCommandWithoutTableColumnsRefusesTheTableFormat() throws IOException
    {
        var run = run(new Client(), "holdings", "--format", "table");

        assertThat(run.exitCode, is(2));
        var problem = run.json().getAsJsonObject();
        assertThat(problem.get("code").getAsString(), is("invalid-option-value"));
        assertThat(problem.get("detail").getAsString(), is("Option '--format' must be one of json."));
    }

    @Test
    public void anUnknownFormatIsAUsageError() throws IOException
    {
        var run = run(new Client(), "instruments", "--format", "csv");

        assertThat(run.exitCode, is(2));
        var problem = run.json().getAsJsonObject();
        assertThat(problem.get("code").getAsString(), is("invalid-option-value"));
        assertThat(problem.get("detail").getAsString(), is("Option '--format' must be one of json, table."));
    }

    @Test
    public void errorsStayProblemJsonInTheTableFormat()
    {
        var run = CliRun.run("instruments", "--format", "table");

        assertThat(run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("file-required"));
    }
}
