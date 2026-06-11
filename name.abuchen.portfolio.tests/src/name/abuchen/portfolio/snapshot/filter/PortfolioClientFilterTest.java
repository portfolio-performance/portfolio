package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import name.abuchen.portfolio.model.Classification;
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
    private static final int HALF = Classification.ONE_HUNDRED_PERCENT / 2;
    private static final int EIGHTY = Classification.ONE_HUNDRED_PERCENT * 8 / 10;

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
        // - buy -> withdrawal
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
        // - buy -> withdrawal
        // - deposit
        // - dividend -> deposit
        // - transfer -> withdrawal
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

    @Test
    public void testCanonicalFormAndWeightAccessors()
    {
        Portfolio p = client.getPortfolios().get(0);

        var def = new PortfolioClientFilter(Arrays.asList(p), Collections.emptyList());

        // an explicit 100% entry must be normalized away (canonical form) so it
        // stays equal to the default filter
        var explicit = new PortfolioClientFilter(Arrays.asList(p), Collections.emptyList(),
                        Map.of(p, Classification.ONE_HUNDRED_PERCENT));
        assertThat(explicit, is(def));
        assertThat(explicit.hashCode(), is(def.hashCode()));
        assertThat(def.getWeight(p), is(Classification.ONE_HUNDRED_PERCENT));

        var half = new PortfolioClientFilter(new ArrayList<>(Arrays.asList(p)),
                        new ArrayList<>(Collections.emptyList()), new HashMap<>());
        half.setWeight(p, HALF);
        assertThat(half.getWeight(p), is(HALF));
        assertThat(half, is(not(def)));

        // setting back to 100% removes the entry -> equal to default again
        half.setWeight(p, Classification.ONE_HUNDRED_PERCENT);
        assertThat(half, is(def));

        // removeElement drops the weight; absent -> 100%
        half.setWeight(p, HALF);
        half.removeElement(p);
        assertThat(half.getWeight(p), is(Classification.ONE_HUNDRED_PERCENT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetWeightRejectsZero()
    {
        Portfolio p = client.getPortfolios().get(0);
        new PortfolioClientFilter(new ArrayList<>(Arrays.asList(p)), new ArrayList<>()).setWeight(p, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetWeightRejectsAboveHundredPercent()
    {
        Portfolio p = client.getPortfolios().get(0);
        new PortfolioClientFilter(new ArrayList<>(Arrays.asList(p)), new ArrayList<>()).setWeight(p,
                        Classification.ONE_HUNDRED_PERCENT + 1);
    }

    @Test
    public void testAccountAtFiftyPercentScalesAllCashFlows()
    {
        Account accountA = client.getAccounts().get(0);

        Client result = new PortfolioClientFilter(Collections.emptyList(), Arrays.asList(accountA),
                        Map.of(accountA, HALF)).filter(client);

        Account account = result.getAccounts().get(0);

        // deposit 100 -> 50, buy 100 -> removal 50, dividend 10 -> deposit 5
        // => funds 50 - 50 + 5 = 5
        assertThat(AccountSnapshot.create(account, new TestCurrencyConverter(), LocalDate.parse("2016-09-03"))
                        .getFunds(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5))));
    }

    @Test
    public void testSecuritiesAccountAtFiftyWithFullReferenceAccount()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Account referenceA = portfolioA.getReferenceAccount();

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA), Arrays.asList(referenceA),
                        Map.of(portfolioA, HALF)).filter(client);

        Portfolio portfolio = result.getPortfolios().get(0);
        Account account = result.getAccounts().get(0);

        // the buy is split: common part at 50% (amount 50, half a share)
        assertThat(portfolio.getTransactions().stream() //
                        .filter(t -> t.getType() == PortfolioTransaction.Type.BUY) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(50)), is(true));
        // ... with a 50 removal in the (fully owned) reference account
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(50)), is(true));

        // dividend booked at the portfolio weight (5) + a balancing deposit (5)
        // so the fully-owned account keeps its cash but only 5 counts as income
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(5)), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(5)), is(true));
    }

    @Test
    public void testFullSecuritiesAccountWithReferenceAccountAtFiftyKeepsSecurityCashFlowAndCorrection()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Account referenceA = portfolioA.getReferenceAccount();

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA), Arrays.asList(referenceA),
                        Map.of(referenceA, HALF)).filter(client);

        Account account = result.getAccounts().get(0);

        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(10)), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(5)), is(true));
    }

    @Test
    public void testIncludedAccountWithMixedCandidatePortfolioWeightsUsesMaximumWithCorrection()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);
        Account referenceA = portfolioA.getReferenceAccount();

        portfolioB.setReferenceAccount(referenceA);

        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA, portfolioB), Arrays.asList(referenceA),
                        Map.of(referenceA, HALF, portfolioA, HALF, portfolioB, EIGHTY)).filter(client);

        Account account = result.getAccounts().get(0);

        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(8)), is(true));
        assertThat(account.getTransactions().stream() //
                        .filter(t -> t.getType() == AccountTransaction.Type.REMOVAL) //
                        .anyMatch(t -> t.getAmount() == Values.Amount.factorize(3)), is(true));
    }

    @Test
    public void testPortfolioTransferWithMismatchedWeightsCreatesDeliveryDelta()
    {
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setDate(LocalDateTime.parse("2016-04-01T00:00"));
        entry.setAmount(Values.Amount.factorize(10));
        entry.setShares(Values.Share.factorize(1));
        entry.setSecurity(client.getSecurities().get(0));
        entry.insert();

        // source A @ 100% -> target B @ 50%: linked transfer of half a share +
        // an outbound delivery of the excess half on the higher-weighted source
        Client result = new PortfolioClientFilter(Arrays.asList(portfolioA, portfolioB), Collections.emptyList(),
                        Map.of(portfolioB, HALF)).filter(client);

        Portfolio a = byName(result.getPortfolios(), "A");
        Portfolio b = byName(result.getPortfolios(), "B");

        assertThat(transferDate(a.getTransactions(), PortfolioTransaction.Type.TRANSFER_OUT,
                        Values.Share.factorize(0.5)), is(true));
        assertThat(transferDate(a.getTransactions(), PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                        Values.Share.factorize(0.5)), is(true));
        assertThat(transferDate(b.getTransactions(), PortfolioTransaction.Type.TRANSFER_IN,
                        Values.Share.factorize(0.5)), is(true));
        // target is not the higher-weighted side -> no inbound delivery delta
        assertThat(result.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00")))
                        .anyMatch(t -> t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND), is(false));

        // reverse direction A @ 50% -> target B @ 100%: inbound delivery delta
        // on B
        Client reverse = new PortfolioClientFilter(Arrays.asList(portfolioA, portfolioB), Collections.emptyList(),
                        Map.of(portfolioA, HALF)).filter(client);
        Portfolio bReverse = byName(reverse.getPortfolios(), "B");
        assertThat(transferDate(bReverse.getTransactions(), PortfolioTransaction.Type.DELIVERY_INBOUND,
                        Values.Share.factorize(0.5)), is(true));
    }

    private static Portfolio byName(List<Portfolio> portfolios, String name)
    {
        return portfolios.stream().filter(p -> name.equals(p.getName())).findFirst().orElseThrow();
    }

    private static boolean transferDate(List<PortfolioTransaction> transactions, PortfolioTransaction.Type type,
                    long shares)
    {
        return transactions.stream() //
                        .filter(t -> t.getType() == type) //
                        .filter(t -> t.getDateTime().equals(LocalDateTime.parse("2016-04-01T00:00"))) //
                        .anyMatch(t -> t.getShares() == shares);
    }
}
