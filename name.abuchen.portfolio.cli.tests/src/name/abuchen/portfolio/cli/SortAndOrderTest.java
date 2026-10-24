package name.abuchen.portfolio.cli;

import static name.abuchen.portfolio.cli.TaxonomyFixtures.taxonomy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * <code>--sort</code>, <code>--order</code> and <code>updatedAt</code> of the
 * paged list commands, through the command line.
 */
public class SortAndOrderTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Instant JAN = Instant.parse("2026-01-10T08:00:00Z");
    private static final Instant FEB = Instant.parse("2026-02-10T08:30:15Z");
    private static final Instant MAR = Instant.parse("2026-03-10T09:00:00Z");

    private CliRun run(Client client, String command, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of(command, "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.run(args.toArray(String[]::new));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    private static List<String> values(CliRun run, String member)
    {
        var values = new ArrayList<String>();
        for (var item : data(run).getAsJsonArray("items"))
            values.add(item.getAsJsonObject().get(member).getAsString());
        return values;
    }

    private static Account cashAccount(Client client, String name, Instant updatedAt)
    {
        var account = new AccountBuilder().addTo(client);
        account.setName(name);
        account.setUpdatedAt(updatedAt);
        return account;
    }

    // --- updatedAt on master data

    @Test
    public void masterDataCarriesUpdatedAtAsAnIsoInstantAndLeavesItOutWhenCoreHasNone() throws IOException
    {
        var client = new Client();
        cashAccount(client, "dated", FEB);
        cashAccount(client, "undated", null);

        var items = data(run(client, "cash-accounts")).getAsJsonArray("items");

        assertThat(items.get(0).getAsJsonObject().get("updatedAt").getAsString(), is("2026-02-10T08:30:15Z"));
        assertThat(items.get(1).getAsJsonObject().has("updatedAt"), is(false));
    }

    @Test
    public void instrumentsAndInvestmentAccountsCarryUpdatedAt() throws IOException
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");
        security.setUpdatedAt(JAN);
        var cash = cashAccount(client, "Cash", null);
        var depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Depot");
        depot.setUpdatedAt(MAR);

        assertThat(values(run(client, "instruments"), "updatedAt"), contains("2026-01-10T08:00:00Z"));
        assertThat(values(run(client, "investment-accounts"), "updatedAt"), contains("2026-03-10T09:00:00Z"));
    }

    // --- sorting master data

    @Test
    public void masterDataSortsByUpdatedAtWithUndatedFirstInAscending() throws IOException
    {
        var client = new Client();
        cashAccount(client, "feb", FEB);
        cashAccount(client, "undated", null);
        cashAccount(client, "mar", MAR);
        cashAccount(client, "jan", JAN);

        assertThat(values(run(client, "cash-accounts", "--sort", "updatedAt"), "name"),
                        contains("undated", "jan", "feb", "mar"));
        assertThat(values(run(client, "cash-accounts", "--sort", "updatedAt", "--order", "desc"), "name"),
                        contains("mar", "feb", "jan", "undated"));
    }

    @Test
    public void masterDataSortsByNameInBothOrders() throws IOException
    {
        var client = new Client();
        cashAccount(client, "b", null);
        cashAccount(client, "C", null);
        cashAccount(client, "a", null);

        assertThat(values(run(client, "cash-accounts", "--sort", "name"), "name"), contains("a", "b", "C"));
        assertThat(values(run(client, "cash-accounts", "--order", "desc"), "name"), contains("C", "b", "a"));
    }

    @Test
    public void everyMasterDataCommandSortsByNameAndUpdatedAt() throws IOException
    {
        var client = new Client();
        var cash = cashAccount(client, "b", JAN);
        for (var name : List.of("a", "c"))
        {
            var security = new SecurityBuilder().addTo(client);
            security.setName(name);
            security.setUpdatedAt(name.equals("a") ? MAR : JAN);
            var depot = new PortfolioBuilder(cash).addTo(client);
            depot.setName(name);
            depot.setUpdatedAt(name.equals("a") ? MAR : JAN);
        }

        for (var command : List.of("instruments", "investment-accounts"))
        {
            assertThat(command, values(run(client, command, "--sort", "name", "--order", "desc"), "name"),
                            contains("c", "a"));
            assertThat(command, values(run(client, command, "--sort", "updatedAt", "--order", "desc"), "name"),
                            contains("a", "c"));
        }
    }

    @Test
    public void descendingReversesTheTieBreakerToo() throws IOException
    {
        var client = new Client();
        for (var i = 0; i < 4; i++)
            cashAccount(client, "same", FEB);

        var ascending = values(run(client, "cash-accounts", "--sort", "updatedAt"), "uuid");
        var descending = values(run(client, "cash-accounts", "--sort", "updatedAt", "--order", "desc"), "uuid");

        var sorted = new ArrayList<>(ascending);
        sorted.sort(null);
        assertThat(ascending, is(sorted));
        Collections.reverse(sorted);
        assertThat(descending, is(sorted));
    }

    @Test
    public void pagingIsStableUnderEverySortAndOrder() throws IOException
    {
        var client = new Client();
        cashAccount(client, "x", FEB);
        cashAccount(client, "x", FEB);
        cashAccount(client, "y", FEB);
        cashAccount(client, "y", null);
        cashAccount(client, "z", JAN);

        for (var sort : List.of("name", "updatedAt"))
        {
            for (var order : List.of("asc", "desc"))
            {
                var whole = values(run(client, "cash-accounts", "--sort", sort, "--order", order), "uuid");

                var paged = new ArrayList<String>();
                for (var offset = 0; offset < 5; offset += 2)
                    paged.addAll(values(run(client, "cash-accounts", "--sort", sort, "--order", order, "--limit", "2",
                                    "--offset", String.valueOf(offset)), "uuid"));

                assertThat(sort + " " + order, paged, is(whole));
                assertThat(sort + " " + order, whole.stream().distinct().count(), is(5L));
            }
        }
    }

    @Test
    public void metaEchoesTheResolvedSortAndOrder() throws IOException
    {
        var client = new Client();
        cashAccount(client, "a", null);

        var byDefault = meta(run(client, "cash-accounts"));
        assertThat(byDefault.get("sort").getAsString(), is("name"));
        assertThat(byDefault.get("order").getAsString(), is("asc"));

        var explicit = meta(run(client, "cash-accounts", "--sort", "updatedAt", "--order", "desc"));
        assertThat(explicit.get("sort").getAsString(), is("updatedAt"));
        assertThat(explicit.get("order").getAsString(), is("desc"));
    }

    @Test
    public void anUnknownSortFieldOrOrderIsAnInvalidOptionValueListingTheAllowedValues() throws IOException
    {
        var client = new Client();
        cashAccount(client, "a", null);

        var sort = run(client, "cash-accounts", "--sort", "date");
        assertThat(sort.exitCode, is(2));
        var problem = sort.json().getAsJsonObject();
        assertThat(problem.get("code").getAsString(), is("invalid-option-value"));
        assertThat(problem.get("option").getAsString(), is("sort"));
        assertThat(problem.get("value").getAsString(), is("date"));
        assertThat(problem.get("detail").getAsString(), containsString("name, updatedAt"));

        var order = run(client, "cash-accounts", "--order", "up");
        assertThat(order.exitCode, is(2));
        problem = order.json().getAsJsonObject();
        assertThat(problem.get("code").getAsString(), is("invalid-option-value"));
        assertThat(problem.get("option").getAsString(), is("order"));
        assertThat(problem.get("detail").getAsString(), containsString("asc, desc"));
    }

    // --- transactions

    private static AccountTransaction deposit(Account account, String date, Instant updatedAt)
    {
        var transaction = TransactionFixtures.cash(account, AccountTransaction.Type.DEPOSIT, date, 100_00);
        transaction.setUpdatedAt(updatedAt);
        return transaction;
    }

    @Test
    public void transactionsOrderDescendingReturnsTheLatestRecordsFirst() throws IOException
    {
        var client = new Client();
        var account = cashAccount(client, "Cash", null);
        deposit(account, "2026-01-05T10:00", JAN);
        deposit(account, "2026-03-05T10:00", JAN);
        deposit(account, "2026-02-05T10:00", JAN);

        var run = run(client, "transactions", "--order", "desc", "--limit", "2");

        assertThat(values(run, "date"), contains("2026-03-05", "2026-02-05"));
        assertThat(data(run).get("total").getAsInt(), is(3));
        assertThat(meta(run).get("sort").getAsString(), is("date"));
        assertThat(meta(run).get("order").getAsString(), is("desc"));
    }

    @Test
    public void aBuysUpdatedAtIsTheLaterOfItsTwoSides() throws IOException
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        var cash = cashAccount(client, "Cash", null);
        var depot = new PortfolioBuilder(cash).addTo(client);

        var buy = TransactionFixtures.buy(depot, cash, security, "2026-01-02T10:00", 1_00000000L, 100_00);
        buy.getPortfolioTransaction().setUpdatedAt(FEB);
        buy.getAccountTransaction().setUpdatedAt(MAR);
        var older = TransactionFixtures.buy(depot, cash, security, "2026-01-03T10:00", 1_00000000L, 100_00);
        older.getPortfolioTransaction().setUpdatedAt(JAN);
        older.getAccountTransaction().setUpdatedAt(JAN);

        var run = run(client, "transactions", "--sort", "updatedAt", "--order", "desc");

        var items = data(run).getAsJsonArray("items");
        assertThat(items.get(0).getAsJsonObject().get("id").getAsString(),
                        is(buy.getPortfolioTransaction().getUUID()));
        assertThat(items.get(0).getAsJsonObject().get("updatedAt").getAsString(), is("2026-03-10T09:00:00Z"));
        assertThat(items.get(1).getAsJsonObject().get("updatedAt").getAsString(), is("2026-01-10T08:00:00Z"));
    }

    @Test
    public void aTransferUsesTheLatestUpdatedAtOfBothSidesAndSortsUndatedFirst() throws IOException
    {
        var client = new Client();
        var from = cashAccount(client, "From", null);
        var to = cashAccount(client, "To", null);

        var transfer = TransactionFixtures.cashTransfer(from, to, "2026-01-02T10:00", 50_00);
        transfer.getSourceTransaction().setUpdatedAt(null);
        transfer.getTargetTransaction().setUpdatedAt(FEB);
        var undated = deposit(from, "2026-01-03T10:00", null);

        var run = run(client, "transactions", "--sort", "updatedAt");

        var items = data(run).getAsJsonArray("items");
        assertThat(items.get(0).getAsJsonObject().get("id").getAsString(), is(undated.getUUID()));
        assertThat(items.get(0).getAsJsonObject().has("updatedAt"), is(false));
        assertThat(items.get(1).getAsJsonObject().get("updatedAt").getAsString(), is("2026-02-10T08:30:15Z"));
    }

    @Test
    public void transactionPagingIsStableUnderEverySortAndOrder() throws IOException
    {
        var client = new Client();
        var account = cashAccount(client, "Cash", null);
        for (var i = 0; i < 5; i++)
            deposit(account, "2026-01-05T10:00", i < 3 ? FEB : JAN);

        for (var sort : List.of("date", "updatedAt"))
        {
            for (var order : List.of("asc", "desc"))
            {
                var whole = values(run(client, "transactions", "--sort", sort, "--order", order), "id");

                var paged = new ArrayList<String>();
                for (var offset = 0; offset < 5; offset += 2)
                    paged.addAll(values(run(client, "transactions", "--sort", sort, "--order", order, "--limit", "2",
                                    "--offset", String.valueOf(offset)), "id"));

                assertThat(sort + " " + order, paged, is(whole));
                assertThat(sort + " " + order, whole.stream().distinct().count(), is(5L));
            }
        }
    }

    @Test
    public void transactionsRejectAFieldTheyDoNotSortBy() throws IOException
    {
        var client = new Client();

        var run = run(client, "transactions", "--sort", "name");

        assertThat(run.exitCode, is(2));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("invalid-option-value"));
        assertThat(run.json().getAsJsonObject().get("detail").getAsString(), containsString("date, updatedAt"));
    }

    // --- prices and taxonomies

    private static Security instrumentWithPrices(Client client)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");
        security.setIsin(null);
        security.setTickerSymbol(null);
        for (var day : List.of("2026-01-02", "2026-01-03", "2026-01-04"))
            security.addPrice(new SecurityPrice(LocalDate.parse(day), 10_00000000L));
        return security;
    }

    @Test
    public void pricesSortByDateInBothOrders() throws IOException
    {
        var client = new Client();
        var security = instrumentWithPrices(client);

        var ascending = run(client, "prices", "--instrument", security.getUUID());
        var descending = run(client, "prices", "--instrument", security.getUUID(), "--order", "desc");

        assertThat(values(ascending, "date"), contains("2026-01-02", "2026-01-03", "2026-01-04"));
        assertThat(values(descending, "date"), contains("2026-01-04", "2026-01-03", "2026-01-02"));
        assertThat(meta(descending).get("sort").getAsString(), is("date"));
        assertThat(meta(descending).get("order").getAsString(), is("desc"));

        var invalid = run(client, "prices", "--instrument", security.getUUID(), "--sort", "value");
        assertThat(invalid.exitCode, is(2));
        assertThat(invalid.json().getAsJsonObject().get("detail").getAsString(), containsString("date"));
    }

    @Test
    public void taxonomiesKeepTheAppOrderByDefaultAndSortByName() throws IOException
    {
        var client = new Client();
        taxonomy(client, "Regions");
        taxonomy(client, "Asset classes");
        taxonomy(client, "Industries");

        Function<CliRun, List<String>> names = run -> values(run, "name");

        assertThat(names.apply(run(client, "taxonomies")), contains("Regions", "Asset classes", "Industries"));
        assertThat(meta(run(client, "taxonomies")).get("sort").getAsString(), is("position"));
        assertThat(names.apply(run(client, "taxonomies", "--order", "desc")),
                        contains("Industries", "Asset classes", "Regions"));
        assertThat(names.apply(run(client, "taxonomies", "--sort", "name")),
                        contains("Asset classes", "Industries", "Regions"));
        assertThat(names.apply(run(client, "taxonomies", "--sort", "name", "--order", "desc")),
                        contains("Regions", "Industries", "Asset classes"));

        var invalid = run(client, "taxonomies", "--sort", "updatedAt");
        assertThat(invalid.exitCode, is(2));
        assertThat(invalid.json().getAsJsonObject().get("code").getAsString(), is("invalid-option-value"));
    }

    @Test
    public void unpagedCommandsTakeNoSortOptions() throws IOException
    {
        var client = new Client();

        var run = run(client, "holdings", "--sort", "name");

        assertThat(run.exitCode, is(2));
    }
}
