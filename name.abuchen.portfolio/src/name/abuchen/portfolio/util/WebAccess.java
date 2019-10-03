package name.abuchen.portfolio.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

//@formatter:off
/**
 * This class is centralizing the download of web content and returns the response as string.
 * This is a FLUENT-API.
 * 
 * Constructor
 * 
 *   WebAccess​(java.lang.String url)
 *         > Parameters:
 *              url :: URL address like 'http://example.com/path/to/document.html?parameter=value'
 *              
 *   WebAccess​(java.lang.String host, java.lang.String path)
 *         > Parameters:
 *              host :: URI host like 'www.example.com'
 *              path :: URI path like '/path/to/document.html'
 *
 *
 * Method
 *   get()
 *         > This method executes web request using the given context. 
 *
 *  
 * Optional Methods
 *
 *   withScheme​(java.lang.String scheme)
 *         > Parameters:
 *              scheme - URI scheme. Set the URI scheme like http, https or ftp. Default is 'https'.
 *   
 *   addParameter​(java.lang.String param, java.lang.String value
 *         > Parameters
 *              param & value :: Sets URI query parameters.
 *                               The parameter name / value are expected to be unescaped and may contain non ASCII characters.
 *
 *   addHeader​(java.lang.String param, java.lang.String value)
 *         > Parameters
 *              param & value :: Assigns default request header values.
 *                               The parameter name / value are expected to be unescaped and may contain non ASCII characters.
 *
 *   addUserAgent​(java.lang.String userAgent)
 *         > Parameters
 *              userAgent :: Assigns User-Agent value. Default is determined for OS platform Windows, Apple and Linux.
 *              
 *   ignoreContentType​(java.lang.Boolean ignoreContentType)
 *         > Parameters
 *              ignoreContentType :: Configures the request to ignore the Content-Type of the response.
 *                                   Default (false) handled mime types:
 *                                   text/plain, application/json, application/xhtml+xml, application/xml, text/html, text/plain, text/xml
 *
 *
 * Example
 * 
 *   Target URL
 *          http://example.com/path/page.html?parameter=value
 * 
 *  String html = new WebAccess("example.com", "/path/page.html")
 *                       .withScheme("http")
 *                       .addParameter("parameter", "value")
 *                       .addHeader("Content-Type", "application/json;chartset=UTF-8")
 *                       .addHeader("X-Response", "daily")
 *                       .addUserAgent("Mozilla/1.0N (Windows)")
 *                       .ignoreContentType(true)
 *                       .get();
 */
//@formatter:on
public class WebAccess
{
    public static final RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(20000)
                    .setConnectTimeout(2000).setConnectionRequestTimeout(20000).setCookieSpec(CookieSpecs.STANDARD)
                    .build();

    private String scheme = "https"; //$NON-NLS-1$
    private String host;
    private Integer port;
    private String path;
    private String fragment;
    private String userAgent = OnlineHelper.getUserAgent();
    private List<Header> headers = new ArrayList<>();
    private List<NameValuePair> parameters = new ArrayList<>();

    public WebAccess(String host, String path)
    {
        this.host = Objects.requireNonNull(host).trim();
        this.path = Objects.requireNonNull(path).trim();
    }

    public WebAccess(String url) throws URISyntaxException
    {
        URIBuilder builder = new URIBuilder(url);

        this.scheme = builder.getScheme();
        this.host = builder.getHost();
        this.port = builder.getPort();
        this.path = builder.getPath();
        this.fragment = builder.getFragment();
        this.parameters.addAll(builder.getQueryParams());
    }

    public WebAccess withScheme(String scheme)
    {
        this.scheme = Objects.requireNonNull(scheme).trim();
        return this;
    }

    public WebAccess withPort(Integer port)
    {
        this.port = Objects.requireNonNull(port);
        return this;
    }

    public WebAccess withFragment(String fragment)
    {
        this.fragment = Objects.requireNonNull(fragment).trim();
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

    public WebAccess addUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
        return this;
    }

    public String get() throws IOException
    {
        CloseableHttpResponse response = null;

        try
        {
            CloseableHttpClient client = HttpClientBuilder.create() //
                            .setDefaultRequestConfig(defaultRequestConfig) //
                            .setDefaultHeaders(this.headers) //
                            .setUserAgent(this.userAgent) //
                            .useSystemProperties() //
                            .build();

            URI uri = new URIBuilder().setScheme(this.scheme).setHost(this.host).setPort(port).setPath(this.path)
                            .setParameters(this.parameters).setFragment(fragment).build();
            response = client.execute(new HttpGet(uri));

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException(uri.toString() + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

            return EntityUtils.toString(response.getEntity());
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }
}
