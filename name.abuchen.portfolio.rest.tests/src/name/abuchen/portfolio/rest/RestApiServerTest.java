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

        server = new RestApiServer(0, () -> TOKEN, router);
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

    @Test
    public void testMissingTokenIs401() throws Exception
    {
        var response = http.send(request("/v1/ping").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(401));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), is("application/problem+json"));
        assertThat(response.headers().firstValue("WWW-Authenticate").orElse(""), is("Bearer"));
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
        try (var socket = new Socket(InetAddress.getLoopbackAddress(), server.getPort()))
        {
            var request = "GET /v1/ping HTTP/1.1\r\n" //
                            + (hostHeader != null ? "Host: " + hostHeader + "\r\n" : "") //
                            + "Authorization: Bearer " + TOKEN + "\r\n" //
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

    @Test
    public void testUnknownRouteIs404Problem() throws Exception
    {
        var response = http.send(request("/v1/nope").header("Authorization", "Bearer " + TOKEN).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(404));
        assertThat(response.body(), containsString("problems/not-found"));
    }
}
