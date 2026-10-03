package name.abuchen.portfolio.rest.internal;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.spi.OpenFile;

/**
 * What every write handler needs besides the request body: the addressed
 * file, whether the request is a dry run ({@code ?dry_run=true}), and the
 * client's idempotency key ({@code clientRef} body field), if any.
 */
public record WriteContext(OpenFile file, boolean dryRun, String clientRef)
{
    public static final String DRY_RUN_PARAMETER = "dry_run"; //$NON-NLS-1$
    public static final String CLIENT_REF_FIELD = "clientRef"; //$NON-NLS-1$
    public static final int CLIENT_REF_MAX_LENGTH = 128;

    public static WriteContext of(OpenFile file, Request request)
    {
        return new WriteContext(file, isDryRun(request), clientRef(request));
    }

    public Client client()
    {
        return file.getClient();
    }

    /**
     * {@code ?dry_run=true} requests a dry run; absent or {@code false} a real
     * one. Any other value is rejected rather than silently treated as a real
     * write.
     */
    public static boolean isDryRun(Request request)
    {
        var value = request.queryParam(DRY_RUN_PARAMETER);
        if (value == null || "false".equals(value)) //$NON-NLS-1$
            return false;
        if ("true".equals(value)) //$NON-NLS-1$
            return true;
        throw ApiException.badRequest(List.of(new ApiException.FieldError(DRY_RUN_PARAMETER, "invalid-value", //$NON-NLS-1$
                        MessageFormat.format("dry_run must be true or false, got {0}", value)))); //$NON-NLS-1$
    }

    /**
     * The {@code clientRef} of the body, or null if the body has none. A body
     * that is not a JSON object is left to the handler to reject.
     */
    private static String clientRef(Request request)
    {
        if (request.body() == null || request.body().length == 0)
            return null;

        try
        {
            var element = JsonParser.parseString(new String(request.body(), StandardCharsets.UTF_8));
            if (!element.isJsonObject())
                return null;

            var json = new Json(element.getAsJsonObject());
            var clientRef = json.optString(CLIENT_REF_FIELD, CLIENT_REF_MAX_LENGTH);
            if (clientRef != null && clientRef.isBlank())
                json.add(new ApiException.FieldError(CLIENT_REF_FIELD, "invalid-value", "clientRef must not be blank")); //$NON-NLS-1$ //$NON-NLS-2$
            json.throwIfErrors();
            return clientRef;
        }
        catch (JsonParseException e)
        {
            return null;
        }
    }
}
