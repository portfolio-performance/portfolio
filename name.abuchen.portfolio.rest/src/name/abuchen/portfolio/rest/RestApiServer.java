package name.abuchen.portfolio.rest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
 * Loopback-only HTTP server hosting the REST API and the MCP endpoint. Binds
 * 127.0.0.1 on a fixed port and never hops ports on bind failure. Requests must
 * address the API as loopback (Host header) and must not come from a browser
 * context (Origin header); all must present a bearer token - except the pairing
 * endpoints, whose purpose is to obtain one, and the MCP endpoint, which
 * decides per JSON-RPC method. One server, one port, one switch: a second
 * toggle would advertise a boundary that does not exist, since the sixteen MCP
 * tools <em>are</em> the REST operations.
 */
public class RestApiServer
{
    /** 127.0.0.0/8, as a literal - a host name must never match */
    private static final Pattern IPV4_LOOPBACK = Pattern.compile("127(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}"); //$NON-NLS-1$

    private static final Set<String> IPV6_LOOPBACK = Set.of("::1", "0:0:0:0:0:0:0:1"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * How long one exchange may take before the watchdog closes it. A connection
     * that sends headers and then stalls cannot be left parked: {@code
     * com.sun.net.httpserver} has no read timeout, so without a deadline the body
     * read blocks until EOF - for a stalled client never - and every worker with
     * it. Closing the exchange closes the socket, which is what unblocks the read.
     */
    private static final int REQUEST_DEADLINE_SECONDS = 30;

    /** the bearer scheme, matched case-insensitively below */
    private static final String BEARER_PREFIX = "Bearer "; //$NON-NLS-1$

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
    private ScheduledExecutorService watchdog;

    /** daemon threads, so a restart or an app shutdown never leaves a worker parked */
    private static final ThreadFactory DAEMON = new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable runnable)
        {
            var thread = new Thread(runnable, "Portfolio Performance REST API"); //$NON-NLS-1$
            thread.setDaemon(true);
            return thread;
        }
    };

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
        // one virtual thread per request: a stalled or slow exchange cannot park a
        // pooled worker and take the whole API down with it. Virtual threads do not
        // keep the JVM alive, so a shutdown needs no interruption of them; the
        // watchdog below is a daemon platform thread, which does.
        executor = Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().name("Portfolio Performance REST API").factory()); //$NON-NLS-1$
        watchdog = Executors.newSingleThreadScheduledExecutor(DAEMON);
        server.setExecutor(executor);
        server.start();
    }

    public void stop()
    {
        if (server != null)
        {
            server.stop(0);
            // interrupt, do not wait: a thread blocked in a read or in the UI
            // thread must not outlive the server
            executor.shutdownNow();
            watchdog.shutdownNow();
            server = null;
        }
    }

    public int getPort()
    {
        return server != null ? server.getAddress().getPort() : port;
    }

    private void dispatch(HttpExchange exchange)
    {
        var deadline = watchdog.schedule(() -> exchange.close(), REQUEST_DEADLINE_SECONDS, TimeUnit.SECONDS);

        try
        {
            Response response;
            try
            {
                checkHost(exchange);
                checkOrigin(exchange);
                // the normalized path is what routing sees, so it is also what the
                // auth exemption must compare against - /mcp/ routes to the MCP
                // handler and would otherwise be refused before it can negotiate
                var path = normalizePath(exchange.getRequestURI().getPath());
                var authorization = checkAuthorization(exchange, path);

                var match = router.match(exchange.getRequestMethod(), path);
                var request = new Request(exchange.getRequestMethod(), path, match.pathParams(),
                                Request.parseQuery(exchange.getRequestURI().getRawQuery()), readBody(exchange),
                                authorization, exchange.getRequestHeaders().getFirst("User-Agent")); //$NON-NLS-1$
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
            deadline.cancel(false);
            exchange.close();
        }
    }

    /**
     * The path as the router matches it: a trailing slash is not a second path,
     * and any number of leading slashes is one path. Both doors must agree about
     * that or a client with a trailing slash is refused a token-free handshake it
     * would then be allowed to make. An empty middle segment stays unmatched - a
     * malformed path is not a route someone registered.
     */
    private static String normalizePath(String path)
    {
        var leading = path.replaceAll("^/+", "/"); //$NON-NLS-1$ //$NON-NLS-2$
        var stripped = leading.replaceAll("/+$", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return stripped.isEmpty() ? "/" : stripped; //$NON-NLS-1$
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

    /**
     * Decides whether the caller may proceed, and hands the handler what it
     * needs to decide the rest.
     * <p/>
     * Every {@code /v1} route is refused outright without a valid token. The
     * MCP endpoint is neither refused nor exempt: ADR 0005 decides it per
     * JSON-RPC method inside the handler, so all that is settled here is what
     * the caller presented, which travels on the {@link Request}.
     * <p/>
     * Which is why no {@code /mcp} request is recorded as a rejected connection
     * here either. A token-free {@code initialize} is answered, not refused,
     * and only the handler knows whether the call needed a token.
     */
    private Request.Authorization checkAuthorization(HttpExchange exchange, String path)
    {
        var header = exchange.getRequestHeaders().getFirst("Authorization"); //$NON-NLS-1$

        // the scheme is case-insensitive per RFC 6750, and a header in the wrong
        // shape is a different fault from no header at all - the preference page
        // says "no token offered" for both otherwise, which is a misdiagnosis
        var bearer = header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
        var token = bearer ? header.substring(BEARER_PREFIX.length()).trim() : null;

        if (token != null && tokenValidator.test(token))
            return Request.Authorization.VALID;

        var authorization = token != null ? Request.Authorization.INVALID : Request.Authorization.MISSING;

        if (isAuthExempt(path) || RestApiConstants.MCP_ENDPOINT.equals(path))
            return authorization;

        // a wrong token never becomes an entry in the client list, so this
        // record is the only trace the user will have of it
        RejectedConnections.record(path, exchange.getRequestHeaders().getFirst("User-Agent"), //$NON-NLS-1$
                token != null, token != null ? RejectedConnections.Reason.INVALID_TOKEN
                        : header != null ? RejectedConnections.Reason.NO_BEARER_SCHEME
                                : RejectedConnections.Reason.NO_HEADER);

        throw ApiException.unauthorized();
    }

    /**
     * The pairing endpoints exist to obtain a token, and the OpenAPI document
     * describes how; both are reachable without one. So is the contract
     * version, which a client needs before it can judge whether pairing is even
     * worth attempting.
     * <p/>
     * {@code /.well-known/*} is exempt for a different reason: an MCP client
     * with no credential asks for OAuth metadata in seven placements, and the
     * honest answer to all seven is a 404 rather than a 401, which says
     * something else entirely. Host and Origin checks still apply to all.
     */
    private static boolean isAuthExempt(String path)
    {
        return path.equals(RestApiConstants.PAIRING_ENDPOINT)
                        || path.startsWith(RestApiConstants.PAIRING_ENDPOINT + "/") //$NON-NLS-1$
                        || path.equals(RestApiConstants.OPENAPI_ENDPOINT)
                        || path.equals(RestApiConstants.VERSION_ENDPOINT)
                        || path.startsWith(RestApiConstants.WELL_KNOWN_PREFIX);
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

        // 0, not -1: -1 asks for chunked transfer encoding, and a 204 or a 202
        // with no body should carry no body at all, not an empty chunked stream
        var length = response.body().length;
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
