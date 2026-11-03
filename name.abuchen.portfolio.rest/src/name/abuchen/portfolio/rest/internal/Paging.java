package name.abuchen.portfolio.rest.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * One page of a list, continued from a cursor rather than counted from an
 * offset. Without {@code cursor} and {@code limit} the whole list is returned
 * as before, in its usual order, and the response carries no {@code page} - a
 * client that never pages sees no change.
 * <p/>
 * A paged list is in an order of keys that never change: an instrument's
 * uuid; a trade's instrument uuid, start and the ids of the transactions that
 * open and close it. Every item of a page carries its {@code cursor}, and
 * {@code page.nextCursor} is the cursor of the page's last item - or
 * {@code null} on the last page. The next page holds the items after that key.
 * An item added or removed between two pages therefore shifts nothing: no
 * item of the list is skipped or sent twice, as with an offset.
 *
 * @param given
 *            whether the request named {@code cursor} or {@code limit} at all
 * @param after
 *            the key the page continues after, or null for the first page
 * @param limit
 *            null for no limit
 */
public record Paging(boolean given, String after, Integer limit)
{
    /** the whole list, as when neither parameter is sent */
    public static final Paging ALL = new Paging(false, null, null);

    /** which list a cursor belongs to, so that one list's cursor is refused by another */
    public enum Kind
    {
        INSTRUMENTS("i"), //$NON-NLS-1$
        TRADES("t"); //$NON-NLS-1$

        private final String tag;

        Kind(String tag)
        {
            this.tag = tag;
        }
    }

    private static final String VERSION = "v1"; //$NON-NLS-1$

    /**
     * The paging a request asked for. An empty {@code cursor} asks for the
     * first page. A parameter that does not parse is reported in
     * {@code errors} and treated as absent, like every other query parameter
     * of this API.
     */
    public static Paging parse(Kind kind, String cursorParam, String limitParam,
                    List<ApiException.FieldError> errors)
    {
        if (cursorParam == null && limitParam == null)
            return ALL;

        var after = cursorParam == null || cursorParam.isBlank() ? null : decode(kind, cursorParam.strip(), errors);
        var limit = limitParam == null ? null : whole("limit", limitParam, 1, errors); //$NON-NLS-1$

        return new Paging(true, after, limit);
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

    /** the cursor of the item with this key: opaque to the client, and only good for this list */
    public static String encode(Kind kind, String key)
    {
        var plain = VERSION + ':' + kind.tag + ':' + key;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(Kind kind, String cursor, List<ApiException.FieldError> errors)
    {
        try
        {
            var plain = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            var prefix = VERSION + ':' + kind.tag + ':';
            if (plain.startsWith(prefix) && plain.length() > prefix.length())
                return plain.substring(prefix.length());
        }
        catch (IllegalArgumentException e)
        {
            // reported below
        }

        errors.add(new ApiException.FieldError("cursor", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "cursor must be a page.nextCursor or an item's cursor from this same list")); //$NON-NLS-1$
        return null;
    }

    /**
     * Puts the page of {@code items} into the response: the items whose key
     * follows the cursor, in key order, at most {@code limit} of them, each
     * with its {@code cursor}, and a {@code page} that says where the next one
     * starts - or all of them, in the order given and with no {@code page},
     * when no paging was asked for. Only the items of the page are rendered.
     */
    public <T> void apply(Kind kind, JsonObject response, List<T> items, Function<T, String> key,
                    Function<T, JsonElement> render)
    {
        var rendered = new JsonArray();
        if (!given)
        {
            items.forEach(item -> rendered.add(render.apply(item)));
            response.add("items", rendered); //$NON-NLS-1$
            return;
        }

        var ordered = new ArrayList<T>(items);
        ordered.sort(Comparator.comparing(key));

        var start = 0;
        if (after != null)
            while (start < ordered.size() && key.apply(ordered.get(start)).compareTo(after) <= 0)
                start++;

        var end = limit == null ? ordered.size() : (int) Math.min((long) start + limit, ordered.size());

        for (var ii = start; ii < end; ii++)
        {
            var item = ordered.get(ii);
            var json = render.apply(item);
            if (json.isJsonObject())
                json.getAsJsonObject().addProperty("cursor", encode(kind, key.apply(item))); //$NON-NLS-1$
            rendered.add(json);
        }

        var next = end < ordered.size() && end > start ? encode(kind, key.apply(ordered.get(end - 1))) : null;
        response.add("page", describe(limit, items.size(), next)); //$NON-NLS-1$
        response.add("items", rendered); //$NON-NLS-1$
    }

    /**
     * The {@code page} of a response: how many items it was asked to hold, how
     * many there are in all and the cursor the next one continues after.
     */
    public static JsonObject describe(Integer limit, int total, String nextCursor)
    {
        var page = new JsonObject();
        if (limit != null)
            page.addProperty("limit", limit); //$NON-NLS-1$
        else
            page.add("limit", JsonNull.INSTANCE); //$NON-NLS-1$
        page.addProperty("total", total); //$NON-NLS-1$
        if (nextCursor != null)
            page.addProperty("nextCursor", nextCursor); //$NON-NLS-1$
        else
            page.add("nextCursor", JsonNull.INSTANCE); //$NON-NLS-1$
        return page;
    }
}
