package name.abuchen.portfolio.ui.update;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class UpdateHelper
{
    private static final String VERSION_HISTORY = "version.history"; //$NON-NLS-1$

    private final IWorkbench workbench;
    private final EPartService partService;
    private final IProvisioningAgent agent;
    private UpdateOperation operation;

    public UpdateHelper(IWorkbench workbench, EPartService partService) throws CoreException
    {
        this.workbench = workbench;
        this.partService = partService;
        this.agent = getService(IProvisioningAgent.class, IProvisioningAgent.SERVICE_NAME);

        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);

        IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
        if (profile == null)
        {
            IStatus status = new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, Messages.MsgNoProfileFound);
            throw new CoreException(status);
        }
    }

    public void runUpdate(IProgressMonitor monitor, boolean silent) throws OperationCanceledException, CoreException
    {
        SubMonitor sub = SubMonitor.convert(monitor, Messages.JobMsgCheckingForUpdates, 200);

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
                runUpdateOperation(sub.newChild(100));
                promptForRestart();
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

    private void promptForRestart()
    {
        Display.getDefault().asyncExec(() -> {
            MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.LabelInfo, null,
                            Messages.MsgRestartRequired, MessageDialog.INFORMATION, //
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
    }

    private NewVersion checkForUpdates(IProgressMonitor monitor) throws OperationCanceledException, CoreException
    {
        ProvisioningSession session = new ProvisioningSession(agent);
        operation = new UpdateOperation(session);
        configureUpdateOperation(operation);

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

            return v;
        }
    }

    private void configureUpdateOperation(UpdateOperation operation)
    {
        try
        {
            String updateSite = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getString(UIConstants.Preferences.UPDATE_SITE);
            URI uri = new URI(updateSite);

            operation.getProvisioningContext().setArtifactRepositories(new URI[] { uri });
            operation.getProvisioningContext().setMetadataRepositories(new URI[] { uri });

        }
        catch (final URISyntaxException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private void runUpdateOperation(IProgressMonitor monitor) throws OperationCanceledException, CoreException
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
}
