package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.OnlineHelper;
import name.abuchen.portfolio.util.WebAccess;

public class MayaTaseQuoteFeed implements QuoteFeed
{

 public static final String ID = "MAYA"; //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelTaseFinance;
    }

    @Override
    public String getGroupingCriterion(Security security)
    {
        return "maya.tase.co.il"; //$NON-NLS-1$
    }
    
    @SuppressWarnings("nls")
    @VisibleForTesting
    public String rpcLatestQuote(Security security) throws IOException
    {
    	// https://maya.tase.co.il/api/v1/funds/mutual/1178235
        return new WebAccess("maya.tase.co.il", "/api/v1/funds/mutual/" + security.getTickerSymbol())
                        .addUserAgent(OnlineHelper.getMayaTaseUserAgent()) 
                        //.addParameter("lang", "en-US").addParameter("region", "US")
                        //.addParameter("corsDomain", "finance.yahoo.com")
                        .addParameter("X-Maya-With", "allow")
                        .addParameter("Referer", "https://maya.tase.co.il")
                        .get();
    }
    
    

    
     @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return internalGetQuotes(security, LocalDate.now().minusMonths(2));
    }    
    
    private QuoteFeedData internalGetQuotes(Security security, LocalDate startDate)
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        try
        {
            String responseBody = requestData(security, startDate);
            return extractQuotes(responseBody, security.getCurrencyCode());
        }
        catch (IOException e)
        {
            return QuoteFeedData.withError(new IOException(MessageFormat.format(Messages.MsgErrorDownloadYahoo, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }
    }
    
    
    /* package */ QuoteFeedData extractQuotes(String responseBody)
    {
        return extractQuotes(responseBody, ""); //$NON-NLS-1$
    }
    
     @SuppressWarnings("nls")
    private String requestData(Security security, LocalDate startDate) throws IOException
    {
        int days = Dates.daysBetween(startDate, LocalDate.now());

        // "max" only returns a sample of quotes
        String range = "30y"; //$NON-NLS-1$

        if (days < 25)
            range = "1mo"; //$NON-NLS-1$
        else if (days < 75)
            range = "3mo"; //$NON-NLS-1$
        else if (days < 150)
            range = "6mo"; //$NON-NLS-1$
        else if (days < 300)
            range = "1y"; //$NON-NLS-1$
        else if (days < 600)
            range = "2y"; //$NON-NLS-1$
        else if (days < 1500)
            range = "5y"; //$NON-NLS-1$
        else if (days < 3000)
            range = "10y"; //$NON-NLS-1$
        else if (days < 6000)
            range = "20y"; //$NON-NLS-1$

        return new WebAccess("query1.finance.yahoo.com", "/v8/finance/chart/" + security.getTickerSymbol()) //
                        .addUserAgent(OnlineHelper.getYahooFinanceUserAgent()) //
                        .addParameter("range", range) //
                        .addParameter("interval", "1d").get();

    }
    
    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
    	LocalDate start = caculateStart(security);
    	return internalGetQuotes(security, start);
	}
    	
    /**
     * Calculate the first date to request historical quotes for.
     */
    /* package */
    final LocalDate caculateStart(Security security)
    {
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            return lastHistoricalQuote.getDate();
        }
        else
        {
            return LocalDate.of(1900, 1, 1);
        }
    }
 
 
    private QuoteFeedData extractQuotes(String responseBody, String securityCurrency)
    {
        List<LatestSecurityPrice> answer = new ArrayList<>();

        try
        {
            JSONObject responseData = (JSONObject) JSONValue.parse(responseBody);
            if (responseData == null)
                throw new IOException("responseBody"); //$NON-NLS-1$

            JSONObject resultSet = (JSONObject) responseData.get("chart"); //$NON-NLS-1$
            if (resultSet == null)
                throw new IOException("chart"); //$NON-NLS-1$

            JSONArray result = (JSONArray) resultSet.get("result"); //$NON-NLS-1$
            if (result == null || result.isEmpty())
                throw new IOException("result"); //$NON-NLS-1$

            JSONObject result0 = (JSONObject) result.get(0);
            if (result0 == null)
                throw new IOException("result[0]"); //$NON-NLS-1$

            String quoteCurrency = null;
            ZoneId exchangeZoneId = ZoneOffset.UTC;
            if (result0.containsKey("meta")) //$NON-NLS-1$
            {
                JSONObject meta = (JSONObject) result0.get("meta"); //$NON-NLS-1$
                exchangeZoneId = extractTimezone((String) meta.get("exchangeTimezoneName"), ZoneOffset.UTC); //$NON-NLS-1$
                quoteCurrency = (String) meta.get("currency"); //$NON-NLS-1$
            }

            JSONArray timestamp = (JSONArray) result0.get("timestamp"); //$NON-NLS-1$

            JSONObject indicators = (JSONObject) result0.get("indicators"); //$NON-NLS-1$
            if (indicators == null)
                throw new IOException("indicators"); //$NON-NLS-1$

            JSONObject quotes = extractQuotesObject(indicators);

            JSONArray close = (JSONArray) extractCloseArray(quotes);
            int size = close.size();

            JSONArray high = null;
            try
            {
                high = (JSONArray) quotes.get("high"); //$NON-NLS-1$
            }
            catch (NullPointerException e)
            {
                // Ignore
            }

            JSONArray low = null;
            try
            {
                low = (JSONArray) quotes.get("low"); //$NON-NLS-1$
            }
            catch (NullPointerException e)
            {
                // Ignore
            }

            JSONArray volume = null;
            try
            {
                volume = (JSONArray) quotes.get("volume"); //$NON-NLS-1$
            }
            catch (NullPointerException e)
            {
                // Ignore
            }

            LatestSecurityPrice previous = null;
            for (int index = 0; index < size; index++)
            {
                Long ts = (Long) timestamp.get(index);
                Double q = (Double) close.get(index);
                Double h = null;
                Double l = null;
                Long v = null;

                if (high != null && !high.isEmpty())
                    try
                    {
                        h = (Double) high.get(index);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        // Ignore
                    }
                if (low != null && !low.isEmpty())
                    try
                    {
                        l = (Double) low.get(index);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        // Ignore
                    }
                if (volume != null && !volume.isEmpty())
                    try
                    {
                        v = (Long) volume.get(index);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        // Ignore
                    }

                if (ts != null && q != null && q.doubleValue() > 0)
                {
                    LatestSecurityPrice price = new LatestSecurityPrice();
                    price.setDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), exchangeZoneId).toLocalDate());

                    price.setValue(convertBritishPounds(Values.Quote.factorize(roundQuoteValue(q)), quoteCurrency, securityCurrency));

                    if (h != null && h.doubleValue() > 0)
                        price.setHigh(convertBritishPounds(Values.Quote.factorize(roundQuoteValue(h)), quoteCurrency, securityCurrency));
                    else
                        price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);

                    if (l != null && l.doubleValue() > 0)
                        price.setLow(convertBritishPounds(Values.Quote.factorize(roundQuoteValue(l)), quoteCurrency, securityCurrency));
                    else
                        price.setLow(LatestSecurityPrice.NOT_AVAILABLE);

                    if (v != null && v.longValue() > 0)
                        price.setVolume(v.longValue());
                    else
                        price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                    if (previous != null && previous.getDate().equals(price.getDate()))
                        answer.set(answer.size() - 1, price);
                    else
                        answer.add(price);

                    previous = price;
                }
            }
        }
        catch (IOException | IndexOutOfBoundsException | IllegalArgumentException e)
        {
            return QuoteFeedData.withError(e);
        }

        QuoteFeedData data = new QuoteFeedData();
        data.getLatestPrices().addAll(answer);
        data.addResponse("n/a", responseBody); //$NON-NLS-1$
        return data;
    }   
    
    // Yahoo API seesms to return floating numbers --> limit to 4
    // digits which seems to round it to the right value
    protected double roundQuoteValue(double value)
    {
        return Math.round(value * 10000) / 10000d;
    }
    
    /**
     * Convert GBP to GBX and vice versa if the quote and security currency
     * match.
     */
    private long convertBritishPounds(long price, String quoteCurrency, String securityCurrency)
    {
        if (quoteCurrency != null)
        {
            if ("GBP".equals(quoteCurrency) && "GBX".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
                return price * 100;
            if ("GBp".equals(quoteCurrency) && "GBP".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
                return price / 100;
        }
        return price;
    }
    
	private ZoneId extractTimezone(String exchangeTimezoneName, ZoneOffset defaultZone)
    {
        if (exchangeTimezoneName == null || exchangeTimezoneName.isEmpty())
            return defaultZone;

        try
        {
            return ZoneId.of(exchangeTimezoneName);
        }
        catch (DateTimeException e)
        {
            return defaultZone;
        }
    }

	protected JSONObject extractQuotesObject(JSONObject indicators) throws IOException
    {
        JSONArray quotes = (JSONArray) indicators.get("quote"); //$NON-NLS-1$
        if (quotes == null || quotes.isEmpty())
            throw new IOException("quote"); //$NON-NLS-1$

        JSONObject quote = (JSONObject) quotes.get(0);
        if (quote == null)
            throw new IOException();

        return quote;
    }

    protected JSONArray extractCloseArray(JSONObject quotes) throws IOException
    {
        JSONArray close = (JSONArray) quotes.get("close"); //$NON-NLS-1$
        if (close == null || close.isEmpty())
            throw new IOException("close"); //$NON-NLS-1$

        return close;
    }

@Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {

            return Optional.empty();

    }

    private Optional<String> extract(String body, int startIndex, String startToken, String endToken)
    {
        int begin = body.indexOf(startToken, startIndex);

        if (begin < 0)
            return Optional.empty();

        int end = body.indexOf(endToken, begin + startToken.length());
        if (end < 0)
            return Optional.empty();

        return Optional.of(body.substring(begin + startToken.length(), end));
    }

}
