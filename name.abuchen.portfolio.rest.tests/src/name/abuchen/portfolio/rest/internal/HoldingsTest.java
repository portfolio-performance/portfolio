package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.google.gson.JsonObject;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;

@SuppressWarnings("nls")
public class HoldingsTest
{
    /** calls the handler with a factory of its own, as the host would provide it */
    private static JsonElement list(Client client, String date, String currency)
    {
        return list(client, date, currency, null, null);
    }

    /** the same, narrowed to one account - an investment account or a cash account */
    private static JsonElement list(Client client, String date, String currency, String investmentAccount,
                    String cashAccount)
    {
        return HoldingsHandler.list(client, null, new ExchangeRateProviderFactory(client), date, currency,
                        investmentAccount, cashAccount, null, null);
    }

    /** the same, asking for one page of the rows */
    private static JsonObject page(Client client, String date, String currency, String investmentAccount,
                    String cashAccount, String cursor, String limit)
    {
        return HoldingsHandler.list(client, null, new ExchangeRateProviderFactory(client), date, currency,
                        investmentAccount, cashAccount, cursor, limit).getAsJsonObject();
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

        // only a manually priced holding says so
        assertThat(instrument.has("priceManual"), is(false));

        // the same staleness, decided instead of left to the caller to compare
        // the two dates: 2026-07-01 is 19 days before the valuation date, and
        // twelve trading days passed in between without a quote
        assertThat(instrument.get("priceStale").getAsBoolean(), is(true));
        assertThat(instrument.get("priceAgeDays").getAsLong(), is(19L));
        assertThat(instrument.get("priceMissedTradingDays").getAsLong(), is(12L));

        var valuation = instrument.get("valuation").getAsJsonObject();
        assertThat(valuation.get("value").getAsDouble(), is(1100d));
        assertThat(valuation.get("currency").getAsString(), is("EUR"));

        var cash = items.get(1).getAsJsonObject();
        assertThat(cash.get("type").getAsString(), is("cash-account"));
        assertThat(cash.get("uuid").getAsString(), is(account.getUUID()));
        assertThat(cash.get("name").getAsString(), is("Cash"));
        assertThat(cash.has("shares"), is(false));
        assertThat(cash.has("price"), is(false));
        assertThat(cash.has("priceStale"), is(false));
        assertThat(cash.has("priceAgeDays"), is(false));
        assertThat(cash.has("priceMissedTradingDays"), is(false));
        assertThat(cash.get("valuation").getAsJsonObject().get("value").getAsDouble(), is(500d));
        assertThat(cash.get("weight").getAsDouble(), closeTo(0.3125, 1e-10));
    }

    // // scope: the same statement narrowed to one account

    private record Fixture(Client client, Security security, Account cash, Portfolio depot)
    {
    }

    /** ACME worth 1100 in a depot, 500 in its reference cash account, 1600 across the file */
    private static Fixture fixture()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        var cash = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(1500)) //
                        .addTo(client);
        cash.setName("Cash");

        var depot = new PortfolioBuilder(cash) //
                        .buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);
        depot.setName("Depot");

        return new Fixture(client, security, cash, depot);
    }

    /** sent without an account, the answer is exactly what it was: no scope, file-wide totals */
    @Test
    public void testWithoutScopeNothingIsAdded()
    {
        var f = fixture();
        var holdings = list(f.client(), "2026-07-20", null).getAsJsonObject();

        assertThat(holdings.has("scope"), is(false));
        assertThat(holdings.get("items").getAsJsonArray().size(), is(2));
        assertThat(holdings.get("totalAssets").getAsJsonObject().get("value").getAsDouble(), is(1600d));
    }

    @Test
    public void testScopedToAnInvestmentAccount()
    {
        var f = fixture();
        var holdings = list(f.client(), "2026-07-20", null, f.depot().getUUID(), null).getAsJsonObject();

        // the scope says once what a column on every row would repeat
        var scope = holdings.get("scope").getAsJsonObject();
        assertThat(scope.get("type").getAsString(), is("investment-account"));
        assertThat(scope.get("uuid").getAsString(), is(f.depot().getUUID()));
        assertThat(scope.get("name").getAsString(), is("Depot"));
        assertThat(scope.get("referenceCashAccount").getAsJsonObject().get("uuid").getAsString(), //$NON-NLS-1$
                        is(f.cash().getUUID()));

        // only the position: the settlement cash is a cash account of its own, so
        // it belongs to a call of its own, not to these rows
        var items = holdings.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));

        var position = items.get(0).getAsJsonObject();
        assertThat(position.get("uuid").getAsString(), is(f.security().getUUID()));
        assertThat(position.has("investmentAccount"), is(false));

        // the row is stated exactly as in the file-wide report - same value, same
        // price - while the total and the weight are the account's: 1100 of 1100,
        // not the file's 1600 and 0.6875
        assertThat(position.get("valuation").getAsJsonObject().get("value").getAsDouble(), is(1100d));
        assertThat(position.get("price").getAsJsonObject().get("value").getAsDouble(), is(110d));
        assertThat(position.get("weight").getAsDouble(), closeTo(1d, 1e-10));

        assertThat(holdings.get("totalAssets").getAsJsonObject().get("value").getAsDouble(), is(1100d));
    }

    @Test
    public void testScopedToACashAccount()
    {
        var f = fixture();
        var holdings = list(f.client(), "2026-07-20", null, null, f.cash().getUUID()).getAsJsonObject();

        assertThat(holdings.get("scope").getAsJsonObject().get("type").getAsString(), is("cash-account"));
        assertThat(holdings.get("scope").getAsJsonObject().has("referenceCashAccount"), is(false));

        var items = holdings.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("type").getAsString(), is("cash-account"));
        assertThat(items.get(0).getAsJsonObject().get("valuation").getAsJsonObject().get("value").getAsDouble(),
                        is(500d));

        assertThat(holdings.get("totalAssets").getAsJsonObject().get("value").getAsDouble(), is(500d));
    }

    @Test
    public void testUnknownAccountUuidIs400()
    {
        var f = fixture();

        try
        {
            list(f.client(), "2026-07-20", null, "no-such-account", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("investmentAccount"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    @Test
    public void testBothScopesAtOnceIs400()
    {
        var f = fixture();

        try
        {
            list(f.client(), "2026-07-20", null, f.depot().getUUID(), f.cash().getUUID());
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("investmentAccount"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    /** a quote dated exactly the valuation date is younger than it, not stale */
    @Test
    public void testQuoteOnTheValuationDateIsNotStale()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-20", Values.Quote.factorize(110)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(instrument.get("priceStale").getAsBoolean(), is(false));
        assertThat(instrument.get("priceAgeDays").getAsLong(), is(0L));
    }

    /**
     * A weekend is not a missed quote: valued on a Sunday, Friday's close is two
     * days old and as fresh as a quote can be.
     */
    @Test
    public void testFridaysQuoteOnASundayIsNotStale()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-10-23", Values.Quote.factorize(110)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-10-25", null).getAsJsonObject();
        var instrument = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(instrument.get("priceStale").getAsBoolean(), is(false));
        assertThat(instrument.get("priceAgeDays").getAsLong(), is(2L));
        assertThat(instrument.get("priceMissedTradingDays").getAsLong(), is(0L));
    }

    /**
     * Reported in review: a price kept by hand changes when the user decides,
     * not every trading day, and the data quality view ends a manual
     * instrument's interval at its last quote. It is never flagged; its age is
     * still reported.
     */
    @Test
    public void testAManualPriceIsNeverStale()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2026-07-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setFeed(QuoteFeed.MANUAL);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        var instrument = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        assertThat(instrument.get("priceStale").getAsBoolean(), is(false));
        assertThat(instrument.get("priceAgeDays").getAsLong(), is(19L));
        assertThat(instrument.get("priceMissedTradingDays").getAsLong(), is(12L));
        // and it says why the old price is not flagged
        assertThat(instrument.get("priceManual").getAsBoolean(), is(true));
    }

    /**
     * Valued at a future date, today's quote is as fresh as a quote can be -
     * the age is measured to today, not to the future date.
     */
    @Test
    public void testTodaysQuoteIsNotStaleAtAFutureDate()
    {
        var client = new Client();
        var today = LocalDate.now();

        var security = new SecurityBuilder() //
                        .addPrice(today.toString(), Values.Quote.factorize(110)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2026-01-15", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        var holdings = list(client, today.plusDays(66).toString(), null).getAsJsonObject();
        var instrument = holdings.get("items").getAsJsonArray().get(0).getAsJsonObject();

        // the handler reads today itself: past midnight the quote is a day old
        Assume.assumeTrue("the date changed during the test", LocalDate.now().equals(today));
        assertThat(instrument.get("priceStale").getAsBoolean(), is(false));
        assertThat(instrument.get("priceAgeDays").getAsLong(), is(0L));
        assertThat(instrument.get("priceMissedTradingDays").getAsLong(), is(0L));
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

    /**
     * A report whose currency pairs all have rates converts normally, and says
     * so by carrying an empty array - the array is always present, so a client
     * can tell "nothing fell back" apart from "this report did not look".
     */
    @Test
    public void testFxWarningsAlwaysPresentAndEmptyWhenNothingFallsBack()
    {
        var client = new Client();
        new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        assertThat(holdings.get("fxWarnings").getAsJsonArray().size(), is(0));
    }

    /**
     * A USD amount reported in EUR with no rates configured is shown at face
     * value - the 1:1 fallback the UI applies - and the report names that pair
     * rather than letting the number pass as converted. ARS is the example
     * because no shipped provider has a series for it; USD does, through the
     * built-in ECB defaults, so it would not exercise the fallback.
     */
    @Test
    public void testFxWarningsNameThePairWithoutRates()
    {
        var client = new Client();
        new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();

        var warnings = holdings.get("fxWarnings").getAsJsonArray();
        assertThat(warnings.size(), is(1));

        var warning = warnings.get(0).getAsJsonObject();
        assertThat(warning.get("currency").getAsString(), is("ARS"));
        assertThat(warning.get("reportingCurrency").getAsString(), is("EUR"));
        assertThat(warning.get("reason").getAsString(), is("no-series"));
    }

    /** a pair with rates converts normally, so it is not reported */
    @Test
    public void testFxWarningsEmptyWhenRatesExist()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        assertThat(holdings.get("fxWarnings").getAsJsonArray().size(), is(0));
    }

    /**
     * The other fallback shape: a series exists for the pair, but holds no
     * rates at all, so it answers no date. Named apart from the missing series
     * because the user's fix differs - a series with nothing in it is edited,
     * a missing one is created.
     */
    @Test
    public void testFxWarningsNameThePairWhoseSeriesHoldsNoRates()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(rates);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();

        var warnings = holdings.get("fxWarnings").getAsJsonArray();
        assertThat(warnings.size(), is(1));
        assertThat(warnings.get(0).getAsJsonObject().get("reason").getAsString(), is("no-rate"));
    }

    /**
     * The valuation date decides, not today: an exchange-rate security whose
     * quote on the valuation date is zero has no rate for that day, so the
     * statement converts at 1:1 and has to say so - even though the series holds
     * a valid rate today. On a day with a valid quote the same pair is silent.
     */
    @Test
    public void testFxWarningsFollowTheValuationDateNotToday()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 7, 15), 0));
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 7, 25), Values.Quote.factorize(970)));
        client.addSecurity(rates);

        var onAZeroQuote = list(client, "2026-07-20", null).getAsJsonObject().get("fxWarnings").getAsJsonArray();
        assertThat(onAZeroQuote.size(), is(1));
        assertThat(onAZeroQuote.get(0).getAsJsonObject().get("reason").getAsString(), is("no-rate"));

        var onAValidQuote = list(client, "2026-07-30", null).getAsJsonObject().get("fxWarnings").getAsJsonArray();
        assertThat(onAValidQuote.size(), is(0));
    }

    /**
     * Only money the statement of assets converted is named. An empty account and
     * a security that is no longer held contribute nothing to it, whatever their
     * currency, so a warning about them would send the reader looking for
     * amounts that are not in the response.
     */
    @Test
    public void testFxWarningsDoNotNameCurrenciesTheStatementNeverConverted()
    {
        var client = new Client();
        new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(200)).addTo(client);
        new AccountBuilder("ARS").addTo(client);
        new SecurityBuilder("BBD").addPrice("2026-01-01", Values.Quote.factorize(10)).addTo(client);

        var holdings = list(client, "2026-07-20", null).getAsJsonObject();
        assertThat(holdings.get("fxWarnings").getAsJsonArray().size(), is(0));
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

    // // paging: a working file holds more positions than one MCP answer carries

    /** three instruments in one depot and its settlement cash: four rows of the statement */
    private record Many(Client client, Account cash, Portfolio depot, List<Security> securities)
    {
        /** every row of the whole statement, in the order a paged walk goes through them */
        List<String> pagedUuids()
        {
            var uuids = securities.stream().map(Security::getUUID).collect(Collectors.toCollection(ArrayList::new));
            uuids.add(cash.getUUID());
            uuids.sort(Comparator.naturalOrder());
            return uuids;
        }
    }

    private static Many many()
    {
        var client = new Client();

        var cash = new AccountBuilder() //
                        .deposit_("2026-01-01", Values.Amount.factorize(10_000)) //
                        .addTo(client);
        cash.setName("Cash");

        var depot = new PortfolioBuilder(cash);
        var securities = new ArrayList<Security>();
        for (var ii = 0; ii < 3; ii++)
        {
            var security = new SecurityBuilder() //
                            .addPrice("2026-07-01", Values.Quote.factorize(100 + ii)) //
                            .addTo(client);
            security.setName("ACME " + ii);

            depot.buy(security, "2026-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000 + ii));
            securities.add(security);
        }

        var portfolio = depot.addTo(client);
        portfolio.setName("Depot");

        return new Many(client, cash, portfolio, securities);
    }

    /** unpaged, the statement is what it always was: the whole file in the application's order */
    @Test
    public void testWithoutPagingNothingIsAdded()
    {
        var m = many();
        var holdings = list(m.client(), "2026-07-20", null).getAsJsonObject();

        assertThat(holdings.has("page"), is(false));
        assertThat(holdings.get("items").getAsJsonArray().size(), is(4));
        for (var item : holdings.get("items").getAsJsonArray())
            assertThat(item.getAsJsonObject().has("cursor"), is(false));

        var names = new ArrayList<String>();
        holdings.get("items").getAsJsonArray()
                        .forEach(item -> names.add(item.getAsJsonObject().get("name").getAsString()));
        assertThat(names, contains("ACME 0", "ACME 1", "ACME 2", "Cash"));
    }

    /** one page: as many rows as were asked for, the count of all of them, and the cursor onward */
    @Test
    public void testAPageHoldsTheRowsAskedForAndNamesTheNext()
    {
        var m = many();
        var first = page(m.client(), "2026-07-20", null, null, null, null, "2");

        var page = first.get("page").getAsJsonObject();
        assertThat(page.get("limit").getAsInt(), is(2));
        assertThat(page.get("total").getAsInt(), is(4));
        assertThat(page.get("nextCursor").isJsonNull(), is(false));

        var items = first.get("items").getAsJsonArray();
        assertThat(items.size(), is(2));

        // paged, the rows are in the order of the paging key - the vehicle's uuid -
        // and the last one's cursor is the cursor the next page continues after
        var ordered = m.pagedUuids();
        assertThat(items.get(0).getAsJsonObject().get("uuid").getAsString(), is(ordered.get(0)));
        assertThat(items.get(1).getAsJsonObject().get("uuid").getAsString(), is(ordered.get(1)));
        assertThat(items.get(1).getAsJsonObject().get("cursor").getAsString(),
                        is(page.get("nextCursor").getAsString()));
    }

    /** the walk through every page holds each row exactly once, and all of them */
    @Test
    public void testAWalkThroughThePagesHoldsEveryRowOnce()
    {
        var m = many();

        var seen = new ArrayList<String>();
        var pages = 0;
        String cursor = null;
        boolean anotherPage = true;

        while (anotherPage)
        {
            var answer = page(m.client(), "2026-07-20", null, null, null, cursor, "2");
            assertThat(answer.get("page").getAsJsonObject().get("total").getAsInt(), is(4));

            answer.get("items").getAsJsonArray()
                            .forEach(item -> seen.add(item.getAsJsonObject().get("uuid").getAsString()));

            var next = answer.get("page").getAsJsonObject().get("nextCursor");
            anotherPage = !next.isJsonNull();
            cursor = anotherPage ? next.getAsString() : null;
            pages++;
        }

        assertThat(pages, is(2));
        assertThat(seen.size(), is(4));
        assertThat(seen, containsInAnyOrder(m.pagedUuids().toArray(new String[0])));
    }

    /**
     * A page narrows the rows and nothing else: the file's total stays the file's,
     * so the weights on a page are the shares of the whole statement and a reader
     * cannot mistake a part of the file for the sum of it.
     */
    @Test
    public void testAPageKeepsTheWholeFilesTotalAndWeights()
    {
        var m = many();
        var first = page(m.client(), "2026-07-20", null, null, null, null, "1");
        var whole = list(m.client(), "2026-07-20", null).getAsJsonObject();

        assertThat(first.get("totalAssets").getAsJsonObject().get("value").getAsDouble(),
                        is(whole.get("totalAssets").getAsJsonObject().get("value").getAsDouble()));

        var row = first.get("items").getAsJsonArray().get(0).getAsJsonObject();
        var same = findByUuid(whole, row.get("uuid").getAsString());
        assertThat(row.get("valuation").getAsJsonObject().get("value").getAsDouble(),
                        is(same.get("valuation").getAsJsonObject().get("value").getAsDouble()));
        assertThat(row.get("weight").getAsDouble(), closeTo(same.get("weight").getAsDouble(), 1e-10));
    }

    /** scoped, the page counts the rows of that one account, and says so in its total */
    @Test
    public void testAScopedReportPagesItsOwnRows()
    {
        var m = many();
        var answer = page(m.client(), "2026-07-20", null, m.depot().getUUID(), null, null, "2");

        assertThat(answer.get("page").getAsJsonObject().get("total").getAsInt(), is(3));
        assertThat(answer.get("scope").getAsJsonObject().get("uuid").getAsString(), is(m.depot().getUUID()));
        assertThat(answer.get("items").getAsJsonArray().size(), is(2));

        // still the depot's own value, not the file's: 100 + 101 + 102 for ten shares each
        assertThat(answer.get("totalAssets").getAsJsonObject().get("value").getAsDouble(), is(3030d));
    }

    /**
     * A cursor names the list it was written for. The same rows narrowed to one
     * account, the same file valued at another day, and a cursor of another list
     * altogether are refused rather than answered with a page of something else.
     */
    @Test
    public void testACursorOfAnotherListIsRefused()
    {
        var m = many();
        var cursor = page(m.client(), "2026-07-20", null, null, null, null, "2") //
                        .get("page").getAsJsonObject().get("nextCursor").getAsString();

        assertCursorRefused(m.client(), "2026-07-20", m.depot().getUUID(), null, cursor);
        assertCursorRefused(m.client(), "2026-07-21", null, null, cursor);
        assertCursorRefused(m.client(), "2026-07-20", null, null,
                        Paging.encode(Paging.Kind.INSTRUMENTS, Paging.scopeHash(""), m.cash().getUUID()));
    }

    private static void assertCursorRefused(Client client, String date, String investmentAccount, String cashAccount,
                    String cursor)
    {
        try
        {
            page(client, date, null, investmentAccount, cashAccount, cursor, "2");
            Assert.fail("expected ApiException for cursor " + cursor);
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("cursor"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    /** the reporting currency changes the figures of the same rows, so it keeps a cursor good */
    @Test
    public void testAnotherReportingCurrencyKeepsTheCursorGood()
    {
        var client = new Client();
        var cash = new AccountBuilder().deposit_("2026-01-01", Values.Amount.factorize(10_000)).addTo(client);
        cash.setName("Cash");
        new AccountBuilder("USD").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var eur = page(client, "2026-07-20", "EUR", null, null, null, "1");
        var againInUsd = page(client, "2026-07-20", "USD", null, null,
                        eur.get("page").getAsJsonObject().get("nextCursor").getAsString(), "1");

        assertThat(againInUsd.get("page").getAsJsonObject().get("total").getAsInt(), is(2));
    }

    @Test
    public void testLimitBelowOneIs400()
    {
        var m = many();

        try
        {
            page(m.client(), "2026-07-20", null, null, null, null, "0");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("limit"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
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
