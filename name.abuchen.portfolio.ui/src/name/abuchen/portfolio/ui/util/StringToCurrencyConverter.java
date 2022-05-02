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
    private final NumberFormat full;

    private final int factor;
    private boolean acceptNegativeValues;

    private String decimalPattern;
    private String groupingPattern;


    public StringToCurrencyConverter(Values<?> type)
    {
        this(type, false);
    }

    public StringToCurrencyConverter(Values<?> type, boolean acceptNegativeValues)
    {
        this.factor = type.factor();
        this.acceptNegativeValues = acceptNegativeValues;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        char groupingSeperator = symbols.getGroupingSeparator();
        char decimalSeperator = symbols.getDecimalSeparator();

        this.decimalPattern = Pattern.quote(Character.toString(decimalSeperator));
        this.groupingPattern = String.format("[ ,\\.%s]", Pattern.quote(Character.toString(groupingSeperator))); //$NON-NLS-1$
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
        // Split the value in the decimal parts
        String[] parts = part.split(this.decimalPattern);
        if(parts.length == 0)
        {
            throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, part));
        }
        if(parts.length > 2)
        {
            throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, part));
        }
        
        String numberPart = parts[0];
        
        // remove the grouping separators
        numberPart = numberPart.replaceAll(this.groupingPattern, ""); //$NON-NLS-1$
        
        Number before = numberPart.trim().length() > 0 ? full.parse(numberPart) : Long.valueOf(0);
        boolean isNegative = numberPart.contains("-"); //$NON-NLS-1$
        
        if(!this.acceptNegativeValues && isNegative)
        {
            throw new IllegalArgumentException(String.format(Messages.CellEditor_NotANumber, part));
        }

        // Check if the number contains decimal parts
        long after = 0;
        if(parts.length == 2)
        {
            String decimalPart = parts[1];
            
            // remove the grouping separators
            decimalPart = decimalPart.replaceAll(this.groupingPattern, ""); //$NON-NLS-1$
            
            after =  convertStringToDecimals(decimalPart);           
        }
        else
        {
            // the input has no decimal part
            if ("BE".equals(Locale.getDefault().getCountry())) //$NON-NLS-1$
            {
                // In some culture (eg. fr_be and nl_be) it is normal do use the dot character as the decimal separator in the input field
                // Other applications like Excel convert this automatically to the correct value
                // So we check if there is only one grouping separator in the string
                
                String[] groups = parts[0].split(Pattern.quote(".")); //$NON-NLS-1$
                if(groups.length == 2)
                {
                    // We found only one grouping separator so we assume this is the decimal separator
                    
                    // remove the grouping separators
                    numberPart = groups[0].replaceAll(this.groupingPattern, ""); //$NON-NLS-1$
                    
                    before = numberPart.trim().length() > 0 ? full.parse(numberPart) : Long.valueOf(0);
                    
                    // remove the grouping separators
                    String decimalPart = groups[1].replaceAll(this.groupingPattern, ""); //$NON-NLS-1$
                    
                    after = convertStringToDecimals(decimalPart);
                }
            }
        }
        
        // For negative numbers: subtract decimal digits instead of adding them
        return before.longValue() * factor + (isNegative ? -after : after);
    }
    
    private long convertStringToDecimals(String strDecimals)
    {
        int length = (int) Math.log10(factor);

        if (strDecimals.length() > length)
            strDecimals = strDecimals.substring(0, length);

        long after = Long.parseLong(strDecimals);

        for (int ii = strDecimals.length(); ii < length; ii++)
            after *= 10;
        
        return after;
    }
}
