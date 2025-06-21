package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.time.LocalDateTime;
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
    public void testTradesProtitAndGrossProfitFIFOAndMovingAverage() throws TradeCollectorException
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

}
