package name.abuchen.portfolio.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public final class HttpClient implements AutoCloseable
{

    public final static RequestConfig defaultRequestConfig = RequestConfig.custom()
                    .setSocketTimeout(600000)
                    .setConnectTimeout(600000)
                    .setConnectionRequestTimeout(600000)
                    .build();
    
    public CloseableHttpResponse response;
    public String uriScheme;
    public String uriHost;
    public String uriPath;
    public URIBuilder uriBuilder;

    public  HttpClient()
    {
    }
    
    public URIBuilder getURIBuilder()
    {
        return uriBuilder;
    }

    public void setURIBuilder()
    {
        this.uriBuilder = new URIBuilder().setScheme(this.uriScheme).setHost(this.uriHost)
                        .setPath(this.uriPath);
    }
    
    public CloseableHttpResponse getResponse()
    {
        return response;
    }

    public void setResponse(CloseableHttpResponse response)
    {
        this.response = response;
    }

    public String getURIScheme()
    {
        return uriScheme;
    }

    public void setURIScheme(String uriScheme)
    {
        this.uriScheme = uriScheme;
    }

    public String getURIHost()
    {
        return uriHost;
    }

    public void setURIHost(String uriHost)
    {
        this.uriHost = uriHost;
    }

    public String getURIPath()
    {
        return uriPath;
    } 

    public void setURIPath(String uriPath)
    {
        this.uriPath = uriPath;
    }

    public String requestData() throws IOException
    {
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(defaultRequestConfig ).build())
        {
            URL objectURL = this.getURIBuilder().build().toURL();
            try (CloseableHttpResponse response = client.execute(new HttpGet(objectURL.toString())))
            {
                this.setResponse(response);
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                    throw new IOException(objectURL.toString() + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

                return EntityUtils.toString(response.getEntity());
            }
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }

    @Override
    public void close() throws Exception
    {
        // TODO Auto-generated method stub
    }
}