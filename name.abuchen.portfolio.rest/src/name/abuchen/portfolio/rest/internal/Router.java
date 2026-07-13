package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Router
{
    public interface Handler
    {
        Response handle(Request request) throws Exception;
    }

    public record Match(Handler handler, Map<String, String> pathParams)
    {
    }

    private record Route(String method, String[] segments, Handler handler)
    {
    }

    private final List<Route> routes = new ArrayList<>();

    public void add(String method, String pattern, Handler handler)
    {
        routes.add(new Route(method, split(pattern), handler));
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
