package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;

/**
 * The watchlists of a file: named baskets of instruments that the application
 * quotes together. A watchlist is not an account and holds nothing - it is a
 * selection of names - and unlike everything else in the file it has no
 * identifier of its own, so its name is all a reader can address it by and no
 * uuid anywhere in the file points at one.
 */
public final class WatchlistsHandler
{
    private WatchlistsHandler()
    {
    }

    /**
     * @param instrumentParam
     *            the uuid of one instrument, to read only the lists holding it -
     *            the way to ask where a name is kept without pulling every list
     *            of the file along with it
     * @param withInstrumentsParam
     *            whether each list also carries the instruments it holds; by
     *            default it only says how many it has, because a working list
     *            carries several hundred of them
     */
    public static JsonObject list(Client client, String instrumentParam, String withInstrumentsParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var instrument = InvestmentPlansHandler.resolve("instrument", "instrument", //$NON-NLS-1$ //$NON-NLS-2$
                        "pp_list_instruments", instrumentParam, client.getSecurities(), //$NON-NLS-1$
                        Security::getUUID, errors);
        var withInstruments = flag("withInstruments", withInstrumentsParam, errors); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var items = new JsonArray();
        for (var watchlist : client.getWatchlists())
        {
            if (instrument != null && !holds(watchlist, instrument))
                continue;

            items.add(watchlistJson(watchlist, withInstruments));
        }

        return EntityJson.envelope(items,
                        instrument != null ? "no watchlist of this file holds this instrument" //$NON-NLS-1$
                                        : "this file has no watchlists"); //$NON-NLS-1$
    }

    private static boolean holds(Watchlist watchlist, Security instrument)
    {
        for (var held : held(watchlist))
            if (held.getUUID().equals(instrument.getUUID()))
                return true;

        return false;
    }

    /** the instruments of the list, with the holes a half-edited file can leave behind gone */
    private static List<Security> held(Watchlist watchlist)
    {
        var securities = new ArrayList<Security>();
        for (var security : watchlist.getSecurities())
            if (security != null)
                securities.add(security);

        return securities;
    }

    private static JsonObject watchlistJson(Watchlist watchlist, boolean withInstruments)
    {
        var json = new JsonObject();
        json.addProperty("name", watchlist.getName()); //$NON-NLS-1$

        var securities = held(watchlist);
        json.addProperty("instrumentCount", securities.size()); //$NON-NLS-1$

        if (withInstruments)
        {
            var instruments = new JsonArray();
            for (var security : securities)
                instruments.add(InvestmentPlansHandler.namedReference(security.getUUID(), security.getName()));
            json.add("instruments", instruments); //$NON-NLS-1$
        }

        return json;
    }

    /** as the application spells a boolean out, so a wrong answer is refused instead of guessed */
    private static boolean flag(String field, String raw, List<ApiException.FieldError> errors)
    {
        if (raw == null || raw.isBlank())
            return false;

        var trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("true")) //$NON-NLS-1$
            return true;
        if (trimmed.equalsIgnoreCase("false")) //$NON-NLS-1$
            return false;

        errors.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        field + " takes true or false")); //$NON-NLS-1$
        return false;
    }
}
