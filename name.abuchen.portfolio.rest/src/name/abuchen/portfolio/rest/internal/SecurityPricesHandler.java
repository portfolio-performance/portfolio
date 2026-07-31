package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityPrice;

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
