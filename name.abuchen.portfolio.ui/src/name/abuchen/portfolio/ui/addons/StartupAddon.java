package name.abuchen.portfolio.ui.addons;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents;
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
import name.abuchen.portfolio.ui.util.swt.ActiveShell;

public class StartupAddon
{
    private static final class UpdateExchangeRatesJob extends Job
    {
        private final IEventBroker broker;
        private final ExchangeRateProvider provider;

        private boolean loadDone = false;

        private UpdateExchangeRatesJob(IEventBroker broker, ExchangeRateProvider provider)
        {
            super(MessageFormat.format(Messages.MsgUpdatingExchangeRates, provider.getName()));
            this.broker = broker;
            this.provider = provider;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            if (!loadDone)
            {
                // load data from file only the first time around

                loadFromFile(monitor);
                loadDone = true;
            }

            updateOnline(monitor);

            // schedule to run at 17 CET because the reference rates are
            // "usually updated at around 16:00 CET every working day"
            // or within in 6 hours in case that is sooner

            ZonedDateTime nowInTimeZone = ZonedDateTime.now();
            ZoneId cet = ZoneId.of("CET"); //$NON-NLS-1$
            ZonedDateTime nowInCET = nowInTimeZone.withZoneSameInstant(cet);
            ZonedDateTime nextCET17 = nowInCET.toLocalDate().atTime(17, 0).atZone(cet);
            if (nowInCET.isAfter(nextCET17))
                nextCET17 = nextCET17.plusDays(1);

            long millisUntilNextCET17 = ChronoUnit.MILLIS.between(nowInCET, nextCET17);
            var sixHours = 1000L * 60 * 60 * 6;

            schedule(millisUntilNextCET17 > 0 && millisUntilNextCET17 < sixHours ? millisUntilNextCET17 : sixHours);

            return Status.OK_STATUS;
        }

        private void loadFromFile(IProgressMonitor monitor)
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
            finally
            {
                broker.post(UIConstants.Event.ExchangeRates.LOADED, provider);
            }
        }

        private void updateOnline(IProgressMonitor monitor)
        {
            try
            {
                provider.update(monitor);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
            }
            finally
            {
                broker.post(UIConstants.Event.ExchangeRates.LOADED, provider);
            }
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
                    @Preference(value = UIConstants.Preferences.AUTO_UPDATE) boolean autoUpdate)
    {
        if (autoUpdate && UpdateHelper.isInAppUpdateEnabled())
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
                    catch (CoreException e) // NOSONAR
                    {
                        PortfolioPlugin.log(e.getStatus());
                    }
                    return Status.OK_STATUS;
                }

            };
            job.setSystem(true);
            job.schedule(3000);
        }
    }

    @PostConstruct
    public void updateExchangeRates(IEventBroker broker)
    {
        for (final ExchangeRateProvider provider : ExchangeRateProviderFactory.getProviders())
        {
            Job job = new UpdateExchangeRatesJob(broker, provider);
            job.schedule();
        }
    }

    @PreDestroy
    public void storeExchangeRates()
    {
        for (ExchangeRateProvider provider : ExchangeRateProviderFactory.getProviders())
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
        // do not update on macOS b/c ICNS file contains all images
        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            return;

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

    @PostConstruct
    public void setupActiveShellTracker()
    {
        ActiveShell.get();
    }
}
