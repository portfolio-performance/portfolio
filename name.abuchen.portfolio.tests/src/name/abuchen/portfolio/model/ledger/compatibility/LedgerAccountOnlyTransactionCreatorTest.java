package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-aware creation and editing of transactions in this family.
 * These tests make sure user-visible rows are rebuilt from ledger truth and structural facts are not written through legacy projections.
 */
@SuppressWarnings("nls")
public class LedgerAccountOnlyTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);

    /**
     * Verifies that standard account-only booking families are created directly in the ledger.
     * The resulting account transaction must be a projection of the persisted ledger entry.
     */
    @Test
    public void testCreatesLedgerEntryDirectlyForAllAccountOnlyFamilies()
    {
        for (var fixture : fixtures())
        {
            var client = new Client();
            var account = account();
            client.addAccount(account);

            var transaction = create(client, account, fixture.type());
            var entry = client.getLedger().getEntries().get(0);
            var posting = entry.getPostings().get(0);
            var projection = entry.getProjectionRefs().get(0);

            assertThat(entry.getType(), is(fixture.entryType()));
            assertThat(client.getLedger().getEntries().size(), is(1));
            assertThat(entry.getPostings().size(), is(1));
            assertThat(posting.getType(), is(fixture.postingType()));
            assertThat(posting.getAmount(), is(Values.Amount.factorize(123)));
            assertThat(posting.getCurrency(), is(CurrencyUnit.EUR));
            assertSame(account, posting.getAccount());
            assertThat(entry.getProjectionRefs().size(), is(1));
            assertSame(account, projection.getAccount());
            assertThat(projection.getPrimaryPostingUUID(), is(posting.getUUID()));
            assertPrimaryMembership(projection, posting.getUUID());
            assertThat(transaction.getUUID(), is(projection.getUUID()));
            assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
            assertThat(transaction.getType(), is(fixture.type()));
            assertThat(transaction.getDateTime(), is(DATE_TIME));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(123)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getNote(), is("note"));
            assertThat(transaction.getSource(), is("source"));
            assertThat(account.getTransactions(), is(List.of(transaction)));
            assertThat(client.getAllTransactions().size(), is(1));
            assertValid(client);
        }
    }

    /**
     * Verifies that dialog-style account-only fee/tax input is stored as the primary posting.
     * The UI can provide the amount through the matching unit field while the total is still zero.
     */
    @Test
    public void testCreatesFeeTaxPrimaryPostingFromMatchingDialogUnit()
    {
        assertPrimaryPostingFromMatchingDialogUnit(AccountTransaction.Type.FEES, LedgerEntryType.FEES,
                        LedgerPostingType.FEE, Unit.Type.FEE);
        assertPrimaryPostingFromMatchingDialogUnit(AccountTransaction.Type.FEES_REFUND, LedgerEntryType.FEES_REFUND,
                        LedgerPostingType.FEE, Unit.Type.FEE);
        assertPrimaryPostingFromMatchingDialogUnit(AccountTransaction.Type.TAXES, LedgerEntryType.TAXES,
                        LedgerPostingType.TAX, Unit.Type.TAX);
        assertPrimaryPostingFromMatchingDialogUnit(AccountTransaction.Type.TAX_REFUND, LedgerEntryType.TAX_REFUND,
                        LedgerPostingType.TAX, Unit.Type.TAX);
    }

    /**
     * Verifies that optional security and unit facts are stored with account-only ledger bookings.
     * The projected account transaction must expose the same business values.
     */
    @Test
    public void testCreatesOptionalSecurityAndUnitPostings()
    {
        var client = new Client();
        var account = account();
        var security = new Security("Security", CurrencyUnit.USD);
        var units = List.of(
                        new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))),
                        new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))),
                        new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(25)),
                                        BigDecimal.valueOf(0.8)));

        client.addAccount(account);
        client.addSecurity(security);

        var transaction = new LedgerAccountOnlyTransactionCreator(client).create(account, AccountTransaction.Type.FEES,
                        DATE_TIME, Values.Amount.factorize(17), CurrencyUnit.EUR, security, units, "note", "source");
        var entry = client.getLedger().getEntries().get(0);
        var projection = entry.getProjectionRefs().get(0);

        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertSame(security, entry.getPostings().get(0).getSecurity());
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.FEE
                        && posting.getAmount() == Values.Amount.factorize(1)));
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.TAX
                        && posting.getAmount() == Values.Amount.factorize(2)));
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.GROSS_VALUE
                        && posting.getAmount() == Values.Amount.factorize(20)
                        && posting.getForexAmount() == Values.Amount.factorize(25)
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && BigDecimal.valueOf(0.8).equals(posting.getExchangeRate())));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.GROSS_VALUE_UNIT).size(), is(1));
        assertValid(client);
    }

    /**
     * Verifies that unsupported account-only families are rejected before creation.
     * The creator must not leave a partial ledger entry behind.
     */
    @Test
    public void testRejectsUnsupportedFamiliesWithoutMutation()
    {
        var client = new Client();
        var account = account();
        client.addAccount(account);

        assertThrows(UnsupportedOperationException.class,
                        () -> create(client, account, AccountTransaction.Type.DIVIDENDS));
        assertThrows(UnsupportedOperationException.class, () -> create(client, account, AccountTransaction.Type.BUY));
        assertThrows(UnsupportedOperationException.class, () -> create(client, account, AccountTransaction.Type.SELL));
        assertThrows(UnsupportedOperationException.class,
                        () -> create(client, account, AccountTransaction.Type.TRANSFER_IN));
        assertThrows(UnsupportedOperationException.class,
                        () -> create(client, account, AccountTransaction.Type.TRANSFER_OUT));

        assertTrue(client.getLedger().getEntries().isEmpty());
        assertTrue(account.getTransactions().isEmpty());
    }

    /**
     * Verifies that metadata setters remain allowed on account-only projections.
     * Structural setters must stay blocked so runtime projections do not become a second truth.
     */
    @Test
    public void testCreatedProjectionAllowsMetadataEditsAndRejectsStructuralSetters()
    {
        var client = new Client();
        var account = account();
        client.addAccount(account);

        var transaction = create(client, account, AccountTransaction.Type.DEPOSIT);
        var updatedDateTime = DATE_TIME.plusDays(1);

        transaction.setDateTime(updatedDateTime);
        transaction.setNote("updated note");
        transaction.setSource("updated source");

        var entry = client.getLedger().getEntries().get(0);

        assertThat(entry.getDateTime(), is(updatedDateTime));
        assertThat(entry.getNote(), is("updated note"));
        assertThat(entry.getSource(), is("updated source"));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setAmount(1L));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setCurrencyCode(CurrencyUnit.USD));
        assertThrows(UnsupportedOperationException.class, transaction::clearUnits);
        assertValid(client);
    }

    /**
     * Verifies that same-shape account edits and owner moves are applied through ledger paths.
     * The projected booking must move owners without replaying legacy setters as persisted truth.
     */
    @Test
    public void testFacadeAppliesSameShapeLedgerEditAndMovesOwner() throws Exception
    {
        var client = new Client();
        var account = account();
        var otherAccount = account();
        client.addAccount(account);
        client.addAccount(otherAccount);
        var creator = new LedgerAccountOnlyTransactionCreator(client);
        var transaction = create(client, account, AccountTransaction.Type.DEPOSIT);
        var updatedDateTime = DATE_TIME.plusDays(2);
        var entry = client.getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();

        creator.update(transaction, account, AccountTransaction.Type.DEPOSIT, updatedDateTime,
                        Values.Amount.factorize(456), CurrencyUnit.EUR, null, List.of(
                                        new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR,
                                                        Values.Amount.factorize(1)))),
                        "updated note", "updated source");

        assertThat(entry.getDateTime(), is(updatedDateTime));
        assertThat(entry.getPostings().get(0).getAmount(), is(Values.Amount.factorize(456)));
        assertThat(transaction.getNote(), is("updated note"));
        assertThat(transaction.getSource(), is("updated source"));
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.FEE
                        && posting.getAmount() == Values.Amount.factorize(1)));

        var moved = creator.update(transaction, otherAccount, AccountTransaction.Type.DEPOSIT, updatedDateTime,
                        Values.Amount.factorize(789), CurrencyUnit.EUR, null, List.of(), "moved note",
                        "moved source");

        assertTrue(account.getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions(), is(List.of(moved)));
        assertThat(moved.getUUID(), is(projectionUUID));
        assertThat(client.getLedger().getEntries().get(0).getUUID(), is(entryUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertSame(otherAccount, entry.getPostings().get(0).getAccount());
        assertSame(otherAccount, entry.getProjectionRefs().get(0).getAccount());
        assertThat(moved.getAmount(), is(Values.Amount.factorize(789)));
        assertThat(moved.getNote(), is("moved note"));
        assertThat(client.getAllTransactions().size(), is(1));
        assertThat(projectionUUIDs(loadXml(saveXml(client))), is(List.of(projectionUUID)));
        assertThat(projectionUUIDs(loadProtobuf(saveProtobuf(client))), is(List.of(projectionUUID)));
        assertValid(client);
    }

    /**
     * Verifies that XML save/load/save preserves account-only projection identity.
     * The booking must rematerialize from the same ledger entry after reload.
     */
    @Test
    public void testXmlSaveLoadSavePreservesAccountOnlyLedgerProjectionUUID() throws Exception
    {
        var client = accountOnlyClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadXml(saveXml(client));
        var reloaded = loadXml(saveXml(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(8));
        assertThat(reloaded.getAccounts().get(0).getTransactions().size(), is(8));
        assertTrue(reloaded.getAccounts().get(0).getTransactions().stream()
                        .allMatch(LedgerBackedTransaction.class::isInstance));
        assertThat(reloaded.getAllTransactions().size(), is(8));
        assertValid(reloaded);
    }

    /**
     * Verifies that protobuf save/load/save preserves account-only projection identity.
     * The booking must rematerialize from the same ledger entry after reload.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesAccountOnlyLedgerProjectionUUID() throws Exception
    {
        var client = accountOnlyClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadProtobuf(saveProtobuf(client));
        var reloaded = loadProtobuf(saveProtobuf(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(8));
        assertThat(reloaded.getAccounts().get(0).getTransactions().size(), is(8));
        assertTrue(reloaded.getAccounts().get(0).getTransactions().stream()
                        .allMatch(LedgerBackedTransaction.class::isInstance));
        assertThat(reloaded.getAllTransactions().size(), is(8));
        assertValid(reloaded);
    }

    private AccountTransaction create(Client client, Account account, AccountTransaction.Type type)
    {
        return new LedgerAccountOnlyTransactionCreator(client).create(account, type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, List.of(), "note", "source");
    }

    private void assertPrimaryPostingFromMatchingDialogUnit(AccountTransaction.Type type, LedgerEntryType entryType,
                    LedgerPostingType postingType, Unit.Type unitType)
    {
        var client = new Client();
        var account = account();
        var units = List.of(new Unit(unitType, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123))));

        client.addAccount(account);

        var transaction = new LedgerAccountOnlyTransactionCreator(client).create(account, type, DATE_TIME, 0,
                        CurrencyUnit.EUR, null, units, "note", "source");
        var entry = client.getLedger().getEntries().get(0);
        var posting = entry.getPostings().get(0);
        var projection = entry.getProjectionRefs().get(0);

        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(entry.getType(), is(entryType));
        assertThat(entry.getPostings().size(), is(1));
        assertThat(posting.getType(), is(postingType));
        assertThat(posting.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(posting.getCurrency(), is(CurrencyUnit.EUR));
        assertSame(account, posting.getAccount());
        assertThat(entry.getProjectionRefs().size(), is(1));
        assertThat(projection.getPrimaryPostingUUID(), is(posting.getUUID()));
        assertPrimaryMembership(projection, posting.getUUID());
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction.getType(), is(type));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(account.getTransactions(), is(List.of(transaction)));
        assertThat(client.getAllTransactions().size(), is(1));
        assertValid(client);
    }

    private Client accountOnlyClient()
    {
        var client = new Client();
        var account = account();
        client.addAccount(account);

        for (var fixture : fixtures())
            create(client, account, fixture.type());

        return client;
    }

    private List<String> projectionUUIDs(Client client)
    {
        return client.getLedger().getEntries().stream()
                        .flatMap(entry -> entry.getProjectionRefs().stream())
                        .map(LedgerProjectionRef::getUUID)
                        .toList();
    }

    private void assertPrimaryMembership(LedgerProjectionRef projection, String postingUUID)
    {
        assertThat(projection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(postingUUID));
        assertThat(projection.getPrimaryMembership().orElseThrow().getRole(), is(ProjectionMembershipRole.PRIMARY));
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-account-only", ".xml");
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

    private Client loadXml(String xml) throws IOException
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] saveProtobuf(Client client) throws IOException
    {
        return ProtobufTestUtilities.save(client);
    }

    private Client loadProtobuf(byte[] bytes) throws IOException
    {
        return ProtobufTestUtilities.load(bytes);
    }

    private void assertValid(Client client)
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());

        assertThat(result.getIssues().toString(), result.isOK(), is(true));
    }

    private Account account()
    {
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        return account;
    }

    private List<Fixture> fixtures()
    {
        return List.of(
                        new Fixture(AccountTransaction.Type.DEPOSIT, LedgerEntryType.DEPOSIT,
                                        LedgerPostingType.CASH),
                        new Fixture(AccountTransaction.Type.REMOVAL, LedgerEntryType.REMOVAL,
                                        LedgerPostingType.CASH),
                        new Fixture(AccountTransaction.Type.INTEREST, LedgerEntryType.INTEREST,
                                        LedgerPostingType.CASH),
                        new Fixture(AccountTransaction.Type.INTEREST_CHARGE, LedgerEntryType.INTEREST_CHARGE,
                                        LedgerPostingType.CASH),
                        new Fixture(AccountTransaction.Type.FEES, LedgerEntryType.FEES,
                                        LedgerPostingType.FEE),
                        new Fixture(AccountTransaction.Type.FEES_REFUND, LedgerEntryType.FEES_REFUND,
                                        LedgerPostingType.FEE),
                        new Fixture(AccountTransaction.Type.TAXES, LedgerEntryType.TAXES,
                                        LedgerPostingType.TAX),
                        new Fixture(AccountTransaction.Type.TAX_REFUND, LedgerEntryType.TAX_REFUND,
                                        LedgerPostingType.TAX));
    }

    private record Fixture(AccountTransaction.Type type, LedgerEntryType entryType, LedgerPostingType postingType)
    {
    }
}
