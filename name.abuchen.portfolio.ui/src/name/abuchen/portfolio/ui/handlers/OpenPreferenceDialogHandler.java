package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.preferences.AlphaVantagePreferencePage;
import name.abuchen.portfolio.ui.preferences.CalendarPreferencePage;
import name.abuchen.portfolio.ui.preferences.DivvyDiaryPreferencePage;
import name.abuchen.portfolio.ui.preferences.FinnhubPreferencePage;
import name.abuchen.portfolio.ui.preferences.GeneralPreferencePage;
import name.abuchen.portfolio.ui.preferences.LanguagePreferencePage;
import name.abuchen.portfolio.ui.preferences.PresentationPreferencePage;
import name.abuchen.portfolio.ui.preferences.ProxyPreferencePage;
import name.abuchen.portfolio.ui.preferences.QuandlPreferencePage;
import name.abuchen.portfolio.ui.preferences.ThemePreferencePage;
import name.abuchen.portfolio.ui.preferences.UpdatePreferencePage;

@SuppressWarnings("restriction")
public class OpenPreferenceDialogHandler
{
    public static class APIKeyPreferencePage extends PreferencePage
    {
        public APIKeyPreferencePage()
        {
            setTitle(Messages.PrefTitleAPIKeys);
        }

        @Override
        protected Control createContents(Composite parent)
        {
            return new Composite(parent, SWT.None);
        }
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, IThemeEngine themeEngine)
    {
        PreferenceManager pm = new PreferenceManager('/');
        pm.addToRoot(new PreferenceNode("general", new GeneralPreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode("presentation", new PresentationPreferencePage())); //$NON-NLS-1$
        pm.addTo("presentation", new PreferenceNode("language", new LanguagePreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("presentation", new PreferenceNode("theme", new ThemePreferencePage(themeEngine))); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addToRoot(new PreferenceNode("calendar", new CalendarPreferencePage())); //$NON-NLS-1$

        pm.addToRoot(new PreferenceNode("api", new APIKeyPreferencePage())); //$NON-NLS-1$
        pm.addTo("api", new PreferenceNode("alphavantage", new AlphaVantagePreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("quandl", new QuandlPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("finnhub", new FinnhubPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("divvydiary", new DivvyDiaryPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$

        pm.addToRoot(new PreferenceNode("proxy", new ProxyPreferencePage())); //$NON-NLS-1$
        pm.addToRoot(new PreferenceNode("updates", new UpdatePreferencePage())); //$NON-NLS-1$

        PreferenceDialog dialog = new PreferenceDialog(shell, pm)
        {
            @Override
            protected void configureShell(Shell newShell)
            {
                super.configureShell(newShell);
                newShell.setText(Messages.LabelSettings);
            }
        };

        // if the dialog reopens with the previously selected node, some of the
        // nodes are not visible. Workaround: make sure not previous node exists
        dialog.setSelectedNode(null);

        dialog.setPreferenceStore(PortfolioPlugin.getDefault().getPreferenceStore());
        dialog.create();
        dialog.getTreeViewer().expandAll();
        dialog.open();
    }
}
