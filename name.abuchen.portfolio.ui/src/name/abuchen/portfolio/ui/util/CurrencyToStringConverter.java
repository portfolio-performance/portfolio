package name.abuchen.portfolio.ui.util;

import org.eclipse.core.databinding.conversion.IConverter;

public class CurrencyToStringConverter implements IConverter
{
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
    public Object convert(Object fromObject)
    {
        return String.format("%,.2f", ((Long) fromObject) / 100d); //$NON-NLS-1$
    }

}
