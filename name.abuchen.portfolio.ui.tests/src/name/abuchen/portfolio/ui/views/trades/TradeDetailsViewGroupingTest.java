package name.abuchen.portfolio.ui.views.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import name.abuchen.portfolio.snapshot.trades.TradeGrouping;

public class TradeDetailsViewGroupingTest
{
    private static TradeDetailsView.Input inputWith(TradeGrouping grouping)
    {
        return new TradeDetailsView.Input(null, Collections.emptyList(), Collections.emptyList(), false, grouping);
    }

    @Test
    public void testGroupingIsReadFromThePreferences()
    {
        var preferences = new PreferenceStore();
        preferences.setValue(TradeDetailsView.PREF_TRADE_GROUPING, TradeGrouping.PER_LOT.name());

        assertThat(TradeDetailsView.determineGrouping(null, preferences), is(TradeGrouping.PER_LOT));
    }

    @Test
    public void testGroupingOfPreselectedTradesWins()
    {
        // the trades of a preselected input are not recalculated, so the menu
        // must show the grouping the trades were built with - otherwise the
        // view claims a grouping that the rows in the table do not have

        var preferences = new PreferenceStore();
        preferences.setValue(TradeDetailsView.PREF_TRADE_GROUPING, TradeGrouping.PER_LOT.name());

        assertThat(TradeDetailsView.determineGrouping(inputWith(TradeGrouping.COMBINED), preferences),
                        is(TradeGrouping.COMBINED));
    }
}
