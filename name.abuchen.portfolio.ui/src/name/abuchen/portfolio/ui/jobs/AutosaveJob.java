package name.abuchen.portfolio.ui.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class AutosaveJob extends AbstractClientJob
{
    private ClientInput clientInput;
    private long repeatPeriod;

    public AutosaveJob(ClientInput clientInput)
    {
        super(clientInput.getClient(), Messages.JobLabelAutosave);
        this.clientInput = clientInput; 
    }

    public AutosaveJob repeatEvery(long milliseconds)
    {
        this.repeatPeriod = milliseconds;
        return this;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        if (repeatPeriod > 0)
            schedule(repeatPeriod);

        if (clientInput.isDirty() && clientInput.getFile() != null)
        {
            File file = clientInput.getFile();
            monitor.beginTask(Messages.JobLabelAutosave, 10);
            String filename = file.getName();
            String suffix = "autosave";  //$NON-NLS-1$
            int l = filename.lastIndexOf('.');
            String autosaveName = l > 0 ? filename.substring(0, l) + '.' + suffix + filename.substring(l)
                            : filename + '.' + suffix;
            Path sourceFile = file.toPath();
            File autosaveFile = sourceFile.resolveSibling(autosaveName).toFile();
            try
            {
                ClientFactory.save(clientInput.getClient(), autosaveFile, null, null);
            }
            catch (IOException e)
            {
                return new Status(IStatus.WARNING, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e);
            }
            monitor.worked(1);            
        }

        return Status.OK_STATUS;
    }
}
