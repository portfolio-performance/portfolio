package name.abuchen.portfolio.model;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.protobuf.NullValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.model.LimitPrice.TargetMetric;
import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TextUtil;

public class AttributeType implements Named
{
    private static final Pattern LIMIT_PRICE_PATTERN = Pattern.compile("^\\s*(<=?|>=?)\\s*(.*)\\s*$"); //$NON-NLS-1$
    private static final String FORMULA_PROTO_PREFIX = "F:"; //$NON-NLS-1$
    private static final Pattern LIMIT_FORMULA_PATTERN = Pattern.compile(
                    "^(?<num>.+?)\\s*(?<pct>%)?\\s*(?:\\*\\s*)?(?<target>PP|AVG(?:\\s*(?:\\((?<groupParen>[^()]*)\\)|\\[(?<groupBracket>[^\\[\\]]*)\\]))?)$", //$NON-NLS-1$
                    Pattern.CASE_INSENSITIVE);

    /* protobuf only */ interface ProtoConverter
    {
        PAnyValue toProto(Object object);

        Object fromProto(PAnyValue entry);
    }

    public interface Converter
    {
        String toString(Object object);

        Object fromString(String value);
    }

    public static class StringConverter implements Converter, ProtoConverter
    {

        @Override
        public String toString(Object object)
        {
            return object != null ? (String) object : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            return value.trim().length() > 0 ? value.trim() : null;
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setString(String.valueOf(object)).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasString() ? value.getString() : null;
        }
    }

    public static class LimitPriceConverter implements Converter, ProtoConverter
    {
        private final LongConverter delegate = new LongConverter(Values.Quote);
        private final Pattern numberPattern = buildLocaleDependentNumberPattern();
        private final DecimalFormat numberFormat;

        public LimitPriceConverter()
        {
            this.numberFormat = new DecimalFormat("#,###"); //$NON-NLS-1$
            this.numberFormat.setParseBigDecimal(true);
        }

        @Override
        public String toString(Object object)
        {
            return object != null ? ((LimitPrice) object).toString() : ""; //$NON-NLS-1$
        }

        @Override
        public LimitPrice fromString(String value)
        {
            if (value.isBlank())
                return null;

            Matcher m = LIMIT_PRICE_PATTERN.matcher(value);
            if (!m.matches())
                throw new IllegalArgumentException(Messages.MsgNotAComparator);

            Optional<RelationalOperator> operator = RelationalOperator.findByOperator(m.group(1));

            // should not happen b/c regex pattern check
            if (!operator.isPresent())
                throw new IllegalArgumentException(Messages.MsgNotAComparator);

            var fm = LIMIT_FORMULA_PATTERN.matcher(m.group(2).trim());
            if (fm.matches())
                return parseFormulaLimit(value, operator.get(), fm);

            Long price = delegate.fromString(m.group(2));

            if (price == null || price < 0)
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value));

            return new LimitPrice(operator.get(), price);
        }

        private LimitPrice parseFormulaLimit(String fullInput, RelationalOperator operator, Matcher fm)
        {
            var rawNum = fm.group("num").trim(); //$NON-NLS-1$
            if (!numberPattern.matcher(rawNum).matches())
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, fullInput));

            BigDecimal parsed;
            try
            {
                parsed = (BigDecimal) numberFormat.parse(rawNum);
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, fullInput), e);
            }

            if (fm.group("pct") != null) //$NON-NLS-1$
                parsed = parsed.divide(BigDecimal.valueOf(100), Values.MC);

            double factor = parsed.doubleValue();
            if (!Double.isFinite(factor) || factor <= 0d)
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, fullInput));

            var rawTarget = fm.group("target").trim(); //$NON-NLS-1$
            if ("PP".equalsIgnoreCase(rawTarget)) //$NON-NLS-1$
                return new LimitPrice(operator, TargetMetric.PURCHASE_PRICE_FIFO, factor, null);

            var groupParen = fm.group("groupParen"); //$NON-NLS-1$
            var groupBracket = fm.group("groupBracket"); //$NON-NLS-1$
            var rawGroup = groupParen != null ? groupParen : groupBracket;

            if (rawGroup != null)
            {
                var trimmedGroup = rawGroup.trim();
                if (trimmedGroup.isEmpty() || trimmedGroup.indexOf('\t') >= 0 || trimmedGroup.indexOf('\n') >= 0
                                || trimmedGroup.indexOf('\r') >= 0)
                    throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, fullInput));

                return new LimitPrice(operator, TargetMetric.AVG_POSITION_VALUE, factor, trimmedGroup);
            }

            return new LimitPrice(operator, TargetMetric.AVG_POSITION_VALUE, factor, null);
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();

            var limitPrice = (LimitPrice) object;

            if (limitPrice.isStatic())
            {
                return PAnyValue.newBuilder().setString(limitPrice.getRelationalOperator().getOperatorString()
                                + Long.toString(limitPrice.getValue())).build();
            }

            var metricCode = limitPrice.getTargetMetric() == TargetMetric.PURCHASE_PRICE_FIFO ? "PP" : "AVG"; //$NON-NLS-1$ //$NON-NLS-2$
            var group = limitPrice.getGroupFilter() != null ? limitPrice.getGroupFilter() : ""; //$NON-NLS-1$
            var encoded = FORMULA_PROTO_PREFIX + limitPrice.getRelationalOperator().getOperatorString() + "\t" //$NON-NLS-1$
                            + Double.toString(limitPrice.getFactor()) + "\t" + metricCode + "\t" + group; //$NON-NLS-1$ //$NON-NLS-2$
            return PAnyValue.newBuilder().setString(encoded).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            if (value.hasString())
            {
                var raw = value.getString();
                if (raw.startsWith(FORMULA_PROTO_PREFIX))
                    return fromFormulaProto(raw.substring(FORMULA_PROTO_PREFIX.length()));

                var m = LIMIT_PRICE_PATTERN.matcher(raw);
                if (!m.matches())
                    return null;

                Optional<RelationalOperator> operator = RelationalOperator.findByOperator(m.group(1));

                // should not happen b/c regex pattern check
                if (!operator.isPresent())
                    throw new IllegalArgumentException(Messages.MsgNotAComparator);

                long price = Long.parseLong(m.group(2));

                return new LimitPrice(operator.get(), price);
            }
            else
            {
                return null;
            }
        }

        private LimitPrice fromFormulaProto(String payload)
        {
            var parts = payload.split("\t", -1); //$NON-NLS-1$
            if (parts.length < 3)
                return null;

            var operator = RelationalOperator.findByOperator(parts[0]);
            if (operator.isEmpty())
                return null;

            double factor;
            try
            {
                factor = Double.parseDouble(parts[1]);
            }
            catch (NumberFormatException e)
            {
                return null;
            }

            if (!Double.isFinite(factor) || factor <= 0d)
                return null;

            TargetMetric metric;
            if ("PP".equals(parts[2])) //$NON-NLS-1$
                metric = TargetMetric.PURCHASE_PRICE_FIFO;
            else if ("AVG".equals(parts[2])) //$NON-NLS-1$
                metric = TargetMetric.AVG_POSITION_VALUE;
            else
                return null;

            var group = parts.length >= 4 && !parts[3].isBlank() ? parts[3].trim() : null;
            if (metric != TargetMetric.AVG_POSITION_VALUE && group != null)
                return null;

            return new LimitPrice(operator.get(), metric, factor, group);
        }
    }

    private static class LongConverter implements Converter, ProtoConverter
    {
        private final Pattern pattern = buildLocaleDependentNumberPattern();
        private final DecimalFormat full;

        private Values<Long> values;

        public LongConverter(Values<Long> values)
        {
            this.values = values;

            this.full = new DecimalFormat("#,###"); //$NON-NLS-1$
            this.full.setParseBigDecimal(true);
        }

        @Override
        public String toString(Object object)
        {
            return object != null ? values.format((Long) object) : ""; //$NON-NLS-1$
        }

        @Override
        public Long fromString(String value)
        {
            if (value.isBlank())
                return null;

            Matcher m = pattern.matcher(value);
            if (!m.matches())
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value));

            try
            {
                BigDecimal v = (BigDecimal) full.parse(value.trim());
                return v.multiply(values.getBigDecimalFactor()).longValue();
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value), e);
            }
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setInt64((Long) object).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasInt64() ? value.getInt64() : null;
        }
    }

    public static class AmountConverter extends LongConverter
    {
        public AmountConverter()
        {
            super(Values.Amount);
        }
    }

    public static class AmountPlainConverter extends LongConverter
    {
        public AmountPlainConverter()
        {
            super(Values.AmountPlain);
        }
    }

    public static class QuoteConverter extends LongConverter
    {
        public QuoteConverter()
        {
            super(Values.Quote);
        }
    }

    public static class ShareConverter extends LongConverter
    {
        public ShareConverter()
        {
            super(Values.Share);
        }
    }

    private static class DoubleConverter implements Converter, ProtoConverter
    {
        private final Pattern pattern = buildLocaleDependentNumberPattern();
        private final NumberFormat full = new DecimalFormat("#,###.##"); //$NON-NLS-1$

        private Values<Double> values;

        public DoubleConverter(Values<Double> values)
        {
            this.values = values;
        }

        @Override
        public String toString(Object object)
        {
            return object != null ? values.format((Double) object) : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            if (value.isBlank())
                return null;

            Matcher m = pattern.matcher(value);
            if (!m.matches())
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value));

            try
            {
                return Double.valueOf(full.parse(value.trim()).doubleValue());
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value), e);
            }
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setDouble((Double) object).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasDouble() ? value.getDouble() : null;
        }
    }

    public static class PercentConverter extends DoubleConverter
    {
        public PercentConverter()
        {
            super(Values.Percent2);
        }

        @Override
        public Object fromString(String value)
        {
            Double v = (Double) super.fromString(value.replace("%", "")); //$NON-NLS-1$ //$NON-NLS-2$
            return v == null ? null : BigDecimal.valueOf(v).divide(BigDecimal.valueOf(100)).doubleValue();
        }
    }

    public static class PercentPlainConverter extends DoubleConverter
    {
        public PercentPlainConverter()
        {
            super(Values.PercentPlain);
        }
    }

    public static class DateConverter implements Converter, ProtoConverter
    {
        private static final DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT), //
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG), //
                        DateTimeFormatter.ofPattern("d.M.yyyy"), //$NON-NLS-1$
                        DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                        DateTimeFormatter.ISO_DATE };

        @Override
        public String toString(Object object)
        {
            if (object != null)
                return Values.Date.format((LocalDate) object);
            else
                return ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            if (value.trim().length() == 0)
                return null;

            for (DateTimeFormatter formatter : formatters)
            {
                try
                {
                    return LocalDate.parse(value, formatter);
                }
                catch (DateTimeParseException ignore)
                {
                    // continue with next formatter
                }
            }
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value));
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setInt64(((LocalDate) object).toEpochDay()).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasInt64() ? LocalDate.ofEpochDay(value.getInt64()) : null;
        }
    }

    public static class BooleanConverter implements Converter, ProtoConverter
    {

        @Override
        public String toString(Object object)
        {
            return object != null ? ((Boolean) object).toString() : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            if (value.trim().length() == 0)
                return null;

            return Boolean.valueOf(value);
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setBool((Boolean) object).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasBool() ? value.getBool() : null;
        }
    }

    public static class BookmarkConverter implements Converter, ProtoConverter
    {
        public static final Pattern PLAIN = Pattern.compile("^(?<link>https?\\:\\/\\/[^ \\t\\r\\n]+)$", //$NON-NLS-1$
                        Pattern.CASE_INSENSITIVE);
        public static final Pattern MARKDOWN = Pattern
                        .compile("^\\[(?<label>[^\\]]*)\\]\\((?<link>[^ \\t\\r\\n\\)]*)\\)$"); //$NON-NLS-1$

        @Override
        public String toString(Object object)
        {
            if (object == null)
            {
                return ""; //$NON-NLS-1$
            }
            else
            {
                Bookmark bookmark = (Bookmark) object;
                return bookmark.getLabel().equals(bookmark.getPattern()) ? bookmark.getPattern()
                                : String.format("[%s](%s)", bookmark.getLabel(), bookmark.getPattern()); //$NON-NLS-1$
            }
        }

        @Override
        public Object fromString(String value)
        {
            String trimmed = value.trim();

            if (trimmed.isEmpty())
                return null;

            Matcher matcher = MARKDOWN.matcher(trimmed);
            if (matcher.matches())
            {
                return new Bookmark(matcher.group("label"), matcher.group("link")); //$NON-NLS-1$ //$NON-NLS-2$
            }

            matcher = PLAIN.matcher(value);
            if (matcher.matches())
            {
                return new Bookmark(matcher.group("link"), matcher.group("link")); //$NON-NLS-1$ //$NON-NLS-2$
            }

            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorInvalidURL, trimmed));
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setString(toString(object)).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasString() ? fromString(value.getString()) : null;
        }
    }

    public static class ImageConverter implements Converter, ProtoConverter
    {
        public static final int MAXIMUM_SIZE_EMBEDDED_IMAGE = 64;

        @Override
        public String toString(Object object)
        {
            return object != null ? (String) object : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            return value;
        }

        @Override
        public PAnyValue toProto(Object object)
        {
            if (object == null)
                return PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build();
            else
                return PAnyValue.newBuilder().setString(String.valueOf(object)).build();
        }

        @Override
        public Object fromProto(PAnyValue value)
        {
            return value.hasString() ? value.getString() : null;
        }
    }

    private final String id;
    private String name;
    private String columnLabel;
    private String source;
    private Class<? extends Attributable> target;
    private Class<?> type;

    /**
     * Converter. Do not persist (includes formats, etc.) but recreate out of
     * type and value parameters.
     */
    private transient Converter converter; // NOSONAR
    private String converterClass;

    private TypedMap properties;

    public AttributeType(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    public String getColumnLabel()
    {
        return columnLabel;
    }

    public void setColumnLabel(String columnLabel)
    {
        this.columnLabel = columnLabel;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public Class<?> getType()
    {
        return type;
    }

    public void setType(Class<?> type)
    {
        this.type = type;
    }

    public Class<? extends Attributable> getTarget()
    {
        return target;
    }

    public void setTarget(Class<? extends Attributable> target)
    {
        this.target = target;
    }

    public boolean supports(Class<? extends Attributable> type)
    {
        return target == null || target.isAssignableFrom(type);
    }

    public void setConverter(Class<? extends Converter> converterClass)
    {
        this.converterClass = converterClass.getName();
        this.converter = null; // in case it was used before
    }

    public Converter getConverter()
    {
        try
        {
            if (converter == null)
                converter = (Converter) Class.forName(converterClass).getConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e)
        {
            throw new IllegalArgumentException(e);
        }
        return converter;
    }

    public boolean isNumber()
    {
        return Number.class.isAssignableFrom(type);
    }

    @SuppressWarnings("unchecked")
    public Comparator<Object> getComparator()
    {
        return (o1, o2) -> {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;

            if (type == Long.class)
                return ((Long) o1).compareTo((Long) o2);
            else if (type == Double.class)
                return ((Double) o1).compareTo((Double) o2);
            else if (type == String.class)
                return TextUtil.compare((String) o1, (String) o2);
            else
                return ((Comparable<Object>) o1).compareTo(o2);
        };
    }

    public TypedMap getProperties()
    {
        if (properties == null)
            properties = new TypedMap();
        return properties;
    }

    @Override
    public String getNote()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNote(String note)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("nls")
    private static Pattern buildLocaleDependentNumberPattern()
    {
        // e.g. \\-?(\\d+\\.)*\\d+(\\,\\d+)?
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.getDefault(Locale.Category.FORMAT));
        String numberRegex = "\\" + dfs.getMinusSign() + "?(\\d+\\" + dfs.getGroupingSeparator() + ")*\\d+(\\"
                        + dfs.getDecimalSeparator() + "\\d+)?";
        return Pattern.compile("^\\s*" + numberRegex + "\\s*$");
    }
}
