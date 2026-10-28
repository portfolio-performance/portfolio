package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.TransactionFixtures;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

/** The <code>transaction.update</code> and <code>transaction.delete</code> operations. */
public class TransactionUpdateDeleteTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private Client client;

    private Account eur;
    private Account eurSavings;
    private Account usd;
    private Portfolio depot;
    private Portfolio otherDepot;
    private Security eurStock;
    private Security usdStock;

    @Before
    public void setUp() throws IOException
    {
        client = new Client();
        eur = cashAccount("EUR Cash", "EUR");
        usd = cashAccount("USD Cash", "USD");
        eurSavings = cashAccount("EUR Savings", "EUR");
        depot = investmentAccount("Depot", eur);
        otherDepot = investmentAccount("Other depot", eur);
        eurStock = instrument("EUR Stock", "EUR");
        usdStock = instrument("USD Stock", "USD");
    }

    private Account cashAccount(String name, String currencyCode)
    {
        var account = new Account();
        account.setName(name);
        account.setCurrencyCode(currencyCode);
        client.addAccount(account);
        return account;
    }

    private Portfolio investmentAccount(String name, Account reference)
    {
        var portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setReferenceAccount(reference);
        client.addPortfolio(portfolio);
        return portfolio;
    }

    private Security instrument(String name, String currencyCode)
    {
        var security = new Security();
        security.setName(name);
        security.setCurrencyCode(currencyCode);
        client.addSecurity(security);
        return security;
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
    public void updatesAFieldOfACashRecordByItsUuid() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        cli.save(client);

        var run = cli.apply(update(deposit.getUUID(), "\"amount\":250.00,\"note\":\"top-up\""));

        assertThat(run.stdout, run.exitCode, is(0));

        var change = run.updated();
        assertThat(change.get("entity").getAsString(), is("transaction"));
        assertThat(change.get("uuid").getAsString(), is(deposit.getUUID()));
        var before = change.getAsJsonObject("before");
        var after = change.getAsJsonObject("after");
        assertThat(before.keySet(), is(after.keySet()));
        assertThat(after.keySet().contains("date"), is(false));
        assertThat(before.get("amount").getAsBigDecimal().doubleValue(), is(1000.00));
        assertThat(after.get("amount").getAsBigDecimal().doubleValue(), is(250.00));
        assertThat(before.get("note").isJsonNull(), is(true));
        assertThat(after.get("note").getAsString(), is("top-up"));

        var transactions = cli.reload().getAccounts().get(0).getTransactions();
        assertThat(transactions.size(), is(1));
        assertThat(transactions.get(0).getUUID(), is(deposit.getUUID()));
        assertThat(transactions.get(0).getAmount(), is(25_000L));
        assertThat(transactions.get(0).getNote(), is("top-up"));
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
        assertThat(change.get("uuid").getAsString(), is(portfolioSide));
        assertThat(change.getAsJsonObject("after").keySet(),
                        is(java.util.Set.of("date", "amount", "shares", "note")));

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

        var byPortfolioSide = cli.apply(update(portfolioSide, "\"amount\":830.00"));
        assertThat(byPortfolioSide.stdout, byPortfolioSide.exitCode, is(0));
        assertThat(cli.reload().getAccounts().get(0).getTransactions().get(0).getAmount(), is(83_000L));
    }

    @Test
    public void movesTheSidesOfASellToOtherAccounts() throws IOException
    {
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(4), 42_000);
        cli.save(client);

        var run = cli.apply(update(sell.getPortfolioTransaction().getUUID(),
                        "\"investmentAccount\":\"" + otherDepot.getUUID() + "\",\"cashAccount\":\""
                                        + eurSavings.getUUID() + "\""));

        // the other depot holds no shares to sell
        run.assertRefused("shares", "negative-holding");

        var moved = cli.apply(update(sell.getAccountTransaction().getUUID(),
                        "\"cashAccount\":\"" + eurSavings.getUUID() + "\""));

        assertThat(moved.stdout, moved.exitCode, is(0));
        var reloaded = cli.reload();
        assertThat(reloaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(reloaded.getAccounts().get(2).getTransactions().size(), is(1));
        var position = reloaded.getPortfolios().get(0).getTransactions().get(1);
        var cash = reloaded.getAccounts().get(2).getTransactions().get(0);
        assertThat(cash.getUUID(), is(sell.getAccountTransaction().getUUID()));
        assertThat(position.getCrossEntry().getCrossOwner(position).getUUID(), is(eurSavings.getUUID()));
        assertThat(cash.getCrossEntry().getCrossOwner(cash).getUUID(), is(depot.getUUID()));
    }

    @Test
    public void updatesASecurityTransferByTheUuidOfItsIncomingSide() throws IOException
    {
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        var transfer = TransactionFixtures.securityTransfer(depot, otherDepot, eurStock, "2026-03-09", shares(6),
                        62_000);
        cli.save(client);

        var run = cli.apply(update(transfer.getTargetTransaction().getUUID(), "\"shares\":5,\"amount\":520.00"));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.updated().get("uuid").getAsString(), is(transfer.getSourceTransaction().getUUID()));
        var reloaded = cli.reload();
        var outgoing = reloaded.getPortfolios().get(0).getTransactions().get(1);
        var incoming = reloaded.getPortfolios().get(1).getTransactions().get(0);
        assertThat(outgoing.getShares(), is(shares(5)));
        assertThat(incoming.getShares(), is(shares(5)));
        assertThat(outgoing.getAmount(), is(52_000L));
        assertThat(incoming.getAmount(), is(52_000L));
    }

    @Test
    public void swapsTheAccountsOfACashTransfer() throws IOException
    {
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);
        cli.save(client);

        var run = cli.apply(update(transfer.getTargetTransaction().getUUID(),
                        "\"from\":\"" + eurSavings.getUUID() + "\",\"to\":\"" + eur.getUUID() + "\""));

        assertThat(run.stdout, run.exitCode, is(0));
        var after = run.updated().getAsJsonObject("after");
        assertThat(after.get("from").getAsString(), is(eurSavings.getUUID()));
        assertThat(after.get("to").getAsString(), is(eur.getUUID()));

        var read = cli.run("transactions", "--type", "TRANSFER");
        var item = read.items().get(0)
                        .getAsJsonObject();
        assertThat(item.get("from").getAsString(), is(eurSavings.getUUID()));
        assertThat(item.get("to").getAsString(), is(eur.getUUID()));
        assertThat(item.get("id").getAsString(), is(transfer.getSourceTransaction().getUUID()));
    }

    @Test
    public void theChangeOfAnUpdateAndADeleteCarriesTheSameNamesAsTheRecords() throws IOException
    {
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        cli.save(client);

        var swap = cli.apply(update(transfer.getSourceTransaction().getUUID(),
                        "\"from\":\"" + eurSavings.getUUID() + "\",\"to\":\"" + eur.getUUID() + "\""));
        assertThat(swap.stdout, swap.exitCode, is(0));
        var change = swap.updated();
        assertThat(change.getAsJsonObject("before").get("fromName").getAsString(), is(eur.getName()));
        assertThat(change.getAsJsonObject("before").get("toName").getAsString(), is(eurSavings.getName()));
        assertThat(change.getAsJsonObject("after").get("fromName").getAsString(), is(eurSavings.getName()));
        assertThat(change.getAsJsonObject("after").get("toName").getAsString(), is(eur.getName()));

        var delete = cli.apply(delete(buy.getPortfolioTransaction().getUUID()));
        assertThat(delete.stdout, delete.exitCode, is(0));
        var before = delete.deleted().getAsJsonObject("before");
        assertThat(before.get("instrumentName").getAsString(), is(eurStock.getName()));
        assertThat(before.get("investmentAccountName").getAsString(), is(depot.getName()));
        assertThat(before.get("cashAccountName").getAsString(), is(eur.getName()));
    }

    @Test
    public void updatesACrossCurrencyTransferAndRefusesAPatchThatContradictsItsRate() throws IOException
    {
        var transfer = TransactionFixtures.crossCurrencyTransfer(eur, usd, "2026-03-09", 10_000, 11_000,
                        "0.9090909091");
        cli.save(client);
        var id = transfer.getSourceTransaction().getUUID();

        var both = cli.apply(update(id, "\"amount\":200.00,\"targetAmount\":220.00"));
        assertThat(both.stdout, both.exitCode, is(0));
        assertThat(both.updated().getAsJsonObject("after").keySet(), is(java.util.Set.of("amount", "targetAmount")));
        var reloaded = cli.reload();
        assertThat(reloaded.getAccounts().get(0).getTransactions().get(0).getAmount(), is(20_000L));
        assertThat(reloaded.getAccounts().get(1).getTransactions().get(0).getAmount(), is(22_000L));

        var amountOnly = cli.apply(update(id, "\"amount\":400.00"));
        amountOnly.assertRefused("exchangeRate", "forex-mismatch");
        assertThat(cli.reload().getAccounts().get(0).getTransactions().get(0).getAmount(), is(20_000L));
    }

    private Transaction forexBuy()
    {
        var entry = TransactionFixtures.buy(depot, eur, usdStock, "2026-03-02", shares(10), 100_000);
        entry.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("EUR", 100_000), Money.of("USD", 110_000), new BigDecimal("0.9090909091")));
        return entry.getPortfolioTransaction();
    }

    @Test
    public void refusesAPatchThatLeavesTheForexUnitInconsistentAndRecomputesNothing() throws IOException
    {
        var buy = forexBuy();
        cli.save(client);

        var run = cli.apply(update(buy.getUUID(), "\"amount\":1010.00"));

        run.assertRefused("units", "import-check-failed");
        assertThat(cli.reload().getPortfolios().get(0).getTransactions().get(0).getAmount(), is(100_000L));

        var fixed = cli.apply(update(buy.getUUID(), "\"amount\":1010.00,\"units\":[{\"type\":\"GROSS_VALUE\","
                        + "\"amount\":1010.00,\"currencyCode\":\"EUR\",\"forexAmount\":1111.00,"
                        + "\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.9090909091}]"));
        assertThat(fixed.stdout, fixed.exitCode, is(0));
        assertThat(cli.reload().getPortfolios().get(0).getTransactions().get(0).getAmount(), is(101_000L));
    }

    @Test
    public void refusesAGrossAmountThatDoesNotReconcileWithTheFees() throws IOException
    {
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(1), 45_000);
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        sell.getPortfolioTransaction().addUnit(
                        new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of("EUR", 500)));
        cli.save(client);

        var run = cli.apply(update(sell.getPortfolioTransaction().getUUID(), "\"grossAmount\":999.00"));

        run.assertRefused("grossAmount", "gross-amount-mismatch");
    }

    @Test
    public void refusesToChangeTheTypeOrTheKindButAcceptsTheSameOne() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);
        cli.save(client);

        var type = cli.apply(update(deposit.getUUID(), "\"type\":\"REMOVAL\""));
        type.assertRefused("type", "type-immutable");

        var kind = cli.apply(update(transfer.getSourceTransaction().getUUID(), "\"kind\":\"security\""));
        kind.assertRefused("kind", "kind-immutable");

        var same = cli.apply(update(deposit.getUUID(), "\"type\":\"DEPOSIT\",\"amount\":900.00"));
        assertThat(same.stdout, same.exitCode, is(0));
    }

    /**
     * A failing operation changes nothing, so later operations of the batch
     * see the record as it was.
     */
    @Test
    public void leavesTheRecordUnchangedWhenTheTypeOrTheKindIsRefused()
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);

        var batch = JsonParser.parseString("[" + update(deposit.getUUID(), "\"type\":\"REMOVAL\",\"amount\":250.00")
                        + "," + update(transfer.getSourceTransaction().getUUID(),
                                        "\"kind\":\"security\",\"amount\":50.00")
                        + "]").getAsJsonArray();
        var e = assertThrows(CliException.class, () -> ApplyBatch.run(client, batch, TransactionOperations.ALL));

        @SuppressWarnings("unchecked")
        var failures = (List<Map<String, Object>>) e.toProblem().get("failures");
        assertThat(failures.stream().map(f -> f.get("code")).toList(), is(List.of("type-immutable", "kind-immutable")));
        assertThat(deposit.getAmount(), is(100_000L));
        assertThat(transfer.getSourceTransaction().getAmount(), is(25_000L));
        assertThat(transfer.getTargetTransaction().getAmount(), is(25_000L));
    }

    @Test
    public void refusesAnUnknownUuidAnUnknownFieldAndAPatchThatBreaksAnAddRule() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        cli.save(client);

        cli.apply(update("does-not-exist", "\"amount\":1.00")).assertRefused("uuid", "reference-not-found");
        cli.apply(update(deposit.getUUID(), "\"colour\":\"red\"")).assertRefused("colour", "unknown-field");
        cli.apply(update(deposit.getUUID(), "\"amount\":-5")).assertRefused("amount", "not-positive");
        cli.apply(update(deposit.getUUID(), "\"instrument\":\"" + eurStock.getUUID() + "\""))
                        .assertRefused("instrument", "instrument-not-allowed");
        cli.apply(update(deposit.getUUID(), "\"currencyCode\":\"USD\"")).assertRefused("currencyCode",
                        "currency-mismatch");
        cli.apply("{\"operation\":\"transaction.update\",\"amount\":1.00}").assertRefused("uuid", "required");

        assertThat(cli.reload().getAccounts().get(0).getTransactions().get(0).getAmount(), is(100_000L));
    }

    @Test
    public void clearsAnOptionalFieldWithNullAndLeavesAnUnchangedRecordUntouched() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        deposit.setNote("keep");
        cli.save(client);
        var stamp = cli.reload().getAccounts().get(0).getTransactions().get(0).getUpdatedAt();

        var unchanged = cli.apply(update(deposit.getUUID(), "\"note\":\"keep\""));
        assertThat(unchanged.stdout, unchanged.exitCode, is(0));
        assertThat(unchanged.data().getAsJsonArray("results").get(0)
                        .getAsJsonObject().getAsJsonArray("updated").size(), is(0));
        assertThat(cli.reload().getAccounts().get(0).getTransactions().get(0).getUpdatedAt(), is(stamp));

        var cleared = cli.apply(update(deposit.getUUID(), "\"note\":null"));
        assertThat(cleared.stdout, cleared.exitCode, is(0));
        assertThat(cleared.updated().getAsJsonObject("before").get("note").getAsString(), is("keep"));
        assertThat(cleared.updated().getAsJsonObject("after").get("note").isJsonNull(), is(true));
        assertThat(cli.reload().getAccounts().get(0).getTransactions().get(0).getNote() == null, is(true));
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

    @Test
    public void refusesToDeleteAnUnknownRecordAndTouchesNothingOnAFailedBatch() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        cli.save(client);

        var run = cli.apply(delete(deposit.getUUID()), delete("does-not-exist"));

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("operationIndex").getAsInt(), is(1));
        assertThat(run.failure(0).get("code").getAsString(), is("reference-not-found"));
        assertThat(cli.reload().getAccounts().get(0).getTransactions().size(), is(1));
    }

    @Test
    public void refusesTwoOperationsOnTheSameRecordInOneBatch() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        cli.save(client);

        var run = cli.apply(update(buy.getPortfolioTransaction().getUUID(), "\"note\":\"a\""),
                        delete(buy.getAccountTransaction().getUUID()));

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("operationIndex").getAsInt(), is(1));
        assertThat(run.failure(0).get("code").getAsString(), is("duplicate-target"));
    }

    /**
     * A record created by an earlier operation of the batch is not an existing
     * entity, so it can't be changed in the same batch. The CLI can't name such
     * a record, because a new UUID is only known from the response; a probe
     * operation creates one with a known UUID instead.
     */
    @Test
    public void refusesToChangeARecordCreatedEarlierInTheBatch()
    {
        for (var operation : List.of("transaction.update", "transaction.delete"))
        {
            var deposit = new AccountTransaction(TransactionFixtures.at("2026-03-02"), "EUR", 100_000, null,
                            AccountTransaction.Type.DEPOSIT);
            var probe = new OperationSpec<OperationContext>("probe", "Adds the deposit.",
                            Schema.object("A probe.").property(OperationSpec.OPERATION_MEMBER,
                                            Schema.enumOf("Operation name.", List.of("probe"))),
                            context -> eur.addTransaction(deposit));
            var specs = new ArrayList<>(TransactionOperations.ALL);
            specs.add(probe);

            var batch = JsonParser.parseString("[{\"operation\":\"probe\"},{\"operation\":\"" + operation
                            + "\",\"uuid\":\"" + deposit.getUUID() + "\"}]").getAsJsonArray();
            var e = assertThrows(CliException.class, () -> ApplyBatch.run(client, batch, specs));

            @SuppressWarnings("unchecked")
            var failures = (List<Map<String, Object>>) e.toProblem().get("failures");
            assertThat(operation, failures.size(), is(1));
            assertThat(failures.get(0).get("field"), is("uuid"));
            assertThat(failures.get(0).get("code"), is("reference-not-found"));
            assertThat(failures.get(0).get("message"),
                            is("No transaction with UUID " + deposit.getUUID() + " in the file."));
        }
    }

    @Test
    public void refusesADeleteThatRemovesAnEarlierBuyOfALaterSell() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(4), 42_000);
        cli.save(client);

        var run = cli.apply(delete(buy.getAccountTransaction().getUUID()));

        run.assertRefused("uuid", "negative-holding");
        var message = run.failure(0).get("message").getAsString();
        assertThat(message, containsString("Depot"));
        assertThat(message, containsString("2026-03-09"));
        assertThat(message, containsString("4 shares"));
        assertThat(cli.reload().getPortfolios().get(0).getTransactions().size(), is(2));
    }

    @Test
    public void refusesAnUpdateThatShrinksOrMovesAnEarlierBuyBelowALaterSell() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(5), 52_000);
        cli.save(client);
        var id = buy.getPortfolioTransaction().getUUID();

        cli.apply(update(id, "\"shares\":4,\"amount\":400.00")).assertRefused("shares", "negative-holding");
        cli.apply(update(id, "\"date\":\"2026-03-20\"")).assertRefused("shares", "negative-holding");
        cli.apply(update(id, "\"investmentAccount\":\"" + otherDepot.getUUID() + "\"")).assertRefused("shares",
                        "negative-holding");

        var grown = cli.apply(update(id, "\"shares\":12,\"amount\":1200.00"));
        assertThat(grown.stdout, grown.exitCode, is(0));
    }

    @Test
    public void refusesAnUpdateThatMovesASellBeforeItsBuy() throws IOException
    {
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-09", shares(10), 100_000);
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-20", shares(5), 52_000);
        cli.save(client);

        var run = cli.apply(update(sell.getPortfolioTransaction().getUUID(), "\"date\":\"2026-03-02\""));

        run.assertRefused("shares", "negative-holding");
        assertThat(run.failure(0).get("message").getAsString(), containsString("2026-03-02"));
    }

    @Test
    public void anOversellAlreadyInTheFileDoesNotBlockUpdatingOrDeletingIt() throws IOException
    {
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-02", shares(5), 52_000);
        cli.save(client);
        var id = sell.getPortfolioTransaction().getUUID();

        var update = cli.apply(update(id, "\"note\":\"already oversold\""));
        assertThat(update.stdout, update.exitCode, is(0));

        var worse = cli.apply(update(id, "\"shares\":6,\"amount\":620.00"));
        worse.assertRefused("shares", "negative-holding");

        var delete = cli.apply(delete(id));
        assertThat(delete.stdout, delete.exitCode, is(0));
    }
}
