package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * What a request that ran out of its wait for the UI thread is told. The two
 * timeout paths are not the same outcome, and only one of them may be called a
 * refusal: a write the UI thread was already going to perform must not be
 * reported as refused, because a client that retries it applies the change twice.
 */
@SuppressWarnings("nls")
public class UiDeadlineHonestyTest
{
    private static final String PATH = "/tmp/x.portfolio";

    private IEclipsePreferences node;
    private FakeHost host;
    private Router router;
    private Client client;
    private Security security;
    private String fileId;
    private long savedDeadline;

    @Before
    public void setUp()
    {
        savedDeadline = ApiRoutes.uiDeadlineMillis;
        ApiRoutes.uiDeadlineMillis = 1_500;

        client = new Client();
        security = new SecurityBuilder().addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        fileId = registry.byPath(PATH).orElseThrow().uuid();

        host = new FakeHost(List.of(new FakeHost.FakeOpenFile(PATH, "x", client)));
        router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host));
    }

    @After
    public void tearDown() throws Exception
    {
        ApiRoutes.uiDeadlineMillis = savedDeadline;
        node.removeNode();
    }

    /**
     * The UI thread is busy. The request is refused after the deadline - and then
     * the UI thread gets round to it, applying the change the caller was told
     * about in a way that must not read as "did not happen".
     */
    @Test
    public void testTimedOutWriteThatStillRunsIsNotReportedAsRefused() throws Exception
    {
        var gate = new CountDownLatch(1);
        host.blockSyncExecUntil(gate);

        var path = "/v1/files/" + fileId + "/instruments/" + security.getUUID();
        var match = router.match("PATCH", path);

        try
        {
            match.handler().handle(new Request("PATCH", path, match.pathParams(),
                            "{\"name\":\"Renamed\"}".getBytes(StandardCharsets.UTF_8)));
            fail("expected the bounded wait on the UI thread to expire");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(503));
            assertThat(e.getType(), is("ui-request-underway"));
            // retrying would apply the same change a second time
            assertThat(e.getHeaders().get("Retry-After"), is(nullValue()));
        }

        assertThat(host.callablesRun(), is(0));
        assertThat(security.getName(), is(not("Renamed")));

        // the UI thread finishes whatever it was busy with, and the queued write
        // runs - which is exactly why the answer above was not a refusal
        gate.countDown();

        var until = System.currentTimeMillis() + 5_000;
        while (host.syncExecResults().isEmpty() && host.callableFailure().isEmpty()
                        && System.currentTimeMillis() < until)
            Thread.sleep(10);

        // a callable that ran but failed is not one that applied the change
        assertThat(host.callableFailure().map(Throwable::toString).orElse("none"), is("none"));
        assertThat(host.syncExecResults().size(), is(1));
        assertThat(((Response) host.syncExecResults().get(0)).status(), is(200));
        assertThat(security.getName(), is("Renamed"));
    }
}
