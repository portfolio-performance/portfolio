package name.abuchen.portfolio.snapshot;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import name.abuchen.portfolio.math.RiskTest;
import name.abuchen.portfolio.model.Client;

import org.junit.Test;

public class PerformanceIndexTest
{

    private final class PerformanceIndexStub extends PerformanceIndex
    {
        private PerformanceIndexStub(long totals[], double delta[])
        {
            super(new Client(), new ReportingPeriod.LastX(1, 0));

            this.totals = totals;
            this.delta = delta;
        }
    }

    /**
     * Companion test for basic volatility {@link RiskTest#testVolatility()}
     */
    @Test
    public void testVolatilityFromPerformanceIndex()
    {
        long[] totals = new long[] { 1000, 1500, 1000, 500, 1000, 2000, 1000 };
        double[] delta = new double[] { 0, 0.5, -1 / 3d, -0.5, 1, 1, -0.5 };

        PerformanceIndex index = new PerformanceIndexStub(totals, delta);

        assertThat(index.getVolatility(), closeTo(Math.sqrt(3414d / 7776), 0.1e-10));
        assertThat(index.getSemiVolatility(), closeTo(Math.sqrt(1611d / 3888), 0.1e-10));
    }

    @Test
    public void testVolatilityWithFirstDataPointLater()
    {
        long[] totals = new long[] { 0, 0, 1000, 1500, 1000, 500, 1000, 2000, 1000 };
        double[] delta = new double[] { 0, 0, 0, 0.5, -1 / 3d, -0.5, 1, 1, -0.5 };

        PerformanceIndex index = new PerformanceIndexStub(totals, delta);

        assertThat(index.getVolatility(), closeTo(Math.sqrt(3414d / 7776), 0.1e-10));
        assertThat(index.getSemiVolatility(), closeTo(Math.sqrt(1611d / 3888), 0.1e-10));
    }
}
