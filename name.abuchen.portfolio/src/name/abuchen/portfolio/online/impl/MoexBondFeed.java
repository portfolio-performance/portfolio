package name.abuchen.portfolio.online.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;

public class MoexBondFeed extends MoexISSCommon implements QuoteFeed 
{
    private static final String issBondUrl = "https://iss.moex.com/iss/history/engines/stock/markets/bonds/securities/%s.json?from=%s&till=%s&interval=1&boardid=%s&iss.only=history&iss.meta=off"; //$NON-NLS-1$

    @Override
    public String getId() 
    {
        return "moex.iss.bonds"; //$NON-NLS-1$
    }

    @Override
    public String getName() 
    {
        return "MOEX ISS (Bonds)"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
    {
        var to = LocalDate.now();
        var from = to.minusDays(1);
        var issResponce = fetch(issBondUrl, security, from, to);
        List<LatestSecurityPrice> data = parse(issResponce, true);
        return data.isEmpty() ? Optional.empty() : Optional.of(data.getLast());
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean tryCollecting) throws QuoteFeedException 
    {
        var to = LocalDate.now();
        var from = to.minusDays(30);

        var issResponce = fetch(issBondUrl, security, from, to);
        List<LatestSecurityPrice> data = parse(issResponce, true);

        var result = new QuoteFeedData();
        result.addAllPrices(data);
        return result;
    }
}