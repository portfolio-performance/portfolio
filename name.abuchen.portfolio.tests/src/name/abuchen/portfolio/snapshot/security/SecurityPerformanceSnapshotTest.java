package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
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
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        interval);

        new SecurityPerformanceSnapshotComparator(snapshot,
                        LazySecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(), interval))
                                        .compare();

        assertThat(snapshot.getRecords(), hasSize(1));

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getSharesHeld(), is(0L));

        assertThat(record.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getFifoCostPerSharesHeld(), is(Quote.of(CurrencyUnit.EUR, 0)));

        assertThat(record.getMovingAverageCost(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getMovingAverageCostPerSharesHeld(), is(Quote.of(CurrencyUnit.EUR, 0)));
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
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        reportingPeriod);

        new SecurityPerformanceSnapshotComparator(snapshot,
                        LazySecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(), reportingPeriod))
                                        .compare();

        assertThat(snapshot.getRecords(), hasSize(1));

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getSharesHeld(), is(Values.Share.factorize(499999)));

        assertThat(record.getFifoCost(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450000 - 0.9))));
        assertThat(record.getFifoCostPerSharesHeld(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.9))));

        assertThat(record.getMovingAverageCost(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450000 - 0.9))));
        assertThat(record.getMovingAverageCostPerSharesHeld(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.9))));

        SecurityPosition position = ClientSnapshot.create(client, new TestCurrencyConverter(), reportingPeriod.getEnd())
                        .getPositionsByVehicle().get(security).getPosition();

        assertThat(position.getShares(), is(record.getSharesHeld()));
        assertThat(position.calculateValue(), is(record.getMarketValue()));
    }

    @Test
    public void testDividendIsAttributedToExDateWithinInterval()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);
        Account account = new AccountBuilder().addTo(client);

        AccountTransaction withExDate = new AccountTransaction();
        withExDate.setType(AccountTransaction.Type.DIVIDENDS);
        withExDate.setSecurity(security);
        withExDate.setDateTime(LocalDateTime.parse("2020-01-15T00:00"));
        withExDate.setExDate(LocalDateTime.parse("2019-12-20T00:00"));
        withExDate.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 10_00));

        AccountTransaction withoutExDate = new AccountTransaction();
        withoutExDate.setType(AccountTransaction.Type.DIVIDENDS);
        withoutExDate.setSecurity(security);
        withoutExDate.setDateTime(LocalDateTime.parse("2020-01-15T00:00"));
        withoutExDate.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 5_00));

        account.addTransaction(withExDate);
        account.addTransaction(withoutExDate);

        Interval interval = Interval.of(LocalDate.parse("2019-12-01"), LocalDate.parse("2019-12-31"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        interval);

        assertThat(snapshot.getRecords(), hasSize(1));
        SecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSumOfDividends(), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(record.getDividendEventCount(), is(1));
        assertThat(record.getLastDividendPayment(), is(LocalDate.parse("2019-12-20")));
    }
}
