package name.abuchen.portfolio.rest.internal;

import java.util.List;
import java.util.Map;

/**
 * An error to be rendered as RFC 9457 problem+json. Per RFC 9457 §3.1.4 the
 * title is invariant for a given type - anything specific to the one occurrence
 * (a file path, the offending value) belongs in the detail.
 */
public class ApiException extends RuntimeException
{
    public record FieldError(String field, String code, String message)
    {
    }

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String type;
    private final transient String detail;
    private final transient List<FieldError> errors;
    private final transient Map<String, String> headers;

    public ApiException(int status, String type, String title)
    {
        this(status, type, title, null, List.of(), Map.of());
    }

    public ApiException(int status, String type, String title, String detail, List<FieldError> errors,
                    Map<String, String> headers)
    {
        super(title);
        this.status = status;
        this.type = type;
        this.detail = detail;
        this.errors = List.copyOf(errors);
        this.headers = Map.copyOf(headers);
    }

    public static ApiException unauthorized()
    {
        return new ApiException(401, "unauthorized", "Missing or invalid bearer token", null, List.of(), //$NON-NLS-1$ //$NON-NLS-2$
                        Map.of("WWW-Authenticate", "Bearer")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException forbiddenHost()
    {
        return new ApiException(403, "forbidden-host", "The API must be addressed as localhost"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException forbiddenOrigin()
    {
        return new ApiException(403, "browser-origin-forbidden", "Requests with an Origin header are not allowed"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException notFound()
    {
        return new ApiException(404, "not-found", "Not found"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException badRequest(String detail)
    {
        return new ApiException(400, "invalid-request", "Invalid request", detail, List.of(), Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException validation(List<FieldError> errors)
    {
        return new ApiException(422, "validation", "Validation failed", null, errors, Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException conflict(String type, String title, String detail, List<FieldError> errors)
    {
        return new ApiException(409, type, title, detail, errors, Map.of());
    }

    public static ApiException locked()
    {
        return new ApiException(423, "user-interaction", "User interaction in progress, retry later", null, //$NON-NLS-1$ //$NON-NLS-2$
                        List.of(), Map.of("Retry-After", "5")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public int getStatus()
    {
        return status;
    }

    public String getType()
    {
        return type;
    }

    /** may be null; the title alone describes the problem then */
    public String getDetail()
    {
        return detail;
    }

    public List<FieldError> getErrors()
    {
        return errors;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }
}
