package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.TaxesAndFees;

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

            assertThat(left.getIrr(), is(right.getIrr()));
            assertThat(left.getTrueTimeWeightedRateOfReturn(), is(right.getTrueTimeWeightedRateOfReturn()));
            assertThat(left.getTrueTimeWeightedRateOfReturnAnnualized(),
                            is(right.getTrueTimeWeightedRateOfReturnAnnualized()));
            assertThat(left.getMaxDrawdown(), is(right.getDrawdown().getMaxDrawdown()));
            assertThat(left.getMaxDrawdownDuration(), is(right.getDrawdown().getMaxDrawdownDuration().getDays()));
            assertThat(left.getVolatility(), is(right.getVolatility().getStandardDeviation()));
            assertThat(left.getSemiVolatility(), is(right.getVolatility().getSemiDeviation()));
            assertThat(left.getDelta(), is(right.getDelta()));
            assertThat(left.getDeltaPercent(), is(right.getDeltaPercent()));
            assertThat(left.getSumOfDividends(), is(right.getSumOfDividends()));

            assertThat(left.getDividendEventCount(), is(right.getDividendEventCount()));
            assertThat(left.getLastDividendPayment(), is(right.getLastDividendPayment()));
            assertThat(left.getPeriodicity(), is(right.getPeriodicity()));
            
            assertThat(left.getTotalRateOfReturnDiv(), is(right.getTotalRateOfReturnDiv(CostMethod.FIFO)));
            assertThat(left.getTotalRateOfReturnDivMovingAverage(),
                            is(right.getTotalRateOfReturnDiv(CostMethod.MOVING_AVERAGE)));

            assertThat(left.getRealizedCapitalGains().getCapitalGains(),
                            is(right.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains()));
            assertThat(left.getRealizedCapitalGains().getForexCaptialGains(),
                            is(right.getRealizedCapitalGains(CostMethod.FIFO).getForexCaptialGains()));

            assertThat(left.getUnrealizedCapitalGains().getCapitalGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains()));
            assertThat(left.getUnrealizedCapitalGains().getForexCaptialGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.FIFO).getForexCaptialGains()));

            assertThat(left.getRealizedCapitalGainsMovingAvg().getCapitalGains(),
                            is(right.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains()));
            assertThat(left.getRealizedCapitalGainsMovingAvg().getForexCaptialGains(),
                            is(right.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE)
                                            .getForexCaptialGains()));

            assertThat(left.getUnrealizedCapitalGainsMovingAvg().getCapitalGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains()));
            assertThat(left.getUnrealizedCapitalGainsMovingAvg().getForexCaptialGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getForexCaptialGains()));

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
        assertThat(left.getMarketValue(), is(right.getMarketValue()));
        assertThat(left.getQuote(), is(right.getQuote()));

        assertThat(left.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(right.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED)));
        assertThat(left.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(right.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED)));

        assertThat(left.getCapitalGainsOnHoldings(), is(right.getCapitalGainsOnHoldings(CostMethod.FIFO)));
        assertThat(left.getCapitalGainsOnHoldingsPercent(),
                        is(right.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO)));

        assertThat(left.getCapitalGainsOnHoldingsMovingAverage(),
                        is(right.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE)));
        assertThat(left.getCapitalGainsOnHoldingsMovingAveragePercent(),
                        is(right.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE)));

        assertThat(left.getCostPerSharesHeld(CostMethod.FIFO), is(right.getCostPerSharesHeld(CostMethod.FIFO)));

        assertThat(left.getSharesHeld(), is(right.getSharesHeld()));
        assertThat(left.getFees(), is(right.getFees()));
        assertThat(left.getTaxes(), is(right.getTaxes()));
    }
}
