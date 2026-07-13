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
import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.rest.ApiRoutes;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * The model and the list of open files may only be touched on the UI thread.
 * The FakeHost flags any access that does not go through syncExec.
 */
@SuppressWarnings("nls")
public class UIThreadMarshallingTest
{
    private static final String PATH = "/tmp/x.portfolio";

    private IEclipsePreferences node;
    private FakeHost host;
    private Router router;
    private Client client;
    private Security security;
    private String fileId;

    @Before
    public void setUp()
    {
        client = new Client();
        security = new SecurityBuilder().addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        fileId = registry.byPath(PATH).orElseThrow().uuid();

        host = new FakeHost(List.of(new FakeHost.FakeOpenFile(PATH, "x", client)));
        router = ApiRoutes.create(registry, host);
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    private void call(String method, String path, String body) throws Exception
    {
        var match = router.match(method, path);
        match.handler().handle(new Request(method, path, match.pathParams(),
                        body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testAllRoutesReadOpenFilesOnUIThread() throws Exception
    {
        call("GET", "/v1/files", null);
        call("GET", "/v1/files/" + fileId + "/instruments", null);
        call("GET", "/v1/files/" + fileId + "/instruments/" + security.getUUID(), null);
        call("GET", "/v1/files/" + fileId + "/cash-accounts", null);
        call("GET", "/v1/files/" + fileId + "/investment-accounts", null);
        call("PATCH", "/v1/files/" + fileId + "/instruments/" + security.getUUID(), "{\"name\":\"New\"}");
        call("DELETE", "/v1/files/" + fileId + "/instruments/" + security.getUUID(), null);

        assertThat(host.hasAccessedOutsideUIThread(), is(false));
    }
}
