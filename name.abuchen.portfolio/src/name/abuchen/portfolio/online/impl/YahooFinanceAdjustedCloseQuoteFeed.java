package name.abuchen.portfolio.online.impl;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.SecurityPrice;

public class YahooFinanceAdjustedCloseQuoteFeed extends YahooFinanceQuoteFeed
{
    @Override
    public String getId()
    {
        return "YAHOO-ADJUSTEDCLOSE"; //$NON-NLS-1$
    }

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinanceAdjustedClose;
    }

    @Override
    protected <T extends SecurityPrice> void fillValues(String[] values, T price, DecimalFormat priceFormat,
                    DateTimeFormatter dateFormat) throws ParseException
    {
        // Date,Open,High,Low,Close,Volume,Adj Close
        super.fillValues(values, price, priceFormat, dateFormat);

        Number q = priceFormat.parse(values[6]);
        long v = (long) (q.doubleValue() * 100);
        price.setValue(v);
    }

}
