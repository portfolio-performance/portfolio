package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

/**
 * Tests duplicate and copy behavior for ledger-backed transactions.
 * These tests make sure copied bookings get fresh ledger truth and do not share runtime projections with the source.
 */
@SuppressWarnings("nls")
public class LedgerTransactionDuplicateCopyParityTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final long AMOUNT = Values.Amount.factorize(123);
    private static final long SHARES = Values.Share.factorize(12);

    /**
     * Verifies that duplicating a ledger-backed deposit creates a fresh ledger entry.
     * The copy must not reuse posting or projection identifiers from the source booking.
     */
    @Test
    public void testDepositDuplicateCreatesFreshLedgerTruth() throws Exception
    {
        var fixture = fixture();
        var original = new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.account(),
                        AccountTransaction.Type.DEPOSIT, DATE_TIME, AMOUNT, CurrencyUnit.EUR, null, List.of(), "note",
                        "source");
        var originalSnapshot = EntrySnapshot.of(original);

        var model = new AccountTransactionModel(fixture.client(), AccountTransaction.Type.DEPOSIT);
        model.presetFromSource(fixture.account(), original);
        model.applyChanges();

        var duplicate = onlyOther(fixture.account().getTransactions(), original);

        assertAccountProjectionDuplicate(original, duplicate, originalSnapshot, 1, 1);
        assertThat(fixture.account().getTransactions().size(), is(2));
        assertThat(duplicate.getDateTime(), is(DATE_TIME));
        assertThat(duplicate.getNote(), is("note"));
        assertThat(duplicate.getSource(), nullValue());
        assertThat(duplicate.getAmount(), is(AMOUNT));
        assertThat(duplicate.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertNoDuplicateProjectionUUIDs(fixture.client());
        assertRoundtrips(fixture.client(), originalSnapshot.entryUUID(), uuid(entry(duplicate)), 2);
    }

    /**
     * Verifies that duplicating a ledger-backed dividend creates a fresh ledger entry.
     * Dividend facts such as security, shares, and ex-date must be copied without sharing ledger identity.
     */
    @Test
    public void testDividendDuplicateCreatesFreshLedgerTruth() throws Exception
    {
        var fixture = fixture();
        var original = new LedgerDividendTransactionCreator(fixture.client()).create(fixture.account(), DATE_TIME,
                        AMOUNT, CurrencyUnit.EUR, fixture.security(), SHARES, EX_DATE, null, null, List.of(), "note",
                        "source");
        var originalSnapshot = EntrySnapshot.of(original);

        var model = new AccountTransactionModel(fixture.client(), AccountTransaction.Type.DIVIDENDS);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.presetFromSource(fixture.account(), original);
        model.applyChanges();

        var duplicate = onlyOther(fixture.account().getTransactions(), original);

        assertAccountProjectionDuplicate(original, duplicate, originalSnapshot, 1, 1);
        assertThat(fixture.account().getTransactions().size(), is(2));
        assertThat(duplicate.getDateTime(), is(DATE_TIME));
        assertThat(duplicate.getNote(), is("note"));
        assertThat(duplicate.getSource(), nullValue());
        assertSame(fixture.security(), duplicate.getSecurity());
        assertThat(duplicate.getShares(), is(SHARES));
        assertThat(duplicate.getExDate(), is(EX_DATE));
        assertThat(duplicate.getAmount(), is(AMOUNT));
        assertNoDuplicateProjectionUUIDs(fixture.client());
        assertRoundtrips(fixture.client(), originalSnapshot.entryUUID(), uuid(entry(duplicate)), 2);
    }

    /**
     * Verifies that duplicating a ledger-backed buy creates a fresh ledger entry and cross entry.
     * The account and portfolio sides of the copy must point to each other, not to the original booking.
     */
    @Test
    public void testBuyDuplicateCreatesFreshLedgerTruthAndCrossEntry() throws Exception
    {
        var fixture = fixture();
        var original = new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        fixture.account(), PortfolioTransaction.Type.BUY, DATE_TIME, AMOUNT, CurrencyUnit.EUR,
                        fixture.security(), SHARES, List.of(), "note", "source");
        var originalPortfolioTransaction = original.getPortfolioTransaction();
        var originalAccountTransaction = original.getAccountTransaction();
        var originalSnapshot = EntrySnapshot.of(originalPortfolioTransaction);

        var model = new BuySellModel(fixture.client(), PortfolioTransaction.Type.BUY);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.presetFromSource(original);
        model.applyChanges();

        var duplicateAccountTransaction = onlyOther(fixture.account().getTransactions(), originalAccountTransaction);
        var duplicatePortfolioTransaction = onlyOther(fixture.portfolio().getTransactions(), originalPortfolioTransaction);

        assertPortfolioProjectionDuplicate(originalPortfolioTransaction, duplicatePortfolioTransaction, originalSnapshot,
                        2, 2);
        assertThat(fixture.account().getTransactions().size(), is(2));
        assertThat(fixture.portfolio().getTransactions().size(), is(2));
        assertSame(duplicatePortfolioTransaction,
                        duplicateAccountTransaction.getCrossEntry().getCrossTransaction(duplicateAccountTransaction));
        assertSame(originalPortfolioTransaction,
                        originalAccountTransaction.getCrossEntry().getCrossTransaction(originalAccountTransaction));
        assertThat(duplicatePortfolioTransaction, not(originalPortfolioTransaction));
        assertThat(duplicateAccountTransaction, not(originalAccountTransaction));
        assertThat(duplicatePortfolioTransaction.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(duplicatePortfolioTransaction.getDateTime(), is(DATE_TIME));
        assertThat(duplicatePortfolioTransaction.getNote(), is("note"));
        assertThat(duplicatePortfolioTransaction.getSource(), nullValue());
        assertSame(fixture.security(), duplicatePortfolioTransaction.getSecurity());
        assertThat(duplicatePortfolioTransaction.getShares(), is(SHARES));
        assertThat(duplicatePortfolioTransaction.getAmount(), is(AMOUNT));
        assertNoDuplicateProjectionUUIDs(fixture.client());
        assertRoundtrips(fixture.client(), originalSnapshot.entryUUID(), uuid(entry(duplicatePortfolioTransaction)),
                        2);
    }

    /**
     * Verifies that duplicating a ledger-backed inbound delivery creates a fresh ledger entry.
     * The copy must preserve delivery facts while using new ledger and projection identifiers.
     */
    @Test
    public void testDeliveryInboundDuplicateCreatesFreshLedgerTruth() throws Exception
    {
        var fixture = fixture();
        var original = new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND, DATE_TIME, AMOUNT, CurrencyUnit.EUR,
                        fixture.security(), SHARES, null, null, List.of(), "note", "source");
        var originalSnapshot = EntrySnapshot.of(original);

        var model = new SecurityDeliveryModel(fixture.client(), PortfolioTransaction.Type.DELIVERY_INBOUND);
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.presetFromSource(new TransactionPair<>(fixture.portfolio(), original));
        model.applyChanges();

        var duplicate = onlyOther(fixture.portfolio().getTransactions(), original);

        assertPortfolioProjectionDuplicate(original, duplicate, originalSnapshot, 1, 1);
        assertThat(fixture.portfolio().getTransactions().size(), is(2));
        assertThat(duplicate.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(duplicate.getDateTime(), is(DATE_TIME));
        assertThat(duplicate.getNote(), is("note"));
        assertThat(duplicate.getSource(), nullValue());
        assertSame(fixture.security(), duplicate.getSecurity());
        assertThat(duplicate.getShares(), is(SHARES));
        assertThat(duplicate.getAmount(), is(AMOUNT));
        assertNoDuplicateProjectionUUIDs(fixture.client());
        assertRoundtrips(fixture.client(), originalSnapshot.entryUUID(), uuid(entry(duplicate)), 2);
    }

    /**
     * Verifies that duplicating a ledger-backed account transfer creates a fresh transfer entry.
     * Source and target sides of the copy must be paired together and survive save/load.
     */
    @Test
    public void testAccountTransferDuplicateCreatesFreshLedgerTruthAndCrossEntry() throws Exception
    {
        var fixture = fixture();
        var original = new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.account(),
                        fixture.targetAccount(), DATE_TIME, AMOUNT, CurrencyUnit.EUR, AMOUNT, CurrencyUnit.EUR, null,
                        null, "note", "source");
        var originalSourceTransaction = original.getSourceTransaction();
        var originalTargetTransaction = original.getTargetTransaction();
        var originalSnapshot = EntrySnapshot.of(originalSourceTransaction);

        var model = new AccountTransferModel(fixture.client());
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.presetFromSource(original);
        model.applyChanges();

        var duplicateSourceTransaction = onlyOther(fixture.account().getTransactions(), originalSourceTransaction);
        var duplicateTargetTransaction = onlyOther(fixture.targetAccount().getTransactions(), originalTargetTransaction);

        assertAccountProjectionDuplicate(originalSourceTransaction, duplicateSourceTransaction, originalSnapshot, 2, 2);
        assertThat(fixture.account().getTransactions().size(), is(2));
        assertThat(fixture.targetAccount().getTransactions().size(), is(2));
        assertSame(duplicateTargetTransaction,
                        duplicateSourceTransaction.getCrossEntry().getCrossTransaction(duplicateSourceTransaction));
        assertSame(originalTargetTransaction,
                        originalSourceTransaction.getCrossEntry().getCrossTransaction(originalSourceTransaction));
        assertThat(duplicateSourceTransaction, not(originalSourceTransaction));
        assertThat(duplicateTargetTransaction, not(originalTargetTransaction));
        assertThat(duplicateSourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(duplicateTargetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(duplicateSourceTransaction.getDateTime(), is(DATE_TIME));
        assertThat(duplicateSourceTransaction.getNote(), is("note"));
        assertThat(duplicateSourceTransaction.getSource(), nullValue());
        assertThat(duplicateSourceTransaction.getAmount(), is(AMOUNT));
        assertThat(duplicateTargetTransaction.getAmount(), is(AMOUNT));
        assertNoDuplicateProjectionUUIDs(fixture.client());
        assertRoundtrips(fixture.client(), originalSnapshot.entryUUID(), uuid(entry(duplicateSourceTransaction)), 2);
    }

    /**
     * Verifies that duplicating a ledger-backed portfolio transfer creates a fresh transfer entry.
     * Source and target depot sides of the copy must be paired together and survive save/load.
     */
    @Test
    public void testPortfolioTransferDuplicateCreatesFreshLedgerTruthAndCrossEntry() throws Exception
    {
        var fixture = fixture();
        var original = new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        fixture.targetPortfolio(), fixture.security(), DATE_TIME, SHARES, AMOUNT, CurrencyUnit.EUR,
                        "note", "source");
        var originalSourceTransaction = original.getSourceTransaction();
        var originalTargetTransaction = original.getTargetTransaction();
        var originalSnapshot = EntrySnapshot.of(originalSourceTransaction);

        var model = new SecurityTransferModel(fixture.client());
        model.setExchangeRateProviderFactory(new ExchangeRateProviderFactory(fixture.client()));
        model.presetFromSource(original);
        model.applyChanges();

        var duplicateSourceTransaction = onlyOther(fixture.portfolio().getTransactions(), originalSourceTransaction);
        var duplicateTargetTransaction = onlyOther(fixture.targetPortfolio().getTransactions(),
                        originalTargetTransaction);

        assertPortfolioProjectionDuplicate(originalSourceTransaction, duplicateSourceTransaction, originalSnapshot, 2, 2);
        assertThat(fixture.portfolio().getTransactions().size(), is(2));
        assertThat(fixture.targetPortfolio().getTransactions().size(), is(2));
        assertSame(duplicateTargetTransaction,
                        duplicateSourceTransaction.getCrossEntry().getCrossTransaction(duplicateSourceTransaction));
        assertSame(originalTargetTransaction,
                        originalSourceTransaction.getCrossEntry().getCrossTransaction(originalSourceTransaction));
        assertThat(duplicateSourceTransaction, not(originalSourceTransaction));
        assertThat(duplicateTargetTransaction, not(originalTargetTransaction));
        assertThat(duplicateSourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(duplicateTargetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(duplicateSourceTransaction.getDateTime(), is(DATE_TIME));
        assertThat(duplicateSourceTransaction.getNote(), is("note"));
        assertThat(duplicateSourceTransaction.getSource(), nullValue());
        assertSame(fixture.security(), duplicateSourceTransaction.getSecurity());
        assertThat(duplicateSourceTransaction.getShares(), is(SHARES));
        assertThat(duplicateTargetTransaction.getShares(), is(SHARES));
        assertThat(duplicateSourceTransaction.getAmount(), is(AMOUNT));
        assertThat(duplicateTargetTransaction.getAmount(), is(AMOUNT));
        assertNoDuplicateProjectionUUIDs(fixture.client());
        assertRoundtrips(fixture.client(), originalSnapshot.entryUUID(), uuid(entry(duplicateSourceTransaction)), 2);
    }

    private void assertAccountProjectionDuplicate(AccountTransaction original, AccountTransaction duplicate,
                    EntrySnapshot originalSnapshot, int expectedPostings, int expectedProjectionRefs)
    {
        assertThat(duplicate.getClass().getName().contains("LedgerBacked"), is(true));
        assertThat(original.getUUID(), not(duplicate.getUUID()));
        assertFreshLedgerIdentity(originalSnapshot, entry(duplicate), expectedPostings, expectedProjectionRefs);
    }

    private void assertPortfolioProjectionDuplicate(PortfolioTransaction original, PortfolioTransaction duplicate,
                    EntrySnapshot originalSnapshot, int expectedPostings, int expectedProjectionRefs)
    {
        assertThat(duplicate.getClass().getName().contains("LedgerBacked"), is(true));
        assertThat(original.getUUID(), not(duplicate.getUUID()));
        assertFreshLedgerIdentity(originalSnapshot, entry(duplicate), expectedPostings, expectedProjectionRefs);
    }

    private void assertFreshLedgerIdentity(EntrySnapshot originalSnapshot, Object duplicateEntry,
                    int expectedPostings, int expectedProjectionRefs)
    {
        assertThat(uuid(duplicateEntry), not(originalSnapshot.entryUUID()));
        assertThat(postings(duplicateEntry).size(), is(expectedPostings));
        assertThat(projections(duplicateEntry).size(), is(expectedProjectionRefs));
        assertTrue(originalSnapshot.postingUUIDs().stream()
                        .noneMatch(uuid -> postings(duplicateEntry).stream().anyMatch(p -> uuid.equals(uuid(p)))));
        assertTrue(originalSnapshot.projectionUUIDs().stream()
                        .noneMatch(uuid -> projections(duplicateEntry).stream().anyMatch(p -> uuid.equals(uuid(p)))));
    }

    private void assertRoundtrips(Client client, String originalEntryUUID, String duplicateEntryUUID,
                    int expectedLedgerEntries) throws Exception
    {
        assertValid(client);
        assertRoundtrip(loadXml(saveXml(client)), originalEntryUUID, duplicateEntryUUID, expectedLedgerEntries);
        assertRoundtrip(loadProtobuf(saveProtobuf(client)), originalEntryUUID, duplicateEntryUUID,
                        expectedLedgerEntries);
    }

    private void assertRoundtrip(Client loaded, String originalEntryUUID, String duplicateEntryUUID,
                    int expectedLedgerEntries)
    {
        assertValid(loaded);
        assertThat(entries(loaded).size(), is(expectedLedgerEntries));
        assertTrue(entries(loaded).stream().anyMatch(e -> originalEntryUUID.equals(uuid(e))));
        assertTrue(entries(loaded).stream().anyMatch(e -> duplicateEntryUUID.equals(uuid(e))));
        assertNoDuplicateProjectionUUIDs(loaded);
        assertFalse(originalEntryUUID.equals(duplicateEntryUUID));
    }

    private void assertNoDuplicateProjectionUUIDs(Client client)
    {
        var projectionUUIDs = entries(client).stream().flatMap(e -> projections(e).stream()).map(this::uuid).toList();
        assertThat(new HashSet<>(projectionUUIDs).size(), is(projectionUUIDs.size()));
    }

    private void assertValid(Client client)
    {
        try
        {
            var ledger = Client.class.getMethod("getLedger").invoke(client);
            Class<?> validator = Class.forName("name.abuchen.portfolio.model.ledger.LedgerStructuralValidator", true,
                            Client.class.getClassLoader());
            Method validate = validator.getMethod("validate", ledger.getClass());
            Object result = validate.invoke(null, ledger);

            if (!((Boolean) result.getClass().getMethod("isOK").invoke(result)))
                throw new AssertionError(result.getClass().getMethod("format").invoke(result));
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    private Object entry(Transaction transaction)
    {
        try
        {
            return transaction.getClass().getMethod("getLedgerEntry").invoke(transaction);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> entries(Client client)
    {
        try
        {
            var ledger = Client.class.getMethod("getLedger").invoke(client);
            return (List<Object>) ledger.getClass().getMethod("getEntries").invoke(ledger);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> postings(Object entry)
    {
        try
        {
            return (List<Object>) entry.getClass().getMethod("getPostings").invoke(entry);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> projections(Object entry)
    {
        try
        {
            return (List<Object>) entry.getClass().getMethod("getProjectionRefs").invoke(entry);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    private String uuid(Object ledgerObject)
    {
        try
        {
            return (String) ledgerObject.getClass().getMethod("getUUID").invoke(ledgerObject);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }

    private <T extends Transaction> T onlyOther(List<T> transactions, T original)
    {
        return transactions.stream().filter(transaction -> !transaction.getUUID().equals(original.getUUID()))
                        .findFirst().orElseThrow();
    }

    private String saveXml(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-duplicate-copy", ".xml");

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

    private Client loadXml(String xml) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] saveProtobuf(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-duplicate-copy", ".portfolio");

        try
        {
            ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
            return Files.readAllBytes(file.toPath());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadProtobuf(byte[] bytes) throws Exception
    {
        var file = File.createTempFile("ledger-duplicate-copy", ".portfolio");

        try
        {
            Files.write(file.toPath(), bytes);
            return ClientFactory.load(file, null, new NullProgressMonitor());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Fixture fixture()
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        var account = account("Account");
        var targetAccount = account("Target Account");
        var portfolio = portfolio("Portfolio", account);
        var targetPortfolio = portfolio("Target Portfolio", account);
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setUpdatedAt(Instant.now());
        targetAccount.setUpdatedAt(Instant.now());
        portfolio.setUpdatedAt(Instant.now());
        targetPortfolio.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());

        client.addAccount(account);
        client.addAccount(targetAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(targetPortfolio);
        client.addSecurity(security);

        return new Fixture(client, account, targetAccount, portfolio, targetPortfolio, security);
    }

    private Account account(String name)
    {
        var account = new Account(name);
        account.setCurrencyCode(CurrencyUnit.EUR);
        return account;
    }

    private Portfolio portfolio(String name, Account referenceAccount)
    {
        var portfolio = new Portfolio(name);
        portfolio.setReferenceAccount(referenceAccount);
        return portfolio;
    }

    private record Fixture(Client client, Account account, Account targetAccount, Portfolio portfolio,
                    Portfolio targetPortfolio, Security security)
    {
    }

    private record EntrySnapshot(String entryUUID, List<String> postingUUIDs, List<String> projectionUUIDs)
    {
        static EntrySnapshot of(AccountTransaction transaction)
        {
            return of(new LedgerTransactionDuplicateCopyParityTest().entry(transaction));
        }

        static EntrySnapshot of(PortfolioTransaction transaction)
        {
            return of(new LedgerTransactionDuplicateCopyParityTest().entry(transaction));
        }

        private static EntrySnapshot of(Object entry)
        {
            var test = new LedgerTransactionDuplicateCopyParityTest();
            return new EntrySnapshot(test.uuid(entry), test.postings(entry).stream().map(test::uuid).toList(),
                            test.projections(entry).stream().map(test::uuid).toList());
        }
    }
}
