package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.AmountConverter;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.BooleanConverter;
import name.abuchen.portfolio.model.AttributeType.DateConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.PercentPlainConverter;
import name.abuchen.portfolio.model.AttributeType.QuoteConverter;
import name.abuchen.portfolio.model.AttributeType.ShareConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.money.Values;

/**
 * Encodes a stored attribute value as a typed JSON value. It deliberately
 * avoids core's locale-dependent <code>Converter.toString</code>. Attributes
 * that can't be encoded (unsupported converter, unset or mismatched value) are
 * skipped, never an error.
 */
final class AttributeCodec
{
    /** How to read an encoded value; the ids are the values of <code>type</code> in the output. */
    enum Type
    {
        STRING, BOOLEAN, DATE, AMOUNT, QUOTE, SHARES, PERCENT, NUMBER;

        String id()
        {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    record Encoded(Type type, Object value)
    {
    }

    /** The ids of every type this codec can produce. */
    static final List<String> TYPES = Arrays.stream(Type.values()).map(Type::id).toList();

    private AttributeCodec()
    {
    }

    static Optional<Encoded> encode(AttributeType attributeType, Object stored)
    {
        if (stored == null)
            return Optional.empty();

        Object converter;
        try
        {
            converter = attributeType.getConverter();
        }
        catch (RuntimeException e)
        {
            return Optional.empty(); // unknown converter class in the file
        }

        if (converter instanceof StringConverter)
            return stored instanceof String s && MasterData.isSet(s) ? encoded(Type.STRING, s) : Optional.empty();
        if (converter instanceof BooleanConverter)
            return stored instanceof Boolean ? encoded(Type.BOOLEAN, stored) : Optional.empty();
        if (converter instanceof DateConverter)
            return stored instanceof LocalDate date ? encoded(Type.DATE, date.toString()) : Optional.empty();
        if (converter instanceof AmountConverter)
            return decimal(Type.AMOUNT, stored, Values.Amount);
        if (converter instanceof AmountPlainConverter)
            return decimal(Type.AMOUNT, stored, Values.AmountPlain);
        if (converter instanceof QuoteConverter)
            return decimal(Type.QUOTE, stored, Values.Quote);
        if (converter instanceof ShareConverter)
            return decimal(Type.SHARES, stored, Values.Share);
        if (converter instanceof PercentConverter)
            return finite(Type.PERCENT, stored);
        if (converter instanceof PercentPlainConverter)
            return finite(Type.NUMBER, stored);

        return Optional.empty(); // limit price, bookmark, image, unknown
    }

    private static Optional<Encoded> encoded(Type type, Object value)
    {
        return Optional.of(new Encoded(type, value));
    }

    private static Optional<Encoded> decimal(Type type, Object stored, Values<Long> values)
    {
        return stored instanceof Long l ? encoded(type, plain(BigDecimal.valueOf(l, values.precision())))
                        : Optional.empty();
    }

    /** Wrapped so that Gson writes 0.00000001 rather than BigDecimal's 1E-8. */
    private static JsonPrimitive plain(BigDecimal value)
    {
        return new JsonPrimitive(new PlainDecimal(value));
    }

    private static final class PlainDecimal extends Number
    {
        private static final long serialVersionUID = 1L;

        private final BigDecimal value;

        PlainDecimal(BigDecimal value)
        {
            this.value = value;
        }

        @Override
        public int intValue()
        {
            return value.intValue();
        }

        @Override
        public long longValue()
        {
            return value.longValue();
        }

        @Override
        public float floatValue()
        {
            return value.floatValue();
        }

        @Override
        public double doubleValue()
        {
            return value.doubleValue();
        }

        @Override
        public String toString()
        {
            return value.toPlainString();
        }
    }

    private static Optional<Encoded> finite(Type type, Object stored)
    {
        return stored instanceof Double d && Double.isFinite(d) ? encoded(type, d) : Optional.empty();
    }
}
