package name.abuchen.portfolio.cli.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

/** The side join and the rendered shape of transaction records, on an in-memory client. */
public class TransactionRecordsTest
{
    private static final Gson GSON = new Gson();

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

    private TransactionRecords records()
    {
        return TransactionRecords.open(client, any -> true);
    }

    /** The record with a side of the UUID, rendered as JSON. */
    private JsonObject rendered(String sideUuid)
    {
        var records = records();
        return GSON.toJsonTree(records.render(records.find(sideUuid).orElseThrow())).getAsJsonObject();
    }

    private List<String> ids()
    {
        return records().all().stream().map(TransactionRecord::id).toList();
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
    public void aBuyIsOneRecordJoiningBothSidesWithTheInvestmentAccountSideAsId()
    {
        var entry = TransactionFixtures.buy(depot, cash, acme, "2026-03-02T09:30", 10_00000000L, 100_500);
        entry.getPortfolioTransaction()
                        .addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of("EUR", 500)));
        entry.getPortfolioTransaction().setNote("first buy");

        assertThat(ids(), contains(entry.getPortfolioTransaction().getUUID()));

        var buy = rendered(entry.getPortfolioTransaction().getUUID());
        assertThat(text(buy, "id"), is(entry.getPortfolioTransaction().getUUID()));
        assertThat(text(buy, "type"), is("BUY"));
        assertThat(text(buy, "date"), is("2026-03-02"));
        assertThat(text(buy, "time"), is("09:30"));
        assertThat(strings(buy.getAsJsonArray("sides")), contains(entry.getPortfolioTransaction().getUUID(),
                        entry.getAccountTransaction().getUUID()));
        assertThat(decimal(buy, "amount"), is("1005.00"));
        assertThat(text(buy, "currencyCode"), is("EUR"));
        assertThat(text(buy, "instrument"), is(acme.getUUID()));
        assertThat(text(buy, "instrumentName"), is("ACME"));
        assertThat(decimal(buy, "shares"), is("10.00000000"));
        assertThat(text(buy, "investmentAccount"), is(depot.getUUID()));
        assertThat(text(buy, "investmentAccountName"), is("Depot"));
        assertThat(text(buy, "cashAccount"), is(cash.getUUID()));
        assertThat(text(buy, "cashAccountName"), is("Cash"));
        assertThat(text(buy, "note"), is("first buy"));
        assertThat(buy.has("kind"), is(false));
        assertThat(buy.has("from"), is(false));
        assertThat(buy.has("fromName"), is(false));

        var unit = buy.getAsJsonArray("units").get(0).getAsJsonObject();
        assertThat(unit.keySet(), is(Set.of("type", "amount", "currencyCode")));
        assertThat(text(unit, "type"), is("FEE"));
        assertThat(decimal(unit, "amount"), is("5.00"));
        assertThat(text(unit, "currencyCode"), is("EUR"));
    }

    @Test
    public void aRecordIsFoundByTheUuidOfEitherSide()
    {
        var entry = TransactionFixtures.buy(depot, cash, acme, "2026-03-02", 10_00000000L, 100_500);
        var records = records();

        var byPortfolioSide = records.find(entry.getPortfolioTransaction().getUUID()).orElseThrow();
        var byCashSide = records.find(entry.getAccountTransaction().getUUID()).orElseThrow();

        assertThat(byPortfolioSide.id(), is(entry.getPortfolioTransaction().getUUID()));
        assertThat(byCashSide.id(), is(entry.getPortfolioTransaction().getUUID()));
        assertThat(records.find("no-such-uuid").isPresent(), is(false));
    }

    @Test
    public void onlyARecordThatExistedBeforeTheBatchIsFound()
    {
        var deposit = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-01", 1_000);

        var records = TransactionRecords.open(client, entity -> entity != deposit);

        assertThat(records.find(deposit.getUUID()).isPresent(), is(false));
        assertThat(records.all().size(), is(1));
    }

    @Test
    public void theJoinSeesChangesMadeAfterOpening()
    {
        var records = records();
        assertThat(records.all().size(), is(0));

        var deposit = TransactionFixtures.cash(cash, AccountTransaction.Type.DEPOSIT, "2026-01-01", 1_000);

        assertThat(records.find(deposit.getUUID()).isPresent(), is(true));
    }

    @Test
    public void anUnnamedEntityHasAnEmptyName()
    {
        acme.setName(null);
        cash.setName(null);
        depot.setName(null);
        var entry = TransactionFixtures.buy(depot, cash, acme, "2026-03-02", 10_00000000L, 100_500);

        var buy = rendered(entry.getPortfolioTransaction().getUUID());
        assertThat(text(buy, "instrumentName"), is(""));
        assertThat(text(buy, "investmentAccountName"), is(""));
        assertThat(text(buy, "cashAccountName"), is(""));
    }

    @Test
    public void aSellAndDeliveriesAreRecordsWithTheRightAccounts()
    {
        var sell = TransactionFixtures.sell(depot, cash, acme, "2026-03-03", 5_00000000L, 60_000);
        var inbound = new PortfolioTransaction(TransactionFixtures.at("2026-03-04"), "EUR", 10_000, acme, 1_00000000L,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0);
        depot.addTransaction(inbound);
        var outbound = new PortfolioTransaction(TransactionFixtures.at("2026-03-05"), "EUR", 10_000, acme, 1_00000000L,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, 0, 0);
        depot.addTransaction(outbound);

        assertThat(ids(), containsInAnyOrder(sell.getPortfolioTransaction().getUUID(), inbound.getUUID(),
                        outbound.getUUID()));

        var sold = rendered(sell.getAccountTransaction().getUUID());
        assertThat(text(sold, "type"), is("SELL"));
        assertThat(text(sold, "id"), is(sell.getPortfolioTransaction().getUUID()));
        assertThat(sold.getAsJsonArray("sides").size(), is(2));
        assertThat(text(sold, "cashAccount"), is(cash.getUUID()));

        for (var delivery : List.of(inbound, outbound))
        {
            var record = rendered(delivery.getUUID());
            assertThat(text(record, "type"), is(delivery.getType().name()));
            assertThat(text(record, "id"), is(delivery.getUUID()));
            assertThat(record.getAsJsonArray("sides").size(), is(1));
            assertThat(text(record, "investmentAccount"), is(depot.getUUID()));
            assertThat(record.has("cashAccount"), is(false));
            assertThat(decimal(record, "shares"), is("1.00000000"));
            assertThat(text(record, "instrument"), is(acme.getUUID()));
        }
    }

    @Test
    public void everyCashTypeIsTheTransactionItselfOnTheCashAccount()
    {
        var types = List.of(AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL,
                        AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE,
                        AccountTransaction.Type.FEES, AccountTransaction.Type.FEES_REFUND,
                        AccountTransaction.Type.TAXES, AccountTransaction.Type.TAX_REFUND);

        for (var type : types)
        {
            var transaction = TransactionFixtures.cash(cash, type, "2026-01-01", 1_000);

            var record = rendered(transaction.getUUID());
            assertThat(text(record, "type"), is(type.name()));
            assertThat(text(record, "id"), is(transaction.getUUID()));
            assertThat(strings(record.getAsJsonArray("sides")), contains(transaction.getUUID()));
            assertThat(text(record, "cashAccount"), is(cash.getUUID()));
            assertThat(text(record, "cashAccountName"), is("Cash"));
            assertThat(record.has("investmentAccount"), is(false));
            assertThat(record.has("instrument"), is(false));
            assertThat(record.has("shares"), is(false));
            assertThat(record.has("time"), is(false));
            assertThat(record.getAsJsonArray("units").size(), is(0));
        }
    }

    @Test
    public void aDividendCarriesItsInstrumentExDateSharesAndTaxes()
    {
        var dividend = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-05-01", 4_000);
        dividend.setSecurity(acme);
        dividend.setShares(10_00000000L);
        dividend.setExDate(TransactionFixtures.at("2026-04-28"));
        dividend.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of("EUR", 1_000)));

        var withoutShares = TransactionFixtures.cash(cash, AccountTransaction.Type.DIVIDENDS, "2026-06-01", 2_000);
        withoutShares.setSecurity(acme);

        var first = rendered(dividend.getUUID());
        assertThat(text(first, "type"), is("DIVIDENDS"));
        assertThat(text(first, "instrument"), is(acme.getUUID()));
        assertThat(text(first, "instrumentName"), is("ACME"));
        assertThat(text(first, "cashAccountName"), is("Cash"));
        assertThat(first.has("investmentAccountName"), is(false));
        assertThat(decimal(first, "shares"), is("10.00000000"));
        assertThat(text(first, "exDate"), is("2026-04-28"));
        assertThat(text(first.getAsJsonArray("units").get(0).getAsJsonObject(), "type"), is("TAX"));

        var second = rendered(withoutShares.getUUID());
        assertThat(second.has("shares"), is(false));
        assertThat(second.has("exDate"), is(false));
    }

    @Test
    public void aCashTransferIsOneRecordFromTheOutgoingSide()
    {
        var savings = new AccountBuilder("EUR").addTo(client);
        savings.setName("Savings");
        var entry = TransactionFixtures.cashTransfer(cash, savings, "2026-02-01T08:15", 50_000);
        entry.setNote("move");

        assertThat(ids(), contains(entry.getSourceTransaction().getUUID()));

        var transfer = rendered(entry.getTargetTransaction().getUUID());
        assertThat(text(transfer, "type"), is("TRANSFER"));
        assertThat(text(transfer, "kind"), is("cash"));
        assertThat(text(transfer, "id"), is(entry.getSourceTransaction().getUUID()));
        assertThat(strings(transfer.getAsJsonArray("sides")),
                        contains(entry.getSourceTransaction().getUUID(), entry.getTargetTransaction().getUUID()));
        assertThat(text(transfer, "from"), is(cash.getUUID()));
        assertThat(text(transfer, "fromName"), is("Cash"));
        assertThat(text(transfer, "to"), is(savings.getUUID()));
        assertThat(text(transfer, "toName"), is("Savings"));
        assertThat(decimal(transfer, "amount"), is("500.00"));
        assertThat(text(transfer, "note"), is("move"));
        assertThat(transfer.has("cashAccount"), is(false));
        assertThat(transfer.has("instrumentName"), is(false));
        assertThat(transfer.has("targetAmount"), is(false));
        assertThat(transfer.has("targetCurrencyCode"), is(false));
        assertThat(transfer.has("exchangeRate"), is(false));
    }

    @Test
    public void aCrossCurrencyCashTransferShowsTheTargetAndTheRateOnceNotAsAUnit()
    {
        var dollars = new AccountBuilder("USD").addTo(client);
        var entry = TransactionFixtures.crossCurrencyTransfer(cash, dollars, "2026-02-01", 100_000, 108_500,
                        "0.9216589862");

        var transfer = rendered(entry.getSourceTransaction().getUUID());
        assertThat(text(transfer, "currencyCode"), is("EUR"));
        assertThat(decimal(transfer, "amount"), is("1000.00"));
        assertThat(decimal(transfer, "targetAmount"), is("1085.00"));
        assertThat(text(transfer, "targetCurrencyCode"), is("USD"));
        assertThat(decimal(transfer, "exchangeRate"), is("0.9216589862"));
        assertThat(transfer.getAsJsonArray("units").size(), is(0));
    }

    @Test
    public void aSecurityTransferIsOneRecordBetweenInvestmentAccounts()
    {
        var other = new PortfolioBuilder(cash).addTo(client);
        other.setName("Other depot");
        var entry = TransactionFixtures.securityTransfer(depot, other, acme, "2026-02-02", 3_00000000L, 30_000);

        assertThat(ids(), contains(entry.getSourceTransaction().getUUID()));

        var transfer = rendered(entry.getSourceTransaction().getUUID());
        assertThat(text(transfer, "type"), is("TRANSFER"));
        assertThat(text(transfer, "kind"), is("security"));
        assertThat(transfer.getAsJsonArray("sides").size(), is(2));
        assertThat(text(transfer, "from"), is(depot.getUUID()));
        assertThat(text(transfer, "fromName"), is("Depot"));
        assertThat(text(transfer, "to"), is(other.getUUID()));
        assertThat(text(transfer, "toName"), is("Other depot"));
        assertThat(text(transfer, "instrument"), is(acme.getUUID()));
        assertThat(text(transfer, "instrumentName"), is("ACME"));
        assertThat(decimal(transfer, "shares"), is("3.00000000"));
        assertThat(transfer.has("investmentAccount"), is(false));
    }

    @Test
    public void aForexGrossValueUnitKeepsTheShapeThatWritesAccept()
    {
        var usd = new SecurityBuilder("USD").addTo(client);
        var entry = TransactionFixtures.buy(depot, cash, usd, "2026-03-02", 10_00000000L, 100_000);
        entry.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of("EUR", 100_000), Money.of("USD", 108_500), new BigDecimal("0.9216589862")));

        var unit = rendered(entry.getPortfolioTransaction().getUUID()).getAsJsonArray("units").get(0)
                        .getAsJsonObject();
        assertThat(unit.keySet(), is(Set.of("type", "amount", "currencyCode", "forexAmount", "forexCurrencyCode",
                        "exchangeRate")));
        assertThat(text(unit, "type"), is("GROSS_VALUE"));
        assertThat(decimal(unit, "amount"), is("1000.00"));
        assertThat(decimal(unit, "forexAmount"), is("1085.00"));
        assertThat(text(unit, "forexCurrencyCode"), is("USD"));
        assertThat(decimal(unit, "exchangeRate"), is("0.9216589862"));
    }

    @Test
    public void aSideWhosePartnerIsMissingIsItsOwnRecordUnderItsRawType()
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

        assertThat(ids().size(), is(4));

        var buy = rendered(withoutCash.getPortfolioTransaction().getUUID());
        assertThat(text(buy, "type"), is("BUY"));
        assertThat(buy.getAsJsonArray("sides").size(), is(1));
        assertThat(text(buy, "investmentAccount"), is(depot.getUUID()));
        assertThat(buy.has("cashAccount"), is(false));
        assertThat(records().find(withoutCash.getAccountTransaction().getUUID()).isPresent(), is(false));

        var sell = rendered(withoutPortfolio.getAccountTransaction().getUUID());
        assertThat(text(sell, "type"), is("SELL"));
        assertThat(text(sell, "id"), is(withoutPortfolio.getAccountTransaction().getUUID()));
        assertThat(text(sell, "cashAccount"), is(cash.getUUID()));
        assertThat(sell.has("investmentAccount"), is(false));

        var out = rendered(transfer.getSourceTransaction().getUUID());
        assertThat(text(out, "type"), is("TRANSFER_OUT"));
        assertThat(out.has("kind"), is(false));
        assertThat(out.has("from"), is(false));
        assertThat(text(out, "investmentAccount"), is(depot.getUUID()));

        var in = rendered(cashTransfer.getTargetTransaction().getUUID());
        assertThat(text(in, "type"), is("TRANSFER_IN"));
        assertThat(text(in, "id"), is(cashTransfer.getTargetTransaction().getUUID()));
        assertThat(text(in, "cashAccount"), is(savings.getUUID()));
    }

    @Test
    public void aSideItsCrossEntryDoesNotKnowIsItsOwnRecord()
    {
        var savings = new AccountBuilder("EUR").addTo(client);
        var entry = TransactionFixtures.cashTransfer(cash, savings, "2026-01-04", 10_000);
        var outgoing = entry.getSourceTransaction();
        var incoming = entry.getTargetTransaction();
        // the entry forgets its outgoing side, which still points to it
        entry.setSourceTransaction(new AccountTransaction());

        assertThat(ids(), containsInAnyOrder(outgoing.getUUID(), incoming.getUUID()));

        var out = rendered(outgoing.getUUID());
        assertThat(text(out, "type"), is("TRANSFER_OUT"));
        assertThat(strings(out.getAsJsonArray("sides")), contains(outgoing.getUUID()));
        assertThat(text(out, "cashAccount"), is(cash.getUUID()));
        assertThat(out.has("kind"), is(false));

        var in = rendered(incoming.getUUID());
        assertThat(text(in, "type"), is("TRANSFER_IN"));
        assertThat(strings(in.getAsJsonArray("sides")), contains(incoming.getUUID()));
        assertThat(text(in, "cashAccount"), is(savings.getUUID()));
    }
}
