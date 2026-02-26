package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Load NAV prices from AMFI India.
 */
public class AMFIIndiaQuoteFeed implements QuoteFeed
{
    private record MutualFund(String schemeCode, String isin1, String isin2, LatestSecurityPrice price)
    {
    }

    public static final String ID = "AMFIINDIA"; //$NON-NLS-1$

    private final PageCache<List<MutualFund>> cache = new PageCache<>();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "AMFI India"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false);

        if (!data.getErrors().isEmpty())
            PortfolioLog.abbreviated(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        if (prices.isEmpty())
            return Optional.empty();

        Collections.sort(prices, new SecurityPrice.ByDate());

        return Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true);
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        if (security.getIsin() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgErrorMissingIdentifierForSecurity,
                                            Messages.CSVColumn_ISIN, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            var funds = cache.lookup(AMFIIndiaQuoteFeed.class.toString());

            if (funds == null)
            {
                WebAccess webaccess = new WebAccess("www.amfiindia.com", "/spages/NAVAll.txt"); //$NON-NLS-1$ //$NON-NLS-2$
                String content = webaccess.get();

                if (collectRawResponse)
                    data.addResponse("https://www.amfiindia.com/spages/NAVAll.txt", content); //$NON-NLS-1$

                funds = parse(content);
                cache.put(AMFIIndiaQuoteFeed.class.toString(), funds);
            }

            for (MutualFund fund : funds)
            {
                if (security.getIsin().equals(fund.isin1) || security.getIsin().equals(fund.isin2))
                {
                    data.addPrice(fund.price);
                    break;
                }
            }
        }
        catch (IOException e)
        {
            data.addError(e);
        }

        return data;
    }

    private List<MutualFund> parse(String content)
    {
        var answer = new ArrayList<MutualFund>();

        String[] lines = content.split("\\r?\\n"); //$NON-NLS-1$

        var dateFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.US); //$NON-NLS-1$

        for (String line : lines)
        {
            // @formatter:off
            // Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date
            // @formatter:on
            String[] words = line.split(";"); //$NON-NLS-1$
            if (words.length < 6)
                continue;

            String schemeCode = words[0];
            String isin1 = words[1];
            String isin2 = words[2];
            String nav = words[4];
            String date = words[5];

            try
            {
                var price = new LatestSecurityPrice(LocalDate.parse(date, dateFormat), YahooHelper.asPrice(nav));
                price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                if (price.getValue() > 0)
                    answer.add(new MutualFund(schemeCode, isin1, isin2, price));
            }
            catch (ParseException | DateTimeParseException ignore)
            {
                // ignore this record
            }
        }

        return answer;
    }
}
