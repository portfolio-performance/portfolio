package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class SecurityPerformanceSnapshotTest
{

    @Test
    public void testBigPurchases()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        new PortfolioBuilder()
                        .buy(security, "2018-05-01", Values.Share.factorize(500000), Values.Amount.factorize(450000))
                        .sell(security, "2018-05-08", Values.Share.factorize(500000), Values.Amount.factorize(494500))
                        .addTo(client);

        final Interval interval = Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        assertThat(snapshot.getRecords(), hasSize(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getSharesHeld(), is(0L));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, 0)));

        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getCostPerSharesHeld(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testBigPurchases2()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        new PortfolioBuilder()
                        .buy(security, "2018-05-01", Values.Share.factorize(500000), Values.Amount.factorize(450000))
                        .sell(security, "2018-05-08", Values.Share.factorize(1), Values.Amount.factorize(0.989))
                        .addTo(client);

        Interval reportingPeriod = Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), reportingPeriod);

        assertThat(snapshot.getRecords(), hasSize(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getSharesHeld(), is(Values.Share.factorize(499999)));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450000 - 0.9))));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.9))));

        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450000 - 0.9))));
        assertThat(record.getCostPerSharesHeld(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.9))));

        SecurityPosition position = ClientSnapshot.create(client, new TestCurrencyConverter(), reportingPeriod.getEnd())
                        .getPositionsByVehicle().get(security).getPosition();

        assertThat(position.getShares(), is(record.getSharesHeld()));
        assertThat(position.calculateValue(), is(record.getMarketValue()));
    }

    @Test
    public void testOpeningValueOfPositionHeldBeforeTheInterval()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2018-03-01", Values.Quote.factorize(10)) //
                        .addPrice("2018-04-01", Values.Quote.factorize(12)) //
                        .addPrice("2018-06-01", Values.Quote.factorize(15)) //
                        .addTo(client);

        new PortfolioBuilder()
                        .buy(security, "2018-03-15", Values.Share.factorize(100), Values.Amount.factorize(1000))
                        .addTo(client);

        Interval interval = Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        assertThat(snapshot.getRecords(), hasSize(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getOpeningValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 12d))));
        assertThat(record.getMarketValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 15d))));
    }

    @Test
    public void testOpeningValueIsZeroForPositionAcquiredWithinTheInterval()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2018-04-01", Values.Quote.factorize(10)) //
                        .addPrice("2018-06-01", Values.Quote.factorize(15)) //
                        .addTo(client);

        new PortfolioBuilder()
                        .buy(security, "2018-05-01", Values.Share.factorize(100), Values.Amount.factorize(1000))
                        .addTo(client);

        Interval interval = Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        assertThat(snapshot.getRecords(), hasSize(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getOpeningValue(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getMarketValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 15d))));
    }

    /**
     * Issue #463: if the reporting period extends into the future, the
     * valuation at the end must be dated today and not at the end of the
     * reporting period. Otherwise the quote is displayed with a future date and
     * the IRR is calculated over a too long period.
     */
    @Test
    public void testReportingPeriodExtendingIntoTheFuture()
    {
        var today = LocalDate.parse("2024-05-10");

        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, invocation -> {
            if (invocation.getMethod().getName().equals("now"))
                return today;
            else
                return invocation.callRealMethod();
        }))
        {
            var client = new Client();

            var security = new SecurityBuilder() //
                            .addPrice("2024-03-11", Values.Quote.factorize(100)) //
                            .addPrice("2024-05-10", Values.Quote.factorize(110)) //
                            .addTo(client);

            new PortfolioBuilder() //
                            .buy(security, "2024-03-11", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                            .addTo(client);

            var converter = new TestCurrencyConverter();
            var startOfYear = LocalDate.parse("2023-12-31");

            var currentYear = LazySecurityPerformanceSnapshot
                            .create(client, converter, Interval.of(startOfYear, LocalDate.parse("2024-12-31")))
                            .getRecords().get(0);
            var untilToday = LazySecurityPerformanceSnapshot
                            .create(client, converter, Interval.of(startOfYear, today)).getRecords().get(0);

            var valuationAtEnd = currentYear.getLineItems().stream()
                            .filter(CalculationLineItem.ValuationAtEnd.class::isInstance).findFirst().orElseThrow();
            assertThat(valuationAtEnd.getDateTime().toLocalDate(), is(today));

            assertThat(currentYear.getMarketValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1100))));
            assertThat(currentYear.getIrr(), closeTo(untilToday.getIrr(), 0.000001));
        }
    }
}
