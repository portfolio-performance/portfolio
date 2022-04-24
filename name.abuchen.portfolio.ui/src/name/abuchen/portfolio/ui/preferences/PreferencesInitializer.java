package name.abuchen.portfolio.ui.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.PresetValues;
import name.abuchen.portfolio.ui.editor.ClientInput;

public class PreferencesInitializer extends AbstractPreferenceInitializer
{
    @Override
    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = PortfolioPlugin.getDefault().getPreferenceStore();
        store.setDefault(UIConstants.Preferences.AUTO_UPDATE, true);
        store.setDefault(UIConstants.Preferences.UPDATE_SITE,
                        Platform.ARCH_X86.equals(Platform.getOSArch())
                                        ? "https://updates.portfolio-performance.info/portfolio-x86" //$NON-NLS-1$
                                        : "https://updates.portfolio-performance.info/portfolio"); //$NON-NLS-1$
        store.setDefault(UIConstants.Preferences.FORMAT_SHARES_DIGITS, 1);
        store.setDefault(UIConstants.Preferences.FORMAT_CALCULATED_QUOTE_DIGITS, 2);
        store.setDefault(UIConstants.Preferences.USE_INDIRECT_QUOTATION, true);
        store.setDefault(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true);
        store.setDefault(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, true);
        store.setDefault(UIConstants.Preferences.AUTO_SAVE_FILE, 0);
        store.setDefault(UIConstants.Preferences.STORE_SETTINGS_NEXT_TO_FILE, false);
        store.setDefault(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES, false);
        store.setDefault(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS, false);
        store.setDefault(UIConstants.Preferences.ALPHAVANTAGE_CALL_FREQUENCY_LIMIT, 5);
        store.setDefault(UIConstants.Preferences.CALENDAR, "default"); //$NON-NLS-1$
        store.setDefault(UIConstants.Preferences.PORTFOLIO_REPORT_API_URL, "https://api.portfolio-report.net"); //$NON-NLS-1$
        store.setDefault(UIConstants.Preferences.PRESET_VALUE_TIME, PresetValues.TimePreset.MIDNIGHT.name());

        // Backup
        store.setDefault(UIConstants.Preferences.BACKUP_MODE,
                        ClientInput.BACKUP_MODE.getDefault().getPreferenceValue());
        store.setDefault(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE, ClientInput.DEFAULT_RELATIVE_BACKUP_FOLDER);
        store.setDefault(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE, ""); //$NON-NLS-1$
    }
}
