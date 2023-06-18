package name.abuchen.portfolio.ui.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.Locale;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class StringToCurrencyConverter implements IValidatingConverter<String, Long>
{
    private static final char NBSP = '\u00A0'; // No-Break Space
    private static final char NNBS = '\u202f'; // Narrow No-Break Space

    private final DecimalFormat defaultPattern;
    private final DecimalFormat withSpacePattern;
    private final DecimalFormat belgianPattern;

    private final Values<?> type;
    private boolean acceptNegativeValues;

    public StringToCurrencyConverter(Values<?> type)
    {
        this(type, false);
    }

    public StringToCurrencyConverter(Values<?> type, boolean acceptNegativeValues)
    {
        this.type = type;
        this.acceptNegativeValues = acceptNegativeValues;

        defaultPattern = new DecimalFormat("#,###.##"); //$NON-NLS-1$
        defaultPattern.setParseBigDecimal(true);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();

        if (symbols.getGroupingSeparator() == NBSP || symbols.getGroupingSeparator() == NNBS)
        {
            // some (European) locales use the non-breaking space as grouping
            // separator. Support both: space and non-breaking space

            symbols.setGroupingSeparator(' ');
            withSpacePattern = new DecimalFormat("#,###.##", symbols); //$NON-NLS-1$
            withSpacePattern.setParseBigDecimal(true);
        }
        else
        {
            withSpacePattern = null;
        }

        if ("BE".equals(Locale.getDefault().getCountry())) //$NON-NLS-1$
        {
            // In Belgium (i.e. fr_be and nl_be) it is normal do use the
            // dot character as the decimal separator in the input field
            // because that is the key on the numeric keypad

            // Other applications like Excel convert this automatically to
            // the correct value

            symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            belgianPattern = new DecimalFormat("#.##", symbols); //$NON-NLS-1$
            belgianPattern.setParseBigDecimal(true);
        }
        else
        {
            belgianPattern = null;
        }
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

        long result = 0;
        for (String part : value.split("\\+|(?=-)")) //$NON-NLS-1$
            result += convertToLong(part.trim());

        if (result < 0 && !acceptNegativeValues)
            throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, value));

        return Long.valueOf(result);
    }

    private long convertToLong(String part)
    {
        if (belgianPattern != null)
        {
            // see above - parse with a special Belgian format that is used if
            // and only if there is only one dot in the string and not the
            // regular decimal separator

            int dot = part.indexOf('.');
            int comma = part.indexOf(defaultPattern.getDecimalFormatSymbols().getDecimalSeparator());

            if (comma < 0 && dot >= 0 && part.lastIndexOf('.') == dot)
                return parse(part, belgianPattern);
        }

        try
        {
            if (withSpacePattern != null)
                return parse(part, withSpacePattern);
        }
        catch (IllegalArgumentException ignore)
        {
            // fall back to default pattern
        }

        return parse(part, defaultPattern);
    }

    private long parse(String string, DecimalFormat decimalFormat)
    {
        ParsePosition parsePosition = new ParsePosition(0);
        BigDecimal answer = (BigDecimal) decimalFormat.parse(string, parsePosition);

        if (parsePosition.getIndex() == 0 || parsePosition.getIndex() < string.length())
            throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, string));

        return answer.multiply(type.getBigDecimalFactor()).longValue();
    }
}
