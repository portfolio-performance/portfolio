package name.abuchen.portfolio.online.impl;

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
    protected <T extends SecurityPrice> void fillValues(String[] values, T price, DateTimeFormatter dateFormat)
                    throws ParseException
    {
        // Date,Open,High,Low,Close,Volume,Adj Close
        super.fillValues(values, price, dateFormat);

        price.setValue(YahooHelper.asPrice(values[CSVColumn.AdjClose]));
    }

}
