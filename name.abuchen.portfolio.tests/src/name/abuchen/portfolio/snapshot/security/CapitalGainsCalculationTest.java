package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
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
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CapitalGainsCalculationTest
{

    @Test
    public void testPartialTransfersAndTrailMatches()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        Portfolio portfolioA = new PortfolioBuilder(new Account("one"))
                        .inbound_delivery(security, "2021-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000))
                        .outbound_delivery(security, "2021-01-02", Values.Share.factorize(5),
                                        Values.Amount.factorize(500), 0, 0)
                        .addTo(client);

        Portfolio portfolioB = new PortfolioBuilder(new Account("two")).addTo(client);

        PortfolioTransferEntry transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-01-03T00:00"));
        transfer.setShares(Values.Share.factorize(5));
        transfer.setAmount(Values.Amount.factorize(600));
        transfer.setCurrencyCode(security.getCurrencyCode());

        transfer.insert();

        var interval = Interval.of(LocalDate.parse("2020-12-31"), LocalDate.parse("2021-01-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        LazySecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        Money eur100 = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO), is(eur100));

        CapitalGainsRecord unrealizedCapitalGains = record.getUnrealizedCapitalGains(CostMethod.FIFO);
        assertThat(unrealizedCapitalGains.getCapitalGains(), is(eur100));
        assertThat(unrealizedCapitalGains.getCapitalGainsTrail().getValue(), is(eur100));

    }

    @Test
    public void testFifoBuySellTransactions()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2013-03-01", Values.Quote.factorize(100)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20)) //
                        .sell(security, "2010-02-01", Values.Share.factorize(15), Values.Amount.factorize(531.50)) //
                        .buy(security, "2010-03-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92)) //
                        .buy(security, "2010-03-01", Values.Share.factorize(32), Values.Amount.factorize(959.30)) //
                        .addTo(client);

        var interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2021-01-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);
        LazySecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        // expected Realized Gains for FIFO :
        // 531.5 - 3149.20 * 15/109 = 98,1238532110092
        Money expectedGains = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(98.12));
        CapitalGainsRecord realizedCapitalGains = record.getRealizedCapitalGains(CostMethod.FIFO);
        assertThat(realizedCapitalGains.getCapitalGains(), is(expectedGains));

        // expected Realized Gains for moving average is identical because it is
        // only one buy
        CapitalGainsRecord realizedCapitalGainsMovingAvg = record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE);
        assertThat(realizedCapitalGainsMovingAvg.getCapitalGains(), is(expectedGains));

        // expected Unrealized Gains for FIFO :
        // 100 * 178 - [3149.2 * (109-15) / 109 + 1684.92 + 959.3] =
        // 12439,956146789
        Money expectedUnrealizedGains = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12439.96));
        CapitalGainsRecord unRealizedCapitalGains = record.getUnrealizedCapitalGains(CostMethod.FIFO);
        assertThat(unRealizedCapitalGains.getCapitalGains(), is(expectedUnrealizedGains));

        // expected Unrealized Gains for moving average is identical because it
        // is only one buy
        CapitalGainsRecord unRealizedCapitalGainsMovingAvg = record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE);
        assertThat(unRealizedCapitalGainsMovingAvg.getCapitalGains(), is(expectedUnrealizedGains));

    }

    @Test
    public void testFifoBuySellTransactions2()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2013-03-01", Values.Quote.factorize(100)) //
                        .addTo(client);
        new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20)) //
                        .buy(security, "2010-02-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92)) //
                        .sell(security, "2010-03-01", Values.Share.factorize(15), Values.Amount.factorize(531.50)) //
                        .addTo(client);

        var interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2021-01-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);
        LazySecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        // expected Realized Gains for FIFO :
        // 531.5 - 3149.20 * 15/109 = 98,1238532110092
        Money expectedGains = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(98.12));
        CapitalGainsRecord realizedCapitalGains = record.getRealizedCapitalGains(CostMethod.FIFO);
        assertThat(realizedCapitalGains.getCapitalGains(), is(expectedGains));

        // expected Realized Gains for Moving average
        // 531.5 - (3149.20 + 1684.92) * 15/(109 + 52) = 81,116149068323
        Money expectedGainsMovingAvg = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(81.12));
        CapitalGainsRecord realizedCapitalGainsMovingAvg = record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE);
        assertThat(realizedCapitalGainsMovingAvg.getCapitalGains(), is(expectedGainsMovingAvg));

        // expected Unrealized Gains for FIFO :
        // 146 * 100 - [3149,20 + 1684,92 - (3149,20 * 15/109)] =
        // 10199,256146789
        Money expectedUnrealizedGains = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10199.26));
        CapitalGainsRecord unRealizedCapitalGainsFiFO = record.getUnrealizedCapitalGains(CostMethod.FIFO);
        assertThat(unRealizedCapitalGainsFiFO.getCapitalGains(), is(expectedUnrealizedGains));

        // expected Unrealized Gains for Moving average
        // 146 * 100 - (3149.20 + 1684.92) * 146 / (109 + 52) = 10216,2638509317
        Money expectedUnrealizedGainsMovingAvg = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10216.26));
        CapitalGainsRecord unRealizedCapitalGainsMovingAvg = record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE);
        assertThat(unRealizedCapitalGainsMovingAvg.getCapitalGains(), is(expectedUnrealizedGainsMovingAvg));
    }

    /**
     * Test case for the example discussed in
     * https://github.com/portfolio-performance/portfolio/pull/4546
     */
    @Test
    public void testFifoBuySellTransactions3()
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        var security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2025-01-01", Values.Quote.factorize(80)) //
                        .addTo(client);

        var account = new AccountBuilder(CurrencyUnit.EUR) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .inbound_delivery(security, "2024-01-01", Values.Share.factorize(100),
                                        new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4500)),
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(5000)),
                                                        BigDecimal.valueOf(0.90)) //
                        )
                        .inbound_delivery(security, "2024-02-01", Values.Share.factorize(50),
                                        new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2550)),
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(3000)),
                                                        BigDecimal.valueOf(0.85)) //
                        )
                        .outbound_delivery(security, "2024-03-01", Values.Share.factorize(50),
                                        new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3080)),
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(3500)),
                                                        BigDecimal.valueOf(0.88)) //
                        ).addTo(client);

        var interval = Interval.of(LocalDate.parse("2023-12-31"), LocalDate.parse("2024-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);
        LazySecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        // FIFO
        var usingFIFO = record.getRealizedCapitalGains(CostMethod.FIFO);

        // expected realized gains for FIFO
        // [revenue in EUR] - [partial cost of first buy]
        // 3080 - (50/100) * 4500 = 830
        assertThat(usingFIFO.getCapitalGains(), //
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(830))));

        // moving average
        var usingMovingAverage = record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE);

        // expected realized gains
        // [revenue in EUR] - [average costs of first and second buy]
        // 3080 - (50/150) * (4500 + 2550) = 730
        assertThat(usingMovingAverage.getCapitalGains(), //
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(730))));

        // expected forex gains
        // [average costs in USD] * [sale transaction exchange rate] - [average
        // costs in EUR]
        // (50/150) * (5000+3000) * 0.88 - (50/150) * (4500+2550) = -3.33
        assertThat(usingMovingAverage.getForexCaptialGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-3.33))));

        // moving average for unrealized gains
        var realizedUsingMovingAverage = record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE);

        // expected realized gains
        // [current valuation in EUR] - [average costs of first and second buy]
        // exchange rate from the test currency converter is EUR/USD 1.1588
        // (100 * 80 / 1.1588) - (100/150) * (4500+2550) = 2203.69
        assertThat(realizedUsingMovingAverage.getCapitalGains(), //
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2203.69))));

        // expected forex gains
        // [average costs in USD] * [sale transaction exchange rate] - [average
        // costs in EUR]
        // (100/150) * (5000+3000) / 1.1588 - (100/150) * (4500+2550) = -97.5376
        assertThat(realizedUsingMovingAverage.getForexCaptialGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-97.54))));

    }

    @Test
    public void testSpinOffDistributionCapitalGains()
    {
        Client client = new Client();

        // parent: 100 sh @ 5,000 basis; end price 50 -> end value 5,000
        Security parent = new SecurityBuilder() //
                        .addPrice("2010-12-01", Values.Quote.factorize(50)) //
                        .addTo(client);
        // spinco: 100 sh derived basis 1,250; end price 20 -> end value 2,000
        Security spinco = new SecurityBuilder() //
                        .addPrice("2010-12-01", Values.Quote.factorize(20)) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        var exDate = LocalDateTime.parse("2010-06-01T00:00");

        // source leg: 25% of parent basis leaves, shares unchanged
        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(2000));

        // target leg: 100 spinco shares received, basis derived from parent
        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setShares(Values.Share.factorize(100));
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setAmount(Values.Amount.factorize(2000));

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(new BigDecimal("0.25"));
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);

        portfolio.addTransaction(source);
        portfolio.addTransaction(target);

        var interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        LazySecurityPerformanceRecord parentRecord = snapshot.getRecord(parent)
                        .orElseThrow(IllegalArgumentException::new);
        LazySecurityPerformanceRecord spincoRecord = snapshot.getRecord(spinco)
                        .orElseThrow(IllegalArgumentException::new);

        Money zero = Money.of(CurrencyUnit.EUR, 0);

        // the distribution legs themselves produce no realized gain
        assertThat(parentRecord.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(zero));
        assertThat(parentRecord.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(), is(zero));
        assertThat(spincoRecord.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(zero));
        assertThat(spincoRecord.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(), is(zero));

        // parent unrealized gain reflects the reduced basis: 5,000 - 3,750 = 1,250
        Money parentGain = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250));
        assertThat(parentRecord.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(parentGain));
        assertThat(parentRecord.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(parentGain));

        // spinco unrealized gain reflects the derived basis: 2,000 - 1,250 = 750
        Money spincoGain = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750));
        assertThat(spincoRecord.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(spincoGain));
        assertThat(spincoRecord.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(spincoGain));
    }

    /**
     * Deferred item #3: FIFO vs moving-average derived basis at the
     * multi-lot boundary. Two differently-priced parent lots force FIFO
     * (per-lot) and moving average (aggregate) to split the removed basis
     * differently.
     */
    @Test
    public void testSpinOffTwoLotsPerVariantDerivation()
    {
        Client client = new Client();

        Security parent = new SecurityBuilder().addTo(client);
        // seed a spinco end-of-period quote of 120.00 per share
        Security spinco = new SecurityBuilder().addPrice("2010-12-31", Values.Quote.factorize(120)).addTo(client);

        // two lots of 50 shares @ 100.01 -> total basis 200.02 (100 shares)
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
        target.setShares(Values.Share.factorize(1)); // 1 spinco share received
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setAmount(Values.Amount.factorize(50));

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(new BigDecimal("0.5"));
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        portfolio.addTransaction(source);
        portfolio.addTransaction(target);

        var interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        LazySecurityPerformanceRecord spincoRecord = snapshot.getRecord(spinco)
                        .orElseThrow(IllegalArgumentException::new);

        // no realized gain from the receipt itself, either variant
        assertThat(spincoRecord.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(spincoRecord.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, 0)));

        // derived spinco basis: FIFO per-lot 100.02, moving average aggregate 100.01
        assertThat(spincoRecord.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.02))));
        assertThat(spincoRecord.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.01))));

        // unrealized gain = end market value (120.00) - derived basis, per variant
        assertThat(spincoRecord.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.98))));
        assertThat(spincoRecord.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.99))));
    }
}
