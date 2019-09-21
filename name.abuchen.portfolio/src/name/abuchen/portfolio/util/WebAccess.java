package name.abuchen.portfolio.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class WebAccess
{
    public static final RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(20000)
                    .setConnectTimeout(2000).setConnectionRequestTimeout(20000).setCookieSpec(CookieSpecs.STANDARD)
                    .build();

    private String scheme = "https"; //$NON-NLS-1$
    private String host;
    private String path;
    private String userAgent = OnlineHelper.getUserAgent();
    private List<Header> headers = new ArrayList<>();
    private List<NameValuePair> parameters = new ArrayList<>();

    public WebAccess(String host, String path)
    {
        if (Objects.nonNull(host) && Objects.nonNull(path))
        {
            this.host = host;
            this.path = path;
        }
    }

    public WebAccess withScheme(String scheme)
    {
        if (Objects.nonNull(scheme))
            this.scheme = scheme;
        return this;
    }

    public WebAccess addParameter(String param, String value)
    {
        if (Objects.nonNull(param) && Objects.nonNull(value))
        {
            this.parameters.add(new BasicNameValuePair(param, value));
        }
        return this;
    }

    public WebAccess addHeader(String param, String value)
    {
        if (Objects.nonNull(param) && Objects.nonNull(value))
        {
            this.headers.add(new BasicHeader(param, value));
        }
        return this;
    }

    public WebAccess addUserAgent(String userAgent)
    {
        if (Objects.nonNull(userAgent))
            this.userAgent = userAgent;
        return this;
    }

    public String get() throws IOException
    {
        CloseableHttpClient client = HttpClientBuilder.create() //
                        .setDefaultRequestConfig(defaultRequestConfig) //
                        .setDefaultHeaders(this.headers) //
                        .setUserAgent(this.userAgent).build();

        URIBuilder uriBuilder = new URIBuilder().setScheme(scheme).setHost(host).setPath(path);
        uriBuilder.setParameters(this.parameters);

        try
        {
            URL objectURL = uriBuilder.build().toURL();
            CloseableHttpResponse response = client.execute(new HttpGet(objectURL.toString()));
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException(objectURL.toString() + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

            return EntityUtils.toString(response.getEntity());
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }

}
