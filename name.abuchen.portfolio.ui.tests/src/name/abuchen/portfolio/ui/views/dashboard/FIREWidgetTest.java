package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class FIREWidgetTest
{
    @Test
    public void testCurrentValueAtTargetReturnsZero()
    {
        assertThat(FIREWidget.calculateTimeToFIRE(1_000_000_00L, 1_000_000_00L, 100_00L, 0.07), is(0.0));
    }

    @Test
    public void testZeroMonthlySavingsWithPositiveReturnHasFiniteTime()
    {
        double years = FIREWidget.calculateTimeToFIRE(500_000_00L, 1_000_000_00L, 0L, 0.07);

        assertThat(Double.isFinite(years), is(true));
        assertThat(years, closeTo(10.24, 0.05));
    }

    @Test
    public void testZeroMonthlySavingsWithNoGrowthReturnsInfinity()
    {
        double years = FIREWidget.calculateTimeToFIRE(500_000_00L, 1_000_000_00L, 0L, 0.0);

        assertThat(Double.isInfinite(years), is(true));
    }

    @Test
    public void testNegativeMonthlySavingsReturnsInfinity()
    {
        double years = FIREWidget.calculateTimeToFIRE(500_000_00L, 1_000_000_00L, -100_00L, 0.07);

        assertThat(Double.isInfinite(years), is(true));
    }

    @Test
    public void testPositiveMonthlySavingsWithPositiveReturnIsFinite()
    {
        double years = FIREWidget.calculateTimeToFIRE(500_000_00L, 1_000_000_00L, 1_000_00L, 0.07);

        assertThat(Double.isFinite(years), is(true));
        assertThat(years > 0, is(true));
    }
}
