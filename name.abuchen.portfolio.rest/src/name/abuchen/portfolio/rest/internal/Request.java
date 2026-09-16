package name.abuchen.portfolio.rest.internal;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record Request(String method, String path, Map<String, String> pathParams, Map<String, String> queryParams,
                byte[] body)
{
    public Request(String method, String path, Map<String, String> pathParams, byte[] body)
    {
        this(method, path, pathParams, Map.of(), body);
    }

    public String pathParam(String name)
    {
        return pathParams.get(name);
    }

    /** the decoded query parameter, or null if absent */
    public String queryParam(String name)
    {
        return queryParams.get(name);
    }

    /**
     * Parses a raw (still percent-encoded) query string as handed out by
     * {@code URI#getRawQuery}; a null or empty query yields an empty map, a
     * key without {@code =} an empty value.
     */
    public static Map<String, String> parseQuery(String rawQuery)
    {
        if (rawQuery == null || rawQuery.isEmpty())
            return Map.of();

        var params = new HashMap<String, String>();
        for (String pair : rawQuery.split("&")) //$NON-NLS-1$
        {
            if (pair.isEmpty())
                continue;
            int idx = pair.indexOf('=');
            var name = idx < 0 ? pair : pair.substring(0, idx);
            var value = idx < 0 ? "" : pair.substring(idx + 1); //$NON-NLS-1$
            params.put(URLDecoder.decode(name, StandardCharsets.UTF_8),
                            URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return Map.copyOf(params);
    }
}
