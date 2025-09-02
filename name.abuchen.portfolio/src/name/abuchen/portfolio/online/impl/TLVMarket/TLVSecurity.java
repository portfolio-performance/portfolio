package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVSecurity
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

    public String getLatestQuote(Security security)
    {
        try
        {
        return this.rpcLatestQuoteSecurity(security);
        }
        catch (IOException e)
        {
            JSONObject quote = new JSONObject();
            return quote.toString();
        }
    }

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

    public SecurityListing getDetails(Security security, Language lang) throws Exception
    {
        String responseString = this.rpcLatestQuoteSecuritywithLanguage(security, lang);
        Gson gson = new Gson();
        SecurityListing securityListing = gson.fromJson(responseString, this.SecurityListingType);


        return securityListing;
    }




    public SecurityHistory getPriceHistoryChunkSec(Security security, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        return getPriceHistoryChunkInternal(security.getWkn(), fromDate, toDate, page, this.TYPE, this.RECORDS, lang);
    }

    public SecurityHistory getPriceHistoryChunk(String securityId, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        return getPriceHistoryChunkInternal(securityId, fromDate, toDate, page, this.TYPE, this.RECORDS, lang);
    }

    @SuppressWarnings({ "unchecked" })
    private SecurityHistory getPriceHistoryChunkInternal(String securityId, LocalDate fromDate, LocalDate toDate,
                    int page, int p_type, int total_rec, 
                    Language lang) throws Exception
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

        String response = new WebAccess(URL, SECURITYHISTORYPATH) // $NON-NLS-1$
                                                                  // //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$
                        .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                        .postReturn(JSONValue.toJSONString(uploadData));

        Gson gson = GSONUtil.createGson();
        SecurityHistory historyListing = gson.fromJson(response, SecurityHistory.class);
        return historyListing;

    }


    public SecurityHistory getPriceHistory(Security security, LocalDate fromDate, LocalDate toDate, int page,
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

    // Yahoo Function
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
