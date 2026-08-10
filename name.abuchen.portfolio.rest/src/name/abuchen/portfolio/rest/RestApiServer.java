package name.abuchen.portfolio.rest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.ProblemJson;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;

/**
 * Loopback-only HTTP server hosting the REST API. Binds 127.0.0.1 on a fixed
 * port and never hops ports on bind failure. Requests must address the API as
 * loopback (Host header) and must not come from a browser context (Origin
 * header); all must present a bearer token - except the pairing endpoints,
 * whose purpose is to obtain one.
 */
public class RestApiServer
{
    /** 127.0.0.0/8, as a literal - a host name must never match */
    private static final Pattern IPV4_LOOPBACK = Pattern.compile("127(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}"); //$NON-NLS-1$

    private static final Set<String> IPV6_LOOPBACK = Set.of("::1", "0:0:0:0:0:0:0:1"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * The largest request body accepted. Handlers parse JSON, so a body is
     * buffered whole - without a limit any local process could make the
     * application allocate arbitrary amounts of memory, and the pairing
     * endpoints do not even require a token. A JSON merge patch of a portfolio
     * entity is orders of magnitude smaller than this.
     */
    private static final int MAX_REQUEST_BODY = 1024 * 1024;

    private final int port;
    private final Predicate<String> tokenValidator;
    private final Router router;

    private HttpServer server;
    private ExecutorService executor;

    public RestApiServer(int port, Predicate<String> tokenValidator, Router router)
    {
        this.port = port;
        this.tokenValidator = tokenValidator;
        this.router = router;
    }

    public void start() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::dispatch); //$NON-NLS-1$
        executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
    }

    public void stop()
    {
        if (server != null)
        {
            server.stop(0);
            executor.shutdown();
            server = null;
        }
    }

    public int getPort()
    {
        return server != null ? server.getAddress().getPort() : port;
    }

    private void dispatch(HttpExchange exchange)
    {
        try
        {
            Response response;
            try
            {
                checkHost(exchange);
                checkOrigin(exchange);
                checkAuthorization(exchange);

                var match = router.match(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
                var request = new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                                match.pathParams(), Request.parseQuery(exchange.getRequestURI().getRawQuery()),
                                readBody(exchange));
                response = match.handler().handle(request);
            }
            catch (ApiException e)
            {
                response = problem(e);
            }
            catch (Exception e)
            {
                PortfolioLog.error(e);
                response = problem(new ApiException(500, "internal-error", "Internal error")); //$NON-NLS-1$ //$NON-NLS-2$
            }

            write(exchange, response);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }
        finally
        {
            exchange.close();
        }
    }

    /**
     * Reads the body, but never more than {@link #MAX_REQUEST_BODY}. Reading one
     * byte past the limit is how an oversized body announces itself: the
     * Content-Length header is not trusted, as it need not match what is sent.
     */
    private static byte[] readBody(HttpExchange exchange) throws IOException
    {
        var body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BODY + 1);
        if (body.length > MAX_REQUEST_BODY)
            throw ApiException.requestTooLarge(MAX_REQUEST_BODY);
        return body;
    }

    /**
     * Rejects requests that do not address the API as loopback. Without this, a
     * website could defeat the Origin check via DNS rebinding: it points its own
     * domain at 127.0.0.1, at which point the browser considers the API
     * same-origin and sends no Origin header at all. The host name the browser
     * used, however, remains the attacker's domain and is carried in the Host
     * header - so only literal loopback authorities are accepted.
     */
    private void checkHost(HttpExchange exchange)
    {
        var host = exchange.getRequestHeaders().getFirst("Host"); //$NON-NLS-1$
        if (host == null || !isLoopbackAuthority(host))
            throw ApiException.forbiddenHost();
    }

    private static boolean isLoopbackAuthority(String host)
    {
        var name = host;

        if (name.startsWith("[")) //$NON-NLS-1$
        {
            // IPv6 literal, e.g. [::1]:5712
            var end = name.indexOf(']');
            if (end < 0)
                return false;
            name = name.substring(1, end);
        }
        else
        {
            var colon = name.indexOf(':');
            if (colon >= 0)
                name = name.substring(0, colon);
        }

        // the port is deliberately not checked: an attacker cannot get a browser
        // to send a loopback host name for a page served from their domain

        if ("localhost".equalsIgnoreCase(name)) //$NON-NLS-1$
            return true;

        return IPV6_LOOPBACK.contains(name) || IPV4_LOOPBACK.matcher(name).matches();
    }

    private void checkOrigin(HttpExchange exchange)
    {
        if (exchange.getRequestHeaders().getFirst("Origin") != null) //$NON-NLS-1$
            throw ApiException.forbiddenOrigin();
    }

    private void checkAuthorization(HttpExchange exchange)
    {
        if (isAuthExempt(exchange.getRequestURI().getPath()))
            return;

        var header = exchange.getRequestHeaders().getFirst("Authorization"); //$NON-NLS-1$
        if (header == null || !header.startsWith("Bearer ")) //$NON-NLS-1$
            throw ApiException.unauthorized();

        if (!tokenValidator.test(header.substring("Bearer ".length()))) //$NON-NLS-1$
            throw ApiException.unauthorized();
    }

    /**
     * The pairing endpoints exist to obtain a token, and the OpenAPI document
     * describes how; both are reachable without one. Host and Origin checks
     * still apply.
     */
    private static boolean isAuthExempt(String path)
    {
        return path.equals(RestApiConstants.PAIRING_ENDPOINT)
                        || path.startsWith(RestApiConstants.PAIRING_ENDPOINT + "/") //$NON-NLS-1$
                        || path.equals(RestApiConstants.OPENAPI_ENDPOINT);
    }

    private static Response problem(ApiException exception)
    {
        var body = ProblemJson.toJson(exception).toString().getBytes(StandardCharsets.UTF_8);
        return new Response(exception.getStatus(), ProblemJson.CONTENT_TYPE, body, exception.getHeaders());
    }

    private static void write(HttpExchange exchange, Response response) throws IOException
    {
        for (var header : response.headers().entrySet())
            exchange.getResponseHeaders().set(header.getKey(), header.getValue());
        if (response.contentType() != null)
            exchange.getResponseHeaders().set("Content-Type", response.contentType()); //$NON-NLS-1$

        var length = response.body().length == 0 ? -1 : response.body().length;
        exchange.sendResponseHeaders(response.status(), length);
        if (response.body().length > 0)
        {
            try (var out = exchange.getResponseBody())
            {
                out.write(response.body());
            }
        }
    }
}
