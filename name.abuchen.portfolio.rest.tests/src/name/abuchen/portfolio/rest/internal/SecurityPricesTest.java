package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityPricesTest
{
    private static JsonObject prices(Client client, String uuid, String from, String to)
    {
        return SecurityPricesHandler.list(client, uuid, from, to).getAsJsonObject();
    }

    private static JsonArray items(JsonObject result)
    {
        return result.get("items").getAsJsonArray();
    }

    private static Client clientWithPrices()
    {
        var client = new Client();
        client.setBaseCurrency("EUR");

        new SecurityBuilder("USD") //
                        .addPrice("2024-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2024-01-03", Values.Quote.factorize(110)) //
                        .addPrice("2024-01-04", Values.Quote.factorize(105.5)) //
                        .addTo(client);

        return client;
    }

    private static String uuid(Client client)
    {
        return client.getSecurities().get(0).getUUID();
    }

    @Test
    public void testTheWholeHistoryComesBackOldestFirst()
    {
        var client = clientWithPrices();
        var result = prices(client, uuid(client), null, null);
        var list = items(result);

        assertThat(list.size(), is(3));
        assertThat(list.get(0).getAsJsonObject().get("date").getAsString(), is("2024-01-02"));
        assertThat(list.get(2).getAsJsonObject().get("date").getAsString(), is("2024-01-04"));
        assertThat(list.get(2).getAsJsonObject().get("value").getAsDouble(), closeTo(105.5, 0.0001));
    }

    /**
     * The quotes are reported as recorded, in the instrument's own currency - not
     * converted into the file's base currency, which is EUR in this fixture.
     */
    @Test
    public void testTheSeriesIsInTheInstrumentsOwnCurrency()
    {
        var client = clientWithPrices();
        var result = prices(client, uuid(client), null, null);

        assertThat(result.get("currency").getAsString(), is("USD"));
        assertThat(result.get("uuid").getAsString(), is(uuid(client)));
    }

    @Test
    public void testBothBoundsAreInclusive()
    {
        var client = clientWithPrices();
        var list = items(prices(client, uuid(client), "2024-01-03", "2024-01-04"));

        assertThat(list.size(), is(2));
        assertThat(list.get(0).getAsJsonObject().get("date").getAsString(), is("2024-01-03"));
        assertThat(list.get(1).getAsJsonObject().get("date").getAsString(), is("2024-01-04"));
    }

    /**
     * The echoed range is the one the series covers, not the one that was asked
     * for: that is what lets a caller tell a quiet tail of the request apart from a
     * history that stops earlier.
     */
    @Test
    public void testTheEchoedRangeIsTheOneCovered()
    {
        var client = clientWithPrices();
        var result = prices(client, uuid(client), "2023-01-01", "2030-01-01");

        assertThat(result.get("from").getAsString(), is("2024-01-02"));
        assertThat(result.get("to").getAsString(), is("2024-01-04"));
    }

    @Test
    public void testARangeWithNoPricesIsEmptyRatherThanAnError()
    {
        var client = clientWithPrices();
        var result = prices(client, uuid(client), "2020-01-01", "2020-12-31");

        assertThat(items(result).size(), is(0));
        assertThat(result.get("from"), is(nullValue()));
        assertThat(result.get("to"), is(nullValue()));
    }

    /**
     * Unlike a reporting period this range is closed, so a single day is a valid
     * request rather than an empty one.
     */
    @Test
    public void testASingleDayIsAValidRange()
    {
        var client = clientWithPrices();
        var list = items(prices(client, uuid(client), "2024-01-03", "2024-01-03"));

        assertThat(list.size(), is(1));
        assertThat(list.get(0).getAsJsonObject().get("value").getAsDouble(), closeTo(110, 0.0001));
    }

    @Test
    public void testAnInstrumentWithoutPricesReportsNoRange()
    {
        var client = new Client();
        client.setBaseCurrency("EUR");
        new SecurityBuilder("USD").addTo(client);

        var result = prices(client, uuid(client), null, null);

        assertThat(items(result).size(), is(0));
        assertThat(result.get("from"), is(nullValue()));
        assertThat(result.get("currency").getAsString(), is("USD"));
    }

    @Test
    public void testAnUnknownInstrumentIsNotFound()
    {
        var client = clientWithPrices();

        try
        {
            prices(client, "no-such-uuid", null, null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testAMalformedDateIsRejected()
    {
        var client = clientWithPrices();
        assertFieldError(() -> prices(client, uuid(client), "01/02/2024", null), "from", "invalid-value");
        assertFieldError(() -> prices(client, uuid(client), null, "not-a-date"), "to", "invalid-value");
    }

    @Test
    public void testAnInvertedRangeIsRejected()
    {
        var client = clientWithPrices();
        assertFieldError(() -> prices(client, uuid(client), "2024-02-01", "2024-01-01"), "to", "invalid-range");
    }

    /** Both malformed dates are reported at once, not just the first. */
    @Test
    public void testEveryParameterProblemIsReported()
    {
        var client = clientWithPrices();

        try
        {
            prices(client, uuid(client), "yesterday", "tomorrow");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().size(), is(2));
            assertThat(e.getErrors().get(0).field(), is("from"));
            assertThat(e.getErrors().get(1).field(), is("to"));
        }
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
