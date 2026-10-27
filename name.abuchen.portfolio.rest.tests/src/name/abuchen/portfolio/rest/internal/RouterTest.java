package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

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

    /**
     * A literal segment is not a placeholder, so the route with fewer of them
     * answers whichever was registered first. Registration order must not decide
     * this: `/v1/files/{file}/instruments/attribute-types` would otherwise steal
     * the request meant for `/v1/files/{file}/instrument-attribute-types`.
     */
    @Test
    public void testTheMostSpecificRouteWinsRegardlessOfOrder() throws Exception
    {
        var registeredWithWildcardFirst = new Router();
        registeredWithWildcardFirst.add("GET", "/v1/files/{file}/instruments/{uuid}",
                        request -> Response.json(200, JsonParser.parseString("{\"which\":\"instrument\"}")));
        registeredWithWildcardFirst.add("GET", "/v1/files/{file}/instrument-attribute-types",
                        request -> Response.json(200, JsonParser.parseString("{\"which\":\"attributes\"}")));

        assertThat(answer(registeredWithWildcardFirst, "GET", "/v1/files/abc/instrument-attribute-types"),
                        containsString("attributes"));
        assertThat(answer(registeredWithWildcardFirst, "GET", "/v1/files/abc/instruments/xyz"),
                        containsString("instrument"));

        var registeredWithLiteralFirst = new Router();
        registeredWithLiteralFirst.add("GET", "/v1/files/{file}/instrument-attribute-types",
                        request -> Response.json(200, JsonParser.parseString("{\"which\":\"attributes\"}")));
        registeredWithLiteralFirst.add("GET", "/v1/files/{file}/instruments/{uuid}",
                        request -> Response.json(200, JsonParser.parseString("{\"which\":\"instrument\"}")));

        assertThat(answer(registeredWithLiteralFirst, "GET", "/v1/files/abc/instrument-attribute-types"),
                        containsString("attributes"));
        assertThat(answer(registeredWithLiteralFirst, "GET", "/v1/files/abc/instruments/xyz"),
                        containsString("instrument"));
    }

    /** trailing slashes are the same path, and an empty middle segment is not */
    @Test
    public void testTrailingSlashesDoNotMakeADifferentPath() throws Exception
    {
        var router = new Router();
        router.add("GET", "/v1/files", request -> Response.json(200, JsonParser.parseString("{\"which\":\"files\"}")));

        assertThat(answer(router, "GET", "/v1/files/"), containsString("files"));
        assertThat(answer(router, "GET", "//v1/files///"), containsString("files"));

        try
        {
            router.match("GET", "/v1//files");
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    private static String answer(Router router, String method, String path) throws Exception
    {
        var request = new Request(method, path, Map.of(), Map.of(), new byte[0], Request.Authorization.VALID, null);
        return new String(router.match(method, path).handler().handle(request).body(), StandardCharsets.UTF_8);
    }
}
