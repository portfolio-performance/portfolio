package name.abuchen.portfolio.ui.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.ui.UIConstants;

public class MyDividends24APIKeyConfiguredExpression
{
    @Evaluate
    public boolean evaluate(@Preference IEclipsePreferences preferences)
    {
        final String key = preferences.get(UIConstants.Preferences.MYDIVIDENDS24_API_KEY, null);
        return key != null && !key.isBlank();
    }
}
