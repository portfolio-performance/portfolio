package name.abuchen.portfolio.rest.internal;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

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
 * A paged list is in an order of keys that an edit elsewhere does not change:
 * an instrument's uuid; a trade's instrument uuid, start, investment account,
 * closing transaction and the shares and amount of its opening transaction.
 * Every item of a page carries its {@code cursor}, and
 * {@code page.nextCursor} is the cursor of the page's last item - or
 * {@code null} on the last page. The next page holds the items after that key.
 * An item added or removed between two pages therefore does not shift the
 * others, as it would with an offset: every item that is in the list for the
 * whole walk through it comes exactly once. An item added before the cursor
 * comes only with a new walk from the first page; a trade whose start,
 * investment account or closing transaction changes gets a new key and may
 * then come twice or not at all. Lots that agree in all of these are told apart
 * only by an ordinal of their order: when one of them is removed between two
 * pages, the next one may be left out of the walk.
 * <p/>
 * A cursor is good only for the list it came from: it carries a short hash of
 * the scope - the file, for trades of one instrument that instrument, and the
 * filters that decide which items the list holds - and a cursor of another
 * scope is refused as {@code invalid-value} rather than answered with a page
 * of a different list.
 *
 * @param given
 *            whether the request named {@code cursor} or {@code limit} at all
 * @param scope
 *            the hash of the list's scope, which every cursor of it carries
 * @param after
 *            the key the page continues after, or null for the first page
 * @param limit
 *            null for no limit
 */
public record Paging(boolean given, String scope, String after, Integer limit)
{
    /** the whole list, as when neither parameter is sent */
    public static final Paging ALL = new Paging(false, null, null, null);

    /**
     * Which list a cursor belongs to, so that one list's cursor is refused by
     * another - and what a key of it looks like, so that a cursor this API did
     * not write is refused before it is compared with any key.
     */
    public enum Kind
    {
        /** an instrument's uuid */
        INSTRUMENTS("i", Pattern.compile("[^\\p{Cntrl}|]{1,200}")), //$NON-NLS-1$ //$NON-NLS-2$
        /**
         * instrument | start | investment account | closing transaction |
         * shares | currency and amount of the opening transaction | ordinal
         */
        TRADES("t", Pattern.compile("[^\\p{Cntrl}|]{1,200}\\|\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\|" //$NON-NLS-1$ //$NON-NLS-2$
                        + "[^\\p{Cntrl}|]{1,200}\\|[^\\p{Cntrl}|]{0,200}\\|-?\\d{1,19}\\|[A-Z]{3} -?\\d{1,19}\\|\\d{4,}")); //$NON-NLS-1$

        private final String tag;
        private final Pattern key;

        Kind(String tag, Pattern key)
        {
            this.tag = tag;
            this.key = key;
        }
    }


    private static final String VERSION = "v1"; //$NON-NLS-1$

    /**
     * The paging a request asked for. An empty {@code cursor} asks for the
     * first page. A parameter that does not parse - or a cursor of another
     * list - is reported in {@code errors} and treated as absent, like every
     * other query parameter of this API.
     *
     * @param scope
     *            what decides which items the list holds: the file, the
     *            instrument of a sub-collection and the filters, in a fixed
     *            form; only its hash goes into the cursor
     */
    public static Paging parse(Kind kind, String scope, String cursorParam, String limitParam,
                    List<ApiException.FieldError> errors)
    {
        if (cursorParam == null && limitParam == null)
            return ALL;

        var hash = scopeHash(scope);
        var after = cursorParam == null || cursorParam.isBlank() ? null
                        : decode(kind, hash, cursorParam.strip(), errors);
        var limit = limitParam == null ? null : whole("limit", limitParam, 1, errors); //$NON-NLS-1$

        return new Paging(true, hash, after, limit);
    }

    /** a short, fixed-length hash of the scope: enough to tell lists apart, not to keep anything secret */
    /* package */ static String scopeHash(String scope)
    {
        try
        {
            var digest = MessageDigest.getInstance("SHA-256") //$NON-NLS-1$
                            .digest((scope == null ? "" : scope).getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 8));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
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
     * The cursor of the item with this key in the list of this scope hash:
     * opaque to the client, and only good for that list.
     */
    public static String encode(Kind kind, String scopeHash, String key)
    {
        var plain = VERSION + ':' + kind.tag + ':' + scopeHash + ':' + key;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(Kind kind, String scopeHash, String cursor, List<ApiException.FieldError> errors)
    {
        try
        {
            // strict: bytes that are no UTF-8 are a cursor this API did not write
            var plain = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(Base64.getUrlDecoder().decode(cursor))).toString();
            var prefix = VERSION + ':' + kind.tag + ':' + scopeHash + ':';
            if (plain.startsWith(prefix))
            {
                var key = plain.substring(prefix.length());
                if (kind.key.matcher(key).matches())
                    return key;
            }
        }
        catch (IllegalArgumentException | CharacterCodingException e)
        {
            // reported below
        }

        errors.add(new ApiException.FieldError("cursor", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "cursor must be a page.nextCursor or an item's cursor from this same list, " //$NON-NLS-1$
                                        + "with the same file, instrument and filters")); //$NON-NLS-1$
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
                json.getAsJsonObject().addProperty("cursor", encode(kind, scope, key.apply(item))); //$NON-NLS-1$
            rendered.add(json);
        }

        var next = end < ordered.size() && end > start ? encode(kind, scope, key.apply(ordered.get(end - 1))) : null;
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
