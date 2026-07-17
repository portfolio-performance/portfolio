package name.abuchen.portfolio.ui.views;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import name.abuchen.portfolio.model.CostMethod;

@SuppressWarnings("nls")
public class PerformanceViewTest
{
    private static final String PREFERENCE_KEY = "PerformanceView-CAPITAL_GAIN_USE_FIFO";

    @Test
    public void legacyTrueIsReadAsFifoAndNormalized()
    {
        var preferences = new PreferenceStore();
        preferences.setValue(PREFERENCE_KEY, "true");

        assertThat(PerformanceView.readCapitalGainCostMethod(preferences), is(CostMethod.FIFO));
        assertThat(preferences.getString(PREFERENCE_KEY), is(CostMethod.FIFO.name()));
    }

    @Test
    public void legacyFalseIsReadAsMovingAverageAndNormalized()
    {
        var preferences = new PreferenceStore();
        preferences.setValue(PREFERENCE_KEY, "false");

        assertThat(PerformanceView.readCapitalGainCostMethod(preferences), is(CostMethod.MOVING_AVERAGE));
        assertThat(preferences.getString(PREFERENCE_KEY), is(CostMethod.MOVING_AVERAGE.name()));
    }

    @Test
    public void canonicalValuesAreReadWithoutBeingRewritten()
    {
        var preferences = new PreferenceStore();

        preferences.setValue(PREFERENCE_KEY, CostMethod.FIFO.name());
        assertThat(PerformanceView.readCapitalGainCostMethod(preferences), is(CostMethod.FIFO));
        assertThat(preferences.getString(PREFERENCE_KEY), is(CostMethod.FIFO.name()));

        preferences.setValue(PREFERENCE_KEY, CostMethod.MOVING_AVERAGE.name());
        assertThat(PerformanceView.readCapitalGainCostMethod(preferences), is(CostMethod.MOVING_AVERAGE));
        assertThat(preferences.getString(PREFERENCE_KEY), is(CostMethod.MOVING_AVERAGE.name()));
    }

    @Test
    public void emptyAndUnknownValuesUseFifo()
    {
        var preferences = new PreferenceStore();

        assertThat(PerformanceView.readCapitalGainCostMethod(preferences), is(CostMethod.FIFO));

        preferences.setValue(PREFERENCE_KEY, "unknown");
        assertThat(PerformanceView.readCapitalGainCostMethod(preferences), is(CostMethod.FIFO));
        assertThat(preferences.getString(PREFERENCE_KEY), is("unknown"));
    }

    @Test
    public void fifoSelectionWritesCanonicalValue()
    {
        var preferences = new PreferenceStore();

        PerformanceView.writeCapitalGainCostMethod(preferences, CostMethod.FIFO);

        assertThat(preferences.getString(PREFERENCE_KEY), is("FIFO"));
    }

    @Test
    public void movingAverageSelectionWritesCanonicalValue()
    {
        var preferences = new PreferenceStore();

        PerformanceView.writeCapitalGainCostMethod(preferences, CostMethod.MOVING_AVERAGE);

        assertThat(preferences.getString(PREFERENCE_KEY), is("MOVING_AVERAGE"));
    }
}
