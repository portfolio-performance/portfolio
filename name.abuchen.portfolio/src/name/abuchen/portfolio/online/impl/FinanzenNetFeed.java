package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.ImportFinanzenNetQuotesSoup;
import name.abuchen.portfolio.online.QuoteFeed;

public class FinanzenNetFeed implements QuoteFeed
{

    @Override
    public String getId()
    {
        return QuoteFeed.FINANZENNET;
    }

    @Override
    public String getName()
    {
        return "www.finanzen.net";
    }

    @Override
    public void updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        String currentURL = null;
        for (Security security : securities)
        {
            currentURL = security.getFinanzenFeedURL();
            if (currentURL == null)
            {
                errors.add(new Exception("security without feed URL"));
                continue;
            }
            ImportFinanzenNetQuotesSoup parser = new ImportFinanzenNetQuotesSoup();
            try
            {
                List<LatestSecurityPrice> latestPrices = parser.extractFromURL(currentURL);
                System.out.println(latestPrices);
            }
            catch (Exception e)
            {
                errors.add(e);
            }
        }
    }

    @Override
    public void updateHistoricalQuotes(Security security) throws IOException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Exchange> getExchanges(Security subject) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
