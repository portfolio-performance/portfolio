package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.online.impl.Column;

public class Monetary
{
    public String  currencyCode = Messages.LabelNoCurrencyCode;
    public BigDecimal value = null;
    
    public Monetary()
    {
    }
   
    public Monetary parse(String text, String  languageHint) throws ParseException
    {
        text.trim();
        String Currency = "";
        double Amount = 0;
        Pattern pCurrency = Pattern.compile("[^0-9\\.,\\s]*");
        Pattern pAmount   = Pattern.compile("[0-9\\.,]*");
        Matcher mCurrency = pCurrency.matcher(text);
        Matcher mAmount   = pAmount.matcher(text);
        while (mCurrency.find()) 
        {
            for (int i = 0; i < mCurrency.groupCount() + 1; i++)
                if(!mCurrency.group(i).isEmpty()) 
                    currencyCode = mCurrency.group(i);
        }
        while (mAmount.find()) 
        {
            for (int i = 0; i < mAmount.groupCount() + 1; i++)
                if(!mAmount.group(i).isEmpty())
                    value = BigDecimal.valueOf(Column.asDouble(mAmount.group(i), languageHint));
        }
        return this;
    }
    
    public Monetary valueOf(String currencyCode, BigDecimal value)
    {
        this.currencyCode = currencyCode;
        this.value = value;
        return this;
    }
    
    public BigDecimal getValue()
    {
        return value;
    }

    public String getCurrency()
    {
        return currencyCode;
    }

    public String toString()
    {
        DecimalFormat valueFormat = new DecimalFormat("#,##0.00####"); //$NON-NLS-1$        
        String valueStr = Messages.LabelNoAmount;
        if (value != null)
            valueStr =  valueFormat.format(value);
        return currencyCode + " " + valueStr;
    }
}
