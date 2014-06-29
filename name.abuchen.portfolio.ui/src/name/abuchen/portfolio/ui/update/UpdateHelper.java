package name.abuchen.portfolio.ui.update;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class UpdateHelper
{
    private class NewVersion
    {
        private String version;
        private String description;
        private String minimumJavaVersionRequired;
        private String macosxBundleRequired;

        public NewVersion(String version, String description, String minimumJavaVersionRequired, String osMessage)
        {
            this.version = version;
            this.description = description;
            this.minimumJavaVersionRequired = minimumJavaVersionRequired;
            this.macosxBundleRequired = osMessage;
        }

        public String getVersion()
        {
            return version;
        }

        public String getDescription()
        {
            return description;
        }

        public boolean requiresMacOSXBundle()
        {
            if (!"true".equals(macosxBundleRequired)) //$NON-NLS-1$
                return false;

            return Platform.OS_MACOSX.equals(Platform.getOS());
        }

        public boolean requiresNewJavaVersion()
        {
            if (minimumJavaVersionRequired == null)
                return false;

            double current = parseJavaVersion(System.getProperty("java.version")); //$NON-NLS-1$
            double required = parseJavaVersion(minimumJavaVersionRequired);

            return required > current;
        }

        private double parseJavaVersion(String version)
        {
            int pos = 0;
            for (int count = 0; pos < version.length() && count < 2; pos++)
                if (version.charAt(pos) == '.')
                    count++;

            if (pos < version.length()) // exclude second dot from parsing
                pos--;

            return Double.parseDouble(version.substring(0, pos));
        }
    }

    private final IProvisioningAgent agent;
    private UpdateOperation operation;

    public UpdateHelper() throws CoreException
    {
        agent = (IProvisioningAgent) getService(IProvisioningAgent.class, IProvisioningAgent.SERVICE_NAME);

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
            Display.getDefault().syncExec(new Runnable()
            {
                public void run()
                {
                    Dialog dialog = new ExtendedMessageDialog(Display.getDefault().getActiveShell(),
                                    Messages.LabelUpdatesAvailable, //
                                    MessageFormat.format(Messages.MsgConfirmInstall, newVersion.getVersion()), //
                                    newVersion);

                    doUpdate[0] = dialog.open() == 0;
                }
            });

            if (doUpdate[0])
            {
                runUpdateOperation(sub.newChild(100));
                Display.getDefault().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.LabelInfo,
                                        Messages.MsgRestartRequired);
                    }
                });
            }
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

    private NewVersion checkForUpdates(IProgressMonitor monitor) throws OperationCanceledException, CoreException
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
            throw new CoreException(status);

        Update[] possibleUpdates = operation.getPossibleUpdates();
        Update update = possibleUpdates.length > 0 ? possibleUpdates[0] : null;

        if (update == null)
        {
            return new NewVersion(Messages.LabelUnknownVersion, null, null, null);
        }
        else
        {
            return new NewVersion(update.replacement.getVersion().toString(), //
                            update.replacement.getProperty("latest.changes.description", null), //$NON-NLS-1$
                            update.replacement.getProperty("latest.changes.minimumJavaVersionRequired", null), //$NON-NLS-1$
                            update.replacement.getProperty("latest.changes.macosxBundleRequired", null)); //$NON-NLS-1$
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

    private void loadRepository(IProvisioningAgent agent) throws CoreException
    {
        IMetadataRepositoryManager repositoryManager = (IMetadataRepositoryManager) agent
                        .getService(IMetadataRepositoryManager.SERVICE_NAME);

        IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent
                        .getService(IArtifactRepositoryManager.SERVICE_NAME);

        // first: remove existing repositories (preferences might have changed)
        for (URI r : repositoryManager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL))
            repositoryManager.removeRepository(r);

        for (URI r : artifactManager.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL))
            artifactManager.removeRepository(r);

        // second: add repository as configured in preferences
        try
        {
            String updateSite = PortfolioPlugin.getDefault().getPreferenceStore()
                            .getString(PortfolioPlugin.Preferences.UPDATE_SITE);
            URI repoLocation = new URI(updateSite);
            repositoryManager.loadRepository(repoLocation, null);
            artifactManager.loadRepository(repoLocation, null);
        }
        catch (URISyntaxException e)
        {
            IStatus status = new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e);
            throw new CoreException(status);
        }
    }

    private static class ExtendedMessageDialog extends MessageDialog
    {
        private Button checkOnUpdate;
        private NewVersion newVersion;

        public ExtendedMessageDialog(Shell parentShell, String title, String message, NewVersion newVersion)
        {
            super(parentShell, title, null, message, CONFIRM, new String[] { IDialogConstants.OK_LABEL,
                            IDialogConstants.CANCEL_LABEL }, 0);
            this.newVersion = newVersion;
        }

        @Override
        protected Control createCustomArea(Composite parent)
        {
            Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

            createText(container);

            checkOnUpdate = new Button(container, SWT.CHECK);
            checkOnUpdate.setSelection(PortfolioPlugin.getDefault().getPreferenceStore()
                            .getBoolean(PortfolioPlugin.Preferences.AUTO_UPDATE));
            checkOnUpdate.setText(Messages.PrefCheckOnStartup);
            checkOnUpdate.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    PortfolioPlugin.getDefault().getPreferenceStore()
                                    .setValue(PortfolioPlugin.Preferences.AUTO_UPDATE, checkOnUpdate.getSelection());
                }
            });
            GridDataFactory.fillDefaults().grab(true, false);

            return container;
        }

        private void createText(Composite container)
        {
            StyledText text = new StyledText(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);

            List<StyleRange> ranges = new ArrayList<StyleRange>();

            StringBuilder buffer = new StringBuilder();
            if (newVersion.requiresMacOSXBundle())
            {
                // we know that we are *not* running as a Max OS X bundle
                // because this code still exists

                StyleRange style = new StyleRange();
                style.start = 0;
                style.length = Messages.MsgUpdateRequiresMacOSXBundle.length();
                style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
                style.fontStyle = SWT.BOLD;
                ranges.add(style);

                buffer.append(Messages.MsgUpdateRequiresMacOSXBundle);
                buffer.append("\n\n"); //$NON-NLS-1$
            }

            if (newVersion.requiresNewJavaVersion())
            {
                StyleRange style = new StyleRange();
                style.start = buffer.length();
                style.length = Messages.MsgUpdateRequiresLatestJavaVersion.length();
                style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
                style.fontStyle = SWT.BOLD;
                ranges.add(style);

                buffer.append(Messages.MsgUpdateRequiresLatestJavaVersion);
                buffer.append("\n\n"); //$NON-NLS-1$
            }

            buffer.append(newVersion.getDescription());
            text.setText(buffer.toString());
            text.setStyleRanges(ranges.toArray(new StyleRange[0]));
            GridDataFactory.fillDefaults().grab(true, true).applyTo(text);
        }
    }
}
