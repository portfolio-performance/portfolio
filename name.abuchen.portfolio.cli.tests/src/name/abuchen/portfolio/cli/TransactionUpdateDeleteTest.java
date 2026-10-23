package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

/** The <code>transaction.update</code> and <code>transaction.delete</code> operations. */
public class TransactionUpdateDeleteTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path inputFolder;
    private File file;
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
        var clientFolder = folder.newFolder("client").toPath();
        inputFolder = folder.newFolder("input").toPath();

        client = new Client();
        eur = cashAccount("EUR Cash", "EUR");
        usd = cashAccount("USD Cash", "USD");
        eurSavings = cashAccount("EUR Savings", "EUR");
        depot = investmentAccount("Depot", eur);
        otherDepot = investmentAccount("Other depot", eur);
        eurStock = instrument("EUR Stock", "EUR");
        usdStock = instrument("USD Stock", "USD");

        file = clientFolder.resolve("client.xml").toFile();
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

    private void save() throws IOException
    {
        ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.XML));
    }

    private File input(String json) throws IOException
    {
        var input = Files.createTempFile(inputFolder, "batch", ".json");
        Files.writeString(input, json, StandardCharsets.UTF_8);
        return input.toFile();
    }

    private CliRun apply(String... operations) throws IOException
    {
        var args = List.of("apply", "--file", file.getPath(), "--input",
                        input("{\"operations\":[" + String.join(",", operations) + "]}").getPath());
        return CliRun.run(args.toArray(String[]::new));
    }

    private Client reload() throws IOException
    {
        return ClientFactory.load(file, null, new NullProgressMonitor());
    }

    private static String update(String uuid, String members)
    {
        return "{\"operation\":\"transaction.update\",\"uuid\":\"" + uuid + "\"," + members + "}";
    }

    private static JsonObject updated(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("results").get(0)
                        .getAsJsonObject().getAsJsonArray("updated").get(0).getAsJsonObject();
    }

    private static String delete(String uuid)
    {
        return "{\"operation\":\"transaction.delete\",\"uuid\":\"" + uuid + "\"}";
    }

    private static JsonObject deleted(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("results").get(0)
                        .getAsJsonObject().getAsJsonArray("deleted").get(0).getAsJsonObject();
    }

    private static JsonObject failure(CliRun run, int index)
    {
        return run.json().getAsJsonObject().getAsJsonArray("failures").get(index).getAsJsonObject();
    }

    private static void assertRefused(CliRun run, String field, String code)
    {
        assertThat(run.stdout, run.exitCode, is(1));
        assertThat(run.json().getAsJsonObject().get("code").getAsString(), is("validation-failed"));
        assertThat(failure(run, 0).get("field").getAsString(), is(field));
        assertThat(failure(run, 0).get("code").getAsString(), is(code));
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
        save();

        var run = apply(update(deposit.getUUID(), "\"amount\":250.00,\"note\":\"top-up\""));

        assertThat(run.stdout, run.exitCode, is(0));

        var change = updated(run);
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

        var transactions = reload().getAccounts().get(0).getTransactions();
        assertThat(transactions.size(), is(1));
        assertThat(transactions.get(0).getUUID(), is(deposit.getUUID()));
        assertThat(transactions.get(0).getAmount(), is(25_000L));
        assertThat(transactions.get(0).getNote(), is("top-up"));
    }

    @Test
    public void updatesABuyByEitherSidesUuidAndKeepsBothSidesInStep() throws IOException
    {
        var entry = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        save();
        var portfolioSide = entry.getPortfolioTransaction().getUUID();
        var accountSide = entry.getAccountTransaction().getUUID();

        var run = apply(update(accountSide,
                        "\"shares\":8,\"amount\":820.00,\"date\":\"2026-03-03\",\"note\":\"corrected\""));

        assertThat(run.stdout, run.exitCode, is(0));
        var change = updated(run);
        assertThat(change.get("uuid").getAsString(), is(portfolioSide));
        assertThat(change.getAsJsonObject("after").keySet(),
                        is(java.util.Set.of("date", "amount", "shares", "note")));

        var reloaded = reload();
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

        var byPortfolioSide = apply(update(portfolioSide, "\"amount\":830.00"));
        assertThat(byPortfolioSide.stdout, byPortfolioSide.exitCode, is(0));
        assertThat(reload().getAccounts().get(0).getTransactions().get(0).getAmount(), is(83_000L));
    }

    @Test
    public void movesTheSidesOfASellToOtherAccounts() throws IOException
    {
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(4), 42_000);
        save();

        var run = apply(update(sell.getPortfolioTransaction().getUUID(),
                        "\"investmentAccount\":\"" + otherDepot.getUUID() + "\",\"cashAccount\":\""
                                        + eurSavings.getUUID() + "\""));

        // the other depot holds no shares to sell
        assertRefused(run, "shares", "negative-holding");

        var moved = apply(update(sell.getAccountTransaction().getUUID(),
                        "\"cashAccount\":\"" + eurSavings.getUUID() + "\""));

        assertThat(moved.stdout, moved.exitCode, is(0));
        var reloaded = reload();
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
        save();

        var run = apply(update(transfer.getTargetTransaction().getUUID(), "\"shares\":5,\"amount\":520.00"));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(updated(run).get("uuid").getAsString(), is(transfer.getSourceTransaction().getUUID()));
        var reloaded = reload();
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
        save();

        var run = apply(update(transfer.getTargetTransaction().getUUID(),
                        "\"from\":\"" + eurSavings.getUUID() + "\",\"to\":\"" + eur.getUUID() + "\""));

        assertThat(run.stdout, run.exitCode, is(0));
        var after = updated(run).getAsJsonObject("after");
        assertThat(after.get("from").getAsString(), is(eurSavings.getUUID()));
        assertThat(after.get("to").getAsString(), is(eur.getUUID()));

        var read = CliRun.run("transactions", "--file", file.getPath(), "--type", "TRANSFER");
        var item = read.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("items").get(0)
                        .getAsJsonObject();
        assertThat(item.get("from").getAsString(), is(eurSavings.getUUID()));
        assertThat(item.get("to").getAsString(), is(eur.getUUID()));
        assertThat(item.get("id").getAsString(), is(transfer.getSourceTransaction().getUUID()));
    }

    @Test
    public void updatesACrossCurrencyTransferAndRefusesAPatchThatContradictsItsRate() throws IOException
    {
        var transfer = TransactionFixtures.crossCurrencyTransfer(eur, usd, "2026-03-09", 10_000, 11_000,
                        "0.9090909091");
        save();
        var id = transfer.getSourceTransaction().getUUID();

        var both = apply(update(id, "\"amount\":200.00,\"targetAmount\":220.00"));
        assertThat(both.stdout, both.exitCode, is(0));
        assertThat(updated(both).getAsJsonObject("after").keySet(), is(java.util.Set.of("amount", "targetAmount")));
        var reloaded = reload();
        assertThat(reloaded.getAccounts().get(0).getTransactions().get(0).getAmount(), is(20_000L));
        assertThat(reloaded.getAccounts().get(1).getTransactions().get(0).getAmount(), is(22_000L));

        var amountOnly = apply(update(id, "\"amount\":400.00"));
        assertRefused(amountOnly, "exchangeRate", "forex-mismatch");
        assertThat(reload().getAccounts().get(0).getTransactions().get(0).getAmount(), is(20_000L));
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
        save();

        var run = apply(update(buy.getUUID(), "\"amount\":1010.00"));

        assertRefused(run, "units", "import-check-failed");
        assertThat(reload().getPortfolios().get(0).getTransactions().get(0).getAmount(), is(100_000L));

        var fixed = apply(update(buy.getUUID(), "\"amount\":1010.00,\"units\":[{\"type\":\"GROSS_VALUE\","
                        + "\"amount\":1010.00,\"currencyCode\":\"EUR\",\"forexAmount\":1111.00,"
                        + "\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.9090909091}]"));
        assertThat(fixed.stdout, fixed.exitCode, is(0));
        assertThat(reload().getPortfolios().get(0).getTransactions().get(0).getAmount(), is(101_000L));
    }

    @Test
    public void refusesAGrossAmountThatDoesNotReconcileWithTheFees() throws IOException
    {
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(1), 45_000);
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        sell.getPortfolioTransaction().addUnit(
                        new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of("EUR", 500)));
        save();

        var run = apply(update(sell.getPortfolioTransaction().getUUID(), "\"grossAmount\":999.00"));

        assertRefused(run, "grossAmount", "gross-amount-mismatch");
    }

    @Test
    public void refusesToChangeTheTypeOrTheKindButAcceptsTheSameOne() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);
        save();

        var type = apply(update(deposit.getUUID(), "\"type\":\"REMOVAL\""));
        assertRefused(type, "type", "type-immutable");

        var kind = apply(update(transfer.getSourceTransaction().getUUID(), "\"kind\":\"security\""));
        assertRefused(kind, "kind", "kind-immutable");

        var same = apply(update(deposit.getUUID(), "\"type\":\"DEPOSIT\",\"amount\":900.00"));
        assertThat(same.stdout, same.exitCode, is(0));
    }

    @Test
    public void refusesAnUnknownUuidAnUnknownFieldAndAPatchThatBreaksAnAddRule() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        save();

        assertRefused(apply(update("does-not-exist", "\"amount\":1.00")), "uuid", "reference-not-found");
        assertRefused(apply(update(deposit.getUUID(), "\"colour\":\"red\"")), "colour", "unknown-field");
        assertRefused(apply(update(deposit.getUUID(), "\"amount\":-5")), "amount", "not-positive");
        assertRefused(apply(update(deposit.getUUID(), "\"instrument\":\"" + eurStock.getUUID() + "\"")),
                        "instrument", "instrument-not-allowed");
        assertRefused(apply(update(deposit.getUUID(), "\"currencyCode\":\"USD\"")), "currencyCode",
                        "currency-mismatch");
        assertRefused(apply("{\"operation\":\"transaction.update\",\"amount\":1.00}"), "uuid", "required");

        assertThat(reload().getAccounts().get(0).getTransactions().get(0).getAmount(), is(100_000L));
    }

    @Test
    public void clearsAnOptionalFieldWithNullAndLeavesAnUnchangedRecordUntouched() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        deposit.setNote("keep");
        save();
        var stamp = reload().getAccounts().get(0).getTransactions().get(0).getUpdatedAt();

        var unchanged = apply(update(deposit.getUUID(), "\"note\":\"keep\""));
        assertThat(unchanged.stdout, unchanged.exitCode, is(0));
        assertThat(unchanged.json().getAsJsonObject().getAsJsonObject("data").getAsJsonArray("results").get(0)
                        .getAsJsonObject().getAsJsonArray("updated").size(), is(0));
        assertThat(reload().getAccounts().get(0).getTransactions().get(0).getUpdatedAt(), is(stamp));

        var cleared = apply(update(deposit.getUUID(), "\"note\":null"));
        assertThat(cleared.stdout, cleared.exitCode, is(0));
        assertThat(updated(cleared).getAsJsonObject("before").get("note").getAsString(), is("keep"));
        assertThat(updated(cleared).getAsJsonObject("after").get("note").isJsonNull(), is(true));
        assertThat(reload().getAccounts().get(0).getTransactions().get(0).getNote() == null, is(true));
    }

    @Test
    public void deletesAWholeRecordByEitherSidesUuid() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        var transfer = TransactionFixtures.cashTransfer(eur, eurSavings, "2026-03-09", 25_000);
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-10", 5_000);
        save();

        var byAccountSide = apply(delete(buy.getAccountTransaction().getUUID()));
        assertThat(byAccountSide.stdout, byAccountSide.exitCode, is(0));
        var change = deleted(byAccountSide);
        assertThat(change.get("entity").getAsString(), is("transaction"));
        assertThat(change.get("uuid").getAsString(), is(buy.getPortfolioTransaction().getUUID()));
        assertThat(change.getAsJsonObject("before").get("type").getAsString(), is("BUY"));
        assertThat(change.getAsJsonObject("after").size(), is(0));
        assertThat(reload().getPortfolios().get(0).getTransactions().isEmpty(), is(true));

        var byTarget = apply(delete(transfer.getTargetTransaction().getUUID()), delete(deposit.getUUID()));
        assertThat(byTarget.stdout, byTarget.exitCode, is(0));

        var reloaded = reload();
        assertThat(reloaded.getAccounts().stream().mapToInt(a -> a.getTransactions().size()).sum(), is(0));
    }

    @Test
    public void refusesToDeleteAnUnknownRecordAndTouchesNothingOnAFailedBatch() throws IOException
    {
        var deposit = TransactionFixtures.cash(eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        save();

        var run = apply(delete(deposit.getUUID()), delete("does-not-exist"));

        assertThat(run.exitCode, is(1));
        assertThat(failure(run, 0).get("operationIndex").getAsInt(), is(1));
        assertThat(failure(run, 0).get("code").getAsString(), is("reference-not-found"));
        assertThat(reload().getAccounts().get(0).getTransactions().size(), is(1));
    }

    @Test
    public void refusesTwoOperationsOnTheSameRecordInOneBatch() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        save();

        var run = apply(update(buy.getPortfolioTransaction().getUUID(), "\"note\":\"a\""),
                        delete(buy.getAccountTransaction().getUUID()));

        assertThat(run.exitCode, is(1));
        assertThat(failure(run, 0).get("operationIndex").getAsInt(), is(1));
        assertThat(failure(run, 0).get("code").getAsString(), is("duplicate-target"));
    }

    @Test
    public void refusesADeleteThatRemovesAnEarlierBuyOfALaterSell() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(4), 42_000);
        save();

        var run = apply(delete(buy.getAccountTransaction().getUUID()));

        assertRefused(run, "uuid", "negative-holding");
        var message = failure(run, 0).get("message").getAsString();
        assertThat(message, containsString("Depot"));
        assertThat(message, containsString("2026-03-09"));
        assertThat(message, containsString("4 shares"));
        assertThat(reload().getPortfolios().get(0).getTransactions().size(), is(2));
    }

    @Test
    public void refusesAnUpdateThatShrinksOrMovesAnEarlierBuyBelowALaterSell() throws IOException
    {
        var buy = TransactionFixtures.buy(depot, eur, eurStock, "2026-03-02", shares(10), 100_000);
        TransactionFixtures.sell(depot, eur, eurStock, "2026-03-09", shares(5), 52_000);
        save();
        var id = buy.getPortfolioTransaction().getUUID();

        assertRefused(apply(update(id, "\"shares\":4,\"amount\":400.00")), "shares", "negative-holding");
        assertRefused(apply(update(id, "\"date\":\"2026-03-20\"")), "shares", "negative-holding");
        assertRefused(apply(update(id, "\"investmentAccount\":\"" + otherDepot.getUUID() + "\"")), "shares",
                        "negative-holding");

        var grown = apply(update(id, "\"shares\":12,\"amount\":1200.00"));
        assertThat(grown.stdout, grown.exitCode, is(0));
    }

    @Test
    public void refusesAnUpdateThatMovesASellBeforeItsBuy() throws IOException
    {
        TransactionFixtures.buy(depot, eur, eurStock, "2026-03-09", shares(10), 100_000);
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-20", shares(5), 52_000);
        save();

        var run = apply(update(sell.getPortfolioTransaction().getUUID(), "\"date\":\"2026-03-02\""));

        assertRefused(run, "shares", "negative-holding");
        assertThat(failure(run, 0).get("message").getAsString(), containsString("2026-03-02"));
    }

    @Test
    public void anOversellAlreadyInTheFileDoesNotBlockUpdatingOrDeletingIt() throws IOException
    {
        var sell = TransactionFixtures.sell(depot, eur, eurStock, "2026-03-02", shares(5), 52_000);
        save();
        var id = sell.getPortfolioTransaction().getUUID();

        var update = apply(update(id, "\"note\":\"already oversold\""));
        assertThat(update.stdout, update.exitCode, is(0));

        var worse = apply(update(id, "\"shares\":6,\"amount\":620.00"));
        assertRefused(worse, "shares", "negative-holding");

        var delete = apply(delete(id));
        assertThat(delete.stdout, delete.exitCode, is(0));
    }
}
