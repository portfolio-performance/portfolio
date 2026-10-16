package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public class CashAccountsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CliRun run(Client client, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new String[options.length + 3];
        args[0] = "cash-accounts";
        args[1] = "--file";
        args[2] = file.getPath();
        System.arraycopy(options, 0, args, 3, options.length);
        return CliRun.run(args);
    }

    static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    static JsonArray items(CliRun run)
    {
        return data(run).getAsJsonArray("items");
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

        var items = items(run);
        assertThat(items.size(), is(1));

        var item = items.get(0).getAsJsonObject();
        assertThat(item.keySet(), is(Set.of("uuid", "name", "currencyCode", "note", "retired")));
        assertThat(item.get("uuid").getAsString(), is(account.getUUID()));
        assertThat(item.get("name").getAsString(), is("Brokerage cash"));
        assertThat(item.get("currencyCode").getAsString(), is("USD"));
        assertThat(item.get("note").getAsString(), is("for dividends"));
        assertThat(item.get("retired").getAsBoolean(), is(true));

        var data = data(run);
        assertThat(data.keySet(), is(Set.of("items", "total")));
        assertThat(data.get("total").getAsInt(), is(1));

        var meta = run.json().getAsJsonObject().getAsJsonObject("meta");
        assertThat(meta.keySet(), is(Set.of("fileVersion", "asOf", "limit", "offset")));
        assertThat(meta.get("fileVersion").getAsInt(), is(Client.CURRENT_VERSION));
        assertThat(meta.get("asOf").getAsString(), is("2026-10-16"));
        assertThat(meta.get("limit").getAsInt(), is(200));
        assertThat(meta.get("offset").getAsInt(), is(0));
    }

    @Test
    public void unsetNoteIsOmittedAndRetiredIsAlwaysPresent() throws IOException
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);
        account.setNote("   ");

        var item = items(run(client)).get(0).getAsJsonObject();

        assertThat(item.keySet(), is(Set.of("uuid", "name", "currencyCode", "retired")));
        assertThat(item.get("retired").getAsBoolean(), is(false));
    }

    @Test
    public void nullNameIsEmittedAsEmptyString() throws IOException
    {
        var client = new Client();
        var account = new Account();
        client.addAccount(account);

        var item = items(run(client)).get(0).getAsJsonObject();

        assertThat(item.get("name").getAsString(), is(""));
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

        assertThat(items(run(client)).size(), is(1));
    }
}
