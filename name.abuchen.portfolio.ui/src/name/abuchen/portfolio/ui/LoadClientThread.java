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
    private final char[] password;

    public LoadClientThread(IProgressMonitor monitor, Callback callback, File file, char[] password)
    {
        this.monitor = monitor;
        this.callback = callback;
        this.file = file;
        this.password = password;
    }

    @Override
    public void run()
    {
        try
        {
            Client client = ClientFactory.load(file, password, monitor);
            callback.setClient(client);
        }
        catch (Exception exception)
        {
            callback.setErrorMessage(exception.getMessage());
            PortfolioPlugin.log(exception);
        }
    }
}
