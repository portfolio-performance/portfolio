package name.abuchen.portfolio.ui;

import java.io.File;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

import org.eclipse.core.runtime.IProgressMonitor;

public class LoadClientThread extends Thread
{
    public interface Callback
    {
        void setClient(Client client);

        void setErrorMessage(String message);
    }

    private final IProgressMonitor monitor;
    private final Callback callback;
    private final File file;

    public LoadClientThread(IProgressMonitor monitor, Callback callback, File file)
    {
        this.monitor = monitor;
        this.callback = callback;
        this.file = file;
    }

    @Override
    public void run()
    {
        try
        {
            Client client = ClientFactory.load(file, monitor);
            callback.setClient(client);
        }
        catch (Throwable t)
        {
            callback.setErrorMessage(t.getMessage());
            PortfolioPlugin.log(t);
        }
    }
}
