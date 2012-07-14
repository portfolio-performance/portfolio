package name.abuchen.portfolio.ui.preferences;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class UpdateInitializer extends AbstractPreferenceInitializer
{

    public UpdateInitializer()
    {}

    @Override
    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = PortfolioPlugin.getDefault().getPreferenceStore();
        store.setDefault(PortfolioPlugin.Preferences.UPDATE_SITE, "http://updates.abuchen.name/portfolio"); //$NON-NLS-1$
    }

}
