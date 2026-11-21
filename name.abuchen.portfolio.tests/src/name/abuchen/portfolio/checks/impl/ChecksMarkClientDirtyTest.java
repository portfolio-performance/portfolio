package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientTestUtilities;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Checks which silently repair the client must mark the client as dirty -
 * otherwise neither the save nor the auto save picks up the repair. Checks
 * which find nothing to repair must not mark the client as dirty.
 */
@SuppressWarnings("nls")
public class ChecksMarkClientDirtyTest
{
    private Client client;
    private Account account;
    private Portfolio portfolio;
    private Security security;

    @Before
    public void setupClient()
    {
        client = new Client();
        account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        security = new Security("Security", CurrencyUnit.EUR);
        client.addSecurity(security);
    }

    @Test
    public void testCrossEntryCheckMarksDirtyIfEntriesAreMatched()
    {
        var date = LocalDateTime.now();
        portfolio.addTransaction(new PortfolioTransaction(date, CurrencyUnit.EUR, 1, security, 1,
                        PortfolioTransaction.Type.SELL, 1, 0));
        account.addTransaction(new AccountTransaction(date, CurrencyUnit.EUR, 1, security,
                        AccountTransaction.Type.SELL));

        assertThat(executeAndCountDirtyEvents(new CrossEntryCheck()), is(1));
        assertThat(portfolio.getTransactions().get(0).getCrossEntry(), notNullValue());
    }

    @Test
    public void testCrossEntryCheckDoesNotMarkDirtyIfNothingIsMatched()
    {
        insertBuySellEntry();

        assertThat(executeAndCountDirtyEvents(new CrossEntryCheck()), is(0));
    }

    @Test
    public void testDanglingAccountsCheckMarksDirtyIfAccountIsRecovered()
    {
        var dangling = new Account("Dangling");
        dangling.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(dangling);

        assertThat(executeAndCountDirtyEvents(new DanglingAccountsCheck()), is(1));
        assertThat(client.getAccounts(), hasItem(dangling));
    }

    @Test
    public void testDanglingAccountsCheckDoesNotMarkDirtyIfAccountsAreComplete()
    {
        insertBuySellEntry();

        assertThat(executeAndCountDirtyEvents(new DanglingAccountsCheck()), is(0));
    }

    @Test
    public void testFixDepositsWithSecurityCheckMarksDirtyIfSecurityIsRemoved()
    {
        var deposit = new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100, security,
                        AccountTransaction.Type.DEPOSIT);
        account.addTransaction(deposit);

        assertThat(executeAndCountDirtyEvents(new FixDepositsWithSecurityCheck()), is(1));
        assertThat(deposit.getSecurity(), nullValue());
    }

    @Test
    public void testFixDepositsWithSecurityCheckDoesNotMarkDirtyIfNothingIsFixed()
    {
        account.addTransaction(new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100, null,
                        AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100, security,
                        AccountTransaction.Type.DIVIDENDS));

        assertThat(executeAndCountDirtyEvents(new FixDepositsWithSecurityCheck()), is(0));
    }

    @Test
    public void testFixTaxRefundsCheckMarksDirtyIfUnknownSecurityIsRemoved()
    {
        ClientTestUtilities.setFileVersionAfterRead(client, 29);

        var refund = new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100,
                        new Security("Unknown", CurrencyUnit.EUR), AccountTransaction.Type.TAX_REFUND);
        account.addTransaction(refund);

        assertThat(executeAndCountDirtyEvents(new FixTaxRefundsCheck()), is(1));
        assertThat(refund.getSecurity(), nullValue());
    }

    @Test
    public void testFixTaxRefundsCheckDoesNotMarkDirtyIfSecurityIsKnown()
    {
        ClientTestUtilities.setFileVersionAfterRead(client, 29);

        account.addTransaction(new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100, security,
                        AccountTransaction.Type.TAX_REFUND));

        assertThat(executeAndCountDirtyEvents(new FixTaxRefundsCheck()), is(0));
    }

    @Test
    public void testFixWrongTransfersCheckMarksDirtyIfTransferIsReplaced()
    {
        var entry = insertBuySellEntry();
        entry.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_IN);

        assertThat(executeAndCountDirtyEvents(new FixWrongTransfersCheck()), is(1));
        assertThat(portfolio.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
    }

    @Test
    public void testFixWrongTransfersCheckDoesNotMarkDirtyIfNothingIsFixed()
    {
        insertBuySellEntry();

        assertThat(executeAndCountDirtyEvents(new FixWrongTransfersCheck()), is(0));
    }

    @Test
    public void testMissingUUIDCheckMarksDirtyIfUUIDIsAdded()
    {
        var withoutUUID = ClientTestUtilities.createSecurityWithoutUUID();
        client.addSecurity(withoutUUID);

        assertThat(executeAndCountDirtyEvents(new MissingUUIDCheck()), is(1));
        assertThat(withoutUUID.getUUID(), notNullValue());
    }

    @Test
    public void testMissingUUIDCheckDoesNotMarkDirtyIfAllSecuritiesHaveUUID()
    {
        assertThat(executeAndCountDirtyEvents(new MissingUUIDCheck()), is(0));
    }

    @Test
    public void testNullSecurityPricesCheckMarksDirtyIfNullPriceIsRemoved()
    {
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 10, 21), 100));
        ClientTestUtilities.addNullPrice(security);

        assertThat(executeAndCountDirtyEvents(new NullSecurityPricesCheck()), is(1));
        assertThat(security.getPrices().size(), is(1));
        assertThat(security.getPrices().get(0), notNullValue());
    }

    @Test
    public void testNullSecurityPricesCheckDoesNotMarkDirtyIfPricesAreValid()
    {
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 10, 21), 100));

        assertThat(executeAndCountDirtyEvents(new NullSecurityPricesCheck()), is(0));
    }

    @Test
    public void testExDateCheckMarksDirtyIfExDateIsRemoved()
    {
        var deposit = new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100, null,
                        AccountTransaction.Type.DEPOSIT);
        deposit.setExDate(LocalDateTime.now());
        account.addTransaction(deposit);

        assertThat(executeAndCountDirtyEvents(new OnlyAccountTransactionsWithSecurityCanHaveExDateCheck()), is(1));
        assertThat(deposit.getExDate(), nullValue());
    }

    @Test
    public void testExDateCheckDoesNotMarkDirtyIfExDateBelongsToSecurity()
    {
        var dividend = new AccountTransaction(LocalDateTime.now(), CurrencyUnit.EUR, 100, security,
                        AccountTransaction.Type.DIVIDENDS);
        dividend.setExDate(LocalDateTime.now());
        account.addTransaction(dividend);

        assertThat(executeAndCountDirtyEvents(new OnlyAccountTransactionsWithSecurityCanHaveExDateCheck()), is(0));
        assertThat(dividend.getExDate(), notNullValue());
    }

    private BuySellEntry insertBuySellEntry()
    {
        var entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.now());
        entry.setSecurity(security);
        entry.setShares(1);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(100);
        entry.insert();
        return entry;
    }

    private int executeAndCountDirtyEvents(Check check)
    {
        var count = new AtomicInteger();
        client.addPropertyChangeListener(event -> {
            if ("dirty".equals(event.getPropertyName()))
                count.incrementAndGet();
        });

        check.execute(client);

        return count.get();
    }
}
