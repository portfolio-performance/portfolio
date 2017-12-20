package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;

@SuppressWarnings("nls")
public class PortfolioClientFilterTest
{
    private Client client;

    /**
     * Creates three portfolios (A-C) with a reference account each.
     */
    @Before
    public void setupClient()
    {
        client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        // 2016-01-01 deposit
        // 2016-02-01 buy
        // 2016-03-01 dividend

        Arrays.asList("A", "B", "C").forEach(index -> {
            Account a = new AccountBuilder() //
                            .deposit_(LocalDateTime.of(2016, Month.JANUARY, 1, 0, 0), Values.Amount.factorize(100))
                            .dividend(LocalDateTime.of(2016, 03, 01, 0, 0), Values.Amount.factorize(10), security) //
                            .addTo(client);
            a.setName(index);

            Portfolio p = new PortfolioBuilder(a) //
                            .buy(security, LocalDateTime.of(2016, 02, 01, 0, 0), Values.Share.factorize(1), Values.Amount.factorize(100))
                            .addTo(client);
            p.setName(index);
        });
    }

    @Test
    public void testThatDividendTransactionAreIncluded()
    {
        Portfolio portfolio = client.getPortfolios().get(0);

        Client result = new PortfolioClientFilter(portfolio).filter(client);

        assertThat(result.getPortfolios().size(), is(1));
        assertThat(result.getAccounts().size(), is(1));

        Account account = result.getAccounts().get(0);
        assertThat(account.getTransactions().size(), is(2));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .findAny().isPresent(), is(true));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.REMOVAL)
                        .findAny().isPresent(), is(true));

        assertThat(AccountSnapshot.create(account, new TestCurrencyConverter(), LocalDateTime.of(2016, 9, 3, 23, 59))
                        .getFunds(), is(Money.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testThatFullReferenceAccountIsIncluded()
    {
        Portfolio portfolio = client.getPortfolios().get(0);

        Client result = new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount()).filter(client);

        assertThat(result.getPortfolios().size(), is(1));
        assertThat(result.getAccounts().size(), is(1));

        Account account = result.getAccounts().get(0);

        // 3 transactions: buy, dividend, and deposit
        assertThat(account.getTransactions().size(), is(3));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.BUY).findAny()
                        .isPresent(), is(true));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .findAny().isPresent(), is(true));
        assertThat(account.getTransactions().stream().filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT)
                        .findAny().isPresent(), is(true));
    }

    @Test
    public void testCrossAccountTransfersAreKept()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        entry.setDate(LocalDateTime.of(2016, 04, 01, 0, 0));
        entry.setAmount(Values.Amount.factorize(10));
        entry.setCurrencyCode(accountA.getCurrencyCode());
        entry.insert();

        Client result = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(accountA, accountB))
                        .filter(client);

        assertThat(result.getPortfolios(), empty());
        assertThat(result.getAccounts().size(), is(2));

        Account account = result.getAccounts().get(0);

        // check that the 4 transactions are transformed:
        // - buy -> removal
        // - deposit
        // - dividend -> deposit
        // - transfer must exist
        assertThat(account.getTransactions().size(), is(4));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 02, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 01, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 03, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.TRANSFER_OUT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 04, 01, 0, 0))).findAny().isPresent(),
                        is(true));
    }

    @Test
    public void testCrossAccountTransfersAreConvertedIfAccountIsNotIncluded()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        entry.setDate(LocalDateTime.of(2016, 04, 01, 0, 0));
        entry.setAmount(Values.Amount.factorize(10));
        entry.setCurrencyCode(accountA.getCurrencyCode());
        entry.insert();

        Client result = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(accountA)).filter(client);

        assertThat(result.getPortfolios(), empty());
        assertThat(result.getAccounts().size(), is(1));

        Account account = result.getAccounts().get(0);

        // check that the 4 transactions are transformed:
        // - buy -> removal
        // - deposit
        // - dividend -> deposit
        // - transfer -> removal
        assertThat(account.getTransactions().size(), is(4));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 02, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 01, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 03, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 04, 01, 0, 0))).findAny().isPresent(),
                        is(true));

        // check other account
        result = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(accountB)).filter(client);
        assertThat(result.getAccounts().get(0).getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 04, 01, 0, 0))).findAny().isPresent(),
                        is(true));
    }

    @Test
    public void testCrossPortfolioTransfersAreKept()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setDate(LocalDateTime.of(2016, 04, 01, 0, 0));
        entry.setAmount(Values.Amount.factorize(10));
        entry.setShares(Values.Share.factorize(1));
        entry.setSecurity(client.getSecurities().get(0));
        entry.insert();

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA, portfolioB), Collections.emptyList())
                        .filter(client);

        assertThat(result.getPortfolios().size(), is(2));
        assertThat(result.getAccounts().size(), is(2));

        Portfolio portfolio = result.getPortfolios().get(0);

        // check that the 4 transactions are transformed:
        // - buy -> inbound delivery
        // - transfer must exist
        assertThat(portfolio.getTransactions().size(), is(2));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 02, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.TRANSFER_OUT) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 04, 01, 0, 0))).findAny().isPresent(),
                        is(true));
    }

    @Test
    public void testCrossPortfolioTransfersAreConvertedToDeliveries()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setDate(LocalDateTime.of(2016, 04, 01, 0, 0));
        entry.setAmount(Values.Amount.factorize(10));
        entry.setShares(Values.Share.factorize(1));
        entry.setSecurity(client.getSecurities().get(0));
        entry.insert();

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA), Collections.emptyList())
                        .filter(client);

        assertThat(result.getPortfolios().size(), is(1));
        assertThat(result.getAccounts().size(), is(1));

        Portfolio portfolio = result.getPortfolios().get(0);

        // check that the 4 transactions are transformed:
        // - buy -> inbound delivery
        // - transfer -> outbound delivery
        assertThat(portfolio.getTransactions().size(), is(2));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 02, 01, 0, 0))).findAny().isPresent(),
                        is(true));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 04, 01, 0, 0))).findAny().isPresent(),
                        is(true));

        // check the other portfolio
        result = new PortfolioClientFilter(Arrays.asList(portfolioB), Collections.emptyList()).filter(client);
        assertThat(result.getPortfolios().get(0).getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.of(2016, 04, 01, 0, 0))).findAny().isPresent(),
                        is(true));
    }
}
