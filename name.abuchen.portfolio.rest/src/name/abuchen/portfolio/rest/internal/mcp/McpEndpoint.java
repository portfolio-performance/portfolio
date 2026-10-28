package name.abuchen.portfolio.rest.internal.mcp;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
import name.abuchen.portfolio.rest.ApiRoutes;
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
 * <b>A tool call is answered as a stream</b> when the client accepts
 * {@code text/event-stream}: progress notifications while it waits for the UI
 * thread, for a calculation slot or for the calculation - if it sent a
 * {@code progressToken} - heartbeats in any case, and the result as the last
 * event. Everything else, and every client that accepts only JSON, gets plain
 * {@code application/json} as before. {@code GET} stays the router's 405: there
 * is nothing the server would say without being asked.
 * <p/>
 * <b>No session model yet.</b> No {@code Mcp-Session-Id} is issued, and
 * {@code DELETE} falls through to the router's 405. Everything a session could
 * hold travels per request instead.
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

    /**
     * How deeply a message may nest objects and arrays. No tool argument comes
     * near it; the bound is there because the parser recurses.
     */
    private static final int MAX_NESTING = 64;

    /** an entity uuid as {@code pp_list_instruments} reports one */
    private static final Pattern UUID_PATTERN = Pattern.compile(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

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

    /**
     * How often a streamed call sends a heartbeat. Well inside the watchdog of
     * the server and the idle timeouts of the usual HTTP clients. Not final: a
     * test shortens it.
     */
    /* package */ static volatile long heartbeatMillis = 15_000;

    /** sends the heartbeats of every streamed call */
    private static final ScheduledExecutorService HEARTBEATS = Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform().name("Portfolio Performance MCP heartbeat").daemon(true).factory()); //$NON-NLS-1$

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

        if (isToolCall(message) && acceptsStream(request))
            return stream(router, message.getAsJsonObject(), request);

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

    /** a tools/call request - a request, with an id, not a notification */
    private static boolean isToolCall(JsonElement message)
    {
        return isCall(message, "tools/call") && message.getAsJsonObject().has("id")
                        && message.getAsJsonObject().get("id").isJsonPrimitive()
                        && !message.getAsJsonObject().get("id").getAsJsonPrimitive().isBoolean()
                        && !message.getAsJsonObject().has("result") && !message.getAsJsonObject().has("error");
    }

    private static boolean isCall(JsonElement message, String method)
    {
        if (!message.isJsonObject())
            return false;
        var declared = message.getAsJsonObject().get("method");
        return declared != null && declared.isJsonPrimitive() && declared.getAsJsonPrimitive().isString()
                        && method.equals(declared.getAsString());
    }

    private static boolean acceptsStream(Request request)
    {
        var accept = request.header("accept");
        return accept != null && accept.toLowerCase(Locale.ROOT).contains("text/event-stream");
    }

    /**
     * A tool call answered as an event stream. The call runs while the stream
     * is open, on the thread that writes it; progress and heartbeats go out on
     * the same stream from the threads that produce them. A call its client
     * cancelled - or hung up on - ends the stream without a result, as the
     * protocol asks.
     */
    private static Response stream(Router router, JsonObject message, Request request)
    {
        var id = message.get("id");
        var params = message.has("params") && message.get("params").isJsonObject()
                        ? message.getAsJsonObject("params")
                        : new JsonObject();

        var call = new McpCall(progressTokenOf(params));

        return Response.streamed(200, "text/event-stream", sink -> { //$NON-NLS-1$
            call.attach(sink);
            var heartbeat = HEARTBEATS.scheduleAtFixedRate(call::heartbeat, heartbeatMillis, heartbeatMillis,
                            TimeUnit.MILLISECONDS);
            try
            {
                var result = callTool(router, params, request.withProgress(call));
                if (!call.isCancelled())
                    call.send(reply(id, result));
            }
            catch (CancellationException e)
            {
                // cancelled: the client said it no longer wants the answer
            }
            finally
            {
                heartbeat.cancel(false);
            }
        });
    }

    /** the token the client asked progress to be reported under, or null */
    private static JsonElement progressTokenOf(JsonObject params)
    {
        var meta = params.get("_meta");
        if (meta == null || !meta.isJsonObject())
            return null;
        var token = meta.getAsJsonObject().get("progressToken");
        // a string or an integer, says the protocol; anything else asks for nothing
        return token != null && token.isJsonPrimitive() && !token.getAsJsonPrimitive().isBoolean() ? token : null;
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
        var value = readValue(reader, 0);

        if (reader.peek() != JsonToken.END_DOCUMENT)
            throw new JsonSyntaxException("unexpected content after the JSON message");

        return value;
    }

    private static JsonElement readValue(JsonReader reader, int depth) throws IOException
    {
        // the walk recurses per object or array, and the body limit bounds the
        // size, not the nesting: a megabyte of brackets would overflow the stack
        if (depth > MAX_NESTING)
            throw new JsonSyntaxException("the message is nested more than " + MAX_NESTING + " levels deep");

        var token = reader.peek();

        return switch (token)
        {
            case BEGIN_OBJECT -> readObject(reader, depth + 1);
            case BEGIN_ARRAY -> readArray(reader, depth + 1);
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

    private static JsonElement readObject(JsonReader reader, int depth) throws IOException
    {
        var object = new JsonObject();
        var seen = new HashSet<String>();

        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT)
        {
            var name = reader.nextName();
            if (!seen.add(name))
                throw new JsonSyntaxException("duplicate key \"" + name + "\"");
            object.add(name, readValue(reader, depth));
        }
        reader.endObject();

        return object;
    }

    private static JsonElement readArray(JsonReader reader, int depth) throws IOException
    {
        var array = new JsonArray();

        reader.beginArray();
        while (reader.peek() != JsonToken.END_ARRAY)
            array.add(readValue(reader, depth));
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
                            + "have." + hint("no-token", true));
        }

        var name = params.has("name") && params.get("name").isJsonPrimitive() ? params.get("name").getAsString() : null;
        if (name == null)
            return toolError("A tool call needs a `name`." + hint("no-tool-name", false));

        var found = McpTools.byName(name);
        if (found.isEmpty())
            return toolError("There is no tool called `" + name + "` here. Call tools/list to see what there is."
                            + hint("unknown-tool", false));

        var tool = found.get();
        var arguments = trimPathArguments(tool, params.has("arguments") && params.get("arguments").isJsonObject()
                        ? params.getAsJsonObject("arguments")
                        : new JsonObject());

        var rejected = rejectUnknownArguments(tool, arguments);
        if (rejected != null)
            return toolError(rejected + hint("unknown-argument", false));

        var missing = missingArguments(tool, arguments);
        if (!missing.isEmpty())
            return toolError(explainMissing(router, tool, missing) + hint("missing-argument", false));

        var misshapen = rejectMisshapenArguments(tool, arguments);
        if (misshapen != null)
            return toolError(misshapen + hint("misshapen-argument", false));

        try
        {
            // A delete answers 204 and nothing else, which leaves a model unable
            // to say what it removed. The record is read first - through the
            // same routing table - so the result can name it.
            var doomed = "DELETE".equals(tool.method()) ? McpDispatch.readBeforeDelete(router, tool, arguments)
                            : null;

            var response = McpDispatch.call(router, tool, arguments, request.progress());
            // a PATCH says whether a stored value changed, which decides whether
            // the user has anything to save
            var changed = response.headers().get(ApiRoutes.CHANGED_HEADER);

            var payload = payloadOf(response);
            if (doomed != null && payload.isJsonNull())
            {
                var deleted = new JsonObject();
                deleted.addProperty("deleted", true);
                deleted.add("instrument", doomed);
                payload = deleted;
            }

            return toolResult(fit(tool, arguments, payload, MAX_RESULT_CHARS,
                            changed == null ? null : Boolean.valueOf(changed)));
        }
        catch (ApiException e)
        {
            return toolError(explain(router, tool, arguments, e));
        }
        catch (CancellationException e)
        {
            // not a failure: the caller gave up, and gets no answer
            throw e;
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return toolError("Portfolio Performance hit an internal error handling " + tool.name()
                            + ". Nothing the user did caused this, and retrying the same call will probably fail the "
                            + "same way." + hint("internal-error", false));
        }
    }

    /**
     * A file id or an entity uuid with a stray space around it was refused
     * once as misshapen and once as not found, depending on which argument
     * carried it. Neither can contain whitespace, so it is dropped before
     * either check sees it; nothing else about the arguments is touched.
     */
    private static JsonObject trimPathArguments(McpTool tool, JsonObject arguments)
    {
        JsonObject trimmed = null;
        for (McpTool.Binding binding : tool.bindings())
        {
            if (binding.where() != McpTool.Where.PATH)
                continue;
            var value = arguments.get(binding.arg());
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                continue;
            var text = value.getAsString();
            if (text.strip().equals(text))
                continue;
            if (trimmed == null)
                trimmed = arguments.deepCopy();
            trimmed.addProperty(binding.arg(), text.strip());
        }
        return trimmed != null ? trimmed : arguments;
    }

    /**
     * The machine-readable trailer of a refusal this endpoint raised itself.
     * {@link McpExplain} adds the same trailer for a problem the REST layer
     * raised; without it, a connector-level refusal would be the only kind a
     * client could not act on as data. Retryable means only that the same call
     * may succeed later unchanged - here, after the user has added a token.
     */
    private static String hint(String reasonCode, boolean retryable)
    {
        return "\n\nretryable: " + (retryable ? "yes" : "no") + " · reasonCode: " + reasonCode;
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

    /**
     * Whether an argument has a shape the route can use. Dispatch turns an
     * argument into a path segment, a comma-joined query value or a body field,
     * so a number where a segment is expected or an object where a query value is
     * expected would be answered with data for a question nobody asked. The tool
     * declares these types in its schema, which is what a model is told to
     * produce; checking them is cheaper than trusting that it did.
     * <p/>
     * Body fields are not checked beyond their own type: the handler validates
     * each patch field against the model and names the field it rejected.
     */
    private static String rejectMisshapenArguments(McpTool tool, JsonObject arguments)
    {
        var properties = tool.inputSchema().getAsJsonObject("properties");
        if (properties == null)
            return null;

        var problems = new ArrayList<String>();
        for (var entry : arguments.entrySet())
        {
            var schema = properties.get(entry.getKey());
            if (schema == null || !schema.isJsonObject())
                continue;

            var problem = describeMisshapenValue(tool, entry.getKey(), schema.getAsJsonObject(), entry.getValue());
            if (problem != null)
                problems.add("- " + problem);
        }

        if (problems.isEmpty())
            return null;

        return "Some arguments are not of the shape `" + tool.name() + "` declares:\n"
                        + String.join("\n", problems);
    }

    private static String describeMisshapenValue(McpTool tool, String argument, JsonObject schema, JsonElement value)
    {
        // An explicit null on an optional query argument is what not sending it
        // means: the handler applies the endpoint's default (today, the base
        // currency, the latest quote). A body field of null means something else
        // - clearing that field - so it stays checked. A required argument that
        // arrives as null was already refused as missing.
        var binding = tool.binding(argument);
        if (value.isJsonNull() && binding != null && binding.where() == McpTool.Where.QUERY)
            return null;

        var declared = schema.get("type");
        if (declared != null && !matchesType(value, declared))
            return "`" + argument + "` must be " + typesOf(declared) + ", not " + shapeOf(value);

        var enumeration = schema.get("enum");
        if (enumeration != null && enumeration.isJsonArray() && !holds(enumeration.getAsJsonArray(), value))
            return "`" + argument + "` must be one of " + namesOf(enumeration.getAsJsonArray());

        var minLength = schema.get("minLength");
        if (minLength != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && value.getAsString().length() < minLength.getAsInt())
            return "`" + argument + "` must not be empty";

        var items = schema.get("items");
        if (items != null && items.isJsonObject() && value.isJsonArray())
        {
            var itemSchema = items.getAsJsonObject();
            var declaredItems = itemSchema.get("type");
            var permitted = itemSchema.get("enum");

            for (JsonElement element : value.getAsJsonArray())
            {
                if (declaredItems != null && !matchesType(element, declaredItems))
                    return "`" + argument + "` must hold " + typesOf(declaredItems) + ", not " + shapeOf(element);

                if (permitted != null && permitted.isJsonArray() && !holds(permitted.getAsJsonArray(), element))
                    return "`" + argument + "` must hold one of " + namesOf(permitted.getAsJsonArray());
            }
        }

        var minItems = schema.get("minItems");
        if (minItems != null && value.isJsonArray() && value.getAsJsonArray().size() < minItems.getAsInt())
            return "`" + argument + "` must hold at least " + minItems.getAsInt()
                    + " value" + (minItems.getAsInt() == 1 ? "" : "s") + "; an empty list names nothing";

        if (binding != null && binding.where() == McpTool.Where.PATH)
        {
            var text = value.isJsonPrimitive() ? value.getAsString() : value.toString();
            if (text.isBlank())
                return "`" + argument + "` must name something; a blank value resolves no path";

            // A uuid is substituted into a path, and a path whose segment is a
            // reserved literal belongs to another endpoint: "attribute-types"
            // would answer with attribute types instead of one instrument.
            if ("uuid".equals(binding.wire()) && !UUID_PATTERN.matcher(text).matches())
                return "`" + argument + "` must be an entity uuid as " + listingOf(tool) + " reports it, not `"
                        + text + "`";
        }

        return null;
    }

    /** the tool that lists the entities a uuid-taking tool addresses */
    private static String listingOf(McpTool tool)
    {
        if (tool.name().contains("cash_account"))
            return "pp_list_cash_accounts";
        if (tool.name().contains("investment_account"))
            return "pp_list_investment_accounts";
        return "pp_list_instruments";
    }

    private static boolean matchesType(JsonElement value, JsonElement declared)
    {
        if (declared.isJsonArray())
        {
            for (JsonElement type : declared.getAsJsonArray())
            {
                if (matchesType(value, type))
                    return true;
            }
            return false;
        }

        return matchesSingleType(value, declared.getAsString());
    }

    private static boolean matchesSingleType(JsonElement value, String type)
    {
        return switch (type)
        {
            case "string" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
            case "number", "integer" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
            case "boolean" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
            case "array" -> value.isJsonArray();
            case "object" -> value.isJsonObject();
            case "null" -> value.isJsonNull();
            default -> true;
        };
    }

    private static String typesOf(JsonElement declared)
    {
        if (!declared.isJsonArray())
            return "`" + declared.getAsString() + "`";

        var names = new ArrayList<String>();
        for (JsonElement type : declared.getAsJsonArray())
            names.add("`" + type.getAsString() + "`");

        return String.join(" or ", names);
    }

    private static String shapeOf(JsonElement value)
    {
        if (value.isJsonNull())
            return "null";
        if (value.isJsonArray())
            return "an array";
        if (value.isJsonObject())
            return "an object";
        if (value.getAsJsonPrimitive().isBoolean())
            return "a boolean";
        if (value.getAsJsonPrimitive().isNumber())
            return "a number";

        return "the string `" + value.getAsString() + "`";
    }

    private static boolean holds(JsonArray permitted, JsonElement value)
    {
        if (!value.isJsonPrimitive())
            return false;

        for (JsonElement candidate : permitted)
        {
            if (candidate.equals(value))
                return true;
        }
        return false;
    }

    private static String namesOf(JsonArray permitted)
    {
        var names = new ArrayList<String>();
        for (JsonElement element : permitted)
            names.add("`" + element.getAsString() + "`");

        return String.join(", ", names);
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
     * asked for less, and what was dropped is named; or the payload is cut and
     * the guidance that says so is kept. Never silently - an unmarked truncation
     * is the one outcome that produces a confident wrong answer, and a truncation
     * that also dropped the notes is barely better.
     * <p/>
     * Not private: a test in this package drives it with a payload no fixture
     * would ever produce, which is how the cap is checked at its real limit.
     */
    /* package */ static String fit(McpTool tool, JsonObject arguments, JsonElement payload, int maxChars)
    {
        return fit(tool, arguments, payload, maxChars, null);
    }

    /**
     * @param changed
     *            for a write, whether a stored value changed; null when the
     *            response does not say
     */
    /* package */ static String fit(McpTool tool, JsonObject arguments, JsonElement payload, int maxChars,
                    Boolean changed)
    {
        var rendered = McpEnvelope.render(tool, arguments, payload, List.of(), null, Integer.MAX_VALUE, changed);
        if (rendered.length() <= maxChars)
            return rendered;

        var plan = McpEnvelope.planNarrowing(tool, arguments);
        if (plan.isPresent())
        {
            var narrowed = arguments.deepCopy();
            var metrics = new JsonArray();
            plan.get().keep().forEach(metrics::add);
            narrowed.add("metrics", metrics);

            // cut from the answer at hand, not asked for again: the full
            // calculation already ran, and a second one would take another slot
            var smallerPayload = McpEnvelope.narrow(payload, plan.get().keep());
            var smaller = McpEnvelope.render(tool, narrowed, smallerPayload, plan.get().dropped());
            if (smaller.length() <= maxChars)
                return smaller;

            // the dropped groups are not advice any more: they are already gone
            return McpEnvelope.render(tool, narrowed, smallerPayload, plan.get().dropped(),
                            McpEnvelope.oversizeAdvice(tool, plan.get().dropped()), maxChars);
        }

        var advice = McpEnvelope.oversizeAdvice(tool, arguments, payload);
        return McpEnvelope.render(tool, arguments, payload, List.of(), advice, maxChars, changed);
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
