package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class StringToCurrencyConverter implements IValidatingConverter<String, Long>
{
    private final Pattern pattern;
    private final NumberFormat full;

    private final int factor;

    public StringToCurrencyConverter(Values<?> type)
    {
        this.factor = type.factor();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        pattern = Pattern.compile("^([\\d" + symbols.getGroupingSeparator() + "]*)(" //$NON-NLS-1$ //$NON-NLS-2$
                        + symbols.getDecimalSeparator() + "(\\d*))?$"); //$NON-NLS-1$
        full = new DecimalFormat("#,###"); //$NON-NLS-1$
    }

    // Experiment: let's say we'd have another constructor here which accepts numeric input with negative sign. This
    // could replace the original constructor, but one would then need to change all calls throughout PP
    public StringToCurrencyConverter(Values<?> type, boolean acceptNegatives)
    {
        this.factor = type.factor();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        pattern = Pattern.compile("^(" + (acceptNegatives ? "-?" : "") + "[\\d" + symbols.getGroupingSeparator() + "]*)(" //$NON-NLS-1$ //$NON-NLS-2$
                        + symbols.getDecimalSeparator() + "(\\d*))?$"); //$NON-NLS-1$
        full = new DecimalFormat("#,###"); //$NON-NLS-1$
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
    public Long convert(String fromObject)
    {
        String value = fromObject.trim();

        try
        {
            long result = 0;
            for (String part : value.split("\\+")) //$NON-NLS-1$
                result += convertToLong(part.trim());

            return Long.valueOf(result);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }

    }

    private long convertToLong(String part) throws ParseException
    {
        Matcher m = pattern.matcher(String.valueOf(part));
        if (!m.matches())
            throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, part));

        String strBefore = m.group(1);
        Number before = strBefore.trim().length() > 0 ? full.parse(strBefore) : Long.valueOf(0);
        boolean isNegative = strBefore.contains("-");

        String strAfter = m.group(3);
        long after = 0;
        if (strAfter != null && strAfter.length() > 0)
        {
            int length = (int) Math.log10(factor);

            if (strAfter.length() > length)
                strAfter = strAfter.substring(0, length);

            after = Long.parseLong(strAfter);

            for (int ii = strAfter.length(); ii < length; ii++)
                after *= 10;
        }

        // For negative numbers: subtract decimal digits instead of adding them
        return (isNegative ? before.longValue() * factor - after : before.longValue() * factor + after);
    }
}
