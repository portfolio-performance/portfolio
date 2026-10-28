package name.abuchen.portfolio.rest.internal;

import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.rest.RestApiConstants;

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
    private final transient Map<String, String> extensions;

    public ApiException(int status, String type, String title)
    {
        this(status, type, title, null, List.of(), Map.of(), Map.of());
    }

    public ApiException(int status, String type, String title, String detail, List<FieldError> errors,
                    Map<String, String> headers)
    {
        this(status, type, title, detail, errors, headers, Map.of());
    }

    public ApiException(int status, String type, String title, String detail, List<FieldError> errors,
                    Map<String, String> headers, Map<String, String> extensions)
    {
        super(title);
        this.status = status;
        this.type = type;
        this.detail = detail;
        this.errors = List.copyOf(errors);
        this.headers = Map.copyOf(headers);
        this.extensions = Map.copyOf(extensions);
    }

    public static ApiException unauthorized()
    {
        return new ApiException(401, "unauthorized", "Missing or invalid bearer token", null, List.of(), //$NON-NLS-1$ //$NON-NLS-2$
                        Map.of("WWW-Authenticate", "Bearer"), //$NON-NLS-1$ //$NON-NLS-2$
                        // self-serve discovery: where a client can request access
                        Map.of("pairingEndpoint", RestApiConstants.PAIRING_ENDPOINT)); //$NON-NLS-1$
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

    /**
     * The entity exists but the requested interval contains nothing of it - a
     * different problem from an unknown id, and one that calls for different
     * client behaviour: report that nothing was held, rather than fix the id.
     */
    public static ApiException noActivityInPeriod(String detail)
    {
        return new ApiException(404, "no-activity-in-period", "No activity in the period", detail, List.of(), //$NON-NLS-1$ //$NON-NLS-2$
                        Map.of());
    }

    public static ApiException badRequest(String detail)
    {
        return new ApiException(400, "invalid-request", "Invalid request", detail, List.of(), Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** an invalid query parameter, reported per parameter like a validation error */
    public static ApiException badRequest(List<FieldError> errors)
    {
        return new ApiException(400, "invalid-request", "Invalid request", null, errors, Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** the body is refused before it is buffered whole, so the limit is the only detail */
    public static ApiException requestTooLarge(int maxBytes)
    {
        return new ApiException(413, "request-too-large", "Request body too large", //$NON-NLS-1$ //$NON-NLS-2$
                        "The request body must not exceed " + maxBytes + " bytes", List.of(), Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException validation(List<FieldError> errors)
    {
        return new ApiException(422, "validation", "Validation failed", null, errors, Map.of()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static ApiException conflict(String type, String title, String detail, List<FieldError> errors)
    {
        return new ApiException(409, type, title, detail, errors, Map.of());
    }

    public static ApiException tooManyRequests(String type, String title, long retryAfterSeconds)
    {
        return new ApiException(429, type, title, null, List.of(),
                        Map.of("Retry-After", String.valueOf(retryAfterSeconds))); //$NON-NLS-1$
    }

    public static ApiException locked()
    {
        return new ApiException(423, "user-interaction", "User interaction in progress, retry later", null, //$NON-NLS-1$ //$NON-NLS-2$
                        List.of(), Map.of("Retry-After", "5")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The UI thread did not take the request on within the deadline, so it was
     * not executed and never will be. Refused rather than waited: a request
     * parked on a busy UI thread (a long repaint, a slow job running there)
     * would otherwise hold a worker indefinitely, and the whole API would look
     * dead while only the UI is busy. Safe to send again.
     */
    public static ApiException uiBusy(long timeoutMillis)
    {
        return new ApiException(503, "ui-busy", "Application busy, retry later", //$NON-NLS-1$ //$NON-NLS-2$
                        "The application did not take the request on within " + timeoutMillis + " ms", //$NON-NLS-1$ //$NON-NLS-2$
                        List.of(), Map.of("Retry-After", "5")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The deadline passed while a read was already running on the UI thread. A
     * read changes nothing, so - unlike {@link #uiRequestUnderway} - it is the
     * same retryable refusal as {@link #uiBusy}: the answer was thrown away, and
     * sending the read again is the only way to get it.
     */
    public static ApiException uiReadTimedOut(long timeoutMillis)
    {
        return new ApiException(503, "ui-busy", "Application busy, retry later", //$NON-NLS-1$ //$NON-NLS-2$
                        "The read was still running on the UI thread when the wait for it expired after " //$NON-NLS-1$
                                        + timeoutMillis + " ms; it changes nothing, so it is safe to send again", //$NON-NLS-1$
                        List.of(), Map.of("Retry-After", "5")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The deadline passed while the request was already running on the UI
     * thread. Not {@link #uiBusy}: that one was not executed, this one will
     * finish, so it carries no Retry-After and must not be presented as
     * something the caller can safely send again - a second PATCH would apply
     * the same change twice.
     */
    public static ApiException uiRequestUnderway(long timeoutMillis)
    {
        return new ApiException(503, "ui-request-underway", "Application busy; the request is still being executed", //$NON-NLS-1$ //$NON-NLS-2$
                        "The request was already running on the UI thread when the wait for it expired after " //$NON-NLS-1$
                                        + timeoutMillis + " ms", //$NON-NLS-1$
                        List.of(), Map.of());
    }

    /**
     * The calculation did not finish within its deadline. Asking again unchanged
     * would take just as long, so there is no Retry-After: the caller has to ask
     * a smaller question - a shorter interval, or one instrument.
     */
    public static ApiException calculationTimeout(long timeoutSeconds)
    {
        return new ApiException(503, "calculation-timeout", "Calculation took too long", //$NON-NLS-1$ //$NON-NLS-2$
                        "The calculation did not finish within " + timeoutSeconds + " seconds", //$NON-NLS-1$ //$NON-NLS-2$
                        List.of(), Map.of());
    }

    /**
     * More requests are in flight than the server serves at once. Refused before
     * the body is read, which is what keeps the bound cheap to enforce.
     */
    public static ApiException serverBusy()
    {
        return new ApiException(503, "server-busy", "Too many requests in progress, retry shortly", null, //$NON-NLS-1$ //$NON-NLS-2$
                        List.of(), Map.of("Retry-After", "1")); //$NON-NLS-1$ //$NON-NLS-2$
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

    /** RFC 9457 extension members, rendered as additional top-level fields */
    public Map<String, String> getExtensions()
    {
        return extensions;
    }
}
