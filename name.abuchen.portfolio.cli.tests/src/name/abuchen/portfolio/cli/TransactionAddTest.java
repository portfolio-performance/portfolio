package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

/** The <code>transaction.add</code> operation for single-sided cash-account events. */
public class TransactionAddTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private String eurAccount;
    private String usdAccount;
    private String eurInstrument;
    private String usdInstrument;

    @Before
    public void setUp() throws IOException
    {
        var client = new Client();
        var eur = cashAccount(client, "EUR Cash", "EUR");
        var usd = cashAccount(client, "USD Cash", "USD");
        var eurSecurity = instrument(client, "EUR Stock", "EUR");
        var usdSecurity = instrument(client, "USD Stock", "USD");

        eurAccount = eur.getUUID();
        usdAccount = usd.getUUID();
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

    private static Security instrument(Client client, String name, String currencyCode)
    {
        var security = new Security();
        security.setName(name);
        security.setCurrencyCode(currencyCode);
        client.addSecurity(security);
        return security;
    }

    private Account account(Client client, String uuid)
    {
        return client.getAccounts().stream().filter(a -> a.getUUID().equals(uuid)).findFirst().orElseThrow();
    }

    @Test
    public void addsADepositAndReportsTheCreatedRecord() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":1000.00,\"currencyCode\":\"EUR\"}");

        assertThat(run.stdout, run.exitCode, is(0));

        var created = run.created();
        assertThat(created.get("entity").getAsString(), is("transaction"));
        var after = created.getAsJsonObject("after");
        assertThat(after.get("type").getAsString(), is("DEPOSIT"));
        assertThat(after.get("amount").getAsBigDecimal().doubleValue(), is(1000.00));
        assertThat(after.get("cashAccount").getAsString(), is(eurAccount));
        assertThat(after.getAsJsonArray("sides").size(), is(1));
        assertThat(created.get("uuid").getAsString(), is(after.getAsJsonArray("sides").get(0).getAsString()));

        var transactions = account(cli.reload(), eurAccount).getTransactions();
        assertThat(transactions.size(), is(1));
        assertThat(transactions.get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transactions.get(0).getAmount(), is(100_000L));
    }

    @Test
    public void addsEveryCashType() throws IOException
    {
        var types = List.of("DEPOSIT", "REMOVAL", "INTEREST", "INTEREST_CHARGE", "FEES", "FEES_REFUND", "TAXES",
                        "TAX_REFUND");
        for (var type : types)
        {
            var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"" + type + "\",\"cashAccount\":\""
                            + eurAccount + "\",\"date\":\"2026-03-02\",\"amount\":12.50,\"currencyCode\":\"EUR\"}");
            assertThat(type + ": " + run.stdout, run.exitCode, is(0));
        }

        var added = new ArrayList<AccountTransaction.Type>();
        account(cli.reload(), eurAccount).getTransactions().forEach(t -> added.add(t.getType()));
        assertThat(added, contains(AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL,
                        AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE,
                        AccountTransaction.Type.FEES, AccountTransaction.Type.FEES_REFUND,
                        AccountTransaction.Type.TAXES, AccountTransaction.Type.TAX_REFUND));
    }

    @Test
    public void readsBackADepositThroughTransactions() throws IOException
    {
        cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"time\":\"09:30\",\"amount\":1000.00,\"currencyCode\":\"EUR\","
                        + "\"note\":\"salary\"}");

        var run = cli.run("transactions");
        assertThat(run.stdout, run.exitCode, is(0));

        var items = run.items();
        assertThat(items.size(), is(1));
        var record = items.get(0).getAsJsonObject();
        assertThat(record.get("type").getAsString(), is("DEPOSIT"));
        assertThat(record.get("date").getAsString(), is("2026-03-02"));
        assertThat(record.get("time").getAsString(), is("09:30"));
        assertThat(record.get("amount").getAsBigDecimal().doubleValue(), is(1000.00));
        assertThat(record.get("currencyCode").getAsString(), is("EUR"));
        assertThat(record.get("cashAccount").getAsString(), is(eurAccount));
        assertThat(record.get("note").getAsString(), is("salary"));
    }

    @Test
    public void addsADividendWithSharesFeesAndTaxes() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":80.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\",\"shares\":10,\"exDate\":\"2026-02-25\",\"units\":["
                        + "{\"type\":\"TAX\",\"amount\":15.00,\"currencyCode\":\"EUR\"},"
                        + "{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        assertThat(run.stdout, run.exitCode, is(0));

        var transaction = account(cli.reload(), eurAccount).getTransactions().get(0);
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity().getUUID(), is(eurInstrument));
        assertThat(transaction.getShares(), is(1_000_000_000L));
        assertThat(((AccountTransaction) transaction).getExDate().toLocalDate().toString(), is("2026-02-25"));
        assertThat(transaction.getUnitSum(Transaction.Unit.Type.TAX).getAmount(), is(1_500L));
        assertThat(transaction.getUnitSum(Transaction.Unit.Type.FEE).getAmount(), is(500L));
    }

    @Test
    public void acceptsAForexDividendWithAGrossValueUnit() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + usdInstrument + "\",\"units\":[{\"type\":\"GROSS_VALUE\",\"amount\":100.00,"
                        + "\"currencyCode\":\"EUR\",\"forexAmount\":110.00,\"forexCurrencyCode\":\"USD\","
                        + "\"exchangeRate\":0.9090909091}]}");

        assertThat(run.stdout, run.exitCode, is(0));

        var transaction = account(cli.reload(), eurAccount).getTransactions().get(0);
        var grossValue = transaction.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(grossValue.getForex().getCurrencyCode(), is("USD"));
        assertThat(grossValue.getForex().getAmount(), is(11_000L));
    }

    @Test
    public void refusesAForexDividendWithoutTheGrossValueUnit() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + usdInstrument + "\"}");

        run.assertRefused("units", "forex-gross-value-required");
        assertFileUnchanged();
    }

    @Test
    public void refusesANonPositiveAmount() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":0,\"currencyCode\":\"EUR\"}");

        run.assertRefused("amount", "not-positive");
    }

    @Test
    public void refusesANonPositiveExchangeRate() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + usdInstrument + "\",\"units\":[{\"type\":\"GROSS_VALUE\",\"amount\":100.00,"
                        + "\"currencyCode\":\"EUR\",\"forexAmount\":110.00,\"forexCurrencyCode\":\"USD\","
                        + "\"exchangeRate\":0}]}");

        run.assertRefused("units[0].exchangeRate", "not-positive");
    }

    @Test
    public void refusesADividendWithoutAnInstrument() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\"}");

        run.assertRefused("instrument", "required");
    }

    @Test
    public void refusesAnInstrumentOnADeposit() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\"}");

        run.assertRefused("instrument", "instrument-not-allowed");
    }

    @Test
    public void refusesSharesOnADeposit() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"shares\":10}");

        run.assertRefused("shares", "shares-not-allowed");
    }

    @Test
    public void refusesACurrencyThatIsNotTheAccounts() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"USD\"}");

        run.assertRefused("currencyCode", "currency-mismatch");
    }

    @Test
    public void refusesAnExDateOnADeposit() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\","
                        + "\"exDate\":\"2026-02-25\"}");

        run.assertRefused("exDate", "exdate-not-allowed");
    }

    @Test
    public void refusesAnExDateAfterThePaymentDate() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\",\"exDate\":\"2026-03-03\"}");

        run.assertRefused("exDate", "exdate-after-payment");
    }

    @Test
    public void refusesAGrossAmountThatDoesNotReconcile() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":80.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\",\"grossAmount\":99.00,\"units\":["
                        + "{\"type\":\"TAX\",\"amount\":15.00,\"currencyCode\":\"EUR\"},"
                        + "{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        run.assertRefused("grossAmount", "gross-amount-mismatch");
    }

    @Test
    public void acceptsAGrossAmountThatReconciles() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":80.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\",\"grossAmount\":100.00,\"units\":["
                        + "{\"type\":\"TAX\",\"amount\":15.00,\"currencyCode\":\"EUR\"},"
                        + "{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        assertThat(run.stdout, run.exitCode, is(0));
    }

    @Test
    public void refusesAGrossAmountThatDiffersFromTheAmountOfADebitType() throws IOException
    {
        // a debit type carries no units, so its gross amount can only equal the amount
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"INTEREST_CHARGE\",\"cashAccount\":\""
                        + eurAccount + "\",\"date\":\"2026-03-02\",\"amount\":20.00,\"currencyCode\":\"EUR\","
                        + "\"grossAmount\":25.00}");

        run.assertRefused("grossAmount", "gross-amount-mismatch");
    }

    @Test
    public void refusesTaxesOnATypeThatCoreDoesNotAllowThemOn() throws IOException
    {
        // core's CheckValidTypesAction allows taxes only on dividends and interest
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"INTEREST_CHARGE\",\"cashAccount\":\""
                        + eurAccount + "\",\"date\":\"2026-03-02\",\"amount\":20.00,\"currencyCode\":\"EUR\","
                        + "\"units\":[{\"type\":\"TAX\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        run.assertRefused("", "import-check-failed");
        assertThat(run.failure(0).get("check").getAsString(), is("CheckValidTypesAction"));
        assertFileUnchanged();
    }

    @Test
    public void refusesFeesOnATypeThatCoreDoesNotAllowThemOn() throws IOException
    {
        // core's CheckValidTypesAction allows fees only on dividends, not even on interest
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"INTEREST\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":20.00,\"currencyCode\":\"EUR\","
                        + "\"units\":[{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        run.assertRefused("", "import-check-failed");
        assertThat(run.failure(0).get("check").getAsString(), is("CheckValidTypesAction"));
        assertFileUnchanged();
    }

    @Test
    public void readsBackADividendThroughTransactions() throws IOException
    {
        cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":80.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\",\"shares\":10,\"exDate\":\"2026-02-25\",\"units\":["
                        + "{\"type\":\"TAX\",\"amount\":15.00,\"currencyCode\":\"EUR\"},"
                        + "{\"type\":\"FEE\",\"amount\":5.00,\"currencyCode\":\"EUR\"}]}");

        var run = cli.run("transactions");
        assertThat(run.stdout, run.exitCode, is(0));

        var items = run.items();
        assertThat(items.size(), is(1));
        var record = items.get(0).getAsJsonObject();
        assertThat(record.get("type").getAsString(), is("DIVIDENDS"));
        assertThat(record.get("amount").getAsBigDecimal().doubleValue(), is(80.00));
        assertThat(record.get("instrument").getAsString(), is(eurInstrument));
        assertThat(record.get("shares").getAsBigDecimal().doubleValue(), is(10.0));
        assertThat(record.get("exDate").getAsString(), is("2026-02-25"));

        var unitTypes = new ArrayList<String>();
        record.getAsJsonArray("units").forEach(u -> unitTypes.add(u.getAsJsonObject().get("type").getAsString()));
        assertThat(unitTypes, contains("TAX", "FEE"));
    }

    @Test
    public void runsCoreImportChecksAsASafetyNet() throws IOException
    {
        // a same-currency dividend must not carry a forex gross-value unit; core's CheckCurrenciesAction catches it
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DIVIDENDS\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":100.00,\"currencyCode\":\"EUR\",\"instrument\":\""
                        + eurInstrument + "\",\"units\":[{\"type\":\"GROSS_VALUE\",\"amount\":100.00,"
                        + "\"currencyCode\":\"EUR\",\"forexAmount\":110.00,\"forexCurrencyCode\":\"USD\","
                        + "\"exchangeRate\":0.9090909091}]}");

        run.assertRefused("", "import-check-failed");
        assertThat(run.failure(0).has("check"), is(true));
        assertFileUnchanged();
    }

    @Test
    public void refusesAnOverPreciseAmount() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":10.001,\"currencyCode\":\"EUR\"}");

        run.assertRefused("amount", "invalid-amount");
    }

    @Test
    public void refusesAnUnknownCashAccount() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\","
                        + "\"cashAccount\":\"does-not-exist\",\"date\":\"2026-03-02\",\"amount\":100.00,"
                        + "\"currencyCode\":\"EUR\"}");

        run.assertRefused("cashAccount", "reference-not-found");
    }

    @Test
    public void reportsEveryFailureAtOnce() throws IOException
    {
        var run = cli.apply("{\"operation\":\"transaction.add\",\"type\":\"DEPOSIT\",\"cashAccount\":\"" + eurAccount
                        + "\",\"date\":\"2026-03-02\",\"amount\":-1,\"currencyCode\":\"USD\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failures().size(), is(2));
    }

    private void assertFileUnchanged() throws IOException
    {
        // a refused write leaves the account without transactions
        assertThat(account(cli.reload(), eurAccount).getTransactions().isEmpty(), is(true));
    }
}
