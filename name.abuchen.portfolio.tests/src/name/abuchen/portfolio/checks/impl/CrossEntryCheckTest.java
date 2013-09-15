package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.util.Dates;

import org.junit.Before;
import org.junit.Test;

public class CrossEntryCheckTest
{
    private Client client;
    private Account account;
    private Portfolio portfolio;
    private Security security;

    @Before
    public void setupClient()
    {
        client = new Client();
        account = new Account();
        client.addAccount(account);
        portfolio = new Portfolio();
        client.addPortfolio(portfolio);
        security = new Security();
        client.addSecurity(security);
    }

    @Test
    public void testEmptyClient()
    {
        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
    }

    @Test
    public void testMissingSellInAccountIssue()
    {
        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.BUY, 1, 1, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));

        applyFixes(client, issues);
    }

    @Test
    public void testMissingBuyInAccountIssue()
    {
        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.SELL, 1, 1, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));

        applyFixes(client, issues);
    }

    @Test
    public void testThatCorrectBuySellEntriesAreNotReported()
    {
        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(Dates.today());
        entry.setSecurity(security);
        entry.setShares(1);
        entry.setAmount(100);
        entry.insert();

        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
    }

    @Test
    public void testThatMatchingBuySellEntriesAreFixed()
    {
        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.SELL, 1, 1, 1));

        account.addTransaction(new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.SELL, 1));

        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
        assertThat(portfolio.getTransactions().get(0).getCrossEntry(), notNullValue());
        assertThat(account.getTransactions().get(0).getCrossEntry(), notNullValue());
    }

    @Test
    public void testThatAlmostMatchingBuySellEntriesAreNotMatched()
    {
        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.SELL, 1, 1, 1));

        account.addTransaction(new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.SELL, 2));

        List<Issue> issues = new CrossEntryCheck().execute(client);
        assertThat(issues.size(), is(2));
        List<Object> objects = new ArrayList<Object>(issues);
        assertThat(objects, hasItem(instanceOf(MissingBuySellAccountIssue.class)));
        assertThat(objects, hasItem(instanceOf(MissingBuySellPortfolioIssue.class)));

        applyFixes(client, issues);
    }

    @Test
    public void testMissingAccountTransferOutIssue()
    {
        account.addTransaction(new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.TRANSFER_IN, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingAccountTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) account));

        applyFixes(client, issues);
    }

    @Test
    public void testMissingAccountTransferInIssue()
    {
        account.addTransaction(new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.TRANSFER_OUT, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingAccountTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) account));

        applyFixes(client, issues);
    }

    @Test
    public void testThatNotTheSameAccountIsMatched()
    {
        Account second = new Account();
        client.addAccount(second);

        account.addTransaction(new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.TRANSFER_IN, 2));

        AccountTransaction umatched = new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.TRANSFER_OUT, 2);
        account.addTransaction(umatched);

        second.addTransaction(new AccountTransaction(Dates.today(), security, //
                        AccountTransaction.Type.TRANSFER_OUT, 2));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingAccountTransferIssue.class)));

        assertThat(account.getTransactions(), hasItem(umatched));
        assertThat(second.getTransactions().get(0).getCrossEntry(), notNullValue());
        assertThat(second.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        applyFixes(client, issues);
    }

    @Test
    public void testMissingPortfolioTransferOutIssue()
    {
        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.TRANSFER_IN, 1, 1, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingPortfolioTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));

        applyFixes(client, issues);
    }

    @Test
    public void testMissingPortfolioTransferInIssue()
    {
        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.TRANSFER_OUT, 1, 1, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingPortfolioTransferIssue.class)));
        assertThat(issues.get(0).getEntity(), is((Object) portfolio));

        applyFixes(client, issues);
    }

    @Test
    public void testThatNotTheSamePortfolioIsMatched()
    {
        Portfolio second = new Portfolio();
        client.addPortfolio(second);

        portfolio.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.TRANSFER_IN, 1, 3, 1));

        PortfolioTransaction umatched = new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.TRANSFER_OUT, 1, 3, 1);
        portfolio.addTransaction(umatched);

        second.addTransaction(new PortfolioTransaction(Dates.today(), security, //
                        PortfolioTransaction.Type.TRANSFER_OUT, 1, 3, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0), is(instanceOf(MissingPortfolioTransferIssue.class)));

        assertThat(portfolio.getTransactions(), hasItem(umatched));
        assertThat(second.getTransactions().get(0).getCrossEntry(), notNullValue());
        assertThat(second.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));

        applyFixes(client, issues);
    }

    @Test
    public void testThatAccountTransactionsWithoutSecurity()
    {
        Portfolio second = new Portfolio();
        client.addPortfolio(second);

        account.addTransaction(new AccountTransaction(Dates.today(), null, //
                        AccountTransaction.Type.BUY, 1));

        List<Issue> issues = new CrossEntryCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0).getAvailableFixes().get(0), is(instanceOf(DeleteTransactionFix.class)));

        applyFixes(client, issues);

        ClientSnapshot.create(client, Dates.today());
    }

    private void applyFixes(Client client, List<Issue> issues)
    {
        for (Issue issue : issues)
        {
            List<QuickFix> fixes = issue.getAvailableFixes();
            assertThat(fixes.isEmpty(), is(false));
            fixes.get(0).execute();
        }
        assertThat(new CrossEntryCheck().execute(client).size(), is(0));
    }
}
