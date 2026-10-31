package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * <code>transaction.add</code> end to end, one run per flavour. The rules are
 * tested on the module, in
 * {@link name.abuchen.portfolio.cli.transaction.TransactionAddTest}.
 */
public class TransactionAddTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private String eurAccount;
    private String usdAccount;
    private String depot;
    private String eurInstrument;

    @Before
    public void setUp() throws IOException
    {
        var client = new Client();
        var eur = cashAccount(client, "EUR Cash", "EUR");
        var usd = cashAccount(client, "USD Cash", "USD");
        var portfolio = new Portfolio();
        portfolio.setName("Depot");
        portfolio.setReferenceAccount(eur);
        client.addPortfolio(portfolio);
        var security = new Security();
        security.setName("EUR Stock");
        security.setCurrencyCode("EUR");
        client.addSecurity(security);

        eurAccount = eur.getUUID();
        usdAccount = usd.getUUID();
        depot = portfolio.getUUID();
        eurInstrument = security.getUUID();

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

    private static Account account(Client client, String uuid)
    {
        return client.getAccounts().stream().filter(a -> a.getUUID().equals(uuid)).findFirst().orElseThrow();
    }

    private JsonObject onlyRecord(String type)
    {
        var run = cli.run("transactions", "--type", type);
        assertThat(run.stdout, run.exitCode, is(0));
        var items = run.items();
        assertThat(items.size(), is(1));
        return items.get(0).getAsJsonObject();
    }

    private String deposit(String amount)
    {
        return "{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":" + amount + ",\"currencyCode\":\"EUR\"}";
    }

    @Test
    public void addsADepositAndReportsTheCreatedRecord() throws IOException
    {
        var run = cli.apply(deposit("1000.00"));

        assertThat(run.stdout, run.exitCode, is(0));

        var created = run.created();
        assertThat(created.get("entity").getAsString(), is("transaction"));
        var after = created.getAsJsonObject("after");
        assertThat(after.get("type").getAsString(), is("DEPOSIT"));
        assertThat(after.get("cashAccount").getAsString(), is(eurAccount));
        assertThat(created.get("uuid").getAsString(), is(after.getAsJsonArray("sides").get(0).getAsString()));
        assertThat(onlyRecord("DEPOSIT"), is(after));

        var transactions = account(cli.reload(), eurAccount).getTransactions();
        assertThat(transactions.size(), is(1));
        assertThat(transactions.get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transactions.get(0).getAmount(), is(100_000L));
    }

    @Test
    public void addsABuyAsOneRecordWithBothSides() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"BUY\",\"investmentAccount\":\"" + depot
                        + "\",\"cashAccount\":\"" + eurAccount + "\",\"date\":\"2026-03-02\",\"instrument\":\""
                        + eurInstrument + "\",\"shares\":10,\"amount\":1005.00,\"currencyCode\":\"EUR\","
                        + "\"units\":[{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        assertThat(run.stdout, run.exitCode, is(0));

        var after = run.created().getAsJsonObject("after");
        assertThat(after.getAsJsonArray("sides").size(), is(2));
        assertThat(run.created().get("uuid").getAsString(), is(after.get("id").getAsString()));
        assertThat(onlyRecord("BUY"), is(after));

        var client = cli.reload();
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(account(client, eurAccount).getTransactions().size(), is(1));
    }

    @Test
    public void addsACrossCurrencyCashTransferAndEchoesTheDerivedRate() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"TRANSFER\",\"kind\":\"cash\",\"from\":\""
                        + eurAccount + "\",\"to\":\"" + usdAccount + "\",\"date\":\"2026-03-09\",\"amount\":100.00,"
                        + "\"currencyCode\":\"EUR\",\"targetAmount\":110.00}");

        assertThat(run.stdout, run.exitCode, is(0));

        var after = run.created().getAsJsonObject("after");
        assertThat(after.get("exchangeRate").getAsBigDecimal().toPlainString(), is("0.9090909091"));
        assertThat(onlyRecord("TRANSFER"), is(after));
    }

    @Test
    public void refusesAMemberTheTypeDoesNotTake() throws IOException
    {
        var run = cli.apply(deposit("100.00").replace("}", ",\"instrument\":\"" + eurInstrument + "\"}"));

        run.assertRefused("instrument", "instrument-not-allowed");
    }

    @Test
    public void refusesAnUnknownFieldOnce() throws IOException
    {
        var run = cli.apply(deposit("100.00").replace("}", ",\"colour\":\"red\"}"));

        run.assertRefused("colour", "unknown-field");
        assertThat(run.failures().size(), is(1));
        assertThat(account(cli.reload(), eurAccount).getTransactions().isEmpty(), is(true));
    }

    @Test
    public void aLaterFailingOperationLeavesNothingWritten() throws IOException
    {
        var run = cli.apply(deposit("100.00"), deposit("0"));

        assertThat(run.exitCode, is(1));
        assertThat(run.failures().size(), is(1));
        assertThat(account(cli.reload(), eurAccount).getTransactions().isEmpty(), is(true));
    }
}
