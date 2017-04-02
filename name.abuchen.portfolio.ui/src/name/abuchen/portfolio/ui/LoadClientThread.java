package name.abuchen.portfolio.ui;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

public class LoadClientThread extends Thread
{
    public interface Callback
    {
        void setClient(Client client);

        void setErrorMessage(String message);
    }

    private final IEventBroker broker;
    private final IProgressMonitor monitor;
    private final Callback callback;
    private final File file;
    private final char[] password;

    public LoadClientThread(IEventBroker broker, IProgressMonitor monitor, Callback callback, File file,
                    char[] password)
    {
        this.broker = broker;
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
        catch (FileNotFoundException exception)
        {
            broker.post(UIConstants.Event.File.REMOVED, file.getAbsolutePath());
            callback.setErrorMessage(exception.getMessage());
            PortfolioPlugin.log(exception);
        }
        catch (Exception exception)
        {
            callback.setErrorMessage(exception.getMessage());
            PortfolioPlugin.log(exception);
        }
    }
}
