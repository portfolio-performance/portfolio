package name.abuchen.portfolio.util;

import java.time.LocalDate;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class XStreamLocalDateConverter extends AbstractSingleValueConverter
{
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
    {
        return type.equals(LocalDate.class);
    }

    public String toString(Object source)
    {
        return source.toString();
    }

    public Object fromString(String s)
    {
        try
        {
            return LocalDate.parse(s);
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }
}