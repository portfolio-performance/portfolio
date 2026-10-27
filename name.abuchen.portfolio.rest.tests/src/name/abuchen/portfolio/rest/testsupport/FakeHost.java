package name.abuchen.portfolio.rest.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

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

    private int syncExecDepth = 0;
    private boolean accessedOutsideUIThread = false;
    private final List<Object> syncExecResults = new ArrayList<>();
    private CountDownLatch syncExecGate;
    private int callablesRun;
    private Throwable callableFailure;

    public FakeHost(List<OpenFile> openFiles)
    {
        this.openFiles = openFiles;
    }

    /**
     * Makes {@link #syncExec} wait on a latch before running anything, which is
     * how a test shows a UI thread busy with something else. Releasing the latch
     * stands for the UI thread getting round to the queued callable after the
     * client already gave up.
     */
    public void blockSyncExecUntil(CountDownLatch gate)
    {
        this.syncExecGate = gate;
    }

    /** how many callables actually ran on the UI thread */
    public int callablesRun()
    {
        return callablesRun;
    }

    /** the first exception a syncExec callable raised, if any */
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
     * {@link #syncExec(Callable)}, i.e. off the UI thread in the real
     * application.
     */
    public boolean hasAccessedOutsideUIThread()
    {
        return accessedOutsideUIThread;
    }

    @Override
    public List<OpenFile> listOpenFiles()
    {
        if (syncExecDepth == 0)
            accessedOutsideUIThread = true;

        return openFiles;
    }

    /**
     * The values the syncExec callables returned - lets a test verify what was
     * (and was not) computed on the UI thread.
     */
    public List<Object> syncExecResults()
    {
        return syncExecResults;
    }

    @Override
    public <T> T syncExec(Callable<T> callable) throws Exception
    {
        syncExecDepth++;

        try
        {
            var gate = syncExecGate;
            if (gate != null)
                gate.await();

            callablesRun++;

            try
            {
                T result = callable.call();
                syncExecResults.add(result);
                return result;
            }
            catch (Throwable t)
            {
                if (callableFailure == null)
                    callableFailure = t;
                throw t;
            }
        }
        finally
        {
            syncExecDepth--;
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
