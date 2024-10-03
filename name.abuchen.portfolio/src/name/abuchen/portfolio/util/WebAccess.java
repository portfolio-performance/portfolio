package name.abuchen.portfolio.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.impl.EnglishReasonPhraseCatalog;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;

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
 *                       .addHeader("Content-Type", "application/json;charset=UTF-8")
 *                       .addHeader("X-Response", "daily")
 *                       .addUserAgent("Mozilla/1.0N (Windows)")
 *                       .ignoreContentType(true)
 *                       .get();
 */
//@formatter:on
public class WebAccess
{
    @FunctionalInterface
    private interface Request
    {
        HttpUriRequestBase create(URI uri) throws IOException;
    }

    public static class WebAccessException extends IOException
    {
        private static final long serialVersionUID = 1L;
        private final int httpErrorCode;
        private final List<Pair<String, String>> headers;

        public WebAccessException(String message, int httpErrorCode, List<Pair<String, String>> headers)
        {
            super(message);
            this.httpErrorCode = httpErrorCode;
            this.headers = headers;
        }

        public int getHttpErrorCode()
        {
            return httpErrorCode;
        }

        public List<String> getHeader(String key)
        {
            return headers.stream().filter(p -> p.getKey().equalsIgnoreCase(key)).map(Pair::getValue).toList();
        }
    }

    private static class CustomResponseHandler implements HttpClientResponseHandler<String>
    {
        private final String uri;

        public CustomResponseHandler(String uri)
        {
            super();
            this.uri = uri;
        }

        @Override
        public String handleResponse(final ClassicHttpResponse response) throws IOException
        {
            final HttpEntity entity = response.getEntity();
            if (response.getCode() >= HttpStatus.SC_REDIRECTION)
            {
                EntityUtils.consume(entity);
                var headers = Stream.of(response.getHeaders()).map(h -> new Pair<>(h.getName(), h.getValue())).toList();
                throw new WebAccessException(buildMessage(uri, response.getCode()), response.getCode(), headers);
            }

            if (entity == null)
                return null;

            try
            {
                return EntityUtils.toString(entity);
            }
            catch (final ParseException ex)
            {
                throw new ClientProtocolException(ex);
            }
        }
    }

    public static final RequestConfig defaultRequestConfig = RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofSeconds(20)) //
                    .setCookieSpec(StandardCookieSpec.STRICT) //
                    .build();

    public static final ConnectionConfig defaultConnectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(5)) //
                    .build();

    private final URIBuilder builder;
    private List<Header> headers = new ArrayList<>();
    private String userAgent = OnlineHelper.getUserAgent();

    public WebAccess(String host, String path)
    {
        this.builder = new URIBuilder();
        this.builder.setScheme("https"); //$NON-NLS-1$
        this.builder.setHost(Objects.requireNonNull(host).trim());
        this.builder.setPath(Objects.requireNonNull(path).trim());
    }

    public WebAccess(String url) throws URISyntaxException
    {
        this.builder = new URIBuilder(url);
    }

    public WebAccess withScheme(String scheme)
    {
        this.builder.setScheme(Objects.requireNonNull(scheme).trim());
        return this;
    }

    public WebAccess withPort(Integer port)
    {
        this.builder.setPort(port != null ? port : -1);
        return this;
    }

    public WebAccess withFragment(String fragment)
    {
        this.builder.setFragment(Objects.requireNonNull(fragment).trim());
        return this;
    }

    public WebAccess addParameter(String param, String value)
    {
        this.builder.addParameter(param, value);
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
        var response = executeWith(HttpGet::new);

        // the response handler can return a null string.
        // To keep up the contract of the get method, we throw an exception
        if (response == null)
            throw new IOException("No message entity in response: " + builder.toString()); //$NON-NLS-1$
        return response;
    }

    public void post(String body) throws IOException
    {
        executeWith(uri -> {
            HttpPost request = new HttpPost(uri);
            StringEntity userEntity = new StringEntity(body);
            request.setEntity(userEntity);
            return request;
        });
    }

    private String executeWith(Request function) throws IOException
    {
        try
        {
            var connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setDefaultConnectionConfig(defaultConnectionConfig);

            CloseableHttpClient client = HttpClientBuilder.create() //
                            .setConnectionManager(connectionManager) //
                            .setDefaultRequestConfig(defaultRequestConfig) //
                            .setDefaultHeaders(this.headers) //
                            .setUserAgent(this.userAgent) //
                            .useSystemProperties() //
                            .build();

            URI uri = builder.build();
            HttpUriRequestBase request = function.create(uri);

            return client.execute(request, new CustomResponseHandler(uri.toString()));
        }
        catch (HttpResponseException e)
        {
            throw new WebAccessException(buildMessage(builder.toString(), e.getStatusCode()), e.getStatusCode(),
                            new ArrayList<>());
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }

    private static String buildMessage(String uri, int statusCode)
    {
        String message = String.valueOf(statusCode);
        try
        {
            String reason = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.getDefault());
            if (reason != null)
                message += " " + reason; //$NON-NLS-1$
        }
        catch (IllegalArgumentException e)
        {
            // ignore -> unable to retrieve message
        }
        message += " --> " + uri; //$NON-NLS-1$
        return message;
    }

    public String getURL() throws URISyntaxException
    {
        return builder.build().toASCIIString();
    }
}
