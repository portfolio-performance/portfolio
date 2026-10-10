package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class TransactionsReadTest
{
    private static final String PATH = "/tmp/x.portfolio";

    private Client client;
    private Security eurSecurity;
    private Security usdSecurity;
    private Account cash;
    private Account savings;
    private Portfolio broker;

    private PortfolioTransaction eurBuy;
    private BuySellEntry usdBuy;
    private AccountTransferEntry transfer;
    private AccountTransaction dividend;
    private AccountTransaction deposit;

    @Before
    public void setUp()
    {
        client = new Client();
        eurSecurity = new SecurityBuilder().addTo(client);
        usdSecurity = new SecurityBuilder(CurrencyUnit.USD).addTo(client);

        cash = new AccountBuilder() //
                        .deposit_("2026-01-02", Values.Amount.factorize(10000)) //
                        .dividend("2026-04-01", Values.Amount.factorize(50), eurSecurity) //
                        .addTo(client);
        savings = new AccountBuilder().addTo(client);

        broker = new PortfolioBuilder(cash) //
                        .buy(eurSecurity, "2026-02-01", Values.Share.factorize(10), Values.Amount.factorize(1000))
                        .addTo(client);

        deposit = cash.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT)
                        .findFirst().orElseThrow();
        dividend = cash.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().orElseThrow();
        eurBuy = broker.getTransactions().get(0);

        // a buy of a USD instrument paid from the EUR account: gross value in USD
        usdBuy = new BuySellEntry(broker, cash);
        usdBuy.setType(PortfolioTransaction.Type.BUY);
        usdBuy.setDate(LocalDateTime.of(2026, 3, 2, 9, 30));
        usdBuy.setSecurity(usdSecurity);
        usdBuy.setShares(Values.Share.factorize(12.5));
        usdBuy.setCurrencyCode(CurrencyUnit.EUR);
        usdBuy.setAmount(Values.Amount.factorize(1157.32));
        usdBuy.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1152.42)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1265)), new BigDecimal("0.911")));
        usdBuy.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.90))));
        usdBuy.setSource("api:broker-1");
        usdBuy.insert();

        transfer = new AccountTransferEntry(cash, savings);
        transfer.setDate(LocalDateTime.of(2026, 5, 1, 0, 0));
        transfer.setAmount(Values.Amount.factorize(500));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();
    }

    private static List<String> uuids(JsonObject list)
    {
        var answer = new ArrayList<String>();
        list.getAsJsonArray("items").forEach(item -> answer.add(item.getAsJsonObject().get("uuid").getAsString()));
        return answer;
    }

    private JsonObject list(TransactionsHandler.Filter filter)
    {
        return TransactionsHandler.list(client, filter).getAsJsonObject();
    }

    private static TransactionsHandler.Filter filter(String from, String to, String type, String instrument,
                    String cashAccount, String investmentAccount)
    {
        return new TransactionsHandler.Filter(from, to, type, instrument, cashAccount, investmentAccount);
    }

    @Test
    public void testUnfilteredListReportsEachPairOnce()
    {
        assertThat(uuids(list(TransactionsHandler.Filter.NONE)), containsInAnyOrder(deposit.getUUID(),
                        dividend.getUUID(), eurBuy.getUUID(), usdBuy.getPortfolioTransaction().getUUID(),
                        transfer.getSourceTransaction().getUUID()));
    }

    @Test
    public void testDateFilterIsInclusive()
    {
        assertThat(uuids(list(filter("2026-02-01", "2026-04-01", null, null, null, null))), containsInAnyOrder(
                        eurBuy.getUUID(), usdBuy.getPortfolioTransaction().getUUID(), dividend.getUUID()));
    }

    @Test
    public void testTypeFilterAcceptsAList()
    {
        assertThat(uuids(list(filter(null, null, "buy", null, null, null))),
                        containsInAnyOrder(eurBuy.getUUID(), usdBuy.getPortfolioTransaction().getUUID()));
        assertThat(uuids(list(filter(null, null, "deposit, transfer-out", null, null, null))),
                        containsInAnyOrder(deposit.getUUID(), transfer.getSourceTransaction().getUUID()));
    }

    @Test
    public void testInstrumentFilter()
    {
        assertThat(uuids(list(filter(null, null, null, eurSecurity.getUUID(), null, null))),
                        containsInAnyOrder(eurBuy.getUUID(), dividend.getUUID()));
    }

    @Test
    public void testCashAccountFilterMatchesEitherLeg()
    {
        // buys are listed as their investment-account leg but paid from cash
        assertThat(uuids(list(filter(null, null, null, null, cash.getUUID(), null))),
                        containsInAnyOrder(deposit.getUUID(), dividend.getUUID(), eurBuy.getUUID(),
                                        usdBuy.getPortfolioTransaction().getUUID(),
                                        transfer.getSourceTransaction().getUUID()));

        // the inbound leg of a transfer is not listed, but the transfer is
        assertThat(uuids(list(filter(null, null, null, null, savings.getUUID(), null))),
                        containsInAnyOrder(transfer.getSourceTransaction().getUUID()));
    }

    @Test
    public void testInvestmentAccountFilter()
    {
        assertThat(uuids(list(filter(null, null, null, null, null, broker.getUUID()))),
                        containsInAnyOrder(eurBuy.getUUID(), usdBuy.getPortfolioTransaction().getUUID()));
    }

    @Test
    public void testFiltersCombine()
    {
        assertThat(uuids(list(filter("2026-03-01", null, "buy", null, cash.getUUID(), null))),
                        containsInAnyOrder(usdBuy.getPortfolioTransaction().getUUID()));
    }

    @Test
    public void testInvalidFiltersAre400()
    {
        for (var invalid : List.of(filter("2026-13-01", null, null, null, null, null),
                        filter(null, "yesterday", null, null, null, null), filter(null, null, "purchase", null, null, null)))
        {
            try
            {
                list(invalid);
                Assert.fail("expected ApiException for " + invalid);
            }
            catch (ApiException e)
            {
                assertThat(e.getStatus(), is(400));
                assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            }
        }

        try
        {
            list(filter("2026-05-01", "2026-04-01", null, null, null, null));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).code(), is("invalid-range"));
        }
    }

    @Test
    public void testGetResolvesEitherLegOfBuyToThePortfolioLeg()
    {
        var byPortfolioLeg = TransactionsHandler.get(client, usdBuy.getPortfolioTransaction().getUUID())
                        .getAsJsonObject();
        var byAccountLeg = TransactionsHandler.get(client, usdBuy.getAccountTransaction().getUUID())
                        .getAsJsonObject();

        assertThat(byAccountLeg, is(byPortfolioLeg));
        assertThat(byPortfolioLeg.get("uuid").getAsString(), is(usdBuy.getPortfolioTransaction().getUUID()));
        assertThat(byPortfolioLeg.getAsJsonObject("owner").get("type").getAsString(), is("investment-account"));

        var linked = byPortfolioLeg.getAsJsonObject("linked");
        assertThat(linked.get("uuid").getAsString(), is(usdBuy.getAccountTransaction().getUUID()));
        assertThat(linked.get("type").getAsString(), is("buy"));
        assertThat(linked.getAsJsonObject("owner").get("uuid").getAsString(), is(cash.getUUID()));
        assertThat(linked.getAsJsonObject("owner").get("type").getAsString(), is("cash-account"));
    }

    @Test
    public void testGetSerializesUnitsWithForeignCurrency()
    {
        var json = TransactionsHandler.get(client, usdBuy.getPortfolioTransaction().getUUID()).getAsJsonObject();

        var units = json.getAsJsonArray("units");
        assertThat(units.size(), is(2));

        var gross = units.get(0).getAsJsonObject();
        assertThat(gross.get("type").getAsString(), is("gross-value"));
        assertThat(gross.getAsJsonObject("amount").get("value").getAsBigDecimal(), is(new BigDecimal("1152.42")));
        assertThat(gross.getAsJsonObject("amount").get("currency").getAsString(), is("EUR"));
        assertThat(gross.getAsJsonObject("forex").get("value").getAsBigDecimal(), is(new BigDecimal("1265")));
        assertThat(gross.getAsJsonObject("forex").get("currency").getAsString(), is("USD"));
        assertThat(gross.get("exchangeRate").getAsBigDecimal(), is(new BigDecimal("0.911")));

        var fee = units.get(1).getAsJsonObject();
        assertThat(fee.get("type").getAsString(), is("fee"));
        assertThat(fee.getAsJsonObject("amount").get("value").getAsBigDecimal(), is(new BigDecimal("4.9")));
        assertThat(fee.has("forex"), is(false));
        assertThat(fee.has("exchangeRate"), is(false));

        assertThat(json.get("source").getAsString(), is("api:broker-1"));
        assertThat(json.has("updatedAt"), is(true));
        assertThat(json.get("shares").getAsBigDecimal(), is(new BigDecimal("12.5")));
    }

    @Test
    public void testGetResolvesInboundTransferLegToOutbound()
    {
        var json = TransactionsHandler.get(client, transfer.getTargetTransaction().getUUID()).getAsJsonObject();

        assertThat(json.get("uuid").getAsString(), is(transfer.getSourceTransaction().getUUID()));
        assertThat(json.get("type").getAsString(), is("transfer-out"));
        assertThat(json.getAsJsonObject("linked").get("uuid").getAsString(),
                        is(transfer.getTargetTransaction().getUUID()));
        assertThat(json.getAsJsonObject("linked").get("type").getAsString(), is("transfer-in"));
        assertThat(json.getAsJsonObject("linked").getAsJsonObject("owner").get("uuid").getAsString(),
                        is(savings.getUUID()));
    }

    @Test
    public void testGetSingleLegTransaction()
    {
        dividend.setExDate(LocalDateTime.of(2026, 3, 28, 0, 0));

        var json = TransactionsHandler.get(client, dividend.getUUID()).getAsJsonObject();

        assertThat(json.get("type").getAsString(), is("dividends"));
        assertThat(json.get("exDate").getAsString(), is("2026-03-28T00:00"));
        assertThat(json.has("linked"), is(false));
        assertThat(json.get("source"), is(nullValue()));
        assertThat(json.getAsJsonArray("units").size(), is(0));
    }

    @Test
    public void testGetUnknownIs404()
    {
        try
        {
            TransactionsHandler.get(client, UUID.randomUUID().toString());
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testRoutesPassFiltersAndRunOnUIThread() throws Exception
    {
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        try
        {
            var registry = new FileAccessRegistry(node);
            registry.setEnabled(PATH, true);
            var fileId = registry.byPath(PATH).orElseThrow().uuid();
            var host = new FakeHost(List.of(new FakeHost.FakeOpenFile(PATH, "x", client)));
            var router = ApiRoutes.create(registry, host,
                            new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));

            var path = "/v1/files/" + fileId + "/transactions";
            var match = router.match("GET", path);
            var response = match.handler().handle(
                            new Request("GET", path, match.pathParams(), Map.of("type", "dividends"), new byte[0]));
            var body = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertThat(uuids(body), containsInAnyOrder(dividend.getUUID()));

            path = "/v1/files/" + fileId + "/transactions/" + usdBuy.getAccountTransaction().getUUID();
            match = router.match("GET", path);
            response = match.handler().handle(new Request("GET", path, match.pathParams(), new byte[0]));
            body = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertThat(body.get("uuid").getAsString(), is(usdBuy.getPortfolioTransaction().getUUID()));

            assertThat(host.hasAccessedOutsideUIThread(), is(false));
        }
        finally
        {
            node.removeNode();
        }
    }
}
