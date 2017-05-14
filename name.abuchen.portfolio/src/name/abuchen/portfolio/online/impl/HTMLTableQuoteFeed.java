package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;

public class HTMLTableQuoteFeed extends QuoteFeed
{

    public static final String ID = HTML; //$NON-NLS-1$

    private final PageCache cache = new PageCache();
    private final HTMLTableQuoteParser Parser = new HTMLTableQuoteParser();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelHTMLTable;
    }

    @Override
    public boolean updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        boolean isUpdated = false;

        for (Security security : securities)
        {
            // if latestFeed is null, then the policy is 'use same configuration
            // as historic quotes'
            String feedURL = security.getLatestFeed() == null ? security.getFeedURL() : security.getLatestFeedURL();

            List<LatestSecurityPrice> quotes = internalGetQuotes(security, feedURL, errors);
            int size = quotes.size();
            if (size > 0)
            {
                Collections.sort(quotes);

                LatestSecurityPrice latest = quotes.get(size - 1);
                LatestSecurityPrice previous = size > 1 ? quotes.get(size - 2) : null;
                latest.setPreviousClose(previous != null ? previous.getValue() : latest.getValue());
                latest.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                boolean isAdded = security.setLatest(latest);
                isUpdated = isUpdated || isAdded;
            }
        }

        return isUpdated;
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        List<LatestSecurityPrice> quotes = internalGetQuotes(security, security.getFeedURL(), errors);

        boolean isUpdated = false;
        for (LatestSecurityPrice quote : quotes)
        {
            boolean isAdded = security.addPrice(new SecurityPrice(quote.getDate(), quote.getValue()));
            isUpdated = isUpdated || isAdded;
        }

        return isUpdated;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        return internalGetQuotes(security, security.getFeedURL(), errors);
    }

    private List<LatestSecurityPrice> internalGetQuotes(Security security, String feedURL, List<Exception> errors)
    {
        if (feedURL == null || feedURL.length() == 0)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName())));
            return Collections.emptyList();
        }

        List<LatestSecurityPrice> answer = cache.lookup(feedURL);
        if (answer != null)
            return answer;

        answer = Parser.parseFromURL(feedURL, errors);

        if (!answer.isEmpty())
            cache.put(feedURL, answer);

        return answer;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        return Parser.parseFromHTML(response, errors);
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Collections.emptyList();
    }

}
