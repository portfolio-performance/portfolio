package name.abuchen.portfolio.model;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
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

    /* package */static class StringConverter implements Converter
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

    /* package */static class LongConverter implements Converter
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

                long resultValue = before.longValue() * (int) values.factor() + after;
                return Long.valueOf(resultValue);
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /* package */static class DoubleConverter implements Converter
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

    private final String id;
    private String name;
    private String columnLabel;
    private Class<? extends Attributable> target;
    private Class<?> type;
    private Converter converter;

    public AttributeType(String id)
    {
        this.id = id;
    }

    /* package */AttributeType name(String name)
    {
        this.name = name;
        return this;
    }

    /* package */AttributeType columnLabel(String columnLabel)
    {
        this.columnLabel = columnLabel;
        return this;
    }

    /* package */AttributeType target(Class<? extends Attributable> target)
    {
        this.target = target;
        return this;
    }

    /* package */AttributeType type(Class<?> type)
    {
        this.type = type;
        return this;
    }

    /* package */AttributeType converter(Converter converter)
    {
        this.converter = converter;
        return this;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getColumnLabel()
    {
        return columnLabel;
    }

    public boolean supports(Class<? extends Attributable> type)
    {
        return target != null ? target.isAssignableFrom(type) : true;
    }

    public Converter getConverter()
    {
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
                    return ((String) o1).compareTo((String) o2);
                else
                    return ((Comparable<Object>) o1).compareTo((Comparable<Object>) o2);
            }
        };
    }
}
