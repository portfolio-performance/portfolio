package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.time.LocalDate;
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

        pa.setSource("some-source"); //$NON-NLS-1$
        entry.updateFrom(pa);
        assertThat(pt.getSource(), is(pa.getSource()));

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

        pA.setSource("some-source"); //$NON-NLS-1$
        entry.updateFrom(pA);
        assertThat(pB.getSource(), is(pA.getSource()));

        pB.setDateTime(LocalDateTime.of(2013, Month.MARCH, 16, 0, 0));
        entry.updateFrom(pB);
        assertThat(pA.getDateTime(), is(pB.getDateTime()));

        // check deletion
        accountA.deleteTransaction(pA, client);
        assertThat(accountA.getTransactions().size(), is(0));
        assertThat(accountB.getTransactions().size(), is(0));
    }

    @Test
    public void testAccountTransferEntryWithDifferentCurrencies()
    {
        Account accountA = client.getAccounts().get(0);
        Account accountB = client.getAccounts().get(1);

        accountA.setCurrencyCode(CurrencyUnit.EUR);
        accountB.setCurrencyCode(CurrencyUnit.USD);

        // Verify accounts are empty before attempt
        assertThat(accountA.getTransactions().size(), is(0));
        assertThat(accountB.getTransactions().size(), is(0));

        AccountTransferEntry entry = new AccountTransferEntry(accountA, accountB);
        LocalDateTime date = LocalDateTime.now();
        entry.setDate(date);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(1000 * Values.Amount.factor());

        try
        {
            entry.insert();
            fail("Expected IllegalArgumentException due to currency mismatch"); //$NON-NLS-1$
        }
        catch (IllegalArgumentException e)
        {
            assertThat(accountA.getTransactions().size(), is(0));
            assertThat(accountB.getTransactions().size(), is(0));
        }
    }

    @Test
    public void testBuySellEntryWithCurrencyMismatch()
    {
        Portfolio portfolio = client.getPortfolios().get(0);
        Account account = client.getAccounts().get(0);
        Security security = client.getSecurities().get(0);

        account.setCurrencyCode(CurrencyUnit.USD);

        // Verify portfolio and account are empty before attempt
        assertThat(portfolio.getTransactions().size(), is(0));
        assertThat(account.getTransactions().size(), is(0));

        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        LocalDateTime date = LocalDateTime.now();
        entry.setDate(date);
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(1));
        entry.setAmount(Values.Amount.factorize(1000));
        entry.setType(PortfolioTransaction.Type.BUY);

        try
        {
            entry.insert();
            fail("Expected IllegalArgumentException due to currency mismatch"); //$NON-NLS-1$
        }
        catch (IllegalArgumentException e)
        {
            assertThat(portfolio.getTransactions().size(), is(0));
            assertThat(account.getTransactions().size(), is(0));
        }
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

        pA.setSource("some-source"); //$NON-NLS-1$
        entry.updateFrom(pA);
        assertThat(pB.getSource(), is(pA.getSource()));

        pB.setDateTime(LocalDateTime.of(2013, Month.MARCH, 16, 0, 0));
        entry.updateFrom(pB);
        assertThat(pA.getDateTime(), is(pB.getDateTime()));

        // check deletion
        portfolioA.deleteTransaction(pA, client);
        assertThat(portfolioA.getTransactions().size(), is(0));
        assertThat(portfolioB.getTransactions().size(), is(0));
    }

    @Test
    public void testFundTransferEntry()
    {
        Security sourceSecurity = client.getSecurities().get(0);
        Security targetSecurity = new Security();
        targetSecurity.setName("Target security");
        client.addSecurity(targetSecurity);

        Portfolio sourcePortfolio = client.getPortfolios().get(0);
        Portfolio targetPortfolio = client.getPortfolios().get(1);

        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.of(2021, Month.JUNE, 1, 0, 0));
        entry.setSourceSecurity(sourceSecurity);
        entry.setTargetSecurity(targetSecurity);
        entry.setSourceShares(Values.Share.factorize(5));
        entry.setTargetShares(Values.Share.factorize(10));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750)));
        entry.addCarriedLot(new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                        Values.Share.factorize(5), Values.Share.factorize(10),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500)), "source-transaction-uuid"));
        entry.insert();

        assertThat(sourcePortfolio.getTransactions().size(), is(1));
        assertThat(targetPortfolio.getTransactions().size(), is(1));

        PortfolioTransaction sourceTx = sourcePortfolio.getTransactions().get(0);
        PortfolioTransaction targetTx = targetPortfolio.getTransactions().get(0);

        assertThat(sourceTx.getType(), is(PortfolioTransaction.Type.FUND_TRANSFER_OUT));
        assertThat(targetTx.getType(), is(PortfolioTransaction.Type.FUND_TRANSFER_IN));
        assertThat(sourceTx.getSecurity(), is(sourceSecurity));
        assertThat(targetTx.getSecurity(), is(targetSecurity));
        assertThat(sourceTx.getShares(), is(Values.Share.factorize(5)));
        assertThat(targetTx.getShares(), is(Values.Share.factorize(10)));
        assertThat(sourceTx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750))));
        assertThat(targetTx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750))));

        assertThat(entry.getCrossOwner(sourceTx), is((Object) targetPortfolio));
        assertThat(entry.getCrossTransaction(sourceTx), is((Transaction) targetTx));
        assertThat(entry.getCrossOwner(targetTx), is((Object) sourcePortfolio));
        assertThat(entry.getCrossTransaction(targetTx), is((Transaction) sourceTx));

        assertThat(entry.getCarriedLots().get(0).getAcquisitionDate(), is(LocalDate.parse("2020-01-01")));
        assertThat(entry.getCarriedLots().get(0).getSourceShares(), is(Values.Share.factorize(5)));
        assertThat(entry.getCarriedLots().get(0).getTargetShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getCarriedLots().get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        sourceTx.setNote("Test");
        entry.updateFrom(sourceTx);
        assertThat(targetTx.getNote(), is(sourceTx.getNote()));

        targetPortfolio.deleteTransaction(targetTx, client);
        assertThat(sourcePortfolio.getTransactions().size(), is(0));
        assertThat(targetPortfolio.getTransactions().size(), is(0));
    }
}
