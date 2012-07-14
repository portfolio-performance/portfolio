package name.abuchen.portfolio.ui.update;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class UpdateHelper
{
    private final IProvisioningAgent agent;
    private UpdateOperation operation;

    public UpdateHelper() throws IOException
    {
        agent = (IProvisioningAgent) getService(IProvisioningAgent.class, IProvisioningAgent.SERVICE_NAME);

        IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);

        IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
        if (profile == null)
            throw new IOException(Messages.MsgNoProfileFound);
    }

    public void runUpdate(IProgressMonitor monitor, boolean silent) throws OperationCanceledException, IOException
    {
        SubMonitor sub = SubMonitor.convert(monitor, Messages.JobMsgCheckingForUpdates, 200);

        final String newVersion = checkForUpdates(sub.newChild(100));
        if (newVersion != null)
        {
            final boolean[] doUpdate = new boolean[1];
            Display.getDefault().syncExec(new Runnable()
            {
                public void run()
                {
                    doUpdate[0] = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                                    Messages.LabelUpdatesAvailable,
                                    MessageFormat.format(Messages.MsgConfirmInstall, newVersion));
                }
            });

            if (doUpdate[0])
                runUpdateOperation(sub.newChild(100));
        }
        else
        {
            if (!silent)
            {
                Display.getDefault().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.LabelInfo,
                                        Messages.MsgNoUpdatesAvailable);
                    }
                });
            }
        }
    }

    private String checkForUpdates(IProgressMonitor monitor) throws OperationCanceledException, IOException
    {
        loadRepository(agent);

        ProvisioningSession session = new ProvisioningSession(agent);
        operation = new UpdateOperation(session);

        IStatus status = operation.resolveModal(monitor);

        if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
            return null;

        if (status.getSeverity() == IStatus.CANCEL)
            throw new OperationCanceledException();

        if (status.getSeverity() == IStatus.ERROR)
            throw new IOException(status.getException());

        Update[] possibleUpdates = operation.getPossibleUpdates();
        return possibleUpdates.length > 0 ? possibleUpdates[0].replacement.getVersion().toString()
                        : Messages.LabelUnknownVersion;
    }

    private void runUpdateOperation(IProgressMonitor monitor) throws OperationCanceledException, IOException
    {
        if (operation == null)
            checkForUpdates(monitor);

        ProvisioningJob job = operation.getProvisioningJob(null);
        IStatus status = job.runModal(monitor);
        if (status.getSeverity() == IStatus.CANCEL)
            throw new OperationCanceledException();
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

    private void loadRepository(IProvisioningAgent agent) throws IOException
    {
        IMetadataRepositoryManager repositoryManager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);

        IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent
                        .getService(IArtifactRepositoryManager.SERVICE_NAME);

        try
        {
            String updateSite = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getString(PortfolioPlugin.Preferences.UPDATE_SITE);
            URI repoLocation = new URI(updateSite);
            repositoryManager.loadRepository(repoLocation, null);
            artifactManager.loadRepository(repoLocation, null);
        }
        catch (ProvisionException e)
        {
            throw new IOException(e);
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }
}
