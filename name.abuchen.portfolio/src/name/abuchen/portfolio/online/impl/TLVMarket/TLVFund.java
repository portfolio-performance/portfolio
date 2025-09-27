package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

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
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVFund extends TLVListing
{
    public static final int TYPE = 4;
    // private int period = 0;
    private final String URL = "mayaapi.tase.co.il"; //$NON-NLS-1$
    private final String PATH = "/api/fund/details"; //$NON-NLS-1$
    private final String CURRENCY_CODE = "ILS"; //$NON-NLS-1$
    // private Type FundListingType = new TypeToken<FundListing>()
    // {
    // }.getType();


    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        if (security.getWkn() == null || security.getWkn().isEmpty() || security.getWkn().isBlank())
            return Optional.empty();
        try
        {
            String response = this.rpcLatestQuoteFund(security);
            return convertResponseToSecurityPrice(response, security);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }

    }

    public Optional<QuoteFeedData> getHistoricalQuotes(Security security, boolean collectRawData)
    {
        if (security.getWkn() == null || security.getWkn().isEmpty() || security.getWkn().isBlank())
            return Optional.empty();

        LocalDate from = caculateStart(security);
        LocalDate to = LocalDate.now();

        try
        {
            Optional<FundHistory> fundHistory = getPriceHistoryChunkInternal(security, from, to, 1, Language.ENGLISH);

            Optional<QuoteFeedData> feed = convertFundHistoryToQuoteFeedData(fundHistory, security);
            return feed;
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }
    }

    public Optional<QuoteFeedData> convertFundHistoryToQuoteFeedData(Optional<FundHistory> historyopt,
                    Security security)
    {
        QuoteFeedData feed = new QuoteFeedData();
        Optional<String> quoteCurrency = getQuoteCurrency(security);
        LatestSecurityPrice price = null;

        if (historyopt.isEmpty())
            return Optional.empty();

        FundHistory history = historyopt.get();
        if ((history.getItems()).length == 0)
            return Optional.empty();

        FundHistoryEntry[] historyitemsarray = history.getItems();

        for (int i = 0; i < historyitemsarray.length; i++)
        {
            FundHistoryEntry entry = historyitemsarray[i];

            price = new LatestSecurityPrice();

            Optional<LocalDate> tradeDate = Optional.of(entry.getTradeDate());
            if (tradeDate.isPresent())
            {
                LocalDate temp = tradeDate.get();
                price.setDate(temp);
            }

            Optional<String> sellPrice = Optional.of(entry.getSellPrice());
            if (sellPrice.isPresent())
            {
                long priceL = asPrice(sellPrice.get());
                price.setValue(convertILS(priceL, quoteCurrency.orElse(null), security.getCurrencyCode()));
            }

            if (price.getDate() != null && price.getValue() > 0)
            {
                feed.addPrice(price);
            }
        }
        return Optional.of(feed);
    }



    public String rpcLatestQuoteFund(Security security) throws IOException
    {
        return rpcLatestQuoteFundWithLanguage(security, Language.ENGLISH);
    }

    public Optional<FundHistory> getPriceHistoryChunkInternal(Security security, LocalDate fromDate, LocalDate toDate,
                    int page, Language lang) throws Exception
    {
        return getPriceHistoryChunk(security, fromDate, toDate, page, lang);
    }

    public Optional<FundHistory> getPriceHistoryChunk(Security security, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang)
    {
        if (security.getWkn() == null)
            return Optional.empty();
        int _page = (page == 0) ? 1 : page;

        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("DateFrom", fromDate.toString())); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("DateTo", toDate.toString())); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("FundId", security.getWkn())); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("Page", Integer.toString(_page))); //$NON-NLS-1$
        formParams.add(new BasicNameValuePair("Period", Integer.toString(0))); //$NON-NLS-1$

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

            Optional<FundHistory> fundListing = Optional.of(FundHistory.fromJson(response));
            return fundListing;
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }
    }

    public Optional<FundHistory> getPriceHistory(Security security, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        if ((security.getWkn() == null) || (security.getWkn().length() == 0))
            return Optional.empty();

        if (toDate == null)
        {
            toDate = LocalDate.now();
        }
        if (fromDate == null)
        {
            fromDate = toDate.minusDays(1);
        }
        return getPriceHistoryChunk(security, fromDate, toDate, page, lang);
    }

    protected Optional<LatestSecurityPrice> convertResponseToSecurityPrice(String response, Security security)
    {

        class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime>
        {


            @Override
            public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
                            final JsonSerializationContext context)
            {
                return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            @Override
            public LocalDateTime deserialize(final JsonElement json, final Type typeOfT,
                            final JsonDeserializationContext context) throws JsonParseException
            {
                return LocalDateTime.parse(json.getAsString());
            }
        }
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();
        Optional<FundListing> jsonprice = Optional.of(gson.fromJson(response, FundListing.class));

        Optional<LatestSecurityPrice> price = convertFundListingToSecurityPrice(jsonprice, security);
        return price;

    }


    private Optional<LatestSecurityPrice> convertFundListingToSecurityPrice(Optional<FundListing> listingopt,
                    Security security)
    {
        LatestSecurityPrice price = new LatestSecurityPrice();
        Optional<String> quoteCurrency = getQuoteCurrency(security);

        if (listingopt.isEmpty())
            return Optional.empty();

        FundListing listing = listingopt.get();

        // TradeDate
        Optional<LocalDate> tradeDate = Optional.of(listing.getUnitValueValidDate());
        if (tradeDate.isPresent())
        {
            LocalDate dt = tradeDate.get();
            price.setDate(dt);
        }

        // Last Rate
        Optional<String> lastRate = Optional.of(listing.getUnitValuePrice());

        if (lastRate.isPresent())
        {
            String p = lastRate.get().trim();
            long asPrice = asPrice(p);
            price.setValue(convertILS(asPrice, quoteCurrency.orElse(null), security.getCurrencyCode()));
        }

        price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

        price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);

        price.setLow(LatestSecurityPrice.NOT_AVAILABLE);

        if (price.getDate() == null || price.getValue() <= 0)
        {
            return Optional.empty();
        }
        else
        {
            return Optional.of(price);
        }
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


    @Override
    protected Optional<String> getQuoteCurrency(Security security)
    {
        return Optional.of(this.CURRENCY_CODE);
    }

}
