package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
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

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        // expected:
        // 3149,20 - round(3149,20 * 15/109) + 1684,92 + 959,30 = 5360,04385

        assertThat(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5360.04))));

        assertThat(cost.getFifoCostTrail().getValue(), is(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED)));

        // expected moving average is identical because it is only one buy
        // transaction
        // 3149,20 * 94/109 + 1684.92 + 959.30 = 5360,04385

        assertThat(cost.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5360.04))));
    }

    @Test
    public void testDistributionOutboundReducesBasisProportionally()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        // 100 shares, cost basis 5,000.00
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        // spin-off source leg: 25% of basis leaves, shares unchanged
        PortfolioTransaction distribution = new PortfolioTransaction();
        distribution.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        distribution.setDateTime(LocalDateTime.parse("2010-06-01T00:00"));
        distribution.setSecurity(security);
        distribution.setShares(0);
        distribution.setCurrencyCode(CurrencyUnit.EUR);
        distribution.setAmount(Values.Amount.factorize(2000)); // market value; NOT the basis

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(new BigDecimal("0.25")); // 25% of basis leaves
        entry.addLeg(portfolio, distribution, LegRole.SOURCE);
        portfolio.addTransaction(distribution);

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        // 5,000 - 25% = 3,750 basis remaining; shares still 100
        assertThat(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3750))));
        assertThat(cost.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3750))));
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

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        // expected:
        // 3149,20 + 1684,92 - round(3149,20 * 15/109) = 4400,743853211009174

        assertThat(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4400.74))));

        assertThat(cost.getFifoCostTrail().getValue(), is(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED)));

        // transaction
        // (3149,20 + 1684.92) * 146/161 = 4383,736149068322981

        assertThat(cost.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4383.74))));
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
                        is(record.explain(BaseSecurityPerformanceRecord.Trails.FIFO_COST)
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

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        assertThat(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
        assertThat(cost.getFifoCostTrail(), is(TrailRecord.empty()));
        assertThat(cost.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
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

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        assertThat(cost.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
        assertThat(cost.getFifoCostTrail(), is(TrailRecord.empty()));
        assertThat(cost.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testDistributionMultiLotConservationIsExact()
    {
        Client client = new Client();

        Security parent = new SecurityBuilder().addTo(client);
        Security spinco = new SecurityBuilder().addTo(client);

        // two separate lots of 50 shares @ 100.01 each -> total basis 200.02
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2010-01-01", Values.Share.factorize(50), Values.Amount.factorize(100.01)) //
                        .buy(parent, "2010-02-01", Values.Share.factorize(50), Values.Amount.factorize(100.01)) //
                        .addTo(client);

        var exDate = LocalDateTime.parse("2010-06-01T00:00");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(50));

        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setShares(Values.Share.factorize(50));
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setAmount(Values.Amount.factorize(50));

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(new BigDecimal("0.5"));
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        portfolio.addTransaction(source);
        portfolio.addTransaction(target);

        CurrencyConverter converter = new TestCurrencyConverter();
        Interval interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);

        Money parentFifo = snapshot.getRecord(parent).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money spincoFifo = snapshot.getRecord(spinco).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);

        // per-lot rounding: parent 100.00, spinco 100.02, sum == 200.02 to the cent
        assertThat(parentFifo, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(spincoFifo, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.02))));
        assertThat(parentFifo.add(spincoFifo), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.02))));

        Money parentMovAvg = snapshot.getRecord(parent).orElseThrow()
                        .getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED);
        Money spincoMovAvg = snapshot.getRecord(spinco).orElseThrow()
                        .getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED);

        // moving average rounds on the aggregate: parent 100.01, spinco 100.01, sum == 200.02
        assertThat(parentMovAvg, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.01))));
        assertThat(spincoMovAvg, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.01))));
        assertThat(parentMovAvg.add(spincoMovAvg), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.02))));
    }

}
