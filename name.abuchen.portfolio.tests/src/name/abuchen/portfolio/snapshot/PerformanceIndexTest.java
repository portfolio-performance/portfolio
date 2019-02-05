package name.abuchen.portfolio.snapshot;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class PerformanceIndexTest
{

    private final class PerformanceIndexStub extends PerformanceIndex
    {
        private PerformanceIndexStub(LocalDate[] dates, long[] totals, double[] delta)
        {
            super(new Client(), new TestCurrencyConverter(),
                            new ReportingPeriod.LastX(1, 0).toInterval(LocalDate.now()));

            this.dates = dates;
            this.totals = totals;
            this.delta = delta;
        }
    }

    /**
     * Companion test for basic volatility RiskTest#testVolatility
     */
    @Test
    public void testVolatilityFromPerformanceIndex()
    {
        LocalDate[] dates = new LocalDate[] { LocalDate.parse("2015-02-02"), LocalDate.parse("2015-02-03"),
                        LocalDate.parse("2015-02-04"), LocalDate.parse("2015-02-05"), LocalDate.parse("2015-02-06"),
                        LocalDate.parse("2015-02-07") /* weekend */, LocalDate.parse("2015-02-08") /* weekend */,
                        LocalDate.parse("2015-02-09"), LocalDate.parse("2015-02-10") };
        long[] totals = new long[] { 1000, 1500, 1000, 500, 1000, 1000, 1000, 2000, 1000 };
        double[] delta = new double[] { 0, 0.005, -1 / 300d, -0.005, 0.01, 0, 0, 0.01, -0.005 };

        PerformanceIndex index = new PerformanceIndexStub(dates, totals, delta);

        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.017736692475, 0.1e-10));
        assertThat(index.getVolatility().getSemiDeviation(), closeTo(0.012188677034, 0.1e-10));
    }

    @Test
    public void testVolatilityWithFirstDataPointLater()
    {
        LocalDate[] dates = new LocalDate[] { LocalDate.parse("2015-02-02"), LocalDate.parse("2015-02-03"),
                        LocalDate.parse("2015-02-04"), LocalDate.parse("2015-02-05"), LocalDate.parse("2015-02-06"),
                        LocalDate.parse("2015-02-07") /* weekend */, LocalDate.parse("2015-02-08") /* weekend */,
                        LocalDate.parse("2015-02-09"), LocalDate.parse("2015-02-10"), LocalDate.parse("2015-02-11"),
                        LocalDate.parse("2015-02-12") };
        long[] totals = new long[] { 0, 0, 1000, 1500, 1000, 1000, 1000, 500, 1000, 2000, 1000 };
        double[] delta = new double[] { 0, 0, 0, 0.005, -1 / 300d, 0, 0, -0.005, 0.01, 0.01, -0.005 };

        PerformanceIndex index = new PerformanceIndexStub(dates, totals, delta);

        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.017736692475, 0.1e-10));
        assertThat(index.getVolatility().getSemiDeviation(), closeTo(0.012188677034, 0.1e-10));
    }
}
