package name.abuchen.portfolio.model;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.money.Values;

public class AttributeType
{
    private static final Pattern PATTERN = Pattern.compile("^([\\d.]*)(,(\\d*))?$"); //$NON-NLS-1$

    public interface Converter
    {
        String toString(Object object);

        Object fromString(String value);
    }

    public static class StringConverter implements Converter
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

    }

    private static class LongConverter implements Converter
    {
        private final NumberFormat full = new DecimalFormat("#,###"); //$NON-NLS-1$

        private Values<Long> values;

        public LongConverter(Values<Long> values)
        {
            this.values = values;
        }

        @Override
        public String toString(Object object)
        {
            return object != null ? values.format((Long) object) : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            try
            {
                if (value.trim().length() == 0)
                    return null;

                Matcher m = PATTERN.matcher(value);
                if (!m.matches())
                    throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value));

                String strBefore = m.group(1);
                Number before = strBefore.trim().length() > 0 ? full.parse(strBefore) : Long.valueOf(0);

                String strAfter = m.group(3);
                int after = 0;
                if (strAfter != null && strAfter.length() > 0)
                {
                    after = Integer.parseInt(strAfter);

                    int length = (int) Math.log10(values.factor());
                    for (int ii = strAfter.length(); ii > length; ii--)
                        after /= 10;
                    for (int ii = strAfter.length(); ii < length; ii++)
                        after *= 10;
                }

                long resultValue = before.longValue() * values.factor() + after;
                return Long.valueOf(resultValue);
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(e);
            }
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

    private static class DoubleConverter implements Converter
    {
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
            try
            {
                if (value.trim().length() == 0)
                    return null;

                Matcher m = PATTERN.matcher(value);
                if (!m.matches())
                    throw new IllegalArgumentException(MessageFormat.format(Messages.MsgNotANumber, value));

                return Double.valueOf(full.parse(value).doubleValue());
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static class PercentConverter implements Converter
    {
        private final NumberFormat format = NumberFormat.getPercentInstance();

        public PercentConverter()
        {
            format.setMinimumFractionDigits(2);
        }

        @Override
        public String toString(Object object)
        {
            return object != null ? format.format((double) object) : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            try
            {
                String inputValue = value.trim();
                if (inputValue.length() == 0)
                    return null;
                // ensure there is a percent sign at the end
                if (!inputValue.endsWith("%")) //$NON-NLS-1$
                    inputValue += "%"; //$NON-NLS-1$

                return format.parse(inputValue).doubleValue();
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static class PercentPlainConverter extends DoubleConverter
    {
        public PercentPlainConverter()
        {
            super(Values.PercentPlain);
        }
    }

    public static class DateConverter implements Converter
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
    }

    private final String id;
    private String name;
    private String columnLabel;
    private Class<? extends Attributable> target;
    private Class<?> type;

    /**
     * Converter. Do not persist (includes formats, etc.) but recreate out of
     * type and value parameters.
     */
    private transient Converter converter; // NOSONAR
    private String converterClass;

    public AttributeType(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

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
        return target != null ? target.isAssignableFrom(type) : true;
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
                converter = (Converter) Class.forName(converterClass).newInstance();
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
        {
            throw new IllegalArgumentException(e);
        }
        return converter;
    }

    public boolean isNumber()
    {
        return Number.class.isAssignableFrom(type);
    }

    public Comparator<Object> getComparator()
    {
        return new Comparator<Object>()
        {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(Object o1, Object o2)
            {
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
                    return ((String) o1).compareToIgnoreCase((String) o2);
                else
                    return ((Comparable<Object>) o1).compareTo((Comparable<Object>) o2);
            }
        };
    }
}
