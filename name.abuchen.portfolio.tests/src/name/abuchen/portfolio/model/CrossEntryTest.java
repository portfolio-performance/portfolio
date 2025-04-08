package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.time.Month;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

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
        entry.setCurrencyCode(CurrencyUnit.EUR);
        LocalDateTime date = LocalDateTime.now();
        entry.setDate(date);
        entry.setSecurity(security);
        entry.setShares(1 * Values.Share.factor());
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, 10)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, 11)));
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
        assertThat(pt.getDateTime(), is(date));
        assertThat(pa.getDateTime(), is(date));

        assertThat(pt.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 10L)));
        assertThat(pt.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 11L)));

        // check cross entity identification
        assertThat(entry.getCrossOwner(pt), is((Object) account));
        assertThat(entry.getCrossTransaction(pt), is((Transaction) pa));

        assertThat(entry.getCrossOwner(pa), is((Object) portfolio));
        assertThat(entry.getCrossTransaction(pa), is((Transaction) pt));

        // check cross editing
        pa.setDateTime(LocalDateTime.of(2013, Month.MARCH, 16, 0, 0));
        entry.updateFrom(pa);
        assertThat(pt.getDateTime(), is(pa.getDateTime()));

        // check deletion
        portfolio.deleteTransaction(pt, client);
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(account.getTransactions().size(), is(0));
    }

    @Test
    public void testAccountTransferEntry()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        LocalDateTime date = LocalDateTime.now();
        entry.setDate(date);
        entry.setCurrencyCode(CurrencyUnit.EUR);
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
        assertThat(pA.getDateTime(), is(date));
        assertThat(pB.getDateTime(), is(date));

        // check cross entity identification
        assertThat(entry.getCrossOwner(pA), is((Object) accountB));
        assertThat(entry.getCrossTransaction(pA), is((Transaction) pB));

        assertThat(entry.getCrossOwner(pB), is((Object) accountA));
        assertThat(entry.getCrossTransaction(pB), is((Transaction) pA));

        // check cross editing
        pA.setNote("Test"); //$NON-NLS-1$
        entry.updateFrom(pA);
        assertThat(pB.getNote(), is(pA.getNote()));

        pB.setDateTime(LocalDateTime.of(2013, Month.MARCH, 16, 0, 0));
        entry.updateFrom(pB);
        assertThat(pA.getDateTime(), is(pB.getDateTime()));

        // check deletion
        accountA.deleteTransaction(pA, client);
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
        entry.setCurrencyCode(CurrencyUnit.EUR);
        LocalDateTime date = LocalDateTime.now();
        entry.setDate(date);
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
        assertThat(pA.getDateTime(), is(date));
        assertThat(pB.getDateTime(), is(date));

        // check cross entity identification
        assertThat(entry.getCrossOwner(pA), is((Object) portfolioB));
        assertThat(entry.getCrossTransaction(pA), is((Transaction) pB));

        assertThat(entry.getCrossOwner(pB), is((Object) portfolioA));
        assertThat(entry.getCrossTransaction(pB), is((Transaction) pA));

        // check cross editing
        pA.setShares(2);
        entry.updateFrom(pA);
        assertThat(pB.getShares(), is(2L));

        pB.setDateTime(LocalDateTime.of(2013, Month.MARCH, 16, 0, 0));
        entry.updateFrom(pB);
        assertThat(pA.getDateTime(), is(pB.getDateTime()));

        // check deletion
        portfolioA.deleteTransaction(pA, client);
        assertThat(portfolioA.getTransactions().size(), is(0));
        assertThat(portfolioB.getTransactions().size(), is(0));
    }
}
