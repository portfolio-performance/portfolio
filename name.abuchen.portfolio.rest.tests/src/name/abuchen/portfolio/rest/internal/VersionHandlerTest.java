package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import com.google.gson.JsonParser;

import org.junit.Test;

import name.abuchen.portfolio.rest.RestApiConstants;

@SuppressWarnings("nls")
public class VersionHandlerTest
{
    private static com.google.gson.JsonObject body()
    {
        var response = VersionHandler.serve();
        assertThat(response.status(), is(200));
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    public void testReportsTheContractVersion()
    {
        assertThat(body().get("apiVersion").getAsString(), is(RestApiConstants.API_VERSION));
    }

    @Test
    public void testContentTypeIsJson()
    {
        assertThat(VersionHandler.serve().contentType(), is("application/json"));
    }

    /**
     * These tests run inside an OSGi framework, so the bundle is present and the
     * release must be reported. The other half of the contract - that the key is
     * omitted rather than faked when there is no bundle, as in the headless dev
     * server - cannot be exercised from here; it is guarded by the null check in
     * the handler.
     */
    @Test
    public void testReportsTheApplicationReleaseAsThreeNumbers()
    {
        var json = body();
        assertTrue("applicationVersion must be present when running as a bundle", json.has("applicationVersion"));
        assertThat(json.get("applicationVersion").getAsString(), matchesPattern("\\d+\\.\\d+\\.\\d+"));
    }

    /** The OSGi qualifier is build metadata, not part of the release a user reads. */
    @Test
    public void testApplicationVersionCarriesNoQualifier()
    {
        assertThat(body().get("applicationVersion").getAsString(), not(containsString("qualifier")));
    }

    @Test
    public void testCarriesNoUserData()
    {
        var json = body().toString();
        assertThat(json, not(containsString("file")));
        assertThat(json, not(containsString("token")));
    }
}
