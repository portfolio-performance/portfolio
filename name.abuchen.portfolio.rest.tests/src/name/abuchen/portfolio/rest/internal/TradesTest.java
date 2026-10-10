package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradesTest
{
    private static JsonObject trades(Client client, String currency, String onlyClosed)
    {
        return TradesHandler.list(client, new ExchangeRateProviderFactory(client), currency, onlyClosed)
                        .getAsJsonObject();
    }

    private static JsonArray items(JsonObject result)
    {
        return result.get("items").getAsJsonArray();
    }

    private static JsonObject byName(JsonObject result, String name)
    {
        var list = items(result);
        for (var ii = 0; ii < list.size(); ii++)
        {
            var item = list.get(ii).getAsJsonObject();
            if (name.equals(item.get("security").getAsJsonObject().get("name").getAsString()))
                return item;
        }
        throw new AssertionError("no trade for " + name);
    }

    @Test
    public void testPairsAClosedRoundTrip()
    {
        var result = trades(closedRoundTrip(), null, null);

        assertThat(result.get("currency").getAsString(), is("EUR"));

        var trade = byName(result, "ROUNDTRIP");
        assertThat(trade.get("closed").getAsBoolean(), is(true));
        assertThat(trade.get("start").getAsString(), is("2024-02-01"));
        assertThat(trade.get("end").getAsString(), is("2024-08-01"));
        assertThat(trade.get("shares").getAsDouble(), closeTo(10d, 1e-6));
        assertThat(trade.get("entryValue").getAsJsonObject().get("value").getAsDouble(), closeTo(500d, 1e-6));
        assertThat(trade.get("exitValue").getAsJsonObject().get("value").getAsDouble(), closeTo(800d, 1e-6));
        assertThat(trade.get("profitLoss").getAsJsonObject().get("value").getAsDouble(), closeTo(300d, 1e-6));
        assertThat(trade.get("holdingPeriodDays").getAsLong(), greaterThan(0L));
        assertThat(trade.get("return").getAsDouble(), closeTo(0.6, 1e-6));
    }

    /** an open position is still a trade, just one without an end */
    @Test
    public void testAnOpenTradeHasNoEnd()
    {
        var trade = byName(trades(openPosition(), null, null), "OPEN");

        assertThat(trade.get("closed").getAsBoolean(), is(false));
        assertThat(trade.has("end"), is(false));
    }

    @Test
    public void testOnlyClosedFiltersOpenTrades()
    {
        var client = openPosition();
        // the same file also holds a completed round trip
        var security = new SecurityBuilder() //
                        .addPrice("2024-02-01", Values.Quote.factorize(50)) //
                        .addPrice("2024-08-01", Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("ROUNDTRIP");
        new PortfolioBuilder(client.getAccounts().get(0)) //
                        .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .sell(security, "2024-08-01", Values.Share.factorize(10), Values.Amount.factorize(800)) //
                        .addTo(client);

        assertThat(items(trades(client, null, null)).size(), is(2));

        var closedOnly = items(trades(client, null, "true"));
        assertThat(closedOnly.size(), is(1));
        assertThat(closedOnly.get(0).getAsJsonObject().get("closed").getAsBoolean(), is(true));

        assertThat(items(trades(client, null, "false")).size(), is(2));
    }

    @Test
    public void testCurrencyOverrideIsReported()
    {
        assertThat(trades(closedRoundTrip(), "USD", null).get("currency").getAsString(), is("USD"));
    }

    /** a file with no portfolio activity has no trades, and says so with an empty list */
    @Test
    public void testNoActivityIsAnEmptyList()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(1000)).addTo(client);

        var result = trades(client, null, null);
        assertThat(items(result).size(), is(0));
        // nothing was skipped, so the caller can trust the emptiness
        assertThat(result.has("skippedInstruments"), is(false));
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        assertFieldError(() -> trades(closedRoundTrip(), "ZZZ", null), "currency", "unknown-currency");
    }

    @Test
    public void testInvalidOnlyClosedIs400()
    {
        assertFieldError(() -> trades(closedRoundTrip(), null, "yes"), "onlyClosed", "invalid-value");
    }

    /** bought at 50 and sold at 80, entirely inside 2024 */
    private static Client closedRoundTrip()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2024-02-01", Values.Quote.factorize(50)) //
                        .addPrice("2024-08-01", Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("ROUNDTRIP");

        var account = new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(5000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .sell(security, "2024-08-01", Values.Share.factorize(10), Values.Amount.factorize(800)) //
                        .addTo(client);

        return client;
    }

    private static Client openPosition()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2024-02-01", Values.Quote.factorize(50)) //
                        .addTo(client);
        security.setName("OPEN");

        var account = new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(5000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .addTo(client);

        return client;
    }

    private static void assertFieldError(Runnable call, String field, String code)
    {
        try
        {
            call.run();
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is(field));
            assertThat(e.getErrors().get(0).code(), is(code));
        }
    }
}
