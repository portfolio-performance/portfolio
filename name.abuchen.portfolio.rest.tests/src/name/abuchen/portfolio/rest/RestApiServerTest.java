package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;

@SuppressWarnings("nls")
public class RestApiServerTest
{
    private static final String TOKEN = "test-token";

    private RestApiServer server;
    private HttpClient http;

    @Before
    public void setUp() throws Exception
    {
        var router = new Router();
        router.add("GET", "/v1/ping", request -> Response.json(200, JsonParser.parseString("{\"pong\":true}")));
        router.add("GET", "/v1/echo", request -> Response.json(200,
                        JsonParser.parseString("{\"name\":\"" + request.queryParam("name") + "\"}")), "name");
        router.add("GET", "/v1/openapi.yaml",
                        request -> Response.of(200, "application/yaml", "openapi: 3.1.0\n".getBytes(StandardCharsets.UTF_8)));
        router.add("POST", "/v1/auth/requests",
                        request -> Response.json(201, JsonParser.parseString("{\"status\":\"pending\"}")));
        router.add("GET", "/v1/auth/requests/{id}",
                        request -> Response.json(200, JsonParser.parseString("{\"status\":\"pending\"}")));

        server = new RestApiServer(0, TOKEN::equals, router);
        server.start();
        http = HttpClient.newHttpClient();
    }

    @After
    public void tearDown()
    {
        server.stop();
    }

    private HttpRequest.Builder request(String path)
    {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + path));
    }

    @Test
    public void testAuthorizedRequestSucceeds() throws Exception
    {
        var response = http.send(request("/v1/ping").header("Authorization", "Bearer " + TOKEN).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));
        assertThat(response.body(), containsString("pong"));
    }

    /**
     * The server must hand the (decoded) query string through to the handler -
     * the path-only routing must not swallow it.
     */
    @Test
    public void testQueryStringReachesHandlerDecoded() throws Exception
    {
        var response = http.send(request("/v1/echo?name=a%20b").header("Authorization", "Bearer " + TOKEN).GET()
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));
        assertThat(response.body(), containsString("a b"));
    }

    /**
     * The strict query-parameter check, seen the way a client sees it: the
     * unknown name comes back as problem+json over the wire, not as a 200 the
     * caller has to second-guess.
     */
    @Test
    public void testUnknownQueryParameterIs400Problem() throws Exception
    {
        var response = http.send(request("/v1/echo?nickname=a").header("Authorization", "Bearer " + TOKEN).GET()
                        .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(400));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), is("application/problem+json"));
        assertThat(response.body(), containsString("invalid-request"));
        assertThat(response.body(), containsString("unknown-parameter"));
        assertThat(response.body(), containsString("nickname"));
    }

    @Test
    public void testMissingTokenIs401() throws Exception
    {
        var response = http.send(request("/v1/ping").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(401));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), is("application/problem+json"));
        assertThat(response.headers().firstValue("WWW-Authenticate").orElse(""), is("Bearer"));
    }

    /** the 401 tells the client where to pair (self-serve discovery) */
    @Test
    public void test401BodyCarriesThePairingEndpoint() throws Exception
    {
        var response = http.send(request("/v1/ping").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(401));
        assertThat(response.body(), containsString("\"pairingEndpoint\":\"/v1/auth/requests\""));
    }

    /** the OpenAPI document is reachable without a token, so the API is self-describing */
    @Test
    public void testOpenApiEndpointIsExemptFromBearerAuth() throws Exception
    {
        var response = http.send(request("/v1/openapi.yaml").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), is("application/yaml"));
        assertThat(response.body(), containsString("openapi:"));
    }

    /** pairing endpoints are reachable without a token - that is their point */
    @Test
    public void testPairingEndpointsAreExemptFromBearerAuth() throws Exception
    {
        var post = http.send(request("/v1/auth/requests")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"clientName\":\"x\"}")).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(post.statusCode(), is(201));

        var get = http.send(request("/v1/auth/requests/some-id").GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(get.statusCode(), is(200));
    }

    /** ...but they remain behind the browser (Origin) and rebinding (Host) checks */
    @Test
    public void testPairingEndpointsStillRejectOriginHeader() throws Exception
    {
        var response = http.send(request("/v1/auth/requests").header("Origin", "https://evil.example")
                        .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(403));
    }

    @Test
    public void testPairingEndpointsStillRejectRebindingHost() throws Exception
    {
        assertThat(sendRaw("POST", "/v1/auth/requests", "evil.example:" + server.getPort()), is(403));
    }

    @Test
    public void testWrongTokenIs401() throws Exception
    {
        var response = http.send(request("/v1/ping").header("Authorization", "Bearer nope").GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(401));
    }

    @Test
    public void testOriginHeaderIs403EvenWithValidToken() throws Exception
    {
        var response = http.send(
                        request("/v1/ping").header("Authorization", "Bearer " + TOKEN)
                                        .header("Origin", "https://evil.example").GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(403));
    }

    /**
     * Sends a request with a hand-crafted Host header; HttpClient does not allow
     * setting it. Returns the status code of the response.
     */
    private int sendWithHost(String hostHeader) throws Exception
    {
        return sendRaw("GET", "/v1/ping", hostHeader);
    }

    private int sendRaw(String method, String path, String hostHeader) throws Exception
    {
        try (var socket = new Socket(InetAddress.getLoopbackAddress(), server.getPort()))
        {
            var request = method + " " + path + " HTTP/1.1\r\n" //
                            + (hostHeader != null ? "Host: " + hostHeader + "\r\n" : "") //
                            + "Authorization: Bearer " + TOKEN + "\r\n" //
                            + "Content-Length: 0\r\n" //
                            + "Connection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();

            var reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            var statusLine = reader.readLine();
            return Integer.parseInt(statusLine.split(" ")[1]);
        }
    }

    @Test
    public void testRebindingHostIs403EvenWithValidTokenAndNoOrigin() throws Exception
    {
        // DNS rebinding: the browser thinks it is same-origin and sends no
        // Origin header, but the Host header still names the attacker's domain
        assertThat(sendWithHost("evil.example:" + server.getPort()), is(403));
    }

    @Test
    public void testLoopbackHostsAreAccepted() throws Exception
    {
        assertThat(sendWithHost("127.0.0.1:" + server.getPort()), is(200));
        assertThat(sendWithHost("localhost:" + server.getPort()), is(200));
        assertThat(sendWithHost("[::1]:" + server.getPort()), is(200));
    }

    @Test
    public void testLoopbackAddressesFromTheWholeRangeAreAccepted() throws Exception
    {
        assertThat(sendWithHost("127.255.255.254:" + server.getPort()), is(200));
        assertThat(sendWithHost("127.1.2.3:" + server.getPort()), is(200));
    }

    /** only real 127.0.0.0/8 literals - anything else is a host name */
    @Test
    public void testHostWithOutOfRangeOctetIs403() throws Exception
    {
        assertThat(sendWithHost("127.300.1.1:" + server.getPort()), is(403));
        assertThat(sendWithHost("127.0.0.256:" + server.getPort()), is(403));
    }

    @Test
    public void testHostThatMerelyContainsLoopbackIs403() throws Exception
    {
        assertThat(sendWithHost("127.0.0.1.evil.example:" + server.getPort()), is(403));
        assertThat(sendWithHost("localhost.evil.example:" + server.getPort()), is(403));
    }

    @Test
    public void testMissingHostIs403() throws Exception
    {
        assertThat(sendWithHost(null), is(403));
    }

    /** an unbounded read would let any local process exhaust the heap */
    @Test
    public void testOversizedRequestBodyIs413Problem() throws Exception
    {
        var response = http.send(request("/v1/auth/requests")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[2 * 1024 * 1024])).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(413));
        assertThat(response.body(), containsString("problems/request-too-large"));
    }

    /** ...but a body up to the limit is passed through untouched */
    @Test
    public void testBodyAtTheLimitIsAccepted() throws Exception
    {
        var response = http.send(request("/v1/auth/requests")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[1024 * 1024])).build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(201));
    }

    @Test
    public void testUnknownRouteIs404Problem() throws Exception
    {
        var response = http.send(request("/v1/nope").header("Authorization", "Bearer " + TOKEN).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(404));
        assertThat(response.body(), containsString("problems/not-found"));
    }
}
