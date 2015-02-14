package name.abuchen.portfolio.snapshot;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.util.Date;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.math.RiskTest;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class PerformanceIndexTest
{

    private final class PerformanceIndexStub extends PerformanceIndex
    {
        private PerformanceIndexStub(Date[] dates, long[] totals, double[] delta)
        {
            super(new Client(), new TestCurrencyConverter(), new ReportingPeriod.LastX(1, 0));

            this.dates = dates;
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
        Date[] dates = new Date[] { Dates.date("2015-02-02"), Dates.date("2015-02-03"), Dates.date("2015-02-04"),
                        Dates.date("2015-02-05"), Dates.date("2015-02-06"), Dates.date("2015-02-07") /* weekend */,
                        Dates.date("2015-02-08") /* weekend */, Dates.date("2015-02-09"), Dates.date("2015-02-10") };
        long[] totals = new long[] { 1000, 1500, 1000, 500, 1000, 1000, 1000, 2000, 1000 };
        double[] delta = new double[] { 0, 0.5, -1 / 3d, -0.5, 1, 0, 0, 1, -0.5 };

        PerformanceIndex index = new PerformanceIndexStub(dates, totals, delta);

        assertThat(index.getVolatility().getStandardDeviation(), closeTo(Math.sqrt(3414d / 7776), 0.1e-10));
        assertThat(index.getVolatility().getSemiDeviation(), closeTo(Math.sqrt(1611d / 7776), 0.1e-10));
    }

    @Test
    public void testVolatilityWithFirstDataPointLater()
    {
        Date[] dates = new Date[] { Dates.date("2015-02-02"), Dates.date("2015-02-03"), Dates.date("2015-02-04"),
                        Dates.date("2015-02-05"), Dates.date("2015-02-06"), Dates.date("2015-02-07") /* weekend */,
                        Dates.date("2015-02-08") /* weekend */, Dates.date("2015-02-09"), Dates.date("2015-02-10"),
                        Dates.date("2015-02-11"), Dates.date("2015-02-12") };
        long[] totals = new long[] { 0, 0, 1000, 1500, 1000, 1000, 1000, 500, 1000, 2000, 1000 };
        double[] delta = new double[] { 0, 0, 0, 0.5, -1 / 3d, 0, 0, -0.5, 1, 1, -0.5 };

        PerformanceIndex index = new PerformanceIndexStub(dates, totals, delta);

        assertThat(index.getVolatility().getStandardDeviation(), closeTo(Math.sqrt(3414d / 7776), 0.1e-10));
        assertThat(index.getVolatility().getSemiDeviation(), closeTo(Math.sqrt(1611d / 7776), 0.1e-10));
    }
}
