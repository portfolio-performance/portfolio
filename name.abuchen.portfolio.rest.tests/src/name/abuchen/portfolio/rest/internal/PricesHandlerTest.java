package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

/**
 * The price history of one instrument: its order, its window, its paging and
 * the two answers that must not be an error - the instrument with no prices, and
 * the window that holds none.
 */
@SuppressWarnings("nls")
public class PricesHandlerTest
{
    private static SecurityPrice price(String date, double value)
    {
        return new SecurityPrice(LocalDate.parse(date), Values.Quote.factorize(value));
    }

    /** a file with one instrument carrying three prices; the consumer gets that instrument, e.g. to keep its uuid */
    private static Client client(Consumer<Security> consumer)
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");
        security.addPrice(price("2026-01-02", 10d));
        security.addPrice(price("2026-01-05", 12.5d));
        security.addPrice(price("2026-01-09", 11d));
        consumer.accept(security);
        return client;
    }

    /** the series as stored, oldest first, with the span of the window stated */
    @Test
    public void testWholeSeriesOldestFirst()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        var json = PricesHandler.list(client, uuid[0], null, null, null, null);

        assertThat(json.get("instrument").getAsString(), is(uuid[0]));
        assertThat(json.get("instrumentName").getAsString(), is("ACME"));
        assertThat(json.get("currencyCode").getAsString(), is(client.getSecurities().get(0).getCurrencyCode()));
        assertThat(json.get("count").getAsInt(), is(3));
        assertThat(json.get("firstDate").getAsString(), is("2026-01-02"));
        assertThat(json.get("lastDate").getAsString(), is("2026-01-09"));

        var items = json.get("items").getAsJsonArray();
        assertThat(items.size(), is(3));
        assertThat(items.get(0).getAsJsonObject().get("date").getAsString(), is("2026-01-02"));
        assertThat(items.get(1).getAsJsonObject().get("value").getAsDouble(), is(12.5));
        assertThat(items.get(2).getAsJsonObject().get("date").getAsString(), is("2026-01-09"));

        // no paging asked for: no page, and nothing to continue with
        assertThat(json.has("page"), is(false));
        assertThat(items.get(0).getAsJsonObject().has("cursor"), is(false));
    }

    /** both ends of the window are inside it */
    @Test
    public void testWindowIsInclusive()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        var json = PricesHandler.list(client, uuid[0], "2026-01-05", "2026-01-09", null, null);

        assertThat(json.get("count").getAsInt(), is(2));
        assertThat(json.get("firstDate").getAsString(), is("2026-01-05"));
        assertThat(json.get("items").getAsJsonArray().size(), is(2));
    }

    /** a window holding nothing is an empty list, and says so without a span */
    @Test
    public void testWindowHoldingNothing()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        var json = PricesHandler.list(client, uuid[0], "2026-02-01", "2026-02-28", null, null);

        assertThat(json.get("count").getAsInt(), is(0));
        assertThat(json.get("items").getAsJsonArray().size(), is(0));
        assertThat(json.has("firstDate"), is(false));
        assertThat(json.has("lastDate"), is(false));
    }

    /** the pages of a series: disjoint, in order, every price once */
    @Test
    public void testPagingWalksTheSeries()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        var first = PricesHandler.list(client, uuid[0], null, null, null, "2");
        assertThat(first.get("count").getAsInt(), is(3));
        var page = first.get("page").getAsJsonObject();
        assertThat(page.get("limit").getAsInt(), is(2));
        assertThat(page.get("total").getAsInt(), is(3));
        var next = page.get("nextCursor").getAsString();
        assertThat(first.get("items").getAsJsonArray().size(), is(2));
        // every item of a page can be pointed at
        assertThat(first.get("items").getAsJsonArray().get(0).getAsJsonObject().has("cursor"), is(true));

        var second = PricesHandler.list(client, uuid[0], null, null, next, "2");
        var items = second.get("items").getAsJsonArray();
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("date").getAsString(), is("2026-01-09"));
        assertThat(second.get("page").getAsJsonObject().has("nextCursor"), is(false));
    }

    /** a cursor belongs to the window it was made for: it is refused, not obeyed */
    @Test
    public void testCursorOfAnotherWindowIsRefused()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        var cursor = PricesHandler.list(client, uuid[0], null, null, null, "1").get("page").getAsJsonObject()
                        .get("nextCursor").getAsString();

        try
        {
            PricesHandler.list(client, uuid[0], "2026-01-05", null, cursor, "1");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("cursor"));
        }
    }

    @Test
    public void testToBeforeFromIsRejected()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        try
        {
            PricesHandler.list(client, uuid[0], "2026-01-09", "2026-01-02", null, null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("to"));
            assertThat(e.getErrors().get(0).code(), is("invalid-range"));
        }
    }

    @Test
    public void testUndatedWindowIsRejected()
    {
        var uuid = new String[1];
        var client = client(security -> uuid[0] = security.getUUID());

        try
        {
            PricesHandler.list(client, uuid[0], "02.01.2026", null, null, null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("from"));
        }
    }

    /** an instrument that has never been quoted: an empty list, not an error */
    @Test
    public void testInstrumentWithoutPrices()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var json = PricesHandler.list(client, security.getUUID(), null, null, null, null);

        assertThat(json.get("count").getAsInt(), is(0));
        assertThat(json.get("items").getAsJsonArray().size(), is(0));
    }

    /** an uuid of no instrument is the 404 it is everywhere else on this API */
    @Test
    public void testUnknownInstrument()
    {
        var client = new Client();
        new SecurityBuilder().addTo(client);

        try
        {
            PricesHandler.list(client, "00000000-0000-0000-0000-000000000000", null, null, null, null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }
}
