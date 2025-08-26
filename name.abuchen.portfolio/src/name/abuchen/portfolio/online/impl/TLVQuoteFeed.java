package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

// import java.io.BufferedReader;
import java.io.IOException;
// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.net.URI;
// import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.DayOfWeek;
// import java.text.ParseException;
// import java.time.DateTimeException;
// import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

// import org.json.simple.JSONArray;
// import org.json.simple.JSONObject;
// import org.json.simple.JSONValue;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.OnlineHelper;
import name.abuchen.portfolio.util.WebAccess;

public class TLVQuoteFeed implements QuoteFeed
{

    public static final String ID = "TASE"; //$NON-NLS-1$

    // private static final String ENDPOINT = "mayaapi.tase.co.il";
    // //$NON-NLS-1$
    // private static final String PATH_FUND_DETAILS = "/api/fund/details";
    // //$NON-NLS-1$
    // private static final String PATH_SECURITY_DETAILS = "/api/fund/details";
    // //$NON-NLS-1$
    // private static final String PATH_FUND_HISTORIC = "/v1/candle";
    // //$NON-NLS-1$
    // private static final String PATH_SECURITY_HISTORIC = "/v1/candle";
    // //$NON-NLS-1$



    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        // return Messages.LabelTaseFinance;
        return "TLV - Tel Aviv Stock Exchange"; //$NON-NLS-1$
    }

    @Override
    public String getGroupingCriterion(Security security)
    {
    return "mayaapi.tase.co.il"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @VisibleForTesting
    public String rpcLatestQuote(Security security) throws IOException
    {
        String response = new WebAccess("mayaapi.tase.co.il", "/api/fund/details")
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //
                        .addParameter("lang", "1")
                        .addParameter("fundId", security.getTickerSymbol())
                        .addHeader("referer", "https://www.tase.co.il/")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("X-Maya-With", "allow")
                        .addHeader("Accept-Language", "en-US")
                        .get();
        return response;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
    {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        LocalDate start;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

        try 
        {
            LatestSecurityPrice price = new LatestSecurityPrice();
            String json = this.rpcLatestQuote(security);


            // TODO replace with Calendar
            if (day == DayOfWeek.SUNDAY) 
            {
                start = LocalDate.now().minusDays(3);
            }
            else if (day == DayOfWeek.SATURDAY) 
            {
                start = LocalDate.now().minusDays(2);
            }
            else 
            {
                start = LocalDate.now().minusDays(1);
            }


            Optional<String> time = extract(json, 0, "\"RelevantDate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (time.isPresent()) 
            {

                String dt = (time.get().trim()).replace("\"", "");
                LocalDateTime datetime = LocalDateTime.parse(dt);
                price.setDate(datetime.toLocalDate());

            }

            // TradeDate
            Optional<String> tradeDate = extract(json, 0, "\"TradeDate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (tradeDate.isPresent())
            {
                String td = (tradeDate.get().trim()).replace("\"", "");
                // System.out.println(td);
                LocalDate date = LocalDate.parse(td, formatter);
                price.setDate(date);
            }


            // Hardcoded to ILA for Tel-Aviv Market
            Optional<String> quoteCurrency = Optional.of("ILA"); //$NON-NLS-1$
            

            Optional<String> value = extract(json, 0, "\"UnitValuePrice\":", ","); //$NON-NLS-1$ //$NON-NLS-2$

            if (value.isPresent())
            {
                String p = value.get().trim();
                long asPrice = asPrice(p);
                price.setValue(convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            // Last Rate
            Optional<String> lastRate = extract(json, 0, "\"LastRate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$

            if (lastRate.isPresent())
            {
                String p = lastRate.get().trim();
                long asPrice = asPrice(p);
                price.setValue(convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }


            // MarketVolume

            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);
            
            Optional<String> highrate = extract(json, 0, "\"HighRate\":", ","); //$NON-NLS-1$
            if (highrate.isPresent())
            {
                String p = highrate.get().trim();
                long asPrice = asPrice(p);
                price.setHigh(convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }
            else
            {
                price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            }


            Optional<String> lowrate = extract(json, 0, "\"LowRate\":", ","); //$NON-NLS-1$
            if (lowrate.isPresent())
            {
                String p = lowrate.get().trim();
                long asPrice = asPrice(p);
                price.setLow(convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }
            else
            {
                price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            }


            if (price.getDate() == null  || price.getValue() <= 0) 
            {
                return Optional.empty();
            } 
            else 
            {
                return Optional.of(price);
            }
        } 
        catch (IOException | ParseException e)
        {
            // PortfolioLog.abbreviated(e);
            return Optional.empty();
        }
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
    /* package */final LocalDate caculateStart(Security security)
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

    /* package */ QuoteFeedData extractQuotes(String responseBody)
    {
        return extractQuotes(responseBody, ""); //$NON-NLS-1$
    }

    private QuoteFeedData extractQuotes(String responseBody, String securityCurrency)
    {

        QuoteFeedData data = new QuoteFeedData();
        data.addResponse("n/a", responseBody); //$NON-NLS-1$
        return data;
    }



    private static long convertILS(long price, String quoteCurrency, String securityCurrency)
    {
        if (quoteCurrency != null)
        {
            if ("ILA".equals(quoteCurrency) && "ILS".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
                return price / 100;
            if ("ILS".equals(quoteCurrency) && "ILA".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
                return price * 100;
        }
        return price;
    }
}

