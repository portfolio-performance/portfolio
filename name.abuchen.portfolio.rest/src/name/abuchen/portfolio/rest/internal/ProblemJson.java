package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Serializes ApiException to RFC 9457 problem+json.
 */
public final class ProblemJson
{
    public static final String CONTENT_TYPE = "application/problem+json"; //$NON-NLS-1$
    private static final String TYPE_PREFIX = "https://portfolio-performance.info/rest/problems/"; //$NON-NLS-1$

    private ProblemJson()
    {
    }

    public static JsonObject toJson(ApiException exception)
    {
        var json = new JsonObject();
        json.addProperty("type", TYPE_PREFIX + exception.getType()); //$NON-NLS-1$
        json.addProperty("title", exception.getMessage()); //$NON-NLS-1$
        json.addProperty("status", exception.getStatus()); //$NON-NLS-1$

        if (exception.getDetail() != null)
            json.addProperty("detail", exception.getDetail()); //$NON-NLS-1$

        if (!exception.getErrors().isEmpty())
        {
            var errors = new JsonArray();
            for (var error : exception.getErrors())
            {
                var e = new JsonObject();
                e.addProperty("field", error.field()); //$NON-NLS-1$
                e.addProperty("code", error.code()); //$NON-NLS-1$
                e.addProperty("message", error.message()); //$NON-NLS-1$
                errors.add(e);
            }
            json.add("errors", errors); //$NON-NLS-1$
        }

        return json;
    }
}
