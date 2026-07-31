package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class HoldingsTest
{
    /** calls the handler with a factory of its own, as the host would provide it */
    private static JsonElement list(Client client, String date, String currency)
    {
        return list(client, date, null, currency, null);
    }

    private static JsonElement list(Client client, String date, String openingDate, String currency,
                    String costMethod)
    {
        return HoldingsHandler.list(client, new ExchangeRateProviderFactory(client), date, openingDate, currency,
                        costMethod);
    }

    @Test
    public void testInstrumentAndCashHolding()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        // deposit 1500, of which 1000 buys the shares: 500 cash remains
        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(1500)) //
                        .addTo(client);
        account.setName("Cash");

        new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();

        assertThat(holdings.get("date").getAsString(), is("2026-07-20"));

        var total = holdings.get("totalAssets").getAsJsonObject();
        assertThat(total.get("value").getAsDouble(), is(1600d));
        assertThat(total.get("currency").getAsString(), is("EUR"));

        var items = holdings.get("items").getAsJsonArray();
        assertThat(items.size(), is(2));

        // sorted by name: ACME before Cash
        var instrument = items.get(0).getAsJsonObject();
        assertThat(instrument.get("type").getAsString(), is("instrument"));
        assertThat(instrument.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(instrument.get("name").getAsString(), is("ACME"));
        assertThat(instrument.get("shares").getAsDouble(), is(10d));
        assertThat(instrument.get("weight").getAsDouble(), closeTo(0.6875, 1e-10));
        assertThat(instrument.has("localValuation"), is(false));

        // the price used for the valuation, including the date it is from -
        // which may be well before the snapshot date
        var price = instrument.get("price").getAsJsonObject();
        assertThat(price.get("value").getAsDouble(), is(110d));
        assertThat(price.get("currency").getAsString(), is("EUR"));
        assertThat(price.get("date").getAsString(), is("2026-07-01"));

        var valuation = instrument.get("valuation").getAsJsonObject();
        assertThat(valuation.get("value").getAsDouble(), is(1100d));
        assertThat(valuation.get("currency").getAsString(), is("EUR"));

        var cash = items.get(1).getAsJsonObject();
        assertThat(cash.get("type").getAsString(), is("cash-account"));
        assertThat(cash.get("uuid").getAsString(), is(account.getUUID()));
        assertThat(cash.get("name").getAsString(), is("Cash"));
        assertThat(cash.has("shares"), is(false));
        assertThat(cash.has("price"), is(false));
        assertThat(cash.get("valuation").getAsJsonObject().get("value").getAsDouble(), is(500d));
        assertThat(cash.get("weight").getAsDouble(), closeTo(0.3125, 1e-10));
    }

    /**
     * Decimal values must be written as plain decimal literals - a BigDecimal
     * like 1.1E+3 would be valid JSON but hostile to every consumer.
     */
    @Test
    public void testNumbersAreSerializedAsPlainDecimals()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        // inbound delivery: no cash account involved, the instrument is the
        // only holding
        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(instrument.get("shares").toString(), is("10"));
        assertThat(instrument.get("price").getAsJsonObject().get("value").toString(), is("110"));
        assertThat(instrument.get("valuation").getAsJsonObject().get("value").toString(), is("1100"));
        assertThat(holdings.get("totalAssets").getAsJsonObject().get("value").toString(), is("1100"));
    }

    /**
     * Shares carry 8 decimals, so a dust position lands at a scale where
     * BigDecimal#toString switches to scientific notation (5E-7) - which the
     * schema promises never to emit.
     */
    @Test
    public void testDustSharesAreNotSerializedAsScientificNotation()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(0.0000005),
                                        Values.Amount.factorize(1)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(instrument.get("shares").toString(), is("0.0000005"));
    }

    /**
     * The weight of a dust position is a tiny fraction; as a raw double it
     * would render as 1.0E-7.
     */
    @Test
    public void testDustWeightIsNotSerializedAsScientificNotation()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(1)) //
                        .addTo(client);
        security.setName("A dust");

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(0.01),
                                        Values.Amount.factorize(1)) //
                        .addTo(client);

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(1000000)) //
                        .addTo(client);
        account.setName("B cash");

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var dust = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(dust.get("weight").toString(), not(containsString("E")));
        assertThat(dust.get("weight").getAsDouble(), closeTo(1e-8, 1e-12));
    }

    @Test
    public void testDateDefaultsToToday()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var holdings = list(client, null, null).getAsJsonObject();
        assertThat(holdings.get("date").getAsString(), is(LocalDate.now().toString()));
    }

    /**
     * A date before any transaction is a legitimate question: cash accounts
     * exist with a zero balance (the model reports them), zero-share instrument
     * positions are filtered by the model, and the weight must not become NaN
     * when total assets are zero.
     */
    @Test
    public void testDateBeforeAnyTransaction()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(500)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2000-01-01", null).getAsJsonObject();

        assertThat(holdings.get("totalAssets").getAsJsonObject().get("value").getAsDouble(), is(0d));

        var items = holdings.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));

        var cash = items.get(0).getAsJsonObject();
        assertThat(cash.get("type").getAsString(), is("cash-account"));
        assertThat(cash.get("valuation").getAsJsonObject().get("value").getAsDouble(), is(0d));
        assertThat(cash.get("weight").getAsDouble(), is(0d));
    }

    /**
     * Without an exchange rate series the converter falls back to 1:1 (UI
     * parity). The unconverted value is reported as localValuation whenever the
     * holding's currency differs from the reporting currency.
     */
    @Test
    public void testForeignCurrencyHoldingCarriesLocalValuation()
    {
        var client = new Client();

        var eur = new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);
        eur.setName("A EUR");
        var usd = new AccountBuilder("USD").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);
        usd.setName("B USD");

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var items = holdings.get("items").getAsJsonArray();

        var eurItem = items.get(0).getAsJsonObject();
        assertThat(eurItem.has("localValuation"), is(false));

        var usdItem = items.get(1).getAsJsonObject();
        assertThat(usdItem.get("valuation").getAsJsonObject().get("currency").getAsString(), is("EUR"));
        var local = usdItem.get("localValuation").getAsJsonObject();
        assertThat(local.get("value").getAsDouble(), is(100d));
        assertThat(local.get("currency").getAsString(), is("USD"));
    }

    @Test
    public void testCurrencyOverride()
    {
        var client = new Client();
        var account = new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);

        var holdings = list(client, "2026-07-20", "USD").getAsJsonObject();

        assertThat(holdings.get("totalAssets").getAsJsonObject().get("currency").getAsString(), is("USD"));

        var item = findByUuid(holdings, account.getUUID());
        assertThat(item.get("valuation").getAsJsonObject().get("currency").getAsString(), is("USD"));
        assertThat(item.get("localValuation").getAsJsonObject().get("currency").getAsString(), is("EUR"));
    }

    /**
     * The cost basis is enriched only for instruments, over the reporting
     * period (default: since inception). "gross" includes fees/taxes in the
     * cost basis (the total economic outlay); "net" excludes them - matching
     * the desktop's "purchase price"/"gross purchase price" columns.
     */
    @Test
    public void testCostBasisGrossAndNet()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        // 10 shares, 1050 paid in total, of which 50 is a fee: the pure share
        // cost (net of fees) is 1000, i.e. 100/share
        var account = new PortfolioBuilder() //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1050),
                                        Values.Amount.factorize(50), 0) //
                        .addTo(client).getReferenceAccount();

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = findByUuid(holdings, security.getUUID());

        var purchasePrice = instrument.get("purchasePrice").getAsJsonObject();
        assertThat(purchasePrice.get("net").getAsJsonObject().get("value").getAsDouble(), is(100d));
        assertThat(purchasePrice.get("gross").getAsJsonObject().get("value").getAsDouble(), is(105d));

        var purchaseValue = instrument.get("purchaseValue").getAsJsonObject();
        assertThat(purchaseValue.get("net").getAsJsonObject().get("value").getAsDouble(), is(1000d));
        assertThat(purchaseValue.get("gross").getAsJsonObject().get("value").getAsDouble(), is(1050d));

        // a cash account never carries a cost basis
        var cash = findByUuid(holdings, account.getUUID());
        assertThat(cash.has("purchasePrice"), is(false));
        assertThat(cash.has("purchaseValue"), is(false));
    }

    /**
     * profitLoss is the unrealized gain against the (gross) cost basis at the
     * chosen cost method; delta is a cost-method independent "market value +
     * sells + dividends - purchase costs" figure that, absent any sells or
     * dividends, coincides with profitLoss here.
     */
    @Test
    public void testProfitLossAndDelta()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-20", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        new PortfolioBuilder() //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = findByUuid(holdings, security.getUUID());

        // valuation 1100, cost basis (no fees here) 1000: profit 100, 10%
        var profitLoss = instrument.get("profitLoss").getAsJsonObject();
        assertThat(profitLoss.get("value").getAsJsonObject().get("value").getAsDouble(), is(100d));
        assertThat(profitLoss.get("percent").getAsDouble(), closeTo(0.1, 1e-10));

        assertThat(instrument.get("delta").getAsJsonObject().get("value").getAsDouble(), is(100d));
        assertThat(instrument.get("deltaPercent").getAsDouble(), closeTo(0.1, 1e-10));
    }

    /**
     * TTWROR/IRR are cost-method independent and delegate straight to
     * {@code LazySecurityPerformanceRecord} - this only pins down that they
     * are wired up and land as finite fractions, not the exact math (already
     * covered by that class's own tests).
     */
    @Test
    public void testTtwrorAndIrrArePresent()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-20", Values.Quote.factorize(121)) //
                        .addTo(client);
        security.setName("ACME");

        new PortfolioBuilder() //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = findByUuid(holdings, security.getUUID());

        assertThat(instrument.get("ttwror").getAsDouble(), closeTo(0.21, 1e-10));
        assertThat(instrument.get("ttwrorAnnualized").getAsDouble() > 0, is(true));
        assertThat(instrument.get("irr").getAsDouble() > 0, is(true));
    }

    /**
     * The dividend sum/rate of return come from the record (same period as
     * cost basis); the next ex-date/payment-date/amount come straight from the
     * security's own dividend calendar events and are always relative to
     * today, independent of the valuation date - so they only show up when an
     * event is still in the future.
     */
    @Test
    public void testDividends()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(1500)) //
                        .dividend("2026-03-01", Values.Amount.factorize(50), security) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var future = LocalDate.now().plusMonths(1);
        var payDate = future.plusDays(5);
        security.addEvent(new DividendEvent(future, payDate, Money.of("EUR", Values.Amount.factorize(45)), "test"));

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = findByUuid(holdings, security.getUUID());

        var dividends = instrument.get("dividends").getAsJsonObject();
        assertThat(dividends.get("sum").getAsJsonObject().get("value").getAsDouble(), is(50d));
        assertThat(dividends.get("totalRateOfReturn").getAsDouble(), closeTo(0.05, 1e-10));
        assertThat(dividends.get("nextExDate").getAsString(), is(future.toString()));
        assertThat(dividends.get("nextPaymentDate").getAsString(), is(payDate.toString()));
        assertThat(dividends.get("nextPaymentAmount").getAsJsonObject().get("value").getAsDouble(), is(45d));
    }

    /**
     * A taxonomy classifies an instrument or a cash account alike (e.g. an
     * asset-allocation taxonomy commonly classifies cash accounts too); the
     * root node itself never appears in {@code path}.
     */
    @Test
    public void testClassifications()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        var taxonomy = new Taxonomy("Asset Allocation");
        var root = new Classification("root", "Root");
        taxonomy.setRootNode(root);
        var equities = new Classification(root, "equities", "Equities");
        root.addChild(equities);
        equities.addAssignment(new Classification.Assignment(security));
        client.addTaxonomy(taxonomy);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = findByUuid(holdings, security.getUUID());

        var classifications = instrument.get("classifications").getAsJsonObject();
        var assignment = classifications.get(taxonomy.getId()).getAsJsonArray().get(0).getAsJsonObject();
        var path = assignment.get("path").getAsJsonArray();
        assertThat(path.size(), is(1));
        assertThat(path.get(0).getAsString(), is("Equities"));
        assertThat(assignment.get("weight").getAsDouble(), is(1d));
    }

    @Test
    public void testOpeningDateMustPrecedeDate()
    {
        try
        {
            list(new Client(), "2026-01-01", "2026-06-01", null, null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("openingDate"));
            assertThat(e.getErrors().get(0).code(), is("invalid-range"));
        }
    }

    @Test
    public void testInvalidOpeningDateIs400()
    {
        try
        {
            list(new Client(), null, "not-a-date", null, null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("openingDate"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    @Test
    public void testInvalidCostMethodIs400()
    {
        try
        {
            list(new Client(), null, null, null, "not-a-method");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("costMethod"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    @Test
    public void testInvalidDateIs400()
    {
        try
        {
            list(new Client(), "not-a-date", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("date"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        try
        {
            list(new Client(), null, "ZZZ");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("currency"));
            assertThat(e.getErrors().get(0).code(), is("unknown-currency"));
        }
    }

    private static JsonObject findByUuid(JsonObject holdings, String uuid)
    {
        for (var element : holdings.get("items").getAsJsonArray())
        {
            var item = element.getAsJsonObject();
            if (uuid.equals(item.get("uuid").getAsString()))
                return item;
        }
        throw new AssertionError("no item with uuid " + uuid);
    }
}
