package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.amount;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.eur;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.unit;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.usd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class CreateBuySellTest
{
    private TransactionFixture f;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private String buy(String extra)
    {
        return "{'type':'buy','date':'2026-03-02T09:30','investmentAccount':'" + f.broker.getUUID()
                        + "','cashAccount':'" + f.cash.getUUID() + "','instrument':'" + f.eurSecurity.getUUID()
                        + "','shares':'10'" + extra + "}";
    }

    private String usdBuy(String extra)
    {
        return "{'type':'buy','date':'2026-03-02','investmentAccount':'" + f.broker.getUUID() + "','cashAccount':'"
                        + f.cash.getUUID() + "','instrument':'" + f.usdSecurity.getUUID() + "','shares':'12.5'"
                        + extra + "}";
    }

    @Test
    public void testSameCurrencyBuyPutsUnitsOnThePortfolioLegOnly()
    {
        var json = f.create(buy(",'quote':'100.5','fees':'4.90','taxes':'1.10','note':'first'"));

        var pair = f.find(json.get("uuid").getAsString());
        var portfolioTx = (PortfolioTransaction) pair.getTransaction();
        var entry = (BuySellEntry) portfolioTx.getCrossEntry();
        var accountTx = entry.getAccountTransaction();

        assertThat(pair.getOwner(), is(f.broker));
        assertThat(entry.getAccount(), is(f.cash));
        assertThat(f.broker.getTransactions().contains(portfolioTx), is(true));
        assertThat(f.cash.getTransactions().contains(accountTx), is(true));

        assertThat(portfolioTx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(accountTx.getType(), is(AccountTransaction.Type.BUY));
        assertThat(portfolioTx.getDateTime(), is(LocalDateTime.of(2026, 3, 2, 9, 30)));
        assertThat(accountTx.getDateTime(), is(LocalDateTime.of(2026, 3, 2, 9, 30)));
        assertThat(portfolioTx.getSecurity(), is(f.eurSecurity));
        assertThat(accountTx.getSecurity(), is(f.eurSecurity));
        assertThat(accountTx.getNote(), is("first"));

        // shares on the portfolio leg only, as BuySellEntry#setShares does
        assertThat(portfolioTx.getShares(), is(shares("10")));
        assertThat(accountTx.getShares(), is(0L));

        // total = gross + fees + taxes, on both legs
        assertThat(portfolioTx.getMonetaryAmount(), is(eur("1011.00")));
        assertThat(accountTx.getMonetaryAmount(), is(eur("1011.00")));

        assertThat(portfolioTx.getUnitSum(Transaction.Unit.Type.FEE), is(eur("4.90")));
        assertThat(portfolioTx.getUnitSum(Transaction.Unit.Type.TAX), is(eur("1.10")));
        assertThat(portfolioTx.getUnit(Transaction.Unit.Type.GROSS_VALUE).isPresent(), is(false));
        assertThat(accountTx.getUnits().count(), is(0L));

        assertThat(json.get("linked").getAsJsonObject().get("uuid").getAsString(), is(accountTx.getUUID()));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testSellSubtractsFeesAndTaxes()
    {
        var json = f.create(buy(",'quote':'100','fees':'5','taxes':'10'").replace("'buy'", "'sell'"));

        var tx = (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(tx.getMonetaryAmount(), is(eur("985.00")));
        assertThat(((BuySellEntry) tx.getCrossEntry()).getAccountTransaction().getType(),
                        is(AccountTransaction.Type.SELL));
    }

    @Test
    public void testForeignCurrencyBuyWritesGrossValueUnitWithForexAndRate()
    {
        var json = f.create(usdBuy(",'quote':'101.2','exchangeRate':'0.911','fees':'4.90','forexFees':'2'"));

        var tx = (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        var accountTx = ((BuySellEntry) tx.getCrossEntry()).getAccountTransaction();

        // 12.5 x 101.2 = 1265 USD x 0.911 = 1152.415 → 1152.42 EUR
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getAmount(), is(eur("1152.42")));
        assertThat(gross.getForex(), is(usd("1265")));
        assertThat(gross.getExchangeRate(), comparesEqualTo(new BigDecimal("0.911")));

        var forexFee = unit(tx, Transaction.Unit.Type.FEE, true).orElseThrow();
        assertThat(forexFee.getForex(), is(usd("2")));
        assertThat(forexFee.getAmount(), is(eur("1.82")));
        assertThat(unit(tx, Transaction.Unit.Type.FEE, false).orElseThrow().getAmount(), is(eur("4.90")));

        // the transaction is in the cash account's currency
        assertThat(tx.getCurrencyCode(), is("EUR"));
        assertThat(tx.getMonetaryAmount(), is(eur("1159.14")));
        assertThat(accountTx.getMonetaryAmount(), is(eur("1159.14")));
        assertThat(accountTx.getUnits().count(), is(0L));
    }

    @Test
    public void testForeignCurrencyRateIsLookedUpWhenOmitted()
    {
        var json = f.create(usdBuy(",'grossValue':'1000'"));

        var tx = (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getExchangeRate(), comparesEqualTo(TransactionFixture.USD_EUR));
        assertThat(gross.getAmount(), is(eur("900")));
        assertThat(tx.getMonetaryAmount(), is(eur("900")));
    }

    @Test
    public void testExchangeRateRequiredWithoutKnownRate()
    {
        var chf = new name.abuchen.portfolio.junit.SecurityBuilder("CHF").addTo(f.client);
        var e = f.createFails(buy(",'quote':'10'").replace(f.eurSecurity.getUUID(), chf.getUUID()));
        assertError(e, "exchangeRate", "exchange-rate-required");
    }

    @Test
    public void testExchangeRateAndForexFeesRejectedForSameCurrency()
    {
        var e = f.createFails(buy(",'quote':'10','exchangeRate':'1.2','forexFees':'1'"));
        assertError(e, "exchangeRate", "currency-mismatch");
        assertError(e, "forexFees", "currency-mismatch");
    }

    @Test
    public void testExplicitAmountWithinToleranceWins()
    {
        // 12.5 x 101.2 = 1265 USD; at 0.911 exactly 1152.415, at 0.9111 1152.54
        var json = f.create(usdBuy(",'quote':'101.2','exchangeRate':'0.911','amount':'1152.50'"));

        var tx = (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        assertThat(tx.getMonetaryAmount(), is(eur("1152.50")));
        assertThat(tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow().getAmount(), is(eur("1152.50")));
        assertThat(tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow().getForex(), is(usd("1265")));
    }

    @Test
    public void testExplicitAmountOutsideToleranceIsTotalMismatch()
    {
        var e = f.createFails(usdBuy(",'quote':'101.2','exchangeRate':'0.911','amount':'1160'"));
        assertError(e, "amount", "total-mismatch");

        // same currency: within shares x (quote ± 0.01) = 10 x 0.01 = 0.10
        f.create(buy(",'quote':'100','amount':'1000.10'"));
        e = f.createFails(buy(",'quote':'100','amount':'1000.20'"));
        assertError(e, "amount", "total-mismatch");
    }

    @Test
    public void testAmountAloneDerivesTheGrossValue()
    {
        var json = f.create(buy(",'amount':'1005','fees':'5'"));

        var tx = (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        assertThat(tx.getMonetaryAmount(), is(eur("1005")));
        assertThat(tx.getGrossValue(), is(eur("1000")));
    }

    @Test
    public void testGrossValueAndQuoteMustAgree()
    {
        f.create(buy(",'quote':'100','grossValue':'1000.05'"));
        var e = f.createFails(buy(",'quote':'100','grossValue':'1001'"));
        assertError(e, "grossValue", "gross-mismatch");
    }

    @Test
    public void testSellWithZeroTotalMustBeADelivery()
    {
        var e = f.createFails(buy(",'quote':'1','fees':'10'").replace("'buy'", "'sell'"));
        assertError(e, "amount", "zero-total-use-delivery");
    }

    @Test
    public void testMissingAndInvalidFields()
    {
        var e = f.createFails("{'type':'buy','shares':'-1','quote':'1.123456789','fees':'-1','exDate':'2026-01-01'}");
        assertError(e, "investmentAccount", "required");
        assertError(e, "cashAccount", "required");
        assertError(e, "instrument", "required");
        assertError(e, "date", "required");
        assertError(e, "quote", "invalid-value");
        assertError(e, "exDate", "not-allowed-for-type");

        e = f.createFails(buy(",'fees':'-1'"));
        assertError(e, "fees", "must-be-positive");
        assertError(e, "grossValue", "required");

        e = f.createFails(buy(",'quote':'1','bogus':1").replace(f.cash.getUUID(), "no-such-account"));
        assertError(e, "cashAccount", "unknown-reference");
        assertError(e, "bogus", "unknown-field");

        e = f.createFails("{'type':'purchase'}");
        assertError(e, "type", "invalid-value");

        assertThat(f.legCount(), is(0));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testCreateThroughRouterIs201OnTheUIThread() throws Exception
    {
        var response = f.call("POST", "/transactions", Map.of(), buy(",'quote':'100'"));

        assertThat(response.status(), is(201));
        var json = TransactionFixture.body(response);
        assertThat(response.headers().get("Location"), notNullValue());
        assertThat(response.headers().get("Location").endsWith("/transactions/" + json.get("uuid").getAsString()),
                        is(true));
        assertThat(json.get("type").getAsString(), is("buy"));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
        assertThat(f.cash.getTransactions().size(), is(1));
    }

    @Test
    public void testCreateThroughRouterIs423WhileUserEditing() throws Exception
    {
        f.host().setUserEditing(true);
        try
        {
            f.call("POST", "/transactions", Map.of(), buy(",'quote':'100'"));
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(423));
        }
        assertThat(f.legCount(), is(0));
    }

    @Test
    public void testDecimalsAsJsonNumbers()
    {
        var json = f.create(buy(",'quote':100.25").replace("'shares':'10'", "'shares':2"));
        var tx = f.find(json.get("uuid").getAsString()).getTransaction();
        assertThat(tx.getAmount(), is(amount("200.50")));
    }
}
