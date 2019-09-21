package name.abuchen.portfolio.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

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

public class WebAccess
{
    public static final RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(20000)
                    .setConnectTimeout(2000).setConnectionRequestTimeout(20000).setCookieSpec(CookieSpecs.STANDARD)
                    .build();

    private static ArrayList<String> filterContentType = new ArrayList<>();

    static
    {
        filterContentType.add(ContentType.DEFAULT_TEXT.getMimeType().toString());
        filterContentType.add(ContentType.APPLICATION_JSON.getMimeType().toString());
        filterContentType.add(ContentType.APPLICATION_XHTML_XML.getMimeType().toString());
        filterContentType.add(ContentType.APPLICATION_XML.getMimeType().toString());
        filterContentType.add(ContentType.TEXT_HTML.getMimeType().toString());
        filterContentType.add(ContentType.TEXT_PLAIN.getMimeType().toString());
        filterContentType.add(ContentType.TEXT_XML.getMimeType().toString());
    }

    private String url;
    private String scheme = "https"; //$NON-NLS-1$
    private String host;
    private String path;
    private String userAgent = OnlineHelper.getUserAgent();
    private Boolean ignoreContentType = false;
    private List<Header> headers = new ArrayList<>();
    private List<NameValuePair> parameters = new ArrayList<>();

    public WebAccess(String host, String path)
    {
        if (Objects.nonNull(host) && Objects.nonNull(path))
        {
            this.host = host.trim();
            this.path = path.trim();
        }
    }

    public WebAccess(String url)
    {
        this.url = Objects.requireNonNull(url.trim(), "PP WebAccess: url is null"); //$NON-NLS-1$
    }

    public WebAccess withScheme(String scheme)
    {
        this.scheme = Objects.requireNonNull(scheme.trim(), "PP WebAccess: scheme is null"); //$NON-NLS-1$
        return this;
    }

    public WebAccess addParameter(String param, String value)
    {
        param = Objects.requireNonNull(param, "PP WebAccess: Parameter is null"); //$NON-NLS-1$
        value = Objects.requireNonNull(value, "PP WebAccess: Value Agent is null"); //$NON-NLS-1$
        this.parameters.add(new BasicNameValuePair(param, value));
        return this;
    }

    public WebAccess addHeader(String param, String value)
    {
        param = Objects.requireNonNull(param, "PP WebAccess: Header: Parameter is null"); //$NON-NLS-1$
        value = Objects.requireNonNull(value, "PP WebAccess: Header: Value Agent is null"); //$NON-NLS-1$
        this.headers.add(new BasicHeader(param, value));
        return this;
    }

    public WebAccess addUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
        return this;
    }

    public WebAccess ignoreContentType(Boolean ignoreContentType)
    {
        this.ignoreContentType = ignoreContentType;
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

            if (!this.ignoreContentType)
            {
                HttpEntity entity = response.getEntity();
                ContentType contentType;
                if (entity != null)
                {
                    contentType = ContentType.get(entity);
                    Boolean contentFound = filterContentType.stream()
                                    .anyMatch(s -> contentType.getMimeType().toString().contains(s));
                    if (!contentFound)
                        throw new IllegalStateException(errorURL + " --> content type " + contentType.getMimeType() //$NON-NLS-1$
                                        + " is not supported"); //$NON-NLS-1$
                }
                else
                    throw new IllegalStateException(errorURL + " --> missing ContentType"); //$NON-NLS-1$
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
