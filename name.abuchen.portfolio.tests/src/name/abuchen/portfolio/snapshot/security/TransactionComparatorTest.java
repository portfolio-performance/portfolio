package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class TransactionComparatorTest
{
    private Client client = new Client();

    private Security security = new SecurityBuilder() //
                    .addTo(client);

    @Test
    public void testBuyIsPreferredOverSell()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .sell(security, LocalDateTime.of(2010, 01, 01, 0, 0), 100, 100) //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 100, 100) //
                        .addTo(client);

        List<PortfolioTransaction> list = portfolio.getTransactions();

        Collections.sort(list, new TransactionComparator());

        assertThat(list.get(0).getType(), is(Type.BUY));
        assertThat(list.get(1).getType(), is(Type.SELL));
    }

    @Test
    public void testBuyIsPreferredOverSell2()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 100, 100) //
                        .sell(security, LocalDateTime.of(2010, 01, 01, 0, 0), 100, 100) //
                        .addTo(client);

        List<PortfolioTransaction> list = portfolio.getTransactions();

        Collections.sort(list, new TransactionComparator());

        assertThat(list.get(0).getType(), is(Type.BUY));
        assertThat(list.get(1).getType(), is(Type.SELL));
    }

    @Test
    public void testTwoInboundTransactionsStay()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 1, 100) //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 2, 100) //
                        .addTo(client);

        List<PortfolioTransaction> list = portfolio.getTransactions();

        Collections.sort(list, new TransactionComparator());

        assertThat(list.get(0).getShares(), is(1L));
        assertThat(list.get(1).getShares(), is(2L));
    }

    @Test
    public void testThatDatePreceedsType()
    {
        Portfolio portfolio = new PortfolioBuilder() //
                        .sell(security, LocalDateTime.of(2010, 01, 01, 0, 0), 100, 100) //
                        .buy(security, LocalDateTime.of(2010, 01, 02, 0, 0), 100, 100) //
                        .addTo(client);

        List<PortfolioTransaction> list = portfolio.getTransactions();

        Collections.sort(list, new TransactionComparator());

        assertThat(list.get(0).getType(), is(Type.SELL));
        assertThat(list.get(1).getType(), is(Type.BUY));
    }

}
