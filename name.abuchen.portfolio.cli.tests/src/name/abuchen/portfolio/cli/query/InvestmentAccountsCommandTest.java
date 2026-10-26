package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Client;

public class InvestmentAccountsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private CliRun run(Client client, String... options) throws IOException
    {
        return cli.save(client).run("investment-accounts", options);
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
        assertThat(run.data().get("total").getAsInt(), is(1));

        var item = run.items().get(0).getAsJsonObject();
        assertThat(item.keySet(), is(Set.of("uuid", "name", "referenceCashAccount", "note", "retired", "updatedAt")));
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

        var item = run(client).items().get(0).getAsJsonObject();

        assertThat(item.keySet(), is(Set.of("uuid", "name", "retired", "updatedAt")));
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

        var data = page.data();
        var names = new ArrayList<String>();
        data.getAsJsonArray("items").forEach(i -> names.add(i.getAsJsonObject().get("name").getAsString()));
        assertThat(names, contains("a", "b"));
        assertThat(data.get("total").getAsInt(), is(3));
        assertThat(data.get("nextOffset").getAsInt(), is(2));
    }
}
