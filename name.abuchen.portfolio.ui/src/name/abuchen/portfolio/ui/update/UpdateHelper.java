package name.abuchen.portfolio.ui.update;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public final class UpdateHelper
{
    private interface Task
    {
        public void run(IProgressMonitor monitor) throws CoreException;
    }

    private static final String VERSION_HISTORY = "version.history"; //$NON-NLS-1$
    private static final String HEADER = "header"; //$NON-NLS-1$

    private final IWorkbench workbench;
    private final EPartService partService;
    private IProvisioningAgent agent;
    private UpdateOperation operation;

    public UpdateHelper(IWorkbench workbench, EPartService partService)
    {
        this.workbench = workbench;
        this.partService = partService;
    }

    public void runUpdateWithUIMonitor()
    {
        runWithUIMonitor(monitor -> runUpdate(monitor, false));
    }

    public void runUpdate(IProgressMonitor monitor, boolean silent) throws CoreException
    {
        SubMonitor sub = SubMonitor.convert(monitor, Messages.JobMsgCheckingForUpdates, 200);

        checkForLetsEncryptRootCertificate(silent);

        configureProvisioningAgent();

        final NewVersion newVersion = checkForUpdates(sub.newChild(100));
        if (newVersion != null)
        {
            final boolean[] doUpdate = new boolean[1];
            Display.getDefault().syncExec(() -> {
                Dialog dialog = new UpdateMessageDialog(Display.getDefault().getActiveShell(),
                                Messages.LabelUpdatesAvailable, //
                                MessageFormat.format(Messages.MsgConfirmInstall, newVersion.getVersion()), //
                                newVersion);

                doUpdate[0] = dialog.open() == 0;
            });

            if (doUpdate[0])
            {
                if (silent)
                {
                    // if the update check was started silently in the
                    // background, but the user has chosen to update, show a
                    // meaningful progress monitor

                    runWithUIMonitor(m -> {
                        runUpdateOperation(m);
                        promptForRestart();
                    });
                }
                else
                {
                    runUpdateOperation(sub.newChild(100));
                    promptForRestart();
                }
            }
        }
        else
        {
            if (!silent)
            {
                Display.getDefault()
                                .asyncExec(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                                                Messages.LabelInfo, Messages.MsgNoUpdatesAvailable));
            }
        }
    }

    private void checkForLetsEncryptRootCertificate(boolean silent) throws CoreException
    {
        // if the java version is too old, the Let's Encrypt certificate is not
        // trusted. Unfortunately, the exception is only printed to the log and
        // propagated up. This checks upfront. As PP does not run on 1.7, we do
        // not check for the 1.7 version with the certificate.

        try
        {
            String javaVersion = System.getProperty("java.version"); //$NON-NLS-1$

            int p = javaVersion.indexOf('-');
            if (p >= 0)
                javaVersion = javaVersion.substring(0, p);

            String[] digits = javaVersion.split("[\\._]"); //$NON-NLS-1$
            if (digits.length < 4)
                return;

            int majorVersion = Integer.parseInt(digits[0]);
            if (majorVersion > 1)
                return;

            int minorVersion = Integer.parseInt(digits[1]);
            if (minorVersion > 8)
                return;

            int patchVersion = Integer.parseInt(digits[2]);
            if (patchVersion > 0)
                return;

            int updateNumber = Integer.parseInt(digits[3]);
            if (updateNumber >= 101)
                return;

            CoreException exception = new CoreException(new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID,
                            MessageFormat.format(Messages.MsgJavaVersionTooOldForLetsEncrypt, javaVersion)));

            if (silent)
                PortfolioPlugin.log(exception);
            else
                throw exception;

        }
        catch (NumberFormatException e)
        {
            PortfolioPlugin.log(e);
        }

    }

    private void promptForRestart()
    {
        // start a new job before prompting to restart the application to allow
        // the UI progress monitor to complete. Otherwise the open dialog will
        // prevent the automatic restart.

        new Job(Messages.JobMsgCheckingForUpdates)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                Display.getDefault().asyncExec(() -> {
                    MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.LabelInfo,
                                    null, Messages.MsgRestartRequired, MessageDialog.INFORMATION, //
                                    new String[] { Messages.BtnLabelRestartNow, Messages.BtnLabelRestartLater }, 0);

                    int returnCode = dialog.open();

                    if (returnCode == 0)
                    {
                        try
                        {
                            boolean successful = partService.saveAll(true);

                            if (successful)
                                workbench.restart();
                        }
                        catch (IllegalStateException e)
                        {
                            PortfolioPlugin.log(e);
                            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                            Messages.MsgCannotRestartBecauseOfOpenDialog);
                        }
                    }
                });

                return Status.OK_STATUS;
            }

        }.schedule(500);
    }

    private NewVersion checkForUpdates(IProgressMonitor monitor) throws CoreException
    {
        ProvisioningSession session = new ProvisioningSession(agent);
        operation = new UpdateOperation(session);

        configureRepositories(monitor);

        IStatus status = operation.resolveModal(monitor);

        if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
            return null;

        if (status.getSeverity() == IStatus.CANCEL)
            throw new OperationCanceledException();

        if (status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);

        Update[] possibleUpdates = operation.getPossibleUpdates();
        Update update = possibleUpdates.length > 0 ? possibleUpdates[0] : null;

        if (update == null)
        {
            return new NewVersion(Messages.LabelUnknownVersion);
        }
        else
        {
            NewVersion v = new NewVersion(update.replacement.getVersion().toString());
            v.setMinimumJavaVersionRequired(
                            update.replacement.getProperty("latest.changes.minimumJavaVersionRequired", null)); //$NON-NLS-1$

            // try for locale first
            String history = update.replacement.getProperty( //
                            VERSION_HISTORY + "_" + Locale.getDefault().getLanguage(), null); //$NON-NLS-1$
            if (history == null)
                history = update.replacement.getProperty(VERSION_HISTORY, null);
            if (history != null)
                v.setVersionHistory(history);

            // header
            String header = update.replacement.getProperty(HEADER + "_" + Locale.getDefault().getLanguage(), null); //$NON-NLS-1$
            if (header == null)
                header = update.replacement.getProperty(HEADER, null);
            if (header != null)
                v.setHeader(header);

            return v;
        }
    }

    private void configureProvisioningAgent() throws CoreException
    {
        this.agent = getService(IProvisioningAgent.class, IProvisioningAgent.SERVICE_NAME);
        if (agent == null)
        {
            IStatus status = new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, Messages.MsgNoProfileFound);
            throw new CoreException(status);
        }

        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);

        IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
        if (profile == null)
        {
            IStatus status = new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, Messages.MsgNoProfileFound);
            throw new CoreException(status);
        }
    }

    private void configureRepositories(IProgressMonitor monitor)
    {
        try
        {
            String updateSite = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getString(UIConstants.Preferences.UPDATE_SITE);
            URI uri = new URI(updateSite);

            IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
            IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

            // remove all repos, this is important if the update site in preferences has been changed
            final URI[] metaReposToClean = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
            Arrays.stream(metaReposToClean).forEach(manager::removeRepository);
            final URI[] artifactReposToClean = artifactManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
            Arrays.stream(artifactReposToClean).forEach(artifactManager::removeRepository);

            manager.addRepository(uri);
            artifactManager.addRepository(uri);

            // Working around bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=520461
            // by forcing a refresh of the repositories.
            // p2 never tries to reconnect if a connection timeout happened like described in 
            // https://github.com/buchen/portfolio/issues/578#issuecomment-251653225
            manager.refreshRepository(uri, monitor);
            artifactManager.refreshRepository(uri, monitor);
        }
        catch (final URISyntaxException | ProvisionException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private void runUpdateOperation(IProgressMonitor monitor) throws CoreException
    {
        if (operation == null)
            checkForUpdates(monitor);

        ProvisioningJob job = operation.getProvisioningJob(null);
        IStatus status = job.runModal(monitor);

        if (status.getSeverity() == IStatus.CANCEL)
            throw new OperationCanceledException();
        if (status.getSeverity() != IStatus.OK)
            throw new CoreException(status);
    }

    private <T> T getService(Class<T> type, String name)
    {
        BundleContext context = PortfolioPlugin.getDefault().getBundle().getBundleContext();
        ServiceReference<?> reference = context.getServiceReference(name);
        if (reference == null)
            return null;
        Object result = context.getService(reference);
        context.ungetService(reference);
        return type.cast(result);
    }

    private void runWithUIMonitor(Task task)
    {
        Display.getDefault().syncExec(() -> {
            try
            {
                new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, m -> {
                    try
                    {
                        task.run(m);
                    }
                    catch (CoreException e)
                    {
                        PortfolioPlugin.log(e);
                        Display.getDefault()
                                        .asyncExec(() -> ErrorDialog.openError(Display.getDefault().getActiveShell(),
                                                        Messages.LabelError, Messages.MsgErrorUpdating, e.getStatus()));
                    }
                });
            }
            catch (InvocationTargetException e)
            {
                PortfolioPlugin.log(e);
            }
            catch (InterruptedException e)
            {
                PortfolioPlugin.log(e);
                Thread.currentThread().interrupt();
            }
        });
    }
}
