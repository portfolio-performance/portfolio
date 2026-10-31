package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.TransactionFixtures;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * End-to-end smoke runs of <code>transaction.update</code> and
 * <code>transaction.delete</code>; the rules are tested on
 * <code>TransactionRecords</code> itself.
 */
public class TransactionUpdateDeleteTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private Client client;

    private Account eur;
    private Account eurSavings;
    private Portfolio depot;
    private Security eurStock;

    @Before
    public void setUp() throws IOException
    {
        client = new Client();
        eur = cashAccount("EUR Cash", "EUR");
        eurSavings = cashAccount("EUR Savings", "EUR");
        depot = new Portfolio();
        depot.setName("Depot");
        depot.setReferenceAccount(eur);
        client.addPortfolio(depot);
        eurStock = new Security();
        eurStock.setName("EUR Stock");
        eurStock.setCurrencyCode("EUR");
        client.addSecurity(eurStock);
    }

    private Account cashAccount(String name, String currencyCode)
    {
        var account = new Account();
        account.setName(name);
        account.setCurrencyCode(currencyCode);
        client.addAccount(account);
        return account;
    }

    private static String update(String uuid, String members)
    {
        return "{\"operation\":\"transaction.update\",\"uuid\":\"" + uuid + "\"," + members + "}";
    }

    private static String delete(String uuid)
    {
        return "{\"operation\":\"transaction.delete\",\"uuid\":\"" + uuid + "\"}";
    }

    /** Shares as stored. */
    private static long shares(long count)
    {
        return count * 100_000_000L;
    }

    @Test
    public void updatesABuyByEitherSidesUuidAndKeepsBothSidesInStep() throws IOException
    {
        var entry = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        cli.save(client);
        var portfolioSide = entry.getPortfolioTransaction().getUUID();
        var accountSide = entry.getAccountTransaction().getUUID();

        var run = cli.apply(update(accountSide,
                        "\"shares\":8,\"amount\":820.00,\"date\":\"2026-03-03\",\"note\":\"corrected\""));

        assertThat(run.stdout, run.exitCode, is(0));
        var change = run.updated();
        assertThat(change.get("entity").getAsString(), is("transaction"));
        assertThat(change.get("uuid").getAsString(), is(portfolioSide));
        assertThat(change.getAsJsonObject("after").keySet(), is(Set.of("date", "amount", "shares", "note")));
        assertThat(change.getAsJsonObject("before").get("note").isJsonNull(), is(true));

        var reloaded = cli.reload();
        var position = reloaded.getPortfolios().get(0).getTransactions().get(0);
        var cash = reloaded.getAccounts().get(0).getTransactions().get(0);
        assertThat(position.getUUID(), is(portfolioSide));
        assertThat(cash.getUUID(), is(accountSide));
        for (var side : List.of(position, cash))
        {
            assertThat(side.getAmount(), is(82_000L));
            assertThat(side.getDateTime().toLocalDate().toString(), is("2026-03-03"));
            assertThat(side.getNote(), is("corrected"));
        }
        assertThat(position.getShares(), is(shares(8)));
    }

    @Test
    public void deletesAWholeRecordByEitherSidesUuid() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-10", 5_000);
        cli.save(client);

        var byAccountSide = cli.apply(delete(buy.getAccountTransaction().getUUID()));
        assertThat(byAccountSide.stdout, byAccountSide.exitCode, is(0));
        var change = byAccountSide.deleted();
        assertThat(change.get("entity").getAsString(), is("transaction"));
        assertThat(change.get("uuid").getAsString(), is(buy.getPortfolioTransaction().getUUID()));
        assertThat(change.getAsJsonObject("before").get("type").getAsString(), is("BUY"));
        assertThat(change.getAsJsonObject("after").size(), is(0));
        assertThat(cli.reload().getPortfolios().get(0).getTransactions().isEmpty(), is(true));

        var byTarget = cli.apply(delete(transfer.getTargetTransaction().getUUID()), delete(deposit.getUUID()));
        assertThat(byTarget.stdout, byTarget.exitCode, is(0));

        var reloaded = cli.reload();
        assertThat(reloaded.getAccounts().stream().mapToInt(a -> a.getTransactions().size()).sum(), is(0));
    }
}
