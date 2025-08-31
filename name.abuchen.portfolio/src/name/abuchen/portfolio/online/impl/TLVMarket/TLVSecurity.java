package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;
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

    @SuppressWarnings("nls")
    @VisibleForTesting
    public String rpcLatestQuoteSecurity(Security security) throws IOException
    {

        String response = new WebAccess(URL, SECURITYDATAPATH) // $NON-NLS-1$
                                                               // //$NON-NLS-2$
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("securityId", security.getWkn())
                        .addParameter("lang", Language.ENGLISH.toString())
                        .get();
        return response;
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

    @SuppressWarnings({ "unchecked" })
    public SecurityHistory getPriceHistoryChunk(String securityId, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        int _page = (page == 0) ? 1 : page;

        JSONObject uploadData = new JSONObject();
        uploadData.put("dFrom", fromDate.toString()); //$NON-NLS-1$
        uploadData.put("dTo", toDate.toString()); //$NON-NLS-1$
        uploadData.put("oID", securityId);
        uploadData.put("pageNum", Integer.toString(_page)); //$NON-NLS-1$
        uploadData.put("pType", Integer.toString(TYPE)); //$NON-NLS-1$
        uploadData.put("TotalRec", Integer.toString(RECORDS)); //$NON-NLS-1$
        uploadData.put("lang", Integer.toString(lang.getValue())); //$NON-NLS-1$

        // System.err.println(JSONValue.toJSONString(uploadData));

        String response = new WebAccess(URL, SECURITYHISTORYPATH) // $NON-NLS-1$
                                                                  // //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                        .postReturn(JSONValue.toJSONString(uploadData));

        Gson gson = GSONUtil.createGson();
        SecurityHistory historyListing = gson.fromJson(response, SecurityHistory.class);
        return historyListing;

    }

    public SecurityHistory getPriceHistory(String securityId, LocalDate fromDate, LocalDate toDate, int page,
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

}
