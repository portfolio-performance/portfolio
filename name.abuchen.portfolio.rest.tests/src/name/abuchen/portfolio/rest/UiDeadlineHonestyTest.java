package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * What a request that ran out of its wait for the UI thread, or out of its
 * calculation deadline, is told. The two
 * timeout outcomes are not the same, and only one of them may be called a
 * refusal: a request the UI thread never took on is refused and never runs, so
 * it may be sent again; one that was already running will finish, and a client
 * that retries a write applies the change twice.
 */
@SuppressWarnings("nls")
public class UiDeadlineHonestyTest
{
    private static final String PATH = "/tmp/x.portfolio";
    private static final Duration UI_DEADLINE = Duration.ofMillis(1_500);

    private IEclipsePreferences node;
    private FakeHost host;
    private Router router;
    private Security security;
    private String fileId;

    @Before
    public void setUp()
    {
        var client = new Client();
        security = new SecurityBuilder().addTo(client);

        node = InstanceScope.INSTANCE.getNode("rest-test-" + UUID.randomUUID());
        var registry = new FileAccessRegistry(node);
        registry.setEnabled(PATH, true);
        fileId = registry.byPath(PATH).orElseThrow().uuid();

        host = new FakeHost(List.of(new FakeHost.FakeOpenFile(PATH, "x", client)));
        router = ApiRoutes.create(registry, host,
                        new PairingService(new ClientStore(Path.of("target", "unused-client-store")), host),
                        new ApiRoutes.Deadlines(UI_DEADLINE, Duration.ofSeconds(120)));
    }

    @After
    public void tearDown() throws Exception
    {
        node.removeNode();
    }

    /**
     * The UI thread is busy. The write is refused after the deadline - and when
     * the UI thread gets round to it, it does nothing, which is what makes the
     * refusal true and a retry safe.
     */
    @Test
    public void testAWriteTheUiThreadNeverTookOnIsRefusedAndNeverRuns() throws Exception
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
            assertThat(e.getType(), is("ui-busy"));
            assertThat(e.getHeaders().get("Retry-After"), is("5"));
        }

        // the UI thread finishes whatever it was busy with and reaches the
        // queued request - which must now leave the model alone
        gate.countDown();
        waitUntil(() -> host.callablesRun() == 1);

        assertThat(host.callableFailure().map(Throwable::toString).orElse("none"), is("none"));
        assertThat(security.getName(), is(not("Renamed")));
    }

    /** a read that never started is the same refusal - and a read is always safe to repeat */
    @Test
    public void testAReadTheUiThreadNeverTookOnIsRetryable() throws Exception
    {
        var gate = new CountDownLatch(1);
        host.blockSyncExecUntil(gate);

        var path = "/v1/files/" + fileId + "/instruments/" + security.getUUID();
        var match = router.match("GET", path);

        try
        {
            match.handler().handle(new Request("GET", path, match.pathParams(), new byte[0]));
            fail("expected the bounded wait on the UI thread to expire");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("ui-busy"));
        }
        finally
        {
            gate.countDown();
        }
    }

    /**
     * The UI thread had already started the request when the deadline passed. It
     * will finish, so the caller must not be told it was refused.
     */
    @Test
    public void testARequestAlreadyRunningIsReportedAsUnderway() throws Exception
    {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);

        try
        {
            ApiRoutes.syncExecBounded(host, () -> {
                started.countDown();
                release.await(10, TimeUnit.SECONDS);
                return "done";
            }, Duration.ofMillis(300));
            fail("expected the bounded wait on the UI thread to expire");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("ui-request-underway"));
            // retrying would apply the same change a second time
            assertThat(e.getHeaders().get("Retry-After"), is(nullValue()));
        }

        assertThat(started.getCount(), is(0L));
        release.countDown();
        waitUntil(() -> host.syncExecResults().contains("done"));
    }

    /** an answer within the deadline is simply the answer */
    @Test
    public void testAnAnswerWithinTheDeadlineIsReturned() throws Exception
    {
        assertThat(ApiRoutes.syncExecBounded(host, () -> "fast", UI_DEADLINE), is("fast"));
    }

    /**
     * A calculation that outlasts its deadline ends in a refusal the client can
     * act on - not in a connection the watchdog drops without a word.
     */
    @Test
    public void testACalculationOverItsDeadlineIsRefused() throws Exception
    {
        var release = new CountDownLatch(1);

        try
        {
            ApiRoutes.computeBounded(() -> {
                release.await(10, TimeUnit.SECONDS);
                return null;
            }, Duration.ofMillis(200));
            fail("expected the calculation deadline to expire");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(503));
            assertThat(e.getType(), is("calculation-timeout"));
            // the same question takes just as long again
            assertThat(e.getHeaders().get("Retry-After"), is(nullValue()));
        }
        finally
        {
            release.countDown();
        }
    }

    /** a failure inside the calculation is that failure, not a timeout */
    @Test
    public void testACalculationFailureIsPassedThrough() throws Exception
    {
        try
        {
            ApiRoutes.computeBounded(() -> {
                throw ApiException.notFound();
            }, Duration.ofSeconds(5));
            fail("expected the calculation's own exception");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("not-found"));
        }
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException
    {
        var until = System.currentTimeMillis() + 5_000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < until)
            Thread.sleep(10);
        assertThat(condition.getAsBoolean(), is(true));
    }
}
