package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Compares the result of security with the result of the lazy security
 * performance snapshot.
 */
public class SecurityPerformanceSnapshotComparator
{

    private final SecurityPerformanceSnapshot regular;
    private final LazySecurityPerformanceSnapshot lazy;

    public SecurityPerformanceSnapshotComparator(SecurityPerformanceSnapshot regular,
                    LazySecurityPerformanceSnapshot lazy)
    {
        this.regular = regular;
        this.lazy = lazy;
    }

    public void compare()
    {
        assertThat(regular.getRecords().size(), is(lazy.getRecords().size()));

        for (var left : regular.getRecords())
        {
            var right = lazy.getRecord(left.getSecurity()).get();

            assertThat(left.getIrr(), is(right.getIrr().get()));
            assertThat(left.getTrueTimeWeightedRateOfReturn(), is(right.getTrueTimeWeightedRateOfReturn().get()));
            assertThat(left.getTrueTimeWeightedRateOfReturnAnnualized(),
                            is(right.getTrueTimeWeightedRateOfReturnAnnualized().get()));
            assertThat(left.getMaxDrawdown(), is(right.getDrawdown().get().getMaxDrawdown()));
            assertThat(left.getMaxDrawdownDuration(), is(right.getDrawdown().get().getMaxDrawdownDuration().getDays()));
            assertThat(left.getVolatility(), is(right.getVolatility().get().getStandardDeviation()));
            assertThat(left.getSemiVolatility(), is(right.getVolatility().get().getSemiDeviation()));
            assertThat(left.getDelta(), is(right.getDelta().get()));
            assertThat(left.getDeltaPercent(), is(right.getDeltaPercent().get()));
            assertThat(left.getSumOfDividends(), is(right.getSumOfDividends().get()));
            assertThat(left.getTotalRateOfReturnDiv(), is(right.getTotalRateOfReturnDiv().get()));
            assertThat(left.getTotalRateOfReturnDivMovingAverage(),
                            is(right.getTotalRateOfReturnDivMovingAverage().get()));

            assertCosts(left, right);

        }
    }

    public void compareCosts()
    {
        assertThat(regular.getRecords().size(), is(lazy.getRecords().size()));

        for (var left : regular.getRecords())
        {
            var right = lazy.getRecord(left.getSecurity()).get();
            assertCosts(left, right);
        }
    }

    private void assertCosts(SecurityPerformanceRecord left, LazySecurityPerformanceRecord right)
    {
        assertThat(left.getMarketValue(), is(right.getMarketValue().get()));
        assertThat(left.getFifoCost(), is(right.getFifoCost().get()));
        assertThat(left.getMovingAverageCost(), is(right.getMovingAverageCost().get()));
        assertThat(left.getCapitalGainsOnHoldings(), is(right.getCapitalGainsOnHoldings().get()));
        assertThat(left.getCapitalGainsOnHoldingsPercent(), is(right.getCapitalGainsOnHoldingsPercent().get()));
        assertThat(left.getCapitalGainsOnHoldingsMovingAverage(),
                        is(right.getCapitalGainsOnHoldingsMovingAverage().get()));
        assertThat(left.getCapitalGainsOnHoldingsMovingAveragePercent(),
                        is(right.getCapitalGainsOnHoldingsMovingAveragePercent().get()));
        assertThat(left.getFifoCostPerSharesHeld(), is(right.getFifoCostPerSharesHeld().get()));
    }
}
