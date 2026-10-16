package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Client;

public class InvestmentAccountsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CliRun run(Client client, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>();
        args.add("investment-accounts");
        args.add("--file");
        args.add(file.getPath());
        args.addAll(List.of(options));
        return CliRun.run(args.toArray(String[]::new));
    }

    private static JsonArray items(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("items");
    }

    @Test
    public void listsInvestmentAccountsWithTheDocumentedFields() throws IOException
    {
        var client = new Client();
        var cash = new AccountBuilder().addTo(client);
        var portfolio = new PortfolioBuilder(cash).addTo(client);
        portfolio.setName("Depot");
        portfolio.setNote("long term");
        portfolio.setRetired(true);

        var run = run(client);

        assertThat(run.exitCode, is(0));
        assertThat(run.json().getAsJsonObject().getAsJsonObject("data").get("total").getAsInt(), is(1));

        var item = items(run).get(0).getAsJsonObject();
        assertThat(item.keySet(), is(Set.of("uuid", "name", "referenceCashAccount", "note", "retired")));
        assertThat(item.get("uuid").getAsString(), is(portfolio.getUUID()));
        assertThat(item.get("name").getAsString(), is("Depot"));
        assertThat(item.get("referenceCashAccount").getAsString(), is(cash.getUUID()));
        assertThat(item.get("note").getAsString(), is("long term"));
        assertThat(item.get("retired").getAsBoolean(), is(true));
    }

    @Test
    public void unsetFieldsAreOmitted() throws IOException
    {
        var client = new Client();
        var portfolio = new PortfolioBuilder().addTo(client);
        portfolio.setName("Depot");
        portfolio.setNote("");
        portfolio.setReferenceAccount(null);

        var item = items(run(client)).get(0).getAsJsonObject();

        assertThat(item.keySet(), is(Set.of("uuid", "name", "retired")));
        assertThat(item.get("retired").getAsBoolean(), is(false));
    }

    @Test
    public void ordersByNameThenUuidAndPages() throws IOException
    {
        var client = new Client();
        var cash = new AccountBuilder().addTo(client);
        for (var name : new String[] { "b", "C", "a" })
            new PortfolioBuilder(cash).addTo(client).setName(name);

        var page = run(client, "--limit", "2");

        var data = page.json().getAsJsonObject().getAsJsonObject("data");
        var names = new ArrayList<String>();
        data.getAsJsonArray("items").forEach(i -> names.add(i.getAsJsonObject().get("name").getAsString()));
        assertThat(names, contains("a", "b"));
        assertThat(data.get("total").getAsInt(), is(3));
        assertThat(data.get("nextOffset").getAsInt(), is(2));
    }
}
