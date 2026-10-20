package name.abuchen.portfolio.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Tests {@link WebAccess} against a local HTTP server: connections are reused
 * across requests, while headers, user agent and cookies stay per request.
 */
@SuppressWarnings("nls")
public class WebAccessTest
{
    private record RecordedRequest(String method, String path, int remotePort, String userAgent, String testHeader,
                    String cookie, String body)
    {
    }

    private HttpServer server;
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    @Before
    public void startServer() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        server.createContext("/echo", exchange -> {
            record(exchange);
            respond(exchange, 200, "ok");
        });

        server.createContext("/set-cookie", exchange -> {
            record(exchange);
            exchange.getResponseHeaders().add("Set-Cookie", "session=first; Path=/");
            respond(exchange, 200, "cookie set");
        });

        server.createContext("/redirect", exchange -> {
            record(exchange);
            exchange.getResponseHeaders().add("Set-Cookie", "session=redirect; Path=/");
            exchange.getResponseHeaders().add("Location", "/echo");
            respond(exchange, 302, "");
        });

        server.createContext("/missing", exchange -> {
            record(exchange);
            respond(exchange, 404, "not found");
        });

        server.start();
    }

    @After
    public void stopServer()
    {
        server.stop(0);
    }

    private void record(HttpExchange exchange) throws IOException
    {
        var headers = exchange.getRequestHeaders();
        var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        requests.add(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRemoteAddress().getPort(), headers.getFirst("User-Agent"),
                        headers.getFirst("X-Test"), headers.getFirst("Cookie"), body));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException
    {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0)
        {
            try (var out = exchange.getResponseBody())
            {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    private WebAccess webAccess(String path)
    {
        return new WebAccess(server.getAddress().getHostString(), path) //
                        .withScheme("http") //
                        .withPort(server.getAddress().getPort());
    }

    @Test
    public void testConnectionIsReusedAcrossRequests() throws IOException
    {
        assertThat(webAccess("/echo").get(), is("ok"));
        assertThat(webAccess("/echo").get(), is("ok"));
        assertThat(webAccess("/echo").get(), is("ok"));

        assertThat(requests, hasSize(3));

        // same client port = same TCP connection
        var port = requests.get(0).remotePort();
        assertThat(requests.get(1).remotePort(), is(port));
        assertThat(requests.get(2).remotePort(), is(port));
    }

    @Test
    public void testHeadersAndUserAgentAreAppliedPerRequest() throws IOException
    {
        webAccess("/echo").addHeader("X-Test", "first").addUserAgent("TestAgent/1.0").get();
        webAccess("/echo").get();

        assertThat(requests, hasSize(2));

        assertThat(requests.get(0).testHeader(), is("first"));
        assertThat(requests.get(0).userAgent(), is("TestAgent/1.0"));

        assertThat(requests.get(1).testHeader(), is(nullValue()));
        assertThat(requests.get(1).userAgent(), is(OnlineHelper.getUserAgent()));
    }

    @Test
    public void testExplicitUserAgentHeaderTakesPrecedence() throws IOException
    {
        webAccess("/echo").addHeader("User-Agent", "HeaderAgent/1.0").addUserAgent("TestAgent/1.0").get();

        assertThat(requests, hasSize(1));
        assertThat(requests.get(0).userAgent(), is("HeaderAgent/1.0"));
    }

    @Test
    public void testCookiesAreNotSharedBetweenRequests() throws IOException
    {
        webAccess("/set-cookie").get();
        webAccess("/echo").get();

        assertThat(requests, hasSize(2));
        assertThat(requests.get(1).cookie(), is(nullValue()));
    }

    @Test
    public void testCookiesAreKeptWithinRedirect() throws IOException
    {
        assertThat(webAccess("/redirect").addHeader("X-Test", "redirect").get(), is("ok"));

        assertThat(requests, hasSize(2));
        assertThat(requests.get(1).path(), is("/echo"));
        assertThat(requests.get(1).cookie(), is("session=redirect"));
        assertThat(requests.get(1).testHeader(), is("redirect"));
    }

    @Test
    public void testErrorStatusIsReportedAndConnectionIsReleased() throws IOException
    {
        try
        {
            webAccess("/missing").get();
            fail("expected WebAccessException");
        }
        catch (WebAccess.WebAccessException e)
        {
            assertThat(e.getHttpErrorCode(), is(404));
            assertThat(e.getBody(), is("not found"));
        }

        assertThat(webAccess("/echo").get(), is("ok"));

        assertThat(requests, hasSize(2));
        assertThat(requests.get(1).remotePort(), is(requests.get(0).remotePort()));
    }

    @Test
    public void testPost() throws IOException
    {
        webAccess("/echo").post("{\"value\":1}");

        assertThat(requests, hasSize(1));
        assertThat(requests.get(0).method(), is("POST"));
        assertThat(requests.get(0).body(), is("{\"value\":1}"));
    }
}
