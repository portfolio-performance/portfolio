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

    @SuppressWarnings("unchecked")
    @Override
    protected JSONObject extractQuotesObject(JSONObject indicators) throws IOException
    {
        JSONObject quote = null;

        // High, low and volume are not available in the adjclose array, thus
        // try and get them from the super class
        try
        {
            quote = super.extractQuotesObject(indicators);
        }
        catch (IOException e)
        {
            quote = new JSONObject();
        }

        JSONArray quotes = (JSONArray) indicators.get("adjclose"); //$NON-NLS-1$
        if (quotes == null || quotes.isEmpty())
            throw new IOException();

        JSONObject adjquote = (JSONObject) quotes.get(0);
        if (adjquote == null)
            throw new IOException();

        quote.put("adjclose", extractCloseArray(adjquote)); //$NON-NLS-1$

        return quote;
    }

    @Override
    protected JSONArray extractCloseArray(JSONObject quotes) throws IOException
    {
        JSONArray adjclose = (JSONArray) quotes.get("adjclose"); //$NON-NLS-1$
        if (adjclose == null || adjclose.isEmpty())
            throw new IOException();

        return adjclose;
    }
}
