package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
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

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01")));

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

        SecurityPosition position = ClientSnapshot
                        .create(client, new TestCurrencyConverter(), reportingPeriod.getEnd())
                        .getPositionsByVehicle().get(security).getPosition();

        assertThat(position.getFIFOPurchaseValue(), is(record.getFifoCost()));
        assertThat(position.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.9))));

        assertThat(position.getMovingAveragePurchaseValue(), is(record.getMovingAverageCost()));
        assertThat(position.getMovingAveragePurchasePrice(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.9))));
    }
}
