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

    private String url;
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

    public WebAccess(String url)
    {
        this.url = Objects.requireNonNull(url.trim(), "PP WebAccess: rawURL null"); //$NON-NLS-1$
    }

    public WebAccess withAddress(String host, String path)
    {
        this.host = Objects.requireNonNull(host.trim(), "PP WebAccess: host is null"); //$NON-NLS-1$
        this.path = Objects.requireNonNull(path.trim(), "PP WebAccess: path is null"); //$NON-NLS-1$
        return this;
    }

    public WebAccess withScheme(String scheme)
    {
        this.scheme = Objects.requireNonNull(scheme.trim(), "PP WebAccess: scheme is null"); //$NON-NLS-1$
        return this;
    }

    public WebAccess addParameter(String param, String value)
    {
        param = Objects.requireNonNull(param.trim(), "PP WebAccess: Parameter is null"); //$NON-NLS-1$
        value = Objects.requireNonNull(value.trim(), "PP WebAccess: Value Agent is null"); //$NON-NLS-1$
        this.parameters.add(new BasicNameValuePair(param, value));
        return this;
    }

    public WebAccess addHeader(String param, String value)
    {
        param = Objects.requireNonNull(param.trim(), "PP WebAccess: Header: Parameter is null"); //$NON-NLS-1$
        value = Objects.requireNonNull(value.trim(), "PP WebAccess: Header: Value Agent is null"); //$NON-NLS-1$
        this.headers.add(new BasicHeader(param, value));
        return this;
    }

    public WebAccess addUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
        return this;
    }

    public String get() throws IOException
    {

        String errorURL = null;
        CloseableHttpResponse response = null;

        try
        {
            CloseableHttpClient client = HttpClientBuilder.create() //
                            .setDefaultRequestConfig(defaultRequestConfig) //
                            .setDefaultHeaders(this.headers) //
                            .setUserAgent(this.userAgent).build();

            if (this.url != null && !this.url.isEmpty())
            {
                response = client.execute(new HttpGet(url));
                errorURL = this.url;
            }
            else
            {
                URIBuilder uriBuilder = new URIBuilder().setScheme(this.scheme).setHost(this.host).setPath(this.path);
                uriBuilder.setParameters(this.parameters);
                URL objectURL = uriBuilder.build().toURL();
                response = client.execute(new HttpGet(objectURL.toString()));
                errorURL = objectURL.toString();
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException(errorURL + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$
            return EntityUtils.toString(response.getEntity());
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }
}
