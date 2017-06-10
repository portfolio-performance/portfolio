package name.abuchen.portfolio.ui.update;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import name.abuchen.portfolio.p2.P2Service;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

@SuppressWarnings("restriction")
public class UpdateHelper
{
    private static final String VERSION_HISTORY = "version.history"; //$NON-NLS-1$

    private final IWorkbench workbench;
    private final EPartService partService;
    private P2Service p2Service;

    // TODO convert to local variable
    private ProfileChangeOperation operation;

    public UpdateHelper(IWorkbench workbench, EPartService partService, P2Service p2Service) throws CoreException
    {
        this.workbench = workbench;
        this.partService = partService;
        this.p2Service = p2Service;
        
        IProvisioningAgent agent = getService(IProvisioningAgent.class, IProvisioningAgent.SERVICE_NAME);
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

    public void runUpdate(IProgressMonitor monitor, boolean silent) throws CoreException
    {
        SubMonitor sub = SubMonitor.convert(monitor, Messages.JobMsgCheckingForUpdates, 200);
        try
        {

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
                    runProfileChangeOperation(operation, sub.newChild(100));
                }
            }
            else
            {
                if (!silent)
                {
                    Display.getDefault()
                                    .asyncExec(() -> MessageDialog.openInformation(
                                                    Display.getDefault().getActiveShell(), Messages.LabelInfo,
                                                    Messages.MsgNoUpdatesAvailable));
                }
            }
        }
        catch (UpdateConflictException e)
        {
            IStatus status = e.getStatus();
            if (status.getSeverity() == IStatus.ERROR && status.isMultiStatus())
            {
                for (IStatus innerStatus : ((MultiStatus) status).getChildren())
                {
                    if (innerStatus instanceof PlannerStatus)
                    {
                        RequestStatus requestStatus = ((PlannerStatus) innerStatus).getRequestStatus();
                        Set<IInstallableUnit> conflictingInstalledPlugins = requestStatus.getConflictsWithAnyRoots();
                        UninstallOperation uninstallOperation = p2Service
                                        .newUninstallOperation(conflictingInstalledPlugins);
                        status = uninstallOperation.resolveModal(monitor);
                        if (status.isOK())
                        {
                            StringBuilder sb = new StringBuilder(Messages.MsgUninstallConflicts + ":");

                            sb.append("\n\n"); //$NON-NLS-1$

                            for (IInstallableUnit installableUnit : conflictingInstalledPlugins)
                            {
                                sb.append(installableUnit.getId()).append("\n"); //$NON-NLS-1$
                            }

                            final boolean[] doUninstall = new boolean[1];
                            Display.getDefault()
                                            .syncExec(() -> doUninstall[0] = MessageDialog.openConfirm(
                                                            Display.getDefault().getActiveShell(),
                                                            Messages.LabelUninstallConflicts, sb.toString()));

                            if (doUninstall[0])
                            {
                                runProfileChangeOperation(uninstallOperation, sub.newChild(100));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void promptForRestart(EPartService partService, IWorkbench workbench)
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

    private NewVersion checkForUpdates(IProgressMonitor monitor) throws UpdateConflictException, CoreException
    {
        IInstallableUnit product = p2Service.getProduct();
        Set<IInstallableUnit> latestProducts = p2Service.getLatestProductsFromUpdateSite(getUpdateSite(), monitor);

        Optional<IInstallableUnit> firstProductUpdate = latestProducts.stream().filter(
                        iu -> iu.getId().equals(product.getId()) && iu.getVersion().compareTo(product.getVersion()) > 0)
                        .findFirst();

        if (!firstProductUpdate.isPresent()) { return null; }

        IInstallableUnit productUpdate = firstProductUpdate.get();
        operation = p2Service.newUpdateOperation(Collections.singleton(getUpdateSite()));
        IStatus status = operation.resolveModal(monitor);

        if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) { return null; }

        if (status.getSeverity() == IStatus.CANCEL) { throw new OperationCanceledException(); }

        if (!status.isOK()) { throw new UpdateConflictException(status); }

        if (productUpdate == null)
        {
            return new NewVersion(Messages.LabelUnknownVersion);
        }
        else
        {
            NewVersion v = new NewVersion(productUpdate.getVersion().toString());
            v.setMinimumJavaVersionRequired(
                            productUpdate.getProperty("latest.changes.minimumJavaVersionRequired", null)); //$NON-NLS-1$

            // try for locale first
            String history = productUpdate.getProperty( //
                            VERSION_HISTORY + "_" + Locale.getDefault().getLanguage(), null); //$NON-NLS-1$
            if (history == null)
                history = productUpdate.getProperty(VERSION_HISTORY, null);
            if (history != null)
                v.setVersionHistory(history);

            return v;
        }
    }

    private void runProfileChangeOperation(ProfileChangeOperation operation, IProgressMonitor monitor)
                    throws CoreException
    {
        if (operation == null)
        {
            try
            {
                checkForUpdates(monitor);
            }
            catch (UpdateConflictException e)
            {
                throw new CoreException(e.getStatus());
            }
        }

        p2Service.executeProfileChangeOperation(operation, new JobChangeAdapter()
        {
            @Override
            public void done(IJobChangeEvent event)
            {
                IStatus status = event.getResult();
                if (status.getSeverity() == IStatus.CANCEL) { throw new OperationCanceledException(); }
                if (status.isOK())
                {
                    promptForRestart(partService, workbench);
                }
                else
                {
                    PortfolioPlugin.log(status);
                }
            }
        });

    }

    private URI getUpdateSite()
    {
        try
        {
            String updateSite = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getString(UIConstants.Preferences.UPDATE_SITE);
            return new URI(updateSite);

        }
        catch (final URISyntaxException e)
        {
            PortfolioPlugin.log(e);
            return null;
        }
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
