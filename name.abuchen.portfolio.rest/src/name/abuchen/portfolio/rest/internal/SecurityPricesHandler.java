package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.Messages;

public final class SecurityPricesHandler
{
    private SecurityPricesHandler()
    {
    }

    /**
     * The historical price series stored in the file for one instrument, in the
     * instrument's own currency - the same series the desktop's price chart draws.
     * <p>
     * Nothing is computed or converted here: these are the quotes as recorded, so a
     * consumer plotting them next to the desktop sees the same line. The latest
     * intraday quote is merged in when the history has no entry for its date (see
     * {@link name.abuchen.portfolio.model.Security#getPricesIncludingLatest()}),
     * which is what keeps the last point of the chart current between quote updates.
     * <p>
     * {@code from} and {@code to} are optional and inclusive; the response echoes
     * the range it actually covers, which for an absent bound is the first (or last)
     * price on record rather than the requested one.
     */
    public static JsonElement list(Client client, String uuid, String fromParam, String toParam)
    {
        var security = SecuritiesHandler.find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        var from = parseDate("from", fromParam, errors);
        var to = parseDate("to", toParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // Unlike a reporting period this range is closed, so a single-day request is
        // meaningful and only from *after* to is contradictory.
        if (from != null && to != null && from.isAfter(to))
            throw ApiException.badRequest(List.of(new ApiException.FieldError("to", "invalid-range",
                            "to must not be before from")));

        var selected = new ArrayList<SecurityPrice>();
        for (SecurityPrice price : security.getPricesIncludingLatest())
        {
            if (from != null && price.getDate().isBefore(from))
                continue;
            if (to != null && price.getDate().isAfter(to))
                continue;
            selected.add(price);
        }

        return EntityJson.securityPrices(security, selected);
    }

    /**
     * Adds or replaces historical prices: {@code {"items": [{"date":
     * "2026-01-02", "value": "12.34"}]}}. A price on a date that already has
     * one replaces it, as a manual edit in the application does. Answers how
     * many prices were inserted, updated or left unchanged; a request that
     * changes nothing does not mark the file dirty. Must be called on the UI
     * thread.
     */
    public static JsonObject upsert(WriteContext context, String uuid, JsonObject body)
    {
        var client = context.client();
        var security = SecuritiesHandler.find(client, uuid);

        var prices = parsePrices(body);

        var existing = new HashMap<LocalDate, Long>();
        for (var price : security.getPrices())
            existing.put(price.getDate(), price.getValue());

        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        for (var price : prices)
        {
            var before = existing.get(price.getDate());
            if (before == null)
                inserted++;
            else if (before.longValue() != price.getValue())
                updated++;
            else
                unchanged++;
        }

        var json = new JsonObject();
        json.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
        if (security.getCurrencyCode() != null)
            json.addProperty("currency", security.getCurrencyCode()); //$NON-NLS-1$
        json.addProperty("inserted", inserted); //$NON-NLS-1$
        json.addProperty("updated", updated); //$NON-NLS-1$
        json.addProperty("unchanged", unchanged); //$NON-NLS-1$

        if (context.dryRun())
        {
            json.addProperty("dryRun", true); //$NON-NLS-1$
            return json;
        }

        if (inserted + updated == 0)
            return json;

        for (var price : prices)
            security.addPrice(price, true);
        client.markDirty();

        var dates = prices.stream().map(SecurityPrice::getDate).sorted().toList();
        ChangeLog.recordChanges(List.of(new ChangeLog.Change("prices", null, //$NON-NLS-1$
                        MessageFormat.format("{0} inserted, {1} updated ({2} - {3})", inserted, updated, //$NON-NLS-1$
                                        dates.get(0), dates.get(dates.size() - 1)))),
                        Messages.MsgApiInstrumentChanged, security.getName(), context.file().getLabel());

        return json;
    }

    private static List<SecurityPrice> parsePrices(JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        for (var key : body.keySet())
        {
            if (!"items".equals(key)) //$NON-NLS-1$
                errors.add(new ApiException.FieldError(key, "unknown-field", key + " is not a known field")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        var items = body.get("items"); //$NON-NLS-1$
        if (items == null || items.isJsonNull())
        {
            errors.add(new ApiException.FieldError("items", "required", "items is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            throw ApiException.validation(errors);
        }
        if (!items.isJsonArray())
        {
            errors.add(new ApiException.FieldError("items", "invalid-type", "items must be an array")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            throw ApiException.validation(errors);
        }

        var prices = new ArrayList<SecurityPrice>();
        var dates = new HashSet<LocalDate>();

        var array = items.getAsJsonArray();
        for (int ii = 0; ii < array.size(); ii++)
        {
            var prefix = "items[" + ii + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            var element = array.get(ii);
            if (!element.isJsonObject())
            {
                errors.add(new ApiException.FieldError(prefix, "invalid-type", "a price must be an object")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var json = new Json(element.getAsJsonObject());
            var date = json.requireDate("date"); //$NON-NLS-1$
            var value = json.requireDecimal("value", Values.Quote.precision()); //$NON-NLS-1$
            json.rejectUnknownFields();

            if (value != null && value.signum() <= 0)
                json.add(new ApiException.FieldError("value", "must-be-positive", "value must be positive")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (date != null && !dates.add(date))
                json.add(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("{0} is given more than once", date))); //$NON-NLS-1$

            for (var error : json.errors())
                errors.add(new ApiException.FieldError(prefix + "." + error.field(), error.code(), error.message())); //$NON-NLS-1$

            if (!json.hasErrors())
                prices.add(new SecurityPrice(date, Amounts.toQuote(value)));
        }

        if (!errors.isEmpty())
            throw ApiException.validation(errors);

        return prices;
    }

    /**
     * Removes the historical prices between {@code from} and {@code to}
     * (inclusive; an absent bound is open, both absent remove all prices).
     * The latest quote is not touched. Answers the number of prices removed.
     * Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String uuid, String fromParam, String toParam)
    {
        var client = context.client();
        var security = SecuritiesHandler.find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        var from = parseDate("from", fromParam, errors); //$NON-NLS-1$
        var to = parseDate("to", toParam, errors); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        if (from != null && to != null && from.isAfter(to))
            throw ApiException.badRequest(List.of(new ApiException.FieldError("to", "invalid-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "to must not be before from"))); //$NON-NLS-1$

        var selected = security.getPrices().stream() //
                        .filter(p -> from == null || !p.getDate().isBefore(from)) //
                        .filter(p -> to == null || !p.getDate().isAfter(to)) //
                        .toList();

        var json = new JsonObject();
        json.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
        json.addProperty("removed", selected.size()); //$NON-NLS-1$

        if (context.dryRun())
        {
            json.addProperty("dryRun", true); //$NON-NLS-1$
            return json;
        }

        if (selected.isEmpty())
            return json;

        selected.forEach(security::removePrice);
        client.markDirty();

        ChangeLog.recordChanges(List.of(new ChangeLog.Change("prices", //$NON-NLS-1$
                        MessageFormat.format("{0} ({1} - {2})", selected.size(), selected.get(0).getDate(), //$NON-NLS-1$
                                        selected.get(selected.size() - 1).getDate()),
                        null)), Messages.MsgApiInstrumentChanged, security.getName(), context.file().getLabel());

        return json;
    }

    private static LocalDate parseDate(String field, String value, List<ApiException.FieldError> errors)
    {
        if (value == null)
            return null;

        try
        {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException e)
        {
            errors.add(new ApiException.FieldError(field, "invalid-value",
                            field + " must be an ISO 8601 date (YYYY-MM-DD)"));
            return null;
        }
    }
}
