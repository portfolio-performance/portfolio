package name.abuchen.portfolio.snapshot.trades;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.quoteOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.sharesOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * This is intended to be unit test for Trade (and by extension, TradeCollector)
 * class, trying to enumerate various most common scenarios in one place and
 * have more or all exhaustive checks, thus ensuring good coverage.
 */
@SuppressWarnings("nls")
public class TradeTest
{
    @Test
    public void testLong() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades;

        var port = new PortfolioBuilder();
        port.addTo(client);

        Security securityLong = new SecurityBuilder().addTo(client);
        port.buyPrice(securityLong, "2024-01-01", 5.0, 100.0).sellPrice(securityLong, "2024-12-31", 5.0, 180.0);

        trades = collector.collect(securityLong);
        assertThat(trades.size(), is(1));

        Trade trade1 = trades.get(0);
        assertThat(trade1.isClosed(), is(true));
        assertThat(trade1.isLong(), is(true));
        assertThat(trade1.getShares(), is(sharesOf(5)));
        assertThat(trade1.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));
        assertThat(trade1.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100) * 5)));
        assertThat(trade1.getEnd().get(), is(LocalDateTime.parse("2024-12-31T00:00")));
        assertThat(trade1.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(180) * 5)));
        assertThat(trade1.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(180 - 100) * 5)));
        assertThat(trade1.getReturn(), is(0.8));
        assertEquals(trade1.getIRR(), 0.8, 0.0001);

        assertThat(trade1.getEntryValueMovingAverage(), is(trade1.getEntryValue()));
        assertThat(trade1.getProfitLossMovingAverage(), is(trade1.getProfitLoss()));
        assertThat(trade1.getReturnMovingAverage(), is(trade1.getReturn()));
    }

    @Test
    public void testLongUnclosed() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades;

        var port = new PortfolioBuilder();
        port.addTo(client);

        Security securityLong = new SecurityBuilder().addPrice("2025-01-01", quoteOf(210)).addTo(client);
        port.buyPrice(securityLong, "2024-01-01", 5.0, 100.0).sellPrice(securityLong, "2024-12-31", 3.0, 180.0);

        trades = collector.collect(securityLong);
        assertThat(trades.size(), is(2));

        Trade trade1 = trades.get(0);
        assertThat(trade1.isClosed(), is(true));
        assertThat(trade1.isLong(), is(true));
        assertThat(trade1.getShares(), is(sharesOf(3)));
        assertThat(trade1.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));
        assertThat(trade1.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100) * 3)));
        assertThat(trade1.getEnd().get(), is(LocalDateTime.parse("2024-12-31T00:00")));
        assertThat(trade1.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(180) * 3)));
        assertThat(trade1.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(180 - 100) * 3)));
        assertThat(trade1.getReturn(), is(0.8));
        assertEquals(trade1.getIRR(), 0.8, 0.0001);

        Trade trade2 = trades.get(1);
        assertThat(trade2.isClosed(), is(false));
        assertThat(trade2.isLong(), is(true));
        assertThat(trade2.getShares(), is(sharesOf(2)));
        assertThat(trade1.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));
        assertThat(trade2.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(100) * 2)));
        assertThat(trade2.getEnd().isPresent(), is(false));
        assertThat(trade2.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(210) * 2)));
        assertThat(trade2.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(210 - 100) * 2)));
        assertThat(trade2.getReturn(), is((210 - 100) / 100.0));
    }

    @Test
    public void testShort() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades;

        var port = new PortfolioBuilder();
        port.addTo(client);

        Security securityShort = new SecurityBuilder().addTo(client);
        port.sellPrice(securityShort, "2024-01-01", 3.0, 20.0).buyPrice(securityShort, "2024-12-31", 3.0, 5.0);

        trades = collector.collect(securityShort);
        assertThat(trades.size(), is(1));

        Trade trade1 = trades.get(0);
        assertThat(trade1.isClosed(), is(true));
        assertThat(trade1.isLong(), is(false));
        assertThat(trade1.getShares(), is(sharesOf(3)));
        assertThat(trade1.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));
        assertThat(trade1.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(20) * 3)));
        assertThat(trade1.getEnd().get(), is(LocalDateTime.parse("2024-12-31T00:00")));
        assertThat(trade1.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(5) * 3)));
        assertThat(trade1.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(20 - 5) * 3)));
        assertThat(trade1.getReturn(), is(0.75));
        assertEquals(trade1.getIRR(), 0.75, 0.0001);

        assertThat(trade1.getEntryValueMovingAverage(), is(trade1.getEntryValue()));
        assertThat(trade1.getProfitLossMovingAverage(), is(trade1.getProfitLoss()));
        assertThat(trade1.getReturnMovingAverage(), is(trade1.getReturn()));
    }

    @Test
    public void testLongMultipleBuys() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades;

        var port = new PortfolioBuilder();
        port.addTo(client);

        Security security = new SecurityBuilder().addPrice("2025-01-01", quoteOf(2)).addTo(client);
        port.buyPrice(security, "2024-01-01", 12.0, 10.0).buyPrice(security, "2024-02-01", 5.0, 12.0)
                        .buyPrice(security, "2024-03-01", 3.0, 30.0).sellPrice(security, "2024-12-31", 18.0, 20.0);

        trades = collector.collect(security);
        assertThat(trades.size(), is(2));

        Trade trade1 = trades.get(0);
        assertThat(trade1.isClosed(), is(true));
        assertThat(trade1.isLong(), is(true));
        assertThat(trade1.getShares(), is(sharesOf(18)));
        var entryAmount = 12 * 10 + 5 * 12 + 1 * 30;
        var exitAmount = 18 * 20;
        assertThat(trade1.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(exitAmount - entryAmount))));
        assertEquals(trade1.getReturn(), (double) (exitAmount - entryAmount) / entryAmount, 0.00000001);
        assertEquals(trade1.getIRR(), 0.76018, 0.0001);

        Trade trade2 = trades.get(1);
        assertThat(trade2.isClosed(), is(false));
        assertThat(trade2.isLong(), is(true));
        assertThat(trade2.getShares(), is(sharesOf(2)));
        assertThat(trade2.getEnd().isPresent(), is(false));
        assertThat(trade2.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(2 * 2 - 2 * 30))));
    }

    @Test
    public void testShortMultipleSells() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades;

        var port = new PortfolioBuilder();
        port.addTo(client);

        Security securityShort = new SecurityBuilder().addTo(client);
        port.sellPrice(securityShort, "2024-01-01", 2.0, 100.0).sellPrice(securityShort, "2024-02-01", 3.0, 120.0)
                        .sellPrice(securityShort, "2024-03-01", 2.0, 50.0)
                        .buyPrice(securityShort, "2024-12-31", 4.0, 20.0);

        trades = collector.collect(securityShort);
        assertThat(trades.size(), is(2));

        Trade trade1 = trades.get(0);
        assertThat(trade1.isClosed(), is(true));
        assertThat(trade1.isLong(), is(false));
        assertThat(trade1.getShares(), is(sharesOf(4)));
        var entryAmount = 2 * 100 + 2 * 120;
        var exitAmount = 4 * 20;
        assertThat(trade1.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(entryAmount))));
        assertThat(trade1.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(exitAmount))));
        assertThat(trade1.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(entryAmount - exitAmount))));
        assertThat(trade1.getReturn(), is(1.0 - (double) exitAmount / entryAmount));
        assertEquals(0.8710, trade1.getIRR(), 0.0001);

        Trade trade2 = trades.get(1);
        assertThat(trade2.isClosed(), is(false));
        assertThat(trade2.isLong(), is(false));
        assertThat(trade2.getShares(), is(sharesOf(3)));
        assertThat(trade2.getEnd().isPresent(), is(false));
        assertThat(trade2.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(1 * 120 + 2 * 50 - 0))));
        assertThat(trade2.getReturn(), is(1.0));
    }

    @Test
    public void testShortMultipleSellsMovingAverage() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades;

        var port = new PortfolioBuilder();
        port.addTo(client);

        Security securityShort = new SecurityBuilder().addPrice("2025-03-01", Values.Quote.factorize(30)).addTo(client);
        port.sellPrice(securityShort, "2024-01-01", 2.0, 100.0).sellPrice(securityShort, "2024-02-01", 3.0, 120.0)
                        .sellPrice(securityShort, "2024-03-01", 2.0, 50.0)
                        .buyPrice(securityShort, "2024-12-31", 4.0, 20.0);

        trades = collector.collect(securityShort);
        assertThat(trades.size(), is(2));

        Trade trade1 = trades.get(0);
        assertThat(trade1.isClosed(), is(true));
        assertThat(trade1.isLong(), is(false));
        assertThat(trade1.getShares(), is(sharesOf(4)));
        // entryAmount = (2 * 100 + 3 * 120 + 2 * 50) * 4 / 7 = 377.142857
        var exitAmount = 4 * 20;
        assertThat(trade1.getEntryValueMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(377.14))));
        assertThat(trade1.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(exitAmount))));
        assertThat(trade1.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(377.14 - exitAmount))));
        assertThat(trade1.getReturnMovingAverage(), closeTo(1.0 - (double) exitAmount / 377.14, 0.0001));
        assertEquals(0.8710, trade1.getIRR(), 0.0001);

        Trade trade2 = trades.get(1);
        // entryAmount2 = (2 * 100 + 3 * 120 + 2 * 50) * 3 / 7 = 282.85714
        var exitAmount2 = 3 * 30;

        assertThat(trade2.isClosed(), is(false));
        assertThat(trade2.isLong(), is(false));
        assertThat(trade2.getShares(), is(sharesOf(3)));
        assertThat(trade2.getEnd().isPresent(), is(false));
        assertThat(trade2.getProfitLossMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(282.86 - exitAmount2))));
        assertThat(trade2.getReturnMovingAverage(), is(1 - exitAmount2 / 282.86));
    }
}
