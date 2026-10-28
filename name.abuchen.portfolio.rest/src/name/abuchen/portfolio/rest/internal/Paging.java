package name.abuchen.portfolio.rest.internal;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * One page of a list: {@code offset} items skipped, at most {@code limit}
 * returned. Without either parameter the whole list is returned as before, and
 * the response carries no {@code page} - a client that never pages sees no
 * change.
 * <p/>
 * The pages are cut from one ordered list - the file's order of instruments,
 * the trades sorted by instrument name - and a list is recomputed for every
 * call, so a page is stable only while the file is not changed in between.
 * {@code nextOffset} says where the next page starts, and is {@code null} on
 * the last one.
 *
 * @param given
 *            whether the request named {@code offset} or {@code limit} at all
 * @param limit
 *            null for no limit
 */
public record Paging(boolean given, int offset, Integer limit)
{
    /** the whole list, as when neither parameter is sent */
    public static final Paging ALL = new Paging(false, 0, null);

    /**
     * The paging a request asked for. A parameter that does not parse is
     * reported in {@code errors} and treated as absent, like every other query
     * parameter of this API.
     */
    public static Paging parse(String offsetParam, String limitParam, List<ApiException.FieldError> errors)
    {
        if (offsetParam == null && limitParam == null)
            return ALL;

        var offset = offsetParam == null ? Integer.valueOf(0) : whole("offset", offsetParam, 0, errors); //$NON-NLS-1$
        var limit = limitParam == null ? null : whole("limit", limitParam, 1, errors); //$NON-NLS-1$

        return new Paging(true, offset == null ? 0 : offset, limit);
    }

    private static Integer whole(String field, String value, int minimum, List<ApiException.FieldError> errors)
    {
        try
        {
            var parsed = Integer.parseInt(value.strip());
            if (parsed >= minimum)
                return parsed;
        }
        catch (NumberFormatException e)
        {
            // reported below, with the bound
        }

        errors.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        field + " must be a whole number of at least " + minimum)); //$NON-NLS-1$
        return null;
    }

    /**
     * Puts the page of {@code items} into the response, with a {@code page}
     * that says where it lies in the whole - or all of them, and no
     * {@code page}, when no paging was asked for.
     */
    public void apply(JsonObject response, JsonArray items)
    {
        if (!given)
        {
            response.add("items", items); //$NON-NLS-1$
            return;
        }

        var total = items.size();
        var from = Math.min(offset, total);
        var to = limit == null ? total : (int) Math.min((long) from + limit, total);

        var page = new JsonArray();
        for (var ii = from; ii < to; ii++)
            page.add(items.get(ii));

        response.add("page", describe(offset, limit, total, to < total ? Integer.valueOf(to) : null)); //$NON-NLS-1$
        response.add("items", page); //$NON-NLS-1$
    }

    /**
     * The {@code page} of a response: where it starts, how many it was asked to
     * hold, how many there are in all and where the next one starts.
     */
    public static JsonObject describe(int offset, Integer limit, int total, Integer nextOffset)
    {
        var page = new JsonObject();
        page.addProperty("offset", offset); //$NON-NLS-1$
        if (limit != null)
            page.addProperty("limit", limit); //$NON-NLS-1$
        else
            page.add("limit", JsonNull.INSTANCE); //$NON-NLS-1$
        page.addProperty("total", total); //$NON-NLS-1$
        if (nextOffset != null)
            page.addProperty("nextOffset", nextOffset); //$NON-NLS-1$
        else
            page.add("nextOffset", JsonNull.INSTANCE); //$NON-NLS-1$
        return page;
    }
}
