package name.abuchen.portfolio.ui.util.chart;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Test;

@SuppressWarnings("nls")
public class ChartLineWidthTest
{
    @After
    public void resetToDefault()
    {
        ChartLineWidth.set(ChartLineWidth.DEFAULT_WIDTH);
    }

    @Test
    public void testDefaultWidthIsUsedInitially()
    {
        assertThat(ChartLineWidth.get(), is(ChartLineWidth.DEFAULT_WIDTH));
    }

    @Test
    public void testSupportedWidthsAreApplied()
    {
        for (int width = ChartLineWidth.MIN_WIDTH; width <= ChartLineWidth.MAX_WIDTH; width++)
        {
            ChartLineWidth.set(width);
            assertThat(ChartLineWidth.get(), is(width));
        }
    }

    @Test
    public void testWidthsOutsideTheRangeFallBackToDefault()
    {
        // the preference can be edited manually and the injected value is 0 if
        // it cannot be read as a number

        for (int width : new int[] { 0, -1, ChartLineWidth.MAX_WIDTH + 1, 99 })
        {
            ChartLineWidth.set(width);
            assertThat("width " + width + " must fall back to the default", ChartLineWidth.get(),
                            is(ChartLineWidth.DEFAULT_WIDTH));
        }
    }
}
