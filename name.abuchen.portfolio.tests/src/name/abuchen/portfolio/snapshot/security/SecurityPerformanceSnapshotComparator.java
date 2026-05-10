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

            assertThat(left.getDividendEventCount(), is(right.getDividendEventCount().get()));
            assertThat(left.getLastDividendPayment(), is(right.getLastDividendPayment().get()));
            assertThat(left.getPeriodicity(), is(right.getPeriodicity().get()));
            
            assertThat(left.getTotalRateOfReturnDiv(), is(right.getTotalRateOfReturnDiv(CostMethod.FIFO).get()));
            assertThat(left.getTotalRateOfReturnDivMovingAverage(),
                            is(right.getTotalRateOfReturnDiv(CostMethod.MOVING_AVERAGE).get()));

            assertThat(left.getRealizedCapitalGains().getCapitalGains(),
                            is(right.getRealizedCapitalGains(CostMethod.FIFO).get().getCapitalGains()));
            assertThat(left.getRealizedCapitalGains().getForexCaptialGains(),
                            is(right.getRealizedCapitalGains(CostMethod.FIFO).get().getForexCaptialGains()));

            assertThat(left.getUnrealizedCapitalGains().getCapitalGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.FIFO).get().getCapitalGains()));
            assertThat(left.getUnrealizedCapitalGains().getForexCaptialGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.FIFO).get().getForexCaptialGains()));

            assertThat(left.getRealizedCapitalGainsMovingAvg().getCapitalGains(),
                            is(right.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).get().getCapitalGains()));
            assertThat(left.getRealizedCapitalGainsMovingAvg().getForexCaptialGains(),
                            is(right.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).get()
                                            .getForexCaptialGains()));

            assertThat(left.getUnrealizedCapitalGainsMovingAvg().getCapitalGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).get().getCapitalGains()));
            assertThat(left.getUnrealizedCapitalGainsMovingAvg().getForexCaptialGains(),
                            is(right.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).get().getForexCaptialGains()));

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
        assertThat(left.getQuote(), is(right.getQuote().get()));

        assertThat(left.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(right.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED)));
        assertThat(left.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(right.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED)));

        assertThat(left.getCapitalGainsOnHoldings(), is(right.getCapitalGainsOnHoldings(CostMethod.FIFO).get()));
        assertThat(left.getCapitalGainsOnHoldingsPercent(),
                        is(right.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO).get()));

        assertThat(left.getCapitalGainsOnHoldingsMovingAverage(),
                        is(right.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE).get()));
        assertThat(left.getCapitalGainsOnHoldingsMovingAveragePercent(),
                        is(right.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE).get()));

        assertThat(left.getCostPerSharesHeld(CostMethod.FIFO), is(right.getCostPerSharesHeld(CostMethod.FIFO).get()));

        assertThat(left.getSharesHeld(), is(right.getSharesHeld().get()));
        assertThat(left.getFees(), is(right.getFees().get()));
        assertThat(left.getTaxes(), is(right.getTaxes().get()));
    }
}
