package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests transaction context menu behavior for legacy and ledger-backed rows.
 * These tests make sure batch actions delete whole ledger entries instead of only one visible runtime row.
 */
@SuppressWarnings("nls")
public class TransactionContextMenuTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);

    /**
     * Checks the UI scenario: ledger backed buy/sell selection supports convert to delivery menu action.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLedgerBackedBuySellSelectionSupportsConvertToDeliveryMenuAction()
    {
        var fixture = fixture();

        new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(),
                        PortfolioTransaction.Type.BUY, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        fixture.security(), Values.Share.factorize(5), List.of(), "note", "source");

        var transaction = fixture.portfolio().getTransactions().get(0);

        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(
                        List.of(new TransactionPair<>(fixture.portfolio(), transaction))), is(true));
        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(
                        List.of(new TransactionPair<>(fixture.portfolio(), transaction))), is(false));
    }

    /**
     * Checks the UI scenario: ledger backed delivery selection supports convert to buy/sell menu action.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLedgerBackedDeliverySelectionSupportsConvertToBuySellMenuAction()
    {
        var fixture = fixture();

        new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND, DATE_TIME, Values.Amount.factorize(123),
                        CurrencyUnit.EUR, fixture.security(), Values.Share.factorize(5), null, null, List.of(), "note",
                        "source");

        var transaction = fixture.portfolio().getTransactions().get(0);

        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(
                        List.of(new TransactionPair<>(fixture.portfolio(), transaction))), is(true));
        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(
                        List.of(new TransactionPair<>(fixture.portfolio(), transaction))), is(false));
    }

    /**
     * Checks the UI scenario: mixed security selection does not expose conversion menu actions.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testMixedSecuritySelectionDoesNotExposeConversionMenuActions()
    {
        var fixture = fixture();

        new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(),
                        PortfolioTransaction.Type.BUY, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        fixture.security(), Values.Share.factorize(5), List.of(), "note", "source");
        new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND, DATE_TIME.plusDays(1),
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), null, null, List.of(), "note", "source");

        var pairs = fixture.portfolio().getTransactions().stream() //
                        .map(transaction -> new TransactionPair<>(fixture.portfolio(), transaction)) //
                        .toList();

        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(pairs), is(false));
        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(pairs), is(false));
    }

    /**
     * Checks the UI scenario: ledger backed buy/sell selection delete removes ledger entry once.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLedgerBackedBuySellSelectionDeleteRemovesLedgerEntryOnce() throws Exception
    {
        var fixture = fixture();
        var entry = new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(),
                        PortfolioTransaction.Type.BUY, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        fixture.security(), Values.Share.factorize(5), List.of(), "note", "source");
        var portfolioPair = new TransactionPair<>(fixture.portfolio(), entry.getPortfolioTransaction());
        var accountPair = new TransactionPair<>(fixture.account(), entry.getAccountTransaction());

        assertThat(portfolioPair.getTransaction().getUUID().equals(accountPair.getTransaction().getUUID()), is(false));
        assertThat(portfolioPair.getLedgerEntryUUID(), is(accountPair.getLedgerEntryUUID()));

        TransactionContextMenu.deleteTransactions(fixture.client(), new Object[] { portfolioPair, accountPair });

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertTrue(fixture.portfolio().getTransactions().isEmpty());
        assertThat(reloadXml(fixture.client()).getAllTransactions().size(), is(0));
    }

    /**
     * Checks the UI scenario: ledger backed account transfer selection delete removes ledger entry once.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLedgerBackedAccountTransferSelectionDeleteRemovesLedgerEntryOnce() throws Exception
    {
        var fixture = accountTransferFixture();
        var transfer = new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.source(),
                        fixture.target(), DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null, "note", "source");
        var sourcePair = new TransactionPair<>(fixture.source(), transfer.getSourceTransaction());
        var targetPair = new TransactionPair<>(fixture.target(), transfer.getTargetTransaction());

        assertThat(sourcePair.getTransaction().getUUID().equals(targetPair.getTransaction().getUUID()), is(false));
        assertThat(sourcePair.getLedgerEntryUUID(), is(targetPair.getLedgerEntryUUID()));

        TransactionContextMenu.deleteTransactions(fixture.client(), new Object[] { sourcePair, targetPair });

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
        assertThat(reloadXml(fixture.client()).getAllTransactions().size(), is(0));
    }

    /**
     * Checks the UI scenario: ledger backed portfolio transfer selection delete removes ledger entry once.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLedgerBackedPortfolioTransferSelectionDeleteRemovesLedgerEntryOnce() throws Exception
    {
        var fixture = portfolioTransferFixture();
        var transfer = new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.source(),
                        fixture.target(), fixture.security(), DATE_TIME, Values.Share.factorize(5),
                        Values.Amount.factorize(123), CurrencyUnit.EUR, "note", "source");
        var sourcePair = new TransactionPair<>(fixture.source(), transfer.getSourceTransaction());
        var targetPair = new TransactionPair<>(fixture.target(), transfer.getTargetTransaction());

        assertThat(sourcePair.getTransaction().getUUID().equals(targetPair.getTransaction().getUUID()), is(false));
        assertThat(sourcePair.getLedgerEntryUUID(), is(targetPair.getLedgerEntryUUID()));

        TransactionContextMenu.deleteTransactions(fixture.client(), new Object[] { sourcePair, targetPair });

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
        assertThat(reloadXml(fixture.client()).getAllTransactions().size(), is(0));
    }

    /**
     * Checks the UI scenario: mixed selection deduplicates ledger entries and deletes legacy transactions.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testMixedSelectionDeduplicatesLedgerEntriesAndDeletesLegacyTransactions() throws Exception
    {
        var fixture = fixture();
        var firstLedgerEntry = new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        fixture.account(), PortfolioTransaction.Type.BUY, DATE_TIME, Values.Amount.factorize(123),
                        CurrencyUnit.EUR, fixture.security(), Values.Share.factorize(5), List.of(), "note",
                        "source");
        var secondLedgerEntry = new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        fixture.account(), PortfolioTransaction.Type.BUY, DATE_TIME.plusDays(1),
                        Values.Amount.factorize(456), CurrencyUnit.EUR, fixture.security(), Values.Share.factorize(7),
                        List.of(), "note", "source");
        var legacyTransaction = new AccountTransaction();

        legacyTransaction.setType(AccountTransaction.Type.DEPOSIT);
        legacyTransaction.setDateTime(DATE_TIME.plusDays(2));
        legacyTransaction.setAmount(Values.Amount.factorize(99));
        legacyTransaction.setCurrencyCode(CurrencyUnit.EUR);
        fixture.account().addTransaction(legacyTransaction);

        TransactionContextMenu.deleteTransactions(fixture.client(),
                        new Object[] { new TransactionPair<>(fixture.portfolio(),
                                        firstLedgerEntry.getPortfolioTransaction()),
                                        new TransactionPair<>(fixture.account(),
                                                        firstLedgerEntry.getAccountTransaction()),
                                        new TransactionPair<>(fixture.portfolio(),
                                                        secondLedgerEntry.getPortfolioTransaction()),
                                        new TransactionPair<>(fixture.account(), legacyTransaction) });

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertTrue(fixture.portfolio().getTransactions().isEmpty());
        assertThat(reloadXml(fixture.client()).getAllTransactions().size(), is(0));
    }

    /**
     * Checks the UI scenario: ledger backed account only selection delete removes ledger entry.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLedgerBackedAccountOnlySelectionDeleteRemovesLedgerEntry() throws Exception
    {
        var fixture = fixture();
        var transaction = new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.account(),
                        AccountTransaction.Type.FEES, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, null,
                        List.of(), "note", "source");

        TransactionContextMenu.deleteTransactions(fixture.client(),
                        new Object[] { new TransactionPair<>(fixture.account(), transaction) });

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertThat(reloadXml(fixture.client()).getAllTransactions().size(), is(0));
    }

    /**
     * Checks the UI scenario: stale ledger backed pair still fails outside selection dedupe.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testStaleLedgerBackedPairStillFailsOutsideSelectionDedupe()
    {
        var fixture = fixture();
        var entry = new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(),
                        PortfolioTransaction.Type.BUY, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        fixture.security(), Values.Share.factorize(5), List.of(), "note", "source");
        var accountPair = new TransactionPair<>(fixture.account(), entry.getAccountTransaction());
        var portfolioPair = new TransactionPair<>(fixture.portfolio(), entry.getPortfolioTransaction());

        portfolioPair.deleteTransaction(fixture.client());

        assertThrows(IllegalArgumentException.class, () -> accountPair.deleteTransaction(fixture.client()));
    }

    /**
     * Checks the UI scenario: legacy cross entry selection delete remains unchanged.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLegacyCrossEntrySelectionDeleteRemainsUnchanged()
    {
        var fixture = fixture();
        var entry = new BuySellEntry(fixture.portfolio(), fixture.account());

        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(DATE_TIME);
        entry.setSecurity(fixture.security());
        entry.setShares(Values.Share.factorize(5));
        entry.setAmount(Values.Amount.factorize(123));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.insert();

        TransactionContextMenu.deleteTransactions(fixture.client(),
                        new Object[] { new TransactionPair<>(fixture.portfolio(), entry.getPortfolioTransaction()),
                                        new TransactionPair<>(fixture.account(), entry.getAccountTransaction()) });

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertTrue(fixture.portfolio().getTransactions().isEmpty());
    }

    /**
     * Checks the UI scenario: legacy account transfer selection delete remains unchanged.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLegacyAccountTransferSelectionDeleteRemainsUnchanged()
    {
        var fixture = accountTransferFixture();
        var transfer = new AccountTransferEntry(fixture.source(), fixture.target());

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(123));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();

        TransactionContextMenu.deleteTransactions(fixture.client(),
                        new Object[] { new TransactionPair<>(fixture.source(), transfer.getSourceTransaction()),
                                        new TransactionPair<>(fixture.target(), transfer.getTargetTransaction()) });

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
    }

    /**
     * Checks the UI scenario: legacy portfolio transfer selection delete remains unchanged.
     * The visible transaction row must stay consistent with the ledger entry.
     * This protects restored master behavior from falling back to legacy mutation.
     */
    @Test
    public void testLegacyPortfolioTransferSelectionDeleteRemainsUnchanged()
    {
        var fixture = portfolioTransferFixture();
        var transfer = new PortfolioTransferEntry(fixture.source(), fixture.target());

        transfer.setDate(DATE_TIME);
        transfer.setSecurity(fixture.security());
        transfer.setShares(Values.Share.factorize(5));
        transfer.setAmount(Values.Amount.factorize(123));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();

        TransactionContextMenu.deleteTransactions(fixture.client(),
                        new Object[] { new TransactionPair<>(fixture.source(), transfer.getSourceTransaction()),
                                        new TransactionPair<>(fixture.target(), transfer.getTargetTransaction()) });

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
    }

    private Client reloadXml(Client client) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(saveXml(client).getBytes(StandardCharsets.UTF_8)));
    }

    private String saveXml(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-pair-delete", ".xml");

        try
        {
            ClientFactory.save(client, file);
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Fixture fixture()
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    private AccountTransferFixture accountTransferFixture()
    {
        var client = new Client();
        var source = new Account("Source");
        var target = new Account("Target");

        source.setCurrencyCode(CurrencyUnit.EUR);
        target.setCurrencyCode(CurrencyUnit.EUR);

        client.addAccount(source);
        client.addAccount(target);

        return new AccountTransferFixture(client, source, target);
    }

    private PortfolioTransferFixture portfolioTransferFixture()
    {
        var client = new Client();
        var source = new Portfolio("Source");
        var target = new Portfolio("Target");
        var security = new Security("Security", CurrencyUnit.EUR);

        client.addPortfolio(source);
        client.addPortfolio(target);
        client.addSecurity(security);

        return new PortfolioTransferFixture(client, source, target, security);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record AccountTransferFixture(Client client, Account source, Account target)
    {
    }

    private record PortfolioTransferFixture(Client client, Portfolio source, Portfolio target, Security security)
    {
    }
}
