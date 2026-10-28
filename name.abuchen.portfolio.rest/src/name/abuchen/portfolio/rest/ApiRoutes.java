package name.abuchen.portfolio.rest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.rest.internal.AccountsHandler;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.internal.FileResolver;
import name.abuchen.portfolio.rest.internal.FilesHandler;
import name.abuchen.portfolio.rest.internal.HoldingsHandler;
import name.abuchen.portfolio.rest.internal.InstrumentChangeLog;
import name.abuchen.portfolio.rest.internal.InstrumentPerformanceHandler;
import name.abuchen.portfolio.rest.internal.OpenApiHandler;
import name.abuchen.portfolio.rest.internal.PairingHandler;
import name.abuchen.portfolio.rest.internal.PerformanceHandler;
import name.abuchen.portfolio.rest.internal.PortfoliosHandler;
import name.abuchen.portfolio.rest.internal.Progress;
import name.abuchen.portfolio.rest.internal.Request;
import name.abuchen.portfolio.rest.internal.Response;
import name.abuchen.portfolio.rest.internal.Router;
import name.abuchen.portfolio.rest.internal.SecuritiesHandler;
import name.abuchen.portfolio.rest.internal.TradesHandler;
import name.abuchen.portfolio.rest.internal.VersionHandler;
import name.abuchen.portfolio.rest.internal.mcp.McpEndpoint;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;

/**
 * Registers all v1 routes, and the MCP endpoint that dispatches into them.
 * Reads and writes are marshalled to the UI thread, with a deadline; writes
 * are additionally rejected with 423 while an application-modal dialog is open
 * or the user edits a table cell. Calculation endpoints only resolve the
 * {file} scope on the UI thread and compute off it, likewise with a deadline.
 */
public final class ApiRoutes
{
    /** Shared query parameters for the instrument performance list and item routes. */
    private static final String[] INSTRUMENT_PERFORMANCE_PARAMS = { "openingDate", "closingDate", //$NON-NLS-1$ //$NON-NLS-2$
                    "reportingCurrency", "costMethod", "taxesAndFees", "metrics" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /** Query parameters shared by both trade routes. */
    private static final String[] TRADE_PARAMS = { "status", "grouping", "costMethod", "taxesAndFees", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    "reportingCurrency", "from", "to" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /**
     * How long a request may wait for the UI thread, and how long a calculation
     * may run. Both end in a refusal the client can act on rather than in a
     * worker parked indefinitely.
     *
     * @param uiThread
     *            the wait for the UI thread to take a request on. While the UI
     *            is busy an unbounded {@code syncExec} parks the worker and the
     *            API looks dead.
     * @param calculation
     *            the run time of a calculation endpoint, which computes off the
     *            UI thread. Long enough for a large file over its whole history,
     *            short enough that a client is not left hanging without an answer.
     */
    public record Deadlines(Duration uiThread, Duration calculation, Duration calculationQueue)
    {
        /**
         * How long a calculation may wait for a slot. Long enough to absorb the
         * handful of tool calls a model sends at once, short enough that a
         * client waiting without a word is told something within half a minute.
         */
        public static final Duration DEFAULT_CALCULATION_QUEUE = Duration.ofSeconds(30);

        public static final Deadlines DEFAULT = new Deadlines(Duration.ofSeconds(10), Duration.ofSeconds(120));

        public Deadlines(Duration uiThread, Duration calculation)
        {
            this(uiThread, calculation, DEFAULT_CALCULATION_QUEUE);
        }
    }

    /** the host plus how long to wait for its UI thread - everything a marshalled handler needs */
    private record UiThread(HostApplication host, Duration deadline)
    {
    }

    /**
     * Runs the calculations, on platform threads: the work is CPU-bound and long,
     * and on virtual threads it would compete for the same few carriers as every
     * HTTP exchange. The slots bound how many threads this pool ever holds.
     */
    private static final ExecutorService CALCULATIONS = Executors.newCachedThreadPool(Thread.ofPlatform()
                    .name("Portfolio Performance REST API calculation ", 0).daemon(true).factory()); //$NON-NLS-1$

    /**
     * How many calculations run at once, and how many may wait for a turn.
     * <p/>
     * A calculation that outlasts its deadline is not stopped by the interrupt -
     * the engines never check for one - so it holds its slot until it really
     * ends, not until the client stops waiting. Without that, every retry after a
     * timeout would start another calculation next to the ones still running.
     * <p/>
     * A calculation that finds every slot taken waits for one, first come first
     * served, rather than being refused at once: a model asks several questions
     * in one turn, and refusing the third of them only makes it ask again. The
     * wait is bounded twice - in time, and in how many may wait - so that it
     * cannot pile up.
     * <p/>
     * Not private: a test in this package drives it directly.
     */
    /* package */ static final class CalculationSlots
    {
        /** how often a waiting calculation looks whether its caller gave up */
        private static final long POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(250);

        private final int slots;
        private final int queueLimit;
        private final Duration queueWait;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition changed = lock.newCondition();
        /** the waiting calculations, first come first; only the head may take a slot */
        private final ArrayDeque<Object> queue = new ArrayDeque<>();
        private int running;

        /* package */ CalculationSlots(int slots, int queueLimit, Duration queueWait)
        {
            this.slots = slots;
            this.queueLimit = queueLimit;
            this.queueWait = queueWait;
        }

        /* package */ void acquire() throws InterruptedException
        {
            acquire(Progress.NONE);
        }

        /**
         * Takes a slot, waiting for one within the bounds, or refuses as
         * {@code server-busy}.
         * <p/>
         * The queue is kept here rather than in a fair semaphore, because the
         * wait has to look up now and then: to tell the caller how many are
         * still ahead, and to leave the queue when the caller gave up. A
         * semaphore waited on in short turns would send the waiter to the back
         * every time.
         */
        /* package */ void acquire(Progress progress) throws InterruptedException
        {
            var me = new Object();

            lock.lock();
            try
            {
                // free and nobody waiting: no turn to take
                if (queue.isEmpty() && running < slots)
                {
                    running++;
                    return;
                }

                if (queue.size() >= queueLimit)
                    throw ApiException.serverBusy();

                queue.addLast(me);
            }
            finally
            {
                lock.unlock();
            }

            var deadline = System.nanoTime() + queueWait.toNanos();
            var reported = -1;
            try
            {
                while (true)
                {
                    progress.checkCancelled();

                    int ahead;
                    lock.lock();
                    try
                    {
                        if (queue.peekFirst() == me && running < slots)
                        {
                            running++;
                            return;
                        }

                        var left = deadline - System.nanoTime();
                        if (left <= 0)
                            throw ApiException.serverBusy();

                        ahead = indexOf(me);
                        if (ahead == reported)
                        {
                            changed.awaitNanos(Math.min(left, POLL_NANOS));
                            continue;
                        }
                    }
                    finally
                    {
                        lock.unlock();
                    }

                    // reported outside the lock: sending it may take as long as
                    // the client takes to read it
                    reported = ahead;
                    progress.phase(ahead == 0 ? "waiting for a calculation slot to come free" //$NON-NLS-1$
                                    : "waiting for a calculation slot, " + ahead + " ahead"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            finally
            {
                lock.lock();
                try
                {
                    queue.remove(me);
                    changed.signalAll();
                }
                finally
                {
                    lock.unlock();
                }
            }
        }

        /* package */ void release()
        {
            lock.lock();
            try
            {
                running--;
                changed.signalAll();
            }
            finally
            {
                lock.unlock();
            }
        }

        /* package */ int available()
        {
            lock.lock();
            try
            {
                return slots - running;
            }
            finally
            {
                lock.unlock();
            }
        }

        /* package */ int waiting()
        {
            lock.lock();
            try
            {
                return queue.size();
            }
            finally
            {
                lock.unlock();
            }
        }

        private int indexOf(Object waiter)
        {
            var index = 0;
            for (var entry : queue)
            {
                if (entry == waiter)
                    return index;
                index++;
            }
            return index;
        }
    }

    /**
     * On a PATCH: {@code true} when a stored value changed and the file now has
     * unsaved changes, {@code false} when the patch only restated what was
     * already there.
     */
    public static final String CHANGED_HEADER = "X-Portfolio-Changed"; //$NON-NLS-1$

    private ApiRoutes()
    {
    }

    public static Router create(FileAccessRegistry registry, HostApplication host, PairingService pairing)
    {
        return create(registry, host, pairing, Deadlines.DEFAULT);
    }

    public static Router create(FileAccessRegistry registry, HostApplication host, PairingService pairing,
                    Deadlines deadlines)
    {
        var ui = new UiThread(host, deadlines.uiThread());
        var calculation = new Calculation(deadlines.calculation(), new CalculationSlots(
                        RestApiLimits.calculationSlots(), RestApiLimits.calculationQueue(), deadlines.calculationQueue()));
        var router = new Router();
        var resolver = new FileResolver(registry, host);
        var files = new FilesHandler(registry, host);

        // the MCP front door: registered on this same table and dispatching
        // back into it, so the sixteen tools cannot drift from the routes they
        // are - and GET and DELETE get the router's own 405
        McpEndpoint.register(router);

        // the API's own description: a static resource, no UI thread, no auth
        router.add("GET", RestApiConstants.OPENAPI_ENDPOINT, request -> OpenApiHandler.serve()); //$NON-NLS-1$

        // the contract version: likewise static, and cheap enough to poll
        router.add("GET", RestApiConstants.VERSION_ENDPOINT, request -> VersionHandler.serve()); //$NON-NLS-1$

        // pairing endpoints run on the HTTP worker thread: the service is
        // thread-safe and prompting the user is asynchronous by contract
        router.add("POST", "/v1/auth/requests", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> PairingHandler.create(pairing, parseObject(request)));
        router.add("GET", "/v1/auth/requests/{id}", //$NON-NLS-1$ //$NON-NLS-2$
                        request -> PairingHandler.poll(pairing, request.pathParam("id"))); //$NON-NLS-1$

        router.add("GET", "/v1/files", onUiThread(ui, true, files::list)); //$NON-NLS-1$ //$NON-NLS-2$

        router.add("GET", "/v1/files/{file}/instruments", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> SecuritiesHandler.list(client, req.queryParam("query"))), //$NON-NLS-1$
                        "query"); //$NON-NLS-1$
        // literal sub-collection: the Router prefers it over the {uuid} route
        // because it has fewer placeholders, whatever the registration order
        router.add("GET", "/v1/files/{file}/instruments/attribute-types", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> SecuritiesHandler.attributeTypes(client)));
        router.add("GET", "/v1/files/{file}/instruments/{uuid}", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> SecuritiesHandler.get(client, req.pathParam("uuid")))); //$NON-NLS-1$
        router.add("PATCH", "/v1/files/{file}/instruments/{uuid}", write(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (file, req) -> {
                            var result = SecuritiesHandler.patch(file.getClient(), req.pathParam("uuid"), //$NON-NLS-1$
                                            parseObject(req));
                            InstrumentChangeLog.record(file.getLabel(), result.instrumentName(), result.changes());
                            // whether a stored value changed - and with it whether the
                            // file now has unsaved changes - which the body cannot say
                            return Response.json(200, result.entity()).withHeader(CHANGED_HEADER,
                                            String.valueOf(!result.changes().isEmpty()));
                        }));
        router.add("DELETE", "/v1/files/{file}/instruments/{uuid}", write(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (file, req) -> {
                            var name = SecuritiesHandler.delete(file.getClient(), req.pathParam("uuid")); //$NON-NLS-1$
                            InstrumentChangeLog.recordDeletion(file.getLabel(), name);
                            return Response.noContent();
                        }));

        router.add("GET", "/v1/files/{file}/cash-accounts", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> AccountsHandler.list(client)));
        router.add("GET", "/v1/files/{file}/cash-accounts/{uuid}", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> AccountsHandler.get(client, req.pathParam("uuid")))); //$NON-NLS-1$

        router.add("GET", "/v1/files/{file}/investment-accounts", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> PortfoliosHandler.list(client)));
        router.add("GET", "/v1/files/{file}/investment-accounts/{uuid}", read(resolver, ui, //$NON-NLS-1$ //$NON-NLS-2$
                        (client, req) -> PortfoliosHandler.get(client, req.pathParam("uuid")))); //$NON-NLS-1$

        router.add("GET", "/v1/files/{file}/holdings", calc(resolver, ui, calculation, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, HoldingsHandler.list(context.client(), context.factory(),
                                        req.queryParam("date"), req.queryParam("reportingCurrency")))), //$NON-NLS-1$ //$NON-NLS-2$
                        "date", "reportingCurrency"); //$NON-NLS-1$ //$NON-NLS-2$

        router.add("GET", "/v1/files/{file}/performance", calc(resolver, ui, calculation, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, PerformanceHandler.list(context.client(),
                                        context.factory(), req.queryParam("openingDate"), //$NON-NLS-1$
                                        req.queryParam("closingDate"), req.queryParam("reportingCurrency"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("costMethod")))), //$NON-NLS-1$
                        "openingDate", "closingDate", "reportingCurrency", "costMethod"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        // the performance report broken down by instrument. Five segments like
        // /files/{file}/instruments/{uuid}, but segment 4 is a literal in both
        // and they differ, so first-match routing cannot confuse the two and no
        // registration-order guard is needed
        router.add("GET", "/v1/files/{file}/performance/instruments", calc(resolver, ui, calculation, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, InstrumentPerformanceHandler.list(context.client(),
                                        context.factory(), req.queryParam("openingDate"), //$NON-NLS-1$
                                        req.queryParam("closingDate"), req.queryParam("reportingCurrency"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("costMethod"), req.queryParam("taxesAndFees"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("metrics")))), //$NON-NLS-1$
                        INSTRUMENT_PERFORMANCE_PARAMS);
        router.add("GET", "/v1/files/{file}/performance/instruments/{uuid}", calc(resolver, ui, calculation, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, InstrumentPerformanceHandler.get(context.client(),
                                        context.factory(), req.pathParam("uuid"), req.queryParam("openingDate"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("closingDate"), req.queryParam("reportingCurrency"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("costMethod"), req.queryParam("taxesAndFees"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("metrics")))), //$NON-NLS-1$
                        INSTRUMENT_PERFORMANCE_PARAMS);

        // matched trades for the whole file or one instrument sub-collection
        router.add("GET", "/v1/files/{file}/trades", calc(resolver, ui, calculation, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, TradesHandler.list(context.client(), context.factory(),
                                        req.queryParam("status"), req.queryParam("grouping"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("costMethod"), req.queryParam("taxesAndFees"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("reportingCurrency"), req.queryParam("from"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("to")))), //$NON-NLS-1$
                        TRADE_PARAMS);
        router.add("GET", "/v1/files/{file}/instruments/{uuid}/trades", calc(resolver, ui, calculation, //$NON-NLS-1$ //$NON-NLS-2$
                        (context, req) -> Response.json(200, TradesHandler.forInstrument(context.client(),
                                        context.factory(), req.pathParam("uuid"), req.queryParam("status"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("grouping"), req.queryParam("costMethod"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("taxesAndFees"), req.queryParam("reportingCurrency"), //$NON-NLS-1$ //$NON-NLS-2$
                                        req.queryParam("from"), req.queryParam("to")))), //$NON-NLS-1$ //$NON-NLS-2$
                        TRADE_PARAMS);

        return router;
    }

    /**
     * Runs the handler on the UI thread. Everything that touches the open files
     * or the model must go through here - including resolving the {file}
     * segment, which reads the list of open files.
     */
    private static Router.Handler onUiThread(UiThread ui, boolean idempotent, Router.Handler handler)
    {
        return request -> uiExecBounded(ui.host(), () -> handler.handle(request), ui.deadline(), idempotent,
                        request.progress());
    }

    /** the deadline of a calculation and the slots that bound how many run at once */
    private record Calculation(Duration deadline, CalculationSlots slots)
    {
    }

    /** how long the UI thread may take before the caller is told that it waits */
    private static final long UI_QUICK_MILLIS = 500;

    /** how often a running calculation looks whether its caller gave up */
    private static final long CALCULATION_POLL_MILLIS = 250;

    private static final int NOT_STARTED = 0;
    private static final int STARTED = 1;
    private static final int ABANDONED = 2;

    /**
     * {@link HostApplication#asyncExec} with a deadline, and with exactly two
     * outcomes when it passes.
     * <p/>
     * The caller parks on the future, which holds no thread: a request given
     * up on leaves only the queued callable behind, which does nothing once the
     * UI thread reaches it.
     * <p/>
     * Whether the request ran is decided on the UI thread itself, by one
     * compare-and-set against the caller's: the callable starts only if the
     * caller has not given up yet, and the caller gives up only if the callable
     * has not started. So a request the UI thread never took on is refused as
     * {@code ui-busy} and is guaranteed never to run - the client may send it
     * again - while one that had started is reported as
     * {@code ui-request-underway}, because it will finish and a second write
     * would apply the same change twice.
     * <p/>
     * A read changes nothing, so sending it again is safe even when it had
     * started - and it is the only way to get the answer that was thrown away.
     * An {@code idempotent} request is therefore always refused as retryable.
     * <p/>
     * Not private: a test in this package drives all outcomes directly.
     */
    /* package */ static <T> T uiExecBounded(HostApplication host, Callable<T> callable, Duration deadline)
                    throws Exception
    {
        return uiExecBounded(host, callable, deadline, false);
    }

    /* package */ static <T> T uiExecBounded(HostApplication host, Callable<T> callable, Duration deadline,
                    boolean idempotent) throws Exception
    {
        return uiExecBounded(host, callable, deadline, idempotent, Progress.NONE);
    }

    /* package */ static <T> T uiExecBounded(HostApplication host, Callable<T> callable, Duration deadline,
                    boolean idempotent, Progress progress) throws Exception
    {
        var state = new AtomicInteger(NOT_STARTED);

        var future = host.asyncExec(() -> {
            if (!state.compareAndSet(NOT_STARTED, STARTED))
                // the caller gave up before the UI thread got here; the model
                // is not touched
                return null;
            return callable.call();
        });

        try
        {
            // most requests are taken on at once; only one that is not says so
            var quick = Math.min(UI_QUICK_MILLIS, deadline.toMillis());
            try
            {
                return future.get(quick, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                if (quick >= deadline.toMillis())
                    throw e;
                progress.phase("waiting for Portfolio Performance to take the request on"); //$NON-NLS-1$
                return future.get(deadline.toMillis() - quick, TimeUnit.MILLISECONDS);
            }
        }
        catch (TimeoutException e)
        {
            if (state.compareAndSet(NOT_STARTED, ABANDONED))
                throw ApiException.uiBusy(deadline.toMillis());

            if (idempotent)
                throw ApiException.uiReadTimedOut(deadline.toMillis());

            throw ApiException.uiRequestUnderway(deadline.toMillis());
        }
        catch (ExecutionException e)
        {
            throw unwrap(e);
        }
    }

    /**
     * Runs a calculation with a deadline. A calculation reads the model off the
     * UI thread and changes nothing, so giving up on it is safe: the client is
     * told to ask a smaller question, and the thread is interrupted - the
     * computation may not notice and finish on its own, but its result is
     * discarded.
     * <p/>
     * The slot is taken before the work starts and given back when the work
     * ends - not when the client stops waiting - so a calculation still running
     * after its deadline keeps counting. With every slot taken, the request
     * waits for one in turn; it is refused as {@code server-busy} only when the
     * queue is full or its wait runs out. The deadline counts from the start of
     * the work, not from the arrival, so a turn in the queue never shortens it.
     * <p/>
     * Not private: a test in this package drives it with a calculation that
     * outlasts its deadline.
     */
    /* package */ static Response computeBounded(Callable<Response> calculation, Duration deadline,
                    CalculationSlots slots) throws Exception
    {
        return computeBounded(calculation, deadline, slots, Progress.NONE);
    }

    /**
     * As above, reporting to the progress, and stopping when its caller gave
     * up: a calculation still waiting for a slot leaves the queue, and a
     * running one stops at the next point where this API's own loops check -
     * the engines underneath do not, so one inside them runs to its end and
     * keeps its slot until then.
     */
    /* package */ static Response computeBounded(Callable<Response> calculation, Duration deadline,
                    CalculationSlots slots, Progress progress) throws Exception
    {
        return computeBounded(calculation, deadline, slots, progress, CALCULATIONS);
    }

    /**
     * As above, on the given threads. Not private: a test in this package
     * hands it threads that are all busy, so that a calculation is given up on
     * before it ever starts.
     */
    /* package */ static Response computeBounded(Callable<Response> calculation, Duration deadline,
                    CalculationSlots slots, Progress progress, ExecutorService executor) throws Exception
    {
        slots.acquire(progress);

        // the slot goes back exactly once: from the task when it started, or
        // from here when it never will - a task cancelled before a thread took
        // it up never runs, and with it never its finally
        var claimed = new AtomicBoolean();

        Future<Response> future;
        try
        {
            progress.phase("calculating"); //$NON-NLS-1$

            future = executor.submit(() -> {
                if (!claimed.compareAndSet(false, true))
                    // given up on before it started; the slot is already back
                    return null;
                try
                {
                    return Progress.runWith(progress, calculation);
                }
                finally
                {
                    slots.release();
                }
            });
        }
        catch (RuntimeException e)
        {
            // never submitted, so the finally above never runs
            slots.release();
            throw e;
        }

        var end = System.nanoTime() + deadline.toNanos();
        while (true)
        {
            var left = TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime());
            if (left <= 0)
            {
                abandon(future, claimed, slots);
                throw ApiException.calculationTimeout(deadline.toSeconds());
            }

            try
            {
                return future.get(Math.min(left, CALCULATION_POLL_MILLIS), TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e)
            {
                if (progress.isCancelled())
                {
                    abandon(future, claimed, slots);
                    throw new CancellationException("the client cancelled the request"); //$NON-NLS-1$
                }
            }
            catch (ExecutionException e)
            {
                throw unwrap(e);
            }
        }
    }

    /**
     * Gives up on a calculation: interrupts it when it runs, and gives its slot
     * back when no thread has taken it up yet - otherwise it would stay taken
     * for good, because the task that releases it is never run.
     */
    private static void abandon(Future<Response> future, AtomicBoolean claimed, CalculationSlots slots)
    {
        future.cancel(true);
        if (claimed.compareAndSet(false, true))
            slots.release();
    }

    private static Exception unwrap(ExecutionException e)
    {
        var cause = e.getCause();
        if (cause instanceof Exception exception)
            return exception;
        if (cause instanceof Error error)
            throw error;
        return e;
    }

    /**
     * For reads of the model: the JSON tree is built on the UI thread, which is
     * where the model may be read, and written out as text after it - a tree of
     * strings and numbers is detached from the model, and serializing a large
     * file's instruments is time the UI thread does not need to spend.
     */
    private static Router.Handler read(FileResolver resolver, UiThread ui,
                    BiFunction<Client, Request, JsonElement> body)
    {
        return request -> Response.json(200, uiExecBounded(ui.host(), () -> {
            var resolved = resolver.resolve(request.pathParam("file")); //$NON-NLS-1$
            return body.apply(resolved.file().getClient(), request);
        }, ui.deadline(), true, request.progress()));
    }

    /** what a calculation endpoint needs, fetched from the host on the UI thread */
    /* package */ record CalcContext(Client client, ExchangeRateProviderFactory factory)
    {
    }

    /**
     * For read-only calculation endpoints: resolves the {file} scope on the UI
     * thread, but runs the calculation itself off it so that an expensive
     * computation cannot freeze the UI, and within a deadline so that it cannot
     * leave the client hanging. Deliberately without a consistency guard: a
     * concurrent user edit may - rarely - yield a transiently inconsistent
     * response or an internal error; retrying is cheap for the client, blocking
     * the UI is not.
     */
    private static Router.Handler calc(FileResolver resolver, UiThread ui, Calculation calculation,
                    BiFunction<CalcContext, Request, Response> body)
    {
        return request -> {
            var context = uiExecBounded(ui.host(), () -> {
                var file = resolver.resolve(request.pathParam("file")).file(); //$NON-NLS-1$
                return new CalcContext(file.getClient(), file.getExchangeRateProviderFactory());
            }, ui.deadline(), true, request.progress());
            return computeBounded(() -> body.apply(context, request), calculation.deadline(), calculation.slots(),
                            request.progress());
        };
    }

    private static Router.Handler write(FileResolver resolver, UiThread ui,
                    BiFunction<OpenFile, Request, Response> body)
    {
        return onUiThread(ui, false, request -> {
            // resolve first: an unknown file is a 404 even while the user edits
            var resolved = resolver.resolve(request.pathParam("file")); //$NON-NLS-1$

            if (ui.host().isUserEditing())
                throw ApiException.locked();

            return body.apply(resolved.file(), request);
        });
    }

    private static JsonObject parseObject(Request request)
    {
        try
        {
            var element = JsonParser.parseString(new String(request.body(), StandardCharsets.UTF_8));
            if (!element.isJsonObject())
                throw ApiException.badRequest("request body must be a JSON object"); //$NON-NLS-1$
            return element.getAsJsonObject();
        }
        catch (JsonSyntaxException e)
        {
            throw ApiException.badRequest("request body is not valid JSON"); //$NON-NLS-1$
        }
    }
}
