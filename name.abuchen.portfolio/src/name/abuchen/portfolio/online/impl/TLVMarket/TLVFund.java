package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVFund
{
    public static final int TYPE = 4;
    private int period = 0;
    private final String URL = "mayaapi.tase.co.il"; //$NON-NLS-1$
    private final String PATH = "/api/fund/details"; //$NON-NLS-1$

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

    @SuppressWarnings("nls")
    @VisibleForTesting
    public String rpcLatestQuoteFund(Security security) throws IOException
    {
        String response = new WebAccess(URL, PATH)
                        .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001")
                        .addParameter("lang", "1").addParameter("fundId", security.getWkn())
                        .addHeader("referer", "https://www.tase.co.il/").addHeader("Cache-Control", "no-cache")
                        .addHeader("X-Maya-With", "allow").addHeader("Accept-Language", "en-US").get();
        return response;
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
