package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

@SuppressWarnings("nls")
public class RequestTest
{
    @Test
    public void testParseQuery()
    {
        var params = Request.parseQuery("date=2026-07-20&currency=EUR");
        assertThat(params.get("date"), is("2026-07-20"));
        assertThat(params.get("currency"), is("EUR"));
    }

    @Test
    public void testParseQueryHandlesAbsentAndEmptyQuery()
    {
        assertThat(Request.parseQuery(null).isEmpty(), is(true));
        assertThat(Request.parseQuery("").isEmpty(), is(true));
    }

    @Test
    public void testParseQueryDecodesPercentEncoding()
    {
        var params = Request.parseQuery("q=a%20b%26c");
        assertThat(params.get("q"), is("a b&c"));
    }

    @Test
    public void testParseQueryKeyWithoutValue()
    {
        var params = Request.parseQuery("flag&date=2026-01-01");
        assertThat(params.get("flag"), is(""));
        assertThat(params.get("date"), is("2026-01-01"));
    }

    @Test
    public void testParseQueryRejectsMalformedPercentEncoding()
    {
        for (var query : new String[] { "from=%zz", "from=%2", "%zz=1" })
        {
            try
            {
                Request.parseQuery(query);
                fail("expected 400 for " + query);
            }
            catch (ApiException e)
            {
                assertThat(e.getStatus(), is(400));
            }
        }
    }

    @Test
    public void testQueryParamAccessor()
    {
        var request = new Request("GET", "/v1/files/x/holdings", Map.of(), Map.of("date", "2026-07-20"), new byte[0]);
        assertThat(request.queryParam("date"), is("2026-07-20"));
        assertThat(request.queryParam("currency"), is(nullValue()));
    }

    @Test
    public void testConvenienceConstructorHasNoQueryParams()
    {
        var request = new Request("GET", "/path", Map.of(), new byte[0]);
        assertThat(request.queryParam("date"), is(nullValue()));
    }
}
