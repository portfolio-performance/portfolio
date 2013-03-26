package name.abuchen.portfolio.model;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Calendar;

import name.abuchen.portfolio.util.Dates;

import org.junit.Before;
import org.junit.Test;

public class CrossEntryTest
{
    Client client;

    @Before
    public void createClient()
    {
        client = new Client();
        client.addAccount(new Account());
        client.addAccount(new Account());
        client.addPortfolio(new Portfolio());
        client.addPortfolio(new Portfolio());

        Security security = new Security();
        security.setName("Some security"); //$NON-NLS-1$
        client.addSecurity(security);
    }

    @Test
    public void testBuySellEntry()
    {
        Portfolio portfolio = client.getPortfolios().get(0);
        Account account = client.getAccounts().get(0);
        Security security = client.getSecurities().get(0);

        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setDate(Dates.today());
        entry.setSecurity(security);
        entry.setShares(1 * Values.Share.factor());
        entry.setFees(10);
        entry.setAmount(1000 * Values.Amount.factor());
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.insert();

        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));

        PortfolioTransaction pt = portfolio.getTransactions().get(0);
        AccountTransaction pa = account.getTransactions().get(0);

        assertThat(pt.getSecurity(), is(security));
        assertThat(pa.getSecurity(), is(security));
        assertThat(pt.getAmount(), is(pa.getAmount()));
        assertThat(pt.getDate(), is(Dates.today()));
        assertThat(pa.getDate(), is(Dates.today()));

        // check cross entity identification
        assertThat(entry.getCrossEntity(pt), is((Object) account));
        assertThat(entry.getCrossTransaction(pt), is((Transaction) pa));

        assertThat(entry.getCrossEntity(pa), is((Object) portfolio));
        assertThat(entry.getCrossTransaction(pa), is((Transaction) pt));

        // check cross editing
        pt.setAmount(2000 * Values.Amount.factor());
        entry.updateFrom(pt);
        assertThat(pa.getAmount(), is(pt.getAmount()));

        pa.setDate(Dates.date(2013, Calendar.MARCH, 16));
        entry.updateFrom(pa);
        assertThat(pt.getDate(), is(pa.getDate()));

        // check deletion
        entry.delete();
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(account.getTransactions().size(), is(0));
    }

    @Test
    public void testAccountTransferEntry()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        entry.setDate(Dates.today());
        entry.setAmount(1000 * Values.Amount.factor());
        entry.insert();

        assertThat(accountA.getTransactions().size(), is(1));
        assertThat(accountB.getTransactions().size(), is(1));

        AccountTransaction pA = accountA.getTransactions().get(0);
        AccountTransaction pB = accountB.getTransactions().get(0);

        assertThat(pA.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(pB.getType(), is(AccountTransaction.Type.TRANSFER_IN));

        assertThat(pA.getSecurity(), nullValue());
        assertThat(pB.getSecurity(), nullValue());
        assertThat(pA.getAmount(), is(pB.getAmount()));
        assertThat(pA.getDate(), is(Dates.today()));
        assertThat(pB.getDate(), is(Dates.today()));

        // check cross entity identification
        assertThat(entry.getCrossEntity(pA), is((Object) accountB));
        assertThat(entry.getCrossTransaction(pA), is((Transaction) pB));

        assertThat(entry.getCrossEntity(pB), is((Object) accountA));
        assertThat(entry.getCrossTransaction(pB), is((Transaction) pA));

        // check cross editing
        pA.setAmount(2000 * Values.Amount.factor());
        entry.updateFrom(pA);
        assertThat(pB.getAmount(), is(pA.getAmount()));

        pB.setDate(Dates.date(2013, Calendar.MARCH, 16));
        entry.updateFrom(pB);
        assertThat(pA.getDate(), is(pB.getDate()));

        // check deletion
        entry.delete();
        assertThat(accountA.getTransactions().size(), is(0));
        assertThat(accountB.getTransactions().size(), is(0));
    }

    @Test
    public void testPortoflioTransferEntry()
    {
        Security security = client.getSecurities().get(0);
        Portfolio portfolioA = client.getPortfolios().get(0);
        Portfolio portfolioB = client.getPortfolios().get(1);

        PortfolioTransferEntry entry = new PortfolioTransferEntry(portfolioA, portfolioB);
        entry.setDate(Dates.today());
        entry.setAmount(1000);
        entry.setSecurity(security);
        entry.setShares(1);
        entry.insert();

        assertThat(portfolioA.getTransactions().size(), is(1));
        assertThat(portfolioB.getTransactions().size(), is(1));

        PortfolioTransaction pA = portfolioA.getTransactions().get(0);
        PortfolioTransaction pB = portfolioB.getTransactions().get(0);

        assertThat(pA.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(pB.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));

        assertThat(pA.getSecurity(), is(security));
        assertThat(pB.getSecurity(), is(security));
        assertThat(pA.getAmount(), is(pB.getAmount()));
        assertThat(pA.getDate(), is(Dates.today()));
        assertThat(pB.getDate(), is(Dates.today()));

        // check cross entity identification
        assertThat(entry.getCrossEntity(pA), is((Object) portfolioB));
        assertThat(entry.getCrossTransaction(pA), is((Transaction) pB));

        assertThat(entry.getCrossEntity(pB), is((Object) portfolioA));
        assertThat(entry.getCrossTransaction(pB), is((Transaction) pA));

        // check cross editing
        pA.setAmount(2000);
        entry.updateFrom(pA);
        assertThat(pB.getAmount(), is(2000L));

        pA.setShares(2);
        entry.updateFrom(pA);
        assertThat(pB.getShares(), is(2L));

        pB.setDate(Dates.date(2013, Calendar.MARCH, 16));
        entry.updateFrom(pB);
        assertThat(pA.getDate(), is(pB.getDate()));

        // check deletion
        entry.delete();
        assertThat(portfolioA.getTransactions().size(), is(0));
        assertThat(portfolioB.getTransactions().size(), is(0));
    }
}
