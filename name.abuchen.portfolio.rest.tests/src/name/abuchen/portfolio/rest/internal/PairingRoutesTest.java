package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.PairingService;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class PairingRoutesTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private IEclipsePreferences node;
    private FakeHost host;
    private ClientStore store;
    private Router router;

    @Before
    public void setUp()
    {
        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        host = new FakeHost(List.of());
        store = new ClientStore(tempFolder.getRoot().toPath());
        router = ApiRoutes.create(new FileAccessRegistry(node), host, new PairingService(store, host));
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    private Response call(String method, String path, String body) throws Exception
    {
        var match = router.match(method, path);
        var request = new Request(method, path, match.pathParams(), Request.parseQuery(null),
                        body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        return match.handler().handle(request);
    }

    private JsonObject json(Response response)
    {
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    public void testPairingFlowThroughTheRoutes() throws Exception
    {
        var created = call("POST", "/v1/auth/requests", "{\"clientName\":\"Claude Code\"}");
        assertThat(created.status(), is(201));

        var id = json(created).get("id").getAsString();
        assertThat(json(created).get("status").getAsString(), is("pending"));
        assertThat(created.headers().get("Location"), is("/v1/auth/requests/" + id));

        var pending = call("GET", "/v1/auth/requests/" + id, null);
        assertThat(json(pending).get("status").getAsString(), is("pending"));

        host.lastAccessRequest().allowAlways();

        var approved = call("GET", "/v1/auth/requests/" + id, null);
        assertThat(json(approved).get("status").getAsString(), is("approved"));

        var token = json(approved).get("token").getAsString();
        assertThat(store.authenticate(token).isPresent(), is(true));

        // one-shot: the token is delivered exactly once
        assertStatus("GET", "/v1/auth/requests/" + id, null, 404);
    }

    @Test
    public void testDeniedHasNoTokenField() throws Exception
    {
        var id = json(call("POST", "/v1/auth/requests", "{\"clientName\":\"Claude Code\"}")).get("id").getAsString();
        host.lastAccessRequest().decline();

        var denied = json(call("GET", "/v1/auth/requests/" + id, null));
        assertThat(denied.get("status").getAsString(), is("denied"));
        assertThat(denied.has("token"), is(false));
    }

    @Test
    public void testMissingClientNameIs422() throws Exception
    {
        assertStatus("POST", "/v1/auth/requests", "{}", 422);
    }

    @Test
    public void testNonStringClientNameIs422() throws Exception
    {
        assertStatus("POST", "/v1/auth/requests", "{\"clientName\":42}", 422);
    }

    @Test
    public void testInvalidJsonBodyIs400() throws Exception
    {
        assertStatus("POST", "/v1/auth/requests", "no json", 400);
    }

    @Test
    public void testPairingHandlersDoNotTouchTheUIThread() throws Exception
    {
        var id = json(call("POST", "/v1/auth/requests", "{\"clientName\":\"Claude Code\"}")).get("id").getAsString();
        call("GET", "/v1/auth/requests/" + id, null);

        assertThat(host.syncExecResults(), is(List.of()));
    }

    private void assertStatus(String method, String path, String body, int expectedStatus) throws Exception
    {
        try
        {
            call(method, path, body);
            assertThat("expected ApiException with status " + expectedStatus, false, is(true));
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(expectedStatus));
        }
    }
}
