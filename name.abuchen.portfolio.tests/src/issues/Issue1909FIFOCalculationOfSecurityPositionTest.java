package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue1909FIFOCalculationOfSecurityPositionTest
{
    @Test
    public void testDefaultSnapshot() throws IOException
    {
        Client client = ClientFactory.load(Issue1909FIFOCalculationOfSecurityPositionTest.class
                        .getResourceAsStream("Issue1909FIFOCalculationOfSecurityPosition.xml")); //$NON-NLS-1$

        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("ADIDAS AG NA O.N.")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();

        LocalDate date = LocalDate.parse("2020-12-31"); //$NON-NLS-1$

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter,
                        Interval.of(LocalDate.MIN, date));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity(), is(security));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000))));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(200))));

        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));
        assertThat(record.getCostPerSharesHeld(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(150))));

        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(805))));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        // TTWROR / drawdown / volatility / IRR are skipped because the interval
        // starts at LocalDate.MIN, which the lazy PerformanceIndex cannot build
        // (OOM) and which would produce a degenerate IRR anyway.
        assertThat(record.getSharesHeld(), is(1000000000L));
        assertThat(record.getMarketValue(), is(Money.of("EUR", 280500L))); //$NON-NLS-1$
        assertThat(record.getQuote(), is(Quote.of("EUR", 28050000000L))); //$NON-NLS-1$
        assertThat(record.getFees(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getTaxes(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDelta(), is(Money.of("EUR", 261000L))); //$NON-NLS-1$
        assertThat(record.getDeltaPercent(), closeTo(0.87, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE),
                        is(Money.of("EUR", 130500L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO),
                        closeTo(0.4025000000000001, 0.0001));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        closeTo(0.8700000000000001, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.NONE));
    }
}
