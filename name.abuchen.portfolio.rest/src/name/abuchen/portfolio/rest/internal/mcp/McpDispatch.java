package name.abuchen.portfolio.rest.internal.mcp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;

/**
 * Turns a tool call into a call on the REST routing table - <em>into</em> the
 * same handlers the HTTP routes use, not beside them, since a second path
 * through the model is how two front doors start answering differently.
 * <p/>
 * Arguments become a path, a query map and a body per the tool's bindings. A
 * value carrying a {@code /} cannot resolve, which is correct: no file id,
 * alias ({@code [a-z0-9-]{1,32}}) or entity uuid contains one.
 */
public final class McpDispatch
{
    private McpDispatch()
    {
    }

    public static Response call(Router router, McpTool tool, JsonObject arguments) throws Exception
    {
        var path = tool.pathTemplate();
        var query = new LinkedHashMap<String, String>();
        var body = new JsonObject();

        // A tool whose route takes a body always sends one, even when the caller
        // named none of its fields: an empty merge patch is a documented no-op,
        // whereas an empty request body is refused with "request body must be a
        // JSON object" - a refusal to answer a call that asked for nothing.
        var takesBody = tool.bindings().stream().anyMatch(binding -> binding.where() == McpTool.Where.BODY);

        for (McpTool.Binding binding : tool.bindings())
        {
            // absent is not the same as null: on a merge patch an explicit null
            // clears a field, so a body binding is carried through whenever the
            // caller named it at all
            if (!arguments.has(binding.arg()))
                continue;

            var value = arguments.get(binding.arg());

            switch (binding.where())
            {
                case PATH:
                    // Every path argument is required, so McpEndpoint has already
                    // refused a null or a blank one. Substituting null would put
                    // the literal placeholder back into the template, which is a
                    // path another route owns.
                    if (!value.isJsonNull())
                        path = path.replace("{" + binding.wire() + "}", asText(value)); //$NON-NLS-1$ //$NON-NLS-2$
                    break;
                case QUERY:
                    var wireValue = asQueryValue(value);
                    if (wireValue != null)
                        query.put(binding.wire(), wireValue);
                    break;
                case BODY:
                    body.add(binding.wire(), value);
                    break;
                default:
                    break;
            }
        }

        return route(router, tool.method(), path, query,
                        takesBody ? body.toString().getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }

    /** the shared files, for disambiguating a 404: a call in this process, not an HTTP round trip */
    public static List<McpExplain.FileSummary> listFiles(Router router) throws Exception
    {
        var response = route(router, "GET", "/v1/files", Map.of(), new byte[0]); //$NON-NLS-1$ //$NON-NLS-2$
        var payload = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8));

        var files = new ArrayList<McpExplain.FileSummary>();
        var items = payload.getAsJsonObject().getAsJsonArray("items"); //$NON-NLS-1$
        for (JsonElement element : items)
        {
            var item = element.getAsJsonObject();
            var alias = item.get("alias"); //$NON-NLS-1$
            files.add(new McpExplain.FileSummary(item.get("id").getAsString(), item.get("label").getAsString(), //$NON-NLS-1$ //$NON-NLS-2$
                            alias == null ? null : alias.getAsString()));
        }
        return files;
    }

    private static Response route(Router router, String method, String path, Map<String, String> query, byte[] body)
                    throws Exception
    {
        var match = router.match(method, path);
        // the five-argument constructor is the authenticated one, and a /v1
        // route never consults the flag anyway. It does not carry *which*
        // client called: once tokens can be restricted, the caller
        // must be passed through here, or every tool call bypasses the check
        return match.handler().handle(new Request(method, path, match.pathParams(), query, body));
    }

    /** the value as the wire wants it, without JSON quoting around a string */
    private static String asText(JsonElement value)
    {
        return value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    /**
     * Comma-joined, which is how this API spells a multi-valued parameter
     * throughout. Repetition has no representation in a {@code Map<String,
     * String>} and none is declared; McpToolsDriftTest holds that true.
     * <p/>
     * An empty array is {@code null}, i.e. the parameter is not sent at all: an
     * empty list has no meaning for {@code metrics} or {@code status}, and
     * sending {@code metrics=} would be refused as a malformed value rather than
     * answered with the endpoint's own default.
     */
    private static String asQueryValue(JsonElement value)
    {
        if (value.isJsonNull())
            return null;

        if (!value.isJsonArray())
            return asText(value);

        var parts = new ArrayList<String>();
        for (JsonElement element : (JsonArray) value)
            parts.add(asText(element));

        return parts.isEmpty() ? null : String.join(",", parts); //$NON-NLS-1$
    }
}
