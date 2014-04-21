package name.abuchen.portfolio.ui;

import java.io.File;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class LoadClientJob extends Job
{
    public interface Callback
    {
        void setClient(Client client);

        void setErrorMessage(String message);
    }

    private final Callback callback;
    private final File file;

    public LoadClientJob(Callback callback, File file)
    {
        super("Loading " + file);
        this.callback = callback;
        this.file = file;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        Throwable exp = null;
        String msg = null;

        try
        {
            Client client = ClientFactory.load(file);
            callback.setClient(client);
            return Status.OK_STATUS;
        }
        catch (Throwable t)
        {
            exp = t;
            msg = t.getMessage();
        }

        callback.setErrorMessage(msg);

        return new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, msg, exp);
    }

}
