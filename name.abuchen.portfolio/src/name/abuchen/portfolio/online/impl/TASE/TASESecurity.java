package name.abuchen.portfolio.online.impl.TASE;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TASE.jsondata.SecurityHistory;
import name.abuchen.portfolio.online.impl.TASE.jsondata.SecurityHistoryEntry;
import name.abuchen.portfolio.online.impl.TASE.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TASESecurity extends TASEListing
{
    
    private static final int TYPE = 8;
    private static final int RECORDS = 1;

    private final String URL = "api.tase.co.il"; //$NON-NLS-1$
    private final String SECURITYDATAPATH = "/api/company/securitydata"; //$NON-NLS-1$
    private final String SECURITYHISTORYPATH = "/api/security/historyeod"; //$NON-NLS-1$
    private final String CURRENCY_CODE = "ILA"; //$NON-NLS-1$


    @SuppressWarnings("nls")
    @VisibleForTesting
    public String rpcLatestQuoteSecuritywithLanguage(Security security, Language lang) throws IOException
    {

        String response = new WebAccess(URL, SECURITYDATAPATH) // $NON-NLS-1$
                                                               // //$NON-NLS-2$
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("securityId", security.getWkn())
                        .addParameter("lang", lang.toString())
                        .get();
        return response;
    }

    public String rpcLatestQuoteSecurity(Security security) throws IOException
    {
        return this.rpcLatestQuoteSecuritywithLanguage(security, Language.ENGLISH);
    }

    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>
        {

            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/YYYY"); //$NON-NLS-1$

            @Override
            public JsonElement serialize(final LocalDate date, final Type typeOfSrc,
                            final JsonSerializationContext context)
            {
                return new JsonPrimitive(date.format(formatter));
            }

            @Override
            public LocalDate deserialize(final JsonElement json, final Type typeOfT,
                            final JsonDeserializationContext context) throws JsonParseException
            {
                return LocalDate.parse(json.getAsString(), formatter);
            }
        }

        if (security.getWkn() == null || security.getWkn().isEmpty() || security.getWkn().isBlank())
            return Optional.empty();
        try
        {
            String response = this.rpcLatestQuoteSecurity(security);
            Gson gson = new GsonBuilder()
                            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter()).create();
            Optional<SecurityListing> jsonprice = Optional.of(gson.fromJson(response, SecurityListing.class));

            // return price;
            Optional<LatestSecurityPrice> price = convertSecurityListingToSecurityPrice(jsonprice, security);
            return price;
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }
    }

    public Optional<QuoteFeedData> getHistoricalQuotes(Security security, boolean collectRawResponse)
    {

        if (security.getWkn() == null || security.getWkn().isEmpty() || security.getWkn().isBlank())
            return Optional.empty();

        LocalDate from = caculateStart(security);
        LocalDate to = LocalDate.now();

        try
        {
            Optional<SecurityHistory> securityHistory = getPriceHistoryChunkInternal(security.getWkn(), from, to, 1,
                            TASESecurity.RECORDS, Language.ENGLISH);

            Optional<QuoteFeedData> feed = convertSecurityHistoryToQuoteFeedData(securityHistory, security);
            return feed;
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }

    }



    public Optional<QuoteFeedData> convertSecurityHistoryToQuoteFeedData(Optional<SecurityHistory> historyopt,
                    Security security)
    {
        QuoteFeedData feed = new QuoteFeedData();

        Optional<String> quoteCurrency = getQuoteCurrency(security);
        LatestSecurityPrice price = null;

        if (historyopt.isEmpty())
            return Optional.empty();
        
        SecurityHistory history = historyopt.get();
        
        if ((history.getItems()).length == 0)
            return Optional.empty();

        SecurityHistoryEntry[] historyitemsarray = history.getItems();
        
        for (int i = 0; i < historyitemsarray.length; i++)

        {
            SecurityHistoryEntry entry = historyitemsarray[i];

            price = new LatestSecurityPrice();

            Optional<String> tradeDate = Optional.of(entry.getTradeDate());
            if (tradeDate.isPresent())
                price.setDate(asDate(tradeDate.get()));

            Optional<String> closePrice = Optional.of(entry.getCloseRate());
            if (closePrice != null && closePrice.isPresent())
            {
                long priceL = asPrice(closePrice.get());
                price.setValue(convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            String highRate = entry.getHighRate();
            if (highRate != null)
            {
                long priceL = asPrice(highRate);
                price.setHigh(convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            String lowRate = entry.getLowRate();
            if (lowRate != null)
            {

                long priceL = asPrice(lowRate);
                price.setLow(convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            Optional<String> volume = Optional.of(entry.getOverallTurnOverUnits());
            if (volume != null && volume.isPresent())
            {
                long priceL = asLong(volume.get());
                price.setVolume(priceL);
            }

            if (price.getDate() != null && price.getValue() > 0)
            {
                feed.addPrice(price);
            }
        }
        return Optional.of(feed);

    }



    // private LocalDate asDate(String s)
    // {
    // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // // $NON-NLS-1$
    //
    // if ("\"N/A\"".equals(s)) //$NON-NLS-1$
    // return null;
    // String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
    // return LocalDate.parse(dt, formatter); // $NON-NLS-1$
    // }

    private Optional<LatestSecurityPrice> convertSecurityListingToSecurityPrice(Optional<SecurityListing> listingopt,
                    Security security)
    {
        LatestSecurityPrice price = new LatestSecurityPrice();
        Optional<String> quoteCurrency = getQuoteCurrency(security);
        
        if (listingopt.isEmpty())
            return Optional.empty();

        SecurityListing listing = listingopt.get();
        // price.setVolume(RECORDS);

        // TradeDate
        Optional<String> tradeDate = Optional.of(listing.getTradeDate());
        if (tradeDate.isPresent())
        {
            LocalDate dt = asDate(tradeDate.get());
            price.setDate(dt);
        }

        // Last Rate
        Optional<String> lastRate = Optional.of(listing.getLastRate());

        if (lastRate.isPresent())
        {
            String p = lastRate.get().trim();
            long asPrice = asPrice(p);
            price.setValue(convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
        }

        // MarketVolume - OverallTurnOverUnits
        Optional<String> marketvolume = Optional.of(listing.getOverallTurnOverUnits());

        if (marketvolume.isPresent())
        {
            price.setVolume(Long.parseLong((String) marketvolume.get().trim()));
        }
        else
        {
            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

        }

        Optional<String> highrate = Optional.of(listing.getHighRate());
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


        Optional<String> lowrate = Optional.of(listing.getLowRate());
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

    @Override
    protected Optional<String> getQuoteCurrency(Security security)
    {
        return Optional.of(CURRENCY_CODE);
    }






    public Optional<SecurityHistory> getPriceHistoryChunkSec(Security security, LocalDate fromDate, LocalDate toDate,
                    int page,
                    Language lang) throws Exception
    {
        return getPriceHistoryChunkInternal(security.getWkn(), fromDate, toDate, page, TASESecurity.RECORDS, lang);
    }

    public Optional<SecurityHistory> getPriceHistoryChunk(Security security, LocalDate fromDate, LocalDate toDate,
                    int page,
                    Language lang) throws Exception
    {
        if ((security.getWkn() == null) || (security.getWkn().length() == 0))
            return Optional.empty();
        
        return getPriceHistoryChunkInternal(security.getWkn(), fromDate, toDate, page, TASESecurity.RECORDS, lang);
    }

    @SuppressWarnings({ "unchecked" })
    private Optional<SecurityHistory> getPriceHistoryChunkInternal(String securityId, LocalDate fromDate,
                    LocalDate toDate,
                    int page, int total_rec, 
                    Language lang)
    {
        int _page = (page == 0) ? 1 : page;

        JSONObject uploadData = new JSONObject();
        uploadData.put("dFrom", fromDate.toString()); //$NON-NLS-1$
        uploadData.put("dTo", toDate.toString()); //$NON-NLS-1$
        uploadData.put("oID", securityId); //$NON-NLS-1$
        uploadData.put("pageNum", Integer.toString(_page)); //$NON-NLS-1$
        uploadData.put("pType", Integer.toString(TYPE)); //$NON-NLS-1$
        uploadData.put("TotalRec", Integer.toString(RECORDS)); //$NON-NLS-1$
        uploadData.put("lang", Integer.toString(lang.getValue())); //$NON-NLS-1$

        try
        {
            String response = new WebAccess(URL, SECURITYHISTORYPATH) // $NON-NLS-1$
                                                                      // //$NON-NLS-2$
                            .addHeader("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                            .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$
                            .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                            .postReturn(JSONValue.toJSONString(uploadData));

            Optional<SecurityHistory> historyListing = Optional.of(SecurityHistory.fromJson(response));
            return historyListing;
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }
    }


    public Optional<SecurityHistory> getPriceHistory(Security security, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        if (toDate == null)
        {
            toDate = LocalDate.now();
        }
        if (fromDate == null)
        {
            fromDate = toDate.minusDays(1);
        }
        return getPriceHistoryChunkSec(security, fromDate, toDate, page, lang);
    }

}
