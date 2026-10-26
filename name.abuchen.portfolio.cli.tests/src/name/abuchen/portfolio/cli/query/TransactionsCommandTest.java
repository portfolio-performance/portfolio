package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

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

    private static JsonObject item(CliRun run, int index)
    {
        return run.items().get(index).getAsJsonObject();
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

    private static String decimal(JsonObject json, String member)
    {
        return json.get(member).getAsBigDecimal().toPlainString();
    }

    private static List<String> strings(JsonArray array)
    {
        var strings = new ArrayList<String>();
        array.forEach(e -> strings.add(e.getAsString()));
        return strings;
    }

    @Test
    public void aBuyIsOneRecordJoiningBothSidesWithTheInvestmentAccountSideAsId() throws IOException
    {
        var entry = TransactionFixtures.buy(depot, cash, acme, "2026-03-02T09:30", 10_00000000L, 100_500);
        entry.getPortfolioTransaction().addUnit(
                        new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of("EUR", 500)));
        entry.getPortfolioTransaction().setNote("first buy");

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.data().get("total").getAsInt(), is(1));

        var buy = item(run, 0);
        assertThat(text(buy, "id"), is(entry.getPortfolioTransaction().getUUID()));
        assertThat(text(buy, "type"), is("BUY"));
        assertThat(text(buy, "date"), is("2026-03-02"));
        assertThat(text(buy, "time"), is("09:30"));
        assertThat(strings(buy.getAsJsonArray("sides")), contains(entry.getPortfolioTransaction().getUUID(),
                        entry.getAccountTransaction().getUUID()));
        assertThat(decimal(buy, "amount"), is("1005.00"));
        assertThat(text(buy, "currencyCode"), is("EUR"));
        assertThat(text(buy, "instrument"), is(acme.getUUID()));
        assertThat(decimal(buy, "shares"), is("10.00000000"));
        assertThat(text(buy, "investmentAccount"), is(depot.getUUID()));
        assertThat(text(buy, "cashAccount"), is(cash.getUUID()));
        assertThat(text(buy, "note"), is("first buy"));
        assertThat(buy.has("kind"), is(false));
        assertThat(buy.has("from"), is(false));
        assertThat(buy.has("converted"), is(false));

        var unit = buy.getAsJsonArray("units").get(0).getAsJsonObject();
        assertThat(unit.keySet(), is(Set.of("type", "amount", "currencyCode")));
        assertThat(text(unit, "type"), is("FEE"));
        assertThat(decimal(unit, "amount"), is("5.00"));
        assertThat(text(unit, "currencyCode"), is("EUR"));
    }

    @Test
    public void aBuyNamesItsInstrumentAndBothAccounts() throws IOException
    {
        TransactionFixtures.buy(depot, cash, acme, "2026-03-02", 10_00000000L, 100_500);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var buy = item(run, 0);
        assertThat(text(buy, "instrumentName"), is("ACME"));
        assertThat(text(buy, "investmentAccountName"), is("Depot"));
        assertThat(text(buy, "cashAccountName"), is("Cash"));
        assertThat(buy.has("fromName"), is(false));
        assertThat(buy.has("toName"), is(false));
    }

    @Test
    public void transfersNameTheAccountsTheyLeaveAndEnter() throws IOException
    {
        var savings = new AccountBuilder("EUR").addTo(client);
        savings.setName("Savings");
        var other = new PortfolioBuilder(cash).addTo(client);
        other.setName("Other depot");
        TransactionFixtures.cashTransfer(cash, savings, "2026-02-01", 50_000);
        TransactionFixtures.securityTransfer(depot, other, acme, "2026-02-02", 3_00000000L, 30_000);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var cashTransfer = item(run, 0);
        assertThat(text(cashTransfer, "fromName"), is("Cash"));
        assertThat(text(cashTransfer, "toName"), is("Savings"));
        assertThat(cashTransfer.has("instrumentName"), is(false));
        assertThat(cashTransfer.has("cashAccountName"), is(false));

        var securityTransfer = item(run, 1);
        assertThat(text(securityTransfer, "fromName"), is("Depot"));
        assertThat(text(securityTransfer, "toName"), is("Other depot"));
        assertThat(text(securityTransfer, "instrumentName"), is("ACME"));
        assertThat(securityTransfer.has("investmentAccountName"), is(false));
    }

    @Test
    public void aDividendNamesItsInstrumentAndCashAccount() throws IOException
    {
        var dividend = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-05-01", 4_000);
        dividend.setSecurity(acme);
        TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-06-01", 1_000);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var first = item(run, 0);
        assertThat(text(first, "instrumentName"), is("ACME"));
        assertThat(text(first, "cashAccountName"), is("Cash"));
        assertThat(first.has("investmentAccountName"), is(false));

        var deposit = item(run, 1);
        assertThat(deposit.has("instrumentName"), is(false));
        assertThat(text(deposit, "cashAccountName"), is("Cash"));
    }

    @Test
    public void anUnnamedEntityHasAnEmptyName() throws IOException
    {
        acme.setName(null);
        cash.setName(null);
        depot.setName(null);
        TransactionFixtures.buy(depot, cash, acme, "2026-03-02", 10_00000000L, 100_500);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var buy = item(run, 0);
        assertThat(text(buy, "instrumentName"), is(""));
        assertThat(text(buy, "investmentAccountName"), is(""));
        assertThat(text(buy, "cashAccountName"), is(""));
    }

    @Test
    public void aSellAndDeliveriesAreRecordsWithTheRightAccounts() throws IOException
    {
        var sell = TransactionFixtures.sell(depot, cash, acme, "2026-03-03", 5_00000000L, 60_000);
        var inbound = new PortfolioTransaction(TransactionFixtures.at("2026-03-04"), "EUR", 10_000, acme, 1_00000000L,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0);
        depot.addTransaction(inbound);
        var outbound = new PortfolioTransaction(TransactionFixtures.at("2026-03-05"), "EUR", 10_000, acme, 1_00000000L,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, 0, 0);
        depot.addTransaction(outbound);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.items().size(), is(3));

        assertThat(text(item(run, 0), "type"), is("SELL"));
        assertThat(text(item(run, 0), "id"), is(sell.getPortfolioTransaction().getUUID()));
        assertThat(item(run, 0).getAsJsonArray("sides").size(), is(2));
        assertThat(item(run, 0).get("cashAccount").getAsString(), is(cash.getUUID()));

        for (var index : List.of(1, 2))
        {
            var delivery = item(run, index);
            assertThat(delivery.getAsJsonArray("sides").size(), is(1));
            assertThat(text(delivery, "investmentAccount"), is(depot.getUUID()));
            assertThat(delivery.has("cashAccount"), is(false));
            assertThat(decimal(delivery, "shares"), is("1.00000000"));
            assertThat(text(delivery, "instrument"), is(acme.getUUID()));
        }
        assertThat(text(item(run, 1), "type"), is("DELIVERY_INBOUND"));
        assertThat(text(item(run, 1), "id"), is(inbound.getUUID()));
        assertThat(text(item(run, 2), "type"), is("DELIVERY_OUTBOUND"));
    }

    @Test
    public void everyCashTypeIsTheTransactionItselfOnTheCashAccount() throws IOException
    {
        var types = List.of(AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL,
                        AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE,
                        AccountTransaction.Type.FEES, AccountTransaction.Type.FEES_REFUND,
                        AccountTransaction.Type.TAXES, AccountTransaction.Type.TAX_REFUND);

        var uuids = new ArrayList<String>();
        for (var i = 0; i < types.size(); i++)
            uuids.add(TransactionFixtures.cash(cash, types.get(i), "2026-01-0" + (i + 1), 1_000 + i).getUUID());

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.items().size(), is(types.size()));
        for (var i = 0; i < types.size(); i++)
        {
            var record = item(run, i);
            assertThat(text(record, "type"), is(types.get(i).name()));
            assertThat(text(record, "id"), is(uuids.get(i)));
            assertThat(strings(record.getAsJsonArray("sides")), contains(uuids.get(i)));
            assertThat(text(record, "cashAccount"), is(cash.getUUID()));
            assertThat(record.has("investmentAccount"), is(false));
            assertThat(record.has("instrument"), is(false));
            assertThat(record.has("shares"), is(false));
            assertThat(record.has("time"), is(false));
            assertThat(record.getAsJsonArray("units").size(), is(0));
        }
    }

    @Test
    public void aDividendCarriesItsInstrumentExDateSharesAndTaxes() throws IOException
    {
        var dividend = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-05-01", 4_000);
        dividend.setSecurity(acme);
        dividend.setShares(10_00000000L);
        dividend.setExDate(TransactionFixtures.at("2026-04-28"));
        dividend.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of("EUR", 1_000)));

        var withoutShares = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-06-01", 2_000);
        withoutShares.setSecurity(acme);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var first = item(run, 0);
        assertThat(text(first, "type"), is("DIVIDENDS"));
        assertThat(text(first, "instrument"), is(acme.getUUID()));
        assertThat(decimal(first, "shares"), is("10.00000000"));
        assertThat(text(first, "exDate"), is("2026-04-28"));
        assertThat(text(first.getAsJsonArray("units").get(0).getAsJsonObject(), "type"), is("TAX"));

        var second = item(run, 1);
        assertThat(second.has("shares"), is(false));
        assertThat(second.has("exDate"), is(false));
    }

    @Test
    public void aCashTransferIsOneRecordFromTheOutgoingSideNeverCountedTwice() throws IOException
    {
        var savings = new AccountBuilder("EUR").addTo(client);
        savings.setName("Savings");
        var entry = TransactionFixtures.cashTransfer(cash, savings, "2026-02-01T08:15", 50_000);
        entry.setNote("move");

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.data().get("total").getAsInt(), is(1));

        var transfer = item(run, 0);
        assertThat(text(transfer, "type"), is("TRANSFER"));
        assertThat(text(transfer, "kind"), is("cash"));
        assertThat(text(transfer, "id"), is(entry.getSourceTransaction().getUUID()));
        assertThat(strings(transfer.getAsJsonArray("sides")),
                        contains(entry.getSourceTransaction().getUUID(), entry.getTargetTransaction().getUUID()));
        assertThat(text(transfer, "from"), is(cash.getUUID()));
        assertThat(text(transfer, "to"), is(savings.getUUID()));
        assertThat(decimal(transfer, "amount"), is("500.00"));
        assertThat(text(transfer, "note"), is("move"));
        assertThat(transfer.has("cashAccount"), is(false));
        assertThat(transfer.has("targetAmount"), is(false));
        assertThat(transfer.has("targetCurrencyCode"), is(false));
        assertThat(transfer.has("exchangeRate"), is(false));
    }

    @Test
    public void aCrossCurrencyCashTransferShowsTheTargetAndTheRateOnceNotAsAUnit() throws IOException
    {
        var dollars = new AccountBuilder("USD").addTo(client);
        dollars.setName("Dollars");
        TransactionFixtures.crossCurrencyTransfer(cash, dollars, "2026-02-01", 100_000, 108_500, "0.9216589862");

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var transfer = item(run, 0);
        assertThat(text(transfer, "currencyCode"), is("EUR"));
        assertThat(decimal(transfer, "amount"), is("1000.00"));
        assertThat(decimal(transfer, "targetAmount"), is("1085.00"));
        assertThat(text(transfer, "targetCurrencyCode"), is("USD"));
        assertThat(decimal(transfer, "exchangeRate"), is("0.9216589862"));
        assertThat(transfer.getAsJsonArray("units").size(), is(0));
    }

    @Test
    public void aSecurityTransferIsOneRecordBetweenInvestmentAccounts() throws IOException
    {
        var other = new PortfolioBuilder(cash).addTo(client);
        other.setName("Other depot");
        var entry = TransactionFixtures.securityTransfer(depot, other, acme, "2026-02-02", 3_00000000L, 30_000);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.data().get("total").getAsInt(), is(1));

        var transfer = item(run, 0);
        assertThat(text(transfer, "type"), is("TRANSFER"));
        assertThat(text(transfer, "kind"), is("security"));
        assertThat(text(transfer, "id"), is(entry.getSourceTransaction().getUUID()));
        assertThat(transfer.getAsJsonArray("sides").size(), is(2));
        assertThat(text(transfer, "from"), is(depot.getUUID()));
        assertThat(text(transfer, "to"), is(other.getUUID()));
        assertThat(text(transfer, "instrument"), is(acme.getUUID()));
        assertThat(decimal(transfer, "shares"), is("3.00000000"));
        assertThat(transfer.has("investmentAccount"), is(false));
    }

    @Test
    public void aForexGrossValueUnitKeepsTheShapeThatWritesAccept() throws IOException
    {
        var usd = new SecurityBuilder("USD").addTo(client);
        var entry = TransactionFixtures.buy(depot, cash, usd, "2026-03-02", 10_00000000L, 100_000);
        entry.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("EUR", 100_000), Money.of("USD", 108_500), new BigDecimal("0.9216589862")));

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        var unit = item(run, 0).getAsJsonArray("units").get(0).getAsJsonObject();
        assertThat(unit.keySet(), is(Set.of("type", "amount", "currencyCode", "forexAmount", "forexCurrencyCode",
                        "exchangeRate")));
        assertThat(text(unit, "type"), is("GROSS_VALUE"));
        assertThat(decimal(unit, "amount"), is("1000.00"));
        assertThat(decimal(unit, "forexAmount"), is("1085.00"));
        assertThat(text(unit, "forexCurrencyCode"), is("USD"));
        assertThat(decimal(unit, "exchangeRate"), is("0.9216589862"));
    }

    @Test
    public void aSideWhosePartnerIsMissingIsItsOwnRecordUnderItsRawType() throws IOException
    {
        var withoutCash = TransactionFixtures.buy(depot, cash, acme, "2026-01-01", 1_00000000L, 10_000);
        cash.shallowDeleteTransaction(withoutCash.getAccountTransaction(), client);

        var withoutPortfolio = TransactionFixtures.sell(depot, cash, acme, "2026-01-02", 1_00000000L, 10_000);
        depot.shallowDeleteTransaction(withoutPortfolio.getPortfolioTransaction(), client);

        var other = new PortfolioBuilder(cash).addTo(client);
        var transfer = TransactionFixtures.securityTransfer(depot, other, acme, "2026-01-03", 1_00000000L, 10_000);
        other.shallowDeleteTransaction(transfer.getTargetTransaction(), client);

        var savings = new AccountBuilder("EUR").addTo(client);
        var cashTransfer = TransactionFixtures.cashTransfer(cash, savings, "2026-01-04", 10_000);
        cash.shallowDeleteTransaction(cashTransfer.getSourceTransaction(), client);

        var run = run();

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.items().size(), is(4));

        var buy = item(run, 0);
        assertThat(text(buy, "type"), is("BUY"));
        assertThat(text(buy, "id"), is(withoutCash.getPortfolioTransaction().getUUID()));
        assertThat(buy.getAsJsonArray("sides").size(), is(1));
        assertThat(text(buy, "investmentAccount"), is(depot.getUUID()));
        assertThat(buy.has("cashAccount"), is(false));

        var sell = item(run, 1);
        assertThat(text(sell, "type"), is("SELL"));
        assertThat(text(sell, "id"), is(withoutPortfolio.getAccountTransaction().getUUID()));
        assertThat(text(sell, "cashAccount"), is(cash.getUUID()));
        assertThat(sell.has("investmentAccount"), is(false));

        var out = item(run, 2);
        assertThat(text(out, "type"), is("TRANSFER_OUT"));
        assertThat(out.has("kind"), is(false));
        assertThat(out.has("from"), is(false));
        assertThat(text(out, "investmentAccount"), is(depot.getUUID()));

        var in = item(run, 3);
        assertThat(text(in, "type"), is("TRANSFER_IN"));
        assertThat(text(in, "id"), is(cashTransfer.getTargetTransaction().getUUID()));
        assertThat(text(in, "cashAccount"), is(savings.getUUID()));
        assertThat(run.warnings().size(), is(0));
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
    public void aFromAfterToIsAnInvalidDateRange() throws IOException
    {
        var run = run("--from", "2026-02-01", "--to", "2026-01-01");

        assertThat(run.exitCode, is(2));
        var problem = run.problem();
        assertThat(text(problem, "code"), is("invalid-date-range"));
        assertThat(text(problem, "from"), is("2026-02-01"));
        assertThat(text(problem, "to"), is("2026-01-01"));
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
    public void anUnknownOrEmptyTypeIsAUsageError() throws IOException
    {
        for (var value : List.of("BUY,NOPE", "", "BUY,", ","))
        {
            var run = run("--type", value);

            assertThat(value, run.exitCode, is(2));
            var problem = run.problem();
            assertThat(text(problem, "code"), is("invalid-option-value"));
            assertThat(text(problem, "option"), is("type"));
            assertThat(text(problem, "value"), is(value));
        }
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
