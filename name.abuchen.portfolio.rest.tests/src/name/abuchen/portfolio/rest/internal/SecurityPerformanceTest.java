package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityPerformanceTest
{
    private static JsonObject securities(Client client, String from, String to, String currency, String costMethod)
    {
        return SecurityPerformanceHandler
                        .list(client, new ExchangeRateProviderFactory(client), from, to, currency, costMethod)
                        .getAsJsonObject();
    }

    private static List<String> names(JsonObject result)
    {
        var names = new ArrayList<String>();
        var items = result.get("items").getAsJsonArray();
        for (var ii = 0; ii < items.size(); ii++)
            names.add(items.get(ii).getAsJsonObject().get("name").getAsString());
        return names;
    }

    private static JsonObject byName(JsonObject result, String name)
    {
        var items = result.get("items").getAsJsonArray();
        for (var ii = 0; ii < items.size(); ii++)
        {
            var item = items.get(ii).getAsJsonObject();
            if (name.equals(item.get("name").getAsString()))
                return item;
        }
        throw new AssertionError("no record for " + name);
    }

    private static double value(JsonObject item, String field)
    {
        return item.get(field).getAsJsonObject().get("value").getAsDouble();
    }

    @Test
    public void testReportsTheHeldPosition()
    {
        var result = securities(heldPosition(), "2024-01-01", "2024-12-31", null, null);

        assertThat(result.get("currency").getAsString(), is("EUR"));
        assertThat(names(result), contains("HELD"));

        var held = byName(result, "HELD");
        assertThat(held.get("heldAtClose").getAsBoolean(), is(true));
        assertThat(value(held, "unrealizedGains"), closeTo(200d, 1e-6));
        assertThat(value(held, "realizedGains"), is(0d));
        assertThat(held.get("delta").getAsJsonObject().get("value").getAsDouble(), closeTo(200d, 1e-6));
    }

    /**
     * The reason this endpoint exists rather than reusing the holdings snapshot: a
     * position closed inside the period is invisible at {@code closingDate} but is
     * still a contributor to the period's result.
     */
    @Test
    public void testIncludesAPositionSoldInsideThePeriod()
    {
        var result = securities(soldInsidePeriod(), "2024-01-01", "2024-12-31", null, null);

        assertThat(names(result), hasItem("SOLD"));

        var sold = byName(result, "SOLD");
        assertThat(sold.get("heldAtClose").getAsBoolean(), is(false));
        assertThat(value(sold, "realizedGains"), greaterThan(0d));
        assertThat(value(sold, "unrealizedGains"), is(0d));
    }

    /** a security with no activity in the interval is not a contributor to it */
    @Test
    public void testExcludesSecuritiesWithoutActivityInThePeriod()
    {
        var client = heldPosition();
        new SecurityBuilder().addPrice("2024-06-01", Values.Quote.factorize(50)).addTo(client).setName("UNTRADED");

        assertThat(names(securities(client, "2024-01-01", "2024-12-31", null, null)), not(hasItem("UNTRADED")));
    }

    /** fees and taxes are emitted as signed contributions, as elsewhere in the API */
    @Test
    public void testFeesAndTaxesAreNegative()
    {
        var client = heldPosition();
        var result = securities(client, "2024-01-01", "2024-12-31", null, null);
        var held = byName(result, "HELD");

        // no fees or taxes on this fixture, but the sign convention must hold at zero
        assertThat(value(held, "fees"), is(0d));
        assertThat(value(held, "taxes"), is(0d));
    }

    @Test
    public void testCostMethodIsAccepted()
    {
        var fifo = securities(soldInsidePeriod(), "2024-01-01", "2024-12-31", null, "fifo");
        var movingAverage = securities(soldInsidePeriod(), "2024-01-01", "2024-12-31", null, "moving-average");

        // delta is cost-method independent, so it must agree across both
        assertThat(value(byName(movingAverage, "SOLD"), "delta"),
                        closeTo(value(byName(fifo, "SOLD"), "delta"), 1e-6));
    }

    @Test
    public void testMissingOpeningDateIs400()
    {
        assertFieldError(() -> securities(heldPosition(), null, "2024-12-31", null, null), "openingDate", "required");
    }

    @Test
    public void testInvertedRangeIs400()
    {
        assertFieldError(() -> securities(heldPosition(), "2024-12-31", "2024-01-01", null, null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        assertFieldError(() -> securities(heldPosition(), "2024-01-01", "2024-12-31", "ZZZ", null), "currency",
                        "unknown-currency");
    }

    @Test
    public void testInvalidCostMethodIs400()
    {
        assertFieldError(() -> securities(heldPosition(), "2024-01-01", "2024-12-31", null, "lifo"), "costMethod",
                        "invalid-value");
    }

    /** 10 shares bought before the interval at 100, worth 120 at its end */
    private static Client heldPosition()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(120)) //
                        .addTo(client);
        security.setName("HELD");

        var account = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(5000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        return client;
    }

    /** the same file, plus a position bought and fully sold inside the interval */
    private static Client soldInsidePeriod()
    {
        var client = heldPosition();

        var security = new SecurityBuilder() //
                        .addPrice("2024-02-01", Values.Quote.factorize(50)) //
                        .addPrice("2024-08-01", Values.Quote.factorize(80)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("SOLD");

        var account = client.getAccounts().get(0);
        new PortfolioBuilder(account) //
                        .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .sell(security, "2024-08-01", Values.Share.factorize(10), Values.Amount.factorize(800)) //
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
