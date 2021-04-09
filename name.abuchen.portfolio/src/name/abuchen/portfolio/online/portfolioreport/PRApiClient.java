package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.util.WebAccess;

@SuppressWarnings("nls")
public class PRApiClient
{
    private static final String ENDPOINT = "https://api.portfolio-report.net";

    private CloseableHttpClient client;
    private Gson gson;

    public PRApiClient(String token)
    {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Authorization", "Bearer " + token));
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString()));

        this.client = HttpClientBuilder.create() //
                        .setDefaultRequestConfig(WebAccess.defaultRequestConfig) //
                        .setDefaultHeaders(headers) //
                        .setUserAgent("PortfolioPerformance/"
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString()) //
                        .useSystemProperties() //
                        .build();

        this.gson = new GsonBuilder() //
                        .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (instant, type,
                                        jsonSerializationContext) -> new JsonPrimitive(instant.toString()))
                        .registerTypeAdapter(Instant.class,
                                        (JsonDeserializer<Instant>) (json, type, jsonDeserializationContext) -> Instant
                                                        .parse(json.getAsJsonPrimitive().getAsString()))
                        .create();
    }

    public List<PRPortfolio> listPortfolios() throws IOException
    {
        return list(PRPortfolio.class, "/portfolios");
    }

    public PRPortfolio createPortfolio(PRPortfolio portfolio) throws IOException
    {
        return create(PRPortfolio.class, "/portfolios", portfolio);
    }

    public List<PRSecurity> listSecurities(long portfolioId) throws IOException
    {
        return list(PRSecurity.class, "/portfolios/" + portfolioId + "/securities");
    }

    public PRSecurity updateSecurity(long portfolioId, PRSecurity security) throws IOException
    {
        return update(PRSecurity.class, "/portfolios/" + portfolioId + "/securities/" + security.getUuid(), security);
    }

    public PRSecurity deleteSecurity(long portfolioId, PRSecurity security) throws IOException
    {
        return deleteEntity(PRSecurity.class, "/portfolios/" + portfolioId + "/securities/" + security.getUuid());
    }

    public List<PRAccount> listAccounts(long portfolioId) throws IOException
    {
        return list(PRAccount.class, "/portfolios/" + portfolioId + "/accounts");
    }

    public PRAccount updateAccount(long portfolioId, PRAccount account) throws IOException
    {
        return update(PRAccount.class, "/portfolios/" + portfolioId + "/accounts/" + account.getUuid(), account);
    }

    public PRAccount deleteAccount(long portfolioId, PRAccount account) throws IOException
    {
        return deleteEntity(PRAccount.class, "/portfolios/" + portfolioId + "/accounts/" + account.getUuid());
    }

    public List<PRTransaction> listTransactions(long portfolioId) throws IOException
    {
        return list(PRTransaction.class, "/portfolios/" + portfolioId + "/transactions");
    }

    public PRTransaction updateTransaction(long portfolioId, PRTransaction transaction) throws IOException
    {
        return update(PRTransaction.class, "/portfolios/" + portfolioId + "/transactions/" + transaction.getUuid(),
                        transaction);
    }

    public PRTransaction deleteTransaction(long portfolioId, PRTransaction transaction) throws IOException
    {
        return deleteEntity(PRTransaction.class,
                        "/portfolios/" + portfolioId + "/transactions/" + transaction.getUuid());
    }

    private <T> List<T> list(Class<T> type, String path) throws IOException
    {
        HttpGet request = new HttpGet(ENDPOINT + path);
        CloseableHttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            throw asError(request, response, null);

        return this.gson.fromJson(EntityUtils.toString(response.getEntity()),
                        TypeToken.getParameterized(List.class, type).getType());
    }

    private <T> T create(Class<T> type, String path, T input) throws IOException
    {
        HttpPost request = new HttpPost(ENDPOINT + path);
        request.setEntity(new StringEntity(this.gson.toJson(input), StandardCharsets.UTF_8));
        CloseableHttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED)
            throw asError(request, response, EntityUtils.toString(request.getEntity()));

        return this.gson.fromJson(EntityUtils.toString(response.getEntity()), type);
    }

    private <T> T update(Class<T> type, String path, T input) throws IOException
    {
        HttpPut request = new HttpPut(ENDPOINT + path);
        request.setEntity(new StringEntity(this.gson.toJson(input), StandardCharsets.UTF_8));

        CloseableHttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            throw asError(request, response, EntityUtils.toString(request.getEntity()));

        return this.gson.fromJson(EntityUtils.toString(response.getEntity()), type);
    }

    private <T> T deleteEntity(Class<T> type, String path) throws IOException
    {
        HttpDelete request = new HttpDelete(ENDPOINT + path);
        CloseableHttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            throw asError(request, response, null);

        return this.gson.fromJson(EntityUtils.toString(response.getEntity()), type);
    }

    private IOException asError(HttpRequestBase request, CloseableHttpResponse response, String requestBody)
                    throws IOException
    {
        return new IOException(request.toString() + " --> " + response.getStatusLine().getStatusCode() + "\n\n"
                        + (requestBody != null ? requestBody + "\n\n" : "")
                        + EntityUtils.toString(response.getEntity()));
    }
}
