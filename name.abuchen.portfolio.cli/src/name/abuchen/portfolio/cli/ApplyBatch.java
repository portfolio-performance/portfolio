package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;

/**
 * Applies the operations of one <code>apply</code> batch in order to the
 * in-memory model. A failing operation changes nothing, later operations still
 * run so that every failure is reported at once, and the caller discards the
 * model if there was any failure. That makes the batch atomic.
 */
final class ApplyBatch
{
    static final String OPERATION_MEMBER = "operation";

    private final Client client;
    private final Set<Object> existedBeforeBatch = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String, Integer> claims = new HashMap<>();
    private final List<OperationContext.Failure> failures = new ArrayList<>();

    private ApplyBatch(Client client)
    {
        this.client = client;
        existedBeforeBatch.addAll(client.getSecurities());
    }

    /**
     * @return one result per operation, in order
     * @throws CliException
     *             <code>validation-failed</code> listing every failure
     */
    static List<Map<String, Object>> run(Client client, JsonArray operations, List<OperationSpec> specs)
                    throws CliException
    {
        var batch = new ApplyBatch(client);
        var results = new ArrayList<Map<String, Object>>();

        for (int index = 0; index < operations.size(); index++)
            results.add(batch.apply(index, operations.get(index), specs));

        if (!batch.failures.isEmpty())
            throw CliException.failure("validation-failed", "Validation failed",
                            batch.failures.size() + " check(s) failed. Nothing was written.", 422).with("failures",
                                            batch.failures.stream().map(OperationContext.Failure::toJson).toList());

        return results;
    }

    private Map<String, Object> apply(int index, JsonElement element, List<OperationSpec> specs)
    {
        if (!element.isJsonObject())
        {
            var context = new OperationContext(this, index, new JsonObject());
            context.fail("", "invalid-operation", "An operation must be a JSON object.");
            return context.result(null);
        }

        var input = element.getAsJsonObject();
        var context = new OperationContext(this, index, input);

        var name = operationName(input);
        var spec = specs.stream().filter(s -> s.name().equals(name)).findFirst();
        if (spec.isEmpty())
        {
            context.fail(OPERATION_MEMBER, "unknown-operation", name == null
                            ? "The operation needs an '" + OPERATION_MEMBER + "' member naming it."
                            : "Unknown operation '" + name + "'. Known operations: "
                                            + String.join(", ", specs.stream().map(OperationSpec::name).toList()));
            return context.result(String.valueOf(name));
        }

        spec.get().handler().apply(context);
        return context.result(name);
    }

    private static String operationName(JsonObject input)
    {
        var member = input.get(OPERATION_MEMBER);
        return member != null && member.isJsonPrimitive() && member.getAsJsonPrimitive().isString()
                        ? member.getAsString()
                        : null;
    }

    Client client()
    {
        return client;
    }

    void report(OperationContext.Failure failure)
    {
        failures.add(failure);
    }

    /** @return the index of the operation that claimed the entity first, or null */
    Integer claim(String uuid, int index)
    {
        return claims.putIfAbsent(uuid, index);
    }

    boolean existedBeforeBatch(Object entity)
    {
        return existedBeforeBatch.contains(entity);
    }
}
