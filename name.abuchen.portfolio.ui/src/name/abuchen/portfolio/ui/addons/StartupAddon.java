package name.abuchen.portfolio.ui.addons;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.log.LogEntryCache;
import name.abuchen.portfolio.ui.update.UpdateHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.UIEvents;

public class StartupAddon
{
    @PostConstruct
    public void initLogEntryCache(LogEntryCache cache)
    {
        // force creation of log entry cache
    }
    
    @Inject
    @Optional
    public void checkForUpdates(@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) MApplication application)
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
                    catch (CoreException e)
                    {
                        PortfolioPlugin.log(e.getStatus());
                    }
                    return Status.OK_STATUS;
                }

            };
            job.setSystem(true);
            job.schedule(500);
        }
    }
}
