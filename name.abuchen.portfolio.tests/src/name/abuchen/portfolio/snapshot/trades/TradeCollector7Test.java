package name.abuchen.portfolio.snapshot.trades;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.quoteOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.sharesOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.security.SnapshotCache;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

/**
 * Tests for {@link TradeGrouping#PER_LOT}: every acquisition creates its own
 * trade and the closing transaction is split across them.
 */
@SuppressWarnings("nls")
public class TradeCollector7Test
{
    private static List<Trade> collect(Client client, Security security, TradeGrouping grouping)
                    throws TradeCollectorException
    {
        return collect(client, security, grouping, new SnapshotCache());
    }

    private static List<Trade> collect(Client client, Security security, TradeGrouping grouping, SnapshotCache cache)
                    throws TradeCollectorException
    {
        var trades = new TradeCollector(client, new TestCurrencyConverter(), grouping, cache).collect(security);

        // the order of the open trades depends on the iteration order of the
        // portfolios; sort by start date (closed trades first) to make the
        // assertions independent of it
        trades.sort(Comparator.comparing(Trade::getStart).thenComparing(t -> t.isClosed() ? 0 : 1));

        return trades;
    }

    /**
     * Returns the transaction that closed the trade, i.e. the sale of a long
     * position or the covering purchase of a short position. Under
     * {@link TradeGrouping#PER_LOT} this can be a split copy.
     */
    private static PortfolioTransaction closingOf(Trade trade)
    {
        return trade.getTransactions().stream() //
                        .map(TransactionPair::getTransaction) //
                        .filter(t -> t.getType().isPurchase() != trade.isLong()) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);
    }

    private static PortfolioTransaction openingOf(Trade trade)
    {
        return trade.getTransactions().stream() //
                        .map(TransactionPair::getTransaction) //
                        .filter(t -> t.getType().isPurchase() == trade.isLong()) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);
    }

    private static Money sum(List<Trade> trades, Function<Trade, Money> value)
    {
        return trades.stream().map(value).reduce(Money.of(CurrencyUnit.EUR, 0), Money::add);
    }

    /**
     * A cache that counts how often a snapshot is actually created.
     */
    private static class CountingSnapshotCache extends SnapshotCache
    {
        private final AtomicInteger builds = new AtomicInteger();

        @Override
        public LazySecurityPerformanceSnapshot lookup(Client client, PortfolioTransaction closingTransaction,
                        CurrencyConverter converter, Interval interval,
                        Supplier<LazySecurityPerformanceSnapshot> supplier)
        {
            return super.lookup(client, closingTransaction, converter, interval, () -> {
                builds.incrementAndGet();
                return supplier.get();
            });
        }
    }

    // -----------------------------------------------------------------------
    // open positions
    // -----------------------------------------------------------------------

    @Test
    public void testOpenLotsCreateOneTradeEach() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .buy(security, "2021-03-01", sharesOf(10), amountOf(300)) //
                        .addTo(client);

        // combined: all three purchases are folded into one trade

        List<Trade> combined = collect(client, security, TradeGrouping.COMBINED);
        assertThat(combined.size(), is(1));
        assertThat(combined.get(0).getShares(), is(sharesOf(30)));
        assertThat(combined.get(0).getStart(), is(LocalDateTime.parse("2021-01-01T00:00")));

        // per lot: one trade per purchase, each with its own start date, share
        // count, entry value and holding period

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        String[] dates = new String[] { "2021-01-01", "2021-02-01", "2021-03-01" };
        double[] amounts = new double[] { 100, 200, 300 };

        for (int ii = 0; ii < 3; ii++)
        {
            var trade = trades.get(ii);

            assertThat(trade.isClosed(), is(false));
            assertThat(trade.getShares(), is(sharesOf(10)));
            assertThat(trade.getStart(), is(LocalDate.parse(dates[ii]).atStartOfDay()));
            assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(amounts[ii]))));

            // 10 shares at EUR 30
            assertThat(trade.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(300))));

            assertThat(trade.getHoldingPeriod(),
                            is((long) Dates.daysBetween(LocalDate.parse(dates[ii]), LocalDate.now())));
        }

        // the entry values still add up to the combined trade
        assertThat(sum(trades, Trade::getEntryValue), is(combined.get(0).getEntryValue()));
        assertThat(sum(trades, Trade::getExitValue), is(combined.get(0).getExitValue()));
    }

    // -----------------------------------------------------------------------
    // closed positions
    // -----------------------------------------------------------------------

    @Test
    public void testSaleIsSplitAcrossAllConsumedLots() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .buy(security, "2021-03-01", sharesOf(10), amountOf(300)) //
                        .sell(security, "2021-06-01", sharesOf(30), amountOf(900)) //
                        .addTo(client);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        var sale = security.getTransactions(client).stream() //
                        .map(p -> (PortfolioTransaction) p.getTransaction())
                        .filter(t -> t.getType() == PortfolioTransaction.Type.SELL).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        long sharesOfSplitSales = 0;

        for (var trade : trades)
        {
            // the shares of the trade are the shares of *its* lot and not the
            // shares of the full sale
            assertThat(trade.getShares(), is(sharesOf(10)));

            // all trades end with the same (real) sale
            assertThat(trade.getEnd().orElseThrow(IllegalArgumentException::new),
                            is(LocalDateTime.parse("2021-06-01T00:00")));
            assertThat(trade.getRealClosingTransaction().orElseThrow(IllegalArgumentException::new), is(sale));

            // ... but each trade has its own weighted copy of it
            assertThat(closingOf(trade) == sale, is(false));
            assertThat(closingOf(trade).getShares(), is(sharesOf(10)));

            sharesOfSplitSales += closingOf(trade).getShares();

            // 900 / 3 = 300 proceeds per lot
            assertThat(trade.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(300))));
        }

        // the split sales add up to the original sale
        assertThat(sharesOfSplitSales, is(sale.getShares()));

        assertThat(trades.get(0).getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
        assertThat(trades.get(1).getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(trades.get(2).getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(0))));
    }

    @Test
    public void testPartiallyConsumedLot() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .sell(security, "2021-06-01", sharesOf(15), amountOf(600)) //
                        .addTo(client);

        // combined: one closed trade over both lots plus the remaining lot

        var combined = collect(client, security, TradeGrouping.COMBINED);
        assertThat(combined.size(), is(2));
        assertThat(combined.get(0).getShares(), is(sharesOf(15)));
        assertThat(combined.get(1).getShares(), is(sharesOf(5)));

        // per lot: two closed trades (10 and 5 shares) plus the residual lot

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        var first = trades.get(0);
        assertThat(first.isClosed(), is(true));
        assertThat(first.getShares(), is(sharesOf(10)));
        assertThat(first.getStart(), is(LocalDateTime.parse("2021-01-01T00:00")));
        assertThat(first.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        // 600 x 10/15
        assertThat(first.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(400))));

        var second = trades.get(1);
        assertThat(second.isClosed(), is(true));
        assertThat(second.getShares(), is(sharesOf(5)));
        assertThat(second.getStart(), is(LocalDateTime.parse("2021-02-01T00:00")));
        // the second lot is consumed by half
        assertThat(second.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        // 600 x 5/15
        assertThat(second.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));

        var open = trades.get(2);
        assertThat(open.isClosed(), is(false));
        assertThat(open.getShares(), is(sharesOf(5)));
        // the residual lot keeps the date of the original purchase
        assertThat(open.getStart(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(open.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
    }

    @Test
    public void testDeliveriesAsOpeningAndClosingTransactions() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addTo(client);

        // an inbound delivery and a purchase of the same security: the setting
        // applies to acquisitions in general and not only to on-market
        // purchases. Both are closed by an outbound delivery, which has no
        // cross entry and therefore takes the other branch of #split

        new PortfolioBuilder(new Account("one")) //
                        .inbound_delivery(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .outbound_delivery(security, "2021-06-01", sharesOf(20), amountOf(600), 0, 0) //
                        .addTo(client);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(2));

        var first = trades.get(0);
        assertThat(openingOf(first).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(first.getShares(), is(sharesOf(10)));
        assertThat(first.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(first.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(300))));

        var second = trades.get(1);
        assertThat(openingOf(second).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(second.getShares(), is(sharesOf(10)));
        assertThat(second.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
        assertThat(second.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(300))));

        // the split copies of the outbound delivery have no cross entry
        for (var trade : trades)
        {
            assertThat(closingOf(trade).getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
            assertThat(closingOf(trade).getCrossEntry(), is(nullValue()));
            assertThat(closingOf(trade).getShares(), is(sharesOf(10)));
        }
    }

    @Test
    public void testSingleLotSaleKeepsTheRealClosingTransaction() throws TradeCollectorException
    {
        for (var grouping : TradeGrouping.values())
        {
            var client = new Client();

            var security = new SecurityBuilder().addTo(client);

            new PortfolioBuilder(new Account("one")) //
                            .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                            .sell(security, "2021-06-01", sharesOf(10), amountOf(200)) //
                            .addTo(client);

            var sale = security.getTransactions(client).stream() //
                            .map(p -> (PortfolioTransaction) p.getTransaction())
                            .filter(t -> t.getType() == PortfolioTransaction.Type.SELL).findAny()
                            .orElseThrow(IllegalArgumentException::new);

            var trades = collect(client, security, grouping);
            assertThat(grouping.name(), trades.size(), is(1));

            // if a sale is matched against exactly one lot, the weight is 1 and
            // no copy is created at all
            assertThat(grouping.name(), closingOf(trades.get(0)) == sale, is(true));
            assertThat(grouping.name(),
                            trades.get(0).getRealClosingTransaction().orElseThrow(IllegalArgumentException::new),
                            is(sale));
        }
    }

    // -----------------------------------------------------------------------
    // conservation of the split sale
    // -----------------------------------------------------------------------

    /**
     * Creates a client with three lots of 10 shares that are sold with one sale
     * of 30 shares. The sale carries fees, taxes and a gross value in a foreign
     * currency, and its amounts are deliberately not divisible by three.
     */
    private static Client createClientWithSaleSpanningThreeLots()
    {
        var client = new Client();

        var security = new SecurityBuilder(CurrencyUnit.USD).addTo(client);

        var portfolio = new PortfolioBuilder(new Account("one")) //
                        .inbound_delivery(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .inbound_delivery(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .inbound_delivery(security, "2021-03-01", sharesOf(10), amountOf(300)) //
                        .addTo(client);

        // gross value EUR 1,050.05 (USD 1,312.56 at USD/EUR 0.80) less EUR
        // 30.03 fees and EUR 20.01 taxes = EUR 1,000.01 proceeds
        var sale = new PortfolioTransaction(LocalDateTime.parse("2021-06-01T00:00"), CurrencyUnit.EUR,
                        amountOf(1000.01), security, sharesOf(30), PortfolioTransaction.Type.DELIVERY_OUTBOUND, 0, 0);
        sale.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(CurrencyUnit.EUR, amountOf(1050.05)), //
                        Money.of(CurrencyUnit.USD, amountOf(1312.56)), //
                        BigDecimal.valueOf(0.80)));
        sale.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, amountOf(30.03))));
        sale.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, amountOf(20.01))));
        portfolio.addTransaction(sale);

        return client;
    }

    @Test
    public void testSplitSaleConservesSharesAmountAndUnits() throws TradeCollectorException
    {
        var client = createClientWithSaleSpanningThreeLots();
        var security = client.getSecurities().get(0);

        var sale = security.getTransactions(client).stream() //
                        .map(p -> (PortfolioTransaction) p.getTransaction())
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        // shares are exact because split weights are share counts
        long shares = trades.stream().mapToLong(t -> closingOf(t).getShares()).sum();
        assertThat(shares, is(sale.getShares()));

        // EUR 1,000.01 is apportioned as 333.34, 333.34 and 333.33
        long amount = trades.stream().mapToLong(t -> closingOf(t).getAmount()).sum();
        assertThat(amount, is(sale.getAmount()));

        // each unit type reconciles exactly
        for (Unit.Type type : Unit.Type.values())
        {
            var original = sale.getUnitSum(type);

            long units = trades.stream() //
                            .mapToLong(t -> closingOf(t).getUnitSum(type).getAmount()).sum();
            assertThat(type + " amount", units, is(original.getAmount()));
        }

        // forex is apportioned separately and also reconciles
        long forex = trades.stream() //
                        .mapToLong(t -> closingOf(t).getUnit(Unit.Type.GROSS_VALUE)
                                        .orElseThrow(IllegalArgumentException::new).getForex().getAmount())
                        .sum();
        long forexOfSale = sale.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new).getForex()
                        .getAmount();
        assertThat(forex, is(forexOfSale));

        // the exchange rate is carried over unchanged
        for (Trade trade : trades)
        {
            assertThat(closingOf(trade).getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new)
                            .getExchangeRate(), is(BigDecimal.valueOf(0.80)));
        }
    }

    /**
     * Purchase fixture with amounts that do not divide evenly into the later
     * instalments.
     */
    private static PortfolioTransaction createPurchaseOfHundredShares(Security security)
    {
        var purchase = new PortfolioTransaction(LocalDateTime.parse("2021-01-01T00:00"), CurrencyUnit.EUR,
                        amountOf(1000.01), security, sharesOf(100), PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0);

        // EUR 970.01 gross value plus EUR 30.00 fees = EUR 1,000.01 paid
        purchase.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(CurrencyUnit.EUR, amountOf(970.01)), //
                        Money.of(CurrencyUnit.USD, amountOf(1212.51)), //
                        BigDecimal.valueOf(0.80)));
        purchase.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, amountOf(30))));

        return purchase;
    }

    /**
     * Asserts exact conservation of shares, amount, units and gross-value
     * forex.
     */
    private static void assertConserved(PortfolioTransaction original, List<PortfolioTransaction> pieces)
    {
        assertThat("shares", pieces.stream().mapToLong(PortfolioTransaction::getShares).sum(),
                        is(original.getShares()));
        assertThat("amount", pieces.stream().mapToLong(PortfolioTransaction::getAmount).sum(),
                        is(original.getAmount()));

        for (Unit.Type type : Unit.Type.values())
        {
            assertThat(type + " amount", pieces.stream().mapToLong(p -> p.getUnitSum(type).getAmount()).sum(),
                            is(original.getUnitSum(type).getAmount()));
        }

        assertThat("forex of gross value",
                        pieces.stream().mapToLong(p -> p.getUnit(Unit.Type.GROSS_VALUE)
                                        .orElseThrow(IllegalArgumentException::new).getForex().getAmount()).sum(),
                        is(original.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new)
                                        .getForex().getAmount()));
    }

    @Test
    public void testRepeatedPartialSalesConserveTheLot() throws TradeCollectorException
    {
        // repeated partial sales split the synthetic remainder again, so
        // rounding drift used to compound

        var client = new Client();

        var security = new SecurityBuilder(CurrencyUnit.USD).addPrice("2021-12-01", quoteOf(30)).addTo(client);

        var portfolio = new PortfolioBuilder(new Account("one")) //
                        .outbound_delivery(security, "2021-02-01", sharesOf(7), amountOf(100.01), 0, 0) //
                        .outbound_delivery(security, "2021-03-01", sharesOf(7), amountOf(101.03), 0, 0) //
                        .outbound_delivery(security, "2021-04-01", sharesOf(7), amountOf(102.07), 0, 0) //
                        .outbound_delivery(security, "2021-05-01", sharesOf(7), amountOf(103.11), 0, 0) //
                        .outbound_delivery(security, "2021-06-01", sharesOf(7), amountOf(104.13), 0, 0) //
                        .addTo(client);

        var purchase = createPurchaseOfHundredShares(security);
        portfolio.addTransaction(purchase);

        // createNewTrades runs for both groupings
        for (TradeGrouping grouping : TradeGrouping.values())
        {
            var trades = collect(client, security, grouping);

            // five closed trades plus the 65-share remainder
            assertThat(grouping.name(), trades.size(), is(6));

            assertConserved(purchase, trades.stream().map(TradeCollector7Test::openingOf).toList());
        }
    }

    // -----------------------------------------------------------------------
    // ordering of transactions on the same day
    // -----------------------------------------------------------------------

    @Test
    public void testSameDayLotIsAvailableButNotPreferred() throws TradeCollectorException
    {
        // with an older lot present, the sale consumes the *older* lot: the
        // ordering by type makes the same-day lot available, it does not make
        // it preferred over an earlier one

        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .sell(security, "2021-06-01", sharesOf(10), amountOf(400)) //
                        .buy(security, "2021-06-01", sharesOf(10), amountOf(200)) //
                        .addTo(client);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(2));

        var closed = trades.get(0);
        assertThat(closed.isClosed(), is(true));
        assertThat(closed.getStart(), is(LocalDateTime.parse("2021-01-01T00:00")));
        assertThat(closed.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));

        var open = trades.get(1);
        assertThat(open.isClosed(), is(false));
        assertThat(open.getStart(), is(LocalDateTime.parse("2021-06-01T00:00")));
        assertThat(open.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
    }

    @Test
    public void testSameDayLotIsAvailableForSameDaySale() throws TradeCollectorException
    {
        // without an older lot, the sale consumes the same-day lot. Without the
        // ordering by type, the sale would not find any holdings at all

        for (var grouping : TradeGrouping.values())
        {
            var client = new Client();

            var security = new SecurityBuilder().addTo(client);

            new PortfolioBuilder(new Account("one")) //
                            .sell(security, "2021-06-01", sharesOf(10), amountOf(200)) //
                            .buy(security, "2021-06-01", sharesOf(10), amountOf(100)) //
                            .addTo(client);

            var trades = collect(client, security, grouping);

            assertThat(grouping.name(), trades.size(), is(1));
            assertThat(grouping.name(), trades.get(0).isClosed(), is(true));
            assertThat(grouping.name(), trades.get(0).isLong(), is(true));
            assertThat(grouping.name(), trades.get(0).getHoldingPeriod(), is(0L));
            assertThat(grouping.name(), trades.get(0).getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        }
    }

    @Test
    public void testSameDayTimeIsIgnoredIfOnlyOneTransactionHasATime() throws TradeCollectorException
    {
        // the guard is hasTime1 && hasTime2: because the sale has no time, the
        // time of the purchase is ignored entirely and the ordering by type
        // applies - which is why the sale finds the purchase even though the
        // purchase is later in the day

        for (var grouping : TradeGrouping.values())
        {
            var client = new Client();

            var security = new SecurityBuilder().addTo(client);

            new PortfolioBuilder(new Account("one")) //
                            .buy(security, "2021-06-01T10:00", sharesOf(10), amountOf(100)) //
                            .sell(security, "2021-06-01", sharesOf(10), amountOf(200)) //
                            .addTo(client);

            var trades = collect(client, security, grouping);

            assertThat(grouping.name(), trades.size(), is(1));

            Trade trade = trades.get(0);
            assertThat(grouping.name(), trade.isClosed(), is(true));
            assertThat(grouping.name(), trade.getHoldingPeriod(), is(0L));

            // entry and exit values are calculated inside Trade#calculate,
            // which reads isLong() *before* sorting the transactions, i.e. in
            // the order in which the collector added them: the lot first
            assertThat(grouping.name(), trade.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
            assertThat(grouping.name(), trade.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));

            // Attention: this pins a defect rather than desired behaviour.
            // isLong() reads the first transaction, and #calculate re-sorts the
            // transactions by Transaction.BY_DATE, which compares the full date
            // *and time* (and then the amount). Here the sale has no time and
            // therefore sorts before the purchase of the same day, so the trade
            // reports itself as short afterwards and the sign of the profit is
            // inverted. The defect is independent of the grouping, hence the
            // assertion for both
            assertThat(grouping.name(), trade.isLong(), is(false));
            assertThat(grouping.name(), trade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(-100))));
        }
    }

    @Test
    public void testSameDayLotsWithTimeAreSortedByTime() throws TradeCollectorException
    {
        // among lots of the same day, the earlier time is consumed first - but
        // a lot of an earlier day precedes both

        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-05-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-06-01T14:00", sharesOf(10), amountOf(200)) //
                        .buy(security, "2021-06-01T09:00", sharesOf(10), amountOf(300)) //
                        .sell(security, "2021-07-01", sharesOf(20), amountOf(1000)) //
                        .addTo(client);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        // the lot of the earlier day is consumed first ...
        assertThat(trades.get(0).getStart(), is(LocalDateTime.parse("2021-05-01T00:00")));
        assertThat(trades.get(0).isClosed(), is(true));

        // ... then the same-day lot with the earlier time ...
        assertThat(trades.get(1).getStart(), is(LocalDateTime.parse("2021-06-01T09:00")));
        assertThat(trades.get(1).isClosed(), is(true));

        // ... and the lot with the later time stays open
        assertThat(trades.get(2).getStart(), is(LocalDateTime.parse("2021-06-01T14:00")));
        assertThat(trades.get(2).isClosed(), is(false));
        assertThat(trades.get(2).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
    }

    @Test
    public void testSameDayLotsOfSameTypeKeepInputOrder() throws TradeCollectorException
    {
        // two lots of the same type on the same day compare equal, so the
        // outcome falls back to the order in which the transactions are
        // returned by security.getTransactions(client). Should that order ever
        // change, this test fails rather than silently reshuffling the trades
        // of the users

        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-06-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-06-01", sharesOf(10), amountOf(200)) //
                        .sell(security, "2021-07-01", sharesOf(10), amountOf(400)) //
                        .addTo(client);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(2));

        var closed = trades.stream().filter(Trade::isClosed).findAny().orElseThrow(IllegalArgumentException::new);
        var open = trades.stream().filter(t -> !t.isClosed()).findAny().orElseThrow(IllegalArgumentException::new);

        // the lot added first is consumed first
        assertThat(closed.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(open.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
    }

    @Test
    public void testBuyTransferAndSellOnOneDay() throws TradeCollectorException
    {
        // exercises all three sort orders: inbound (1) before transfer (2)
        // before outbound (3)

        var client = new Client();

        var security = new SecurityBuilder().addTo(client);

        var portfolioA = new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-06-01", sharesOf(10), amountOf(100)) //
                        .addTo(client);

        var portfolioB = new PortfolioBuilder(new Account("two")) //
                        .sell(security, "2021-06-01", sharesOf(10), amountOf(200)) //
                        .addTo(client);

        var transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-06-01T00:00"));
        transfer.setShares(sharesOf(10));
        transfer.setAmount(amountOf(100));
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        var trades = collect(client, security, TradeGrouping.PER_LOT);

        assertThat(trades.size(), is(1));
        assertThat(trades.get(0).isClosed(), is(true));
        assertThat(trades.get(0).getPortfolio(), is(portfolioB));
        assertThat(trades.get(0).getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
    }

    // -----------------------------------------------------------------------
    // short positions
    // -----------------------------------------------------------------------

    /**
     * Creates an example with two opening sells that are covered by one
     * purchase.
     */
    private static Client createClientWithShortPosition()
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2026-07-01", quoteOf(80)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .sell(security, "2026-01-01", sharesOf(10), amountOf(1000)) //
                        .sell(security, "2026-03-01", sharesOf(10), amountOf(900)) //
                        .buy(security, "2026-07-01", sharesOf(20), amountOf(1600)) //
                        .addTo(client);

        return client;
    }

    @Test
    public void testShortPositionIsSplitPerOpeningSell() throws TradeCollectorException
    {
        var client = createClientWithShortPosition();
        var security = client.getSecurities().get(0);

        var combined = collect(client, security, TradeGrouping.COMBINED);
        assertThat(combined.size(), is(1));
        assertThat(combined.get(0).isLong(), is(false));
        assertThat(combined.get(0).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(1900))));
        assertThat(combined.get(0).getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(1600))));
        assertThat(combined.get(0).getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(300))));
        // (10 x 181 + 10 x 122) / 20 = 151.5
        assertThat(combined.get(0).getHoldingPeriod(), is(152L));

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(2));

        var tradeA = trades.get(0);
        assertThat(tradeA.isLong(), is(false));
        assertThat(tradeA.getShares(), is(sharesOf(10)));
        assertThat(tradeA.getStart(), is(LocalDateTime.parse("2026-01-01T00:00")));
        assertThat(tradeA.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2026-07-01T00:00")));
        assertThat(tradeA.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(1000))));
        assertThat(tradeA.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(800))));
        assertThat(tradeA.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
        assertThat(tradeA.getHoldingPeriod(), is(181L));

        var tradeB = trades.get(1);
        assertThat(tradeB.isLong(), is(false));
        assertThat(tradeB.getShares(), is(sharesOf(10)));
        assertThat(tradeB.getStart(), is(LocalDateTime.parse("2026-03-01T00:00")));
        assertThat(tradeB.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(900))));
        assertThat(tradeB.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(800))));
        assertThat(tradeB.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(tradeB.getHoldingPeriod(), is(122L));

        // the entry and exit values (and hence the profit) still reconcile with
        // the combined trade
        assertThat(sum(trades, Trade::getEntryValue), is(combined.get(0).getEntryValue()));
        assertThat(sum(trades, Trade::getExitValue), is(combined.get(0).getExitValue()));
        assertThat(sum(trades, Trade::getProfitLoss), is(combined.get(0).getProfitLoss()));

        // the split cover must release the collateral of its trade in full:
        // its share count has to match the shares it consumed. Otherwise
        // releaseCollateral takes the fractional branch, which changes the IRR
        // while every profit/loss assertion still passes
        for (var trade : trades)
            assertThat(closingOf(trade).getShares(), is(trade.getShares()));

        // therefore the cash flows are the collateral of the trade itself:
        // the opening sell (negated), the released collateral less the cost of
        // the cover, and the terminal collateral

        assertEquals(IRR.calculate( //
                        List.of(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-07-01"),
                                        LocalDate.parse("2026-07-01")),
                        List.of(-1000d, 200d, 1000d)), tradeA.getIRR(), 0.000001);

        assertEquals(IRR.calculate( //
                        List.of(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-07-01"),
                                        LocalDate.parse("2026-07-01")),
                        List.of(-900d, 100d, 900d)), tradeB.getIRR(), 0.000001);
    }

    @Test
    public void testShortPositionWithPartialCover() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2026-07-01", quoteOf(80)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .sell(security, "2026-01-01", sharesOf(10), amountOf(1000)) //
                        .sell(security, "2026-03-01", sharesOf(10), amountOf(900)) //
                        .buy(security, "2026-07-01", sharesOf(15), amountOf(1200)) //
                        .addTo(client);

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        var tradeA = trades.get(0);
        assertThat(tradeA.isClosed(), is(true));
        assertThat(tradeA.getShares(), is(sharesOf(10)));
        assertThat(tradeA.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(1000))));
        // 1200 x 10/15
        assertThat(tradeA.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(800))));
        assertThat(tradeA.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));

        var tradeB = trades.get(1);
        assertThat(tradeB.isClosed(), is(true));
        assertThat(tradeB.getShares(), is(sharesOf(5)));
        // half of the second opening sell
        assertThat(tradeB.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(450))));
        // 1200 x 5/15
        assertThat(tradeB.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(400))));
        assertThat(tradeB.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(50))));

        var open = trades.get(2);
        assertThat(open.isClosed(), is(false));
        assertThat(open.isLong(), is(false));
        assertThat(open.getShares(), is(sharesOf(5)));
        assertThat(open.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(450))));
        // 5 shares at EUR 80
        assertThat(open.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(400))));

        // the shares of both split covers add up to the covering purchase, and
        // each one covers exactly its own trade
        assertThat(closingOf(tradeA).getShares(), is(sharesOf(10)));
        assertThat(closingOf(tradeB).getShares(), is(sharesOf(5)));
    }

    @Test
    public void testMovingAverageCostOfShortTrades() throws TradeCollectorException
    {
        // "Moving average acquisition cost" has no meaningful reading for a
        // position that was never acquired; the point of this test is that the
        // value is a number and not null. TradeCategory sums
        // getProfitLossMovingAverage() without a null check, so a null here is
        // an NPE in the totals row rather than a meaningless number.
        //
        // The value depends on the trade knowing its real closing transaction:
        // it cannot be derived from the transaction type, because the *opening*
        // transactions of a short position are liquidations as well.

        var client = createClientWithShortPosition();
        var security = client.getSecurities().get(0);

        for (var grouping : TradeGrouping.values())
        {
            for (var trade : collect(client, security, grouping))
            {
                assertThat(grouping.name(), trade.getEntryValueMovingAverage(), is(notNullValue()));
                assertThat(grouping.name(), trade.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, 0)));
                assertThat(grouping.name(), trade.getProfitLossMovingAverage(), is(notNullValue()));
            }
        }

        // ... and the same for an open short position

        var openClient = new Client();
        var openSecurity = new SecurityBuilder().addPrice("2026-07-01", quoteOf(80)).addTo(openClient);
        new PortfolioBuilder(new Account("one")) //
                        .sell(openSecurity, "2026-01-01", sharesOf(10), amountOf(1000)) //
                        .addTo(openClient);

        for (var grouping : TradeGrouping.values())
        {
            var trades = collect(openClient, openSecurity, grouping);
            assertThat(grouping.name(), trades.size(), is(1));
            assertThat(grouping.name(), trades.get(0).getEntryValueMovingAverage(), is(notNullValue()));
            assertThat(grouping.name(), trades.get(0).getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, 0)));
        }
    }

    // -----------------------------------------------------------------------
    // transfers
    // -----------------------------------------------------------------------

    @Test
    public void testWholeLotTransferIsAttributedToTheReceivingPortfolio() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        var portfolioA = new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .addTo(client);

        var portfolioB = new PortfolioBuilder(new Account("two")).addTo(client);

        var transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-03-01T00:00"));
        transfer.setShares(sharesOf(20));
        transfer.setAmount(amountOf(300));
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(2));

        for (var trade : trades)
        {
            // the trade belongs to the portfolio that holds the position ...
            assertThat(trade.getPortfolio(), is(portfolioB));

            // ... even though the transaction pair reports the portfolio the
            // lot has been transferred away from: the pair is moved between the
            // open lists unchanged. Pinned so that the inconsistency between
            // the two is documented rather than left to drift
            assertThat(trade.getTransactions().get(0).getOwner(), is((Object) portfolioA));
        }

        assertThat(trades.get(0).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(trades.get(1).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
    }

    @Test
    public void testPartialTransferOfBuySellEntry() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        var portfolioA = new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .addTo(client);

        var portfolioB = new PortfolioBuilder(new Account("two")).addTo(client);

        // transferring 15 shares moves the first lot as a whole and splits the
        // second one
        var transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-03-01T00:00"));
        transfer.setShares(sharesOf(15));
        transfer.setAmount(amountOf(250));
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        var receiving = new ArrayList<Trade>();
        var remaining = new ArrayList<Trade>();
        trades.forEach(t -> (t.getPortfolio() == portfolioB ? receiving : remaining).add(t));

        assertThat(receiving.size(), is(2));
        assertThat(remaining.size(), is(1));

        // the whole lot and half of the second lot are attributed to the
        // receiving portfolio
        assertThat(receiving.get(0).getShares(), is(sharesOf(10)));
        assertThat(receiving.get(0).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(receiving.get(1).getShares(), is(sharesOf(5)));
        assertThat(receiving.get(1).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));

        assertThat(remaining.get(0).getShares(), is(sharesOf(5)));
        assertThat(remaining.get(0).getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100))));
        assertThat(remaining.get(0).getPortfolio(), is(portfolioA));

        // splitBuySell sets the portfolio of the copied entry to the receiving
        // portfolio but wraps it in a pair that reports the original one, so
        // *all* pairs report the portfolio the lots were purchased in
        for (var trade : trades)
            assertThat(trade.getTransactions().get(0).getOwner(), is((Object) portfolioA));
    }

    @Test
    public void testPartialTransferWithoutCrossEntry() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        // an inbound delivery has no BuySellEntry, hence splitting it takes the
        // other branch of #split
        var portfolioA = new PortfolioBuilder(new Account("one")) //
                        .inbound_delivery(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .inbound_delivery(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .addTo(client);

        var portfolioB = new PortfolioBuilder(new Account("two")).addTo(client);

        var transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-03-01T00:00"));
        transfer.setShares(sharesOf(15));
        transfer.setAmount(amountOf(250));
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(3));

        assertThat(trades.stream().filter(t -> t.getPortfolio() == portfolioB).count(), is(2L));
        assertThat(trades.stream().filter(t -> t.getPortfolio() == portfolioA).count(), is(1L));

        // splitPortfolioTransaction ignores the new owner and uses the owner of
        // the candidate, so again all pairs report the original portfolio
        for (var trade : trades)
            assertThat(trade.getTransactions().get(0).getOwner(), is((Object) portfolioA));
    }

    @Test
    public void testPartialTransferConservesTheLot() throws TradeCollectorException
    {
        // partial transfers must conserve transferred and remaining pieces

        var client = new Client();

        var security = new SecurityBuilder(CurrencyUnit.USD).addPrice("2021-12-01", quoteOf(30)).addTo(client);

        var portfolioA = new PortfolioBuilder(new Account("one")).addTo(client);
        var portfolioB = new PortfolioBuilder(new Account("two")).addTo(client);

        var purchase = createPurchaseOfHundredShares(security);
        portfolioA.addTransaction(purchase);

        var transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-03-01T00:00"));
        transfer.setShares(sharesOf(33));
        transfer.setAmount(amountOf(330));
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        var trades = collect(client, security, TradeGrouping.PER_LOT);
        assertThat(trades.size(), is(2));

        var transferred = openingOf(trades.stream().filter(t -> t.getPortfolio() == portfolioB).findAny()
                        .orElseThrow(IllegalArgumentException::new));
        var remaining = openingOf(trades.stream().filter(t -> t.getPortfolio() == portfolioA).findAny()
                        .orElseThrow(IllegalArgumentException::new));

        assertConserved(purchase, List.of(transferred, remaining));

        // 330.0033 / 670.0067 rounds the extra cent into the larger remainder
        assertThat(transferred.getShares(), is(sharesOf(33)));
        assertThat(transferred.getAmount(), is(amountOf(330)));
        assertThat(remaining.getShares(), is(sharesOf(67)));
        assertThat(remaining.getAmount(), is(amountOf(670.01)));
    }

    // -----------------------------------------------------------------------
    // relationship between the two groupings
    // -----------------------------------------------------------------------

    @Test
    public void testTotalsAreIdenticalIfNothingIsSplit() throws TradeCollectorException
    {
        // if every sale is matched against exactly one whole lot, no
        // transaction is split at all and no rounding is introduced: the
        // ungrouped totals must be bit-identical

        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-06-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100.01)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200.03)) //
                        .buy(security, "2021-03-01", sharesOf(10), amountOf(300.07)) //
                        .sell(security, "2021-04-01", sharesOf(10), amountOf(150.11)) //
                        .sell(security, "2021-05-01", sharesOf(10), amountOf(250.13)) //
                        .addTo(client);

        var combined = collect(client, security, TradeGrouping.COMBINED);
        var perLot = collect(client, security, TradeGrouping.PER_LOT);

        assertThat(combined.size(), is(3));
        assertThat(perLot.size(), is(3));

        assertThat(sum(perLot, Trade::getEntryValue), is(sum(combined, Trade::getEntryValue)));
        assertThat(sum(perLot, Trade::getExitValue), is(sum(combined, Trade::getExitValue)));
        assertThat(sum(perLot, Trade::getProfitLoss), is(sum(combined, Trade::getProfitLoss)));
        assertThat(sum(perLot, Trade::getProfitLossWithoutTaxesAndFees),
                        is(sum(combined, Trade::getProfitLossWithoutTaxesAndFees)));
    }

    @Test
    public void testTotalsAreIdenticalIfSalesAreSplit() throws TradeCollectorException
    {
        // the fixture avoids conversion rounding, so split and combined totals
        // match

        var client = createClientWithSaleSpanningThreeLots();
        var security = client.getSecurities().get(0);

        var combined = collect(client, security, TradeGrouping.COMBINED);
        var perLot = collect(client, security, TradeGrouping.PER_LOT);

        assertThat(combined.size(), is(1));
        assertThat(perLot.size(), is(3));

        assertThat(sum(perLot, Trade::getEntryValue), is(sum(combined, Trade::getEntryValue)));
        assertThat(sum(perLot, Trade::getExitValue), is(sum(combined, Trade::getExitValue)));
        assertThat(sum(perLot, Trade::getProfitLoss), is(sum(combined, Trade::getProfitLoss)));
    }

    // -----------------------------------------------------------------------
    // the closing transaction and the snapshot cache
    // -----------------------------------------------------------------------

    @Test
    public void testMovingAverageCostUsesTheRealClosingTransaction() throws TradeCollectorException
    {
        // the moving average cost is calculated on a client that is truncated
        // immediately before the closing transaction. The
        // ClientTransactionFilter identifies that transaction by reference, so
        // handing it a split copy - which exists nowhere in the client - would
        // silently skip the truncation: the sale itself (and everything after
        // it) would then be part of the snapshot and the cost would be wrong
        // without any error being raised

        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-08-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .buy(security, "2021-03-01", sharesOf(10), amountOf(300)) //
                        .sell(security, "2021-06-01", sharesOf(30), amountOf(900)) //
                        // a transaction after the sale which must not leak into
                        // the snapshot
                        .buy(security, "2021-07-01", sharesOf(10), amountOf(999)) //
                        .addTo(client);

        var combined = collect(client, security, TradeGrouping.COMBINED);
        var combinedClosed = combined.stream().filter(Trade::isClosed).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // moving average cost of all 30 shares held before the sale
        assertThat(combinedClosed.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(600))));

        var perLot = collect(client, security, TradeGrouping.PER_LOT);
        var closed = perLot.stream().filter(Trade::isClosed).toList();
        assertThat(closed.size(), is(3));

        for (Trade trade : closed)
        {
            // 10 of 30 shares of EUR 600
            assertThat(trade.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(200))));
        }

        assertThat(sum(closed, Trade::getEntryValueMovingAverage), is(combinedClosed.getEntryValueMovingAverage()));
    }

    @Test
    public void testSnapshotIsSharedByAllTradesOfOneSale() throws TradeCollectorException
    {
        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-08-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .buy(security, "2021-03-01", sharesOf(10), amountOf(300)) //
                        .sell(security, "2021-06-01", sharesOf(30), amountOf(900)) //
                        .addTo(client);

        var cache = new CountingSnapshotCache();

        var trades = collect(client, security, TradeGrouping.PER_LOT, cache);
        assertThat(trades.size(), is(3));

        // the snapshots are created lazily
        assertThat(cache.builds.get(), is(0));

        trades.forEach(t -> {
            t.getEntryValueMovingAverage();
            t.getProfitLossMovingAverageWithoutTaxesAndFees();
        });

        // keyed on the *real* closing transaction, all three trades share one
        // snapshot. Keyed on their split copies, this would silently be three
        assertThat(cache.builds.get(), is(1));
    }

    @Test
    public void testSnapshotOfOpenTradesIsSharedAcrossSecurities() throws TradeCollectorException
    {
        // the collectors are created per security - as they are in the trades
        // view and in the dashboard widgets - and share one cache. For open
        // trades the snapshot is created from the unfiltered client, so one
        // snapshot serves every open lot of every security

        var client = new Client();

        var first = new SecurityBuilder().addPrice("2021-08-01", quoteOf(30)).addTo(client);
        var second = new SecurityBuilder().addPrice("2021-08-01", quoteOf(40)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(first, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(first, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .buy(second, "2021-01-01", sharesOf(10), amountOf(300)) //
                        .buy(second, "2021-02-01", sharesOf(10), amountOf(400)) //
                        .addTo(client);

        var cache = new CountingSnapshotCache();

        var trades = new ArrayList<Trade>();
        trades.addAll(collect(client, first, TradeGrouping.PER_LOT, cache));
        trades.addAll(collect(client, second, TradeGrouping.PER_LOT, cache));

        assertThat(trades.size(), is(4));

        trades.forEach(Trade::getEntryValueMovingAverage);

        assertThat(cache.builds.get(), is(1));
    }

    // -----------------------------------------------------------------------
    // the persisted grouping
    // -----------------------------------------------------------------------

    @Test
    public void testTradeGroupingIsReadTolerantly()
    {
        // the grouping is persisted by name, so parsing it must never throw:
        // the views only pass the stored string through #fromString

        assertThat(TradeGrouping.fromString(TradeGrouping.PER_LOT.name()), is(TradeGrouping.PER_LOT));
        assertThat(TradeGrouping.fromString(TradeGrouping.COMBINED.name()), is(TradeGrouping.COMBINED));

        // a value stored by a newer version of the application (or no value at
        // all) must fall back to the default instead of throwing
        assertThat(TradeGrouping.fromString("CRADLE_TO_GRAVE"), is(TradeGrouping.COMBINED));
        assertThat(TradeGrouping.fromString(""), is(TradeGrouping.COMBINED));
        assertThat(TradeGrouping.fromString(null), is(TradeGrouping.COMBINED));
    }

    @Test
    public void testValuesAreNotAffectedByTheSnapshotCache() throws TradeCollectorException
    {
        // a memoization bug produces correct numbers and merely stops saving
        // anything - so assert that sharing a cache across securities does not
        // change any value either

        var client = new Client();

        var security = new SecurityBuilder().addPrice("2021-08-01", quoteOf(30)).addTo(client);

        new PortfolioBuilder(new Account("one")) //
                        .buy(security, "2021-01-01", sharesOf(10), amountOf(100)) //
                        .buy(security, "2021-02-01", sharesOf(10), amountOf(200)) //
                        .sell(security, "2021-06-01", sharesOf(10), amountOf(400)) //
                        .addTo(client);

        var withOwnCache = collect(client, security, TradeGrouping.PER_LOT);
        var withSharedCache = collect(client, security, TradeGrouping.PER_LOT, new CountingSnapshotCache());

        assertThat(withSharedCache.size(), is(withOwnCache.size()));

        for (int ii = 0; ii < withOwnCache.size(); ii++)
        {
            assertThat(withSharedCache.get(ii).getEntryValueMovingAverage(),
                            is(withOwnCache.get(ii).getEntryValueMovingAverage()));
            assertThat(withSharedCache.get(ii).getEntryValue(), is(withOwnCache.get(ii).getEntryValue()));
        }
    }
}
