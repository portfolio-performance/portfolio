package name.abuchen.portfolio.ui.editor;

import java.io.FileNotFoundException;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

/* package */ class LoadClientThread extends Thread
{
    private final ClientInput clientInput;
    private final IEventBroker broker;
    private final ProgressProvider progressProvider;
    private final char[] password;

    public LoadClientThread(ClientInput clientInput, IEventBroker broker, ProgressProvider progressProvider,
                    char[] password)
    {
        this.clientInput = clientInput;
        this.broker = broker;
        this.progressProvider = progressProvider;
        this.password = password;
    }

    @Override
    public void run()
    {
        try
        {
            Client client = ClientFactory.load(clientInput.getFile(), password, progressProvider.createMonitor());

            Display.getDefault().asyncExec(() -> clientInput.setClient(client));
        }
        catch (FileNotFoundException exception)
        {
            broker.post(UIConstants.Event.File.REMOVED, clientInput.getFile().getAbsolutePath());
            Display.getDefault().asyncExec(() -> clientInput.setErrorMessage(exception.getMessage()));
            PortfolioPlugin.log(exception);
        }
        catch (Exception exception)
        {
            Display.getDefault().asyncExec(() -> clientInput.setErrorMessage(exception.getMessage()));
            PortfolioPlugin.log(exception);
        }
    }
}
