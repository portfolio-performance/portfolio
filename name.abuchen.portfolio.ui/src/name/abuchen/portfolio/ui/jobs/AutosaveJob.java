package name.abuchen.portfolio.ui.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.ClientInput;

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
        List<Security> toBeSynced = getClient().getSecurities().stream().filter(s -> s.getOnlineId() != null)
                        .collect(Collectors.toList());
        System.err.println(">>>> AutosaveJob client: " + clientInput.getClient().toString() + " Dirty: " + clientInput.isDirty());

        if (repeatPeriod > 0)
            schedule(repeatPeriod);

        System.err.println(">>>> AutosaveJob repeatPeriod: " + repeatPeriod);

        if (clientInput.isDirty() && clientInput.getFile() != null)
        {
            File file = clientInput.getFile();
            System.err.println(">>>> AutosaveJob clientInput: " + file.toString());
            monitor.beginTask(Messages.JobLabelAutosave, 10);
            String filename = file.getName();
            String suffix = "autosave";  //$NON-NLS-1$
            int l = filename.lastIndexOf('.');
            String autosaveName = l > 0 ? filename.substring(0, l) + '.' + suffix + filename.substring(l)
                            : filename + '.' + suffix;
            Path sourceFile = file.toPath();
            System.err.println(">>>> AutosaveJob autosaveName: " + autosaveName);
            File autosaveFile = sourceFile.resolveSibling(autosaveName).toFile();
            System.err.println(">>>> AutosaveJob autosaveFile: " + autosaveFile.toString());
            try
            {
                ClientFactory.save(clientInput.getClient(), autosaveFile, null, null);
            }
            catch (IOException e)
            {
                System.err.println(">>>> AutosaveJob Error: " + e.getMessage());
            }
            System.err.println(">>>> AutosaveJob Monitor: " + monitor.toString());
            System.err.println(">>>> AutosaveJob File: " + autosaveFile.toString());
            monitor.worked(1);            
        }

        return Status.OK_STATUS;
    }
}
