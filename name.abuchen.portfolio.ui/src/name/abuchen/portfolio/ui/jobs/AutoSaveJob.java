package name.abuchen.portfolio.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.ClientInput;

public final class AutoSaveJob extends AbstractClientJob
{
    private ClientInput clientInput;
    private long delay;

    public AutoSaveJob(ClientInput clientInput, long delay)
    {
        super(clientInput.getClient(), Messages.JobLabelAutoSave);
        this.clientInput = clientInput;
        this.delay = delay;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        // 0 means not to autosave at all
        if (delay != 0L)
        {
            try
            {
                PortfolioPlugin.info("Auto-saving " + clientInput.getLabel()); //$NON-NLS-1$
                this.clientInput.autoSave();
            }
            finally
            {
                schedule(delay);
            }
        }
        return Status.OK_STATUS;
    }

    public long getDelay()
    {
        return delay;
    }

    public void setDelay(long delay)
    {
        this.delay = delay;
    }
}
