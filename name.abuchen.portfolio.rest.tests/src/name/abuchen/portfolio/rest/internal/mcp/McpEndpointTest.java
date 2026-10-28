package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.RejectedConnections;
import name.abuchen.portfolio.rest.RestApiServer;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * The MCP endpoint over the real server: what answers without a token, what the
 * transport refuses, and that a client asking for OAuth metadata is told there
 * is none rather than asked to authenticate for it.
 */
@SuppressWarnings("nls")
public class McpEndpointTest
{
    private static final String TOKEN = "test-token";
    private static final String FILE = "/tmp/mcp.portfolio";

    private IEclipsePreferences node;
    private RestApiServer server;
    private HttpClient http;
    private FakeHost host;

    @Before
    public void setUp() throws Exception
    {
        var client = new Client();
        new SecurityBuilder().addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(FILE, true);
        registry.setAlias(FILE, "sample");

        host = new FakeHost(List.of(new FakeHost.FakeOpenFile(FILE, "mcp", client)));
        var router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));

        RejectedConnections.clear();
        server = new RestApiServer(0, TOKEN::equals, router);
        server.start();
        http = HttpClient.newHttpClient();
    }

    @After
    public void tearDown() throws Exception
    {
        McpEndpoint.heartbeatMillis = 15_000;
        server.stop();
        node.removeNode();
    }

    // ── what answers without a token (ADR 0005) ─────────────────────────────

    @Test
    public void testInitializeAnswersWithoutAToken() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2025-03-26\"}}");

        assertThat(response.statusCode(), is(200));

        var result = body(response).getAsJsonObject().getAsJsonObject("result");
        // the client's own version, echoed, because we serve it
        assertThat(result.get("protocolVersion").getAsString(), is("2025-03-26"));
        assertThat(result.getAsJsonObject("capabilities").has("tools"), is(true));
        assertThat(result.getAsJsonObject("serverInfo").get("name").getAsString(), is("Portfolio Performance"));
        assertThat(result.get("instructions"), is(notNullValue()));
    }

    @Test
    public void testAnUnknownProtocolVersionGetsOurLatest() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2099-01-01\"}}");

        var result = body(response).getAsJsonObject().getAsJsonObject("result");
        assertThat(result.get("protocolVersion").getAsString(), is("2025-06-18"));
    }

    @Test
    public void testToolsListAndPingAnswerWithoutAToken() throws Exception
    {
        var tools = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
        assertThat(tools.statusCode(), is(200));
        assertThat(body(tools).getAsJsonObject().getAsJsonObject("result").getAsJsonArray("tools").size(), is(16));

        var ping = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}");
        assertThat(ping.statusCode(), is(200));
        assertThat(body(ping).getAsJsonObject().getAsJsonObject("result").size(), is(0));
    }

    /** a 401 is invisible in the client UI, so the refusal has to reach the chat */
    @Test
    public void testToolCallWithoutATokenIsARefusalInTheChat() throws Exception
    {
        var response = rpc(null, call("pp_list_files", "{}"));

        assertThat(response.statusCode(), is(200));
        var result = body(response).getAsJsonObject().getAsJsonObject("result");
        assertThat(result.get("isError").getAsBoolean(), is(true));
        assertThat(text(result), containsString("access token"));
        assertThat(text(result), containsString("Preferences"));
    }

    @Test
    public void testToolCallWithATokenReachesTheRoute() throws Exception
    {
        var response = rpc(TOKEN, call("pp_list_files", "{}"));

        assertThat(response.statusCode(), is(200));
        var result = body(response).getAsJsonObject().getAsJsonObject("result");
        assertThat(result.has("isError"), is(false));
        assertThat(text(result), containsString("sample"));
    }

    // ── the transport ───────────────────────────────────────────────────────

    /** there is nothing the server says unasked, so there is no stream to open */
    @Test
    public void testGetIsNotAllowed() throws Exception
    {
        assertThat(send(HttpRequest.newBuilder(uri()).header("Authorization", "Bearer " + TOKEN).GET()).statusCode(),
                        is(405));
    }

    // ── sessions ────────────────────────────────────────────────────────────

    @Test
    public void testInitializeIssuesASession() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("Mcp-Session-Id").orElse("").length() > 30, is(true));
        assertThat(response.headers().firstValue("Content-Type").orElseThrow(), containsString("application/json"));

        // and a request in it is served as any other
        var ping = send(session(HttpRequest.newBuilder(uri()), initialize())
                        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}")));
        assertThat(ping.statusCode(), is(200));
    }

    /** a session from before a restart: 404 is how the client learns to initialize again */
    @Test
    public void testAnUnknownSessionIsNotFound() throws Exception
    {
        var response = send(session(HttpRequest.newBuilder(uri()), "no-such-session")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}")));

        assertThat(response.statusCode(), is(404));
        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32001));
    }

    /** the handshake itself starts over: the stale id it may still carry is no obstacle */
    @Test
    public void testInitializeWithAStaleSessionStartsANewOne() throws Exception
    {
        var response = send(session(HttpRequest.newBuilder(uri()), "no-such-session").POST(HttpRequest.BodyPublishers
                        .ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")));

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("Mcp-Session-Id").orElseThrow(), is(not("no-such-session")));
    }

    /** a request without a session is served as before - it only cannot be cancelled */
    @Test
    public void testARequestWithoutASessionIsServed() throws Exception
    {
        assertThat(rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}").statusCode(), is(200));
    }

    @Test
    public void testDeleteEndsTheSession() throws Exception
    {
        var id = initialize();

        assertThat(send(session(HttpRequest.newBuilder(uri()), id).DELETE()).statusCode(), is(204));
        assertThat(send(session(HttpRequest.newBuilder(uri()), id).DELETE()).statusCode(), is(404));
        assertThat(send(HttpRequest.newBuilder(uri()).DELETE()).statusCode(), is(400));
    }

    // ── streamed tool calls ─────────────────────────────────────────────────

    /** only a tool call is streamed; a ping is answered as JSON whatever the client accepts */
    @Test
    public void testOnlyAToolCallIsStreamed() throws Exception
    {
        var response = send(HttpRequest.newBuilder(uri()) //
                        .header("Accept", "application/json, text/event-stream") //
                        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}")));

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("Content-Type").orElseThrow(), containsString("application/json"));
    }

    /** a client that accepts only JSON gets JSON, for a tool call too */
    @Test
    public void testAToolCallIsJsonWithoutAStreamInTheAcceptHeader() throws Exception
    {
        var response = rpc(TOKEN, call("pp_list_files", "{}"));

        assertThat(response.headers().firstValue("Content-Type").orElseThrow(), containsString("application/json"));
        assertThat(text(body(response).getAsJsonObject().getAsJsonObject("result")), containsString("sample"));
    }

    @Test
    public void testAToolCallIsStreamedWhenTheClientTakesAStream() throws Exception
    {
        var response = stream(call("pp_list_files", "{}"));

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("Content-Type").orElseThrow(), containsString("text/event-stream"));

        var events = events(response.body());
        var last = events.get(events.size() - 1);
        assertThat(last.get("id").getAsInt(), is(9));
        assertThat(text(last.getAsJsonObject("result")), containsString("sample"));
    }

    /** with a progress token, the stages of a calculation are reported before the result */
    @Test
    public void testACalculationReportsItsProgress() throws Exception
    {
        var events = events(stream(callWithToken("pp_list_holdings", "{\"file\":\"sample\"}", "\"p-1\"")).body());

        var progress = events.stream().filter(event -> event.has("method")).toList();
        assertThat(progress.isEmpty(), is(false));
        assertThat(progress.get(0).get("method").getAsString(), is("notifications/progress"));
        assertThat(progress.get(0).getAsJsonObject("params").get("progressToken").getAsString(), is("p-1"));
        assertThat(progress.stream().anyMatch(event -> "calculating"
                        .equals(event.getAsJsonObject("params").get("message").getAsString())), is(true));

        // each value larger than the one before, as the protocol requires
        var last = 0L;
        for (var event : progress)
        {
            var value = event.getAsJsonObject("params").get("progress").getAsLong();
            assertThat(value > last, is(true));
            last = value;
        }

        assertThat(events.get(events.size() - 1).get("id").getAsInt(), is(9));
    }

    /** without a token nothing but the result: progress is reported only when asked for */
    @Test
    public void testNoProgressWithoutAToken() throws Exception
    {
        var events = events(stream(call("pp_list_holdings", "{\"file\":\"sample\"}")).body());

        assertThat(events.size(), is(1));
        assertThat(events.get(0).get("id").getAsInt(), is(9));
    }

    /** a call that waits says so on the wire, so that the connection is not taken for dead */
    @Test
    public void testAWaitingCallSendsHeartbeats() throws Exception
    {
        McpEndpoint.heartbeatMillis = 50;
        var gate = new CountDownLatch(1);
        host.blockUiThreadUntil(gate);

        var pending = http.sendAsync(streamRequest(call("pp_list_files", "{}"), null).build(),
                        HttpResponse.BodyHandlers.ofString());
        Thread.sleep(400);
        gate.countDown();

        var body = pending.get(10, TimeUnit.SECONDS).body();
        assertThat(body, containsString(": keepalive"));
        assertThat(events(body).get(events(body).size() - 1).get("id").getAsInt(), is(9));
    }

    /**
     * A cancellation is a request of its own, and the session is how it finds
     * the call it names. The call is held on the UI thread until it arrives; the
     * stream then ends without an answer, as the protocol asks.
     */
    @Test
    public void testACancelledCallEndsWithoutAnAnswer() throws Exception
    {
        var id = initialize();
        var gate = new CountDownLatch(1);
        host.blockUiThreadUntil(gate);

        var pending = http.sendAsync(
                        streamRequest(callWithToken("pp_list_holdings", "{\"file\":\"sample\"}", "\"c-1\""), id).build(),
                        HttpResponse.BodyHandlers.ofString());
        Thread.sleep(300);

        var cancel = send(session(HttpRequest.newBuilder(uri()), id).POST(HttpRequest.BodyPublishers.ofString(
                        "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":9}}")));
        assertThat(cancel.statusCode(), is(202));
        gate.countDown();

        var body = pending.get(10, TimeUnit.SECONDS).body();
        assertThat(events(body).stream().anyMatch(event -> event.has("id")), is(false));
    }

    @Test
    public void testANotificationIsAcceptedAndNotAnswered() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

        assertThat(response.statusCode(), is(202));
        assertThat(response.body(), is(""));
    }

    @Test
    public void testABatchAnswersOnlyItsRequests() throws Exception
    {
        var response = rpc(null, "[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"},"
                        + "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"},"
                        + "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}]");

        assertThat(response.statusCode(), is(200));
        assertThat(body(response).getAsJsonArray().size(), is(2));
    }

    @Test
    public void testAnUnknownMethodIsAProtocolError() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"resources/list\"}");

        assertThat(response.statusCode(), is(200));
        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32601));
    }

    @Test
    public void testAMalformedBodyIsRefusedOnTheWire() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\": broken");

        assertThat(response.statusCode(), is(400));
        var error = body(response).getAsJsonObject().getAsJsonObject("error");
        assertThat(error.get("code").getAsInt(), is(-32700));

        // Gson ends a syntax error with advice to parse leniently and a link; a
        // caller told that would try to accept the broken body
        assertThat(error.get("message").getAsString(), containsString("at line 1 column 13"));
        assertThat(error.get("message").getAsString(), not(containsString("setStrictness")));
        assertThat(error.get("message").getAsString(), not(containsString("github.com")));
    }

    /**
     * Neither a notification nor a reply, so the client is waiting for an
     * answer: a silent 202 would tell it everything went fine.
     */
    @Test
    public void testARequestWithNoMethodIsInvalidRatherThanAccepted() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":7}");

        assertThat(response.statusCode(), is(200));
        var reply = body(response).getAsJsonObject();
        assertThat(reply.get("id").getAsInt(), is(7));
        assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    @Test
    public void testAMethodThatIsNotAStringIsInvalid() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":123}");

        var reply = body(response).getAsJsonObject();
        assertThat(reply.get("id").getAsInt(), is(7));
        assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    /** no id to answer with, so the error carries a null one */
    @Test
    public void testAMalformedNotificationIsAnsweredWithANullId() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"method\":123}");

        var reply = body(response).getAsJsonObject();
        assertThat(reply.get("id").isJsonNull(), is(true));
        assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    /** a reply, by contrast, answers a request - and none is ever sent from here */
    @Test
    public void testAReplyToARequestWeNeverSentIsIgnored() throws Exception
    {
        assertThat(rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":{}}").statusCode(), is(202));
        assertThat(rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":7,\"error\":{\"code\":-1,\"message\":\"x\"}}")
                        .statusCode(), is(202));
    }

    @Test
    public void testAnEmptyBatchIsInvalid() throws Exception
    {
        var response = rpc(null, "[]");

        assertThat(response.statusCode(), is(200));
        var reply = body(response).getAsJsonObject();
        assertThat(reply.get("id").isJsonNull(), is(true));
        assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    /** each element of a batch is judged on its own */
    @Test
    public void testABatchOfNonObjectsAnswersOnePerElement() throws Exception
    {
        var response = rpc(null, "[1,2]");

        var replies = body(response).getAsJsonArray();
        assertThat(replies.size(), is(2));
        for (var reply : replies)
            assertThat(reply.getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    @Test
    public void testABatchMixingValidAndInvalidAnswersBoth() throws Exception
    {
        var response = rpc(null, "[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"},"
                        + "{\"jsonrpc\":\"2.0\",\"id\":2}]");

        var replies = body(response).getAsJsonArray();
        assertThat(replies.size(), is(2));
        assertThat(replies.get(0).getAsJsonObject().has("result"), is(true));
        assertThat(replies.get(1).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    /** the cap exists because one body can hold the two worker threads hostage */
    @Test
    public void testABatchOfThirtyTwoIsAnsweredAndThirtyThreeIsNot() throws Exception
    {
        var exactly = batch(32);
        assertThat(body(rpc(null, exactly)).getAsJsonArray().size(), is(32));

        var response = rpc(null, batch(33));
        assertThat(response.statusCode(), is(200));
        var reply = body(response).getAsJsonObject();
        assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
        assertThat(reply.getAsJsonObject("error").get("message").getAsString(), containsString("must not exceed 32"));
    }

    /** a request that carries a `result` is a reply wearing a method, not a request */
    @Test
    public void testARequestThatCarriesAResultIsInvalid() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"ping\",\"result\":{\"pong\":true}}");

        var reply = body(response).getAsJsonObject();
        assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
        assertThat(reply.getAsJsonObject("error").get("message").getAsString(), containsString("not \"result\""));
        // a batch is only readable if every refusal names the message it belongs to
        assertThat(reply.get("id").getAsInt(), is(7));
    }

    /**
     * JSON-RPC ids are strings or numbers. An object, an array or a boolean cannot
     * be echoed as one, and a reply that carries it back is a reply no client can
     * match to its request.
     */
    @Test
    public void testAnIdThatIsNotAStringOrNumberIsInvalid() throws Exception
    {
        for (var id : List.of("{}", "[]", "true", "[1,2]", "{\"a\":1}"))
        {
            var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"ping\"}");

            var reply = body(response).getAsJsonObject();
            assertThat(reply.getAsJsonObject("error").get("code").getAsInt(), is(-32600));
            assertThat(reply.getAsJsonObject("error").get("message").getAsString(),
                            containsString("\"id\" must be a string or a number"));
            assertThat(reply.get("id").isJsonNull(), is(true));
        }
    }

    /** a notification omits `id`; sending `null` is neither, and answering it would echo nothing */
    @Test
    public void testANullIdIsInvalidRatherThanANotification() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"ping\"}");

        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32600));
        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("message").getAsString(),
                        containsString("\"id\" must be a string or a number"));
    }

    /** the parser recurses per level, so a deeply nested body is refused rather than overflowing the stack */
    @Test
    public void testADeeplyNestedMessageIsAParseError() throws Exception
    {
        var nested = "[".repeat(10_000) + "]".repeat(10_000);
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":{\"a\":" + nested + "}}");

        assertThat(response.statusCode(), is(400));
        var error = body(response).getAsJsonObject().getAsJsonObject("error");
        assertThat(error.get("code").getAsInt(), is(-32700));
        assertThat(error.get("message").getAsString(), containsString("nested more than 64 levels"));
    }

    /**
     * A duplicate key silently keeps whichever value Gson happened to take last,
     * so a body that says `"id":1,"id":2` is a malformed message, not a choice we
     * should quietly make for the client.
     */
    @Test
    public void testADuplicateKeyIsRefusedAsAParseError() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"id\":2}");

        assertThat(response.statusCode(), is(400));
        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32700));
    }

    @Test
    public void testATopLevelScalarIsInvalid() throws Exception
    {
        var response = rpc(null, "\"hello\"");

        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32600));
    }

    /** the DNS-rebinding defence applies here exactly as everywhere else */
    @Test
    public void testABrowserOriginIsStillRefused() throws Exception
    {
        var response = send(HttpRequest.newBuilder(uri()).header("Origin", "https://example.com")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}")));

        assertThat(response.statusCode(), is(403));
    }

    // ── there is no authorization server here ───────────────────────────────

    /** a 404 says "no authorization server here"; a 401 says something else entirely */
    @Test
    public void testWellKnownPathsAnswer404AndNot401() throws Exception
    {
        for (String path : List.of("/.well-known/oauth-protected-resource",
                        "/.well-known/oauth-protected-resource/mcp", "/.well-known/oauth-authorization-server",
                        "/.well-known/openid-configuration"))
        {
            var response = send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + server.getPort() + path)).GET());
            assertThat(path, response.statusCode(), is(404));
        }
    }

    // ── diagnosability ──────────────────────────────────────────────────────

    /** a wrong token never reaches the client list, so this is the user's only trace of it */
    @Test
    public void testARefusedToolCallIsRecordedForThePreferencePage() throws Exception
    {
        send(HttpRequest.newBuilder(uri()).header("Authorization", "Bearer wrong-token")
                        .header("User-Agent", "codex-mcp-client/0.153.4")
                        .POST(HttpRequest.BodyPublishers.ofString(call("pp_list_files", "{}"))));

        var rejection = RejectedConnections.last().orElseThrow();
        assertThat(rejection.path(), is("/mcp"));
        assertThat(rejection.userAgent(), is("codex-mcp-client/0.153.4"));
        assertThat(rejection.tokenPresented(), is(true));
    }

    @Test
    public void testAToolCallWithNoTokenAtAllIsRecordedAsSuch() throws Exception
    {
        rpc(null, call("pp_list_files", "{}"));

        var rejection = RejectedConnections.last().orElseThrow();
        assertThat(rejection.tokenPresented(), is(false));
        assertThat(rejection.reason(), is(RejectedConnections.Reason.NO_HEADER));
    }

    /**
     * A header that carries some other scheme is a different fault from no header,
     * and the preference page has to say which - "you forgot the header" is a
     * misdiagnosis of a connector that sent one.
     */
    @Test
    public void testAToolCallWithTheWrongSchemeIsRecordedAsSuch() throws Exception
    {
        var response = send(HttpRequest.newBuilder(uri()).header("Authorization", "Token abc")
                        .POST(HttpRequest.BodyPublishers.ofString(call("pp_list_files", "{}"))));

        var rejection = RejectedConnections.last().orElseThrow();
        assertThat(rejection.path(), is("/mcp"));
        assertThat(rejection.tokenPresented(), is(false));
        assertThat(rejection.reason(), is(RejectedConnections.Reason.NO_BEARER_SCHEME));

        assertThat(text(body(response).getAsJsonObject().getAsJsonObject("result")),
                        containsString("does not carry a bearer token"));
    }

    /**
     * The handshake is specified to work without a token, so it is not a
     * refusal and must not warn the user about a connection that behaved.
     */
    @Test
    public void testATokenFreeHandshakeIsNotARefusal() throws Exception
    {
        rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        rpc(null, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"ping\"}");

        assertThat(RejectedConnections.last().isPresent(), is(false));
    }

    /** nor is a client asking for OAuth metadata that is not there */
    @Test
    public void testProbingForAnAuthorizationServerIsNotARefusal() throws Exception
    {
        send(HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + server.getPort() + "/.well-known/oauth-protected-resource")).GET());

        assertThat(RejectedConnections.last().isPresent(), is(false));
    }

    /** a /v1 route, by contrast, is refused outright and says so */
    @Test
    public void testAnUnauthenticatedRestCallIsStillRecorded() throws Exception
    {
        var response = send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + server.getPort() + "/v1/files")).GET());

        assertThat(response.statusCode(), is(401));
        assertThat(RejectedConnections.last().orElseThrow().path(), is("/v1/files"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private URI uri()
    {
        return URI.create("http://127.0.0.1:" + server.getPort() + "/mcp");
    }

    private static String call(String tool, String arguments)
    {
        return "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                        + "\",\"arguments\":" + arguments + "}}";
    }

    /** a batch of n pings, numbered from 1 */
    private static String batch(int n)
    {
        var messages = new StringBuilder("[");
        for (int ii = 1; ii <= n; ii++)
        {
            if (ii > 1)
                messages.append(",");
            messages.append("{\"jsonrpc\":\"2.0\",\"id\":").append(ii).append(",\"method\":\"ping\"}");
        }
        return messages.append("]").toString();
    }

    /** a tool call that asks for progress under that token */
    private static String callWithToken(String tool, String arguments, String token)
    {
        return "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                        + "\",\"arguments\":" + arguments + ",\"_meta\":{\"progressToken\":" + token + "}}}";
    }

    /** a new session's id */
    private String initialize() throws Exception
    {
        return rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}").headers()
                        .firstValue("Mcp-Session-Id").orElseThrow();
    }

    private static HttpRequest.Builder session(HttpRequest.Builder request, String id)
    {
        return request.header("Mcp-Session-Id", id);
    }

    /** a request as a Streamable HTTP client sends it, with the token, in the session if one is given */
    private HttpRequest.Builder streamRequest(String body, String sessionId)
    {
        var request = HttpRequest.newBuilder(uri()) //
                        .header("Authorization", "Bearer " + TOKEN) //
                        .header("Accept", "application/json, text/event-stream") //
                        .POST(HttpRequest.BodyPublishers.ofString(body));
        return sessionId != null ? session(request, sessionId) : request;
    }

    private HttpResponse<String> stream(String body) throws Exception
    {
        return send(streamRequest(body, null));
    }

    /** the JSON-RPC messages of an event stream, in order; comments are not events */
    private static List<JsonObject> events(String stream)
    {
        var events = new ArrayList<JsonObject>();
        for (var event : stream.split("\n\n"))
        {
            for (var line : event.split("\n"))
            {
                if (line.startsWith("data: "))
                    events.add(JsonParser.parseString(line.substring("data: ".length())).getAsJsonObject());
            }
        }
        return events;
    }

    private HttpResponse<String> rpc(String token, String body) throws Exception
    {
        var request = HttpRequest.newBuilder(uri()).POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null)
            request = request.header("Authorization", "Bearer " + token);
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception
    {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonElement body(HttpResponse<String> response)
    {
        return JsonParser.parseString(response.body());
    }

    private static String text(JsonObject result)
    {
        return result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
    }
}
