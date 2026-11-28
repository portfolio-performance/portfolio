package name.abuchen.portfolio.rest.internal.mcp;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

/**
 * One tool as {@code mcp-tools.json} declares it, in two deliberate halves.
 * Name, description, schema and annotations are the {@code tools/list} payload:
 * served as-is and stable across MCP front doors. Method, path template and
 * bindings are routing, and only this endpoint uses them to dispatch into
 * {@code ApiRoutes}.
 */
public record McpTool(String name, String operationId, String method, String pathTemplate, String description,
                JsonObject inputSchema, JsonObject annotations, List<Binding> bindings)
{
    /** where one argument goes when the tool call becomes an HTTP request */
    public enum Where
    {
        PATH, QUERY, BODY,
        /**
         * one query parameter per key of an object argument, behind a prefix:
         * {@code {"<attribute id>": …}} becomes {@code attribute:<id>=…}. The
         * route permits that family as {@code attribute:*}, because the ids
         * belong to the file and not to the catalog.
         */
        QUERY_PREFIX
    }

    /**
     * @param arg
     *            the argument name as the model sees it
     * @param wire
     *            the path placeholder, query parameter or body property - or, for
     *            a {@link Where#QUERY_PREFIX}, the prefix every generated
     *            parameter name starts with
     */
    public record Binding(String arg, Where where, String wire)
    {
    }

    /**
     * Annotations are advisory and untrusted by specification, so nothing
     * depends on a client acting on one. This is read only to decide whether a
     * result carries the "not saved to disk" note, which is our own behaviour.
     */
    public boolean isReadOnly()
    {
        var flag = annotations.get("readOnlyHint"); //$NON-NLS-1$
        return flag != null && flag.getAsBoolean();
    }

    /** the arguments {@code inputSchema} marks required, in declaration order */
    public Set<String> requiredArguments()
    {
        var required = new LinkedHashSet<String>();
        var declared = inputSchema.getAsJsonArray("required"); //$NON-NLS-1$
        if (declared != null)
            declared.forEach(element -> required.add(element.getAsString()));
        return required;
    }

    /** every argument {@code inputSchema} declares, in declaration order */
    public Set<String> declaredArguments()
    {
        var properties = inputSchema.getAsJsonObject("properties"); //$NON-NLS-1$
        return properties == null ? Set.of() : new LinkedHashSet<>(properties.keySet());
    }

    public Binding binding(String argument)
    {
        return bindings.stream().filter(binding -> binding.arg().equals(argument)).findFirst().orElse(null);
    }
}
