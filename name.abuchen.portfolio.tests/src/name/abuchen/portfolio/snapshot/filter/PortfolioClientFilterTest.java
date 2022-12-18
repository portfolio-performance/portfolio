package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
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
                            .deposit_("2016-01-01", Values.Amount.factorize(100))
                            .dividend("2016-03-01", Values.Amount.factorize(10), security) //
                            .addTo(client);
            a.setName(index);

            Portfolio p = new PortfolioBuilder(a) //
                            .buy(security, "2016-02-01", Values.Share.factorize(1), Values.Amount.factorize(100))
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
        assertThat(account.getTransactions().stream().anyMatch(t -> t.getType() == AccountTransaction.Type.DIVIDENDS),
                        is(true));
        assertThat(account.getTransactions().stream().anyMatch(t -> t.getType() == AccountTransaction.Type.REMOVAL),
                        is(true));

        assertThat(AccountSnapshot.create(account, new TestCurrencyConverter(), LocalDate.parse("2016-09-03"))
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
        assertThat(account.getTransactions().stream().anyMatch(t -> t.getType() == AccountTransaction.Type.BUY),
                        is(true));
        assertThat(account.getTransactions().stream().anyMatch(t -> t.getType() == AccountTransaction.Type.DIVIDENDS),
                        is(true));
        assertThat(account.getTransactions().stream().anyMatch(t -> t.getType() == AccountTransaction.Type.DEPOSIT),
                        is(true));
    }

    @Test
    public void testCrossAccountTransfersAreKept()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        entry.setDate(LocalDateTime.parse("2016-04-01T00:00"));
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
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-01T00:00"))), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-01-01T00:00"))), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-03-01T00:00"))), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.TRANSFER_OUT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))), is(true));
    }

    @Test
    public void testCrossAccountTransfersAreConvertedIfAccountIsNotIncluded()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        entry.setDate(LocalDateTime.parse("2016-04-01T00:00"));
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
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-01T00:00"))), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-01-01T00:00"))), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-03-01T00:00"))), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))), is(true));

        // check other account
        result = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(accountB)).filter(client);
        assertThat(result.getAccounts().get(0).getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))), is(true));
    }

    @Test
    public void testCrossPortfolioTransfersAreKept()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setDate(LocalDateTime.parse("2016-04-01T00:00"));
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
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-01T00:00"))), is(true));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.TRANSFER_OUT) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))), is(true));
    }

    @Test
    public void testCrossPortfolioTransfersAreConvertedToDeliveries()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setDate(LocalDateTime.parse("2016-04-01T00:00"));
        entry.setAmount(Values.Amount.factorize(10));
        entry.setShares(Values.Share.factorize(1));
        entry.setSecurity(client.getSecurities().get(0));
        entry.insert();

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA), Collections.emptyList()).filter(client);

        assertThat(result.getPortfolios().size(), is(1));
        assertThat(result.getAccounts().size(), is(1));

        Portfolio portfolio = result.getPortfolios().get(0);

        // check that the 4 transactions are transformed:
        // - buy -> inbound delivery
        // - transfer -> outbound delivery
        assertThat(portfolio.getTransactions().size(), is(2));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-02-01T00:00"))), is(true));
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))), is(true));

        // check the other portfolio
        result = new PortfolioClientFilter(Arrays.asList(portfolioB), Collections.emptyList()).filter(client);
        assertThat(result.getPortfolios().get(0).getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .anyMatch(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))), is(true));
    }

    @Test
    public void testThatDividendsAreNotIncludedMultipleTimesIfPortfoliosHaveSameReferenceAccount()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        portfolioB.getReferenceAccount().getTransactions().stream()
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS)
                        .forEach(t -> portfolioA.getReferenceAccount().addTransaction(t));

        portfolioB.setReferenceAccount(portfolioA.getReferenceAccount());

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA, portfolioB), Collections.emptyList())
                        .filter(client);

        assertThat(result.getPortfolios().size(), is(2));
        assertThat(result.getAccounts().size(), is(1));

        List<AccountTransaction> dividendTx = result.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS).collect(Collectors.toList());

        assertThat(dividendTx.size(), is(2));
    }

}
