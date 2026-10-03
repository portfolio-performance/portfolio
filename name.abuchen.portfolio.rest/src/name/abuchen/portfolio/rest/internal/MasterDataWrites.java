package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * What the master data write handlers (instruments, accounts, watchlists,
 * investment plans, taxonomies) share: the result of a write, the markers of
 * a dry run and of a replayed create, and the change list for the
 * application log.
 * <p/>
 * The handlers follow one pattern: validate the whole request first
 * (collecting every {@link ApiException.FieldError}), then build the target
 * state on a detached copy of the entity. The copy is the dry-run preview
 * and, compared with the entity, tells whether the write changes anything;
 * only then is the same change applied to the model and the file marked
 * dirty.
 */
public final class MasterDataWrites
{
    /**
     * The outcome of a create or update: the entity as serialized for the
     * response, and whether the model was changed. A dry run, a replayed
     * create (same {@code clientRef}) and an update that changes nothing
     * leave the model untouched.
     */
    public record WriteResult(JsonObject entity, boolean changed)
    {
    }

    private MasterDataWrites()
    {
    }

    /* package */ static WriteResult dryRun(JsonObject entity)
    {
        entity.addProperty("dryRun", true); //$NON-NLS-1$
        return new WriteResult(entity, false);
    }

    /* package */ static WriteResult replayed(JsonObject entity, boolean dryRun)
    {
        entity.addProperty("replayed", true); //$NON-NLS-1$
        if (dryRun)
            entity.addProperty("dryRun", true); //$NON-NLS-1$
        return new WriteResult(entity, false);
    }

    /** the answer of a dry-run delete: what would be removed */
    /* package */ static JsonObject deletePreview(JsonElement... removed)
    {
        var items = new JsonArray();
        for (var element : removed)
            items.add(element);

        var json = new JsonObject();
        json.addProperty("dryRun", true); //$NON-NLS-1$
        json.add("removed", items); //$NON-NLS-1$
        return json;
    }

    /**
     * The top-level fields that differ between two serializations of the
     * same entity, as change log entries; nested values are rendered as
     * their JSON text.
     */
    /* package */ static List<ChangeLog.Change> diff(JsonObject before, JsonObject after)
    {
        var keys = new LinkedHashSet<String>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());

        var changes = new ArrayList<ChangeLog.Change>();
        for (var key : keys)
        {
            var from = before.get(key);
            var to = after.get(key);
            if (!Objects.equals(from, to))
                changes.add(new ChangeLog.Change(key, render(from), render(to)));
        }
        return changes;
    }

    private static String render(JsonElement element)
    {
        if (element == null || element.isJsonNull())
            return null;
        if (element.isJsonPrimitive())
            return element.getAsString();
        return element.toString();
    }
}
