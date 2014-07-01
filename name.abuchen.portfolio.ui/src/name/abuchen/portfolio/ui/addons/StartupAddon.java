package name.abuchen.portfolio.ui.addons;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.log.LogEntryCache;
import name.abuchen.portfolio.ui.update.UpdateHelper;
import name.abuchen.portfolio.ui.util.ProgressMonitorFactory;
import name.abuchen.portfolio.util.IniFileManipulator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;

public class StartupAddon
{
    @PostConstruct
    public void setupProgressMontior(ProgressMonitorFactory factory)
    {
        IJobManager manager = Job.getJobManager();
        manager.setProgressProvider(factory);
    }

    @PostConstruct
    public void setupLogEntryCache(LogEntryCache cache)
    {
        // force creation of log entry cache
    }

    @PostConstruct
    public void unsetPersistedStateFlage()
    {
        // -clearPersistedState is set *after* installing new software, but must
        // be cleared for the next runs

        try
        {
            IniFileManipulator m = new IniFileManipulator();
            m.load();
            m.unsetClearPersistedState();
            if (m.isDirty())
                m.save();
        }
        catch (IOException ignore)
        {
            PortfolioPlugin.log(ignore);
        }
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
                        monitor.beginTask(Messages.JobMsgCheckingForUpdates, 200);
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

    @PostConstruct
    public void setMultipleWindowImages()
    {
        // setting window images
        // http://www.eclipse.org/forums/index.php/t/440442/
            
        Window.setDefaultImages(new Image[] { PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_512),
                        PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_256),
                        PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_128),
                        PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_48),
                        PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_32),
                        PortfolioPlugin.image(PortfolioPlugin.IMG_LOGO_16) });
    }
}
