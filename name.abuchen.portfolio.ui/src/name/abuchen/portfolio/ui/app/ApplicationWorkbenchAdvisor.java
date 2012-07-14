package name.abuchen.portfolio.ui.app;

import java.io.IOException;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.update.UpdateHelper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{

    private static final String PERSPECTIVE_ID = "name.abuchen.portfolio.perspective"; //$NON-NLS-1$

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    public String getInitialWindowPerspectiveId()
    {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer)
    {
        super.initialize(configurer);
        configurer.setSaveAndRestore(true);
    }

    @Override
    public void postStartup()
    {
        boolean autoUpdate = PortfolioPlugin.getDefault().getPreferenceStore()
                        .getBoolean(PortfolioPlugin.Preferences.AUTO_UPDATE);
        if (autoUpdate)
        {
            Job job = new Job(Messages.JobMsgCheckingForUpdates)
            {

                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    try
                    {
                        UpdateHelper updateHelper = new UpdateHelper();
                        updateHelper.runUpdate(monitor, true);
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                    return Status.OK_STATUS;
                }

            };
            job.setSystem(true);
            job.schedule(500);
        }
    }

}
