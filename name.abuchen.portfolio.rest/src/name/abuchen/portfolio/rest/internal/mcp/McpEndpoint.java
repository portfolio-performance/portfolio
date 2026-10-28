package name.abuchen.portfolio.rest.internal.mcp;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.RejectedConnections;
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.internal.VersionHandler;

/**
 * The Model Context Protocol front door: one route, {@code POST /mcp}, serving
 * the sixteen tools of {@code mcp-tools.json} over Streamable HTTP.
 * <p/>
 * <b>No session model.</b> No {@code Mcp-Session-Id} is issued, responses are
 * plain {@code application/json} whatever {@code Accept} asks for, and
 * {@code GET} and {@code DELETE} fall through to the router's 405 - an SSE
 * stream is a parked request, which is why ADR 0002 already refused
 * long-polling. Everything a session could hold travels per request instead. If
 * a client ever demands a session id, emit a constant one and ignore it coming
 * back.
 * <p/>
 * <b>Authorisation is per JSON-RPC method</b> (ADR 0005): {@code initialize},
 * {@code tools/list} and {@code ping} answer without a token, because all they
 * disclose is a tool list {@code /v1/openapi.yaml} already serves to anyone.
 * {@code tools/call} needs one and refuses with {@code isError} at HTTP 200,
 * since a 401 is invisible in the client UI. It is therefore also the only
 * point at which a missing token is a refusal, and the only one that files a
 * {@link RejectedConnections} record.
 */
@SuppressWarnings("nls")
public final class McpEndpoint
{
    /**
     * The cap on what one tool call may return. Sits under Claude Desktop's
     * measured 150,000 truncation point, is roughly 25,000 tokens, and clears a
     * personal portfolio on every collection. Revise on measured evidence.
     */
    public static final int MAX_RESULT_CHARS = 100_000;

    /**
     * How many JSON-RPC messages one HTTP request may carry. Protocol
     * 2025-06-18 dropped batching; accepting a batch is leniency, and an
     * unbounded one is how a single request queues an arbitrary number of tool
     * calls against the server.
     */
    private static final int MAX_BATCH_MESSAGES = 32;

    private static final String NEWEST_SUPPORTED_PROTOCOL_VERSION = "2025-06-18";

    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS = Set.of("2025-06-18", "2025-03-26", "2024-11-05");

    private static final String SERVER_NAME = "Portfolio Performance";

    // JSON-RPC 2.0 error codes
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;

    private McpEndpoint()
    {
    }

    /**
     * Registers the endpoint on the routing table it dispatches into - going
     * through {@link Router} is what answers GET and DELETE with a 405 for free.
     */
    public static void register(Router router)
    {
        router.add("POST", RestApiConstants.MCP_ENDPOINT, request -> handle(router, request));
    }

    static Response handle(Router router, Request request)
    {
        JsonElement message;
        try
        {
            // Gson keeps the last of repeated keys silently, and the Gson in the
            // target platform has no way to ask for the other behaviour, so the
            // message is read here name by name. A message that names one key
            // twice is a fault in whatever built it, and the caller has to hear it.
            var reader = new JsonReader(new StringReader(new String(request.body(), StandardCharsets.UTF_8)));
            message = readMessage(reader);
        }
        catch (JsonSyntaxException | IOException e)
        {
            // A malformed body is a fault in whatever is speaking to us, not
            // something a model ever sees, so it is answered on the wire.
            return Response.json(400, error(JsonNull.INSTANCE, PARSE_ERROR, describeParseFailure(e)));
        }

        if (message.isJsonArray())
        {
            var batch = message.getAsJsonArray();
            if (batch.isEmpty())
                return Response.json(200, error(JsonNull.INSTANCE, INVALID_REQUEST, "a batch must not be empty"));
            if (batch.size() > MAX_BATCH_MESSAGES)
                return Response.json(200, error(JsonNull.INSTANCE, INVALID_REQUEST,
                                "a batch must not exceed " + MAX_BATCH_MESSAGES + " messages"));

            var replies = new JsonArray();
            for (JsonElement element : batch)
            {
                var reply = handleMessage(router, element, request);
                if (reply != null)
                    replies.add(reply);
            }
            // A batch of nothing but notifications has nothing to answer with.
            return replies.isEmpty() ? accepted() : Response.json(200, replies);
        }

        var reply = handleMessage(router, message, request);
        return reply == null ? accepted() : Response.json(200, reply);
    }

    /**
     * What a malformed body announces. Gson appends its own advice to a syntax
     * error - a hint to switch the reader to lenient parsing, plus a documentation
     * link - and that advice is a suggestion to accept the broken message, which is
     * exactly the wrong thing to tell a caller. Only where the reader stopped is
     * reported; a fault this reader raised itself is passed through as written.
     */
    private static String describeParseFailure(Exception e)
    {
        var message = e.getMessage();
        if (message == null)
            return "request body is not valid JSON";
        var position = message.indexOf(" at line ");
        if (position < 0)
            return "request body is not valid JSON: " + message;

        // Gson puts its advice on a second line after the position, so the position
        // is kept and everything from the line break on is dropped
        var end = message.indexOf('\n', position);
        var where = message.substring(position + 1, end < 0 ? message.length() : end);
        return "request body is not valid JSON, " + where;
    }

    /**
     * The whole body as one JSON value, strict: no trailing content, no repeated
     * key. Gson's reader would keep the last duplicate silently, and the Gson in
     * the target platform cannot be told otherwise, so the structure is walked
     * here. Number literals are handed to Gson so their text survives - an id is
     * echoed back exactly as the client sent it.
     */
    private static JsonElement readMessage(JsonReader reader) throws IOException
    {
        var value = readValue(reader);

        if (reader.peek() != JsonToken.END_DOCUMENT)
            throw new JsonSyntaxException("unexpected content after the JSON message");

        return value;
    }

    private static JsonElement readValue(JsonReader reader) throws IOException
    {
        var token = reader.peek();

        return switch (token)
        {
            case BEGIN_OBJECT -> readObject(reader);
            case BEGIN_ARRAY -> readArray(reader);
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> JsonParser.parseString(reader.nextString());
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL ->
            {
                reader.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> throw new JsonSyntaxException("unexpected token " + token);
        };
    }

    private static JsonElement readObject(JsonReader reader) throws IOException
    {
        var object = new JsonObject();
        var seen = new HashSet<String>();

        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT)
        {
            var name = reader.nextName();
            if (!seen.add(name))
                throw new JsonSyntaxException("duplicate key \"" + name + "\"");
            object.add(name, readValue(reader));
        }
        reader.endObject();

        return object;
    }

    private static JsonElement readArray(JsonReader reader) throws IOException
    {
        var array = new JsonArray();

        reader.beginArray();
        while (reader.peek() != JsonToken.END_ARRAY)
            array.add(readValue(reader));
        reader.endArray();

        return array;
    }

    /**
     * Three things go unanswered and they are not the same. A <em>reply</em>
     * (result or error) answers a request, and none is ever sent from here. A
     * <em>notification</em> is a call without an id. Anything else is not a
     * well-formed call and is told so, because 202 would tell a waiting client
     * that all was well.
     * <p/>
     * {@code jsonrpc} is deliberately not policed: refusing a client over it
     * would buy nothing and could cost a working connector.
     *
     * @return the reply, or null where the specification says not to answer
     */
    private static JsonObject handleMessage(Router router, JsonElement element, Request request)
    {
        if (!element.isJsonObject())
            return error(JsonNull.INSTANCE, INVALID_REQUEST, "a JSON-RPC message must be an object");

        var message = element.getAsJsonObject();
        var declared = message.get("method");
        var methodIsString = declared != null && declared.isJsonPrimitive()
                        && declared.getAsJsonPrimitive().isString();
        var id = message.has("id") ? message.get("id") : null;

        // A message carrying result or error and no method is the client's own
        // reply, which answers nothing.
        if (declared == null && (message.has("result") || message.has("error")))
            return null;

        // A notification is a well-formed call without an id, and gets no
        // response. A message that merely looks like one - no method, or a method
        // that is not a string - is malformed, and 202 would tell a client that
        // waits for an answer that all was well.
        if (id == null && methodIsString)
            return null;

        // An id is a string or a number, never null, an object, an array or a
        // boolean. JSON-RPC 2.0 says so; an id that is not a scalar cannot be
        // echoed as one, so a client sending it gets an error it can act on
        // rather than a reply it cannot correlate.
        if (id != null && (!id.isJsonPrimitive() || id.getAsJsonPrimitive().isBoolean()))
            return error(JsonNull.INSTANCE, INVALID_REQUEST,
                    "\"id\" must be a string or a number; a notification omits it");

        // A message carrying method as well as result or error is not a reply: it
        // is a call that was built wrongly, and 202 would tell a client that waits
        // for an answer that all was well. Its id is echoed because a batch is only
        // readable if every refusal names the message it belongs to.
        if (message.has("result") || message.has("error"))
            return error(id, INVALID_REQUEST,
                    "a JSON-RPC request carries \"params\", not \"result\" or \"error\"");

        if (!methodIsString)
            return error(id == null ? JsonNull.INSTANCE : id, INVALID_REQUEST,
                    "a JSON-RPC request needs a string \"method\"");

        var method = declared.getAsString();
        var params = message.has("params") && message.get("params").isJsonObject()
                        ? message.getAsJsonObject("params")
                        : new JsonObject();

        switch (method)
        {
            case "initialize":
                return reply(id, initialize(params));
            case "ping":
                return reply(id, new JsonObject());
            case "tools/list":
                var tools = new JsonObject();
                tools.add("tools", McpTools.listPayload());
                return reply(id, tools);
            case "tools/call":
                return reply(id, callTool(router, params, request));
            default:
                return error(id, METHOD_NOT_FOUND, "no such method: " + method);
        }
    }

    private static JsonObject initialize(JsonObject params)
    {
        var requested = params.has("protocolVersion") && params.get("protocolVersion").isJsonPrimitive()
                        ? params.get("protocolVersion").getAsString()
                        : null;

        var result = new JsonObject();
        // echo what we actually serve, our latest otherwise: claiming a
        // revision we have never seen would be a promise, not a measurement
        result.addProperty("protocolVersion",
                        requested != null && SUPPORTED_PROTOCOL_VERSIONS.contains(requested) ? requested
                                        : NEWEST_SUPPORTED_PROTOCOL_VERSION);

        var capabilities = new JsonObject();
        // Tools only, and the list never changes while the application runs.
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);

        var application = VersionHandler.applicationVersion();

        var serverInfo = new JsonObject();
        serverInfo.addProperty("name", SERVER_NAME);
        // the release a human would be told to update; outside OSGi only the
        // contract version exists
        serverInfo.addProperty("version", application != null ? application : RestApiConstants.API_VERSION);
        result.add("serverInfo", serverInfo);

        result.addProperty("instructions", McpInstructions.INSTRUCTIONS);
        return result;
    }

    private static JsonObject callTool(Router router, JsonObject params, Request request)
    {
        if (!request.authenticated())
        {
            // the one place a missing token is a refusal rather than a design,
            // so the only place that files one. A header in the wrong shape is a
            // different fault from no header at all, and the preference page has
            // to be able to tell them apart.
            RejectedConnections.record(request.path(), request.userAgent(),
                            request.authorization() == Request.Authorization.INVALID,
                            reasonOf(request.authorization()));

            var fault = request.authorization() == Request.Authorization.WRONG_SCHEME
                    ? "The `Authorization` header this connector sent does not carry a bearer token."
                    : "Portfolio Performance will not answer this connector until it presents an access token.";

            // ADR 0005: a 401 reaches nobody. The chat does.
            return toolError(fault + "\n\nAsk the user to open Preferences → MCP Server & REST API in Portfolio "
                            + "Performance, add a client there, and copy the token it shows once into this connector's "
                            + "`Authorization` header as `Bearer <token>`. Nothing can be read or changed until they "
                            + "have.");
        }

        var name = params.has("name") && params.get("name").isJsonPrimitive() ? params.get("name").getAsString() : null;
        if (name == null)
            return toolError("A tool call needs a `name`.");

        var found = McpTools.byName(name);
        if (found.isEmpty())
            return toolError("There is no tool called `" + name + "` here. Call tools/list to see what there is.");

        var tool = found.get();
        var arguments = params.has("arguments") && params.get("arguments").isJsonObject()
                        ? params.getAsJsonObject("arguments")
                        : new JsonObject();

        var rejected = rejectUnknownArguments(tool, arguments);
        if (rejected != null)
            return toolError(rejected);

        var missing = missingArguments(tool, arguments);
        if (!missing.isEmpty())
            return toolError(explainMissing(router, tool, missing));

        try
        {
            var response = McpDispatch.call(router, tool, arguments);
            return toolResult(fit(router, tool, arguments, payloadOf(response), MAX_RESULT_CHARS));
        }
        catch (ApiException e)
        {
            return toolError(explain(router, tool, arguments, e));
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return toolError("Portfolio Performance hit an internal error handling " + tool.name()
                            + ". Nothing the user did caused this, and retrying the same call will probably fail the "
                            + "same way.");
        }
    }

    /** the fault behind an unauthenticated call, spelled for the preference page */
    private static RejectedConnections.Reason reasonOf(Request.Authorization authorization)
    {
        return switch (authorization)
        {
            case INVALID -> RejectedConnections.Reason.INVALID_TOKEN;
            case WRONG_SCHEME -> RejectedConnections.Reason.NO_BEARER_SCHEME;
            default -> RejectedConnections.Reason.NO_HEADER;
        };
    }

    /**
     * Makes {@code additionalProperties: false} mean something. Ignoring an
     * argument the model misspelled would answer a question nobody asked, which
     * is why the routing table refuses undeclared query parameters too.
     */
    private static String rejectUnknownArguments(McpTool tool, JsonObject arguments)
    {
        var declared = tool.declaredArguments();
        var unknown = new ArrayList<String>();
        for (var key : arguments.keySet())
        {
            if (!declared.contains(key))
                unknown.add(key);
        }

        if (unknown.isEmpty())
            return null;

        var accepted = declared.isEmpty() ? tool.name() + " takes no arguments"
                        : tool.name() + " accepts: " + String.join(", ", declared);

        var casing = declared.stream().filter(unknown.get(0)::equalsIgnoreCase).findFirst()
                        .map(match -> " Argument names are case-sensitive — did you mean `" + match + "`?").orElse("");

        return "Unknown argument" + (unknown.size() == 1 ? " " : "s ") + unknown.stream()
                        .map(argument -> "`" + argument + "`").reduce((a, b) -> a + ", " + b).orElseThrow() + "."
                        + casing + " " + accepted + ".";
    }

    private static List<String> missingArguments(McpTool tool, JsonObject arguments)
    {
        var missing = new ArrayList<String>();
        for (String required : tool.requiredArguments())
        {
            if (!arguments.has(required) || arguments.get(required).isJsonNull())
                missing.add(required);
        }
        return missing;
    }

    /** a missing {@code file} hands the files back inline, so the model recovers in this same call */
    private static String explainMissing(Router router, McpTool tool, List<String> missing)
    {
        if (!missing.contains("file"))
        {
            return tool.name() + " needs " + missing.stream().map(name -> "`" + name + "`")
                            .reduce((a, b) -> a + " and " + b).orElseThrow() + ".";
        }

        return tool.name() + " needs a `file`. There is no default file — every call names the portfolio it touches."
                        + "\n\n" + McpExplain.describeFiles(sharedFiles(router));
    }

    /** the problem, disambiguated first where the API is deliberately vague */
    private static String explain(Router router, McpTool tool, JsonObject arguments, ApiException problem)
    {
        var needsFileList = "not-found".equals(problem.getType()) || "ambiguous-alias".equals(problem.getType());
        var fileArgument = arguments.has("file") && arguments.get("file").isJsonPrimitive()
                        ? arguments.get("file").getAsString()
                        : null;

        if (!needsFileList || fileArgument == null)
            return McpExplain.explainProblem(problem, McpExplain.Context.of(tool.name()));

        var files = sharedFiles(router);
        var shared = files == null ? null
                        : Boolean.valueOf(files.stream().anyMatch(file -> fileArgument.equals(file.id())
                                        || fileArgument.equals(file.alias())));

        return McpExplain.explainProblem(problem, new McpExplain.Context(tool.name(), files, shared, fileArgument));
    }

    /** null when even this could not be read: the problem at hand is the one worth reporting */
    private static List<McpExplain.FileSummary> sharedFiles(Router router)
    {
        try
        {
            return McpDispatch.listFiles(router);
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return null;
        }
    }

    /**
     * Fits a response to the cap, in order of preference: it fits; it can be
     * asked for less, and what was dropped is named; or it is truncated with a
     * marker. Never silently - an unmarked truncation is the one outcome that
     * produces a confident wrong answer.
     */
    private static String fit(Router router, McpTool tool, JsonObject arguments, JsonElement payload, int maxChars)
    {
        var rendered = McpEnvelope.render(tool, payload, List.of());
        if (rendered.length() <= maxChars)
            return rendered;

        var plan = McpEnvelope.planNarrowing(tool, arguments);
        if (plan.isPresent())
        {
            try
            {
                var narrowed = arguments.deepCopy();
                var metrics = new JsonArray();
                plan.get().keep().forEach(metrics::add);
                narrowed.add("metrics", metrics);

                var smaller = McpEnvelope.render(tool, payloadOf(McpDispatch.call(router, tool, narrowed)),
                                plan.get().dropped());
                if (smaller.length() <= maxChars)
                    return smaller;
                return McpEnvelope.cap(smaller + "\n\n" + McpEnvelope.oversizeAdvice(tool), maxChars);
            }
            catch (Exception e)
            {
                // The narrowed re-request failed. The first answer is still
                // true, just too large, so truncate that rather than losing it
                // to a transient problem.
                PortfolioLog.error(e);
            }
        }

        return McpEnvelope.cap(rendered + "\n\n" + McpEnvelope.oversizeAdvice(tool), maxChars);
    }

    /** a 204 carries nothing to render; {@code pp_delete_instrument} is the one */
    private static JsonElement payloadOf(Response response)
    {
        return response.body().length == 0 ? JsonNull.INSTANCE
                        : JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8));
    }

    private static JsonObject toolResult(String body)
    {
        var content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", body);

        var items = new JsonArray();
        items.add(content);

        var result = new JsonObject();
        result.add("content", items);
        return result;
    }

    /**
     * Anything the model can act on comes back as {@code isError} with text,
     * never as a protocol error - those are for faults the model never sees.
     */
    private static JsonObject toolError(String body)
    {
        var result = toolResult(body);
        result.addProperty("isError", true);
        return result;
    }

    private static JsonObject reply(JsonElement id, JsonObject result)
    {
        var message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.add("id", id);
        message.add("result", result);
        return message;
    }

    private static JsonObject error(JsonElement id, int code, String text)
    {
        var error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", text);

        var message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.add("id", id);
        message.add("error", error);
        return message;
    }

    private static Response accepted()
    {
        return new Response(202, null, new byte[0], Map.of());
    }
}
