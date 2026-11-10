package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.money.Values;

/**
 * Translates a custom attribute's stored model value to and from typed JSON.
 * Deliberately independent of {@link name.abuchen.portfolio.model.AttributeType.Converter},
 * whose string form is locale-dependent (grouping/decimal separators, percent
 * scaling) and therefore unfit for a machine API. Values are encoded from the
 * stored value directly: fixed-point longs as plain decimals, doubles as
 * stored (a fraction for percent, unscaled for number), dates as ISO-8601.
 */
@SuppressWarnings("nls")
public final class AttributeCodec
{
    /** an attribute value that does not match its declared field type */
    public static class InvalidValueException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public InvalidValueException(String message)
        {
            super(message);
        }
    }

    private static final Set<AttributeFieldType> SUPPORTED = EnumSet.complementOf(EnumSet
                    .of(AttributeFieldType.LIMIT_PRICE, AttributeFieldType.BOOKMARK, AttributeFieldType.IMAGE));

    private AttributeCodec()
    {
    }

    public static boolean isSupported(AttributeFieldType type)
    {
        return SUPPORTED.contains(type);
    }

    public static String wireType(AttributeFieldType type)
    {
        return switch (type)
        {
            case STRING -> "string";
            case BOOLEAN -> "boolean";
            case DATE -> "date";
            // AMOUNTPLAIN differs from AMOUNT only in how the desktop UI renders
            // the value (trailing zeros dropped); storage, precision and wire
            // form are identical, so the API does not distinguish the two
            case AMOUNT, AMOUNTPLAIN -> "amount";
            case QUOTE -> "quote";
            case SHARE -> "shares";
            case PERCENT -> "percent";
            // despite its name PERCENTPLAIN neither scales nor formats as a
            // percentage: it is an unscaled decimal, and calling it a percent
            // on the wire would invite a factor-100 misreading against PERCENT
            case PERCENTPLAIN -> "number";
            case LIMIT_PRICE -> "limit-price";
            case BOOKMARK -> "bookmark";
            case IMAGE -> "image";
        };
    }

    public static JsonElement encode(AttributeFieldType type, Object stored)
    {
        return switch (type)
        {
            case STRING -> new JsonPrimitive((String) stored);
            case BOOLEAN -> new JsonPrimitive((Boolean) stored);
            case DATE -> new JsonPrimitive(stored.toString()); // LocalDate#toString is ISO-8601
            case AMOUNT -> EntityJson.decimal((Long) stored, Values.Amount.precision());
            case AMOUNTPLAIN -> EntityJson.decimal((Long) stored, Values.AmountPlain.precision());
            case QUOTE -> EntityJson.decimal((Long) stored, Values.Quote.precision());
            case SHARE -> EntityJson.decimal((Long) stored, Values.Share.precision());
            case PERCENT, PERCENTPLAIN -> EntityJson.decimal(((Number) stored).doubleValue());
            // unreachable: callers (read serialization) filter to isSupported types first
            default -> throw new IllegalStateException("not an API-encodable attribute type: " + type);
        };
    }

    public static Object decode(AttributeFieldType type, JsonElement value)
    {
        return switch (type)
        {
            case STRING -> requireString(value);
            case BOOLEAN -> requireBoolean(value);
            case DATE -> parseDate(value);
            case AMOUNT -> scaledLong(value, Values.Amount.precision());
            case AMOUNTPLAIN -> scaledLong(value, Values.AmountPlain.precision());
            case QUOTE -> scaledLong(value, Values.Quote.precision());
            case SHARE -> scaledLong(value, Values.Share.precision());
            case PERCENT, PERCENTPLAIN -> finiteDouble(value);
            default -> throw new InvalidValueException("not an API-writable attribute type: " + type);
        };
    }

    private static String requireString(JsonElement value)
    {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
            return value.getAsString();
        throw new InvalidValueException("expected a string");
    }

    private static Boolean requireBoolean(JsonElement value)
    {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean())
            return Boolean.valueOf(value.getAsBoolean());
        throw new InvalidValueException("expected true or false");
    }

    private static BigDecimal requireNumber(JsonElement value)
    {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber())
            return value.getAsBigDecimal();
        throw new InvalidValueException("expected a number");
    }

    private static LocalDate parseDate(JsonElement value)
    {
        var text = requireString(value);
        try
        {
            return LocalDate.parse(text); // strict ISO-8601 yyyy-MM-dd
        }
        catch (DateTimeParseException e)
        {
            throw new InvalidValueException("expected an ISO-8601 date (yyyy-MM-dd)");
        }
    }

    private static Long scaledLong(JsonElement value, int precision)
    {
        var decimal = requireNumber(value);
        try
        {
            return Long.valueOf(decimal.movePointRight(precision).setScale(0, RoundingMode.HALF_UP).longValueExact());
        }
        catch (ArithmeticException e)
        {
            throw new InvalidValueException("number out of range");
        }
    }

    private static Double finiteDouble(JsonElement value)
    {
        var d = requireNumber(value).doubleValue();
        if (!Double.isFinite(d))
            throw new InvalidValueException("number out of range");
        return Double.valueOf(d);
    }
}
