package name.abuchen.portfolio.cli.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.TransactionFixtures;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * <code>add</code> of {@link TransactionRecords}, on an in-memory client: the
 * rule table cell by cell, the rules across members, the round trip through
 * render, and the oversell check.
 */
public class TransactionAddTest
{
    private static final Gson GSON = new Gson();

    /**
     * The refusal code of each ruled member, written out here rather than
     * taken from the table: the codes are published.
     */
    private static final Map<RuleTable.Ruled, String> REFUSAL_CODES = Map.ofEntries(
                    Map.entry(RuleTable.Ruled.KIND, "kind-not-allowed"),
                    Map.entry(RuleTable.Ruled.TARGET_AMOUNT, "target-amount-not-allowed"),
                    Map.entry(RuleTable.Ruled.EXCHANGE_RATE, "exchange-rate-not-allowed"),
                    Map.entry(RuleTable.Ruled.INSTRUMENT, "instrument-not-allowed"),
                    Map.entry(RuleTable.Ruled.SHARES, "shares-not-allowed"),
                    Map.entry(RuleTable.Ruled.INVESTMENT_ACCOUNT, "investment-account-not-allowed"),
                    Map.entry(RuleTable.Ruled.CASH_ACCOUNT, "cash-account-not-allowed"),
                    Map.entry(RuleTable.Ruled.FROM, "from-not-allowed"),
                    Map.entry(RuleTable.Ruled.TO, "to-not-allowed"),
                    Map.entry(RuleTable.Ruled.EX_DATE, "exdate-not-allowed"),
                    Map.entry(RuleTable.Ruled.GROSS_AMOUNT, "gross-amount-not-allowed"),
                    Map.entry(RuleTable.Ruled.UNITS, "units-not-allowed"));

    /** Members that identify the stored sides or say when they were written, so they differ between copies. */
    private static final List<String> IDENTITY = List.of("id", "sides", "updatedAt");

    /**
     * A client with cash accounts in EUR and USD, two investment accounts, an
     * instrument in each currency, and 100 shares of the EUR one in the
     * first investment account.
     */
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
            TransactionFixtures.buy(depot, eur, acme, "2026-01-02", 100_00000000L, 1_000_000);
        }

        TransactionRecords records()
        {
            return TransactionRecords.open(client, any -> true);
        }

        Outcome add(JsonObject input)
        {
            return records().add(input);
        }

        /** Every record of the client as rendered, which a refused add must leave as it was. */
        Set<String> snapshot()
        {
            var records = records();
            return records.all().stream().map(record -> GSON.toJson(records.render(record)))
                            .collect(Collectors.toCollection(TreeSet::new));
        }

        /** An input that the row's type accepts as it is, without any of its optional members. */
        JsonObject base(RuleTable.Row row)
        {
            var json = new JsonObject();
            json.addProperty("type", row.type());
            json.addProperty("date", "2026-03-02");
            json.addProperty("amount", 10.00);
            json.addProperty("currencyCode", "EUR");
            switch (row)
            {
                case BUY, SELL -> {
                    json.addProperty("investmentAccount", depot.getUUID());
                    json.addProperty("cashAccount", eur.getUUID());
                    json.addProperty("instrument", acme.getUUID());
                    json.addProperty("shares", 1);
                }
                case DELIVERY_INBOUND, DELIVERY_OUTBOUND -> {
                    json.addProperty("investmentAccount", depot.getUUID());
                    json.addProperty("instrument", acme.getUUID());
                    json.addProperty("shares", 1);
                }
                case CASH_TRANSFER -> {
                    json.addProperty("kind", "cash");
                    json.addProperty("from", eur.getUUID());
                    json.addProperty("to", savings.getUUID());
                }
                case SECURITY_TRANSFER -> {
                    json.addProperty("kind", "security");
                    json.addProperty("from", depot.getUUID());
                    json.addProperty("to", otherDepot.getUUID());
                    json.addProperty("instrument", acme.getUUID());
                    json.addProperty("shares", 1);
                }
                case DIVIDENDS -> {
                    json.addProperty("cashAccount", eur.getUUID());
                    json.addProperty("instrument", acme.getUUID());
                }
                default -> json.addProperty("cashAccount", eur.getUUID());
            }
            return json;
        }

        /** The base input of the row with a value of the member that the row takes if it takes the member. */
        JsonObject with(RuleTable.Row row, RuleTable.Ruled member)
        {
            var json = base(row);
            var transfer = row.kind().isPresent();
            var security = row == RuleTable.Row.SECURITY_TRANSFER;
            switch (member)
            {
                case KIND -> json.addProperty("kind", row.kind().map(TransactionRecord.TransferKind::id).orElse("cash"));
                case TARGET_AMOUNT, EXCHANGE_RATE -> {
                    // a cash transfer takes them only between accounts of different currencies
                    json.addProperty("to", usd.getUUID());
                    json.addProperty("targetAmount", 11.00);
                    if (member == RuleTable.Ruled.EXCHANGE_RATE)
                        json.addProperty("exchangeRate", 0.9090909091);
                    if (row != RuleTable.Row.CASH_TRANSFER)
                    {
                        json.remove("to");
                        if (transfer)
                            json.addProperty("to", otherDepot.getUUID());
                        if (member == RuleTable.Ruled.EXCHANGE_RATE)
                            json.remove("targetAmount");
                    }
                }
                case INSTRUMENT -> json.addProperty("instrument", acme.getUUID());
                case SHARES -> json.addProperty("shares", 1);
                case INVESTMENT_ACCOUNT -> json.addProperty("investmentAccount", depot.getUUID());
                case CASH_ACCOUNT -> json.addProperty("cashAccount", eur.getUUID());
                case FROM -> json.addProperty("from", security ? depot.getUUID() : eur.getUUID());
                case TO -> json.addProperty("to", security ? otherDepot.getUUID() : savings.getUUID());
                case EX_DATE -> json.addProperty("exDate", "2026-03-01");
                case GROSS_AMOUNT -> json.addProperty("grossAmount", 10.00);
                case UNITS -> json.add("units", new JsonArray());
            }
            return json;
        }
    }

    private static Set<String> failures(Outcome outcome)
    {
        assertThat(String.valueOf(outcome), outcome, instanceOf(Outcome.Refused.class));
        return ((Outcome.Refused) outcome).failures().stream().map(f -> f.field() + "/" + f.code())
                        .collect(Collectors.toSet());
    }

    private static JsonObject after(Outcome outcome)
    {
        assertThat(String.valueOf(outcome), outcome, instanceOf(Outcome.Applied.class));
        return GSON.toJsonTree(((Outcome.Applied) outcome).after()).getAsJsonObject();
    }

    private static JsonObject json(String text)
    {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    public void everyCellOfTheRuleTableIsEnforced()
    {
        for (var row : RuleTable.Row.values())
        {
            var applied = new Fixture();
            after(applied.add(applied.base(row)));

            for (var member : RuleTable.Ruled.values())
            {
                var cell = row + " x " + member.id() + ": ";
                switch (row.rule(member))
                {
                    case REFUSED -> {
                        var fixture = new Fixture();
                        var before = fixture.snapshot();
                        var outcome = fixture.add(fixture.with(row, member));
                        assertThat(cell, failures(outcome), is(Set.of(member.id() + "/" + REFUSAL_CODES.get(member))));
                        assertThat(cell + "model untouched", fixture.snapshot(), is(before));
                    }
                    case REQUIRED -> {
                        var fixture = new Fixture();
                        var before = fixture.snapshot();
                        var input = fixture.base(row);
                        input.remove(member.id());
                        assertThat(cell, failures(fixture.add(input)), is(Set.of(member.id() + "/required")));
                        assertThat(cell + "model untouched", fixture.snapshot(), is(before));
                    }
                    case OPTIONAL -> {
                        var fixture = new Fixture();
                        var outcome = fixture.add(fixture.with(row, member));
                        assertThat(cell + outcome, outcome, instanceOf(Outcome.Applied.class));
                    }
                }
            }
        }
    }

    @Test
    public void aRefusalNamesTheTypeAndTheKindOfATransfer()
    {
        var fixture = new Fixture();

        var outcome = (Outcome.Refused) fixture.add(fixture.with(RuleTable.Row.CASH_TRANSFER,
                        RuleTable.Ruled.INSTRUMENT));

        assertThat(outcome.failures().get(0).message(), is("TRANSFER of kind cash must not have instrument."));
    }

    @Test
    public void aTransferWithoutAKnownKindIsRefusedOnlyWhatEveryTransferRefuses()
    {
        var fixture = new Fixture();
        var input = fixture.base(RuleTable.Row.SECURITY_TRANSFER);
        input.addProperty("kind", "gold");
        input.addProperty("cashAccount", fixture.eur.getUUID());

        assertThat(failures(fixture.add(input)), is(Set.of("kind/unsupported-kind", "cashAccount/cash-account-not-allowed")));
    }

    @Test
    public void aMissingOrUnknownTypeIsRefusedWithTheMembersEveryTypeTakes()
    {
        var fixture = new Fixture();

        assertThat(failures(fixture.add(json("{\"date\":\"2026-03-02\",\"amount\":1,\"currencyCode\":\"EUR\"}"))),
                        is(Set.of("type/required")));
        assertThat(failures(fixture.add(json("{\"type\":\"SPLIT\",\"date\":\"03/02\",\"amount\":-1,"
                        + "\"currencyCode\":\"EUR\"}"))),
                        is(Set.of("type/unsupported-type", "date/invalid-date", "amount/not-positive")));
    }

    @Test
    public void anUnknownMemberIsRefused()
    {
        var fixture = new Fixture();
        var input = fixture.base(RuleTable.Row.DEPOSIT);
        input.addProperty("colour", "red");

        assertThat(failures(fixture.add(input)), is(Set.of("colour/unknown-field")));
    }

    @Test
    public void aCrossCurrencyCashTransferNeedsATargetAmountAndStoresTheDerivedRate()
    {
        var fixture = new Fixture();
        var input = fixture.base(RuleTable.Row.CASH_TRANSFER);
        input.addProperty("to", fixture.usd.getUUID());

        assertThat(failures(fixture.add(input)), is(Set.of("targetAmount/required")));

        input.addProperty("targetAmount", 11.00);
        var after = after(fixture.add(input));
        assertThat(after.get("targetAmount").getAsBigDecimal().toPlainString(), is("11.00"));
        assertThat(after.get("targetCurrencyCode").getAsString(), is("USD"));
        assertThat(after.get("exchangeRate").getAsBigDecimal().toPlainString(), is("0.9090909091"));
    }

    @Test
    public void aSuppliedTransferRateIsOnlyCheckedAgainstTheTolerance()
    {
        var fixture = new Fixture();
        var input = fixture.with(RuleTable.Row.CASH_TRANSFER, RuleTable.Ruled.EXCHANGE_RATE);
        input.addProperty("exchangeRate", 0.9091);
        assertThat(after(fixture.add(input)).get("exchangeRate").getAsBigDecimal().toPlainString(),
                        is("0.9090909091"));

        input.addProperty("exchangeRate", 0.8);
        assertThat(failures(fixture.add(input)), is(Set.of("exchangeRate/forex-mismatch")));
        input.addProperty("exchangeRate", 0);
        assertThat(failures(fixture.add(input)), is(Set.of("exchangeRate/not-positive")));
    }

    @Test
    public void aSameCurrencyCashTransferRefusesATargetAmountAndARate()
    {
        var fixture = new Fixture();
        var input = fixture.base(RuleTable.Row.CASH_TRANSFER);
        input.addProperty("targetAmount", 10.00);
        input.addProperty("exchangeRate", 1);

        assertThat(failures(fixture.add(input)),
                        is(Set.of("targetAmount/target-amount-not-allowed", "exchangeRate/exchange-rate-not-allowed")));
    }

    @Test
    public void aTransferNeedsTwoDifferentAccounts()
    {
        for (var row : List.of(RuleTable.Row.CASH_TRANSFER, RuleTable.Row.SECURITY_TRANSFER))
        {
            var fixture = new Fixture();
            var input = fixture.base(row);
            input.add("to", input.get("from"));

            assertThat(row.name(), failures(fixture.add(input)), is(Set.of("to/same-account")));
        }
    }

    @Test
    public void transferAmountsAreInTheCurrencyOfFromOrOfTheInstrument()
    {
        var fixture = new Fixture();

        var cash = fixture.base(RuleTable.Row.CASH_TRANSFER);
        cash.addProperty("currencyCode", "USD");
        assertThat(failures(fixture.add(cash)), is(Set.of("currencyCode/currency-mismatch")));

        var security = fixture.base(RuleTable.Row.SECURITY_TRANSFER);
        security.addProperty("currencyCode", "USD");
        assertThat(failures(fixture.add(security)), is(Set.of("currencyCode/currency-mismatch")));
    }

    @Test
    public void anInstrumentInAnotherCurrencyNeedsAForexGrossValue()
    {
        var fixture = new Fixture();
        var dividend = fixture.base(RuleTable.Row.DIVIDENDS);
        dividend.addProperty("instrument", fixture.dollarStock.getUUID());
        assertThat(failures(fixture.add(dividend)), is(Set.of("units/forex-gross-value-required")));

        dividend.add("units", JsonParser.parseString("[{\"type\":\"GROSS_VALUE\",\"amount\":10.00,"
                        + "\"currencyCode\":\"EUR\",\"forexAmount\":11.00,\"forexCurrencyCode\":\"USD\","
                        + "\"exchangeRate\":0.9090909091}]"));
        assertThat(after(fixture.add(dividend)).getAsJsonArray("units").size(), is(1));

        var delivery = fixture.base(RuleTable.Row.DELIVERY_INBOUND);
        delivery.addProperty("currencyCode", "USD");
        assertThat(failures(fixture.add(delivery)), is(Set.of("units/forex-gross-value-required")));
    }

    @Test
    public void forexUnitsAreValidated()
    {
        var fixture = new Fixture();
        var buy = fixture.base(RuleTable.Row.BUY);
        buy.addProperty("instrument", fixture.dollarStock.getUUID());

        buy.add("units", JsonParser.parseString("[{\"type\":\"GROSS_VALUE\",\"amount\":10.00,\"currencyCode\":\"EUR\","
                        + "\"forexAmount\":11.00,\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.5}]"));
        assertThat(failures(fixture.add(buy)), is(Set.of("units[0]/forex-mismatch", "units/forex-gross-value-required")));

        buy.add("units", JsonParser.parseString("[{\"type\":\"GROSS_VALUE\",\"amount\":10.00,\"currencyCode\":\"EUR\","
                        + "\"forexAmount\":11.00,\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.9090909091},"
                        + "{\"type\":\"FEE\",\"amount\":1.00,\"currencyCode\":\"EUR\",\"forexAmount\":0.80,"
                        + "\"forexCurrencyCode\":\"GBP\",\"exchangeRate\":1.25}]"));
        assertThat(failures(fixture.add(buy)), is(Set.of("units[1].forexCurrencyCode/forex-currency-mismatch")));

        buy.add("units", JsonParser.parseString("[{\"type\":\"GROSS_VALUE\",\"amount\":10.00,\"currencyCode\":\"EUR\"},"
                        + "{\"type\":\"BONUS\",\"amount\":1.00,\"currencyCode\":\"EUR\"}]"));
        assertThat(failures(fixture.add(buy)), is(Set.of("units[0]/forex-required", "units[1].type/unsupported-unit-type",
                        "units/forex-gross-value-required")));
    }

    @Test
    public void aGrossAmountMustReconcileWithTheAmountFeesAndTaxes()
    {
        var fixture = new Fixture();
        var dividend = fixture.base(RuleTable.Row.DIVIDENDS);
        dividend.addProperty("amount", 80.00);
        dividend.add("units", JsonParser.parseString("[{\"type\":\"TAX\",\"amount\":15.00,\"currencyCode\":\"EUR\"},"
                        + "{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]"));
        dividend.addProperty("grossAmount", 99.00);
        assertThat(failures(fixture.add(dividend)), is(Set.of("grossAmount/gross-amount-mismatch")));
        dividend.addProperty("grossAmount", 100.00);
        after(fixture.add(dividend));

        // a buy's gross amount is the amount minus fees and taxes
        var buy = fixture.base(RuleTable.Row.BUY);
        buy.addProperty("amount", 1005.00);
        buy.add("units", JsonParser.parseString("[{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]"));
        buy.addProperty("grossAmount", 1005.00);
        assertThat(failures(fixture.add(buy)), is(Set.of("grossAmount/gross-amount-mismatch")));
        buy.addProperty("grossAmount", 1000.00);
        after(fixture.add(buy));
    }

    @Test
    public void anExDateIsAnIsoDateNeverAfterThePayment()
    {
        var fixture = new Fixture();
        var dividend = fixture.base(RuleTable.Row.DIVIDENDS);

        dividend.addProperty("exDate", "2026-03-03");
        assertThat(failures(fixture.add(dividend)), is(Set.of("exDate/exdate-after-payment")));
        dividend.addProperty("exDate", "25.02.2026");
        assertThat(failures(fixture.add(dividend)), is(Set.of("exDate/invalid-date")));
        dividend.addProperty("exDate", 20260225);
        assertThat(failures(fixture.add(dividend)), is(Set.of("exDate/invalid-type")));
    }

    @Test
    public void theCashAccountsCurrencyIsTheAmountsCurrency()
    {
        var fixture = new Fixture();
        var deposit = fixture.base(RuleTable.Row.DEPOSIT);
        deposit.addProperty("currencyCode", "USD");

        assertThat(failures(fixture.add(deposit)), is(Set.of("currencyCode/currency-mismatch")));
    }

    @Test
    public void coreImportChecksRunAsASafetyNet()
    {
        var fixture = new Fixture();
        // core's CheckValidTypesAction allows taxes only on dividends and interest
        var charge = fixture.base(RuleTable.Row.INTEREST_CHARGE);
        charge.add("units", JsonParser.parseString("[{\"type\":\"TAX\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]"));
        var before = fixture.snapshot();

        var refused = (Outcome.Refused) fixture.add(charge);

        assertThat(refused.failures().size(), is(1));
        assertThat(refused.failures().get(0).code(), is("import-check-failed"));
        assertThat(refused.failures().get(0).field(), is(""));
        assertThat(refused.failures().get(0).check(), is("CheckValidTypesAction"));
        assertThat(fixture.snapshot(), is(before));
    }

    @Test
    public void theAppliedRecordIsTheOneNowInTheModel()
    {
        var fixture = new Fixture();

        var outcome = (Outcome.Applied) fixture.add(fixture.base(RuleTable.Row.BUY));

        assertThat(outcome.kind(), is(Outcome.Entity.TRANSACTION));
        assertThat(outcome.before() == null, is(true));
        var records = fixture.records();
        var stored = records.render(records.find(outcome.recordId()).orElseThrow());
        assertThat(GSON.toJsonTree(outcome.after()), is(GSON.toJsonTree(stored)));
        assertThat(fixture.depot.getTransactions().size(), is(2));
        assertThat(fixture.eur.getTransactions().size(), is(2));
    }

    /** One record of each row, with the optional members it takes. */
    private static List<JsonObject> everyType(Fixture fixture)
    {
        var inputs = new ArrayList<JsonObject>();
        for (var row : RuleTable.Row.values())
        {
            var input = fixture.base(row);
            input.addProperty("time", "09:30");
            input.addProperty("note", "note of " + row);
            input.addProperty("source", "statement.pdf");
            inputs.add(input);
        }

        var dividend = fixture.base(RuleTable.Row.DIVIDENDS);
        dividend.addProperty("instrument", fixture.dollarStock.getUUID());
        dividend.addProperty("amount", 9.00);
        dividend.addProperty("shares", 4);
        dividend.addProperty("exDate", "2026-02-25");
        dividend.add("units", JsonParser.parseString("[{\"type\":\"GROSS_VALUE\",\"amount\":10.00,"
                        + "\"currencyCode\":\"EUR\",\"forexAmount\":12.50,\"forexCurrencyCode\":\"USD\","
                        + "\"exchangeRate\":0.8},{\"type\":\"TAX\",\"amount\":1.00,\"currencyCode\":\"EUR\"}]"));
        inputs.add(dividend);

        var buy = fixture.base(RuleTable.Row.BUY);
        buy.add("units", JsonParser.parseString("[{\"type\":\"FEE\",\"amount\":0.50,\"currencyCode\":\"EUR\"}]"));
        inputs.add(buy);

        inputs.add(fixture.with(RuleTable.Row.CASH_TRANSFER, RuleTable.Ruled.TARGET_AMOUNT));
        return inputs;
    }

    /** The rendered record as add input: the members an add declares, without an empty list of units. */
    private static JsonObject asInput(JsonObject record)
    {
        var input = new JsonObject();
        for (var member : record.entrySet())
        {
            var value = member.getValue();
            if (TransactionRecords.addSchema().declares(member.getKey())
                            && !(value.isJsonArray() && value.getAsJsonArray().isEmpty()))
                input.add(member.getKey(), value);
        }
        return input;
    }

    private static JsonObject withoutIdentity(JsonObject record)
    {
        var copy = record.deepCopy();
        IDENTITY.forEach(copy::remove);
        return copy;
    }

    @Test
    public void everyTypeOfRecordRenderedAndAddedAgainRendersTheSame()
    {
        var fixture = new Fixture();

        for (var input : everyType(fixture))
        {
            var original = after(fixture.add(input));
            var copy = after(fixture.add(asInput(original)));

            assertThat(original.get("type").getAsString(), withoutIdentity(copy), is(withoutIdentity(original)));
        }
    }

    private static JsonObject trade(Fixture fixture, String type, Security security, String date, String shares)
    {
        var input = fixture.base(RuleTable.Row.valueOf(type));
        input.addProperty("instrument", security.getUUID());
        input.addProperty("date", date);
        input.add("shares", JsonParser.parseString(shares));
        return input;
    }

    @Test
    public void aSellThatWouldMakeTheHoldingNegativeIsRefused()
    {
        var fixture = new Fixture();
        var before = fixture.snapshot();

        var refused = (Outcome.Refused) fixture.add(trade(fixture, "SELL", fixture.acme, "2026-03-09", "100.5"));

        assertThat(failures(refused), is(Set.of("shares/negative-holding")));
        var message = refused.failures().get(0).message();
        assertThat(message, containsString("Depot"));
        assertThat(message, containsString("ACME"));
        assertThat(message, containsString("2026-03-09"));
        assertThat(message, containsString("0.5"));
        assertThat(fixture.snapshot(), is(before));
    }

    @Test
    public void anOversellIsRefusedEvenIfALaterBuyRestoresTheHoldingOrItStarvesALaterSell()
    {
        var fixture = new Fixture();
        var beta = new SecurityBuilder("EUR").addTo(fixture.client);
        after(fixture.add(trade(fixture, "BUY", beta, "2026-03-20", "10")));
        assertThat(failures(fixture.add(trade(fixture, "SELL", beta, "2026-03-09", "5"))),
                        is(Set.of("shares/negative-holding")));

        after(fixture.add(trade(fixture, "SELL", fixture.acme, "2026-03-20", "80")));
        var refused = (Outcome.Refused) fixture.add(trade(fixture, "SELL", fixture.acme, "2026-03-09", "25"));
        assertThat(refused.failures().get(0).message(), containsString("2026-03-20"));
    }

    @Test
    public void theHoldingIsJudgedAtTheEndOfTheDay()
    {
        var fixture = new Fixture();
        var beta = new SecurityBuilder("EUR").addTo(fixture.client);
        var buy = trade(fixture, "BUY", beta, "2026-03-09", "10");
        buy.addProperty("time", "15:00");
        after(fixture.add(buy));

        var sell = trade(fixture, "SELL", beta, "2026-03-09", "10");
        sell.addProperty("time", "09:00");
        after(fixture.add(sell));
    }

    @Test
    public void anOutboundDeliveryAndASecurityTransferThatOversellAreRefused()
    {
        var fixture = new Fixture();

        var delivery = fixture.base(RuleTable.Row.DELIVERY_OUTBOUND);
        delivery.addProperty("shares", 101);
        assertThat(failures(fixture.add(delivery)), is(Set.of("shares/negative-holding")));

        var transfer = fixture.base(RuleTable.Row.SECURITY_TRANSFER);
        transfer.addProperty("shares", 101);
        var refused = (Outcome.Refused) fixture.add(transfer);
        assertThat(failures(refused), is(Set.of("shares/negative-holding")));
        assertThat(refused.failures().get(0).message(), containsString("Depot"));
    }

    @Test
    public void anOversellAlreadyInTheFileOnlyBlocksWritesThatMakeItWorse()
    {
        var fixture = new Fixture();
        var beta = new SecurityBuilder("EUR").addTo(fixture.client);
        TransactionFixtures.sell(fixture.depot, fixture.eur, beta, "2026-03-02", 5_00000000L, 50_000);

        // a buy of the oversold instrument and a trade of another one leave it no worse
        after(fixture.add(trade(fixture, "BUY", beta, "2026-03-09", "2")));
        after(fixture.add(trade(fixture, "SELL", fixture.acme, "2026-03-09", "2")));

        // but selling more of it makes it worse
        assertThat(failures(fixture.add(trade(fixture, "SELL", beta, "2026-03-16", "1"))),
                        is(Set.of("shares/negative-holding")));
    }
}
