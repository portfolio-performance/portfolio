package name.abuchen.portfolio.ui.util.chart;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.swt.graphics.RGB;
import org.junit.Test;

import name.abuchen.portfolio.ui.util.chart.CircularChart.PieColors;

/**
 * Regression test for issue #2829: pie chart colors must not be reused for
 * different slices and must never be white (which makes slices invisible).
 */
@SuppressWarnings("nls")
public class PieColorsTest
{
    @Test
    public void testAllColorsAreUnique()
    {
        var wheel = new PieColors();
        var colors = new ArrayList<RGB>();

        for (int ii = 0; ii < 360; ii++)
            colors.add(wheel.nextRGB());

        var unique = new HashSet<RGB>(colors);
        assertThat("All colors must be unique", unique.size(), is(colors.size()));
    }

    @Test
    public void testConsecutiveColorsAreDistinct()
    {
        var wheel = new PieColors();
        var colors = new ArrayList<RGB>();

        for (int ii = 0; ii < 360; ii++)
            colors.add(wheel.nextRGB());

        for (int ii = 1; ii < colors.size(); ii++)
        {
            RGB previous = colors.get(ii - 1);
            RGB current = colors.get(ii);
            int delta = Math.abs(previous.red - current.red) + Math.abs(previous.green - current.green)
                            + Math.abs(previous.blue - current.blue);
            assertThat("Consecutive colors must be clearly distinguishable", delta, is(greaterThan(50)));
        }
    }

    @Test
    public void testColorsAreNeverWhiteOrGray()
    {
        var wheel = new PieColors();

        for (int ii = 0; ii < 360; ii++)
        {
            float[] hsb = wheel.nextRGB().getHSB();

            assertThat("Color must be saturated (never gray/white)", hsb[1], is(greaterThan(0.3f)));
            assertThat("Color must not be close to white", hsb[2], is(lessThan(1.0f)));
        }
    }
}
