package name.abuchen.portfolio.online.impl;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.SecurityPrice;

public class YahooFinanceAdjustedCloseQuoteFeed extends YahooFinanceQuoteFeed
{
    public static final String ID = "YAHOO-ADJUSTEDCLOSE"; //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinanceAdjustedClose;
    }

    @Override
    protected <T extends SecurityPrice> void fillValues(String[] values, T price, DecimalFormat priceFormat,
                    SimpleDateFormat dateFormat) throws ParseException
    {
        // Date,Open,High,Low,Close,Volume,Adj Close
        super.fillValues(values, price, priceFormat, dateFormat);

        Number q = priceFormat.parse(values[6]);
        long v = (long) (q.doubleValue() * 100);
        price.setValue(v);
    }

}
