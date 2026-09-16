package name.abuchen.portfolio.online.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;

public class MoexSecurityFeed extends MoexISSCommon implements QuoteFeed 
{
    private static final String issUrl = "https://iss.moex.com/iss/history/engines/stock/markets/shares/securities/%s.json?from=%s&till=%s&interval=1&boardid=%s&iss.only=history&iss.meta=off"; //$NON-NLS-1$


    @Override
    public String getId() 
    {
        return "moex.iss.sec"; //$NON-NLS-1$
    }

    @Override
    public String getName() 
    {
        return "MOEX ISS (Security)"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
    {
        var to = LocalDate.now();
        var from = to.minusDays(1);
        var issResponce = fetch(issUrl, security, from, to);
        List<LatestSecurityPrice> data = parse(issResponce, false);
        return data.isEmpty() ? Optional.empty() : Optional.of(data.getLast());
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean tryCollecting) throws QuoteFeedException 
    {
        var to = LocalDate.now();
        var from = to.minusDays(90);

        var issResponce = fetch(issUrl, security, from, to);
        List<LatestSecurityPrice> data = parse(issResponce, false);

        var result = new QuoteFeedData();
        result.addAllPrices(data);
        return result;
    }

}