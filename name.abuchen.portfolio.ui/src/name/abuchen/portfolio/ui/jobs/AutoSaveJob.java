package name.abuchen.portfolio.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInput;

public final class AutoSaveJob extends AbstractClientJob
{
    private ClientInput clientInput;
    private long delay;
    private long heartbeat;
    private long lastRun = 0L;

    public AutoSaveJob(ClientInput clientInput, long delay, long heartbeat)
    {
        super(clientInput.getClient(), Messages.JobLabelAutoSave);
        this.clientInput = clientInput;
        this.delay = delay;
        this.heartbeat = heartbeat;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        // get value from preferences to check, if the value changed
        long newDelay = clientInput.getAutoSavePrefs();

        // 0 means not to autosave at all
        if (newDelay != 0L)
        {
            // if the value changed, use the new value
            if (delay != newDelay)
            {
                delay = newDelay;
            }

            // check, if time till last run passed and new run is needed
            long now = System.currentTimeMillis();
            long diff = now - this.lastRun;
            if (diff > delay)
            {
                this.clientInput.autoSave();
                this.lastRun = now;
            }
        }

        schedule(heartbeat);
        return Status.OK_STATUS;
    }
}
