package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.preferences.AlphaVantagePreferencePage;
import name.abuchen.portfolio.ui.preferences.BackupsPreferencePage;
import name.abuchen.portfolio.ui.preferences.CalendarPreferencePage;
import name.abuchen.portfolio.ui.preferences.CoingeckoPreferencePage;
import name.abuchen.portfolio.ui.preferences.DivvyDiaryPreferencePage;
import name.abuchen.portfolio.ui.preferences.EODHistoricalDataPreferencePage;
import name.abuchen.portfolio.ui.preferences.FinnhubPreferencePage;
import name.abuchen.portfolio.ui.preferences.FormattingPreferencePage;
import name.abuchen.portfolio.ui.preferences.GeneralPreferencePage;
import name.abuchen.portfolio.ui.preferences.LanguagePreferencePage;
import name.abuchen.portfolio.ui.preferences.LeewayPreferencePage;
import name.abuchen.portfolio.ui.preferences.MyDividends24PreferencePage;
import name.abuchen.portfolio.ui.preferences.PortfolioReportPreferencePage;
import name.abuchen.portfolio.ui.preferences.PresentationPreferencePage;
import name.abuchen.portfolio.ui.preferences.PresetsPreferencePage;
import name.abuchen.portfolio.ui.preferences.ProxyPreferencePage;
import name.abuchen.portfolio.ui.preferences.QuandlPreferencePage;
import name.abuchen.portfolio.ui.preferences.ThemePreferencePage;
import name.abuchen.portfolio.ui.preferences.TwelveDataPreferencePage;
import name.abuchen.portfolio.ui.preferences.UpdatePreferencePage;
import name.abuchen.portfolio.ui.update.UpdateHelper;

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
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Preference(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES) boolean enableExperimentalFeatures,
                    @Optional @Named(UIConstants.Parameter.PAGE) String page, IThemeEngine themeEngine)
    {
        // the active client
        var client = MenuHelper.getActiveClient(part, false);

        PreferenceManager pm = new PreferenceManager('/');
        pm.addToRoot(new PreferenceNode("general", new GeneralPreferencePage())); //$NON-NLS-1$
        pm.addTo("general", new PreferenceNode("presets", new PresetsPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("general", new PreferenceNode("backups", new BackupsPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$

        pm.addToRoot(new PreferenceNode("presentation", new PresentationPreferencePage())); // NOSONAR //$NON-NLS-1$
        pm.addTo("presentation", new PreferenceNode("language", new LanguagePreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("presentation", new PreferenceNode("theme", new ThemePreferencePage(themeEngine))); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("presentation", new PreferenceNode("formatting", new FormattingPreferencePage(client))); //$NON-NLS-1$ //$NON-NLS-2$

        pm.addToRoot(new PreferenceNode("calendar", new CalendarPreferencePage())); //$NON-NLS-1$

        pm.addToRoot(new PreferenceNode("api", new APIKeyPreferencePage())); //$NON-NLS-1$
        pm.addTo("api", new PreferenceNode("alphavantage", new AlphaVantagePreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("coingecko", new CoingeckoPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("divvydiary", new DivvyDiaryPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("eodhistoricaldata", new EODHistoricalDataPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("finnhub", new FinnhubPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("leeway", new LeewayPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("mydividends24", new MyDividends24PreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("twelvedata", new TwelveDataPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$

        if (enableExperimentalFeatures)
            pm.addTo("api", new PreferenceNode("portfolio-report", new PortfolioReportPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$
        pm.addTo("api", new PreferenceNode("quandl", new QuandlPreferencePage())); //$NON-NLS-1$ //$NON-NLS-2$

        pm.addToRoot(new PreferenceNode("proxy", new ProxyPreferencePage())); //$NON-NLS-1$
        if (UpdateHelper.isInAppUpdateEnabled())
            pm.addToRoot(new PreferenceNode("updates", new UpdatePreferencePage())); //$NON-NLS-1$

        PreferenceDialog dialog = new PreferenceDialog(shell, pm)
        {
            @Override
            protected void configureShell(Shell newShell)
            {
                super.configureShell(newShell);
                newShell.setText(Messages.LabelSettings);
            }

            @Override
            protected void createButtonsForButtonBar(Composite parent)
            {
                super.createButtonsForButtonBar(parent);

                getButton(IDialogConstants.OK_ID).setText(Messages.BtnLabelApplyAndClose);
            }
        };

        // if the dialog reopens with the previously selected node, some of the
        // nodes are not visible. Either the selected node it given by the
        // incoming parameters or it is deselected
        dialog.setSelectedNode(page);

        dialog.setPreferenceStore(PortfolioPlugin.getDefault().getPreferenceStore());
        dialog.create();
        dialog.getTreeViewer().expandAll();
        dialog.open();
    }
}
