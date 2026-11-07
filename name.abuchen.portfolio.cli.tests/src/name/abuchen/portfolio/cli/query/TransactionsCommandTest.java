package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.TransactionFixtures;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

/** Today is 2026-10-16 (see {@link CliRun#CLOCK}). Amounts of the fixtures are in minor units. */
public class TransactionsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private Client client;
    private Security acme;
    private Account cash;
    private Portfolio depot;

    @Before
    public void setUp()
    {
        client = new Client();
        acme = new SecurityBuilder("EUR").addTo(client);
        acme.setName("ACME");
        cash = new AccountBuilder("EUR").addTo(client);
        cash.setName("Cash");
        depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Depot");
    }

    private CliRun run(String... options) throws IOException
    {
        return cli.save(client).run("transactions", options);
    }

    private static List<String> ids(CliRun run)
    {
        var ids = new ArrayList<String>();
        run.items().forEach(i -> ids.add(i.getAsJsonObject().get("id").getAsString()));
        return ids;
    }

    private static String text(JsonObject json, String member)
    {
        return json.get(member).getAsString();
    }

    private static List<String> strings(JsonArray array)
    {
        var strings = new ArrayList<String>();
        array.forEach(e -> strings.add(e.getAsString()));
        return strings;
    }

    @Test
    public void recordsAreOrderedByDateTimeThenIdAndPaged() throws IOException
    {
        var late = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-01T15:00", 1);
        var early = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-01T08:00", 2);
        var older = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2025-12-31T23:59", 3);

        var tied = new ArrayList<Transaction>();
        for (var i = 0; i < 3; i++)
            tied.add(TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-02-01T10:00", 10 + i));
        var tiedIds = tied.stream().map(Transaction::getUUID).sorted().toList();

        var all = run();
        assertThat(all.stdout, all.exitCode, is(0));

        var expected = new ArrayList<String>(List.of(older.getUUID(), early.getUUID(), late.getUUID()));
        expected.addAll(tiedIds);
        assertThat(ids(all), is(expected));

        var firstPage = run("--limit", "2");
        assertThat(ids(firstPage), is(expected.subList(0, 2)));
        assertThat(firstPage.data().get("total").getAsInt(), is(6));
        assertThat(firstPage.data().get("nextOffset").getAsInt(), is(2));
        assertThat(firstPage.meta().get("limit").getAsInt(), is(2));
        assertThat(firstPage.meta().get("offset").getAsInt(), is(0));

        var lastPage = run("--limit", "2", "--offset", "4");
        assertThat(ids(lastPage), is(expected.subList(4, 6)));
        assertThat(lastPage.data().has("nextOffset"), is(false));

        var beyond = run("--offset", "99");
        assertThat(beyond.items().size(), is(0));
        assertThat(beyond.data().get("total").getAsInt(), is(6));
    }

    @Test
    public void pagingValuesAreValidatedLikeTheLists() throws IOException
    {
        var run = run("--limit", "0");

        assertThat(run.exitCode, is(2));
        var problem = run.problem();
        assertThat(text(problem, "code"), is("invalid-option-value"));
        assertThat(text(problem, "option"), is("limit"));
    }

    @Test
    public void theDateRangeIsInclusiveAndToDefaultsToToday() throws IOException
    {
        var before = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-09", 1);
        var first = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-10T00:00", 2);
        var last = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-20T23:59", 3);
        var after = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-21", 4);
        var today = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-10-16T18:00", 5);
        var future = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-10-17", 6);

        var ranged = run("--from", "2026-01-10", "--to", "2026-01-20");
        assertThat(ranged.stdout, ranged.exitCode, is(0));
        assertThat(ids(ranged), contains(first.getUUID(), last.getUUID()));
        var meta = ranged.meta();
        assertThat(text(meta, "from"), is("2026-01-10"));
        assertThat(text(meta, "to"), is("2026-01-20"));

        var defaulted = run();
        assertThat(ids(defaulted),
                        contains(before.getUUID(), first.getUUID(), last.getUUID(), after.getUUID(), today.getUUID()));
        var defaultedMeta = defaulted.meta();
        assertThat(text(defaultedMeta, "to"), is("2026-10-16"));
        assertThat(defaultedMeta.has("from"), is(false));

        var later = run("--to", "2026-12-31", "--from", "2026-10-16");
        assertThat(ids(later), contains(today.getUUID(), future.getUUID()));
    }

    @Test
    public void accountFiltersMatchARecordWithAnySideInTheAccount() throws IOException
    {
        var savings = new AccountBuilder("EUR").addTo(client);
        savings.setName("Savings");
        var other = new PortfolioBuilder(savings).addTo(client);
        other.setName("Other depot");

        var buy = TransactionFixtures.buy(depot, cash, acme, "2026-01-01", 1_00000000L, 10_000);
        var deposit = TransactionFixtures.cash(savings, AccountTransaction.Type.DEPOSIT, "2026-01-02", 500);
        var cashTransfer = TransactionFixtures.cashTransfer(cash, savings, "2026-01-03", 100);
        var securityTransfer = TransactionFixtures.securityTransfer(depot, other, acme, "2026-01-04", 1_00000000L,
                        100);

        var byCash = run("--cash-account", cash.getUUID());
        assertThat(ids(byCash), contains(buy.getPortfolioTransaction().getUUID(),
                        cashTransfer.getSourceTransaction().getUUID()));
        assertThat(text(byCash.meta(), "cashAccount"), is(cash.getUUID()));

        var bySavings = run("--cash-account", "Savings");
        assertThat(ids(bySavings), contains(deposit.getUUID(), cashTransfer.getSourceTransaction().getUUID()));

        var byDepot = run("--investment-account", depot.getUUID());
        assertThat(ids(byDepot), contains(buy.getPortfolioTransaction().getUUID(),
                        securityTransfer.getSourceTransaction().getUUID()));

        var byOther = run("--investment-account", "Other depot");
        assertThat(ids(byOther), contains(securityTransfer.getSourceTransaction().getUUID()));
        assertThat(text(byOther.meta(), "investmentAccount"),
                        is(other.getUUID()));

        // the two kinds don't mix up: a cash account UUID is not an investment account
        var wrongKind = run("--investment-account", cash.getUUID());
        assertThat(wrongKind.exitCode, is(1));
        assertThat(text(wrongKind.problem(), "code"), is("reference-not-found"));
    }

    @Test
    public void theInstrumentFilterMatchesEveryRecordOfTheInstrumentAndResolvesReferences() throws IOException
    {
        var other = new SecurityBuilder("EUR").addTo(client);
        other.setName("Other");
        other.setIsin("DE000OTHER00");
        other.setTickerSymbol(null);

        var buy = TransactionFixtures.buy(depot, cash, acme, "2026-01-01", 1_00000000L, 10_000);
        var dividend = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-01-02", 500);
        dividend.setSecurity(acme);
        var tax = TransactionFixtures.cash(cash, AccountTransaction.Type.TAXES, "2026-01-03", 50);
        tax.setSecurity(acme);
        var fee = TransactionFixtures.cash(cash, AccountTransaction.Type.FEES, "2026-01-04", 5);
        fee.setSecurity(other);
        TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-05", 5);

        var byUuid = run("--instrument", acme.getUUID());
        assertThat(ids(byUuid), contains(buy.getPortfolioTransaction().getUUID(), dividend.getUUID(), tax.getUUID()));
        assertThat(text(byUuid.meta(), "instrument"), is(acme.getUUID()));

        var byIsin = run("--instrument", "DE000OTHER00");
        assertThat(ids(byIsin), contains(fee.getUUID()));
        assertThat(text(byIsin.meta(), "instrument"), is(other.getUUID()));

        var unknown = run("--instrument", "nothing");
        assertThat(unknown.exitCode, is(1));
        assertThat(text(unknown.problem(), "code"), is("reference-not-found"));
        assertThat(text(unknown.problem(), "option"), is("instrument"));
    }

    @Test
    public void theTypeFilterIsACaseInsensitiveListOfAnyEmittedType() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, cash, acme, "2026-01-01", 1_00000000L, 10_000);
        var sell = TransactionFixtures.sell(depot, cash, acme, "2026-01-02", 1_00000000L, 10_000);
        var deposit = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-03", 5);
        var savings = new AccountBuilder("EUR").addTo(client);
        var orphan = TransactionFixtures.cashTransfer(cash, savings, "2026-01-04", 5);
        savings.shallowDeleteTransaction(orphan.getTargetTransaction(), client);

        var buysAndSells = run("--type", "buy,SELL,Buy");
        assertThat(ids(buysAndSells),
                        contains(buy.getPortfolioTransaction().getUUID(), sell.getPortfolioTransaction().getUUID()));
        assertThat(strings(buysAndSells.meta().getAsJsonArray("types")),
                        contains("BUY", "SELL"));

        var deposits = run("--type", "DEPOSIT");
        assertThat(ids(deposits), contains(deposit.getUUID()));

        var orphans = run("--type", "transfer_out");
        assertThat(ids(orphans), contains(orphan.getSourceTransaction().getUUID()));
        assertThat(orphans.data().get("total").getAsInt(), is(1));
    }

    @Test
    public void filtersCombineWithAnd() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, cash, acme, "2026-02-01", 1_00000000L, 10_000);
        TransactionFixtures.buy(depot, cash, acme, "2026-03-01", 1_00000000L, 10_000);
        var dividend = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-02-02", 500);
        dividend.setSecurity(acme);
        TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-02-03", 5);

        var run = run("--cash-account", "Cash", "--instrument", acme.getUUID(), "--type", "buy,dividends", "--to",
                        "2026-02-28");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(ids(run), contains(buy.getPortfolioTransaction().getUUID(), dividend.getUUID()));
        assertThat(run.data().get("total").getAsInt(), is(2));
    }
}
