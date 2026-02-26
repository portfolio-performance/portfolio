package name.abuchen.portfolio.ui.preferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class Experiments
{
    public enum Feature
    {
        JULY26_PREVENT_UPDATE_WHILE_EDITING_CELLS
    }

    public boolean isEnabled(Feature feature)
    {
        Set<String> set = getFeatures();
        return set.contains(feature.name());
    }

    private Set<String> getFeatures()
    {
        var store = PortfolioPlugin.getDefault().getPreferenceStore();
        var serialized = store.getString(UIConstants.Preferences.EXPERIMENTS);
        if (serialized == null || serialized.isEmpty())
            return Collections.emptySet();
        return new HashSet<>(Arrays.asList(serialized.split(","))); //$NON-NLS-1$
    }
}
