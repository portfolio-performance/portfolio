package name.abuchen.portfolio.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class XStreamInstantConverter extends AbstractSingleValueConverter
{
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
    {
        return type.equals(Instant.class);
    }

    @Override
    public String toString(Object source)
    {
        return DateTimeFormatter.ISO_INSTANT.format((Instant) source);
    }

    @Override
    public Object fromString(String s)
    {
        try
        {
            return Instant.parse(s);
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }
}
