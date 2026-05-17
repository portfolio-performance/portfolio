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
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.CalculationLineItem.DividendPayment;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue1879DividendRateOfReturnPerYearWithSecurityInMultipleAccountsTest
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue1879DividendRateOfReturnPerYearWithSecurityInMultipleAccountsTest.class
                        .getResourceAsStream("Issue1879DividendRateOfReturnPerYearWithSecurityInMultipleAccounts.xml")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        Interval period = Interval.of(LocalDate.parse("2019-12-31"), //$NON-NLS-1$
                        LocalDate.parse("2020-12-31")); //$NON-NLS-1$

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter, period);

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurityName(), is("Public Joint Stock Company Gazprom")); //$NON-NLS-1$

        assertThat(record.getDividendEventCount(), is(2));

        assertThat(record.getRateOfReturnPerYear(), is(IsCloseTo.closeTo(0.096466, 0.000001)));

        record.getLineItems().stream().filter(item -> item instanceof DividendPayment).map(DividendPayment.class::cast)
                        .forEach(payment -> assertThat(payment.getPersonalDividendYieldMovingAverage(),
                                        is(IsCloseTo.closeTo(0.096466, 0.000001))));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        assertThat(record.getSharesHeld(), is(20000000000L));
        assertThat(record.getMarketValue(), is(Money.of("EUR", 80120L))); //$NON-NLS-1$
        assertThat(record.getQuote(), is(Quote.of("EUR", 400600000L))); //$NON-NLS-1$
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(Money.of("EUR", 82930L))); //$NON-NLS-1$
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of("EUR", 82930L))); //$NON-NLS-1$
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO), is(Quote.of("EUR", 414650000L))); //$NON-NLS-1$
        assertThat(record.getFees(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getTaxes(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDelta(), is(Money.of("EUR", 5190L))); //$NON-NLS-1$
        assertThat(record.getDeltaPercent(), IsCloseTo.closeTo(0.06258290124201134, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO), is(Money.of("EUR", -2810L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE),
                        is(Money.of("EUR", -2810L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO),
                        IsCloseTo.closeTo(-0.03388399855299651, 0.0001));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        IsCloseTo.closeTo(-0.03388399855299651, 0.0001));
        assertThat(record.getIrr(), IsCloseTo.closeTo(0.08896216194634476, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), IsCloseTo.closeTo(0.017165724112631064, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturnAnnualized(), IsCloseTo.closeTo(0.01711842406680031, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdown(), IsCloseTo.closeTo(0.48943196829590485, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdownDuration().getDays(), is(152L));
        assertThat(record.getVolatility().getStandardDeviation(), IsCloseTo.closeTo(0.673373955731489, 0.0001));
        assertThat(record.getVolatility().getSemiDeviation(), IsCloseTo.closeTo(0.6706574007756855, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 8000L))); //$NON-NLS-1$
        assertThat(record.getLastDividendPayment(), is(LocalDate.parse("2020-08-01"))); //$NON-NLS-1$
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.UNKNOWN));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.FIFO), IsCloseTo.closeTo(0.09646689979500783, 0.0001));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.MOVING_AVERAGE),
                        IsCloseTo.closeTo(0.09646689979500783, 0.0001));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of("EUR", -2810L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", -2810L))); //$NON-NLS-1$
    }
}
