package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.core.databinding.conversion.IConverter;

import name.abuchen.portfolio.money.Values;

public class CurrencyToStringConverter implements IConverter<Long, String>
{
    private final double factor;
    private final NumberFormat format;

    public CurrencyToStringConverter(Values<?> type)
    {
        this.factor = type.divider();
        this.format = new DecimalFormat(type.pattern());
    }

    @Override
    public Object getFromType()
    {
        return long.class;
    }

    @Override
    public Object getToType()
    {
        return String.class;
    }

    @Override
    public String convert(Long fromObject)
    {
        return format.format(fromObject.longValue() / factor);
    }
}
