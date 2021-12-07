package name.abuchen.portfolio.ui.preferences;

import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.ui.UIConstants;

public class ExperimentalFeaturesEnabledExpression
{

    @Evaluate
    public boolean evaluate(
                    @Preference(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES) boolean enableExperimentalFeatures)
    {
        return enableExperimentalFeatures;
    }
}
