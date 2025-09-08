package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;

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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVFund extends TLVListing
{
    public static final int TYPE = 4;
    private int period = 0;
    private final String URL = "mayaapi.tase.co.il"; //$NON-NLS-1$
    private final String PATH = "/api/fund/details"; //$NON-NLS-1$
    private final String CURRENCY_CODE = "ILS"; //$NON-NLS-1$
    private Type FundListingType = new TypeToken<FundListing>()
    {
    }.getType();


    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        try
        {
            String response = this.rpcLatestQuoteFund(security);
            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter()).create();
            Optional<LatestSecurityPrice> price = Optional.of(gson.fromJson(response, LatestSecurityPrice.class));
            return price;
        }
        catch (IOException e)
        {
            return Optional.empty();
        }

    }

    public class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>
    {

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$

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

    public Optional<QuoteFeedData> getHistoricalQuotes(Security security, boolean collectRawData)
    {
        LocalDate from = caculateStart(security);
        LocalDate to = LocalDate.now();
        try
        {
            Optional<FundHistory> fundHistory = getPriceHistoryChunkInternal(security, from, to, 1,
                            Language.ENGLISH);

            Optional<QuoteFeedData> feed = convertFundHistoryToQuoteFeedData(fundHistory, security);
            return feed;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            return Optional.empty();
        }
    }

    @VisibleForTesting
    public Optional<QuoteFeedData> convertFundHistoryToQuoteFeedData(Optional<FundHistory> historyopt,
                    Security security)
    {
        QuoteFeedData feed = new QuoteFeedData();
        Optional<String> quoteCurrency = getQuoteCurrency(security);

        if (historyopt.isEmpty())
            return Optional.empty();

        FundHistory history = historyopt.get();

        // TODO
        // public FundHistoryEntry[] Table;
        // public int Total;
        // private LocalDateTime StartDate;
        // private LocalDateTime EndDate;

        // private final List<LatestSecurityPrice> prices = new ArrayList<>();
        // private final List<Exception> errors = new ArrayList<>();
        // private final List<RawResponse> responses = new ArrayList<>();

        FundHistoryEntry[] historyitemsarray = history.getItems();

        if (historyitemsarray.length > 0)
        {
            for (int i = 0; i < historyitemsarray.length; i++)
            {
                FundHistoryEntry entry = historyitemsarray[i];

                LatestSecurityPrice price = new LatestSecurityPrice();
                price.setDate(entry.getTradeDate().toLocalDate());

                long highval = convertILS(Values.Quote.factorize(roundQuoteValue(entry.getSellPrice())),
                                quoteCurrency.orElse(null), security.getCurrencyCode());
                price.setHigh(highval);

                long lowval = convertILS(Values.Quote.factorize(roundQuoteValue(entry.getPurchasePrice())),
                                quoteCurrency.orElse(null), security.getCurrencyCode());
                price.setLow(lowval);

                long curval = convertILS(Values.Quote.factorize(roundQuoteValue(entry.getAssetValue())),
                                quoteCurrency.orElse(null), security.getCurrencyCode());
                price.setValue(curval);

                price.setVolume((long) entry.getAssetValue());
                feed.addPrice(price);
            }
            return Optional.of(feed);
        }
        else
        {
            return Optional.empty();
        }
    }

    public Map<String, String> getNames(FundListing englishDetails, FundListing hebrewDetails)
    {

        Map<String, String> names = new HashMap<>();
        names.getOrDefault(names, null);
        names.put("english_short_name", (String) englishDetails.FundLongName); //$NON-NLS-1$
        names.put("english_long_name", (String) englishDetails.FundLongName); //$NON-NLS-1$

        names.put("hebrew_short_name", (String) hebrewDetails.FundShortName); //$NON-NLS-1$
        names.put("hebrew_long_name", (String) hebrewDetails.FundShortName); //$NON-NLS-1$
        return names;
    }

    public String rpcLatestQuoteFund(Security security) throws IOException
    {
        return rpcLatestQuoteFundWithLanguage(security, Language.ENGLISH);
    }

    @SuppressWarnings("nls")
    @VisibleForTesting
    private String rpcLatestQuoteFundWithLanguage(Security security, Language lang) throws IOException
    {
        String response = new WebAccess(URL, PATH)
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001")
                        .addParameter("lang", "1").addParameter("fundId", security.getWkn())
                        .addHeader("referer", "https://www.tase.co.il/").addHeader("Cache-Control", "no-cache")
                        .addHeader("X-Maya-With", "allow").addHeader("Accept-Language", "en-US").get();
        return response;
    }

    // TODO: getDetails change to Optional
    public FundListing getDetails(Security security, Language lang) throws Exception
    {
        String responseString = rpcLatestQuoteFundWithLanguage(security, lang);
        Gson gson = new Gson();
        FundListing listing = gson.fromJson(responseString, this.FundListingType);

        return listing;
    }

    private Optional<String> getQuoteCurrency(Security security)
    {
        return Optional.of(this.CURRENCY_CODE);
    }

    public Optional<FundHistory> getPriceHistoryChunkInternal(Security security, LocalDate fromDate, LocalDate toDate,
                    int page,
                    Language lang) throws Exception
    {
        return getPriceHistoryChunk(security.getWkn(), fromDate, toDate, page, lang);
    }

    @SuppressWarnings("unchecked")
    public Optional<FundHistory> getPriceHistoryChunk(String securityId, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        int _page = (page == 0) ? 1 : page;

        JSONObject uploadData = new JSONObject();
        uploadData.put("DateFrom", fromDate.toString()); //$NON-NLS-1$
        uploadData.put("DateTo", toDate.toString()); //$NON-NLS-1$
        uploadData.put("FundId", securityId); //$NON-NLS-1$
        uploadData.put("Page", Integer.toString(_page)); //$NON-NLS-1$
        uploadData.put("Period", Integer.toString(period)); //$NON-NLS-1$

        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("DateFrom", fromDate.toString())); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("DateTo", toDate.toString())); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("FundId", securityId)); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("Page", Integer.toString(page))); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("Period", Integer.toString(0))); //$NON-NLS-1$
        // httpPost.setEntity(new UrlEncodedFormEntity(formParams,
        // ContentType.APPLICATION_FORM_URLENCODED));

        // System.err.println(JSONValue.toJSONString(uploadData));
        try
        {
            String response = new WebAccess("mayaapi.tase.co.il", "/api/fund/history") //$NON-NLS-1$ //$NON-NLS-2$
                            .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$
                            .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("Content-Type", "application/x-www-form-urlencoded") //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("X-Maya-With", "allow") //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("Accept-Language", "en-US") //$NON-NLS-1$ //$NON-NLS-2$
                            .postUrlEncoding(formParams);

            Gson gson = GSONUtil.createGson();
            Optional<FundHistory> fundListing = Optional.of(gson.fromJson(response, FundHistory.class));
            return fundListing;
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    // TODO: Change to Optional
    public Optional<FundHistory> getPriceHistory(String securityId, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang)
                    throws Exception
    {
        if (toDate == null)
        {
            toDate = LocalDate.now();
        }
        if (fromDate == null)
        {
            fromDate = toDate.minusDays(1);
        }
        return getPriceHistoryChunk(securityId, fromDate, toDate, page, lang);
    }

    // Yahoo Function - not in use
    public Optional<String> extract(String body, int startIndex, String startToken, String endToken)
    {
        int begin = body.indexOf(startToken, startIndex);

        if (begin < 0)
            return Optional.empty();

        int end = body.indexOf(endToken, begin + startToken.length());
        if (end < 0)
            return Optional.empty();

        return Optional.of(body.substring(begin + startToken.length(), end));
    }

    // TODO - No in use
    public static Map<String, Object> ObjectToMap(FundListing listing)
    {
        Gson gson = GSONUtil.createGson();
        String json = gson.toJson(listing);
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>()
        {
        }.getType());
        return map;
    }
}
