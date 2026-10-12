package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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

    /** a repeat must not be silently reduced to its last value */
    @Test
    public void testParseQueryRejectsRepeatedNames()
    {
        var problem = expectRejection("status=open&date=2026-01-01&status=closed&metrics=risk&metrics=gains");

        assertThat(problem.getStatus(), is(400));
        assertThat(problem.getType(), is("invalid-request"));
        assertThat(problem.getErrors().size(), is(2));

        // sorted, so the response does not depend on the query string's order
        assertThat(problem.getErrors().get(0).field(), is("metrics"));
        assertThat(problem.getErrors().get(1).field(), is("status"));
        assertThat(problem.getErrors().get(1).code(), is("duplicate-parameter"));
        assertThat(problem.getErrors().get(1).message(), containsString("metrics=risk,gains"));
    }

    /** no guessing that two equal values mean one: the rule stays simple */
    @Test
    public void testParseQueryRejectsIdenticalRepeats()
    {
        var problem = expectRejection("date=2026-01-01&date=2026-01-01");

        assertThat(problem.getErrors().get(0).field(), is("date"));
        assertThat(problem.getErrors().get(0).code(), is("duplicate-parameter"));
    }

    /** names are compared as decoded, so an encoded spelling is the same name */
    @Test
    public void testParseQueryComparesDecodedNames()
    {
        var problem = expectRejection("status=open&%73tatus=closed");

        assertThat(problem.getErrors().get(0).field(), is("status"));
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

    private static ApiException expectRejection(String rawQuery)
    {
        try
        {
            Request.parseQuery(rawQuery);
            throw new AssertionError("expected ApiException for " + rawQuery);
        }
        catch (ApiException e)
        {
            return e;
        }
    }
}
