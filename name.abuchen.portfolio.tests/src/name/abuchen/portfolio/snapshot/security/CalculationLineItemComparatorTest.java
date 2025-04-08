package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
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

    private PortfolioTransaction unwrapTx(CalculationLineItem data)
    {
        return (PortfolioTransaction) data.getTransaction().orElseThrow(IllegalArgumentException::new);
    }
}
