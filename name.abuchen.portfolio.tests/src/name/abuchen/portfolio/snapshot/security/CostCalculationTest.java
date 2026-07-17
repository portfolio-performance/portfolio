package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CostCalculationTest
{
    private static CostCalculation calculate(CostMethod costMethod, List<CalculationLineItem> items)
    {
        Collections.sort(items, new CalculationLineItemComparator());

        CostCalculation cost = new CostCalculation(costMethod);
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), items);
        return cost;
    }

    private static List<CalculationLineItem> items(Portfolio... portfolios)
    {
        List<CalculationLineItem> items = new ArrayList<>();
        for (Portfolio portfolio : portfolios)
            portfolio.getTransactions().stream().map(t -> CalculationLineItem.of(portfolio, t)).forEach(items::add);
        return items;
    }

    @Test
    public void testFifoBuySellTransactions()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20)) //
                        .sell(security, "2010-02-01", Values.Share.factorize(15), Values.Amount.factorize(531.50)) //
                        .buy(security, "2010-03-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92)) //
                        .buy(security, "2010-03-01", Values.Share.factorize(32), Values.Amount.factorize(959.30)) //
                        .addTo(client);

        List<CalculationLineItem> lineItems = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());
        CostCalculation fifo = calculate(CostMethod.FIFO, new ArrayList<>(lineItems));

        // expected:
        // 3149,20 - round(3149,20 * 15/109) + 1684,92 + 959,30 = 5360,04385

        assertThat(fifo.getCost(TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5360.04))));

        assertThat(fifo.getCostTrail().orElseThrow().getValue(), is(fifo.getCost(TaxesAndFees.INCLUDED)));

        // expected moving average is identical because it is only one buy
        // transaction
        // 3149,20 * 94/109 + 1684.92 + 959.30 = 5360,04385

        CostCalculation movingAverage = calculate(CostMethod.MOVING_AVERAGE, new ArrayList<>(lineItems));
        assertThat(movingAverage.getCost(TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5360.04))));
        assertThat(movingAverage.getCostTrail().isEmpty(), is(true));
    }

    @Test
    public void testFifoBuySellTransactions2()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20)) //
                        .buy(security, "2010-02-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92)) //
                        .sell(security, "2010-03-01", Values.Share.factorize(15), Values.Amount.factorize(531.50)) //
                        .addTo(client);

        List<CalculationLineItem> lineItems = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());
        CostCalculation fifo = calculate(CostMethod.FIFO, new ArrayList<>(lineItems));

        // expected:
        // 3149,20 + 1684,92 - round(3149,20 * 15/109) = 4400,743853211009174

        assertThat(fifo.getCost(TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4400.74))));

        assertThat(fifo.getCostTrail().orElseThrow().getValue(), is(fifo.getCost(TaxesAndFees.INCLUDED)));

        // transaction
        // (3149,20 + 1684.92) * 146/161 = 4383,736149068322981

        CostCalculation movingAverage = calculate(CostMethod.MOVING_AVERAGE, new ArrayList<>(lineItems));
        assertThat(movingAverage.getCost(TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4383.74))));
        assertThat(movingAverage.getCostTrail().isEmpty(), is(true));
    }

    @Test
    public void testFifoBuySellTransactionsWithForex()
    {
        Client client = new Client();

        Security security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder(new Account()) //
                        .addTo(client);

        PortfolioTransaction tx = new PortfolioTransaction();
        tx.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        tx.setSecurity(security);
        tx.setDateTime(LocalDateTime.parse("2015-01-01T00:00"));
        tx.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1100)));
        tx.setShares(Values.Share.factorize(10));
        tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, tx.getMonetaryAmount(),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1000)), BigDecimal.valueOf(1.1)));
        portfolio.addTransaction(tx);

        tx = new PortfolioTransaction();
        tx.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        tx.setSecurity(security);
        tx.setDateTime(LocalDateTime.parse("2015-10-01T00:00"));
        tx.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1100)));
        tx.setShares(Values.Share.factorize(10));
        tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, tx.getMonetaryAmount(),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1000)), BigDecimal.valueOf(1.1)));
        portfolio.addTransaction(tx);

        CurrencyConverter converter = new TestCurrencyConverter().with(CurrencyUnit.EUR);

        var interval = Interval.of(LocalDate.parse("2015-01-16"), LocalDate.parse("2015-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);

        assertThat(snapshot.getRecords().size(), is(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        // 1.1588 = exchange rate of test currency converter on 2015-01-16
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((1000 / 1.1588) + 1100))));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(record.explain(BaseSecurityPerformanceRecord.Trails.COST, CostMethod.FIFO)
                                        .orElseThrow(IllegalArgumentException::new).getRecord().getValue()));
    }

    @Test
    public void testThatRoundingDifferencesAreRemovedIfZeroSharesHeld()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(10), 1) //
                        .sell(security, "2010-02-01", Values.Share.factorize(3), 1) //
                        .sell(security, "2010-03-01", Values.Share.factorize(3), 1) //
                        .sell(security, "2010-03-01", Values.Share.factorize(4), 1) //
                        .addTo(client);

        CostCalculation fifo = calculate(CostMethod.FIFO, items(portfolio));
        assertThat(fifo.getCost(TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
        assertThat(fifo.getCostTrail().orElseThrow(), is(TrailRecord.empty()));

        CostCalculation movingAverage = calculate(CostMethod.MOVING_AVERAGE, items(portfolio));
        assertThat(movingAverage.getCost(TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
        assertThat(movingAverage.getCostTrail().isEmpty(), is(true));
    }

    @Test
    public void testWhenSharesHeldGoToZero()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(100), 314920) //
                        .sell(security, "2010-02-01", Values.Share.factorize(100), 53150) //
                        .buy(security, "2010-03-01", Values.Share.factorize(50), 168492) //
                        .sell(security, "2010-04-01", Values.Share.factorize(50), 53150) //
                        .addTo(client);

        CostCalculation fifo = calculate(CostMethod.FIFO, items(portfolio));
        assertThat(fifo.getCost(TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
        assertThat(fifo.getCostTrail().orElseThrow(), is(TrailRecord.empty()));

        CostCalculation movingAverage = calculate(CostMethod.MOVING_AVERAGE, items(portfolio));
        assertThat(movingAverage.getCost(TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
        assertThat(movingAverage.getCostTrail().isEmpty(), is(true));
    }

}
