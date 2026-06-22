package name.abuchen.portfolio.ui.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.ui.UIConstants;

public class IBFlexHasMultipleCredentialsExpression
{
    @Evaluate
    public boolean evaluate(@Preference IEclipsePreferences preferences)
    {
        return IBFlexConfiguration
                        .deserialize(preferences.get(UIConstants.Preferences.IBFLEX_CREDENTIALS, null)).size() > 1;
    }
}
