package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

public class XStreamLocalDateTimeConverter extends AbstractSingleValueConverter
{
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
    {
        return type.equals(LocalDateTime.class);
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
            return LocalDateTime.parse(s);
        }
        catch (Exception e)
        {
            try
            {
                // legacy models could have values stored as LocalDate 
                LocalDate localDate = LocalDate.parse(s);
                return localDate.atStartOfDay();
            }
            catch (Exception ex)
            {
                ex.addSuppressed(e);
                throw new UnsupportedOperationException(ex);
            }
        }
    }
}