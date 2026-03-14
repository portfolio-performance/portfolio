package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.core.databinding.conversion.IConverter;

public class DateToDateTimeConverter implements IConverter<LocalDate, LocalDateTime>
{

    @Override
    public Object getFromType()
    {
        return LocalDate.class;
    }

    @Override
    public Object getToType()
    {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime convert(LocalDate fromObject)
    {
        return fromObject != null ? fromObject.atStartOfDay() : null;
    }
}
