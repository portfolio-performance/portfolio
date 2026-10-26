package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.TransactionFixtures;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * The <code>transaction.add</code> operation for buys, sells, transfers and
 * deliveries.
 */
public class TransactionAddTradesTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private String eurAccount;
    private String eurSavings;
    private String usdAccount;
    private String depot;
    private String otherDepot;
    private String eurInstrument;
    private String usdInstrument;

    @Before
    public void setUp() throws IOException
    {
        var client = new Client();
        var eur = cashAccount(client, "EUR Cash", "EUR");
        var usd = cashAccount(client, "USD Cash", "USD");
        var savings = cashAccount(client, "EUR Savings", "EUR");
        var first = investmentAccount(client, "Depot", eur);
        var second = investmentAccount(client, "Other depot", eur);
        var eurSecurity = instrument(client, "EUR Stock", "EUR");
        var usdSecurity = instrument(client, "USD Stock", "USD");

        eurAccount = eur.getUUID();
        eurSavings = savings.getUUID();
        usdAccount = usd.getUUID();
        depot = first.getUUID();
        otherDepot = second.getUUID();
        eurInstrument = eurSecurity.getUUID();
        usdInstrument = usdSecurity.getUUID();

        cli.save(client);
    }

    private static Account cashAccount(Client client, String name, String currencyCode)
    {
        var account = new Account();
        account.setName(name);
        account.setCurrencyCode(currencyCode);
        client.addAccount(account);
        return account;
    }

    private static Portfolio investmentAccount(Client client, String name, Account reference)
    {
        var portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setReferenceAccount(reference);
        client.addPortfolio(portfolio);
        return portfolio;
    }

    private static Security instrument(Client client, String name, String currencyCode)
    {
        var security = new Security();
        security.setName(name);
        security.setCurrencyCode(currencyCode);
        client.addSecurity(security);
        return security;
    }

    private JsonObject onlyRecord()
    {
        var run = cli.run("transactions");
        assertThat(run.stdout, run.exitCode, is(0));
        var items = run.items();
        assertThat(items.size(), is(1));
        return items.get(0).getAsJsonObject();
    }

    private String buy(String type, String date, String instrument, String shares, String amount)
    {
        return "{\"operation\":\"transaction.add\",\"type\":\"" + type + "\",\"investmentAccount\":\"" + depot
                        + "\",\"cashAccount\":\"" + eurAccount + "\",\"date\":\"" + date + "\",\"instrument\":\""
                        + instrument + "\",\"shares\":" + shares + ",\"amount\":" + amount
                        + ",\"currencyCode\":\"EUR\"}";
    }

    @Test
    public void addsABuyAsOneRecordWithBothSides() throws IOException
    {
        var run = cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1005.00"));

        assertThat(run.stdout, run.exitCode, is(0));

        var created = run.created();
        assertThat(created.get("entity").getAsString(), is("transaction"));
        var after = created.getAsJsonObject("after");
        assertThat(after.get("type").getAsString(), is("BUY"));
        assertThat(after.get("investmentAccount").getAsString(), is(depot));
        assertThat(after.get("cashAccount").getAsString(), is(eurAccount));
        assertThat(after.getAsJsonArray("sides").size(), is(2));
        assertThat(created.get("uuid").getAsString(), is(after.get("id").getAsString()));

        var record = onlyRecord();
        assertThat(record, is(after));
        assertThat(record.get("amount").getAsBigDecimal().doubleValue(), is(1005.00));
        assertThat(record.get("shares").getAsBigDecimal().doubleValue(), is(10.0));
        assertThat(record.get("instrument").getAsString(), is(eurInstrument));

        var client = cli.reload();
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
    }

    @Test
    public void addsASellWithFeesAndTaxesAndCrossChecksTheGrossAmount() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1000.00"));

        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"SELL\",\"investmentAccount\":\"" + depot
                        + "\",\"cashAccount\":\"" + eurAccount + "\",\"date\":\"2026-03-09\",\"instrument\":\""
                        + eurInstrument + "\",\"shares\":4,\"amount\":450.00,\"currencyCode\":\"EUR\","
                        + "\"grossAmount\":460.00,\"units\":[{\"type\":\"FEE\",\"amount\":6.00,"
                        + "\"currencyCode\":\"EUR\"},{\"type\":\"TAX\",\"amount\":4.00,\"currencyCode\":\"EUR\"}]}");

        assertThat(run.stdout, run.exitCode, is(0));

        var after = run.created().getAsJsonObject("after");
        assertThat(after.get("type").getAsString(), is("SELL"));
        assertThat(after.getAsJsonArray("units").size(), is(2));
    }

    @Test
    public void refusesAGrossAmountThatDoesNotReconcile() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"BUY\",\"investmentAccount\":\"" + depot
                        + "\",\"cashAccount\":\"" + eurAccount + "\",\"date\":\"2026-03-02\",\"instrument\":\""
                        + eurInstrument + "\",\"shares\":10,\"amount\":1005.00,\"currencyCode\":\"EUR\","
                        + "\"grossAmount\":1005.00,\"units\":[{\"type\":\"FEE\",\"amount\":5.00,"
                        + "\"currencyCode\":\"EUR\"}]}");

        run.assertRefused("grossAmount", "gross-amount-mismatch");
    }

    @Test
    public void refusesABuyWithoutAnInvestmentAccountOrACashAccount() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"BUY\",\"date\":\"2026-03-02\","
                        + "\"instrument\":\"" + eurInstrument + "\",\"shares\":10,\"amount\":100.00,"
                        + "\"currencyCode\":\"EUR\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failure(0).get("field").getAsString(), is("investmentAccount"));
        assertThat(run.failure(0).get("code").getAsString(), is("required"));
        assertThat(run.failure(1).get("field").getAsString(), is("cashAccount"));
        assertThat(run.failure(1).get("code").getAsString(), is("required"));
    }

    @Test
    public void refusesACurrencyThatIsNotTheCashAccounts() throws IOException
    {
        var run = cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "100.00").replace("\"EUR\"}", "\"USD\"}"));

        run.assertRefused("currencyCode", "currency-mismatch");
    }

    @Test
    public void refusesNonPositiveSharesAndAnInstrumentThatIsMissing() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"BUY\",\"investmentAccount\":\"" + depot
                        + "\",\"cashAccount\":\"" + eurAccount + "\",\"date\":\"2026-03-02\",\"shares\":0,"
                        + "\"amount\":100.00,\"currencyCode\":\"EUR\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failures().toString(),
                        containsString("\"field\":\"instrument\",\"code\":\"required\""));
        assertThat(run.failures().toString(),
                        containsString("\"field\":\"shares\",\"code\":\"not-positive\""));
    }

    @Test
    public void refusesMembersThatOnlyTransfersTake() throws IOException
    {
        var run = cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "100.00").replace("\"date\"",
                        "\"targetAmount\":90.00,\"date\""));

        run.assertRefused("targetAmount", "target-amount-not-allowed");
    }

    private String delivery(String type, String date, String instrument, String shares, String amount,
                    String currencyCode)
    {
        return "{\"operation\":\"transaction.add\",\"type\":\"" + type + "\",\"investmentAccount\":\"" + depot
                        + "\",\"date\":\"" + date + "\",\"instrument\":\"" + instrument + "\",\"shares\":" + shares
                        + ",\"amount\":" + amount + ",\"currencyCode\":\"" + currencyCode + "\"}";
    }

    @Test
    public void addsInboundAndOutboundDeliveriesAsOneSidedRecords() throws IOException
    {
        var inbound = cli.apply(delivery("DELIVERY_INBOUND", "2026-03-02", eurInstrument, "10", "1000.00", "EUR"));
        assertThat(inbound.stdout, inbound.exitCode, is(0));
        var outbound = cli.apply(delivery("DELIVERY_OUTBOUND", "2026-03-09", eurInstrument, "4", "420.00", "EUR"));
        assertThat(outbound.stdout, outbound.exitCode, is(0));

        var after = inbound.created().getAsJsonObject("after");
        assertThat(after.get("type").getAsString(), is("DELIVERY_INBOUND"));
        assertThat(after.getAsJsonArray("sides").size(), is(1));
        assertThat(after.has("cashAccount"), is(false));
        assertThat(inbound.created().get("uuid").getAsString(), is(after.get("id").getAsString()));

        var client = cli.reload();
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(2));
        assertThat(client.getAccounts().get(0).getTransactions().isEmpty(), is(true));

        var read = cli.run("transactions", "--type", "DELIVERY_OUTBOUND");
        var items = read.items();
        assertThat(items.get(0).getAsJsonObject(), is(outbound.created().getAsJsonObject("after")));
    }

    @Test
    public void refusesACashAccountOnADelivery() throws IOException
    {
        var run = cli.apply(delivery("DELIVERY_INBOUND", "2026-03-02", eurInstrument, "10", "1000.00", "EUR")
                        .replace("\"date\"", "\"cashAccount\":\"" + eurAccount + "\",\"date\""));

        run.assertRefused("cashAccount", "cash-account-not-allowed");
    }

    @Test
    public void acceptsADeliveryInAnotherCurrencyOnlyWithAGrossValueUnit() throws IOException
    {
        var without = cli.apply(delivery("DELIVERY_INBOUND", "2026-03-02", eurInstrument, "10", "1100.00", "USD"));
        without.assertRefused("units", "forex-gross-value-required");

        var with = cli.apply(delivery("DELIVERY_INBOUND", "2026-03-02", eurInstrument, "10", "1100.00", "USD")
                        .replace("}", ",\"units\":[{\"type\":\"GROSS_VALUE\",\"amount\":1100.00,"
                                        + "\"currencyCode\":\"USD\",\"forexAmount\":1000.00,"
                                        + "\"forexCurrencyCode\":\"EUR\",\"exchangeRate\":1.1}]}"));
        assertThat(with.stdout, with.exitCode, is(0));
    }

    private String usdBuy(String gross, String forexAmount, String rate)
    {
        var base = buy("BUY", "2026-03-02", usdInstrument, "10", "1000.00");
        return gross == null ? base
                        : base.replace("}", ",\"units\":[{\"type\":\"GROSS_VALUE\",\"amount\":" + gross
                                        + ",\"currencyCode\":\"EUR\",\"forexAmount\":" + forexAmount
                                        + ",\"forexCurrencyCode\":\"USD\",\"exchangeRate\":" + rate + "}]}");
    }

    @Test
    public void acceptsAForexBuyWithAConsistentGrossValueUnit() throws IOException
    {
        var run = cli.apply(usdBuy("1000.00", "1100.00", "0.9090909091"));

        assertThat(run.stdout, run.exitCode, is(0));

        var units = run.created().getAsJsonObject("after").getAsJsonArray("units");
        assertThat(units.size(), is(1));
        assertThat(units.get(0).getAsJsonObject().get("forexCurrencyCode").getAsString(), is("USD"));
    }

    @Test
    public void refusesAForexBuyWithAnInconsistentRate() throws IOException
    {
        var run = cli.apply(usdBuy("1000.00", "1100.00", "0.5"));

        run.assertRefused("units[0]", "forex-mismatch");
    }

    @Test
    public void refusesAForexBuyWithoutAGrossValueUnit() throws IOException
    {
        var run = cli.apply(usdBuy(null, null, null));

        run.assertRefused("units", "forex-gross-value-required");
    }

    @Test
    public void refusesAFeeWithForexInAnotherCurrencyThanTheInstruments() throws IOException
    {
        var run = cli.apply(usdBuy("1000.00", "1100.00", "0.9090909091").replace("}]}",
                        "},{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\",\"forexAmount\":4.00,"
                                        + "\"forexCurrencyCode\":\"GBP\",\"exchangeRate\":1.25}]}"));

        run.assertRefused("units[1].forexCurrencyCode", "forex-currency-mismatch");
    }

    @Test
    public void acceptsAFeeWithForexInTheInstrumentsCurrency() throws IOException
    {
        var run = cli.apply(usdBuy("995.00", "1094.50", "0.9090909091").replace("}]}",
                        "},{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\",\"forexAmount\":5.50,"
                                        + "\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.9090909091}]}"));

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void refusesAGrossValueUnitThatDoesNotMatchTheAmount() throws IOException
    {
        var run = cli.apply(usdBuy("900.00", "990.00", "0.9090909091"));

        run.assertRefused("units", "import-check-failed");
    }

    private String securityTransfer(String from, String to, String amount)
    {
        return "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"security\",\"from\":\"" + from
                        + "\",\"to\":\"" + to + "\",\"date\":\"2026-03-09\",\"instrument\":\"" + eurInstrument
                        + "\",\"shares\":6,\"amount\":" + amount + ",\"currencyCode\":\"EUR\"}";
    }

    private String cashTransfer(String from, String to, String amount, String currencyCode, String more)
    {
        return "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\"" + from
                        + "\",\"to\":\"" + to + "\",\"date\":\"2026-03-09\",\"amount\":" + amount
                        + ",\"currencyCode\":\"" + currencyCode + "\"" + more + "}";
    }

    @Test
    public void addsASecurityTransferAsOneRecordWithBothSides() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1000.00"));

        var run = cli.apply(securityTransfer(depot, otherDepot, "620.00"));

        assertThat(run.stdout, run.exitCode, is(0));

        var after = run.created().getAsJsonObject("after");
        assertThat(after.get("type").getAsString(), is("TRANSFER"));
        assertThat(after.get("kind").getAsString(), is("security"));
        assertThat(after.get("from").getAsString(), is(depot));
        assertThat(after.get("to").getAsString(), is(otherDepot));
        assertThat(after.getAsJsonArray("sides").size(), is(2));
        assertThat(run.created().get("uuid").getAsString(), is(after.get("id").getAsString()));

        var read = cli.run("transactions", "--type", "TRANSFER");
        var items = read.items();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject(), is(after));

        var client = cli.reload();
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(2));
        assertThat(client.getPortfolios().get(1).getTransactions().size(), is(1));
    }

    @Test
    public void refusesASecurityTransferWithTheWrongShape() throws IOException
    {
        var sameAccount = cli.apply(securityTransfer(depot, depot, "620.00"));
        sameAccount.assertRefused("to", "same-account");

        var accountMember = cli.apply(securityTransfer(depot, otherDepot, "620.00").replace("\"date\"",
                        "\"investmentAccount\":\"" + depot + "\",\"date\""));
        accountMember.assertRefused("investmentAccount", "investment-account-not-allowed");

        var units = cli.apply(securityTransfer(depot, otherDepot, "620.00").replace("}", ",\"units\":[]}"));
        units.assertRefused("units", "units-not-allowed");

        var gross = cli.apply(securityTransfer(depot, otherDepot, "620.00").replace("}", ",\"grossAmount\":620.00}"));
        gross.assertRefused("grossAmount", "gross-amount-not-allowed");

        var currency = cli.apply(securityTransfer(depot, otherDepot, "620.00").replace("\"EUR\"}", "\"USD\"}"));
        currency.assertRefused("currencyCode", "currency-mismatch");

        var unknown = cli.apply(securityTransfer(depot, "does-not-exist", "620.00"));
        unknown.assertRefused("to", "reference-not-found");
    }

    @Test
    public void addsASameCurrencyCashTransfer() throws IOException
    {
        var run = cli.apply(cashTransfer(eurAccount, eurSavings, "250.00", "EUR", ""));

        assertThat(run.stdout, run.exitCode, is(0));

        var after = run.created().getAsJsonObject("after");
        assertThat(after.get("kind").getAsString(), is("cash"));
        assertThat(after.get("from").getAsString(), is(eurAccount));
        assertThat(after.has("targetAmount"), is(false));
        assertThat(after.has("exchangeRate"), is(false));
        assertThat(after.getAsJsonArray("sides").size(), is(2));

        var read = cli.run("transactions", "--type", "TRANSFER");
        var items = read.items();
        assertThat(items.get(0).getAsJsonObject(), is(after));
    }

    @Test
    public void addsACrossCurrencyCashTransferAndEchoesTheDerivedRate() throws IOException
    {
        var run = cli.apply(cashTransfer(eurAccount, usdAccount, "100.00", "EUR", ",\"targetAmount\":110.00"));

        assertThat(run.stdout, run.exitCode, is(0));

        var after = run.created().getAsJsonObject("after");
        assertThat(after.get("targetAmount").getAsBigDecimal().doubleValue(), is(110.00));
        assertThat(after.get("targetCurrencyCode").getAsString(), is("USD"));
        assertThat(after.get("exchangeRate").getAsBigDecimal().toPlainString(), is("0.9090909091"));

        var read = cli.run("transactions", "--type", "TRANSFER");
        var items = read.items();
        assertThat(items.get(0).getAsJsonObject(), is(after));
    }

    @Test
    public void checksASuppliedRateAgainstTheTolerance() throws IOException
    {
        var good = cli.apply(cashTransfer(eurAccount, usdAccount, "100.00", "EUR",
                        ",\"targetAmount\":110.00,\"exchangeRate\":0.9091"));
        assertThat(good.stdout, good.exitCode, is(0));

        var bad = cli.apply(cashTransfer(eurAccount, usdAccount, "100.00", "EUR",
                        ",\"targetAmount\":110.00,\"exchangeRate\":0.8"));
        bad.assertRefused("exchangeRate", "forex-mismatch");
    }

    @Test
    public void refusesACashTransferWithTheWrongShape() throws IOException
    {
        var sameCurrencyTarget = cli.apply(cashTransfer(eurAccount, eurSavings, "250.00", "EUR",
                        ",\"targetAmount\":250.00"));
        sameCurrencyTarget.assertRefused("targetAmount", "target-amount-not-allowed");

        var sameCurrencyRate = cli.apply(cashTransfer(eurAccount, eurSavings, "250.00", "EUR",
                        ",\"exchangeRate\":1"));
        sameCurrencyRate.assertRefused("exchangeRate", "exchange-rate-not-allowed");

        var missingTarget = cli.apply(cashTransfer(eurAccount, usdAccount, "100.00", "EUR", ""));
        missingTarget.assertRefused("targetAmount", "required");

        var currency = cli.apply(cashTransfer(eurAccount, eurSavings, "250.00", "USD", ""));
        currency.assertRefused("currencyCode", "currency-mismatch");

        var instrument = cli.apply(cashTransfer(eurAccount, eurSavings, "250.00", "EUR",
                        ",\"instrument\":\"" + eurInstrument + "\""));
        instrument.assertRefused("instrument", "instrument-not-allowed");

        var sameAccount = cli.apply(cashTransfer(eurAccount, eurAccount, "250.00", "EUR", ""));
        sameAccount.assertRefused("to", "same-account");
    }

    @Test
    public void refusesASellThatWouldMakeTheHoldingNegative() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1000.00"));

        var run = cli.apply(buy("SELL", "2026-03-09", eurInstrument, "10.5", "1000.00"));

        run.assertRefused("shares", "negative-holding");
        var message = run.failure(0).get("message").getAsString();
        assertThat(message, containsString("Depot"));
        assertThat(message, containsString("2026-03-09"));
        assertThat(message, containsString("0.5"));
        assertThat(cli.reload().getPortfolios().get(0).getTransactions().size(), is(1));
    }

    @Test
    public void refusesAnOversellEvenIfALaterBuyWouldRestoreTheHolding() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-20", eurInstrument, "10", "1000.00"));

        var run = cli.apply(buy("SELL", "2026-03-09", eurInstrument, "5", "500.00"));

        run.assertRefused("shares", "negative-holding");
    }

    @Test
    public void refusesASellThatStarvesALaterSell() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1000.00"),
                        buy("SELL", "2026-03-20", eurInstrument, "8", "800.00"));

        var run = cli.apply(buy("SELL", "2026-03-09", eurInstrument, "5", "500.00"));

        run.assertRefused("shares", "negative-holding");
        assertThat(run.failure(0).get("message").getAsString(), containsString("2026-03-20"));
    }

    @Test
    public void judgesAHoldingByTheEndOfTheDayAndIgnoresTheTime() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-09", eurInstrument, "10", "1000.00").replace("\"date\"",
                        "\"time\":\"15:00\",\"date\""));

        var run = cli.apply(buy("SELL", "2026-03-09", eurInstrument, "10", "1000.00").replace("\"date\"",
                        "\"time\":\"09:00\",\"date\""));

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void countsEarlierOperationsOfTheSameBatch() throws IOException
    {
        var run = cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1000.00"),
                        buy("SELL", "2026-03-09", eurInstrument, "10", "1000.00"));

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void refusesAnOutboundDeliveryAndAnOutboundTransferThatOversell() throws IOException
    {
        cli.apply(buy("BUY", "2026-03-02", eurInstrument, "10", "1000.00"));

        var delivery = cli.apply(delivery("DELIVERY_OUTBOUND", "2026-03-09", eurInstrument, "11", "1100.00", "EUR"));
        delivery.assertRefused("shares", "negative-holding");

        var transfer = cli.apply(
                        securityTransfer(depot, otherDepot, "1100.00").replace("\"shares\":6", "\"shares\":11"));
        transfer.assertRefused("shares", "negative-holding");
        assertThat(transfer.failure(0).get("message").getAsString(), containsString("Depot"));
    }

    @Test
    public void anOversellAlreadyInTheFileDoesNotBlockUnrelatedWrites() throws IOException
    {
        var client = cli.reload();
        var portfolio = client.getPortfolios().get(0);
        var account = client.getAccounts().get(0);
        TransactionFixtures.sell(portfolio, account, client.getSecurities().get(0), "2026-03-02", 500_000_000L,
                        50_000L);
        cli.save(client);

        // a buy of the same instrument and a trade of another one both leave the oversold holding no worse
        var buyOfTheOversold = cli.apply(buy("BUY", "2026-03-09", eurInstrument, "2", "200.00"));
        assertThat(buyOfTheOversold.stdout, buyOfTheOversold.exitCode, is(0));
        var other = cli.apply(buy("BUY", "2026-03-09", usdInstrument, "2", "200.00").replace("\"EUR\"}",
                        "\"EUR\",\"units\":[{\"type\":\"GROSS_VALUE\",\"amount\":200.00,\"currencyCode\":\"EUR\","
                                        + "\"forexAmount\":220.00,\"forexCurrencyCode\":\"USD\","
                                        + "\"exchangeRate\":0.9090909091}]}"));
        assertThat(other.stdout, other.exitCode, is(0));

        // but selling more of the oversold instrument makes it worse
        var worse = cli.apply(buy("SELL", "2026-03-16", eurInstrument, "1", "100.00"));
        worse.assertRefused("shares", "negative-holding");
    }
}
