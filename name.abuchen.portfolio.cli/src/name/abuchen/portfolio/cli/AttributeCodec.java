package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    record Encoded(String type, Object value)
    {
    }

    /** The values of <code>type</code> that this codec can produce. */
    static final List<String> TYPES = List.of("string", "boolean", "date", "amount", "quote", "shares", "percent",
                    "number");

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
            return stored instanceof String s && MasterData.isSet(s) ? encoded("string", s) : Optional.empty();
        if (converter instanceof BooleanConverter)
            return stored instanceof Boolean ? encoded("boolean", stored) : Optional.empty();
        if (converter instanceof DateConverter)
            return stored instanceof LocalDate date ? encoded("date", date.toString()) : Optional.empty();
        if (converter instanceof AmountConverter)
            return decimal("amount", stored, Values.Amount);
        if (converter instanceof AmountPlainConverter)
            return decimal("amount", stored, Values.AmountPlain);
        if (converter instanceof QuoteConverter)
            return decimal("quote", stored, Values.Quote);
        if (converter instanceof ShareConverter)
            return decimal("shares", stored, Values.Share);
        if (converter instanceof PercentConverter)
            return finite("percent", stored);
        if (converter instanceof PercentPlainConverter)
            return finite("number", stored);

        return Optional.empty(); // limit price, bookmark, image, unknown
    }

    private static Optional<Encoded> encoded(String type, Object value)
    {
        return Optional.of(new Encoded(type, value));
    }

    private static Optional<Encoded> decimal(String type, Object stored, Values<Long> values)
    {
        return stored instanceof Long l ? encoded(type, BigDecimal.valueOf(l, values.precision())) : Optional.empty();
    }

    private static Optional<Encoded> finite(String type, Object stored)
    {
        return stored instanceof Double d && Double.isFinite(d) ? encoded(type, d) : Optional.empty();
    }
}
