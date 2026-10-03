package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.eur;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.usd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class UpdateTransactionTest
{
    private TransactionFixture f;

    private BuySellEntry buy;
    private String buyUuid;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();

        buyUuid = f.create("{'type':'buy','date':'2026-03-02T09:30','investmentAccount':'" + f.broker.getUUID()
                        + "','cashAccount':'" + f.cash.getUUID() + "','instrument':'" + f.eurSecurity.getUUID()
                        + "','shares':'10','quote':'100','fees':'5','note':'old'}").get("uuid").getAsString();
        buy = (BuySellEntry) f.find(buyUuid).getTransaction().getCrossEntry();
        f.file.setDirty(false);
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    @Test
    public void testDateAndNotePropagateToBothLegs()
    {
        var accountUuid = buy.getAccountTransaction().getUUID();
        var json = f.patch(buyUuid, "{'date':'2026-04-01T10:15','note':'new'}");

        for (Transaction tx : List.of(buy.getPortfolioTransaction(), buy.getAccountTransaction()))
        {
            assertThat(tx.getDateTime(), is(LocalDateTime.of(2026, 4, 1, 10, 15)));
            assertThat(tx.getNote(), is("new"));
            assertThat(tx.getMonetaryAmount(), is(eur("1005")));
        }
        assertThat(buy.getPortfolioTransaction().getUnitSum(Transaction.Unit.Type.FEE), is(eur("5")));
        assertThat(json.get("uuid").getAsString(), is(buyUuid));
        assertThat(json.getAsJsonObject("linked").get("uuid").getAsString(), is(accountUuid));
        assertThat(buy.getAccountTransaction().getUUID(), is(accountUuid));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testEitherLegAddressesTheTransaction()
    {
        var json = f.patch(buy.getAccountTransaction().getUUID(), "{'note':null}");

        assertThat(json.get("uuid").getAsString(), is(buyUuid));
        assertThat(buy.getPortfolioTransaction().getNote(), nullValue());
        assertThat(buy.getAccountTransaction().getNote(), nullValue());
    }

    @Test
    public void testSharesOnlyOnPortfolioLegAndGrossValueKept()
    {
        f.patch(buyUuid, "{'shares':'20'}");

        assertThat(buy.getPortfolioTransaction().getShares(), is(shares("20")));
        assertThat(buy.getAccountTransaction().getShares(), is(0L));
        assertThat(buy.getPortfolioTransaction().getMonetaryAmount(), is(eur("1005")));

        // with a quote, the gross value is recomputed, on both legs
        f.patch(buyUuid, "{'shares':'20','quote':'100'}");
        assertThat(buy.getPortfolioTransaction().getMonetaryAmount(), is(eur("2005")));
        assertThat(buy.getAccountTransaction().getMonetaryAmount(), is(eur("2005")));
        assertThat(buy.getPortfolioTransaction().getUnitSum(Transaction.Unit.Type.FEE), is(eur("5")));
    }

    @Test
    public void testFeesAndAmountRecompute()
    {
        f.patch(buyUuid, "{'fees':null,'taxes':'2'}");
        assertThat(buy.getPortfolioTransaction().getMonetaryAmount(), is(eur("1002")));
        assertThat(buy.getPortfolioTransaction().getUnitSum(Transaction.Unit.Type.FEE), is(eur("0")));
        assertThat(buy.getPortfolioTransaction().getUnitSum(Transaction.Unit.Type.TAX), is(eur("2")));

        // a new total alone derives the gross value
        f.patch(buyUuid, "{'amount':'1102'}");
        assertThat(buy.getAccountTransaction().getMonetaryAmount(), is(eur("1102")));
        assertThat(buy.getPortfolioTransaction().getGrossValue(), is(eur("1100")));
    }

    @Test
    public void testOwnerChangeKeepsUuidsAndMovesBothLegs()
    {
        var portfolioTx = buy.getPortfolioTransaction();
        var accountTx = buy.getAccountTransaction();

        var json = f.patch(buyUuid, "{'investmentAccount':'" + f.broker2.getUUID() + "','cashAccount':'"
                        + f.cash2.getUUID() + "'}");

        assertThat(f.broker.getTransactions().isEmpty(), is(true));
        assertThat(f.cash.getTransactions().isEmpty(), is(true));
        assertThat(f.broker2.getTransactions(), is(List.of(portfolioTx)));
        assertThat(f.cash2.getTransactions(), is(List.of(accountTx)));
        assertThat(buy.getPortfolio(), is(f.broker2));
        assertThat(buy.getAccount(), is(f.cash2));

        assertThat(portfolioTx.getUUID(), is(buyUuid));
        assertThat(json.get("uuid").getAsString(), is(buyUuid));
        assertThat(json.getAsJsonObject("owner").get("uuid").getAsString(), is(f.broker2.getUUID()));
        assertThat(json.getAsJsonObject("linked").get("uuid").getAsString(), is(accountTx.getUUID()));
        assertThat(json.getAsJsonObject("linked").getAsJsonObject("owner").get("uuid").getAsString(),
                        is(f.cash2.getUUID()));
        assertThat(portfolioTx.getMonetaryAmount(), is(eur("1005")));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCashLegCannotMoveToAnotherCurrency()
    {
        var e = f.patchFails(buyUuid, "{'cashAccount':'" + f.usdCash.getUUID() + "'}");
        assertError(e, "cashAccount", "currency-mismatch");
        assertThat(buy.getAccount(), is(f.cash));
    }

    @Test
    public void testEmptyAndUnchangedPatchesDoNotDirty()
    {
        var result = TransactionsHandler.patch(f.context(false, null), buyUuid, json("{}"));
        assertThat(result.changed(), is(false));
        assertThat(result.entity().get("uuid").getAsString(), is(buyUuid));

        result = TransactionsHandler.patch(f.context(false, null), buyUuid,
                        json("{'note':'old','shares':'10','fees':'5','investmentAccount':'" + f.broker.getUUID()
                                        + "'}"));
        assertThat(result.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testUnknownAndInapplicableFieldsAre422()
    {
        var e = f.patchFails(buyUuid, "{'colour':'red','exDate':'2026-01-01','clientRef':'x'}");
        assertError(e, "colour", "unknown-field");
        assertError(e, "exDate", "not-allowed-for-type");
        assertError(e, "clientRef", "unknown-field");
        assertThat(buy.getPortfolioTransaction().getNote(), is("old"));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testTypeChangeWithinKind()
    {
        f.patch(buyUuid, "{'type':'sell'}");

        assertThat(buy.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(buy.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        // gross 1000 minus fees
        assertThat(buy.getAccountTransaction().getMonetaryAmount(), is(eur("995")));

        var e = f.patchFails(buyUuid, "{'type':'deposit'}");
        assertError(e, "type", "not-allowed-for-type");
        e = f.patchFails(buyUuid, "{'type':'nonsense'}");
        assertError(e, "type", "invalid-value");
    }

    @Test
    public void testTypeChangeMustClearInapplicableValues()
    {
        var uuid = f.create("{'type':'interest','date':'2026-01-31','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'75','taxes':'25'}").get("uuid").getAsString();

        var e = f.patchFails(uuid, "{'type':'deposit'}");
        assertError(e, "taxes", "not-allowed-for-type");

        f.patch(uuid, "{'type':'deposit','taxes':null}");
        var tx = (AccountTransaction) f.find(uuid).getTransaction();
        assertThat(tx.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(tx.getMonetaryAmount(), is(eur("75")));
        assertThat(tx.getUnits().count(), is(0L));
        assertThat(tx.getUUID(), is(uuid));
    }

    @Test
    public void testDividendExDateAndMove()
    {
        var uuid = f.create("{'type':'dividends','date':'2026-07-20','cashAccount':'" + f.cash.getUUID()
                        + "','instrument':'" + f.eurSecurity.getUUID() + "','grossValue':'50','taxes':'5'}")
                        .get("uuid").getAsString();

        var e = f.patchFails(uuid, "{'exDate':'2026-07-21'}");
        assertError(e, "exDate", "ex-date-after-date");

        f.patch(uuid, "{'exDate':'2026-07-10','cashAccount':'" + f.cash2.getUUID() + "'}");
        var tx = (AccountTransaction) f.find(uuid).getTransaction();
        assertThat(tx.getExDate(), is(LocalDateTime.of(2026, 7, 10, 0, 0)));
        assertThat(f.cash2.getTransactions(), is(List.of(tx)));
        assertThat(f.cash.getTransactions().contains(tx), is(false));
        assertThat(tx.getMonetaryAmount(), is(eur("45")));

        f.patch(uuid, "{'exDate':null,'grossValue':'60'}");
        assertThat(tx.getExDate(), nullValue());
        assertThat(tx.getMonetaryAmount(), is(eur("55")));
    }

    @Test
    public void testCashTransferUpdateKeepsLegsConsistent()
    {
        var uuid = f.create("{'type':'cash-transfer','date':'2026-05-01','fromCashAccount':'" + f.cash.getUUID()
                        + "','toCashAccount':'" + f.usdCash.getUUID() + "','amount':'500','targetAmount':'550'}")
                        .get("uuid").getAsString();
        var entry = (AccountTransferEntry) f.find(uuid).getTransaction().getCrossEntry();
        var target = entry.getTargetTransaction();

        // addressed by the inbound leg
        f.patch(target.getUUID(), "{'targetAmount':'560','date':'2026-05-02'}");

        assertThat(entry.getSourceTransaction().getMonetaryAmount(), is(eur("500")));
        assertThat(target.getMonetaryAmount(), is(usd("560")));
        assertThat(target.getDateTime(), is(LocalDateTime.of(2026, 5, 2, 0, 0)));
        assertThat(entry.getSourceTransaction().getDateTime(), is(LocalDateTime.of(2026, 5, 2, 0, 0)));
        var gross = entry.getSourceTransaction().getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getForex(), is(usd("560")));
        assertThat(gross.getAmount(), is(eur("500")));

        var e = f.patchFails(uuid, "{'toCashAccount':'" + f.cash.getUUID() + "'}");
        assertError(e, "toCashAccount", "currency-mismatch");
        e = f.patchFails(uuid, "{'fromCashAccount':'" + f.cash2.getUUID() + "','toCashAccount':'" + f.usdCash.getUUID()
                        + "','type':'security-transfer'}");
        assertError(e, "type", "not-allowed-for-type");

        f.patch(uuid, "{'fromCashAccount':'" + f.cash2.getUUID() + "'}");
        assertThat(f.cash2.getTransactions(), is(List.of(entry.getSourceTransaction())));
        assertThat(f.usdCash.getTransactions(), is(List.of(target)));
        assertThat(target.getUUID(), is(entry.getTargetTransaction().getUUID()));
    }

    @Test
    public void testSecurityTransferMove()
    {
        var uuid = f.create("{'type':'security-transfer','date':'2026-05-01','fromInvestmentAccount':'"
                        + f.broker.getUUID() + "','toInvestmentAccount':'" + f.broker2.getUUID() + "','instrument':'"
                        + f.eurSecurity.getUUID() + "','shares':'3','amount':'300'}").get("uuid").getAsString();
        var entry = (PortfolioTransferEntry) f.find(uuid).getTransaction().getCrossEntry();

        var e = f.patchFails(uuid, "{'toInvestmentAccount':'" + f.broker.getUUID() + "'}");
        assertError(e, "toInvestmentAccount", "same-investment-account");

        f.patch(uuid, "{'shares':'4','amount':'400'}");
        assertThat(entry.getSourceTransaction().getShares(), is(shares("4")));
        assertThat(entry.getTargetTransaction().getShares(), is(shares("4")));
        assertThat(entry.getTargetTransaction().getMonetaryAmount(), is(eur("400")));
    }

    @Test
    public void testForeignCurrencyBuyKeepsRateUnlessPatched()
    {
        var uuid = f.create("{'type':'buy','date':'2026-03-02','investmentAccount':'" + f.broker.getUUID()
                        + "','cashAccount':'" + f.cash.getUUID() + "','instrument':'" + f.usdSecurity.getUUID()
                        + "','shares':'10','grossValue':'1000','exchangeRate':'0.95'}").get("uuid").getAsString();
        var tx = f.find(uuid).getTransaction();

        f.patch(uuid, "{'fees':'1'}");
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getExchangeRate(), comparesEqualTo(new java.math.BigDecimal("0.95")));
        assertThat(tx.getMonetaryAmount(), is(eur("951")));

        f.patch(uuid, "{'exchangeRate':'0.8'}");
        assertThat(tx.getMonetaryAmount(), is(eur("801")));
    }

    @Test
    public void testDryRunPatchChangesNothing()
    {
        var result = TransactionsHandler.patch(f.context(true, null), buyUuid,
                        json("{'note':'preview','investmentAccount':'" + f.broker2.getUUID() + "','quote':'50'}"));

        assertThat(result.changed(), is(false));
        var preview = result.entity();
        assertThat(preview.get("dryRun").getAsBoolean(), is(true));
        assertThat(preview.get("uuid").getAsString(), is(buyUuid));
        assertThat(preview.getAsJsonObject("linked").get("uuid").getAsString(),
                        is(buy.getAccountTransaction().getUUID()));
        assertThat(preview.get("note").getAsString(), is("preview"));
        assertThat(preview.getAsJsonObject("owner").get("uuid").getAsString(), is(f.broker2.getUUID()));
        assertThat(preview.getAsJsonObject("value").get("value").getAsBigDecimal().toPlainString(), is("505"));

        assertThat(buy.getPortfolioTransaction().getNote(), is("old"));
        assertThat(buy.getPortfolio(), is(f.broker));
        assertThat(f.broker.getTransactions().size(), is(1));
        assertThat(buy.getPortfolioTransaction().getMonetaryAmount(), is(eur("1005")));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatchThroughRouter() throws Exception
    {
        var response = f.call("PATCH", "/transactions/" + buyUuid, Map.of(), "{'note':'routed'}");

        assertThat(response.status(), is(200));
        assertThat(TransactionFixture.body(response).get("note").getAsString(), is("routed"));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));

        f.host().setUserEditing(true);
        try
        {
            f.call("PATCH", "/transactions/" + buyUuid, Map.of(), "{'note':'blocked'}");
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
        assertThat(buy.getAccountTransaction().getNote(), is("routed"));
    }

    @Test
    public void testUnknownTransactionIs404()
    {
        try
        {
            f.patch("no-such-uuid", "{'note':'x'}");
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }
}
