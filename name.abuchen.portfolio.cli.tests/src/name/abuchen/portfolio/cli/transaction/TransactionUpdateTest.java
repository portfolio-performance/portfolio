package name.abuchen.portfolio.cli.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.TransactionFixtures;
import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

/**
 * <code>update</code> and <code>delete</code> of {@link TransactionRecords},
 * on an in-memory client: the round trip through render, moves between
 * accounts, incomplete records, claims, immutable types and the oversell
 * check.
 */
public class TransactionUpdateTest
{
    private static final Gson GSON = new Gson();

    private static final Claims NO_CLAIMS = uuid -> OptionalInt.empty();

    /** Every claim is held by operation 0 already. */
    private static final Claims ALL_CLAIMED = uuid -> OptionalInt.of(0);

    /** Shares as stored. */
    private static long shares(long count)
    {
        return count * 100_000_000L;
    }

    /** Cash accounts in EUR and USD, two investment accounts and an instrument in each currency. */
    private static final class Fixture
    {
        final Client client = new Client();
        final Account eur;
        final Account savings;
        final Account usd;
        final Portfolio depot;
        final Portfolio otherDepot;
        final Security acme;
        final Security dollarStock;

        Fixture()
        {
            eur = new AccountBuilder("EUR").addTo(client);
            eur.setName("EUR Cash");
            savings = new AccountBuilder("EUR").addTo(client);
            savings.setName("EUR Savings");
            usd = new AccountBuilder("USD").addTo(client);
            usd.setName("USD Cash");
            depot = new PortfolioBuilder(eur).addTo(client);
            depot.setName("Depot");
            otherDepot = new PortfolioBuilder(eur).addTo(client);
            otherDepot.setName("Other depot");
            acme = new SecurityBuilder("EUR").addTo(client);
            acme.setName("ACME");
            dollarStock = new SecurityBuilder("USD").addTo(client);
            dollarStock.setName("Dollar Stock");
        }

        TransactionRecords records()
        {
            return TransactionRecords.open(client, any -> true);
        }

        Outcome update(String uuid, String members)
        {
            return records().update(patch(uuid, members), NO_CLAIMS);
        }

        Outcome delete(String uuid)
        {
            return records().delete(patch(uuid, ""), NO_CLAIMS);
        }

        /** Every record of the client as rendered, which a refused write must leave as it was. */
        Set<String> snapshot()
        {
            var records = records();
            return records.all().stream().map(record -> GSON.toJson(records.render(record)))
                            .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static JsonObject patch(String uuid, String members)
    {
        return JsonParser.parseString("{\"uuid\":\"" + uuid + "\"" + (members.isEmpty() ? "" : "," + members) + "}")
                        .getAsJsonObject();
    }

    private static Set<String> failures(Outcome outcome)
    {
        assertThat(String.valueOf(outcome), outcome, instanceOf(Outcome.Refused.class));
        return ((Outcome.Refused) outcome).failures().stream().map(f -> f.field() + "/" + f.code())
                        .collect(Collectors.toSet());
    }

    private static Outcome.Applied applied(Outcome outcome)
    {
        assertThat(String.valueOf(outcome), outcome, instanceOf(Outcome.Applied.class));
        return (Outcome.Applied) outcome;
    }

    private static JsonObject json(Object map)
    {
        return GSON.toJsonTree(map).getAsJsonObject();
    }

    /** The rendered record as a patch: the members an add declares, without an empty list of units. */
    private static JsonObject asPatch(JsonObject record)
    {
        var patch = new JsonObject();
        patch.addProperty("uuid", record.get("id").getAsString());
        for (var member : record.entrySet())
        {
            var value = member.getValue();
            if (TransactionRecords.addSchema().declares(member.getKey())
                            && !(value.isJsonArray() && value.getAsJsonArray().isEmpty()))
                patch.add(member.getKey(), value);
        }
        return patch;
    }

    /** One record of each row, with the optional members it takes, and a few with units. */
    private static List<String> everyType(Fixture f)
    {
        var inputs = new ArrayList<String>();
        var cash = "\"cashAccount\":\"" + f.eur.getUUID() + "\",\"date\":\"2026-03-02\",\"time\":\"09:30\","
                        + "\"amount\":10.00,\"currencyCode\":\"EUR\",\"note\":\"n\",\"source\":\"s.pdf\"";
        for (var type : List.of("DEPOSIT", "REMOVAL", "INTEREST", "INTEREST_CHARGE", "FEES", "FEES_REFUND", "TAXES",
                        "TAX_REFUND"))
            inputs.add("\"type\":\"" + type + "\"," + cash);
        inputs.add("\"type\":\"INTEREST\"," + cash
                        + ",\"units\":[{\"type\":\"TAX\",\"amount\":0.50,\"currencyCode\":\"EUR\"}]");
        inputs.add("\"type\":\"FEES\",\"instrument\":\"" + f.acme.getUUID() + "\"," + cash);
        inputs.add("\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + f.eur.getUUID()
                        + "\",\"date\":\"2026-03-01\",\"amount\":9.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + f.dollarStock.getUUID() + "\",\"shares\":4,\"exDate\":\"2026-02-25\",\"units\":["
                        + "{\"type\":\"GROSS_VALUE\",\"amount\":10.00,\"currencyCode\":\"EUR\",\"forexAmount\":12.50,"
                        + "\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.8},"
                        + "{\"type\":\"TAX\",\"amount\":1.00,\"currencyCode\":\"EUR\"}]");

        var trade = "\"investmentAccount\":\"" + f.depot.getUUID() + "\",\"instrument\":\"" + f.acme.getUUID()
                        + "\",\"shares\":1,\"amount\":10.00,\"currencyCode\":\"EUR\",\"date\":\"2026-03-03\"";
        var cashSide = ",\"cashAccount\":\"" + f.eur.getUUID() + "\"";
        inputs.add("\"type\":\"BUY\"," + trade + cashSide
                        + ",\"units\":[{\"type\":\"FEE\",\"amount\":0.50,\"currencyCode\":\"EUR\"}]");
        inputs.add("\"type\":\"SELL\"," + trade + cashSide);
        inputs.add("\"type\":\"DELIVERY_INBOUND\"," + trade);
        inputs.add("\"type\":\"DELIVERY_OUTBOUND\"," + trade);

        inputs.add("\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\"" + f.eur.getUUID() + "\",\"to\":\""
                        + f.savings.getUUID() + "\",\"date\":\"2026-03-04\",\"amount\":10.00,\"currencyCode\":\"EUR\"");
        inputs.add("\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\"" + f.eur.getUUID() + "\",\"to\":\""
                        + f.usd.getUUID()
                        + "\",\"date\":\"2026-03-04\",\"amount\":10.00,\"currencyCode\":\"EUR\",\"targetAmount\":11.00");
        inputs.add("\"type\":\"TRANSFER\",\"kind\":\"security\",\"from\":\"" + f.depot.getUUID() + "\",\"to\":\""
                        + f.otherDepot.getUUID() + "\",\"instrument\":\"" + f.acme.getUUID()
                        + "\",\"shares\":1,\"date\":\"2026-03-05\",\"amount\":10.00,\"currencyCode\":\"EUR\"");
        return inputs;
    }

    @Test
    public void everyRecordUpdatedWithItsOwnRenderingIsUnchanged()
    {
        var f = new Fixture();
        TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-01-02", shares(100), 1_000_000);
        TransactionFixtures.crossCurrencyTransfer(f.eur, f.usd, "2026-01-03", 10_000, 11_000, "0.9090909091");
        for (var input : everyType(f))
            applied(f.records().add(JsonParser.parseString("{" + input + "}").getAsJsonObject()));

        var before = f.snapshot();
        var records = f.records();
        for (var record : records.all())
        {
            var outcome = records.update(asPatch(json(records.render(record))), NO_CLAIMS);
            assertThat(record.type(), outcome, instanceOf(Outcome.Unchanged.class));
            assertThat(((Outcome.Unchanged) outcome).recordId(), is(record.id()));
        }
        assertThat(f.snapshot(), is(before));
    }

    @Test
    public void anUpdateReportsOnlyTheChangedMembersAndNullClearsAMember()
    {
        var f = new Fixture();
        var deposit = TransactionFixtures.cash(f.eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);

        var change = applied(f.update(deposit.getUUID(), "\"amount\":250.00,\"note\":\"top-up\""));

        assertThat(change.kind(), is(Outcome.Entity.TRANSACTION));
        assertThat(change.recordId(), is(deposit.getUUID()));
        assertThat(change.before().keySet(), is(Set.of("amount", "note")));
        assertThat(change.after().keySet(), is(Set.of("amount", "note")));
        assertThat(json(change.after()).get("amount").getAsBigDecimal().doubleValue(), is(250.00));
        assertThat(change.before().get("note") == null, is(true));
        assertThat(deposit.getAmount(), is(25_000L));
        assertThat(deposit.getNote(), is("top-up"));

        var cleared = applied(f.update(deposit.getUUID(), "\"note\":null"));
        assertThat(cleared.before().get("note"), is("top-up"));
        assertThat(cleared.after().get("note") == null, is(true));
        assertThat(deposit.getNote() == null, is(true));

        assertThat(f.update(deposit.getUUID(), "\"amount\":250.00"), instanceOf(Outcome.Unchanged.class));
    }

    @Test
    public void aBuyIsUpdatedByEitherSideAndBothSidesStayInStep()
    {
        var f = new Fixture();
        var entry = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);

        var change = applied(f.update(entry.getAccountTransaction().getUUID(),
                        "\"shares\":8,\"amount\":820.00,\"date\":\"2026-03-03\""));

        assertThat(change.recordId(), is(entry.getPortfolioTransaction().getUUID()));
        assertThat(change.after().keySet(), is(Set.of("date", "amount", "shares")));
        for (var side : List.of(entry.getPortfolioTransaction(), entry.getAccountTransaction()))
        {
            assertThat(side.getAmount(), is(82_000L));
            assertThat(side.getDateTime().toLocalDate().toString(), is("2026-03-03"));
        }
        assertThat(entry.getPortfolioTransaction().getShares(), is(shares(8)));
        assertThat(f.depot.getTransactions(), is(List.of(entry.getPortfolioTransaction())));
        assertThat(f.eur.getTransactions(), is(List.of(entry.getAccountTransaction())));
    }

    @Test
    public void sidesMoveToTheAccountsThePatchNamesAndTheCrossEntryFollows()
    {
        var f = new Fixture();
        var buy = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);
        var transfer = TransactionFixtures.cashTransfer(f.eur, f.savings, "2026-03-09", 25_000);

        var moved = applied(f.update(buy.getAccountTransaction().getUUID(), "\"investmentAccount\":\""
                        + f.otherDepot.getUUID() + "\",\"cashAccount\":\"" + f.savings.getUUID() + "\""));
        assertThat(moved.before().get("investmentAccountName"), is("Depot"));
        assertThat(moved.after().get("investmentAccountName"), is("Other depot"));
        assertThat(moved.after().get("cashAccount"), is(f.savings.getUUID()));
        assertThat(f.depot.getTransactions().isEmpty(), is(true));
        assertThat(f.otherDepot.getTransactions(), is(List.of(buy.getPortfolioTransaction())));
        assertThat(f.savings.getTransactions().contains(buy.getAccountTransaction()), is(true));
        assertThat(f.eur.getTransactions().contains(buy.getAccountTransaction()), is(false));
        assertThat(buy.getPortfolio(), is(f.otherDepot));
        assertThat(buy.getAccount(), is(f.savings));

        var swapped = applied(f.update(transfer.getTargetTransaction().getUUID(),
                        "\"from\":\"" + f.savings.getUUID() + "\",\"to\":\"" + f.eur.getUUID() + "\""));
        assertThat(swapped.recordId(), is(transfer.getSourceTransaction().getUUID()));
        assertThat(swapped.after().get("fromName"), is("EUR Savings"));
        assertThat(transfer.getSourceAccount(), is(f.savings));
        assertThat(transfer.getTargetAccount(), is(f.eur));
        assertThat(f.savings.getTransactions().contains(transfer.getSourceTransaction()), is(true));
        assertThat(f.eur.getTransactions().contains(transfer.getTargetTransaction()), is(true));
    }

    @Test
    public void aSecurityTransferIsUpdatedByItsIncomingSide()
    {
        var f = new Fixture();
        TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);
        var transfer = TransactionFixtures.securityTransfer(f.depot, f.otherDepot, f.acme, "2026-03-09", shares(6),
                        62_000);

        var change = applied(f.update(transfer.getTargetTransaction().getUUID(), "\"shares\":5,\"amount\":520.00"));

        assertThat(change.recordId(), is(transfer.getSourceTransaction().getUUID()));
        assertThat(transfer.getSourceTransaction().getShares(), is(shares(5)));
        assertThat(transfer.getTargetTransaction().getShares(), is(shares(5)));
        assertThat(transfer.getTargetTransaction().getAmount(), is(52_000L));
    }

    @Test
    public void aRecordMissingASideCanOnlyBeDeleted()
    {
        var f = new Fixture();
        var buy = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);
        f.eur.getTransactions().remove(buy.getAccountTransaction());
        var before = f.snapshot();

        var id = buy.getPortfolioTransaction().getUUID();
        assertThat(failures(f.update(id, "\"note\":\"n\"")), is(Set.of("uuid/record-incomplete")));
        assertThat(failures(f.update(id, "\"cashAccount\":\"" + f.eur.getUUID() + "\"")),
                        is(Set.of("uuid/record-incomplete")));
        assertThat(f.snapshot(), is(before));

        applied(f.delete(id));
        assertThat(f.depot.getTransactions().isEmpty(), is(true));
    }

    @Test
    public void everySideIsClaimedBeforeAnythingIsChecked()
    {
        var f = new Fixture();
        var buy = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);
        var id = buy.getPortfolioTransaction().getUUID();
        var before = f.snapshot();

        assertThat(failures(f.records().update(patch(id, "\"amount\":-5"), ALL_CLAIMED)),
                        is(Set.of("uuid/duplicate-target")));
        assertThat(failures(f.records().delete(patch(id, ""), ALL_CLAIMED)), is(Set.of("uuid/duplicate-target")));

        Claims cashSideClaimed = uuid -> uuid.equals(buy.getAccountTransaction().getUUID()) ? OptionalInt.of(2)
                        : OptionalInt.empty();
        var outcome = f.records().update(patch(id, "\"note\":\"n\""), cashSideClaimed);
        assertThat(failures(outcome), is(Set.of("uuid/duplicate-target")));
        assertThat(((Outcome.Refused) outcome).failures().get(0).message(), containsString("Operation 2"));
        assertThat(f.snapshot(), is(before));

        var claimed = new ArrayList<String>();
        applied(f.records().update(patch(id, "\"note\":\"n\""), uuid -> {
            claimed.add(uuid);
            return OptionalInt.empty();
        }));
        assertThat(claimed, is(List.of(id, buy.getAccountTransaction().getUUID())));
    }

    /** Regression of c84342165: a refused type or kind change stops the whole update. */
    @Test
    public void aRefusedTypeOrKindChangeStopsTheUpdate()
    {
        var f = new Fixture();
        var deposit = TransactionFixtures.cash(f.eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        var transfer = TransactionFixtures.cashTransfer(f.eur, f.savings, "2026-03-09", 25_000);

        assertThat(failures(f.update(deposit.getUUID(), "\"type\":\"REMOVAL\",\"amount\":250.00")),
                        is(Set.of("type/type-immutable")));
        assertThat(deposit.getAmount(), is(100_000L));

        assertThat(failures(f.update(transfer.getSourceTransaction().getUUID(),
                        "\"kind\":\"security\",\"amount\":50.00")), is(Set.of("kind/kind-immutable")));
        assertThat(transfer.getSourceTransaction().getAmount(), is(25_000L));
        assertThat(transfer.getTargetTransaction().getAmount(), is(25_000L));

        applied(f.update(deposit.getUUID(), "\"type\":\"DEPOSIT\",\"amount\":900.00"));
        assertThat(failures(f.update(deposit.getUUID(), "\"kind\":\"cash\"")), is(Set.of("kind/kind-not-allowed")));
    }

    @Test
    public void thePatchIsValidatedLikeAnAdd()
    {
        var f = new Fixture();
        var deposit = TransactionFixtures.cash(f.eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);
        var before = f.snapshot();

        assertThat(failures(f.update(deposit.getUUID(), "\"colour\":\"red\"")), is(Set.of("colour/unknown-field")));
        assertThat(failures(f.update(deposit.getUUID(), "\"amount\":-5")), is(Set.of("amount/not-positive")));
        assertThat(failures(f.update(deposit.getUUID(), "\"amount\":null")), is(Set.of("amount/required")));
        assertThat(failures(f.update(deposit.getUUID(), "\"instrument\":\"" + f.acme.getUUID() + "\"")),
                        is(Set.of("instrument/instrument-not-allowed")));
        assertThat(failures(f.update(deposit.getUUID(), "\"currencyCode\":\"USD\"")),
                        is(Set.of("currencyCode/currency-mismatch")));
        assertThat(f.snapshot(), is(before));
    }

    @Test
    public void nothingIsRecomputedSoAContradictedRateOrForexUnitIsRefused()
    {
        var f = new Fixture();
        var transfer = TransactionFixtures.crossCurrencyTransfer(f.eur, f.usd, "2026-03-09", 10_000, 11_000,
                        "0.9090909091");
        var id = transfer.getSourceTransaction().getUUID();

        var both = applied(f.update(id, "\"amount\":200.00,\"targetAmount\":220.00"));
        assertThat(both.after().keySet(), is(Set.of("amount", "targetAmount")));
        assertThat(transfer.getTargetTransaction().getAmount(), is(22_000L));

        assertThat(failures(f.update(id, "\"amount\":400.00")), is(Set.of("exchangeRate/forex-mismatch")));
        assertThat(transfer.getSourceTransaction().getAmount(), is(20_000L));

        var buy = TransactionFixtures.buy(f.depot, f.eur, f.dollarStock, "2026-03-02", shares(10), 100_000);
        buy.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("EUR", 100_000), Money.of("USD", 110_000), new BigDecimal("0.9090909091")));
        assertThat(failures(f.update(buy.getPortfolioTransaction().getUUID(), "\"amount\":1010.00")),
                        is(Set.of("units/import-check-failed")));
    }

    @Test
    public void onlyARecordInTheFileBeforeTheBatchCanBeFound()
    {
        var f = new Fixture();
        var deposit = TransactionFixtures.cash(f.eur, AccountTransaction.Type.DEPOSIT, "2026-03-02", 100_000);

        assertThat(failures(f.update("does-not-exist", "\"amount\":1.00")), is(Set.of("uuid/reference-not-found")));
        assertThat(failures(f.delete("does-not-exist")), is(Set.of("uuid/reference-not-found")));
        assertThat(failures(f.records().update(JsonParser.parseString("{\"amount\":1.00}").getAsJsonObject(),
                        NO_CLAIMS)), is(Set.of("uuid/required")));

        var newInBatch = TransactionRecords.open(f.client, any -> false);
        var update = newInBatch.update(patch(deposit.getUUID(), "\"amount\":1.00"), NO_CLAIMS);
        assertThat(failures(update), is(Set.of("uuid/reference-not-found")));
        assertThat(((Outcome.Refused) update).failures().get(0).message(),
                        is("No transaction with UUID " + deposit.getUUID() + " in the file."));
        assertThat(failures(newInBatch.delete(patch(deposit.getUUID(), ""), NO_CLAIMS)),
                        is(Set.of("uuid/reference-not-found")));
    }

    @Test
    public void aDeleteRemovesEverySideAndReportsTheRecordAsItWas()
    {
        var f = new Fixture();
        var buy = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);

        var change = applied(f.delete(buy.getAccountTransaction().getUUID()));

        assertThat(change.recordId(), is(buy.getPortfolioTransaction().getUUID()));
        assertThat(change.after().isEmpty(), is(true));
        assertThat(change.before().get("type"), is("BUY"));
        assertThat(change.before().get("instrumentName"), is("ACME"));
        assertThat(f.depot.getTransactions().isEmpty(), is(true));
        assertThat(f.eur.getTransactions().isEmpty(), is(true));
    }

    @Test
    public void anUpdateThatMovesABuyBetweenInvestmentAccountsMustNotOversell()
    {
        var f = new Fixture();
        var buy = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);
        TransactionFixtures.sell(f.depot, f.eur, f.acme, "2026-03-09", shares(5), 52_000);
        var id = buy.getPortfolioTransaction().getUUID();
        var before = f.snapshot();

        assertThat(failures(f.update(id, "\"investmentAccount\":\"" + f.otherDepot.getUUID() + "\"")),
                        is(Set.of("shares/negative-holding")));
        assertThat(failures(f.update(id, "\"shares\":4,\"amount\":400.00")), is(Set.of("shares/negative-holding")));
        assertThat(failures(f.update(id, "\"date\":\"2026-03-20\"")), is(Set.of("shares/negative-holding")));
        assertThat(f.snapshot(), is(before));

        applied(f.update(id, "\"shares\":12,\"amount\":1200.00"));
    }

    @Test
    public void aDeleteOfABuyMustNotOversell()
    {
        var f = new Fixture();
        var buy = TransactionFixtures.buy(f.depot, f.eur, f.acme, "2026-03-02", shares(10), 100_000);
        TransactionFixtures.sell(f.depot, f.eur, f.acme, "2026-03-09", shares(4), 42_000);

        var outcome = f.delete(buy.getAccountTransaction().getUUID());

        assertThat(failures(outcome), is(Set.of("uuid/negative-holding")));
        var message = ((Outcome.Refused) outcome).failures().get(0).message();
        assertThat(message, containsString("Depot"));
        assertThat(message, containsString("2026-03-09"));
        assertThat(message, containsString("4 shares"));
        assertThat(f.depot.getTransactions().size(), is(2));
    }

    @Test
    public void anOversellAlreadyInTheFileDoesNotBlockUpdatingOrDeletingIt()
    {
        var f = new Fixture();
        var sell = TransactionFixtures.sell(f.depot, f.eur, f.acme, "2026-03-02", shares(5), 52_000);
        var id = sell.getPortfolioTransaction().getUUID();

        applied(f.update(id, "\"note\":\"already oversold\""));
        assertThat(failures(f.update(id, "\"shares\":6,\"amount\":620.00")), is(Set.of("shares/negative-holding")));
        applied(f.delete(id));
    }
}
