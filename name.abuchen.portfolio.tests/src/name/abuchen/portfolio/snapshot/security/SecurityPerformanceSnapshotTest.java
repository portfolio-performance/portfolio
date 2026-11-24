package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Transaction.Unit;
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

    /**
     * If the reporting currency is the currency of the security, the delta
     * must use the exchange rate of the purchase (like the cost, the capital
     * gains and the IRR) and not the exchange rate of the day.
     */
    @Test
    public void testDeltaUsesExchangeRateOfTransaction()
    {
        var client = new Client();

        var security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2015-01-02", Values.Quote.factorize(130)) //
                        .addPrice("2015-01-16", Values.Quote.factorize(130)) //
                        .addTo(client);

        var account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        var portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        // 10 shares at 130 USD paid with 1,000 EUR (1 EUR = 1.30 USD, daily
        // rate 1.2043)
        var entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2015-01-02T00:00"));
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(10));
        entry.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1300)),
                        BigDecimal.ONE.divide(BigDecimal.valueOf(1.3), 10, RoundingMode.HALF_UP)));
        entry.insert();

        var interval = Interval.of(LocalDate.parse("2015-01-01"), LocalDate.parse("2015-01-16"));
        var record = LazySecurityPerformanceSnapshot
                        .create(client, new TestCurrencyConverter(CurrencyUnit.USD), interval).getRecord(security)
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1300))));
        assertThat(record.getDelta(), is(Money.of(CurrencyUnit.USD, 0)));
        assertThat(record.getDeltaPercent(), is(0d));
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
}
