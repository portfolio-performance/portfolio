package name.abuchen.portfolio.util.webaccess;

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

public class WebAccess implements AutoCloseable
{

    public final static RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(600000)
                    .setConnectTimeout(600000).setConnectionRequestTimeout(600000).build();

    private String scheme;
    private String host;
    private String path;
    private CloseableHttpResponse response;
    private String document;

    private List<WebAccessHeader> headers;
    private List<WebAccessParameter> parameters;

    public static WebAccessBuilder builder()
    {
        return new WebAccess.WebAccessBuilder();
    }

    private WebAccess()
    {
    }

    public String getScheme()
    {
        return scheme;
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public List<WebAccessHeader> getHeaders()
    {
        return headers;
    }

    public void setHeaders(List<WebAccessHeader> headers)
    {
        this.headers = headers;
    }

    public List<WebAccessParameter> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<WebAccessParameter> parameters)
    {
        this.parameters = parameters;
    }

    public CloseableHttpResponse getResponse()
    {
        return response;
    }

    public void setResponse(CloseableHttpResponse response)
    {
        this.response = response;
    }

    public String getDocument()
    {

        List<Header> lstHeaders = new ArrayList<>();
        if (headers != null)
        {
            for (WebAccessHeader header : headers)
            {
                lstHeaders.add(new BasicHeader(header.getParam(), header.getValue()));
            }
        }

        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(defaultRequestConfig)
                        .setDefaultHeaders(lstHeaders)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36") //$NON-NLS-1$
                        .build();

        URIBuilder uriBuilder = new URIBuilder().setScheme(scheme).setHost(host).setPath(path);

        List<NameValuePair> lstParameters = new ArrayList<NameValuePair>();
        if (parameters != null)
        {
            for (WebAccessParameter parameter : parameters)
            {
                lstParameters.add(new BasicNameValuePair(parameter.getParam(), parameter.getValue()));
            }
        }
        uriBuilder.setParameters(lstParameters);

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
        catch (URISyntaxException | IOException e)
        {
        }
        return this.document;
    }

    @Override
    public void close() throws Exception
    {
        // TODO Auto-generated method stub
    }

    public static class WebAccessBuilder
    {
        private WebAccess managedInstance = new WebAccess();

        public WebAccessBuilder document(String scheme, String host, String path)
        {
            managedInstance.setScheme(scheme);
            managedInstance.setHost(host);
            managedInstance.setPath(path);
            return this;
        }

        public WebAccessBuilder withHeader(WebAccessHeader header)
        {
            if (managedInstance.headers == null)
            {
                managedInstance.headers = new ArrayList<WebAccessHeader>();
            }
            managedInstance.headers.add(header);
            return this;
        }

        public WebAccessBuilder withParameter(WebAccessParameter parameter)
        {
            // new BasicHeader(WEBMATE_USER_HEADERKEY, authInfo.emailAddress)
            if (managedInstance.parameters == null)
            {
                managedInstance.parameters = new ArrayList<WebAccessParameter>();
            }
            managedInstance.parameters.add(parameter);
            return this;
        }

        public WebAccess build()
        {
            return managedInstance;
        }

    }
}
