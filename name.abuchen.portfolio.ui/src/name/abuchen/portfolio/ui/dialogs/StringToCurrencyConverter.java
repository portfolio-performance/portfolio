package name.abuchen.portfolio.ui.dialogs;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.ui.Messages;

import org.eclipse.core.databinding.conversion.IConverter;

public class StringToCurrencyConverter implements IConverter
{
    private static final Pattern pattern = Pattern.compile("^([\\d.]*)(,(\\d\\d?))?$"); //$NON-NLS-1$

    @Override
    public Object getFromType()
    {
        return String.class;
    }

    @Override
    public Object getToType()
    {
        return int.class;
    }

    @Override
    public Object convert(Object fromObject)
    {
        try
        {
            String value = (String) fromObject;

            Matcher m = pattern.matcher(String.valueOf(value));
            if (!m.matches())
                throw new IllegalArgumentException(String.format(Messages.CurrencyConverter_MsgNotANumber, value));

            String strEuros = m.group(1);
            Number euros = strEuros.trim().length() > 0 ? new DecimalFormat("#,###").parse(strEuros) : Integer //$NON-NLS-1$
                            .valueOf(0);

            String strCents = m.group(3);
            int cents = 0;
            if (strCents != null)
            {
                cents = Integer.parseInt(strCents);
                if (strCents.length() == 1)
                    cents *= 10;
            }

            return Integer.valueOf(euros.intValue() * 100 + cents);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
