package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.ArivaHistQuotesSoup;
import name.abuchen.portfolio.online.QuoteFeed;

public class ArivaHistKursFeed implements QuoteFeed
{

    private Exchange exchange;

    public ArivaHistKursFeed()
    {
        exchange = new Exchange("URL Default", "URL Default");
    }

    @Override
    public String getId()
    {
        return QuoteFeed.URL;
    }

    @Override
    public String getName()
    {
        return "historische Kurse URL";
    }

    @Override
    public void updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        String currentURL = null;
        for (Security security : securities)
        {
            currentURL = security.getQuoteFeedURL();
            if (currentURL == null)
            {
                errors.add(new Exception("security without feed URL"));
                continue;
            }
            ArivaHistQuotesSoup parser = new ArivaHistQuotesSoup();
            try
            {
                List<LatestSecurityPrice> latestPrices = parser.extractFromURL(currentURL);
                security.setLatest(latestPrices.get(0));
            }
            catch (Exception e)
            {
                errors.add(e);
            }
        }
    }

    private List<LatestSecurityPrice> getQuotes(Security security) throws IOException
    {
        List<LatestSecurityPrice> latestPrices = new ArrayList<LatestSecurityPrice>();
        ArivaHistQuotesSoup parser = new ArivaHistQuotesSoup();
        try
        {
            latestPrices = parser.extractFromURL(security.getQuoteFeedURL());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return latestPrices;
    }

    @Override
    public void updateHistoricalQuotes(Security security) throws IOException
    {
        for (LatestSecurityPrice price : getQuotes(security))
        {
            security.addPrice(price);
        }
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start) throws IOException
    {
        // At the moment the best I can do is to ignore the start and return
        // what is there
        return getQuotes(security);
    }

    @Override
    public List<Exchange> getExchanges(Security subject) throws IOException
    {
        List<Exchange> result = new ArrayList<Exchange>();
        result.add(exchange);
        return result;
    }

}
