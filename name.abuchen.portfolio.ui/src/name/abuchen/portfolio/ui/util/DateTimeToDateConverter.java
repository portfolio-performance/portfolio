package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.core.databinding.conversion.IConverter;

public class DateTimeToDateConverter implements IConverter<LocalDateTime, LocalDate>
{

    @Override
    public Object getFromType()
    {
        return LocalDateTime.class;
    }

    @Override
    public Object getToType()
    {
        return LocalDate.class;
    }

    @Override
    public LocalDate convert(LocalDateTime fromObject)
    {
        return fromObject != null ? fromObject.toLocalDate() : null;
    }
}
