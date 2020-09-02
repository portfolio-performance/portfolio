package name.abuchen.portfolio.online.impl;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.Messages;

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
    protected JSONArray extractQuotesArray(JSONObject indicators) throws IOException
    {
        JSONArray quotes = (JSONArray) indicators.get("adjclose"); //$NON-NLS-1$
        if (quotes == null || quotes.isEmpty())
            throw new IOException();

        JSONObject quote = (JSONObject) quotes.get(0);
        if (quote == null)
            throw new IOException();

        JSONArray adjclose = (JSONArray) quote.get("adjclose"); //$NON-NLS-1$
        if (adjclose == null || adjclose.isEmpty())
            throw new IOException();

        return adjclose;
    }
}
