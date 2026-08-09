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
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
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

    /**
     * Same portfolio as {@link #testFifoBuySellTransactions3()} but reported in
     * the security's currency (USD) rather than the transaction/account currency
     * (EUR). In that case the FIFO lot basis must be the {@code GROSS_VALUE}
     * unit's forex amount (the rate recorded on the transaction) rather than the
     * historic day rate published by the exchange-rate provider, matching the
     * moving-average sibling and {@code CostCalculation}. See
     * https://github.com/portfolio-performance/portfolio/issues/5908
     */
    @Test
    public void testFifoBuySellTransactions3ReportedInSecurityCurrency()
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
                        new TestCurrencyConverter(CurrencyUnit.USD), interval);
        LazySecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        // realized gains reported in USD
        // [revenue in USD] - [partial cost of first buy in USD]
        // the basis must be the GROSS_VALUE unit's forex amount (USD 5000), not
        // the historic conversion of EUR 4500
        // 3500 - (50/100) * 5000 = 1000
        var realizedFifo = record.getRealizedCapitalGains(CostMethod.FIFO);
        assertThat(realizedFifo.getCapitalGains(), //
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1000))));

        // no currency gains when reporting in the security's own currency
        assertThat(realizedFifo.getForexCaptialGains(), //
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0))));

        // unrealized gains reported in USD
        // [current valuation in USD] - [remaining basis of both buys in USD]
        // 100 * 80 - ((50/100) * 5000 + 3000) = 8000 - 5500 = 2500
        var unrealizedFifo = record.getUnrealizedCapitalGains(CostMethod.FIFO);
        assertThat(unrealizedFifo.getCapitalGains(), //
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2500))));

        assertThat(unrealizedFifo.getForexCaptialGains(), //
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0))));
    }

    /**
     * Safeguards the {@link TaxesAndFees#INCLUDED} cost basis: with a purchase
     * and a sale carrying taxes and fees inside the reporting period, the gains
     * measured against the fee-inclusive basis must differ from the ex-fee
     * basis by exactly those embedded charges, on both the FIFO and the
     * moving-average engine. Crucially the fee-inclusive unrealized gains have
     * to reconcile with
     * {@link LazySecurityPerformanceRecord#getCapitalGainsOnHoldings} - i.e.
     * subtract cleanly from the cost basis reported next to them - which is the
     * mismatch this change fixes.
     */
    @Test
    public void testCapitalGainsAgainstFeeInclusiveBasis()
    {
        var client = new Client();

        Security security = new SecurityBuilder().addPrice("2024-01-01", Values.Quote.factorize(120)) //
                        .addTo(client);

        // buy inside the interval: pays 10100 including 80 fees + 20 taxes, so
        // the ex-fee basis is 10000 (100/share) and the fee-inclusive basis is
        // 10100 (101/share)
        new PortfolioBuilder() //
                        .buy(security, "2024-02-01", Values.Share.factorize(100), Values.Amount.factorize(10100),
                                        Values.Amount.factorize(80), Values.Amount.factorize(20)) //
                        // sell 40 inside the interval: receives 4550 net after
                        // 30 fees + 20 taxes, so the gross proceeds are 4600
                        .sell(security, "2024-03-01", Values.Share.factorize(40), Values.Amount.factorize(4550),
                                        Values.Amount.factorize(30), Values.Amount.factorize(20)) //
                        .addTo(client);

        var interval = Interval.of(LocalDate.parse("2023-12-31"), LocalDate.parse("2024-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);
        LazySecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        // 60 shares remain, valued at 60 * 120 = 7200

        // single buy lot, so FIFO and moving average coincide - both engines
        // are exercised through the loop
        for (var method : new CostMethod[] { CostMethod.FIFO, CostMethod.MOVING_AVERAGE })
        {
            // realized, ex-fee: gross proceeds - relieved gross cost
            // 4600 - (40/100) * 10000 = 600
            var realizedExFee = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(600));
            assertThat(record.getRealizedCapitalGains(method).getCapitalGains(), is(realizedExFee));
            assertThat(record.getRealizedCapitalGains(method, TaxesAndFees.NOT_INCLUDED).getCapitalGains(),
                            is(realizedExFee));

            // realized, fee-inclusive: net proceeds - relieved fee-inclusive
            // cost
            // 4550 - (40/100) * 10100 = 510
            // differs from the ex-fee gains by the sale's 50 charges plus the
            // relieved 40 of the buy's charges
            assertThat(record.getRealizedCapitalGains(method, TaxesAndFees.INCLUDED).getCapitalGains(),
                            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(510))));

            // unrealized, ex-fee: market value - remaining gross cost
            // 7200 - (60/100) * 10000 = 1200
            var unrealizedExFee = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200));
            assertThat(record.getUnrealizedCapitalGains(method).getCapitalGains(), is(unrealizedExFee));
            assertThat(record.getUnrealizedCapitalGains(method, TaxesAndFees.NOT_INCLUDED).getCapitalGains(),
                            is(unrealizedExFee));

            // unrealized, fee-inclusive: market value - remaining fee-inclusive
            // cost
            // 7200 - (60/100) * 10100 = 1140
            var unrealizedInclusive = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1140));
            assertThat(record.getUnrealizedCapitalGains(method, TaxesAndFees.INCLUDED).getCapitalGains(),
                            is(unrealizedInclusive));

            // the reconciliation this change is about: the fee-inclusive
            // unrealized gains subtract cleanly from the fee-inclusive cost
            // reported next to them (market value - cost with charges)
            assertThat(record.getUnrealizedCapitalGains(method, TaxesAndFees.INCLUDED).getCapitalGains(),
                            is(record.getCapitalGainsOnHoldings(method)));
            assertThat(record.getCapitalGainsOnHoldings(method), is(unrealizedInclusive));
        }
    }

    /**
     * Reconciles the two cost bases against the second engine that computes the
     * very same figures: a closed {@link Trade} reports its profit and loss
     * both including the taxes and fees embedded in its trades
     * ({@link Trade#getProfitLoss()}) and without them
     * ({@link Trade#getProfitLossWithoutTaxesAndFees()}), which must be the
     * realized capital gains measured against the fee-inclusive and the ex-fee
     * basis respectively.
     * <p>
     * The comparison only holds under three conditions, hence the scenario
     * below: the trade has to be closed, because {@link Trade} values an open
     * position at today's price instead of at the end of the reporting period;
     * the reporting period has to span the entire history, because the capital
     * gains are scoped to the interval while the trade is not; and the
     * scenario has to be single-currency, because {@link Trade} converts with
     * the historic day rate whereas the capital gains use the exchange rate
     * recorded on the transaction itself.
     */
    @Test
    public void testTradeProfitLossMatchesRealizedCapitalGains() throws TradeCollectorException
    {
        var client = new Client();

        Security security = new SecurityBuilder().addPrice("2024-01-01", Values.Quote.factorize(120)) //
                        .addTo(client);

        // buy: pays 10100 including 80 fees + 20 taxes, so the ex-fee basis is
        // 10000; sell all of it: receives 11400 net after 60 fees + 40 taxes,
        // so the gross proceeds are 11500
        new PortfolioBuilder() //
                        .buy(security, "2024-02-01", Values.Share.factorize(100), Values.Amount.factorize(10100),
                                        Values.Amount.factorize(80), Values.Amount.factorize(20)) //
                        .sell(security, "2024-03-01", Values.Share.factorize(100), Values.Amount.factorize(11400),
                                        Values.Amount.factorize(60), Values.Amount.factorize(40)) //
                        .addTo(client);

        var converter = new TestCurrencyConverter();

        var trades = new TradeCollector(client, converter).collect(security);
        assertThat(trades.size(), is(1));

        Trade trade = trades.get(0);
        assertThat(trade.getEnd().isPresent(), is(true));

        var interval = Interval.of(LocalDate.parse("2023-12-31"), LocalDate.parse("2024-12-31"));
        LazySecurityPerformanceRecord record = LazySecurityPerformanceSnapshot.create(client, converter, interval)
                        .getRecord(security).orElseThrow(IllegalArgumentException::new);

        // 11500 - 10000 = 1500
        var gainsExFee = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500));
        // 11400 - 10100 = 1300, i.e. less by the 200 of charges on both legs
        var gainsInclusive = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1300));

        assertThat(trade.getProfitLossWithoutTaxesAndFees(), is(gainsExFee));
        assertThat(trade.getProfitLoss(), is(gainsInclusive));

        // a single lot bought and sold in one go, so FIFO and moving average
        // arrive at the same figures - both engines are exercised through the
        // loop
        for (var method : new CostMethod[] { CostMethod.FIFO, CostMethod.MOVING_AVERAGE })
        {
            assertThat(record.getRealizedCapitalGains(method, TaxesAndFees.NOT_INCLUDED).getCapitalGains(),
                            is(gainsExFee));
            assertThat(record.getRealizedCapitalGains(method, TaxesAndFees.INCLUDED).getCapitalGains(),
                            is(gainsInclusive));

            // nothing held at the end, hence the trade's profit and loss is
            // realized in full and nothing may leak into the unrealized gains
            assertThat(record.getUnrealizedCapitalGains(method, TaxesAndFees.NOT_INCLUDED).getCapitalGains(),
                            is(Money.of(CurrencyUnit.EUR, 0)));
            assertThat(record.getUnrealizedCapitalGains(method, TaxesAndFees.INCLUDED).getCapitalGains(),
                            is(Money.of(CurrencyUnit.EUR, 0)));
        }

        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED).getCapitalGains(),
                        is(trade.getProfitLossWithoutTaxesAndFees()));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO, TaxesAndFees.INCLUDED).getCapitalGains(),
                        is(trade.getProfitLoss()));

        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED)
                        .getCapitalGains(), is(trade.getProfitLossMovingAverageWithoutTaxesAndFees()));
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED).getCapitalGains(),
                        is(trade.getProfitLossMovingAverage()));
    }
}
