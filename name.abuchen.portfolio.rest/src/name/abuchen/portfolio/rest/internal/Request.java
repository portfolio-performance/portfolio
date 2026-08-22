package name.abuchen.portfolio.rest.internal;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
     * <p/>
     * A name given more than once is rejected rather than reduced to one of
     * its values: {@code ?metrics=risk&metrics=gains} would otherwise be
     * answered as if only {@code gains} had been asked for. This is the only
     * place that still sees every occurrence, so the check lives here.
     */
    public static Map<String, String> parseQuery(String rawQuery)
    {
        if (rawQuery == null || rawQuery.isEmpty())
            return Map.of();

        var params = new HashMap<String, String>();
        var counts = new TreeMap<String, Integer>();
        for (String pair : rawQuery.split("&")) //$NON-NLS-1$
        {
            if (pair.isEmpty())
                continue;
            int idx = pair.indexOf('=');
            var name = URLDecoder.decode(idx < 0 ? pair : pair.substring(0, idx), StandardCharsets.UTF_8);
            var value = idx < 0 ? "" : pair.substring(idx + 1); //$NON-NLS-1$
            params.put(name, URLDecoder.decode(value, StandardCharsets.UTF_8));
            counts.merge(name, 1, Integer::sum);
        }

        // sorted by name, so that the response is stable
        var duplicates = counts.entrySet().stream() //
                        .filter(entry -> entry.getValue() > 1) //
                        .map(entry -> new ApiException.FieldError(entry.getKey(), "duplicate-parameter", //$NON-NLS-1$
                                        explainRepeat(entry.getKey(), entry.getValue())))
                        .toList();
        if (!duplicates.isEmpty())
            throw ApiException.badRequest(duplicates);

        return Map.copyOf(params);
    }

    /** points at the list syntax, the usual reason for a repeat */
    private static String explainRepeat(String name, int count)
    {
        return "query parameter '" + name + "' is given " + count + " times; give it once" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + " - a list is one comma-separated value, e.g. metrics=risk,gains"; //$NON-NLS-1$
    }
}
