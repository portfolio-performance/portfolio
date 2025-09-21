package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVSecurity extends TLVListing
{
    
    private static final int TYPE = 8;
    private static final int RECORDS = 1;

    private final String URL = "api.tase.co.il"; //$NON-NLS-1$
    private final String SECURITYDATAPATH = "/api/company/securitydata"; //$NON-NLS-1$
    private final String SECURITYHISTORYPATH = "/api/security/historyeod"; //$NON-NLS-1$
    private Type SecurityListingType = new TypeToken<SecurityListing>()
    {
    }.getType();

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
                            TLVSecurity.RECORDS, Language.ENGLISH);

            Optional<QuoteFeedData> feed = convertSecurityHistoryToQuoteFeedData(securityHistory, security);
            return feed;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
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
                price.setDate(TLVHelper.asDate(tradeDate.get()));

            Optional<String> closePrice = Optional.of(entry.getCloseRate());
            if (closePrice != null && closePrice.isPresent())
            {
                long priceL = TLVHelper.asPrice(closePrice.get());
                price.setValue(TLVHelper.convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            String highRate = entry.getHighRate();
            if (highRate != null)
            {
                long priceL = TLVHelper.asPrice(highRate);
                price.setHigh(TLVHelper.convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            String lowRate = entry.getLowRate();
            if (lowRate != null)
            {

                long priceL = TLVHelper.asPrice(lowRate);
                price.setLow(TLVHelper.convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            Optional<String> volume = Optional.of(entry.getOverallTurnOverUnits());
            if (volume != null && volume.isPresent())
            {
                long priceL = TLVHelper.asLong(volume.get());
                price.setVolume(priceL);
            }

            if (price.getDate() != null && price.getValue() > 0)
            {
                feed.addPrice(price);
            }
        }
        return Optional.of(feed);

    }

    // private void DoNoUseFoNow()
    // {
    //
    // // return TLVHelper.ObjectToMap(securityHistory);
    //
    // JSONObjet jsonObject = (JSONObject) parser.parse(pricehistory);
    // try
    // {
    //
    // JSONObject parentObject = (JSONObject) jsonObject.get("Table");
    // //$NON-NLS-1$
    //
    // if (parentObject != null)
    // {
    // /*
    // * "FundId": null, "TradeDate": "2025-08-25T00:00:00",
    // * "LastUpdateDate": "0001-01-01T00:00:00", "PurchasePrice":
    // * 146.88, "SellPrice": 146.88, "CreationPrice": null,
    // * "DayYield": 0.04, "ShowDayYield": true, "Rate": 0,
    // * "ManagmentFee": 0.25, "TrusteeFee": 0.025, "SuccessFee":
    // * null, "AssetValue": 130.3
    // */
    // Set<String> keys = parentObject.keySet();
    // //
    // // Iterate over the keys
    // Iterator<String> iterator = keys.iterator();
    // while (iterator.hasNext())
    // {
    // String key = iterator.next();
    // Object value = parentObject.get(key);
    // }
    // }
    // }
    // catch (ClassCastException e)
    // {
    // JSONArray parentObject = (JSONArray) jsonObject.get("Table");
    // //$NON-NLS-1$
    //
    // for (int i = 0; i < parentObject.size(); i++)
    // {
    // JSONObject item = (JSONObject) parentObject.get(i);
    //
    // String strTradeDate = (String) item.get("TradeDate"); //$NON-NLS-1$
    // LocalDate tradedate = null;
    // if (strTradeDate.length() > 0)
    // {
    // tradedate = TLVHelper.asDateTime(strTradeDate); // $NON-NLS-1$
    // }
    // long curvalue = DoubletoLong(item, "SellPrice", quoteCurrency,
    // security.getCurrencyCode()); //$NON-NLS-1$
    //
    // LatestSecurityPrice curprice = new LatestSecurityPrice(tradedate,
    // curvalue);
    // historicalprices.addPrice(curprice);
    //
    // }
    // return historicalprices;
    // }
    // catch (Exception e)
    // {
    // System.out.println(e.getMessage());
    // }
    // // Handle Bonds
    // JSONArray ItemsObject = (JSONArray) jsonObject.get("Items");
    // //$NON-NLS-1$
    //
    // if (ItemsObject != null)
    // {
    // // System.out.println("Price history Items " + ItemsObject);
    // // //$NON-NLS-1$
    //
    // // Iterate over the keys
    // for (int i = 0; i < ItemsObject.size(); i++)
    // {
    // JSONObject item = (JSONObject) ItemsObject.get(i);
    // LocalDate curdate = LocalDate.parse((String) item.get("TradeDate"),
    // this.formatter); //$NON-NLS-1$
    //
    // // Rates from history are in ILS. Need to convert to ILA
    // long curvalue = DoubletoLong(item, "CloseRate", quoteCurrency,
    // security.getCurrencyCode()); //$NON-NLS-1$
    // // Double r = null;
    // // Long curvalue = null;
    // // Double closerate = (Double) item.get("CloseRate");
    //
    // //
    // // try
    // // {
    // // r = (Double) item.get("CloseRate");
    // // }
    // // catch (IndexOutOfBoundsException e)
    // // {
    // // // Ignore
    // // }
    // // if (r != null && r.doubleValue() > 0)
    // // {
    // // long convertedprice =
    // // convertILS(Values.Quote.factorize(roundQuoteValue(r)),
    // // quoteCurrency.orElse(null),
    // // security.getCurrencyCode());
    // // curvalue = convertedprice;
    // // }
    // // else
    // // {
    // // curvalue = LatestSecurityPrice.NOT_AVAILABLE;
    // // }
    // //
    //
    // LatestSecurityPrice curprice = new LatestSecurityPrice(curdate,
    // curvalue);
    //
    // // long lowvalue = convertILSToILA((Double)
    // // item.get("LowtRate")); //$NON-NLS-1$
    // long lowvalue = DoubletoLong(item, "LowtRate", quoteCurrency,
    // security.getCurrencyCode()); //$NON-NLS-1$
    //
    // // long highvalue = convertILSToILA((Double)
    // // item.get("HighRate")); //$NON-NLS-1$
    // long highvalue = DoubletoLong(item, "HighRate", quoteCurrency,
    // security.getCurrencyCode()); //$NON-NLS-1$
    //
    // curprice.setHigh(highvalue);
    // curprice.setLow(lowvalue);
    // historicalprices.addPrice(curprice);
    // // System.out.println("Date " + curdate + " Value: " +
    // // curvalue); //$NON-NLS-1$ //$NON-NLS-2$
    // // System.out.println("High " + highvalue + " Low: " +
    // // highvalue); //$NON-NLS-1$ //$NON-NLS-2$
    // }
    //
    // }
    // return historicalprices;
    // }
    // catch (Exception e)
    // {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // return historicalprices;
    // }
    //
    // }

        // public Map<String, Object> getPriceHistoryChunk2(Security security,
        // LocalDate fromDate, LocalDate toDate, int page,
        // Language lang) throws Exception
        // {
        // TLVType securityType = this.getSecurityType(security.getWkn());
        // if (securityType != TLVType.NONE)
        // {
        // if (securityType == TLVType.SECURITY)
        // {
        // SecurityHistory securityHistory =
        // this.TLVSecurities.getPriceHistoryChunkSec(security, fromDate,
        // toDate,
        // page, lang);
        // return TLVHelper.ObjectToMap(securityHistory);
        // }
        // if (securityType == TLVType.FUND)
        // {
        // FundHistory fundHistory =
        // this.TLVFunds.getPriceHistoryChunkSec(security, fromDate, toDate,
        // page, lang);
        // return TLVHelper.ObjectToMap(fundHistory);
        // }
        // return Collections.emptyMap();
        // }
        // else
        // {
        // return Collections.emptyMap();
        // }
        //
        // }

        private LocalDate asDate(String s)
        {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

            if ("\"N/A\"".equals(s)) //$NON-NLS-1$
                return null;
            String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
            return LocalDate.parse(dt, formatter); // $NON-NLS-1$
        }

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
            long asPrice = TLVHelper.asPrice(p);
            price.setValue(TLVHelper.convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
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
            long asPrice = TLVHelper.asPrice(p);
            price.setHigh(TLVHelper.convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
        }
        else
        {
            price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
        }


        Optional<String> lowrate = Optional.of(listing.getLowRate());
        if (lowrate.isPresent())
        {
            String p = lowrate.get().trim();
            long asPrice = TLVHelper.asPrice(p);
            price.setLow(TLVHelper.convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
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

    private Optional<String> getQuoteCurrency(Security security)
    {
        return Optional.of("ILA"); //$NON-NLS-1$
    }

    private class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>
    {

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/YYYY"); //$NON-NLS-1$

        @Override
        public JsonElement serialize(final LocalDate date, final Type typeOfSrc, final JsonSerializationContext context)
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

    // TODO: convert to new format
    public Map<String, String> getNames(SecurityListing englishDetails, SecurityListing hebrewDetails)
    {

        Map<String, String> names = new HashMap<>();
        names.getOrDefault(names, null);
        names.put("english_short_name", (String) englishDetails.Name); //$NON-NLS-1$
        names.put("english_long_name", //$NON-NLS-1$
                        (String) englishDetails.getOrDefault("LongName", englishDetails.SecurityLongName)); //$NON-NLS-1$

        names.put("hebrew_short_name", (String) hebrewDetails.Name); //$NON-NLS-1$
        names.put("hebrew_long_name", (String) hebrewDetails.getOrDefault("LongName", hebrewDetails.SecurityLongName)); //$NON-NLS-1$ //$NON-NLS-2$
        return names;
    }

    // TODO: convert to new format
    public SecurityListing getDetails(Security security, Language lang) throws Exception
    {
        String responseString = this.rpcLatestQuoteSecuritywithLanguage(security, lang);
        Gson gson = new Gson();
        SecurityListing securityListing = gson.fromJson(responseString, this.SecurityListingType);


        return securityListing;
    }




    public Optional<SecurityHistory> getPriceHistoryChunkSec(Security security, LocalDate fromDate, LocalDate toDate,
                    int page,
                    Language lang) throws Exception
    {
        return getPriceHistoryChunkInternal(security.getWkn(), fromDate, toDate, page, TLVSecurity.RECORDS, lang);
    }

    public Optional<SecurityHistory> getPriceHistoryChunk(Security security, LocalDate fromDate, LocalDate toDate,
                    int page,
                    Language lang) throws Exception
    {
        if ((security.getWkn() == null) || (security.getWkn().length() == 0))
            return Optional.empty();
        
        return getPriceHistoryChunkInternal(security.getWkn(), fromDate, toDate, page, TLVSecurity.RECORDS, lang);
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

        // System.err.println(JSONValue.toJSONString(uploadData));
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

            Gson gson = GSONUtil.createGson();
            Optional<SecurityHistory> historyListing = Optional.of(gson.fromJson(response, SecurityHistory.class));
            return historyListing;
        }
        catch (Exception e)
        {
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

    // Yahoo Function - not in use
    // public Optional<String> extract(String body, int startIndex, String
    // startToken, String endToken)
    // {
    // int begin = body.indexOf(startToken, startIndex);
    //
    // if (begin < 0)
    // return Optional.empty();
    //
    // int end = body.indexOf(endToken, begin + startToken.length());
    // if (end < 0)
    // return Optional.empty();
    //
    // return Optional.of(body.substring(begin + startToken.length(), end));
    // }
    //
    public static Map<String, Object> ObjectToMap(Object listing)
    {
        Gson gson = GSONUtil.createGson();
        String json = gson.toJson(listing);
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>()
        {
        }.getType());
        return map;
    }
}
