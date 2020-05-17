package name.abuchen.portfolio.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInput;

public final class AutoSaveJob extends AbstractClientJob
{
    private ClientInput clientInput;
    private int delay;
    
    public AutoSaveJob(Client client, ClientInput clientInput, int delay)
    {
        super(client, Messages.JobLabelAutoSave);
        this.clientInput = clientInput;
        this.delay = delay;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        
        clientInput.autoSave();
        
        schedule(delay); 

        return Status.OK_STATUS;
    }
}
