package name.abuchen.portfolio.rest.testsupport;

import java.util.List;
import java.util.concurrent.Callable;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;

public class FakeHost implements HostApplication
{
    public record FakeOpenFile(String path, String label, Client client) implements OpenFile
    {
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
    }

    private final List<OpenFile> openFiles;
    private boolean userEditing = false;

    private int syncExecDepth = 0;
    private boolean accessedOutsideUIThread = false;

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

    @Override
    public <T> T syncExec(Callable<T> callable) throws Exception
    {
        syncExecDepth++;

        try
        {
            return callable.call();
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
}
