package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class ForceClearPersistedStateHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, //
                    IWorkbench workbench, //
                    EPartService partService,
                    @Preference(nodePath = "name.abuchen.portfolio.bootstrap") IEclipsePreferences preferences)
    {
        if (!MessageDialog.openQuestion(shell, Messages.ForceClearPersistedStateDialogTitle,
                        Messages.ForceClearPersistedStateMessage))
            return;

        try
        {
            // we must avoid a dependency to 'bootstrap' bundle -> copy string
            preferences.putBoolean("model.forceClearPersistedState", true); //$NON-NLS-1$
            preferences.flush();

            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.MsgRestartRequired);

        }
        catch (BackingStoreException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }
}
