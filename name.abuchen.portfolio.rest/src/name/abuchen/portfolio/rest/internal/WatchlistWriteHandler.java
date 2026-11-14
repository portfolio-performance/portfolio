package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.rest.WriteApprovals;

/**
 * The two changes a watchlist takes: a list that does not exist yet, and
 * instruments into or out of a list that does.
 *
 * <p>
 * A watchlist is the one object of a file without an identifier: its name is the
 * address, and the only one. That answers two questions here rather than leaving
 * them to the caller. Two lists of the same name are refused, not created - once
 * they exist nothing this API returns could tell them apart. And the name travels
 * in the body rather than in the path, because a watchlist name is free text,
 * free text can hold a slash, and a path segment cannot be asked to survive one.
 * </p>
 */
public final class WatchlistWriteHandler
{
    private WatchlistWriteHandler()
    {
    }

    /**
     * What the prompt will be about, checked before anybody is asked: everything
     * the change itself will read is settled here, so that a user is never offered
     * a change that could not have been made.
     */
    public static WriteApprovals.Facts describe(Client client, String fileLabel, JsonObject body, boolean creation)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var name = name(body, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var existing = lookup(client, name);

        if (creation && existing != null)
            throw nameTaken(existing);

        if (!creation && existing == null)
            throw noSuchList(client, name);

        // in the same order the change reads them, and refused the same way: this
        // was checked live and found wanting - the name was settled here while the
        // instruments were not, so a request carrying an uuid the file does not
        // have asked the user first and only failed afterwards, on a change that
        // could never have been made
        if (creation)
            resolveAll("instruments", uuids(body, "instruments", errors), client, errors); //$NON-NLS-1$ //$NON-NLS-2$
        else
            updateArguments(body, client, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // the operation is the key "allow for this session" is remembered under,
        // and the route stamps it in: a describer names the thing, not the right
        return new WriteApprovals.Facts(null, fileLabel, "watchlist", name, null, null); //$NON-NLS-1$
    }

    /**
     * A new list, optionally holding instruments. Created empty is not a failure:
     * an empty list is what the application calls an empty watchlist, and filling
     * it is the other tool's job.
     */
    public static JsonObject create(Client client, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var name = name(body, errors);
        var wanted = uuids(body, "instruments", errors); //$NON-NLS-1$
        var instruments = resolveAll("instruments", wanted, client, errors); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        // checked here as well as in describe(): the prompt and the change are two
        // calls, and a duplicate name would outlive whichever one forgot to look
        var existing = lookup(client, name);
        if (existing != null)
            throw nameTaken(existing);

        var watchlist = new Watchlist();
        watchlist.setName(name);

        var before = client.getWatchlists().size();
        client.addWatchlist(watchlist);
        for (var security : instruments)
            watchlist.addSecurity(security);
        client.markDirty();

        var json = answer(watchlist, instruments.size(), 0, true);
        json.addProperty("watchlistsBefore", before); //$NON-NLS-1$
        json.addProperty("watchlistsAfter", client.getWatchlists().size()); //$NON-NLS-1$
        return json;
    }

    /**
     * Instruments into and out of one existing list, both directions in one
     * request so that a caller moving a name from one list to another needs no
     * second round - and so that the tool count does not grow per direction.
     *
     * <p>
     * Removing what the list does not hold is not an error and says so in the
     * answer: the state the caller asked for already holds, which is what makes
     * the request safe to send twice.
     * </p>
     */
    public static JsonObject update(Client client, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var name = name(body, errors);
        var watchlist = lookup(client, name);
        if (name != null && watchlist == null)
            throw noSuchList(client, name);

        var toChange = updateArguments(body, client, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var before = held(watchlist).size();
        var notHeld = new JsonArray();
        var changed = false;
        var put = 0;
        var taken = 0;

        for (var security : toChange.added())
        {
            if (positionOf(watchlist, security) < 0)
            {
                watchlist.addSecurity(security);
                put++;
                changed = true;
            }
        }

        for (var security : toChange.removed())
        {
            var position = positionOf(watchlist, security);
            if (position >= 0)
            {
                // by place rather than by equality: the list is the model's own, and
                // a uuid is what tells two entries apart here
                watchlist.getSecurities().remove(position);
                taken++;
                changed = true;
            }
            else
                notHeld.add(InvestmentPlansHandler.namedReference(security.getUUID(), security.getName()));
        }

        // a restatement changed nothing, so the file is not left unsaved over a
        // change the API did not actually make
        if (changed)
            client.markDirty();

        var json = answer(watchlist, put, taken, false);
        json.addProperty("instrumentCountBefore", before); //$NON-NLS-1$
        json.addProperty("instrumentCountAfter", held(watchlist).size()); //$NON-NLS-1$
        json.addProperty("changed", changed); //$NON-NLS-1$
        if (notHeld.size() > 0)
            json.add("notHeld", notHeld); //$NON-NLS-1$
        return json;
    }

    // ------------------------------------------------------------------ answer

    /** what the list is called, how many it now holds, and that it has no identifier */
    private static JsonObject answer(Watchlist watchlist, int added, int removed, boolean created)
    {
        var json = new JsonObject();
        json.addProperty("name", watchlist.getName()); //$NON-NLS-1$
        json.addProperty("created", created); //$NON-NLS-1$
        // a reader that expects a uuid somewhere will look for one in every answer;
        // saying it plainly here is cheaper than letting it search
        json.addProperty("hasIdentifier", false); //$NON-NLS-1$
        json.addProperty("instrumentCount", held(watchlist).size()); //$NON-NLS-1$
        json.addProperty("added", added); //$NON-NLS-1$
        json.addProperty("removed", removed); //$NON-NLS-1$
        return json;
    }

    // -------------------------------------------------------------- validations

    /** the name of the list this request is about, trimmed and required */
    private static String name(JsonObject body, List<ApiException.FieldError> errors)
    {
        var element = body == null ? null : body.get("name"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
        {
            errors.add(new ApiException.FieldError("name", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "name is required - a watchlist has no identifier but its name")); //$NON-NLS-1$
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            errors.add(new ApiException.FieldError("name", "invalid-type", "name must be a string")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        var trimmed = element.getAsString().trim();
        if (trimmed.isEmpty())
        {
            errors.add(new ApiException.FieldError("name", "required", "name must not be blank")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        return trimmed;
    }

    /** a list of names the caller did not choose twice */
    private static ApiException nameTaken(Watchlist existing)
    {
        var detail = "a watchlist named \"" + existing.getName() + "\" already exists" //$NON-NLS-1$ //$NON-NLS-2$
                        + " - names are the only way to address a list, so they cannot repeat"; //$NON-NLS-1$
        var errors = List.of(new ApiException.FieldError("name", "invalid-value", detail)); //$NON-NLS-1$ //$NON-NLS-2$
        return ApiException.conflict("name-taken", "Watchlist name already in use", detail, errors); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** the list the caller named, with the names it could have meant */
    private static ApiException noSuchList(Client client, String name)
    {
        var known = new StringBuilder();
        for (var watchlist : client.getWatchlists())
        {
            if (known.length() > 0)
                known.append(", "); //$NON-NLS-1$
            known.append(watchlist.getName());
        }

        var detail = "this file has no watchlist named \"" + name + "\"" //$NON-NLS-1$ //$NON-NLS-2$
                        + (known.length() == 0 ? " - it has no watchlists at all" //$NON-NLS-1$
                                        : " - it has: " + known); //$NON-NLS-1$
        var errors = List.of(new ApiException.FieldError("name", "invalid-value", detail)); //$NON-NLS-1$ //$NON-NLS-2$
        return ApiException.conflict("no-such-watchlist", "No watchlist of this file has that name", detail, errors); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static Watchlist lookup(Client client, String name)
    {
        if (name == null)
            return null;

        for (var watchlist : client.getWatchlists())
            if (name.equals(watchlist.getName()))
                return watchlist;

        return null;
    }

    // ---------------------------------------------------------------- arguments

    /** what an update asked for, once resolved; both callers read it the same way */
    private record Changes(List<Security> added, List<Security> removed)
    {
    }

    /**
     * The two directions of an update, read and resolved in one place so that the
     * prompt and the change cannot disagree about what a request asked for.
     */
    private static Changes updateArguments(JsonObject body, Client client, List<ApiException.FieldError> errors)
    {
        var toAdd = uuids(body, "add", errors); //$NON-NLS-1$
        var toRemove = uuids(body, "remove", errors); //$NON-NLS-1$
        var added = resolveAll("add", toAdd, client, errors); //$NON-NLS-1$
        var removed = resolveAll("remove", toRemove, client, errors); //$NON-NLS-1$

        if (toAdd == null && toRemove == null)
            errors.add(new ApiException.FieldError("add", "required", "give at least one of add or remove")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        return new Changes(added, removed);
    }

    /**
     * The uuids of one argument, as given. A missing argument and an empty one are
     * told apart, because "add nothing" and "I did not speak of adding" are not the
     * same request.
     */
    private static List<String> uuids(JsonObject body, String field, List<ApiException.FieldError> errors)
    {
        var element = body == null ? null : body.get(field);
        if (element == null || element.isJsonNull())
            return null;
        if (!element.isJsonArray())
        {
            errors.add(new ApiException.FieldError(field, "invalid-type", field + " must be an array of uuids")); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        var uuids = new ArrayList<String>();
        var seen = new LinkedHashSet<String>();
        for (var entry : element.getAsJsonArray())
        {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString())
            {
                errors.add(new ApiException.FieldError(field, "invalid-type", field + " takes uuids as strings")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var trimmed = entry.getAsString().trim();
            if (trimmed.isEmpty())
            {
                errors.add(new ApiException.FieldError(field, "invalid-value", "a uuid must not be blank")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }
            if (seen.add(trimmed))
                uuids.add(trimmed);
        }

        return uuids;
    }

    /**
     * Every instrument the caller named, or nobody of them: an instrument that is
     * not in the file is refused rather than skipped, because a caller that meant
     * to add two names and silently got one would never hear about it.
     */
    private static List<Security> resolveAll(String field, List<String> uuids, Client client,
                    List<ApiException.FieldError> errors)
    {
        var resolved = new ArrayList<Security>();
        if (uuids == null)
            return resolved;

        for (var uuid : uuids)
        {
            Security found = null;
            for (var security : client.getSecurities())
            {
                if (security != null && uuid.equals(security.getUUID()))
                {
                    found = security;
                    break;
                }
            }

            if (found == null)
            {
                var detail = "no instrument of this file has uuid " + uuid + " - see pp_list_instruments"; //$NON-NLS-1$ //$NON-NLS-2$
                errors.add(new ApiException.FieldError(field, "invalid-value", detail)); //$NON-NLS-1$
            }
            else
                resolved.add(found);
        }

        return resolved;
    }

    // ------------------------------------------------------------------- model

    /** the instruments of the list, with the holes a half-edited file leaves gone */
    private static List<Security> held(Watchlist watchlist)
    {
        var securities = new ArrayList<Security>();
        for (var security : watchlist.getSecurities())
            if (security != null)
                securities.add(security);

        return securities;
    }

    /** where this instrument sits in the list, or -1 when the list does not hold it */
    private static int positionOf(Watchlist watchlist, Security security)
    {
        var securities = watchlist.getSecurities();
        for (var i = 0; i < securities.size(); i++)
        {
            var held = securities.get(i);
            if (held != null && held.getUUID().equals(security.getUUID()))
                return i;
        }

        return -1;
    }
}
