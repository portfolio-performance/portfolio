package name.abuchen.portfolio.ui.util.chart;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.graphics.RGB;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ChartColorWheelTest
{
    private ChartColorWheel colorWheel;

    @Before
    public void setup()
    {
        colorWheel = new ChartColorWheel();
    }

    @Test
    public void testColorConsistency()
    {
        var vehicle = new Security();

        var color1 = colorWheel.getRGB(vehicle);
        var color2 = colorWheel.getRGB(vehicle);

        assertThat("Same vehicle should get identical color", color1, is(color2));
    }

    @Test
    public void testColorDistinction()
    {
        var vehicle1 = new Security();
        var vehicle2 = new Security();

        var color1 = colorWheel.getRGB(vehicle1);
        var color2 = colorWheel.getRGB(vehicle2);

        assertThat("Different vehicles should get different colors", color1, is(not(color2)));
    }

    @Test
    public void testMultipleColorsHaveExpectedDistance()
    {
        List<RGB> colors = new ArrayList<>();

        // Create 8 vehicles to test good color distribution
        for (int ii = 0; ii < 8; ii++)
            colors.add(colorWheel.getRGB(new Security()));

        // Check that colors are sufficiently distinct
        for (int ii = 0; ii < colors.size(); ii++)
        {
            for (int jj = ii + 1; jj < colors.size(); jj++)
            {
                float[] hsb1 = colors.get(ii).getHSB();
                float[] hsb2 = colors.get(jj).getHSB();
                float diff = Math.abs(hsb1[0] - hsb2[0]) % 360;
                float distance = diff > 180 ? 360 - diff : diff;
                assertThat("Colors should be at least 44 degrees apart", distance, is(greaterThan(44.0f)));
            }
        }
    }

    @Test
    public void testLargeNumberOfInstruments()
    {
        var uniqueColors = new HashSet<RGB>();

        for (int i = 0; i < 600; i++)
        {
            var color = colorWheel.getRGB(new Security());
            assertThat("Color should not be null for vehicle " + i, color, is(notNullValue()));

            uniqueColors.add(color);
        }

        // All vehicles should get colors (may have some duplicates due to RGB
        // precision)
        assertThat("Should have significant number of unique colors", uniqueColors.size(), is(greaterThan(500)));
    }
}
