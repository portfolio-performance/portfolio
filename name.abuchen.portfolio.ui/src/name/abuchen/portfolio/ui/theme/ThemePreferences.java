package name.abuchen.portfolio.ui.theme;

import java.util.Optional;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngine;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Helper class to read the configured themeId. There is no public API to read
 * this. And, more importantly, there is no way to remove the stored themeId to
 * go back to the automatic behavior.
 */
@SuppressWarnings("restriction")
public class ThemePreferences
{
    private static final String THEMEID_KEY = "themeid"; //$NON-NLS-1$

    private ThemePreferences()
    {
    }

    public static Optional<String> getConfiguredThemeId()
    {
        var preferences = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(ThemeEngine.class).getSymbolicName());
        return Optional.ofNullable(preferences != null ? preferences.get(THEMEID_KEY, null) : null);
    }

    public static void clearConfiguredThemeId()
    {
        var preferences = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(ThemeEngine.class).getSymbolicName());
        if (preferences == null)
            return;

        preferences.remove(THEMEID_KEY);
        try
        {
            preferences.flush();
        }
        catch (BackingStoreException e)
        {
            PortfolioPlugin.log(e);
        }
    }

}
