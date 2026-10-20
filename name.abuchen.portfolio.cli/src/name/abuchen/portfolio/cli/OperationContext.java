package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;

/**
 * What one operation of a batch sees: the evolving model, its own input and
 * the places to report failures and changes.
 */
final class OperationContext
{
    /** One failed check of one operation. */
    record Failure(int operationIndex, String field, String code, String message, String check)
    {
        Map<String, Object> toJson()
        {
            var json = new LinkedHashMap<String, Object>();
            json.put("operationIndex", operationIndex);
            json.put("field", field);
            json.put("code", code);
            json.put("message", message);
            if (check != null)
                json.put("check", check);
            return json;
        }
    }

    /**
     * What an operation did to one entity. A created entity has only
     * <code>after</code>, an updated one the changed fields in both.
     */
    record Change(String entity, String uuid, Map<String, Object> before, Map<String, Object> after)
    {
        Map<String, Object> toJson()
        {
            var json = new LinkedHashMap<String, Object>();
            json.put("entity", entity);
            json.put("uuid", uuid);
            if (before != null)
                json.put("before", before);
            json.put("after", after);
            return json;
        }
    }

    private final ApplyBatch batch;
    private final int index;
    private final JsonObject input;
    private final List<Change> created = new ArrayList<>();
    private final List<Change> updated = new ArrayList<>();
    private int failureCount;

    OperationContext(ApplyBatch batch, int index, JsonObject input)
    {
        this.batch = batch;
        this.index = index;
        this.input = input;
    }

    /** Whether the JSON value is a string. */
    static boolean isText(JsonElement element)
    {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    Client client()
    {
        return batch.client();
    }

    JsonObject input()
    {
        return input;
    }

    void fail(String field, String code, String message)
    {
        fail(field, code, message, null);
    }

    /**
     * @param check
     *            the core check that raised the failure, or null
     */
    void fail(String field, String code, String message, String check)
    {
        failureCount++;
        batch.report(new Failure(index, field, code, message, check));
    }

    boolean failed()
    {
        return failureCount > 0;
    }

    /** Reports every member of the input that the operation does not know. */
    void rejectUnknownFields(Set<String> known)
    {
        for (var member : input.keySet())
        {
            if (!known.contains(member))
                fail(member, "unknown-field", "The operation has no field '" + member + "'.");
        }
    }

    /**
     * Claims an entity that existed before the batch for this operation; a
     * second operation on it is refused.
     *
     * @return false, after reporting the failure, if an earlier operation has
     *         claimed it
     */
    boolean claim(String field, String uuid)
    {
        var earlier = batch.claim(uuid, index);
        if (earlier == null)
            return true;

        fail(field, "duplicate-target", "Operation " + earlier + " already changes " + uuid
                        + "; one batch may change an entity only once.");
        return false;
    }

    /** Whether the entity was already in the file before the batch started. */
    boolean existedBeforeBatch(Object entity)
    {
        return batch.existedBeforeBatch(entity);
    }

    void created(Change change)
    {
        created.add(change);
    }

    void updated(Change change)
    {
        updated.add(change);
    }

    Map<String, Object> result(String operation)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("operationIndex", index);
        json.put("operation", operation);
        json.put("created", created.stream().map(Change::toJson).toList());
        json.put("updated", updated.stream().map(Change::toJson).toList());
        json.put("deleted", List.of()); // no operation deletes yet
        return json;
    }
}
