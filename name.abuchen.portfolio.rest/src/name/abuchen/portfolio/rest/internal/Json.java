package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Typed, validating access to the fields of a JSON request body. Every
 * accessor records a {@link ApiException.FieldError} instead of throwing, so
 * that a handler can report all violations at once: read all fields, then call
 * {@link #throwIfErrors()} before touching the model.
 * <p/>
 * Decimals (amounts, shares, quotes, rates) are accepted as a JSON number or a
 * JSON string holding a plain decimal literal and are never converted to a
 * binary floating point number: Gson keeps the literal text of a number, so
 * {@code 12.30} stays exactly 12.30. Exponent notation ({@code 1e2}) is
 * rejected.
 */
public final class Json
{
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?"); //$NON-NLS-1$
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}"); //$NON-NLS-1$
    private static final Pattern DATE_TIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?"); //$NON-NLS-1$
    private static final BigDecimal MAX_UNSCALED = BigDecimal.valueOf(Long.MAX_VALUE);

    private final JsonObject body;
    private final List<ApiException.FieldError> errors = new ArrayList<>();
    private final Set<String> consumed = new HashSet<>();

    public Json(JsonObject body)
    {
        this.body = body;
    }

    public JsonObject body()
    {
        return body;
    }

    /** true if the field is present, even with an explicit {@code null} */
    public boolean has(String field)
    {
        consumed.add(field);
        return body.has(field);
    }

    /** true if the field is present with an explicit {@code null} */
    public boolean isNull(String field)
    {
        consumed.add(field);
        return body.has(field) && body.get(field).isJsonNull();
    }

    /** marks fields as known without reading them, see {@link #rejectUnknownFields()} */
    public void ignore(String... fields)
    {
        consumed.addAll(List.of(fields));
    }

    /** the string value, or null if absent or null */
    public String optString(String field)
    {
        var element = element(field);
        if (element == null)
            return null;
        if (!isString(element))
        {
            add(new ApiException.FieldError(field, "invalid-type", MessageFormat.format("{0} must be a string", field))); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        return element.getAsString();
    }

    /** the string value, or null if absent or null; longer strings are rejected */
    public String optString(String field, int maxLength)
    {
        var value = optString(field);
        if (value != null && value.length() > maxLength)
        {
            add(new ApiException.FieldError(field, "too-long", //$NON-NLS-1$
                            MessageFormat.format("{0} must be at most {1} characters", field, maxLength))); //$NON-NLS-1$
            return null;
        }
        return value;
    }

    /** the non-blank string value; absent, null or blank is an error */
    public String requireString(String field)
    {
        var value = optString(field);
        if ((value == null && !hasInvalidType(field)) || (value != null && value.isBlank()))
        {
            required(field);
            return null;
        }
        return value;
    }

    /**
     * The decimal value, or null if absent or null. Rejects more than
     * {@code maxScale} decimal places.
     */
    public BigDecimal decimal(String field, int maxScale)
    {
        var element = element(field);
        if (element == null)
            return null;

        if (!element.isJsonPrimitive() || element.getAsJsonPrimitive().isBoolean())
        {
            add(new ApiException.FieldError(field, "invalid-type", //$NON-NLS-1$
                            MessageFormat.format("{0} must be a decimal number or a string holding one", field))); //$NON-NLS-1$
            return null;
        }

        // for a number, getAsString returns the literal text as parsed
        var text = element.getAsString().strip();
        if (!DECIMAL.matcher(text).matches())
        {
            add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} must be a plain decimal number like 12.34, got {1}", field, //$NON-NLS-1$
                                            text)));
            return null;
        }

        var value = new BigDecimal(text);
        if (value.signum() != 0 && value.stripTrailingZeros().scale() > maxScale)
        {
            add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} must have at most {1} decimal places", field, maxScale))); //$NON-NLS-1$
            return null;
        }

        if (value.abs().movePointRight(maxScale).compareTo(MAX_UNSCALED) > 0)
        {
            add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} is too large", field))); //$NON-NLS-1$
            return null;
        }

        return value;
    }

    /** the decimal value; absent or null is an error */
    public BigDecimal requireDecimal(String field, int maxScale)
    {
        var present = element(field) != null;
        var value = decimal(field, maxScale);
        if (!present)
            required(field);
        return value;
    }

    /** a date {@code YYYY-MM-DD}, or null if absent or null */
    public LocalDate date(String field)
    {
        var text = optString(field);
        if (text == null)
            return null;

        if (DATE.matcher(text).matches())
        {
            try
            {
                return LocalDate.parse(text);
            }
            catch (DateTimeParseException e)
            {
                // fall through to the error
            }
        }

        add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        MessageFormat.format("{0} must be a date YYYY-MM-DD, got {1}", field, text))); //$NON-NLS-1$
        return null;
    }

    /** a date {@code YYYY-MM-DD}; absent or null is an error */
    public LocalDate requireDate(String field)
    {
        var present = element(field) != null;
        var value = date(field);
        if (!present)
            required(field);
        return value;
    }

    /**
     * A date with an optional time, {@code YYYY-MM-DD} or
     * {@code YYYY-MM-DDTHH:MM[:SS]}; the time defaults to 00:00 as in the
     * application's dialogs. Null if absent or null.
     */
    public LocalDateTime dateTime(String field)
    {
        var text = optString(field);
        if (text == null)
            return null;

        try
        {
            if (DATE.matcher(text).matches())
                return LocalDate.parse(text).atStartOfDay();
            if (DATE_TIME.matcher(text).matches())
                return LocalDateTime.parse(text);
        }
        catch (DateTimeParseException e)
        {
            // fall through to the error
        }

        add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        MessageFormat.format("{0} must be YYYY-MM-DD or YYYY-MM-DDTHH:MM, got {1}", field, text))); //$NON-NLS-1$
        return null;
    }

    /** see {@link #dateTime(String)}; absent or null is an error */
    public LocalDateTime requireDateTime(String field)
    {
        var present = element(field) != null;
        var value = dateTime(field);
        if (!present)
            required(field);
        return value;
    }

    /** the boolean value, or null if absent or null */
    public Boolean bool(String field)
    {
        var element = element(field);
        if (element == null)
            return null;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean())
        {
            add(new ApiException.FieldError(field, "invalid-type", MessageFormat.format("{0} must be true or false", field))); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        return element.getAsBoolean();
    }

    /** reports every field of the body that no accessor has read as {@code unknown-field} */
    public void rejectUnknownFields()
    {
        for (var field : body.keySet())
        {
            if (!consumed.contains(field))
                add(new ApiException.FieldError(field, "unknown-field", MessageFormat.format("{0} is not a known field", field))); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /** records a violation found by the handler itself, e.g. a cross-field rule */
    public void add(ApiException.FieldError error)
    {
        errors.add(error);
    }

    public List<ApiException.FieldError> errors()
    {
        return List.copyOf(errors);
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    /** throws a 422 with every violation recorded so far */
    public void throwIfErrors()
    {
        if (!errors.isEmpty())
            throw ApiException.validation(errors);
    }

    private void required(String field)
    {
        add(new ApiException.FieldError(field, "required", MessageFormat.format("{0} is required", field))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private boolean hasInvalidType(String field)
    {
        return errors.stream().anyMatch(e -> e.field().equals(field));
    }

    /** the field's element, or null if absent or JSON null */
    private JsonElement element(String field)
    {
        consumed.add(field);
        var element = body.get(field);
        return element == null || element.isJsonNull() ? null : element;
    }

    private static boolean isString(JsonElement element)
    {
        return element instanceof JsonPrimitive primitive && primitive.isString();
    }
}
