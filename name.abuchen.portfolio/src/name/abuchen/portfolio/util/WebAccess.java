package name.abuchen.portfolio.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
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
    public final static RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(600000)
                    .setConnectTimeout(600000).setConnectionRequestTimeout(600000).build();

    private String scheme;
    private String host;
    private String path;
    private String document;
    private List<Header> headers;
    private List<NameValuePair> parameters;
    private CloseableHttpResponse response;

    public WebAccess document(String scheme, String host, String path)
    {
        this.setScheme(scheme);
        this.setHost(host);
        this.setPath(path);
        this.headers = new ArrayList<>();
        this.parameters = new ArrayList<NameValuePair>();
        return this;
    }

    public WebAccess addParameter(String param, String value)
    {
        this.parameters.add(new BasicNameValuePair(param, value));
        return this;
    }

    public WebAccess addHeader(String param, String value)
    {
        this.headers.add(new BasicHeader(param, value));
        return this;
    }

    public String get() throws IOException
    {

        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(defaultRequestConfig)
                        .setDefaultHeaders(this.headers).setUserAgent(getUserAgent()).build();

        URIBuilder uriBuilder = new URIBuilder().setScheme(scheme).setHost(host).setPath(path);
        uriBuilder.setParameters(this.parameters);

        URL objectURL;
        try
        {
            objectURL = uriBuilder.build().toURL();
            CloseableHttpResponse response = client.execute(new HttpGet(objectURL.toString()));
            setResponse(response);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException(objectURL.toString() + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

            this.document = EntityUtils.toString(response.getEntity());
        }
        catch (URISyntaxException e)
        {
        }

        return this.document;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getScheme()
    {
        return scheme;
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public List<Header> getHeaders()
    {
        return headers;
    }

    public void addHeaders(String param, String value)
    {
        this.headers.add(new BasicHeader(param, value));
    }

    public List<NameValuePair> getParameters()
    {
        return parameters;
    }

    public void addParameters(String param, String value)
    {
        this.parameters.add(new BasicNameValuePair(param, value));
    }

    public CloseableHttpResponse getResponse()
    {
        return response;
    }

    public void setResponse(CloseableHttpResponse response)
    {
        this.response = response;
    }

    protected String getUserAgent()
    {
        return OnlineHelper.getUserAgent();
    }
}
