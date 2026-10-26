package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
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

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * The input of <code>transaction.add</code> mirrors the <code>transactions</code>
 * output: a record read from one file, without the members that add doesn't
 * declare, adds the same record to another file with the same master data.
 */
public class TransactionRoundTripTest
{
    /** Members that identify the stored sides or say when they were written, so they differ between files. */
    private static final List<String> IDENTITY = List.of("id", "sides", "updatedAt");

    @Rule
    public TemporaryFolder sourceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder targetFolder = new TemporaryFolder();

    private final CliFixture source = new CliFixture(sourceFolder);
    private final CliFixture target = new CliFixture(targetFolder);

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

        source.save(client);
        target.save(client);
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

    /** One record of every type, each on a day of its own so that both files list them in the same order. */
    private List<String> everyType()
    {
        return List.of(
                        cash("DEPOSIT", "2026-01-02\",\"time\":\"09:30", "10000.00",
                                        ",\"note\":\"salary\",\"source\":\"bank.pdf\""),
                        "{\"operation\":\"transaction.add\",\"type\":\"BUY\",\"investmentAccount\":\"" + depot
                                        + "\",\"cashAccount\":\"" + eurAccount
                                        + "\",\"date\":\"2026-01-05\",\"instrument\":\"" + eurInstrument
                                        + "\",\"shares\":10,\"amount\":1005.00,\"currencyCode\":\"EUR\","
                                        + "\"units\":[{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}",
                        "{\"operation\":\"transaction.add\",\"type\":\"DELIVERY_INBOUND\",\"investmentAccount\":\""
                                        + depot + "\",\"date\":\"2026-01-06\",\"instrument\":\"" + usdInstrument
                                        + "\",\"shares\":5,\"amount\":500.00,\"currencyCode\":\"USD\"}",
                        "{\"operation\":\"transaction.add\",\"type\":\"SELL\",\"investmentAccount\":\"" + depot
                                        + "\",\"cashAccount\":\"" + eurAccount
                                        + "\",\"date\":\"2026-02-05\",\"instrument\":\"" + eurInstrument
                                        + "\",\"shares\":4,\"amount\":395.00,\"currencyCode\":\"EUR\","
                                        + "\"units\":[{\"type\":\"FEE\",\"amount\":3.00,\"currencyCode\":\"EUR\"},"
                                        + "{\"type\":\"TAX\",\"amount\":2.00,\"currencyCode\":\"EUR\"}]}",
                        "{\"operation\":\"transaction.add\",\"type\":\"DELIVERY_OUTBOUND\",\"investmentAccount\":\""
                                        + depot + "\",\"date\":\"2026-02-06\",\"instrument\":\"" + usdInstrument
                                        + "\",\"shares\":1,\"amount\":100.00,\"currencyCode\":\"USD\"}",
                        cash("DIVIDENDS", "2026-03-01", "9.00", ",\"instrument\":\"" + usdInstrument
                                        + "\",\"shares\":4,\"exDate\":\"2026-02-25\",\"units\":["
                                        + "{\"type\":\"GROSS_VALUE\",\"amount\":10.00,\"currencyCode\":\"EUR\","
                                        + "\"forexAmount\":12.50,\"forexCurrencyCode\":\"USD\",\"exchangeRate\":0.8},"
                                        + "{\"type\":\"TAX\",\"amount\":1.00,\"currencyCode\":\"EUR\"}]"),
                        cash("INTEREST", "2026-03-02", "1.50",
                                        ",\"units\":[{\"type\":\"TAX\",\"amount\":0.50,\"currencyCode\":\"EUR\"}]"),
                        cash("INTEREST_CHARGE", "2026-03-03", "0.75", ""),
                        cash("FEES", "2026-03-04", "2.00", ",\"instrument\":\"" + eurInstrument + "\""),
                        cash("FEES_REFUND", "2026-03-05", "1.00", ""),
                        cash("TAXES", "2026-03-06", "3.00", ",\"instrument\":\"" + eurInstrument + "\""),
                        cash("TAX_REFUND", "2026-03-07", "1.20", ""),
                        cash("REMOVAL", "2026-03-08", "50.00", ""),
                        "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\""
                                        + eurAccount + "\",\"to\":\"" + eurSavings
                                        + "\",\"date\":\"2026-03-09\",\"amount\":100.00,\"currencyCode\":\"EUR\"}",
                        "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\""
                                        + eurAccount + "\",\"to\":\"" + usdAccount
                                        + "\",\"date\":\"2026-03-10\",\"amount\":100.00,\"currencyCode\":\"EUR\","
                                        + "\"targetAmount\":110.00}",
                        "{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"security\",\"from\":\""
                                        + depot + "\",\"to\":\"" + otherDepot
                                        + "\",\"date\":\"2026-03-11\",\"instrument\":\"" + eurInstrument
                                        + "\",\"shares\":2,\"amount\":200.00,\"currencyCode\":\"EUR\"}");
    }

    private String cash(String type, String date, String amount, String more)
    {
        return "{\"operation\":\"transaction.add\",\"type\":\"" + type + "\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"" + date + "\",\"amount\":" + amount + ",\"currencyCode\":\"EUR\"" + more
                        + "}";
    }

    private static JsonArray records(CliFixture cli)
    {
        var run = cli.run("transactions");
        assertThat(run.stdout, run.exitCode, is(0));
        return run.items();
    }

    /** The record as add input: only the members that add declares, and no empty list of units. */
    private static String asInput(JsonObject record)
    {
        var input = new JsonObject();
        input.addProperty(ApplyBatch.OPERATION_MEMBER, TransactionOperations.ADD.name());
        for (var member : record.entrySet())
        {
            var name = member.getKey();
            var value = member.getValue();
            if (!TransactionOperations.ADD.input().declares(name))
                continue;
            if (value.isJsonArray() && value.getAsJsonArray().isEmpty())
                continue;
            input.add(name, value);
        }
        return input.toString();
    }

    private static JsonObject withoutIdentity(JsonObject record)
    {
        var copy = record.deepCopy();
        IDENTITY.forEach(copy::remove);
        return copy;
    }

    @Test
    public void everyTypeOfRecordReadBackAddsTheSameRecord() throws IOException
    {
        var seeded = source.apply(everyType().toArray(String[]::new));
        assertThat(seeded.stdout, seeded.exitCode, is(0));

        var original = records(source);
        assertThat(original.size(), is(everyType().size()));

        var inputs = new ArrayList<String>();
        original.forEach(record -> inputs.add(asInput(record.getAsJsonObject())));
        var copied = target.apply(inputs.toArray(String[]::new));
        assertThat(copied.stdout, copied.exitCode, is(0));

        var copies = records(target);
        assertThat(copies.size(), is(original.size()));
        for (var i = 0; i < original.size(); i++)
        {
            var expected = withoutIdentity(original.get(i).getAsJsonObject());
            var actual = withoutIdentity(copies.get(i).getAsJsonObject());
            assertThat(expected.get("type").getAsString(), actual, is(expected));
        }
    }
}
