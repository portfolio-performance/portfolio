package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.p2.P2Service;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ManagePluginsDialog;

@SuppressWarnings("restriction")
public class ManagePluginsHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Preference(nodePath = "name.abuchen.portfolio."
                                    + UIConstants.Preferences.PLUGIN_UPDATE_SITES) IEclipsePreferences preferences,
                    final IWorkbench workbench, final EPartService partService, final P2Service p2Service)
    {
        new ManagePluginsDialog(shell, preferences, workbench, partService, p2Service).open();
    }
}
