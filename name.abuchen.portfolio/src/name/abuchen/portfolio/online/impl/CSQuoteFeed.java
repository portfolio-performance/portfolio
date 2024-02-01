/**
 * 
 */
package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

/**
 * This class provides a feed for Credit Suisse Quotes of institutional funds
 * which are normally not offered to the general public except in special
 * scenarios like via the Swiss third pillar provider VIAC (third pillar =
 * "Säule 3a", a tax-exempt retirement saving scheme for residents of
 * Switzerland).
 */
public class CSQuoteFeed implements QuoteFeed
{
    // The ID still contains "HTML" even though the format has changed. The ID
    // is not changed though so that users don't have to save a new feed but can
    // still use the old one (they have to change the URL though)
    public static final String ID = "CREDITSUISSE_HTML_TABLE"; //$NON-NLS-1$
    private static final DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy"); //$NON-NLS-1$
    private static final Pattern OLDURLPATTERN = Pattern.compile("^.*(CH[\\d|A-Z]{9}\\d).*"); //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelCreditSuisseHTMLTable;
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        return internalGetQuotes(security, security.getFeedURL(), collectRawResponse);
    }

    private QuoteFeedData internalGetQuotes(Security security, String feedURL, boolean collectRawResponse)
    {
        try
        {
            if (feedURL == null || feedURL.length() == 0)
                throw new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName()));

            QuoteFeedData result = new QuoteFeedData();

            String urlToUse;
            if (feedURL.startsWith("https://amfunds.credit")) //$NON-NLS-1$
            {
                urlToUse = getNewURLfromOldURL(security, feedURL);
            }
            else
            {
                urlToUse = feedURL;
            }

            String content = new WebAccess(urlToUse).get();
            boolean dataLine = false;

            if (collectRawResponse)
            {
                result.addResponse(feedURL, content);
            }

            for (String line : content.lines().toArray(String[]::new))
            {
                if (dataLine)
                {
                    if (line.isBlank())
                    {
                        dataLine = false;
                    }
                    else
                    {
                        result.addPrice(getPrice(line));
                    }
                }
                else if (line.startsWith("Currency")) //$NON-NLS-1$
                {
                    checkCurrency(security, line);
                }
                else if (line.startsWith("NAV Date")) //$NON-NLS-1$
                {
                    dataLine = true;
                }
            }
            return result;
        }
        catch (Exception e)
        {
            return QuoteFeedData.withError(e);
        }
    }

    private String getNewURLfromOldURL(Security security, String oldURL)
    {
        Matcher m = OLDURLPATTERN.matcher(oldURL);
        if (!m.find() || m.groupCount() != 1)
        {
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorSecurityHasMalformedURL,
                            security.getName(), oldURL));
        }

        String isin = m.group(1);
        return "https://am.credit-suisse.com/content/dam/fund-related-documents/nav-history/NAVHistory_" + isin //$NON-NLS-1$
                        + ".csv"; //$NON-NLS-1$
    }

    private void checkCurrency(Security security, String line)
    {
        String feedCurrency = line.split(" ")[1].trim(); //$NON-NLS-1$
        if (!security.getCurrencyCode().equals(feedCurrency))
        {
            throw new IllegalStateException(MessageFormat.format(Messages.MsgErrorFeedCurrencyMismatch,
                            security.getName(), feedCurrency, security.getCurrencyCode()));
        }
    }

    private LatestSecurityPrice getPrice(String line)
    {
        String[] tokens = line.split(","); //$NON-NLS-1$
        LocalDate localDate = LocalDate.parse(tokens[0], DATEFORMATTER);
        long price = Values.Quote.factorize(Double.parseDouble(tokens[1]));
        return new LatestSecurityPrice(localDate, price);
    }
}
