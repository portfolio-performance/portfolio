package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

/**
 * The stored price history of one instrument - the quotes the application
 * values its positions from, day by day, as the user sees them in the price
 * chart. Everything else on this API speaks of a quote only in passing: a
 * holding states the one price its valuation rests on, an instrument states its
 * master data, and a performance figure states its result. This is the endpoint
 * that answers what a series actually looked like.
 * <p/>
 * The series is always oldest first and its last entry is the newest quote, so
 * that a chart can be drawn from the items in the order they arrive. There is
 * deliberately no {@code order} parameter: reversed pages would have to run
 * backwards through the cursor, which is a forward-only device here, and a
 * caller after the recent end of a long series gets it cheaper by asking for a
 * window than by asking for the tail of everything.
 * <p/>
 * The list includes the instrument's latest quote even when that quote was
 * fetched by a feed and never stored as history: the application values a
 * position at it, and a series that stopped one day before the valuation the API
 * reports elsewhere would leave a caller to explain the difference themselves.
 */
public final class PricesHandler
{
    private PricesHandler()
    {
    }

    /**
     * The prices of one instrument, optionally bounded by {@code from} and
     * {@code to} (both inclusive), paged by cursor and limit like every other
     * list on this API. The cursor is the date of the last item rendered, so the
     * scope it is tied to names the instrument and the window it was produced
     * for: a cursor from one window is refused in another rather than quietly
     * skipping prices.
     *
     * @throws ApiException
     *             400 when a date does not parse or {@code to} precedes
     *             {@code from}, 404 when no instrument of the file has that uuid
     */
    public static JsonObject list(Client client, String uuid, String fromParam, String toParam, String cursorParam,
                    String limitParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var from = fromParam == null ? null : CalcParams.date("from", fromParam, errors); //$NON-NLS-1$
        var to = toParam == null ? null : CalcParams.date("to", toParam, errors); //$NON-NLS-1$

        if (from != null && to != null && to.isBefore(from))
            errors.add(new ApiException.FieldError("to", "invalid-range", "to must not be before from")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var security = find(client, uuid);

        var inWindow = window(security.getPricesIncludingLatest(), from, to);

        var json = new JsonObject();
        json.addProperty("instrument", security.getUUID()); //$NON-NLS-1$
        json.addProperty("instrumentName", security.getName()); //$NON-NLS-1$

        // a currency-less series, an index for one, has no currency to name; the
        // values it carries are plain numbers
        var currency = security.getCurrencyCode();
        if (currency != null)
            json.addProperty("currencyCode", currency); //$NON-NLS-1$

        // the coverage of the list on its own, so that a caller knows the span of
        // the series before it walks the pages of it
        json.addProperty("count", inWindow.size()); //$NON-NLS-1$
        if (!inWindow.isEmpty())
        {
            json.addProperty("firstDate", inWindow.get(0).getDate().toString()); //$NON-NLS-1$
            json.addProperty("lastDate", inWindow.get(inWindow.size() - 1).getDate().toString()); //$NON-NLS-1$
        }

        var paging = Paging.parse(Paging.Kind.PRICES, scope(security.getUUID(), from, to), cursorParam, limitParam,
                        errors);
        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        paging.apply(Paging.Kind.PRICES, json, "items", inWindow, PricesHandler::key, //$NON-NLS-1$
                        PricesHandler::toJson);

        return json;
    }

    private static Security find(Client client, String uuid)
    {
        for (var candidate : client.getSecurities())
        {
            if (candidate.getUUID().equals(uuid))
                return candidate;
        }
        throw ApiException.notFound();
    }

    /**
     * The prices of the window, in the order the model keeps them. An entry
     * without a date cannot be placed on a timeline, cannot be paged by one and
     * cannot be drawn: it is left out rather than reported with a null date.
     */
    private static List<SecurityPrice> window(List<SecurityPrice> prices, LocalDate from, LocalDate to)
    {
        if (from == null && to == null)
            return prices.stream().filter(price -> price.getDate() != null).toList();

        var bounded = new ArrayList<SecurityPrice>(prices.size());
        for (var price : prices)
        {
            var date = price.getDate();
            if (date == null)
                continue;
            if (from != null && date.isBefore(from))
                continue;
            if (to != null && date.isAfter(to))
                continue;
            bounded.add(price);
        }
        return bounded;
    }

    /** the cursor key: an ISO date, unique because the model holds at most one price per day */
    private static String key(SecurityPrice price)
    {
        return price.getDate().toString();
    }

    private static JsonObject toJson(SecurityPrice price)
    {
        var json = new JsonObject();
        json.addProperty("date", price.getDate().toString()); //$NON-NLS-1$
        json.add("value", EntityJson.decimal(price.getValue(), Values.Quote.precision())); //$NON-NLS-1$
        return json;
    }

    /** what the page belongs to: this instrument, in this window */
    private static String scope(String uuid, LocalDate from, LocalDate to)
    {
        var window = (from == null ? "" : from) + "|" + (to == null ? "" : to); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return "prices|" + uuid + "|" + window; //$NON-NLS-1$
    }
}
