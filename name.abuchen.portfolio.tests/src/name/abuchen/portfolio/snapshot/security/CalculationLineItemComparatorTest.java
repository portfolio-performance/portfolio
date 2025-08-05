package name.abuchen.portfolio.snapshot.security;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.snapshot.SecurityPosition;

@SuppressWarnings("nls")
public class CalculationLineItemComparatorTest
{
    private Client client = new Client();

    private Security security = new SecurityBuilder() //
                    .addTo(client);

    @Test
    public void testBuyIsPreferredOverSell()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .sell(security, "2010-01-01", 100, 100) //
                        .buy(security, "2010-01-01", 100, 100) //
                        .addTo(client);

        List<CalculationLineItem> list = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());

        Collections.sort(list, new CalculationLineItemComparator());

        assertThat(unwrapTx(list.get(0)).getType(), is(Type.BUY));
        assertThat(unwrapTx(list.get(1)).getType(), is(Type.SELL));
    }

    @Test
    public void testBuyIsPreferredOverSell2()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", 100, 100) //
                        .sell(security, "2010-01-01", 100, 100) //
                        .addTo(client);

        List<CalculationLineItem> list = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());

        Collections.sort(list, new CalculationLineItemComparator());

        assertThat(unwrapTx(list.get(0)).getType(), is(Type.BUY));
        assertThat(unwrapTx(list.get(1)).getType(), is(Type.SELL));
    }

    @Test
    public void testTwoInboundTransactionsStay()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", 1, 100) //
                        .buy(security, "2010-01-01", 2, 100) //
                        .addTo(client);

        List<CalculationLineItem> list = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());

        Collections.sort(list, new CalculationLineItemComparator());

        assertThat(unwrapTx(list.get(0)).getShares(), is(1L));
        assertThat(unwrapTx(list.get(1)).getShares(), is(2L));
    }

    @Test
    public void testThatDatePreceedsType()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .sell(security, "2010-01-01", 100, 100) //
                        .buy(security, "2010-01-02", 100, 100) //
                        .addTo(client);

        List<CalculationLineItem> list = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());

        Collections.sort(list, new CalculationLineItemComparator());

        assertThat(unwrapTx(list.get(0)).getType(), is(Type.SELL));
        assertThat(unwrapTx(list.get(1)).getType(), is(Type.BUY));
    }

    @Test
    public void testThatValuationAtStartIsAlwaysFirst()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .sell(security, "2010-01-01", 100, 100) //
                        .buy(security, "2010-01-01", 100, 100) //
                        .addTo(client);

        List<CalculationLineItem> list = new ArrayList<>();

        portfolio.getTransactions().stream().map(t -> CalculationLineItem.of(portfolio, t))
                        .forEach(item -> list.add(item));

        list.add(CalculationLineItem.atStart(portfolio, new SecurityPosition(security, new TestCurrencyConverter(),
                        new SecurityPrice(), Collections.emptyList()), LocalDateTime.parse("2010-01-01T00:00")));

        list.add(CalculationLineItem.atEnd(portfolio, new SecurityPosition(security, new TestCurrencyConverter(),
                        new SecurityPrice(), Collections.emptyList()), LocalDateTime.parse("2010-01-01T00:00")));

        Collections.sort(list, new CalculationLineItemComparator());

        assertThat(list.get(0), is(instanceOf(CalculationLineItem.ValuationAtStart.class)));
        assertThat(list.get(list.size() - 1), is(instanceOf(CalculationLineItem.ValuationAtEnd.class)));
    }

    @Test
    public void testThatTypeIsUsedWhenDateAndTimeIsIdentical()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-02-01T11:11", 100, 100) //
                        .sell(security, "2010-02-01T11:11", 100, 100) //
                        .sell(security, "2010-02-01T11:11", 100, 100) //
                        .buy(security, "2010-02-01T11:11", 100, 100) //
                        .addTo(client);

        List<CalculationLineItem> list = new ArrayList<>();

        portfolio.getTransactions().stream().map(t -> CalculationLineItem.of(portfolio, t))
                        .forEach(item -> list.add(item));

        Collections.sort(list, new CalculationLineItemComparator());

        assertThat(unwrapTx(list.get(0)).getType(), is(Type.BUY));
        assertThat(unwrapTx(list.get(1)).getType(), is(Type.BUY));
        assertThat(unwrapTx(list.get(2)).getType(), is(Type.SELL));
        assertThat(unwrapTx(list.get(3)).getType(), is(Type.SELL));
    }

    @Test
    public void testMixedTimedAndTimelessTransactions()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        // timed sale
                        .sell(security, "2010-01-01T15:30", 100, 100) //
                        // timeless // purchase
                        .buy(security, "2010-01-01", 100, 100)
                        // timeless sale
                        .sell(security, "2010-01-01", 200, 100)
                        // timed purchase
                        .buy(security, "2010-01-01T09:15", 200, 100).addTo(client);

        List<CalculationLineItem> list = portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList());

        Collections.sort(list, new CalculationLineItemComparator());

        // Should be: timeless purchase, timed purchase (09:15), timed sale
        // (15:30), timeless sale
        var tx0 = unwrapTx(list.get(0));
        var tx1 = unwrapTx(list.get(1));
        var tx2 = unwrapTx(list.get(2));
        var tx3 = unwrapTx(list.get(3));

        assertThat(tx0.getType(), is(Type.BUY));
        assertThat(tx0.getDateTime().getHour(), is(0)); // timeless

        assertThat(tx1.getType(), is(Type.BUY));
        assertThat(tx1.getDateTime().getHour(), is(9)); // timed

        assertThat(tx2.getType(), is(Type.SELL));
        assertThat(tx2.getDateTime().getHour(), is(0)); // timeless

        assertThat(tx3.getType(), is(Type.SELL));
        assertThat(tx3.getDateTime().getHour(), is(15)); // timed
    }

    @Test
    public void testThatTransferIsSortedBeforeSale()
    {
        var portfolioA = new PortfolioBuilder() //
                        // timed sale
                        .sell(security, "2010-01-01T15:30", 100, 100) //
                        // timeless sale
                        .sell(security, "2010-01-01", 200, 100)
                        // timed purchase
                        .buy(security, "2010-01-01T09:15", 200, 100).addTo(client);

        var portfolioB = new PortfolioBuilder().addTo(client);

        PortfolioTransferEntry transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2010-01-01T00:00"));
        transfer.setShares(100);
        transfer.setAmount(100);
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        var list = Stream.of(portfolioA, portfolioB).flatMap(p -> p.getTransactions().stream())
                        .map(t -> CalculationLineItem.of(portfolioA, t)).collect(Collectors.toList());

        Collections.sort(list, new CalculationLineItemComparator());

        var tx0 = unwrapTx(list.get(0));
        var tx1 = unwrapTx(list.get(1));
        var tx2 = unwrapTx(list.get(2));
        var tx3 = unwrapTx(list.get(3));
        var tx4 = unwrapTx(list.get(4));

        assertThat(tx0.getType(), is(Type.BUY));
        assertThat(tx0.getDateTime().getHour(), is(9)); // timeless

        assertThat(tx1.getType(), is(Type.TRANSFER_OUT));
        assertThat(tx1.getDateTime().getHour(), is(0)); // timed

        assertThat(tx2.getType(), is(Type.TRANSFER_IN));
        assertThat(tx2.getDateTime().getHour(), is(0)); // timed

        assertThat(tx3.getType(), is(Type.SELL));
        assertThat(tx3.getDateTime().getHour(), is(0)); // timeless

        assertThat(tx4.getType(), is(Type.SELL));
        assertThat(tx4.getDateTime().getHour(), is(15)); // timeless
    }

    @Test
    public void testComparisonMethodViolatesContract()
    {
        // this tests that sorting transactions items with or without time
        // works. There used to be a bug that resulted sometimes in a
        // 'Comparison method violates its general contract' exception that was
        // reliably reproducible within 10K iterations.

        Account account = new AccountBuilder() //
                        .dividend("2010-01-01", 10, security) // without time!
                        .addTo(client);

        var portfolio = new PortfolioBuilder(account);

        var time = LocalDateTime.parse("2010-01-01T10:00");
        for (int ii = 0; ii < 30; ii++)
        {
            portfolio.sell(security, time.toString(), 100, 100) //
                            .buy(security, time.toString(), 100, 100);
            time = time.plusMinutes(10);
        }

        portfolio.addTo(client);

        var transactions = client.getAllTransactions().stream().collect(toMutableList());

        for (int ii = 0; ii < 10000; ii++)
        {
            Collections.shuffle(transactions);

            List<CalculationLineItem> list = new ArrayList<>();
            transactions.stream().map(CalculationLineItem::of).forEach(list::add);
            Collections.sort(list, new CalculationLineItemComparator());
        }
    }

    private PortfolioTransaction unwrapTx(CalculationLineItem data)
    {
        return (PortfolioTransaction) data.getTransaction().orElseThrow(IllegalArgumentException::new);
    }
}
