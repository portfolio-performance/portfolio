package name.abuchen.portfolio.rest.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

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

    public FakeHost(List<OpenFile> openFiles)
    {
        this.openFiles = openFiles;
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
            T result = callable.call();
            syncExecResults.add(result);
            return result;
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
