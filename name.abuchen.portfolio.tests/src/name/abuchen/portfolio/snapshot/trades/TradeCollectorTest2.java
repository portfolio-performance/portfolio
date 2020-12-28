package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeCollectorTest2
{
    private static Client client;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(TradeCollectorTest2.class.getResourceAsStream("trade_test_case2.xml"));
    }

    @Test
    public void testSplitOfUnitWithForex() throws TradeCollectorException
    {
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        Security att = client.getSecurities().stream().filter(s -> "AT&T Inc.".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(att);

        assertThat(trades.size(), is(2));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2018-10-23T00:00")));
        assertThat(firstTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2018-11-28T00:00")));

        Trade secondTrade = trades.get(1);

        assertThat(secondTrade.getStart(), is(LocalDateTime.parse("2018-10-23T00:00")));
        assertThat(secondTrade.getEnd().isPresent(), is(false));

        // the first transaction has been partially closed by the first trade

        // shares bought = 0.859
        // shares sold = 0.802

        double percentageRemainingShares = ((0.859 - 0.802) / 0.859);

        PortfolioTransaction tx = secondTrade.getTransactions().get(0).getTransaction();
        Unit grossValue = tx.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValue.getAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.63 * percentageRemainingShares))));
        assertThat(grossValue.getExchangeRate(), is(BigDecimal.valueOf(0.8712319219)));
        assertThat(grossValue.getForex(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(28.27 * percentageRemainingShares))));
    }

}
