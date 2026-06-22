package name.abuchen.portfolio.ui.preferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

@Creatable
@Singleton
public class Experiments
{
    public static enum Feature
    {
        JULY26_PREVENT_UPDATE_WHILE_EDITING_CELLS,
        /**
         * Experimental feature for relaxing the restriction of providing
         * positive values for the most input and CSV fields. With this set to
         * true, most fields also accepts negative values. In addition the
         * (gross) amount now accepts 0 (zero).
         * <p/>
         * ATTENTION: <br/>
         * After setting this to true and adding some transactions ('Buchungen')
         * that use this feature, there is NO WAY BACK!
         */
        JAN26_ALLOW_NEGATIVE_VALUES
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
