package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Tests the client-level ledger container and its legacy owner-list boundary.
 * These tests make sure Ledger-V6 has one persisted transaction truth without changing direct legacy list behavior.
 */
@SuppressWarnings("nls")
public class ClientLedgerTest
{
    /**
     * Verifies that every new client owns an empty ledger.
     * New files must have a place for ledger truth before any transactions are created.
     */
    @Test
    public void testNewClientOwnsEmptyLedger()
    {
        var client = new Client();

        assertNotNull(client.getLedger());
        assertSame(client.getLedger(), client.getLedger());
        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    /**
     * Verifies that post-load initialization restores a missing ledger field.
     * Old or malformed serialized clients must not leave the application without a ledger container.
     */
    @Test
    public void testPostLoadInitializationRestoresNullLedger() throws ReflectiveOperationException
    {
        var client = new Client();
        var originalLedger = client.getLedger();
        var field = Client.class.getDeclaredField("ledger");

        field.setAccessible(true);
        field.set(client, null);

        client.doPostLoadInitialization();

        assertNotNull(client.getLedger());
        assertNotSame(originalLedger, client.getLedger());
        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    /**
     * Verifies that the client ledger field is part of persistence.
     * The ledger must be saved as the source of truth rather than treated as runtime-only state.
     */
    @Test
    public void testLedgerFieldIsPersisted() throws NoSuchFieldException
    {
        var field = Client.class.getDeclaredField("ledger");

        assertFalse(java.lang.reflect.Modifier.isTransient(field.getModifiers()));
    }

    /**
     * Verifies that legacy owner lists remain unchanged when rows are added directly.
     * Adding legacy transactions must not silently populate ledger truth.
     */
    @Test
    public void testLegacyTransactionListsRemainUnchangedAndDoNotPopulateLedger()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Some security", CurrencyUnit.EUR);
        var accountTransaction = new AccountTransaction();
        var portfolioTransaction = new PortfolioTransaction();

        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
        accountTransaction.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        accountTransaction.setCurrencyCode(CurrencyUnit.EUR);
        accountTransaction.setAmount(100L);
        account.addTransaction(accountTransaction);

        portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        portfolioTransaction.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        portfolioTransaction.setSecurity(security);
        portfolioTransaction.setShares(1000L);
        portfolio.addTransaction(portfolioTransaction);

        assertThat(account.getTransactions(), is(List.of(accountTransaction)));
        assertThat(portfolio.getTransactions(), is(List.of(portfolioTransaction)));
        assertTrue(client.getLedger().getEntries().isEmpty());
    }
}
