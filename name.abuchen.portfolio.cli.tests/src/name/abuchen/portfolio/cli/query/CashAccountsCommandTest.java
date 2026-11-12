package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public class CashAccountsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private CliRun run(Client client, String... options) throws IOException
    {
        return cli.save(client).run("cash-accounts", options);
    }

    @Test
    public void listsCashAccountsWithTheDocumentedFields() throws IOException
    {
        var client = new Client();
        var account = new AccountBuilder("USD").addTo(client);
        account.setName("Brokerage cash");
        account.setNote("for dividends");
        account.setRetired(true);

        var run = run(client);

        assertThat(run.exitCode, is(0));

        var items = run.items();
        assertThat(items.size(), is(1));

        var item = items.get(0).getAsJsonObject();
        assertThat(item.keySet(), is(Set.of("uuid", "name", "currencyCode", "note", "retired", "updatedAt")));
        assertThat(item.get("uuid").getAsString(), is(account.getUUID()));
        assertThat(item.get("name").getAsString(), is("Brokerage cash"));
        assertThat(item.get("currencyCode").getAsString(), is("USD"));
        assertThat(item.get("note").getAsString(), is("for dividends"));
        assertThat(item.get("retired").getAsBoolean(), is(true));

        var data = run.data();
        assertThat(data.keySet(), is(Set.of("items", "total")));
        assertThat(data.get("total").getAsInt(), is(1));

        var meta = run.meta();
        assertThat(meta.keySet(), is(Set.of("fileVersion", "asOf", "limit", "offset", "sort", "order")));
        assertThat(meta.get("fileVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("asOf").getAsString(), is("2026-10-16"));
        assertThat(meta.get("limit").getAsInt(), is(200));
        assertThat(meta.get("offset").getAsInt(), is(0));
    }

    @Test
    public void portfoliosAreNotListed() throws IOException
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);
        var portfolio = new Portfolio();
        portfolio.setName("securities");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        assertThat(run(client).items().size(), is(1));
    }
}
