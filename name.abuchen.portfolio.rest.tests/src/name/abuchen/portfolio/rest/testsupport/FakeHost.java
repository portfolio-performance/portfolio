package name.abuchen.portfolio.rest.testsupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.rest.spi.ApiAccessRequest;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;

public class FakeHost implements HostApplication
{
    public record FakeOpenFile(String path, String label, Client client, ExchangeRateProviderFactory factory)
                    implements OpenFile
    {
        public FakeOpenFile(String path, String label, Client client)
        {
            this(path, label, client, new ExchangeRateProviderFactory(client));
        }

        @Override
        public String getPath()
        {
            return path;
        }

        @Override
        public String getLabel()
        {
            return label;
        }

        @Override
        public Client getClient()
        {
            return client;
        }

        @Override
        public ExchangeRateProviderFactory getExchangeRateProviderFactory()
        {
            return factory;
        }
    }

    private final List<OpenFile> openFiles;
    private boolean userEditing = false;
    private ApiAccessRequest lastAccessRequest;

    /** marks the thread that runs a callable as this host's UI thread while it does */
    private final ThreadLocal<Boolean> onUiThread = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * The UI thread: one thread, so callables run one after the other in the
     * order they were queued, as on SWT's. Its thread ends when idle, so that a
     * test run does not keep one per host.
     */
    private final ThreadPoolExecutor uiThread;

    private volatile boolean accessedOutsideUIThread = false;
    // read by tests from another thread than the one that ran the callable
    private final List<Object> uiThreadResults = Collections.synchronizedList(new ArrayList<>());
    private volatile CountDownLatch uiThreadGate;
    private final AtomicInteger callablesRun = new AtomicInteger();
    private volatile Throwable callableFailure;

    public FakeHost(List<OpenFile> openFiles)
    {
        this.openFiles = openFiles;
        this.uiThread = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                        Thread.ofPlatform().name("fake UI thread").daemon(true).factory());
        this.uiThread.allowCoreThreadTimeOut(true);
    }

    /**
     * Makes the UI thread wait on a latch before running anything, which is how
     * a test shows a UI thread busy with something else. Releasing the latch
     * stands for the UI thread getting round to the queued callable after the
     * client already gave up.
     */
    public void blockUiThreadUntil(CountDownLatch gate)
    {
        this.uiThreadGate = gate;
    }

    /** how many callables actually ran on the UI thread */
    public int callablesRun()
    {
        return callablesRun.get();
    }

    /** the first exception a callable on the UI thread raised, if any */
    public Optional<Throwable> callableFailure()
    {
        return Optional.ofNullable(callableFailure);
    }

    public void setUserEditing(boolean userEditing)
    {
        this.userEditing = userEditing;
    }

    /**
     * Whether the open files were read without going through
     * {@link #asyncExec(Callable)}, i.e. off the UI thread in the real
     * application.
     */
    public boolean hasAccessedOutsideUIThread()
    {
        return accessedOutsideUIThread;
    }

    @Override
    public List<OpenFile> listOpenFiles()
    {
        if (!onUiThread.get())
            accessedOutsideUIThread = true;

        return openFiles;
    }

    /**
     * The values the callables on the UI thread returned - lets a test verify
     * what was (and was not) computed on the UI thread.
     */
    public List<Object> uiThreadResults()
    {
        return uiThreadResults;
    }

    @Override
    public <T> CompletableFuture<T> asyncExec(Callable<T> callable)
    {
        // on the UI thread already: run it, as the real host does - queueing it
        // behind the callable that is running would wait for ever
        if (onUiThread.get())
        {
            try
            {
                return CompletableFuture.completedFuture(run(callable));
            }
            catch (Throwable t)
            {
                return CompletableFuture.failedFuture(t);
            }
        }

        var future = new CompletableFuture<T>();
        uiThread.execute(() -> {
            onUiThread.set(Boolean.TRUE);
            try
            {
                var gate = uiThreadGate;
                if (gate != null)
                    gate.await();

                future.complete(run(callable));
            }
            catch (Throwable t)
            {
                future.completeExceptionally(t);
            }
            finally
            {
                onUiThread.set(Boolean.FALSE);
            }
        });
        return future;
    }

    private <T> T run(Callable<T> callable) throws Exception
    {
        callablesRun.incrementAndGet();

        try
        {
            T result = callable.call();
            uiThreadResults.add(result);
            return result;
        }
        catch (Throwable t)
        {
            if (callableFailure == null)
                callableFailure = t;
            throw t;
        }
    }

    @Override
    public boolean isUserEditing()
    {
        return userEditing;
    }

    @Override
    public void requestApiAccessApproval(ApiAccessRequest request)
    {
        this.lastAccessRequest = request;
    }

    /** the most recent access request the service asked the user about */
    public ApiAccessRequest lastAccessRequest()
    {
        return lastAccessRequest;
    }
}
