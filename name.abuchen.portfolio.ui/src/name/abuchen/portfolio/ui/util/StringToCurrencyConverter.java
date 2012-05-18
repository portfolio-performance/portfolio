package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.core.databinding.conversion.IConverter;

public class StringToCurrencyConverter implements IConverter
{
    private static final Pattern PATTERN = Pattern.compile("^([\\d.]*)(,(\\d*))?$"); //$NON-NLS-1$
    private final NumberFormat full = new DecimalFormat("#,###"); //$NON-NLS-1$

    private final double factor;

    public StringToCurrencyConverter(Values<?> type)
    {
        this.factor = type.divider();
    }

    @Override
    public Object getFromType()
    {
        return String.class;
    }

    @Override
    public Object getToType()
    {
        return long.class;
    }

    @Override
    public Object convert(Object fromObject)
    {
        try
        {
            String value = (String) fromObject;

            Matcher m = PATTERN.matcher(String.valueOf(value));
            if (!m.matches())
                throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, value));

            String strBefore = m.group(1);
            Number before = strBefore.trim().length() > 0 ? full.parse(strBefore) : Long.valueOf(0);

            String strAfter = m.group(3);
            int after = 0;
            if (strAfter != null && strAfter.length() > 0)
            {
                after = Integer.parseInt(strAfter);

                int length = (int) Math.log10(factor);
                for (int ii = strAfter.length(); ii > length; ii--)
                    after /= 10;
                for (int ii = strAfter.length(); ii < length; ii++)
                    after *= 10;
            }

            long resultValue = before.longValue() * (int) factor + after;
            return Long.valueOf(resultValue);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
