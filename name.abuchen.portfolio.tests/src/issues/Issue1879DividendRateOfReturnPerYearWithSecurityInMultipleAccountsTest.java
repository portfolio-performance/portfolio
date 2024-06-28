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
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.security.CalculationLineItem.DividendPayment;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshotComparator;
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

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, converter, period);

        new SecurityPerformanceSnapshotComparator(snapshot,
                        LazySecurityPerformanceSnapshot.create(client, converter, period)).compare();

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurityName(), is("Public Joint Stock Company Gazprom")); //$NON-NLS-1$

        assertThat(record.getDividendEventCount(), is(2));

        assertThat(record.getRateOfReturnPerYear(), is(IsCloseTo.closeTo(0.096466, 0.000001)));

        record.getLineItems().stream().filter(item -> item instanceof DividendPayment).map(DividendPayment.class::cast)
                        .forEach(payment -> assertThat(payment.getPersonalDividendYieldMovingAverage(),
                                        is(IsCloseTo.closeTo(0.096466, 0.000001))));

    }
}
