package name.abuchen.portfolio.cli.apply;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.model.Client;

/**
 * What one operation of a batch sees: the evolving model, its own input and
 * the places to report failures and changes.
 */
public final class OperationContext
{
    /** One failed check of one operation. */
    record Failure(int operationIndex, FieldFailure failure)
    {
        Map<String, Object> toJson()
        {
            var json = new LinkedHashMap<String, Object>();
            json.put("operationIndex", operationIndex);
            json.put("field", failure.field());
            json.put("code", failure.code());
            json.put("message", failure.message());
            if (failure.check() != null)
                json.put("check", failure.check());
            return json;
        }
    }

    /**
     * What an operation did to one entity. A created entity has only
     * <code>after</code>, an updated one the changed fields in both, and a
     * deleted one its whole <code>before</code> and an empty <code>after</code>.
     */
    record Change(Outcome.Entity entity, String uuid, Map<String, Object> before, Map<String, Object> after)
    {
        Map<String, Object> toJson()
        {
            var json = new LinkedHashMap<String, Object>();
            json.put("entity", entity.id());
            json.put("uuid", uuid);
            if (before != null)
                json.put("before", before);
            json.put("after", after);
            return json;
        }
    }

    private final ApplyBatch batch;
    private final int index;
    private final OperationInput input;
    private final List<Change> created;
    private final List<Change> updated;
    private final List<Change> deleted;
    private final Optional<TransactionUpdates.Existing> replacing;
    private int failureCount;

    OperationContext(ApplyBatch batch, int index, JsonObject input)
    {
        this.batch = batch;
        this.index = index;
        this.input = new OperationInput(input, batch.client(), batch::existedBeforeBatch, this::fail);
        this.created = new ArrayList<>();
        this.updated = new ArrayList<>();
        this.deleted = new ArrayList<>();
        this.replacing = Optional.empty();
    }

    private OperationContext(OperationContext outer, JsonObject input, TransactionUpdates.Existing replacing)
    {
        this.batch = outer.batch;
        this.index = outer.index;
        this.input = new OperationInput(input, batch.client(), batch::existedBeforeBatch, this::fail);
        this.created = outer.created;
        this.updated = outer.updated;
        this.deleted = outer.deleted;
        this.replacing = Optional.of(replacing);
    }

    /**
     * A context of the same operation whose input is the given one and whose
     * add of a transaction replaces the existing record instead. Changes go to
     * this operation's result; failures are counted separately.
     */
    OperationContext replacing(JsonObject merged, TransactionUpdates.Existing existing)
    {
        return new OperationContext(this, merged, existing);
    }

    /** The record that the transaction being built replaces, if this is an update. */
    Optional<TransactionUpdates.Existing> replacing()
    {
        return replacing;
    }

    Client client()
    {
        return batch.client();
    }

    OperationInput input()
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
        fail(new FieldFailure(field, code, message, check));
    }

    private void fail(FieldFailure failure)
    {
        failureCount++;
        batch.report(new Failure(index, failure));
    }

    boolean failed()
    {
        return failureCount > 0;
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
        var earlier = claims().claim(uuid);
        if (earlier.isEmpty())
            return true;

        fail(field, "duplicate-target", "Operation " + earlier.getAsInt() + " already changes " + uuid
                        + "; one batch may change an entity only once.");
        return false;
    }

    /** Claims entities for this operation. */
    Claims claims()
    {
        return uuid -> batch.claim(uuid, index);
    }

    /** Whether the entity was already in the file before the batch started. */
    boolean existedBeforeBatch(Object entity)
    {
        return batch.existedBeforeBatch(entity);
    }

    /** Reports the outcome of this operation as its change or its failures. */
    void record(Outcome outcome)
    {
        switch (outcome)
        {
            case Outcome.Applied applied when applied.before() == null -> created(
                            new Change(applied.kind(), applied.recordId(), null, applied.after()));
            case Outcome.Applied applied when applied.after().isEmpty() -> deleted(
                            new Change(applied.kind(), applied.recordId(), applied.before(), applied.after()));
            case Outcome.Applied applied -> updated(
                            new Change(applied.kind(), applied.recordId(), applied.before(), applied.after()));
            case Outcome.Unchanged unchanged -> {
                // nothing to report
            }
            case Outcome.Refused refused -> refused.failures().forEach(this::fail);
        }
    }

    void created(Change change)
    {
        created.add(change);
    }

    void updated(Change change)
    {
        updated.add(change);
    }

    void deleted(Change change)
    {
        deleted.add(change);
    }

    Map<String, Object> result(String operation)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("operationIndex", index);
        json.put("operation", operation);
        json.put("created", created.stream().map(Change::toJson).toList());
        json.put("updated", updated.stream().map(Change::toJson).toList());
        json.put("deleted", deleted.stream().map(Change::toJson).toList());
        return json;
    }
}
