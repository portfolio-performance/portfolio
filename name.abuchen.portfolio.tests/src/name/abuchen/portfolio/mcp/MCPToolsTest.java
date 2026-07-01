package name.abuchen.portfolio.mcp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class MCPToolsTest
{
    private static final String FILE_ID = "test-file";

    private Client client;
    private Account account;
    private Account usdAccount;
    private Portfolio portfolio;
    private Portfolio otherPortfolio;
    private Security security;
    private MCPServer server;

    @Before
    public void setup()
    {
        client = new Client();
        client.setBaseCurrency("EUR");

        account = new Account();
        account.setName("Cash");
        account.setCurrencyCode("EUR");
        account.setNote("NL20BNDA0100725869");
        client.addAccount(account);

        usdAccount = new Account();
        usdAccount.setName("USD Cash");
        usdAccount.setCurrencyCode("USD");
        client.addAccount(usdAccount);

        portfolio = new Portfolio();
        portfolio.setName("Broker");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        otherPortfolio = new Portfolio();
        otherPortfolio.setName("Other Broker");
        otherPortfolio.setReferenceAccount(usdAccount);
        client.addPortfolio(otherPortfolio);

        security = new Security("Apple Inc.", "EUR");
        security.setIsin("US0378331005");
        security.setCurrencyCode("EUR");
        client.addSecurity(security);

        OpenFile openFile = new OpenFile(FILE_ID, "test.portfolio", null, client, false, null);
        ClientSource clientSource = () -> List.of(openFile);
        MCPTools tools = new MCPTools(clientSource, new SyncModelExecutor(), false);
        server = new MCPServer(tools, 8090);
    }

    @Test
    public void listOpenFilesReturnsOpenFile()
    {
        JsonArray files = callToolArray("list_open_files", new JsonObject());

        assertThat(files.size(), is(1));
        assertThat(files.get(0).getAsJsonObject().get("id").getAsString(), is(FILE_ID));
        assertThat(files.get(0).getAsJsonObject().get("baseCurrency").getAsString(), is("EUR"));
    }

    @Test
    public void listAccountsReturnsAccountsAndPortfolios()
    {
        JsonObject args = new JsonObject();
        args.addProperty("file", FILE_ID);

        JsonObject result = callToolObject("list_accounts", args);

        assertThat(result.getAsJsonArray("accounts").size(), is(2));
        assertThat(result.getAsJsonArray("accounts").get(0).getAsJsonObject().get("name").getAsString(), is("Cash"));
        assertThat(result.getAsJsonArray("accounts").get(0).getAsJsonObject().get("note").getAsString(),
                        is("NL20BNDA0100725869"));
        assertThat(result.getAsJsonArray("portfolios").size(), is(2));
        assertThat(result.getAsJsonArray("portfolios").get(0).getAsJsonObject().get("name").getAsString(),
                        is("Broker"));
    }

    @Test
    public void listSecuritiesReturnsSecurities()
    {
        JsonObject args = new JsonObject();
        args.addProperty("file", FILE_ID);

        JsonArray securities = callToolArray("list_securities", args);

        assertThat(securities.size(), is(1));
        assertThat(securities.get(0).getAsJsonObject().get("name").getAsString(), is("Apple Inc."));
        assertThat(securities.get(0).getAsJsonObject().get("isin").getAsString(), is("US0378331005"));
    }

    @Test
    public void missingFileReturnsError()
    {
        JsonObject args = new JsonObject();
        args.addProperty("file", "unknown-file");

        JsonObject response = invokeToolCall("list_accounts", args);

        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("Unknown or closed file: unknown-file"));
    }

    @Test
    public void addAndListSimpleAccountTransactions() throws Exception
    {
        addSimpleAccountTransaction(AccountTransaction.Type.DEPOSIT, 100.0, null, "deposit");
        addSimpleAccountTransaction(AccountTransaction.Type.REMOVAL, 25.0, null, "removal");
        addSimpleAccountTransaction(AccountTransaction.Type.INTEREST, 5.5, null, "interest");
        addSimpleAccountTransaction(AccountTransaction.Type.INTEREST_CHARGE, 1.2, null, "interest charge");
        addSimpleAccountTransaction(AccountTransaction.Type.DIVIDENDS, 42.0, security, "dividends");
        addSimpleAccountTransaction(AccountTransaction.Type.FEES, 2.0, null, "fees");
        addSimpleAccountTransaction(AccountTransaction.Type.FEES_REFUND, 1.0, null, "fees refund");
        addSimpleAccountTransaction(AccountTransaction.Type.TAXES, 3.0, null, "taxes");
        addSimpleAccountTransaction(AccountTransaction.Type.TAX_REFUND, 0.5, null, "tax refund");

        JsonObject listArgs = new JsonObject();
        listArgs.addProperty("file", FILE_ID);
        JsonArray transactions = callToolArray("list_transactions", listArgs);

        assertThat(transactions.size(), is(9));

        assertThat(account.getTransactions().size(), is(9));
        assertAccountTransaction(AccountTransaction.Type.DEPOSIT, 100.0, null);
        assertAccountTransaction(AccountTransaction.Type.REMOVAL, 25.0, null);
        assertAccountTransaction(AccountTransaction.Type.INTEREST, 5.5, null);
        assertAccountTransaction(AccountTransaction.Type.INTEREST_CHARGE, 1.2, null);
        assertAccountTransaction(AccountTransaction.Type.DIVIDENDS, 42.0, security);
        assertAccountTransaction(AccountTransaction.Type.FEES, 2.0, null);
        assertAccountTransaction(AccountTransaction.Type.FEES_REFUND, 1.0, null);
        assertAccountTransaction(AccountTransaction.Type.TAXES, 3.0, null);
        assertAccountTransaction(AccountTransaction.Type.TAX_REFUND, 0.5, null);
    }

    @Test
    public void addAndListSecurityTransactions() throws Exception
    {
        addSecurityTransaction(PortfolioTransaction.Type.BUY, 10.0, 1500.0, "buy");
        addSecurityTransaction(PortfolioTransaction.Type.SELL, 5.0, 800.0, "sell");

        JsonObject listArgs = new JsonObject();
        listArgs.addProperty("file", FILE_ID);
        JsonArray transactions = callToolArray("list_transactions", listArgs);

        assertThat(transactions.size(), greaterThan(1));

        assertThat(portfolio.getTransactions().size(), is(2));
        assertPortfolioTransaction(PortfolioTransaction.Type.BUY, 10.0, 1500.0);
        assertPortfolioTransaction(PortfolioTransaction.Type.SELL, 5.0, 800.0);

        assertThat(account.getTransactions().stream()
                        .anyMatch(t -> t.getType() == AccountTransaction.Type.BUY), is(true));
        assertThat(account.getTransactions().stream()
                        .anyMatch(t -> t.getType() == AccountTransaction.Type.SELL), is(true));
    }

    @Test
    public void deleteSimpleAccountTransactionViaMcp() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", AccountTransaction.Type.DEPOSIT.name());
        addArgs.addProperty("account", account.getUUID());
        addArgs.addProperty("date", "2024-03-01");
        addArgs.addProperty("amount", 200.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String id = addResult.get("id").getAsString();

        JsonObject deleteArgs = new JsonObject();
        deleteArgs.addProperty("file", FILE_ID);
        deleteArgs.addProperty("id", id);

        JsonObject deleteResult = callToolObject("delete_transaction", deleteArgs);
        assertThat(deleteResult.get("id").getAsString(), is(id));
        assertThat(deleteResult.has("crossEntryId"), is(false));

        assertThat(account.getTransactions().stream().anyMatch(t -> t.getUUID().equals(id)), is(false));
    }

    @Test
    public void deleteSecurityTransactionRemovesBothLegsViaMcp() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", PortfolioTransaction.Type.BUY.name());
        addArgs.addProperty("portfolio", portfolio.getUUID());
        addArgs.addProperty("account", account.getUUID());
        addArgs.addProperty("security", security.getUUID());
        addArgs.addProperty("date", "2024-02-01");
        addArgs.addProperty("shares", 10.0);
        addArgs.addProperty("amount", 1500.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String portfolioId = addResult.get("portfolioTransactionId").getAsString();
        String accountId = addResult.get("accountTransactionId").getAsString();

        JsonObject deleteArgs = new JsonObject();
        deleteArgs.addProperty("file", FILE_ID);
        deleteArgs.addProperty("id", portfolioId);

        JsonObject deleteResult = callToolObject("delete_transaction", deleteArgs);
        assertThat(deleteResult.get("id").getAsString(), is(portfolioId));
        assertThat(deleteResult.get("crossEntryId").getAsString(), is(accountId));

        assertThat(portfolio.getTransactions().stream().anyMatch(t -> t.getUUID().equals(portfolioId)), is(false));
        assertThat(account.getTransactions().stream().anyMatch(t -> t.getUUID().equals(accountId)), is(false));
    }

    @Test
    public void deleteUnknownTransactionReturnsError()
    {
        JsonObject deleteArgs = new JsonObject();
        deleteArgs.addProperty("file", FILE_ID);
        deleteArgs.addProperty("id", "missing-id");

        JsonObject response = invokeToolCall("delete_transaction", deleteArgs);

        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("Transaction not found: missing-id"));
    }

    @Test
    public void updateTransactionViaMcp() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", AccountTransaction.Type.DEPOSIT.name());
        addArgs.addProperty("account", account.getUUID());
        addArgs.addProperty("date", "2024-03-01");
        addArgs.addProperty("amount", 200.0);
        addArgs.addProperty("note", "before");

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String id = addResult.get("id").getAsString();

        JsonObject updateArgs = new JsonObject();
        updateArgs.addProperty("file", FILE_ID);
        updateArgs.addProperty("id", id);
        updateArgs.addProperty("amount", 250.0);
        updateArgs.addProperty("note", "after");

        callToolObject("update_transaction", updateArgs);

        JsonObject listArgs = new JsonObject();
        listArgs.addProperty("file", FILE_ID);
        JsonArray transactions = callToolArray("list_transactions", listArgs);

        JsonObject updated = transactions.asList().stream().map(e -> e.getAsJsonObject())
                        .filter(t -> id.equals(t.get("id").getAsString())).findFirst().orElseThrow();
        assertThat(updated.get("amount").getAsDouble(), is(250.0));
        assertThat(updated.get("note").getAsString(), is("after"));

        AccountTransaction modelTx = account.getTransactions().stream().filter(t -> t.getUUID().equals(id))
                        .findFirst().orElseThrow();
        assertThat(modelTx.getAmount(), is(Values.Amount.factorize(250.0)));
        assertThat(modelTx.getNote(), is("after"));
    }

    @Test
    public void listTransactionsFilteredByAccount() throws Exception
    {
        Account other = new Account();
        other.setName("Other");
        other.setCurrencyCode("EUR");
        client.addAccount(other);

        addSimpleAccountTransaction(AccountTransaction.Type.DEPOSIT, 10.0, null, "cash deposit");
        addSimpleAccountTransactionOnAccount(other, AccountTransaction.Type.DEPOSIT, 20.0, "other deposit");

        JsonObject listArgs = new JsonObject();
        listArgs.addProperty("file", FILE_ID);
        listArgs.addProperty("account", account.getUUID());

        JsonArray transactions = callToolArray("list_transactions", listArgs);
        assertThat(transactions.size(), is(1));
        assertThat(transactions.get(0).getAsJsonObject().get("note").getAsString(), is("cash deposit"));
    }

    @Test
    public void addAccountTransferSameCurrencyShowsInboundLegWithCounterparty() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("date", "2024-04-01");
        addArgs.addProperty("amount", 500.0);
        addArgs.addProperty("note", "same currency transfer");

        Account eurOther = new Account();
        eurOther.setName("EUR Other");
        eurOther.setCurrencyCode("EUR");
        client.addAccount(eurOther);
        addArgs.addProperty("toAccount", eurOther.getUUID());

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();
        String toId = addResult.get("toTransactionId").getAsString();

        assertThat(account.getTransactions().stream().anyMatch(t -> t.getUUID().equals(fromId)), is(true));
        assertThat(eurOther.getTransactions().stream().anyMatch(t -> t.getUUID().equals(toId)), is(true));

        JsonObject listArgs = new JsonObject();
        listArgs.addProperty("file", FILE_ID);
        listArgs.addProperty("account", eurOther.getUUID());
        JsonArray transactions = callToolArray("list_transactions", listArgs);

        assertThat(transactions.size(), is(1));
        JsonObject inbound = transactions.get(0).getAsJsonObject();
        assertThat(inbound.get("type").getAsString(), is(AccountTransaction.Type.TRANSFER_IN.name()));
        assertThat(inbound.get("direction").getAsString(), is("IN"));
        assertThat(inbound.getAsJsonObject("counterparty").get("uuid").getAsString(), is(account.getUUID()));
        assertThat(inbound.get("amount").getAsDouble(), is(500.0));
    }

    @Test
    public void addAccountTransferFxCreatesGrossValueUnit() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-04-02");
        addArgs.addProperty("amount", 1000.0);
        addArgs.addProperty("targetAmount", 1100.0);
        addArgs.addProperty("note", "fx transfer");

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();

        AccountTransaction sourceTx = account.getTransactions().stream().filter(t -> t.getUUID().equals(fromId))
                        .findFirst().orElseThrow();
        AccountTransaction targetTx = usdAccount.getTransactions().stream()
                        .filter(t -> t.getType() == AccountTransaction.Type.TRANSFER_IN).findFirst().orElseThrow();

        assertThat(sourceTx.getAmount(), is(Values.Amount.factorize(1000.0)));
        assertThat(targetTx.getAmount(), is(Values.Amount.factorize(1100.0)));

        Unit grossValue = sourceTx.getUnit(Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(grossValue.getAmount().getCurrencyCode(), is("EUR"));
        assertThat(grossValue.getForex().getCurrencyCode(), is("USD"));
        assertThat(grossValue.getExchangeRate().doubleValue(), closeTo(1000.0 / 1100.0, 0.0000001));
    }

    @Test
    public void addSecurityTransferBetweenPortfolios() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "SECURITY_TRANSFER");
        addArgs.addProperty("fromPortfolio", portfolio.getUUID());
        addArgs.addProperty("toPortfolio", otherPortfolio.getUUID());
        addArgs.addProperty("security", security.getUUID());
        addArgs.addProperty("date", "2024-04-03");
        addArgs.addProperty("shares", 3.0);
        addArgs.addProperty("amount", 450.0);
        addArgs.addProperty("note", "security transfer");

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();
        String toId = addResult.get("toTransactionId").getAsString();

        PortfolioTransaction fromTx = portfolio.getTransactions().stream().filter(t -> t.getUUID().equals(fromId))
                        .findFirst().orElseThrow();
        PortfolioTransaction toTx = otherPortfolio.getTransactions().stream().filter(t -> t.getUUID().equals(toId))
                        .findFirst().orElseThrow();

        assertThat(fromTx.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(toTx.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(fromTx.getShares(), is(Values.Share.factorize(3.0)));
        assertThat(toTx.getShares(), is(Values.Share.factorize(3.0)));
        assertThat(fromTx.getSecurity(), is(security));
        assertThat(toTx.getSecurity(), is(security));
    }

    @Test
    public void addDeliveryInboundAndOutbound() throws Exception
    {
        addDelivery(PortfolioTransaction.Type.DELIVERY_INBOUND, 5.0, 500.0, 1.0, 2.0, "delivery in");
        addDelivery(PortfolioTransaction.Type.DELIVERY_OUTBOUND, 2.0, 200.0, 0.5, 0.0, "delivery out");

        assertThat(portfolio.getTransactions().stream()
                        .anyMatch(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND), is(true));
        assertThat(portfolio.getTransactions().stream()
                        .anyMatch(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND), is(true));

        PortfolioTransaction inbound = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND).findFirst()
                        .orElseThrow();
        assertThat(inbound.getShares(), is(Values.Share.factorize(5.0)));
        assertThat(inbound.getAmount(), is(Values.Amount.factorize(500.0)));
        assertThat(inbound.getUnit(Unit.Type.FEE).orElseThrow().getAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1.0))));
        assertThat(inbound.getUnit(Unit.Type.TAX).orElseThrow().getAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2.0))));
    }

    @Test
    public void updateTransferPropagatesDateAndNote() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-05-01");
        addArgs.addProperty("amount", 200.0);
        addArgs.addProperty("targetAmount", 220.0);
        addArgs.addProperty("note", "before");

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();
        String toId = addResult.get("toTransactionId").getAsString();

        JsonObject updateArgs = new JsonObject();
        updateArgs.addProperty("file", FILE_ID);
        updateArgs.addProperty("id", fromId);
        updateArgs.addProperty("date", "2024-05-02T12:00:00");
        updateArgs.addProperty("note", "after");

        callToolObject("update_transaction", updateArgs);

        AccountTransaction sourceTx = account.getTransactions().stream().filter(t -> t.getUUID().equals(fromId))
                        .findFirst().orElseThrow();
        AccountTransaction targetTx = usdAccount.getTransactions().stream().filter(t -> t.getUUID().equals(toId))
                        .findFirst().orElseThrow();

        assertThat(sourceTx.getNote(), is("after"));
        assertThat(targetTx.getNote(), is("after"));
        assertThat(sourceTx.getDateTime().toString(), is("2024-05-02T12:00"));
        assertThat(targetTx.getDateTime().toString(), is("2024-05-02T12:00"));
    }

    @Test
    public void updateFxTransferAmountsRebuildsGrossValueUnit() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-05-03");
        addArgs.addProperty("amount", 100.0);
        addArgs.addProperty("targetAmount", 110.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();
        String toId = addResult.get("toTransactionId").getAsString();

        JsonObject updateArgs = new JsonObject();
        updateArgs.addProperty("file", FILE_ID);
        updateArgs.addProperty("id", fromId);
        updateArgs.addProperty("amount", 200.0);
        updateArgs.addProperty("targetAmount", 230.0);

        callToolObject("update_transaction", updateArgs);

        AccountTransaction sourceTx = account.getTransactions().stream().filter(t -> t.getUUID().equals(fromId))
                        .findFirst().orElseThrow();
        AccountTransaction targetTx = usdAccount.getTransactions().stream().filter(t -> t.getUUID().equals(toId))
                        .findFirst().orElseThrow();

        assertThat(sourceTx.getAmount(), is(Values.Amount.factorize(200.0)));
        assertThat(targetTx.getAmount(), is(Values.Amount.factorize(230.0)));
        assertThat(sourceTx.getUnit(Unit.Type.GROSS_VALUE).orElseThrow().getExchangeRate().doubleValue(),
                        closeTo(200.0 / 230.0, 0.0000001));
    }

    @Test
    public void updateSameCurrencyTransferAmountUpdatesBothLegs() throws Exception
    {
        Account eurOther = new Account();
        eurOther.setName("EUR Savings");
        eurOther.setCurrencyCode("EUR");
        client.addAccount(eurOther);

        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", eurOther.getUUID());
        addArgs.addProperty("date", "2024-05-04");
        addArgs.addProperty("amount", 50.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();
        String toId = addResult.get("toTransactionId").getAsString();

        JsonObject updateArgs = new JsonObject();
        updateArgs.addProperty("file", FILE_ID);
        updateArgs.addProperty("id", toId);
        updateArgs.addProperty("amount", 75.0);

        callToolObject("update_transaction", updateArgs);

        AccountTransaction sourceTx = account.getTransactions().stream().filter(t -> t.getUUID().equals(fromId))
                        .findFirst().orElseThrow();
        AccountTransaction targetTx = eurOther.getTransactions().stream().filter(t -> t.getUUID().equals(toId))
                        .findFirst().orElseThrow();

        assertThat(sourceTx.getAmount(), is(Values.Amount.factorize(75.0)));
        assertThat(targetTx.getAmount(), is(Values.Amount.factorize(75.0)));
    }

    @Test
    public void deleteAccountTransferRemovesBothLegsViaMcp() throws Exception
    {
        Account eurOther = new Account();
        eurOther.setName("EUR Vault");
        eurOther.setCurrencyCode("EUR");
        client.addAccount(eurOther);

        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", eurOther.getUUID());
        addArgs.addProperty("date", "2024-05-05");
        addArgs.addProperty("amount", 30.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();
        String toId = addResult.get("toTransactionId").getAsString();

        JsonObject deleteArgs = new JsonObject();
        deleteArgs.addProperty("file", FILE_ID);
        deleteArgs.addProperty("id", fromId);

        JsonObject deleteResult = callToolObject("delete_transaction", deleteArgs);
        assertThat(deleteResult.get("crossEntryId").getAsString(), is(toId));

        assertThat(account.getTransactions().stream().anyMatch(t -> t.getUUID().equals(fromId)), is(false));
        assertThat(eurOther.getTransactions().stream().anyMatch(t -> t.getUUID().equals(toId)), is(false));
    }

    @Test
    public void updateFeeOnFxTransferPreservesGrossValueUnit() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-06-01");
        addArgs.addProperty("amount", 100.0);
        addArgs.addProperty("targetAmount", 110.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();

        JsonObject updateArgs = new JsonObject();
        updateArgs.addProperty("file", FILE_ID);
        updateArgs.addProperty("id", fromId);
        updateArgs.addProperty("fee", 2.0);

        callToolObject("update_transaction", updateArgs);

        AccountTransaction sourceTx = account.getTransactions().stream().filter(t -> t.getUUID().equals(fromId))
                        .findFirst().orElseThrow();

        assertThat(sourceTx.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(true));
        assertThat(sourceTx.getUnit(Unit.Type.FEE).orElseThrow().getAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2.0))));
    }

    @Test
    public void updateTypeOnTransferReturnsError() throws Exception
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-06-02");
        addArgs.addProperty("amount", 100.0);
        addArgs.addProperty("targetAmount", 110.0);

        JsonObject addResult = callToolObject("add_transaction", addArgs);
        String fromId = addResult.get("fromTransactionId").getAsString();

        JsonObject updateArgs = new JsonObject();
        updateArgs.addProperty("file", FILE_ID);
        updateArgs.addProperty("id", fromId);
        updateArgs.addProperty("type", AccountTransaction.Type.DEPOSIT.name());

        JsonObject response = invokeToolCall("update_transaction", updateArgs);
        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("Changing the type of a transfer transaction is not supported"));
    }

    @Test
    public void addFxTransferWithoutTargetAmountReturnsError()
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-06-03");
        addArgs.addProperty("amount", 100.0);

        JsonObject response = invokeToolCall("add_transaction", addArgs);
        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("targetAmount is required for cross-currency transfers"));
    }

    @Test
    public void addFxTransferWithZeroTargetAmountReturnsError()
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", usdAccount.getUUID());
        addArgs.addProperty("date", "2024-06-04");
        addArgs.addProperty("amount", 100.0);
        addArgs.addProperty("targetAmount", 0.0);

        JsonObject response = invokeToolCall("add_transaction", addArgs);
        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("targetAmount must be greater than zero"));
    }

    @Test
    public void addTransferToSameAccountReturnsError()
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "TRANSFER");
        addArgs.addProperty("fromAccount", account.getUUID());
        addArgs.addProperty("toAccount", account.getUUID());
        addArgs.addProperty("date", "2024-06-05");
        addArgs.addProperty("amount", 100.0);

        JsonObject response = invokeToolCall("add_transaction", addArgs);
        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("fromAccount and toAccount must be different"));
    }

    @Test
    public void addSecurityTransferToSamePortfolioReturnsError()
    {
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("file", FILE_ID);
        addArgs.addProperty("type", "SECURITY_TRANSFER");
        addArgs.addProperty("fromPortfolio", portfolio.getUUID());
        addArgs.addProperty("toPortfolio", portfolio.getUUID());
        addArgs.addProperty("security", security.getUUID());
        addArgs.addProperty("date", "2024-06-06");
        addArgs.addProperty("shares", 1.0);
        addArgs.addProperty("amount", 100.0);

        JsonObject response = invokeToolCall("add_transaction", addArgs);
        assertTrue(response.has("error"));
        assertThat(response.getAsJsonObject("error").get("message").getAsString(),
                        is("fromPortfolio and toPortfolio must be different"));
    }

    private void addDelivery(PortfolioTransaction.Type type, double shares, double amount, double fee, double tax,
                    String note) throws Exception
    {
        JsonObject args = new JsonObject();
        args.addProperty("file", FILE_ID);
        args.addProperty("type", type.name());
        args.addProperty("portfolio", portfolio.getUUID());
        args.addProperty("security", security.getUUID());
        args.addProperty("date", "2024-04-04");
        args.addProperty("shares", shares);
        args.addProperty("amount", amount);
        args.addProperty("fee", fee);
        args.addProperty("tax", tax);
        args.addProperty("note", note);

        JsonObject result = callToolObject("add_transaction", args);
        assertThat(result.get("id"), is(notNullValue()));
    }

    private void addSimpleAccountTransaction(AccountTransaction.Type type, double amount, Security txSecurity,
                    String note) throws Exception
    {
        addSimpleAccountTransactionOnAccount(account, type, amount, txSecurity, note);
    }

    private void addSimpleAccountTransactionOnAccount(Account target, AccountTransaction.Type type, double amount,
                    String note) throws Exception
    {
        addSimpleAccountTransactionOnAccount(target, type, amount, null, note);
    }

    private void addSimpleAccountTransactionOnAccount(Account target, AccountTransaction.Type type, double amount,
                    Security txSecurity, String note) throws Exception
    {
        JsonObject args = new JsonObject();
        args.addProperty("file", FILE_ID);
        args.addProperty("type", type.name());
        args.addProperty("account", target.getUUID());
        args.addProperty("date", "2024-01-15");
        args.addProperty("amount", amount);
        args.addProperty("note", note);
        if (txSecurity != null)
            args.addProperty("security", txSecurity.getUUID());

        JsonObject result = callToolObject("add_transaction", args);
        assertThat(result.get("id"), is(notNullValue()));
    }

    private void addSecurityTransaction(PortfolioTransaction.Type type, double shares, double amount, String note)
                    throws Exception
    {
        JsonObject args = new JsonObject();
        args.addProperty("file", FILE_ID);
        args.addProperty("type", type.name());
        args.addProperty("portfolio", portfolio.getUUID());
        args.addProperty("account", account.getUUID());
        args.addProperty("security", security.getUUID());
        args.addProperty("date", "2024-02-01");
        args.addProperty("shares", shares);
        args.addProperty("amount", amount);
        args.addProperty("note", note);

        JsonObject result = callToolObject("add_transaction", args);
        assertThat(result.get("portfolioTransactionId"), is(notNullValue()));
        assertThat(result.get("accountTransactionId"), is(notNullValue()));
    }

    private void assertAccountTransaction(AccountTransaction.Type type, double amount, Security txSecurity)
    {
        AccountTransaction tx = account.getTransactions().stream().filter(t -> t.getType() == type).findFirst()
                        .orElseThrow();
        assertThat(tx.getAmount(), is(Values.Amount.factorize(amount)));
        if (txSecurity != null)
            assertThat(tx.getSecurity(), is(txSecurity));
    }

    private void assertPortfolioTransaction(PortfolioTransaction.Type type, double shares, double amount)
    {
        PortfolioTransaction tx = portfolio.getTransactions().stream().filter(t -> t.getType() == type).findFirst()
                        .orElseThrow();
        assertThat(tx.getShares(), is(Values.Share.factorize(shares)));
        assertThat(tx.getAmount(), is(Values.Amount.factorize(amount)));
    }

    private JsonObject callToolObject(String toolName, JsonObject arguments)
    {
        String text = extractToolResultText(invokeToolCall(toolName, arguments));
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private JsonArray callToolArray(String toolName, JsonObject arguments)
    {
        String text = extractToolResultText(invokeToolCall(toolName, arguments));
        return JsonParser.parseString(text).getAsJsonArray();
    }

    private JsonObject invokeToolCall(String toolName, JsonObject arguments)
    {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 1);
        request.addProperty("method", "tools/call");

        JsonObject params = new JsonObject();
        params.addProperty("name", toolName);
        params.add("arguments", arguments);
        request.add("params", params);

        return server.handle(request);
    }

    private String extractToolResultText(JsonObject response)
    {
        assertThat("Expected success but got error: " + response, response.has("error"), is(false));
        return response.getAsJsonObject("result").getAsJsonArray("content").get(0).getAsJsonObject().get("text")
                        .getAsString();
    }
}
