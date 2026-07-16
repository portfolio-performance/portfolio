package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("nls")
public class RouterTest
{
    @Test
    public void testPathParamsAreExtracted() throws Exception
    {
        var router = new Router();
        router.add("GET", "/v1/files/{file}/instruments/{uuid}", request -> Response.noContent());

        var match = router.match("GET", "/v1/files/abc/instruments/123");

        assertThat(match.pathParams().get("file"), is("abc"));
        assertThat(match.pathParams().get("uuid"), is("123"));
    }

    @Test
    public void testUnknownPathThrows404()
    {
        var router = new Router();
        router.add("GET", "/v1/files", request -> Response.noContent());
        try
        {
            router.match("GET", "/v1/nonsense");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    @Test
    public void testWrongMethodThrows405()
    {
        var router = new Router();
        router.add("GET", "/v1/files", request -> Response.noContent());
        try
        {
            router.match("DELETE", "/v1/files");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(405));
        }
    }
}
