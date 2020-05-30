package name.abuchen.portfolio.util;

import java.time.LocalDate;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class XStreamLocalDateConverter extends AbstractSingleValueConverter
{
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
    {
        return type.equals(LocalDate.class);
    }

    @Override
    public String toString(Object source)
    {
        return source.toString();
    }

    @Override
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