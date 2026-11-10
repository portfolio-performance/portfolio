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
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class HoldingsTest
{
    /** calls the handler with a factory of its own, as the host would provide it */
    private static JsonElement list(Client client, String date, String currency)
    {
        return HoldingsHandler.list(client, new ExchangeRateProviderFactory(client), date, currency);
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

    /**
     * Every amount has been converted into the reporting currency, so the
     * report states it: a client must not have to infer it from an arbitrary
     * nested money object. Without an override it is the file's base currency.
     */
    @Test
    public void testReportingCurrencyDefaultsToBaseCurrency()
    {
        var client = new Client();
        new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();

        assertThat(holdings.get("reportingCurrency").getAsString(), is(client.getBaseCurrency()));
        assertThat(holdings.get("reportingCurrency").getAsString(), is("EUR"));
    }

    @Test
    public void testCurrencyOverride()
    {
        var client = new Client();
        var account = new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);

        var holdings = list(client, "2026-07-20", "USD").getAsJsonObject();

        assertThat(holdings.get("reportingCurrency").getAsString(), is("USD"));
        assertThat(holdings.get("totalAssets").getAsJsonObject().get("currency").getAsString(), is("USD"));

        var item = findByUuid(holdings, account.getUUID());
        assertThat(item.get("valuation").getAsJsonObject().get("currency").getAsString(), is("USD"));
        assertThat(item.get("localValuation").getAsJsonObject().get("currency").getAsString(), is("EUR"));
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
            assertThat(e.getErrors().get(0).field(), is("reportingCurrency"));
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
