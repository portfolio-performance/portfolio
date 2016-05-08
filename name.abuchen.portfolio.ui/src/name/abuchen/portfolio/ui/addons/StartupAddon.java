package name.abuchen.portfolio.ui.addons;

import java.io.IOException;
import java.text.MessageFormat;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.log.LogEntryCache;
import name.abuchen.portfolio.ui.update.UpdateHelper;
import name.abuchen.portfolio.ui.util.ProgressMonitorFactory;
import name.abuchen.portfolio.ui.util.RecentFilesCache;

@SuppressWarnings("restriction")
public class StartupAddon
{
    private static final class UpdateExchangeRatesJob extends Job
    {
        private final ExchangeRateProvider provider;

        private UpdateExchangeRatesJob(ExchangeRateProvider provider)
        {
            super(MessageFormat.format(Messages.MsgUpdatingExchangeRates, provider.getName()));
            this.provider = provider;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            try
            {
                provider.load(monitor);
            }
            catch (Exception e)
            {
                // also catch runtime exceptions to make sure the update
                // method runs in any case
                PortfolioPlugin.log(e);
            }

            try
            {
                provider.update(monitor);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }

            return Status.OK_STATUS;
        }
    }

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
    public void setupRecentFilesCache(RecentFilesCache cache)
    {
        // force creation of recent files cache
    }

    @Inject
    @Optional
    public void checkForUpdates(@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) Event event, // NOSONAR
                    final IWorkbench workbench, final EPartService partService,
                    @Preference(value = UIConstants.Preferences.AUTO_UPDATE) boolean autoUpdate)
    {
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
                        UpdateHelper updateHelper = new UpdateHelper(workbench, partService);
                        updateHelper.runUpdate(monitor, true);
                    }
                    catch (CoreException e) // NOSONAR
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
    public void updateExchangeRates(ExchangeRateProviderFactory factory)
    {
        for (final ExchangeRateProvider provider : factory.getProviders())
        {
            Job job = new UpdateExchangeRatesJob(provider);
            job.schedule();
        }
    }

    @PreDestroy
    public void storeExchangeRates(ExchangeRateProviderFactory factory)
    {
        for (ExchangeRateProvider provider : factory.getProviders())
        {
            try
            {
                provider.save(new NullProgressMonitor());
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }
        }
    }

    @PostConstruct
    public void setMultipleWindowImages()
    {
        // setting window images
        // http://www.eclipse.org/forums/index.php/t/440442/

        Window.setDefaultImages(new Image[] { Images.LOGO_512.image(), Images.LOGO_256.image(), Images.LOGO_128.image(),
                        Images.LOGO_48.image(), Images.LOGO_32.image(), Images.LOGO_16.image() });
    }

    @PostConstruct
    public void replaceDefaultDialogImages()
    {
        ImageRegistry registry = JFaceResources.getImageRegistry();
        registry.put(Dialog.DLG_IMG_MESSAGE_ERROR, Images.ERROR.descriptor());
        registry.put(Dialog.DLG_IMG_MESSAGE_WARNING, Images.WARNING.descriptor());
        registry.put(Dialog.DLG_IMG_MESSAGE_INFO, Images.INFO.descriptor());
    }
}
