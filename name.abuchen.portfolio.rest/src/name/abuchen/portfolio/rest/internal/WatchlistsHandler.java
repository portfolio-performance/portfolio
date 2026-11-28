package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 * <p/>
 * A file holds more of them than one MCP answer carries once the contents are
 * asked for, so the lists are paged like every other list: one list is one item
 * of a page, contents and all, and a page never ends in the middle of one.
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
     * @param cursorParam
     *            where the page continues after, as {@code page.nextCursor} gave it
     * @param limitParam
     *            how many lists the page holds at most
     */
    public static JsonObject list(Client client, String fileId, String instrumentParam,
                    String withInstrumentsParam, String cursorParam, String limitParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var instrument = InvestmentPlansHandler.resolve("instrument", "instrument", //$NON-NLS-1$ //$NON-NLS-2$
                        "pp_list_instruments", instrumentParam, client.getSecurities(), //$NON-NLS-1$
                        Security::getUUID, errors);
        var withInstruments = flag("withInstruments", withInstrumentsParam, errors); //$NON-NLS-1$

        // the file and what it is narrowed to decide which lists the answer holds
        var scope = "watchlists|file=" + (fileId == null ? "" : fileId) //$NON-NLS-1$ //$NON-NLS-2$
                        + "|instrument=" + (instrument == null ? "" : instrument.getUUID()); //$NON-NLS-1$ //$NON-NLS-2$
        var paging = Paging.parse(Paging.Kind.WATCHLISTS, scope, cursorParam, limitParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var listed = new ArrayList<Entry>();
        var rank = 0;
        for (var watchlist : client.getWatchlists())
        {
            if (instrument == null || holds(watchlist, instrument))
                listed.add(new Entry(rank, watchlist));
            rank++;
        }

        var json = new JsonObject();
        paging.apply(Paging.Kind.WATCHLISTS, json, "items", listed, Entry::key, //$NON-NLS-1$
                        entry -> watchlistJson(entry.watchlist(), withInstruments));
        if (listed.isEmpty())
            json.addProperty("empty", instrument != null ? "no watchlist of this file holds this instrument" //$NON-NLS-1$
                            : "this file has no watchlists"); //$NON-NLS-1$

        return json;
    }

    /**
     * A list of the file and its place in it, which is what makes its key unique:
     * the name alone is not, because a hand-edited file can hold two lists of one
     * name and this API has no second way of telling them apart.
     */
    private record Entry(int rank, Watchlist watchlist)
    {
        String key()
        {
            return String.format(Locale.ROOT, "%03d|%s", Integer.valueOf(rank), //$NON-NLS-1$
                            watchlist.getName());
        }
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
