package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
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
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVFund
{
    public static final int TYPE = 4;
    private int period = 0;
    private final String URL = "mayaapi.tase.co.il"; //$NON-NLS-1$
    private final String PATH = "/api/fund/details"; //$NON-NLS-1$
    private Type FundListingType = new TypeToken<FundListing>()
    {
    }.getType();

    public String getLatestQuote(Security security)
    {
        try
        {
            return this.rpcLatestQuoteFund(security);
        }
        catch (IOException e)
        {
            JSONObject quote = new JSONObject();
            return quote.toString();
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

    public FundListing getDetails(Security security, Language lang) throws Exception
    {
        String responseString = rpcLatestQuoteFundWithLanguage(security, lang);
        Gson gson = new Gson();
        FundListing listing = gson.fromJson(responseString, this.FundListingType);

        return listing;
    }

    public FundHistory getPriceHistoryChunkSec(Security security, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        return getPriceHistoryChunk(security.getWkn(), fromDate, toDate, page, lang);
    }
    @SuppressWarnings("unchecked")
    public FundHistory getPriceHistoryChunk(String securityId, LocalDate fromDate, LocalDate toDate, int page,
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

        String response = new WebAccess("mayaapi.tase.co.il", "/api/fund/history") //$NON-NLS-1$ //$NON-NLS-2$
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$
                        .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/x-www-form-urlencoded") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("X-Maya-With", "allow") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Accept-Language", "en-US") //$NON-NLS-1$ //$NON-NLS-2$
                        .postUrlEncoding(formParams);

        Gson gson = GSONUtil.createGson();
        FundHistory fundListing = gson.fromJson(response, FundHistory.class);
        return fundListing;

    }

    public FundHistory getPriceHistory(String securityId, LocalDate fromDate, LocalDate toDate, int page, Language lang)
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
