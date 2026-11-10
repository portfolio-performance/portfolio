package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

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

    @Before
    public void setUp() throws Exception
    {
        var client = new Client();
        new SecurityBuilder().addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(FILE, true);
        registry.setAlias(FILE, "sample");

        var host = new FakeHost(List.of(new FakeHost.FakeOpenFile(FILE, "mcp", client)));
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

    /** an SSE stream would park one of two worker threads; a client issued no session asks for neither */
    @Test
    public void testGetAndDeleteAreNotAllowed() throws Exception
    {
        assertThat(send(HttpRequest.newBuilder(uri()).header("Authorization", "Bearer " + TOKEN).GET()).statusCode(),
                        is(405));
        assertThat(send(HttpRequest.newBuilder(uri()).header("Authorization", "Bearer " + TOKEN).DELETE()).statusCode(),
                        is(405));
    }

    @Test
    public void testNoSessionIdIsEverIssued() throws Exception
    {
        var response = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");

        assertThat(response.headers().firstValue("Mcp-Session-Id").isPresent(), is(false));
        assertThat(response.headers().firstValue("Content-Type").orElseThrow(), containsString("application/json"));
    }

    /** the client sends this on every request and is perfectly happy with JSON */
    @Test
    public void testAnEventStreamIsNeverServed() throws Exception
    {
        var response = send(HttpRequest.newBuilder(uri()) //
                        .header("Accept", "text/event-stream, application/json") //
                        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}")));

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("Content-Type").orElseThrow(), containsString("application/json"));
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
        var response = rpc(null, "{not json");

        assertThat(response.statusCode(), is(400));
        assertThat(body(response).getAsJsonObject().getAsJsonObject("error").get("code").getAsInt(), is(-32700));
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
