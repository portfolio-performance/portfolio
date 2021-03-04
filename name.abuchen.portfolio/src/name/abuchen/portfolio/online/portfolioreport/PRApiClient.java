package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.util.WebAccess;

public class PRApiClient
{
    private static final String ENDPOINT = "https://api.portfolio-report.net"; //$NON-NLS-1$

    private CloseableHttpClient client;

    public PRApiClient(String token)
    {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Authorization", "Bearer " + token)); //$NON-NLS-1$ //$NON-NLS-2$
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString()));

        this.client = HttpClientBuilder.create() //
                        .setDefaultRequestConfig(WebAccess.defaultRequestConfig) //
                        .setDefaultHeaders(headers) //
                        .setUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString()) //
                        .useSystemProperties() //
                        .build();

    }

    public List<PRPortfolio> listPortfolios() throws IOException
    {
        return list(PRPortfolio.class, "/portfolios"); //$NON-NLS-1$
    }

    public PRPortfolio createPortfolio(PRPortfolio portfolio) throws IOException
    {
        return create(PRPortfolio.class, "/portfolios", portfolio); //$NON-NLS-1$
    }

    public List<PRSecurity> listSecurities(long portfolioId) throws IOException
    {
        return list(PRSecurity.class, "/portfolios/" + portfolioId + "/securities"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public PRSecurity createSecurity(long portfolioId, PRSecurity security) throws IOException
    {
        return create(PRSecurity.class, "/portfolios/" + portfolioId + "/securities", security); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private <T> List<T> list(Class<T> type, String path) throws IOException
    {
        HttpGet request = new HttpGet(ENDPOINT + path);
        CloseableHttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            throw asError(request, response);

        return JClient.GSON.fromJson(EntityUtils.toString(response.getEntity()),
                        TypeToken.getParameterized(List.class, type).getType());
    }

    private <T> T create(Class<T> type, String path, T input) throws IOException
    {
        HttpPost request = new HttpPost(ENDPOINT + path);
        request.setEntity(new StringEntity(JClient.GSON.toJson(input)));
        CloseableHttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED)
            throw asError(request, response);

        return JClient.GSON.fromJson(EntityUtils.toString(response.getEntity()), type);
    }

    private IOException asError(HttpRequestBase request, CloseableHttpResponse response) throws IOException
    {
        return new IOException(request.toString() + " --> " + response.getStatusLine().getStatusCode() + "\n\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + EntityUtils.toString(response.getEntity()));
    }

}
