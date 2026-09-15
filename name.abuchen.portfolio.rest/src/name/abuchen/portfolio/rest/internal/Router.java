package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Router
{
    public interface Handler
    {
        Response handle(Request request) throws Exception;
    }

    public record Match(Handler handler, Map<String, String> pathParams)
    {
    }

    private record Route(String method, String[] segments, Set<String> queryParams, Handler handler)
    {
    }

    private final List<Route> routes = new ArrayList<>();

    /**
     * Registers a route and rejects query parameters not declared for it before
     * invoking the handler.
     */
    public void add(String method, String pattern, Handler handler, String... queryParams)
    {
        // declaration order is the specification's order, which reads better in
        // the error message than an alphabetical list would
        var permitted = new LinkedHashSet<>(List.of(queryParams));

        routes.add(new Route(method, split(pattern), permitted, request -> {
            rejectUnknownQueryParams(request, permitted);
            return handler.handle(request);
        }));
    }

    public Match match(String method, String path)
    {
        var segments = split(path);
        var pathMatched = false;

        for (Route route : routes)
        {
            var params = matchSegments(route.segments(), segments);
            if (params == null)
                continue;
            pathMatched = true;
            if (route.method().equals(method))
                return new Match(route.handler(), params);
        }

        if (pathMatched)
            throw new ApiException(405, "method-not-allowed", "Method not allowed"); //$NON-NLS-1$ //$NON-NLS-2$
        throw ApiException.notFound();
    }

    /**
     * The registered routes as "METHOD /pattern" signatures, e.g.
     * {@code GET /v1/files/{file}/instruments/{uuid}}. Used by the OpenAPI drift
     * test to keep the routing table and the specification in lockstep.
     */
    public List<String> routeSignatures()
    {
        return routes.stream() //
                        .map(Router::signatureOf) //
                        .toList();
    }

    /**
     * The query parameters each route permits, keyed like {@link #routeSignatures()}.
     */
    public Map<String, Set<String>> permittedQueryParameters()
    {
        var result = new LinkedHashMap<String, Set<String>>();
        for (Route route : routes)
            result.put(signatureOf(route), route.queryParams());
        return result;
    }

    /** Reports all unknown parameters and includes the permitted names. */
    private static void rejectUnknownQueryParams(Request request, Set<String> permitted)
    {
        var unknown = request.queryParams().keySet().stream() //
                        .filter(name -> !permitted.contains(name)) //
                        .sorted() // parseQuery does not preserve order; the response should be stable
                        .toList();

        if (unknown.isEmpty())
            return;

        throw ApiException.badRequest(unknown.stream() //
                        .map(name -> new ApiException.FieldError(name, "unknown-parameter", //$NON-NLS-1$
                                        explain(name, permitted)))
                        .toList());
    }

    /** Builds the client-facing message for an unknown parameter. */
    private static String explain(String name, Set<String> permitted)
    {
        var accepted = permitted.isEmpty() ? "this endpoint accepts no query parameters" //$NON-NLS-1$
                        : "this endpoint accepts: " + String.join(", ", permitted); //$NON-NLS-1$ //$NON-NLS-2$

        var casing = permitted.stream().filter(name::equalsIgnoreCase).findFirst()
                        .map(match -> "parameter names are case-sensitive, did you mean '" + match + "'? ") //$NON-NLS-1$ //$NON-NLS-2$
                        .orElse(""); //$NON-NLS-1$

        return "unknown query parameter '" + name + "'; " + casing + accepted; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String signatureOf(Route route)
    {
        return route.method() + " /" + String.join("/", route.segments()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static Map<String, String> matchSegments(String[] pattern, String[] actual)
    {
        if (pattern.length != actual.length)
            return null;

        var params = new HashMap<String, String>();
        for (int ii = 0; ii < pattern.length; ii++)
        {
            var expected = pattern[ii];
            if (expected.startsWith("{") && expected.endsWith("}")) //$NON-NLS-1$ //$NON-NLS-2$
                params.put(expected.substring(1, expected.length() - 1), actual[ii]);
            else if (!expected.equals(actual[ii]))
                return null;
        }
        return params;
    }

    private static String[] split(String path)
    {
        return path.replaceAll("^/+|/+$", "").split("/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
