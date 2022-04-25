package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class StringToCurrencyConverter implements IValidatingConverter<String, Long>
{
    private final Pattern pattern;
    private final NumberFormat full;

    private final int factor;
    private char groupingSeperator;
    private char decimalSeperator;

    public StringToCurrencyConverter(Values<?> type)
    {
        this(type, false);
    }

    public StringToCurrencyConverter(Values<?> type, boolean acceptNegativeValues)
    {
        this.factor = type.factor();

        StringBuilder patternString = new StringBuilder();
        patternString.append("^("); //$NON-NLS-1$

        if (acceptNegativeValues)
            patternString.append("-?"); //$NON-NLS-1$

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        this.groupingSeperator = symbols.getGroupingSeparator();
        this.decimalSeperator = symbols.getDecimalSeparator();
        
        patternString.append("[\\d").append(this.groupingSeperator).append("]*)(") //$NON-NLS-1$ //$NON-NLS-2$
                        .append(this.decimalSeperator).append("(\\d*))?$"); //$NON-NLS-1$

        pattern = Pattern.compile(patternString.toString());
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
        boolean isNegative = strBefore.contains("-"); //$NON-NLS-1$

        String strAfter = m.group(3);
        long after = 0;
        if(strAfter == null) {
            // the input has no decimal part
            if ("BE".equals(Locale.getDefault().getCountry())) { //$NON-NLS-1$
            
                // In some culture (eg. fr_be and nl_be) it is normal do use the group separator as the decimal separator in the input field
                // Other applications like Excel convert this automatically to the correct value
                // So we check if there is only one grouping separator in the string
                
                String[] groups = strBefore.split(Pattern.quote(Character.toString(this.groupingSeperator)));
                if(groups.length == 2) {
                    // We found only one grouping separator so we assume this is the decimal separator
                    before = full.parse(groups[0]);
                    after = convertStringToDecimals(groups[1]);
                }
            }
            
        }
        else if (strAfter.length() > 0)
        {
            after = convertStringToDecimals(strAfter);
        }
        
        
        // For negative numbers: subtract decimal digits instead of adding them
        return before.longValue() * factor + (isNegative ? -after : after);
    }
    
    private long convertStringToDecimals(String strDecimals) {
        int length = (int) Math.log10(factor);

        if (strDecimals.length() > length)
            strDecimals = strDecimals.substring(0, length);

        long after = Long.parseLong(strDecimals);

        for (int ii = strDecimals.length(); ii < length; ii++)
            after *= 10;
        
        return after;
    }
}
