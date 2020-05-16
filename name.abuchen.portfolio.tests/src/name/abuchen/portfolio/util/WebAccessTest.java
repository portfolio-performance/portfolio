package name.abuchen.portfolio.util;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import org.junit.Test;

@SuppressWarnings("nls")
public class WebAccessTest
{

    @Test
    public void testWebAccessStringString() throws URISyntaxException
    {
        WebAccess url = new WebAccess("www.example.com", "/my/path/");
        String url2 = "https://www.example.com/my/path/";
        assertEquals(url.getURL(), url2);
    }

    @Test
    public void testWebAccessString() throws URISyntaxException
    {
        WebAccess url = new WebAccess("www.example.com");
        String url2 = "www.example.com";
        assertEquals(url.getURL(), url2);
    }

    @Test
    public void testWithScheme() throws URISyntaxException
    {
        WebAccess url = new WebAccess("example.com", "/my/path/").withScheme("http");
        String url2 = "http://example.com/my/path/";
        assertEquals(url.getURL(), url2);
    }

    @Test
    public void testWithPort() throws URISyntaxException
    {
        WebAccess url = new WebAccess("example.com", "/my/path/").withPort(8080);
        String url2 = "https://example.com:8080/my/path/";
        String url3 = "https://example.com/my/path/";
        assertEquals(url.getURL(), url2);
        assertEquals(url.withPort(null).getURL(), url3);
    }

    @Test
    public void testWithFragment() throws URISyntaxException
    {
        WebAccess url = new WebAccess("example.com", "/foo.html").withFragment("bar");
        String url2 = "https://example.com/foo.html#bar";
        assertEquals(url.getURL(), url2);
    }

    @Test
    public void testAddParameter() throws URISyntaxException
    {
        WebAccess url = new WebAccess("example.com", "/foo.html").addParameter("limit", "1");
        String url2 = "https://example.com/foo.html?limit=1";
        assertEquals(url.getURL(), url2);
    }
}
