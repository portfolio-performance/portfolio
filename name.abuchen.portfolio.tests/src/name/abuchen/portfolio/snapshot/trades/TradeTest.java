package name.abuchen.portfolio.snapshot.trades;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.quoteOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.sharesOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

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
    }

    @Test
    public void testShortMovingAverageCostDoesNotDivideByZero() throws TradeCollectorException
    {
        Client client = new Client();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        var portfolio = new PortfolioBuilder();
        portfolio.addTo(client);

        Security securityShort = new SecurityBuilder().addTo(client);
        portfolio.sellPrice(securityShort, "2024-01-01", 3.0, 20.0).buyPrice(securityShort, "2024-12-31", 3.0, 5.0);

        List<Trade> trades = collector.collect(securityShort);
        Trade trade = trades.get(0);

        Money movingAverageEntryValue = trade.getEntryValueMovingAverage();

        assertThat(movingAverageEntryValue, is(Money.of(CurrencyUnit.EUR, 0L)));
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

    /**
     * The scenario reported in
     * https://github.com/portfolio-performance/portfolio/pull/5910#issuecomment-5123694459:
     * an open position of 100 shares of a USD security, delivered inbound into
     * an EUR account at 100 x USD 50 = USD 5,000 with the exchange rate USD/EUR
     * 0.90 recorded on the transaction, i.e. EUR 4,500.
     */
    private static Client createClientWithSecurityInForeignCurrency()
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        var security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2025-01-01", quoteOf(80)) //
                        .addTo(client);

        var account = new AccountBuilder(CurrencyUnit.EUR).addTo(client);

        new PortfolioBuilder(account) //
                        .inbound_delivery(security, "2024-01-01", sharesOf(100),
                                        new Unit(Unit.Type.GROSS_VALUE, //
                                                        Money.of(CurrencyUnit.EUR, amountOf(4500)),
                                                        Money.of(CurrencyUnit.USD, amountOf(5000)),
                                                        BigDecimal.valueOf(0.90)))
                        .addTo(client);

        return client;
    }

    @Test
    public void testForeignCurrencySecurityReportedInAccountCurrency() throws TradeCollectorException
    {
        Client client = createClientWithSecurityInForeignCurrency();
        Security security = client.getSecurities().get(0);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter(CurrencyUnit.EUR));

        List<Trade> trades = collector.collect(security);
        assertThat(trades.size(), is(1));

        Trade trade = trades.get(0);
        assertThat(trade.isClosed(), is(false));
        assertThat(trade.getShares(), is(sharesOf(100)));
        assertThat(trade.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));

        // the transaction is booked in EUR, hence both cost methods report the
        // amount of the transaction
        assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.EUR, amountOf(4500))));
        assertThat(trade.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(4500))));

        // the position is still open: 100 x USD 80 converted with the exchange
        // rate of the currency converter (EUR/USD 1.1588) = 6903.69
        assertThat(trade.getExitValue(), is(Money.of(CurrencyUnit.EUR, amountOf(6903.69))));
        assertThat(trade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, amountOf(6903.69 - 4500))));
        assertThat(trade.getProfitLossMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(6903.69 - 4500))));
    }

    @Test
    public void testForeignCurrencySecurityReportedInSecurityCurrency() throws TradeCollectorException
    {
        Client client = createClientWithSecurityInForeignCurrency();
        Security security = client.getSecurities().get(0);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter(CurrencyUnit.USD));

        List<Trade> trades = collector.collect(security);
        assertThat(trades.size(), is(1));

        Trade trade = trades.get(0);
        assertThat(trade.isClosed(), is(false));
        assertThat(trade.getShares(), is(sharesOf(100)));
        assertThat(trade.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));

        // reported in the currency of the security, the entry value is what has
        // been bought: 100 x USD 50. It must be derived from the exchange rate
        // recorded on the transaction and not from the historical rate of that
        // day (which would give EUR 4,500 x 1.1588 = USD 5,214.60)
        assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.USD, amountOf(5000))));
        assertThat(trade.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.USD, amountOf(5000))));

        // the position is still open: 100 x USD 80
        assertThat(trade.getExitValue(), is(Money.of(CurrencyUnit.USD, amountOf(8000))));
        assertThat(trade.getProfitLoss(), is(Money.of(CurrencyUnit.USD, amountOf(8000 - 5000))));
        assertThat(trade.getProfitLossMovingAverage(), is(Money.of(CurrencyUnit.USD, amountOf(8000 - 5000))));
    }

    /**
     * A closed trade in a USD security bought and later sold via an EUR
     * account, with the exchange rate recorded on each transaction (buy at
     * USD/EUR 0.90, sell at USD/EUR 0.80) plus non-zero fees and taxes.
     * Reported in the security's currency, both the exit value and the
     * profit/loss must be derived from the recorded transaction rates and not
     * from the historical day rate.
     */
    private static Client createClientWithClosedForeignCurrencyTrade()
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        var security = new SecurityBuilder(CurrencyUnit.USD).addTo(client);

        var account = new AccountBuilder(CurrencyUnit.EUR).addTo(client);

        var portfolio = new Portfolio();
        portfolio.setName("pf");
        portfolio.setReferenceAccount(account);

        // buy 100 x USD 50 = USD 5,000 gross (EUR 4,500 at USD/EUR 0.90) plus
        // USD 100 fees and USD 50 taxes, i.e. EUR 4,635 paid = USD 5,150
        var buy = new PortfolioTransaction(LocalDateTime.parse("2024-01-01T00:00"), CurrencyUnit.EUR, amountOf(4635),
                        security, sharesOf(100), Type.DELIVERY_INBOUND, amountOf(90), amountOf(45));
        buy.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(CurrencyUnit.EUR, amountOf(4500)), //
                        Money.of(CurrencyUnit.USD, amountOf(5000)), //
                        BigDecimal.valueOf(0.90)));
        portfolio.addTransaction(buy);

        // sell 100 x USD 80 = USD 8,000 gross (EUR 6,400 at USD/EUR 0.80) less
        // USD 100 fees and USD 50 taxes, i.e. EUR 6,280 received = USD 7,850
        var sell = new PortfolioTransaction(LocalDateTime.parse("2024-12-31T00:00"), CurrencyUnit.EUR, amountOf(6280),
                        security, sharesOf(100), Type.DELIVERY_OUTBOUND, amountOf(80), amountOf(40));
        sell.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(CurrencyUnit.EUR, amountOf(6400)), //
                        Money.of(CurrencyUnit.USD, amountOf(8000)), //
                        BigDecimal.valueOf(0.80)));
        portfolio.addTransaction(sell);

        client.addPortfolio(portfolio);

        return client;
    }

    @Test
    public void testForeignCurrencyClosedTradeReportedInSecurityCurrency() throws TradeCollectorException
    {
        var client = createClientWithClosedForeignCurrencyTrade();
        var security = client.getSecurities().get(0);

        var collector = new TradeCollector(client, new TestCurrencyConverter(CurrencyUnit.USD));

        var trades = collector.collect(security);
        assertThat(trades.size(), is(1));

        var trade = trades.get(0);
        assertThat(trade.isClosed(), is(true));
        assertThat(trade.isLong(), is(true));
        assertThat(trade.getShares(), is(sharesOf(100)));
        assertThat(trade.getStart(), is(LocalDateTime.parse("2024-01-01T00:00")));
        assertThat(trade.getEnd().get(), is(LocalDateTime.parse("2024-12-31T00:00")));

        // entry and exit are derived from the exchange rate recorded on each
        // transaction and not from the historical rate of that day: buy
        // EUR 4,635 / 0.90 = USD 5,150, sell EUR 6,280 / 0.80 = USD 7,850
        assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.USD, amountOf(5150))));
        assertThat(trade.getExitValue(), is(Money.of(CurrencyUnit.USD, amountOf(7850))));

        // net profit/loss after fees and taxes: USD 7,850 - USD 5,150
        assertThat(trade.getProfitLoss(), is(Money.of(CurrencyUnit.USD, amountOf(7850 - 5150))));

        // gross profit/loss before fees and taxes: USD 8,000 - USD 5,000
        assertThat(trade.getProfitLossWithoutTaxesAndFees(), is(Money.of(CurrencyUnit.USD, amountOf(8000 - 5000))));

        // the position is held for a full year, hence the IRR equals the
        // simple return of USD 7,850 / USD 5,150 - 1
        assertThat(trade.getReturn(), is(7850 / 5150.0 - 1));
        assertEquals(0.52427, trade.getIRR(), 0.0001);
    }
}
