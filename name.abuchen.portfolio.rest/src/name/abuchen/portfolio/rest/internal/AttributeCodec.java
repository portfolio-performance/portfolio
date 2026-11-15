package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.ImageUtil;

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

    private static final Set<AttributeFieldType> READABLE = EnumSet.complementOf(EnumSet
                    .of(AttributeFieldType.BOOKMARK, AttributeFieldType.IMAGE));

    /**
     * Everything the API takes in - one type more than it hands out: an image
     * travels in as base64 and is stored in the file, but its bytes never
     * travel back out. A response that lists the instruments of a file would
     * otherwise carry every logo in it, and it is already the largest answer
     * of this API.
     */
    private static final Set<AttributeFieldType> WRITABLE = EnumSet.complementOf(EnumSet
                    .of(AttributeFieldType.BOOKMARK));

    /** the largest PNG this API stores - a 64x64 logo, what the application itself saves, fits many times over */
    public static final int MAX_IMAGE_BYTES = 64 * 1024;

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    /** a limit price on the wire: an operator and a dot-decimal price, the model's own two halves, locale-free */
    private static final Pattern LIMIT_PRICE_WIRE = Pattern.compile("^\\s*(<=|>=|<|>)\\s*(\\d+(?:\\.\\d{1,8})?)\\s*$"); //$NON-NLS-1$

    private AttributeCodec()
    {
    }

    /**
     * The field type of an attribute, or null if it has none the API knows.
     * {@link AttributeFieldType#of} instantiates the attribute's converter by
     * class name and throws when the class cannot be loaded - a converter the
     * running version does not have. Such an attribute is skipped like any
     * other the API does not support, instead of failing every response that
     * lists the instruments or the attribute types.
     */
    public static AttributeFieldType fieldType(AttributeType type)
    {
        try
        {
            return AttributeFieldType.of(type);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    /** whether the stored value of this type travels out in a response */
    public static boolean isReadable(AttributeFieldType type)
    {
        return READABLE.contains(type);
    }

    /** whether the API accepts a value of this type - wider than readable: see {@link #WRITABLE} */
    public static boolean isWritable(AttributeFieldType type)
    {
        return WRITABLE.contains(type);
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
            case LIMIT_PRICE -> new JsonPrimitive(wireLimit((LimitPrice) stored));
            // unreachable: callers (read serialization) filter to isReadable types first
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
            case IMAGE -> imageValue(value);
            case LIMIT_PRICE -> parseLimit(value);
            default -> throw new InvalidValueException("not an API-writable attribute type: " + type);
        };
    }

    /**
     * A limit price as {@code "<=62.5"} - the model's operator and price, dot
     * decimal, trailing zeros dropped. The application's own string form carries
     * the user's locale, which is why the API does not use it; this one means
     * the same bytes in and out, in every locale.
     */
    private static String wireLimit(LimitPrice limit)
    {
        var price = BigDecimal.valueOf(limit.getValue(), Values.Quote.precision()).stripTrailingZeros();
        return limit.getRelationalOperator().getOperatorString() + price.toPlainString();
    }

    private static LimitPrice parseLimit(JsonElement value)
    {
        var matcher = LIMIT_PRICE_WIRE.matcher(requireString(value));
        if (!matcher.matches())
            throw new InvalidValueException("expected a limit price like \"<=62.5\" - one of <, <=, >, >= "
                            + "and a price with a dot as decimal separator");

        var operator = RelationalOperator.findByOperator(matcher.group(1)).orElseThrow();
        try
        {
            return new LimitPrice(operator, new BigDecimal(matcher.group(2)) //
                            .movePointRight(Values.Quote.precision()) //
                            .setScale(0, RoundingMode.HALF_UP).longValueExact());
        }
        catch (ArithmeticException e)
        {
            throw new InvalidValueException("price out of range");
        }
    }

    /**
     * A logo as the model keeps it: the bytes of a PNG, base64'd behind the
     * data-url prefix the application's renderer insists on. A caller may
     * hand over raw base64 or the prefixed form - both end up stored the same
     * way - and the size cap keeps what the API writes desktop-shaped, the way
     * the application scales its own logos down to 64x64.
     */
    private static String imageValue(JsonElement value)
    {
        var text = requireString(value).trim();
        var base64 = text.startsWith(ImageUtil.BASE64PREFIX) ? text.substring(ImageUtil.BASE64PREFIX.length()) : text;

        byte[] bytes;
        try
        {
            bytes = Base64.getDecoder().decode(base64);
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidValueException("expected a PNG as base64, with or without the " + ImageUtil.BASE64PREFIX
                            + " prefix");
        }

        if (bytes.length < PNG_MAGIC.length || !Arrays.equals(Arrays.copyOf(bytes, PNG_MAGIC.length), PNG_MAGIC))
            throw new InvalidValueException("the bytes are not a PNG image");

        if (bytes.length > MAX_IMAGE_BYTES)
            throw new InvalidValueException("image too large - at most " + MAX_IMAGE_BYTES
                            + " bytes of PNG, which a 64x64 logo fits into many times over");

        return ImageUtil.BASE64PREFIX + Base64.getEncoder().encodeToString(bytes);
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
