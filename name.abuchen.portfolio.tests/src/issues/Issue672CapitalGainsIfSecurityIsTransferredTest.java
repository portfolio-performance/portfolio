package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue672CapitalGainsIfSecurityIsTransferredTest
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue371PurchaseValueWithTransfersTest.class
                        .getResourceAsStream("Issue672CapitalGainsIfSecurityIsTransferred.xml")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        Portfolio secondPortfolio = client.getPortfolios().get(1);
        Interval period = Interval.of(LocalDate.parse("2016-01-01"), //$NON-NLS-1$
                        LocalDate.parse("2017-01-01")); //$NON-NLS-1$

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot
                        .create(new PortfolioClientFilter(secondPortfolio).filter(client), converter, period);

        assertThat(snapshot.getRecords().size(), is(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getMarketValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(971.41))));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(883.1))));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO),
                        is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(88.31))));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO),
                        is(IsCloseTo.closeTo(0.1d, 0.0000000001)));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        // IRR is omitted because the filtered client produces a degenerate
        // cash-flow series with an absurdly large IRR.
        assertThat(record.getSharesHeld(), is(1000000000L));
        assertThat(record.getQuote(), is(Quote.of("EUR", 9714100000L))); //$NON-NLS-1$
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of("EUR", 88310L))); //$NON-NLS-1$
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of("EUR", 8831000000L))); //$NON-NLS-1$
        assertThat(record.getFees(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getTaxes(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDelta(), is(Money.of("EUR", 8831L))); //$NON-NLS-1$
        assertThat(record.getDeltaPercent(), IsCloseTo.closeTo(0.1, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE), is(Money.of("EUR", 8831L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        IsCloseTo.closeTo(0.1, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), IsCloseTo.closeTo(0.1, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturnAnnualized(), IsCloseTo.closeTo(0.09971358593414137, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdown(), IsCloseTo.closeTo(0.0, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdownDuration().getDays(), is(1L));
        assertThat(record.getVolatility().getStandardDeviation(), IsCloseTo.closeTo(0.0, 0.0001));
        assertThat(record.getVolatility().getSemiDeviation(), IsCloseTo.closeTo(0.0, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.NONE));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.FIFO), IsCloseTo.closeTo(0.0, 0.0001));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.MOVING_AVERAGE), IsCloseTo.closeTo(0.0, 0.0001));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 8831L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 8831L))); //$NON-NLS-1$
    }
}
