package name.abuchen.portfolio.ui.update;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
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
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

@SuppressWarnings("restriction")
public final class UpdateHelper
{
    private interface Task
    {
        public void run(IProgressMonitor monitor) throws CoreException;
    }

    private static final String VERSION_HISTORY = "version.history"; //$NON-NLS-1$
    private static final String HEADER = "header"; //$NON-NLS-1$
    private static final String PREVENT_UPDATE_CONDITION_PREFIX = "latest.changes.preventUpdate."; //$NON-NLS-1$

    private static final boolean IS_IN_APP_UPDATE_ENABLED = !"disable" //$NON-NLS-1$
                    .equals(System.getProperty("name.abuchen.portfolio.in-app-update")); //$NON-NLS-1$

    private IProvisioningAgent agent;
    private UpdateOperation operation;

    /**
     * Returns true if the installation support in-app updates. In-app updates
     * can be disabled if a package manager such as flatpak is controlling the
     * lifecycle.
     */
    public static boolean isInAppUpdateEnabled()
    {
        return IS_IN_APP_UPDATE_ENABLED;
    }

    public void runUpdateWithUIMonitor()
    {
        runWithUIMonitor(monitor -> runUpdate(monitor, false));
    }

    public void runUpdate(IProgressMonitor monitor, boolean silent) throws CoreException
    {
        if (!isInAppUpdateEnabled())
            return;

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

            CoreException exception = new CoreException(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID,
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
        // automatic restarting seems to have to many problems with users out
        // there in the wild. Instead, we just open a dialog to inform the user
        // that a restart is required.

        Display.getDefault().asyncExec(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                        Messages.LabelInfo, Messages.MsgRestartRequired));
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

            int index = 0;
            while (true)
            {
                String condition = update.replacement.getProperty(PREVENT_UPDATE_CONDITION_PREFIX + index);
                if (condition == null)
                    break;
                v.addPreventUpdateCondition(condition);

                index++;
            }

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

            IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
                            .getService(IMetadataRepositoryManager.SERVICE_NAME);
            IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent
                            .getService(IArtifactRepositoryManager.SERVICE_NAME);

            // remove all repos, this is important if the update site in
            // preferences has been changed
            final URI[] metaReposToClean = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
            Arrays.stream(metaReposToClean).forEach(manager::removeRepository);
            final URI[] artifactReposToClean = artifactManager
                            .getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
            Arrays.stream(artifactReposToClean).forEach(artifactManager::removeRepository);

            manager.addRepository(uri);
            artifactManager.addRepository(uri);

            // Working around bug
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=520461
            // by forcing a refresh of the repositories.
            // p2 never tries to reconnect if a connection timeout happened like
            // described in
            // https://github.com/portfolio-performance/portfolio/issues/578#issuecomment-251653225
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

        checkWritePermissions();

        ProvisioningJob job = operation.getProvisioningJob(null);
        IStatus status = job.runModal(monitor);

        if (status.getSeverity() == IStatus.CANCEL)
            throw new OperationCanceledException();
        if (status.getSeverity() != IStatus.OK)
            throw new CoreException(status);
    }

    /**
     * Check that update process has correct permissions to update the
     * installation. Particularly on Windows, we might have to prompt users to
     * run PP as administrator.
     */
    private void checkWritePermissions() throws CoreException
    {
        Location installLocation = Platform.getInstallLocation();
        if (installLocation == null)
            return;

        URL url = installLocation.getURL();
        if (url == null)
            return;

        File installDir = URLUtil.toFile(url);

        boolean canWrite = canWrite(installDir);

        PortfolioPlugin.log(MessageFormat.format("Pre-update check. Write permissions on {0} = {1}", //$NON-NLS-1$
                        installDir.getAbsolutePath(), canWrite));

        if (!canWrite)
            throw new CoreException(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID,
                            MessageFormat.format(Messages.MsgUpdateNoWritePermissions, installDir.getAbsolutePath())));

    }

    private boolean canWrite(File installDir)
    {
        // https://stackoverflow.com/a/18633628/1158146

        if (!installDir.canWrite())
            return false;

        if (!installDir.isDirectory())
            return false;

        File fileTest = null;
        try
        {
            fileTest = Files.createTempFile(installDir.toPath(), "writeableArea", ".dll").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch (IOException e)
        {
            return false;
        }
        finally
        {
            if (fileTest != null)
                fileTest.delete(); // NOSONAR
        }
        return true;
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
