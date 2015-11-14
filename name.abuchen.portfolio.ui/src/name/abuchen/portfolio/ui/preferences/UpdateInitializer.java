package name.abuchen.portfolio.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.ui.PortfolioPlugin;

public class UpdateInitializer extends AbstractPreferenceInitializer
{

    public UpdateInitializer()
    {}

    @Override
    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = PortfolioPlugin.getDefault().getPreferenceStore();
        store.setDefault(PortfolioPlugin.Preferences.AUTO_UPDATE, true);
        store.setDefault(PortfolioPlugin.Preferences.UPDATE_SITE, "http://updates.abuchen.name/portfolio"); //$NON-NLS-1$
    }

}
