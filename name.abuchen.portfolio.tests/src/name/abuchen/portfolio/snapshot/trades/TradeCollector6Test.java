package name.abuchen.portfolio.snapshot.trades;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class TradeCollector6Test
{
    @Test
    public void testTradesSimpleOpenPosition() throws TradeCollectorException
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2022-03-01", Values.Quote.factorize(200)) //
                        .addTo(client);
        new PortfolioBuilder(new Account("one")).inbound_delivery(security, "2022-01-01", Values.Share.factorize(5),
                        Values.Amount.factorize(520), Values.Amount.factorize(10), Values.Amount.factorize(10))
                        .inbound_delivery(security, "2022-02-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000))
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        List<Trade> trades = collector.collect(security);

        assertThat(trades.size(), is(1));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2022-01-01T00:00")));
        assertThat(firstTrade.getEnd().isPresent(), is(false));

        // 200*15-520-1000 = 1480
        assertThat(firstTrade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1480.00))));
        assertThat(firstTrade.getProfitLossMovingAverage(), is(firstTrade.getProfitLoss()));

        // 200*15-520-10-10-1000 = 1500
        assertThat(firstTrade.getProfitLossWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500.00))));
        assertThat(firstTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(firstTrade.getProfitLossWithoutTaxesAndFees()));
    }

    @Test
    public void testTradesProfitAndGrossProfitFIFOAndMovingAverage() throws TradeCollectorException
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2023-03-01", Values.Quote.factorize(200)) //
                        .addTo(client);
        new PortfolioBuilder(new Account("one")).inbound_delivery(security, "2022-01-01", Values.Share.factorize(5),
                        Values.Amount.factorize(520), Values.Amount.factorize(10), Values.Amount.factorize(10))
                        .inbound_delivery(security, "2022-02-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000))
                        .outbound_delivery(security, "2022-03-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1480), Values.Amount.factorize(10),
                                        Values.Amount.factorize(10))
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        List<Trade> trades = collector.collect(security);

        assertThat(trades.size(), is(2));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2022-01-01T00:00")));
        assertThat(firstTrade.getEnd().isPresent(), is(true));

        // 1480 - (520 + 1000*5/10) = 460
        assertThat(firstTrade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(460.00))));
        // 1480 - (520 + 1000)*10/15 = 466.67
        assertThat(firstTrade.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(466.67))));

        // 1480+10+10 - (520-10-10 + 1000*5/10) = 500
        assertThat(firstTrade.getProfitLossWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        // 1480+10+10 - (520-10-10 + 1000)*10/15 = 500
        assertThat(firstTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));

        // return : 1480/(520 + 1000*5/10)-1=0.45098
        assertThat(firstTrade.getReturn(), closeTo(0.4510, 0.0001));
        // return : 1480/[(520 + 1000)*10/15]-1=0.46052
        assertThat(firstTrade.getReturnMovingAverage(), closeTo(0.4605, 0.0001));

        Trade secondTrade = trades.get(1);
        assertThat(secondTrade.getEnd().isPresent(), is(false));
        // 200*5 - (1000*5/10) = 500
        assertThat(secondTrade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        // 200*5 - (520 + 1000)*5/15 = 493.33
        assertThat(secondTrade.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(493.33))));

        // 200*5 - (500*5/10) = 500
        assertThat(secondTrade.getProfitLossWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        // 200*5 - (520-10-10 + 1000)*5/15 = 500
        assertThat(secondTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));

        // return : 200*5 / (1000*5/10)-1 = 1
        assertThat(secondTrade.getReturn(), closeTo(1.0000, 0.0001));
        // return : 200*5 / [(520 + 1000)*5/15]-1 = 0.97368
        assertThat(secondTrade.getReturnMovingAverage(), closeTo(0.9737, 0.0001));
    }

    @Test
    public void testMovingAverageTradesSeveralPortfolio() throws TradeCollectorException
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);
        new PortfolioBuilder(new Account("one")).buyPrice(security, "2022-01-01", 5, 100).addTo(client);
        new PortfolioBuilder(new Account("two")).buyPrice(security, "2022-02-02", 5, 200).addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades = collector.collect(security);
        Collections.sort(trades, (r, l) -> r.getStart().compareTo(l.getStart()));

        assertThat(trades.size(), is(2));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2022-01-01T00:00")));
        assertThat(firstTrade.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(100 * 5))));

        Trade secondTrade = trades.get(1);

        assertThat(secondTrade.getStart(), is(LocalDateTime.parse("2022-02-02T00:00")));
        assertThat(secondTrade.getEntryValueMovingAverage(), is(Money.of(CurrencyUnit.EUR, amountOf(200 * 5))));
    }

    @Test
    public void testMovingAverageTradesSeveralPortfolio2() throws TradeCollectorException
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2023-03-01", Values.Quote.factorize(200)) //
                        .addTo(client);
        var p1 = new PortfolioBuilder(new Account("one")).buyPrice(security, "2022-01-01", 5, 100, 10, 10)
                        .buyPrice(security, "2022-02-01", 10, 100)
                        .sellPrice(security, "2022-03-01", 10, 150, 10, 10)
                        .addTo(client);

        var p2 = new PortfolioBuilder(new Account("two")).buyPrice(security, "2022-01-02", 5, 200, 10, 10)
                        .buyPrice(security, "2022-02-02", 10, 200)
                        .sellPrice(security, "2022-03-02", 10, 150, 10, 10)
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades = collector.collect(security);
        Collections.sort(trades, (r, l) -> r.getStart().compareTo(l.getStart()));

        assertThat(trades.size(), is(4));

        Trade firstClosedTrade = trades.get(0);

        assertThat(firstClosedTrade.getStart(), is(LocalDateTime.parse("2022-01-01T00:00")));
        assertThat(firstClosedTrade.getEnd().isPresent(), is(true));
        // 1500 - (5*100 + 10*100) * 10/15 = 500
        assertThat(firstClosedTrade.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        // 1500+10+10 - (500-10-10 + 1000) * 10/15 = 533.33
        assertThat(firstClosedTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(533.33))));

        Trade secondClosedTrade = trades.get(1);

        assertThat(secondClosedTrade.getStart(), is(LocalDateTime.parse("2022-01-02T00:00")));
        assertThat(secondClosedTrade.getEnd().isPresent(), is(true));
        // 1500 - (5*200 + 10*200) * 10/15 = -500
        assertThat(secondClosedTrade.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-500))));
        // 1500+10+10 - (1000-10-10 + 2000) * 10/15 = -466.666
        assertThat(secondClosedTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-466.67))));

        Trade firstOpenTrade = trades.get(2);

        assertThat(firstOpenTrade.getEnd().isPresent(), is(false));
        assertThat(firstOpenTrade.getStart(), is(LocalDateTime.parse("2022-02-01T00:00")));
        // 200 * 5 - (500 + 1000) * 5/15 = 500
        assertThat(firstOpenTrade.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        // 200 * 5 - (500-10-10 + 1000) * 5/15 = 506.666
        assertThat(firstOpenTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(506.67))));

        var snapshot = LazySecurityPerformanceSnapshot.create(new PortfolioClientFilter(p1).filter(client),
                        new TestCurrencyConverter(), Interval.of(LocalDate.MIN, LocalDate.now()));
        var securityRecord = snapshot.getRecord(security).get();

        assertThat(firstOpenTrade.getEntryValueMovingAverage(), is(securityRecord.getMovingAverageCost().get()));

        Trade secondOpenTrade = trades.get(3);

        assertThat(secondOpenTrade.getEnd().isPresent(), is(false));
        assertThat(secondOpenTrade.getStart(), is(LocalDateTime.parse("2022-02-02T00:00")));
        // 200 * 5 - (1000 + 2000) * 5/15 = 0
        assertThat(secondOpenTrade.getProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        // 200 * 5 - (1000-10-10 + 2000) * 5/15 = 6.666
        assertThat(secondOpenTrade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.67))));

        snapshot = LazySecurityPerformanceSnapshot.create(new PortfolioClientFilter(p2).filter(client),
                        new TestCurrencyConverter(), Interval.of(LocalDate.MIN, LocalDate.now()));
        securityRecord = snapshot.getRecord(security).get();

        assertThat(secondOpenTrade.getEntryValueMovingAverage(), is(securityRecord.getMovingAverageCost().get()));
    }

}
