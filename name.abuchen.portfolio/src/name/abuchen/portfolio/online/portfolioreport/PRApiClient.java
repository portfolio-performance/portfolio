package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.util.WebAccess;

@SuppressWarnings({ "nls", "restriction" })
public class PRApiClient
{
    private String endpoint;

    private CloseableHttpClient client;
    private Gson gson;

    public PRApiClient(String endpoint, String token)
    {
        this.endpoint = endpoint == null ? "https://api.portfolio-report.net" : endpoint;

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
                        .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe()) //
                        .create();
    }

    private static final class LocalDateAdapter extends TypeAdapter<LocalDate>
    {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException
        {
            jsonWriter.value(localDate.toString());
        }

        @Override
        public LocalDate read(final JsonReader jsonReader) throws IOException
        {
            return LocalDate.parse(jsonReader.nextString());
        }
    }

    public List<PRPortfolio> listPortfolios() throws IOException
    {
        return list(PRPortfolio.class, "/portfolios/");
    }

    public PRPortfolio createPortfolio(PRPortfolio portfolio) throws IOException
    {
        return create(PRPortfolio.class, "/portfolios/", portfolio);
    }

    public List<PRSecurity> listSecurities(long portfolioId) throws IOException
    {
        return list(PRSecurity.class, "/portfolios/" + portfolioId + "/securities/");
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
        return list(PRAccount.class, "/portfolios/" + portfolioId + "/accounts/");
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
        return list(PRTransaction.class, "/portfolios/" + portfolioId + "/transactions/");
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
        HttpGet request = new HttpGet(endpoint + path);
        CloseableHttpResponse response = client.execute(request);

        if (response.getCode() != HttpStatus.SC_OK)
            throw asError(request, response, null);

        return this.gson.fromJson(toString(response.getEntity()),
                        TypeToken.getParameterized(List.class, type).getType());
    }

    private <T> T create(Class<T> type, String path, T input) throws IOException
    {
        HttpPost request = new HttpPost(endpoint + path);
        request.setEntity(new StringEntity(this.gson.toJson(input), StandardCharsets.UTF_8));
        CloseableHttpResponse response = client.execute(request);

        if (response.getCode() != HttpStatus.SC_CREATED)
            throw asError(request, response, toString(request.getEntity()));

        return this.gson.fromJson(toString(response.getEntity()), type);
    }

    private <T> T update(Class<T> type, String path, T input) throws IOException
    {
        HttpPut request = new HttpPut(endpoint + path);
        request.setEntity(new StringEntity(this.gson.toJson(input), StandardCharsets.UTF_8));

        CloseableHttpResponse response = client.execute(request);

        if (response.getCode() != HttpStatus.SC_OK)
            throw asError(request, response, toString(request.getEntity()));

        return this.gson.fromJson(toString(response.getEntity()), type);
    }

    private <T> T deleteEntity(Class<T> type, String path) throws IOException
    {
        HttpDelete request = new HttpDelete(endpoint + path);
        CloseableHttpResponse response = client.execute(request);

        if (response.getCode() != HttpStatus.SC_OK)
            throw asError(request, response, null);

        return this.gson.fromJson(toString(response.getEntity()), type);
    }

    private IOException asError(HttpUriRequestBase request, CloseableHttpResponse response, String requestBody)
                    throws IOException
    {
        return new IOException(request.toString() + " --> " + response.getCode() + "\n\n"
                        + (requestBody != null ? requestBody + "\n\n" : "") + toString(response.getEntity()));
    }

    private String toString(HttpEntity httpEntity) throws IOException
    {
        try
        {
            return EntityUtils.toString(httpEntity);
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }
}
