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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import name.abuchen.portfolio.rest.internal.Progress;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
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
        host.blockUiThreadUntil(gate);

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
        host.blockUiThreadUntil(gate);

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
            ApiRoutes.uiExecBounded(host, () -> {
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
        waitUntil(() -> host.uiThreadResults().contains("done"));
    }

    /**
     * A read changes nothing. Even when it had started, the answer was thrown
     * away and sending the read again is safe - so it is the retryable refusal,
     * not "do not resend".
     */
    @Test
    public void testAReadAlreadyRunningIsRetryable() throws Exception
    {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);

        try
        {
            ApiRoutes.uiExecBounded(host, () -> {
                started.countDown();
                release.await(10, TimeUnit.SECONDS);
                return "read";
            }, Duration.ofMillis(300), true);
            fail("expected the bounded wait on the UI thread to expire");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("ui-busy"));
            assertThat(e.getHeaders().get("Retry-After"), is("5"));
        }

        assertThat(started.getCount(), is(0L));
        release.countDown();
        waitUntil(() -> host.uiThreadResults().contains("read"));
    }

    /** an answer within the deadline is simply the answer */
    @Test
    public void testAnAnswerWithinTheDeadlineIsReturned() throws Exception
    {
        assertThat(ApiRoutes.uiExecBounded(host, () -> "fast", UI_DEADLINE), is("fast"));
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
            }, Duration.ofMillis(200), slots(1, 0, Duration.ofSeconds(5)));
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

    /**
     * Reported in review: the engines never check for an interrupt, so a
     * calculation past its deadline keeps running. It keeps its slot until it
     * really ends, and with no room to wait a new calculation is refused at once
     * instead of starting next to it.
     */
    @Test
    public void testACalculationPastItsDeadlineKeepsItsSlot() throws Exception
    {
        var slots = slots(1, 0, Duration.ofSeconds(5));
        var finish = new AtomicBoolean(false);

        try
        {
            ApiRoutes.computeBounded(() -> {
                // ignores the interrupt, as the engines do
                while (!finish.get())
                    Thread.onSpinWait();
                return null;
            }, Duration.ofMillis(200), slots);
            fail("expected the calculation deadline to expire");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("calculation-timeout"));
        }

        // still running, still counted
        assertThat(slots.available(), is(0));
        try
        {
            ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots);
            fail("expected the second calculation to be refused while the first still runs");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("server-busy"));
            assertThat(e.getHeaders().get("Retry-After"), is("1"));
        }

        finish.set(true);
        waitUntil(() -> slots.available() == 1);
        assertThat(ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots), is(nullValue()));
    }

    /**
     * Every slot taken is not a refusal: a model asks several questions in one
     * turn, and the one that finds no slot waits for the next to come free.
     */
    @Test
    public void testACalculationWaitsForAFreeSlot() throws Exception
    {
        var slots = slots(1, 2, Duration.ofSeconds(5));
        var release = new CountDownLatch(1);

        var first = holdSlot(slots, release);
        waitUntil(() -> slots.available() == 0);

        var second = new CompletableFuture<Response>();
        Thread.ofPlatform().start(() -> complete(second,
                        () -> ApiRoutes.computeBounded(() -> Response.noContent(), Duration.ofSeconds(5), slots)));

        waitUntil(() -> slots.waiting() == 1);
        assertThat(second.isDone(), is(false));

        release.countDown();
        assertThat(second.get(5, TimeUnit.SECONDS).status(), is(204));
        first.get(5, TimeUnit.SECONDS);
        assertThat(slots.available(), is(1));
    }

    /** first come, first served: a later arrival does not overtake one that waits */
    @Test
    public void testWaitingCalculationsAreServedInTurn() throws Exception
    {
        var slots = slots(1, 3, Duration.ofSeconds(5));
        var release = new CountDownLatch(1);
        var order = new CopyOnWriteArrayList<String>();

        var first = holdSlot(slots, release);
        waitUntil(() -> slots.available() == 0);

        var b = new CompletableFuture<Response>();
        Thread.ofPlatform().start(() -> complete(b, () -> ApiRoutes.computeBounded(() -> {
            order.add("b");
            return null;
        }, Duration.ofSeconds(5), slots)));
        waitUntil(() -> slots.waiting() == 1);

        var c = new CompletableFuture<Response>();
        Thread.ofPlatform().start(() -> complete(c, () -> ApiRoutes.computeBounded(() -> {
            order.add("c");
            return null;
        }, Duration.ofSeconds(5), slots)));
        waitUntil(() -> slots.waiting() == 2);

        release.countDown();
        b.get(5, TimeUnit.SECONDS);
        c.get(5, TimeUnit.SECONDS);
        first.get(5, TimeUnit.SECONDS);

        assertThat(order, is(List.of("b", "c")));
    }

    /** the queue is bounded: one more than fits is refused at once, not after the wait */
    @Test
    public void testAFullQueueRefusesAtOnce() throws Exception
    {
        var slots = slots(1, 1, Duration.ofSeconds(30));
        var release = new CountDownLatch(1);

        try
        {
            var first = holdSlot(slots, release);
            waitUntil(() -> slots.available() == 0);

            var waiter = new CompletableFuture<Response>();
            Thread.ofPlatform().start(() -> complete(waiter,
                            () -> ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots)));
            waitUntil(() -> slots.waiting() == 1);

            var started = System.nanoTime();
            try
            {
                ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots);
                fail("expected the calculation to be refused while the queue is full");
            }
            catch (ApiException e)
            {
                assertThat(e.getType(), is("server-busy"));
            }
            assertThat(Duration.ofNanos(System.nanoTime() - started).toMillis() < 1_000, is(true));

            release.countDown();
            waiter.get(5, TimeUnit.SECONDS);
            first.get(5, TimeUnit.SECONDS);
        }
        finally
        {
            release.countDown();
        }
    }

    /** the wait is bounded in time as well: it ends in a refusal, not in silence */
    @Test
    public void testAWaitThatRunsOutIsRefused() throws Exception
    {
        var slots = slots(1, 1, Duration.ofMillis(200));
        var release = new CountDownLatch(1);

        try
        {
            holdSlot(slots, release);
            waitUntil(() -> slots.available() == 0);

            try
            {
                ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots);
                fail("expected the wait for a slot to run out");
            }
            catch (ApiException e)
            {
                assertThat(e.getType(), is("server-busy"));
            }
            assertThat(slots.waiting(), is(0));
        }
        finally
        {
            release.countDown();
        }
    }

    /** a caller who gave up leaves the queue at once, and its place goes to the next */
    @Test
    public void testACancelledWaitLeavesTheQueue() throws Exception
    {
        var slots = slots(1, 2, Duration.ofSeconds(30));
        var release = new CountDownLatch(1);
        var cancelled = new CancellableProgress();

        try
        {
            holdSlot(slots, release);
            waitUntil(() -> slots.available() == 0);

            var waiter = new CompletableFuture<Response>();
            Thread.ofPlatform().start(() -> complete(waiter,
                            () -> ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots, cancelled)));
            waitUntil(() -> slots.waiting() == 1);

            cancelled.cancel();
            try
            {
                waiter.get(5, TimeUnit.SECONDS);
                fail("expected the cancelled wait to end in a cancellation");
            }
            catch (CancellationException e)
            {
                // a future completed with a cancellation throws it as it is
            }
            assertThat(slots.waiting(), is(0));
            // it never took the slot, so the one running still holds the only one
            assertThat(slots.available(), is(0));
        }
        finally
        {
            release.countDown();
        }
    }

    /**
     * A running calculation whose caller gave up: the caller stops waiting at
     * once, and the slot comes free when the work stops - here at the point
     * where it checks, as the API's own loops do.
     */
    @Test
    public void testACancelledCalculationStopsAndGivesBackItsSlot() throws Exception
    {
        var slots = slots(1, 0, Duration.ofSeconds(5));
        var cancelled = new CancellableProgress();
        var started = new CountDownLatch(1);

        var caller = new CompletableFuture<Response>();
        Thread.ofPlatform().start(() -> complete(caller, () -> ApiRoutes.computeBounded(() -> {
            started.countDown();
            while (true)
            {
                Progress.current().checkCancelled();
                Thread.onSpinWait();
            }
        }, Duration.ofSeconds(30), slots, cancelled)));

        assertThat(started.await(5, TimeUnit.SECONDS), is(true));
        cancelled.cancel();

        try
        {
            caller.get(5, TimeUnit.SECONDS);
            fail("expected the cancelled calculation to end in a cancellation");
        }
        catch (CancellationException e)
        {
            // a future completed with a cancellation throws it as it is
        }
        waitUntil(() -> slots.available() == 1);
    }

    /**
     * A calculation whose deadline passes before any thread took it up never
     * runs - and so never reaches the code that gives its slot back. The caller
     * gives it back instead, exactly once: when the thread finally comes free,
     * the task does nothing and the slot is not returned a second time.
     */
    @Test
    public void testACalculationThatNeverStartedGivesBackItsSlotOnTimeout() throws Exception
    {
        assertSlotComesBackWhenNeverStarted(false);
    }

    /** the same for a calculation whose caller cancelled before it started */
    @Test
    public void testACalculationThatNeverStartedGivesBackItsSlotOnCancellation() throws Exception
    {
        assertSlotComesBackWhenNeverStarted(true);
    }

    private void assertSlotComesBackWhenNeverStarted(boolean cancel) throws Exception
    {
        var slots = slots(1, 0, Duration.ofSeconds(5));
        var progress = new CancellableProgress();
        var ran = new AtomicBoolean();

        // the only thread is busy, so the calculation stays queued behind it
        var busy = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try
        {
            executor.execute(() -> {
                try
                {
                    busy.await(10, TimeUnit.SECONDS);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            });

            if (cancel)
                progress.cancel();

            try
            {
                ApiRoutes.computeBounded(() -> {
                    ran.set(true);
                    return null;
                }, cancel ? Duration.ofSeconds(5) : Duration.ofMillis(300), slots, progress, executor);
                fail("expected the calculation to be given up on");
            }
            catch (CancellationException e)
            {
                assertThat(cancel, is(true));
            }
            catch (ApiException e)
            {
                assertThat(cancel, is(false));
                assertThat(e.getType(), is("calculation-timeout"));
            }

            assertThat(slots.available(), is(1));
        }
        finally
        {
            busy.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS), is(true));
        }

        assertThat(ran.get(), is(false));
        // given back once, not once more when the thread got to the task
        assertThat(slots.available(), is(1));
    }

    /** the stages a calculation passes through are reported, in order */
    @Test
    public void testACalculationReportsWaitingAndCalculating() throws Exception
    {
        var slots = slots(1, 1, Duration.ofSeconds(5));
        var release = new CountDownLatch(1);
        var phases = new CopyOnWriteArrayList<String>();
        var recording = new CancellableProgress()
        {
            @Override
            public void phase(String message)
            {
                phases.add(message);
            }
        };

        var first = holdSlot(slots, release);
        waitUntil(() -> slots.available() == 0);

        var waiter = new CompletableFuture<Response>();
        Thread.ofPlatform().start(() -> complete(waiter,
                        () -> ApiRoutes.computeBounded(() -> null, Duration.ofSeconds(5), slots, recording)));
        waitUntil(() -> !phases.isEmpty());
        release.countDown();
        waiter.get(5, TimeUnit.SECONDS);
        first.get(5, TimeUnit.SECONDS);

        assertThat(phases, is(List.of("waiting for a calculation slot to come free", "calculating")));
    }

    /** a failure inside the calculation is that failure, not a timeout */
    @Test
    public void testACalculationFailureIsPassedThrough() throws Exception
    {
        try
        {
            ApiRoutes.computeBounded(() -> {
                throw ApiException.notFound();
            }, Duration.ofSeconds(5), slots(1, 0, Duration.ofSeconds(5)));
            fail("expected the calculation's own exception");
        }
        catch (ApiException e)
        {
            assertThat(e.getType(), is("not-found"));
        }
    }

    /** a progress that reports nothing and is cancelled when told */
    private static class CancellableProgress implements Progress
    {
        private volatile boolean cancelled;

        void cancel()
        {
            cancelled = true;
        }

        @Override
        public void phase(String message)
        {
            // not recorded
        }

        @Override
        public void step(int done, int total, String message)
        {
            // not recorded
        }

        @Override
        public boolean isCancelled()
        {
            return cancelled;
        }
    }

    private static ApiRoutes.CalculationSlots slots(int running, int queue, Duration wait)
    {
        return new ApiRoutes.CalculationSlots(running, queue, wait);
    }

    /** a calculation on a thread of its own that holds a slot until the latch opens */
    private static CompletableFuture<Response> holdSlot(ApiRoutes.CalculationSlots slots, CountDownLatch release)
    {
        var future = new CompletableFuture<Response>();
        Thread.ofPlatform().start(() -> complete(future, () -> ApiRoutes.computeBounded(() -> {
            release.await(10, TimeUnit.SECONDS);
            return null;
        }, Duration.ofSeconds(10), slots)));
        return future;
    }

    private static void complete(CompletableFuture<Response> future, Callable<Response> call)
    {
        try
        {
            future.complete(call.call());
        }
        catch (Throwable t)
        {
            future.completeExceptionally(t);
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
