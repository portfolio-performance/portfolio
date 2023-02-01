package name.abuchen.portfolio.ui.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Version;

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
        store.setDefault(UIConstants.Preferences.ALWAYS_DISPLAY_CURRENCY_CODE, false);
        store.setDefault(UIConstants.Preferences.DISPLAY_PER_ANNUM, false);
        store.setDefault(UIConstants.Preferences.CREATE_BACKUP_BEFORE_SAVING, true);
        store.setDefault(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, true);
        store.setDefault(UIConstants.Preferences.AUTO_SAVE_FILE, 0);
        store.setDefault(UIConstants.Preferences.STORE_SETTINGS_NEXT_TO_FILE, false);
        store.setDefault(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES, false);
        store.setDefault(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS,
                        Platform.getOS().equals(Platform.OS_LINUX) || (Platform.getOS().equals(Platform.OS_MACOSX)
                                        && Platform.getOSArch().equals(Platform.ARCH_X86_64)
                                        && compareOSVersion("13.0") >= 0)); //$NON-NLS-1$
        store.setDefault(UIConstants.Preferences.ALPHAVANTAGE_CALL_FREQUENCY_LIMIT, 5);
        store.setDefault(UIConstants.Preferences.CALENDAR, "default"); //$NON-NLS-1$
        store.setDefault(UIConstants.Preferences.PORTFOLIO_REPORT_API_URL, "https://api.portfolio-report.net"); //$NON-NLS-1$
        store.setDefault(UIConstants.Preferences.PRESET_VALUE_TIME, PresetValues.TimePreset.MIDNIGHT.name());

        // Backup
        store.setDefault(UIConstants.Preferences.BACKUP_MODE, BackupMode.getDefault().name());
        store.setDefault(UIConstants.Preferences.BACKUP_FOLDER_RELATIVE, ClientInput.DEFAULT_RELATIVE_BACKUP_FOLDER);
        store.setDefault(UIConstants.Preferences.BACKUP_FOLDER_ABSOLUTE, ""); //$NON-NLS-1$
    }

    public static int compareOSVersion(String version)
    {
        String current = System.getProperty("os.version"); //$NON-NLS-1$
        return compareVersionNumbers(current, version);
    }

    public static int compareVersionNumbers(String newer, String older)
    {
        try
        {
            Version n = new Version(newer);
            Version o = new Version(older);

            return n.compareTo(o);
        }
        catch (IllegalArgumentException e)
        {
            return -1;
        }
    }
}
