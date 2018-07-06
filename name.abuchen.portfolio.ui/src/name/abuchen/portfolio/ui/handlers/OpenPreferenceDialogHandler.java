package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.preferences.APIKeysPreferencePage;
import name.abuchen.portfolio.ui.preferences.GeneralPreferencePage;
import name.abuchen.portfolio.ui.preferences.LanguagePreferencePage;
import name.abuchen.portfolio.ui.preferences.PresentationPreferencePage;
import name.abuchen.portfolio.ui.preferences.ProxyPreferencePage;
import name.abuchen.portfolio.ui.preferences.UpdatePreferencePage;

public class OpenPreferenceDialogHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        PreferenceManager pm = new PreferenceManager();
        pm.addToRoot(new PreferenceNode(PortfolioPlugin.PLUGIN_ID + ".updates", new UpdatePreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode(PortfolioPlugin.PLUGIN_ID + ".language", new LanguagePreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode(PortfolioPlugin.PLUGIN_ID + ".proxy", new ProxyPreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode(PortfolioPlugin.PLUGIN_ID + ".presentation", new PresentationPreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode(PortfolioPlugin.PLUGIN_ID + ".general", new GeneralPreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode(PortfolioPlugin.PLUGIN_ID + ".apikeys", new APIKeysPreferencePage())); //$NON-NLS-1$

        PreferenceDialog dialog = new PreferenceDialog(shell, pm);
        dialog.setPreferenceStore(PortfolioPlugin.getDefault().getPreferenceStore());
        dialog.create();
        dialog.getTreeViewer().setComparator(new ViewerComparator());
        dialog.getTreeViewer().expandAll();
        dialog.open();
    }
}
