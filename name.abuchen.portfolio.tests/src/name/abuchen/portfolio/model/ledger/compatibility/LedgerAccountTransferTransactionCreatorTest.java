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
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
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
public class LedgerAccountTransferTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(0.5);

    /**
     * Verifies that an account transfer is created directly in the ledger.
     * Source and target account rows must be projections of one persisted transfer booking.
     */
    @Test
    public void testCreatesLedgerAccountTransferDirectly()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var transfer = createTransfer(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var sourcePosting = entry.getPostings().get(0);
        var targetPosting = entry.getPostings().get(1);
        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT);

        assertThat(entry.getType(), is(LedgerEntryType.CASH_TRANSFER));
        assertThat(entry.getDateTime(), is(DATE_TIME));
        assertThat(entry.getNote(), is("note"));
        assertThat(entry.getSource(), is("source"));
        assertThat(entry.getPostings().size(), is(2));
        assertThat(sourcePosting.getType(), is(LedgerPostingType.CASH));
        assertThat(sourcePosting.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(sourcePosting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(sourcePosting.getForexAmount(), is(Values.Amount.factorize(200)));
        assertThat(sourcePosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(sourcePosting.getExchangeRate(), is(EXCHANGE_RATE));
        assertSame(fixture.source(), sourcePosting.getAccount());
        assertThat(targetPosting.getType(), is(LedgerPostingType.CASH));
        assertThat(targetPosting.getAmount(), is(Values.Amount.factorize(200)));
        assertThat(targetPosting.getCurrency(), is(CurrencyUnit.USD));
        assertSame(fixture.target(), targetPosting.getAccount());
        assertTrue(entry.getPostings().stream().allMatch(posting -> posting.getPortfolio() == null));
        assertTrue(entry.getPostings().stream().allMatch(posting -> posting.getSecurity() == null));

        assertThat(sourceProjection.getRole(), is(LedgerProjectionRole.SOURCE_ACCOUNT));
        assertSame(fixture.source(), sourceProjection.getAccount());
        assertThat(sourceProjection.getPrimaryPostingUUID(), is(sourcePosting.getUUID()));
        assertThat(sourceProjection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(sourcePosting.getUUID()));
        assertThat(targetProjection.getRole(), is(LedgerProjectionRole.TARGET_ACCOUNT));
        assertSame(fixture.target(), targetProjection.getAccount());
        assertThat(targetProjection.getPrimaryPostingUUID(), is(targetPosting.getUUID()));
        assertThat(targetProjection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(targetPosting.getUUID()));
        assertThat(sourceTransaction.getUUID(), is(sourceProjection.getUUID()));
        assertThat(targetTransaction.getUUID(), is(targetProjection.getUUID()));
        assertThat(sourceTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(targetTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(sourceTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(200)));
        assertThat(targetTransaction.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(sourceTransaction.getNote(), is("note"));
        assertThat(sourceTransaction.getSource(), is("source"));
        assertThat(fixture.source().getTransactions(), is(List.of(sourceTransaction)));
        assertThat(fixture.target().getTransactions(), is(List.of(targetTransaction)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(sourceTransaction, fixture.client().getAllTransactions().get(0).getTransaction());
        assertCrossEntryReadCompatibility(sourceTransaction, targetTransaction, fixture.source(), fixture.target());
        assertValid(fixture.client());
    }

    /**
     * Verifies that same-shape transfer edits and owner moves are applied through ledger paths.
     * Source and target projections must move without legacy delete/insert replay.
     */
    @Test
    public void testFacadeAppliesSameShapeEditAndMovesOwners() throws Exception
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var otherAccount = account("Other", CurrencyUnit.EUR);
        var otherTarget = account("Other Target", CurrencyUnit.USD);
        fixture.client().addAccount(otherAccount);
        fixture.client().addAccount(otherTarget);
        var creator = new LedgerAccountTransferTransactionCreator(fixture.client());
        var transfer = createTransfer(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();
        var sourcePostingUUID = fixture.client().getLedger().getEntries().get(0).getPostings().get(0).getUUID();
        var targetPostingUUID = fixture.client().getLedger().getEntries().get(0).getPostings().get(1).getUUID();
        var entryUUID = fixture.client().getLedger().getEntries().get(0).getUUID();
        var expectedProjectionUUIDs = projectionUUIDs(fixture.client());

        creator.update(transfer, fixture.source(), fixture.target(), DATE_TIME.plusDays(1),
                        Values.Amount.factorize(150), CurrencyUnit.EUR, Values.Amount.factorize(300),
                        CurrencyUnit.USD, Money.of(CurrencyUnit.USD, Values.Amount.factorize(300)), EXCHANGE_RATE,
                        "updated note", "updated source");

        var entry = fixture.client().getLedger().getEntries().get(0);

        assertThat(entry.getDateTime(), is(DATE_TIME.plusDays(1)));
        assertThat(entry.getNote(), is("updated note"));
        assertThat(entry.getSource(), is("updated source"));
        assertThat(entry.getPostings().get(0).getUUID(), is(sourcePostingUUID));
        assertThat(entry.getPostings().get(0).getAmount(), is(Values.Amount.factorize(150)));
        assertThat(entry.getPostings().get(0).getForexAmount(), is(Values.Amount.factorize(300)));
        assertThat(entry.getPostings().get(1).getUUID(), is(targetPostingUUID));
        assertThat(entry.getPostings().get(1).getAmount(), is(Values.Amount.factorize(300)));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(300)));

        var moved = creator.update(transfer, otherAccount, otherTarget, DATE_TIME.plusDays(2),
                        Values.Amount.factorize(175), CurrencyUnit.EUR, Values.Amount.factorize(350),
                        CurrencyUnit.USD, Money.of(CurrencyUnit.USD, Values.Amount.factorize(350)), EXCHANGE_RATE,
                        "moved note", "moved source");
        var movedSourceTransaction = moved.getSourceTransaction();
        var movedTargetTransaction = moved.getTargetTransaction();

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions(), is(List.of(movedSourceTransaction)));
        assertThat(otherTarget.getTransactions(), is(List.of(movedTargetTransaction)));
        assertThat(movedSourceTransaction.getUUID(), is(expectedProjectionUUIDs.get(0)));
        assertThat(movedTargetTransaction.getUUID(), is(expectedProjectionUUIDs.get(1)));
        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(sourcePostingUUID));
        assertThat(entry.getPostings().get(1).getUUID(), is(targetPostingUUID));
        assertSame(otherAccount, entry.getPostings().get(0).getAccount());
        assertSame(otherTarget, entry.getPostings().get(1).getAccount());
        assertSame(otherAccount, projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getAccount());
        assertSame(otherTarget, projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getAccount());
        assertCrossEntryReadCompatibility(movedSourceTransaction, movedTargetTransaction, otherAccount, otherTarget);
        assertThrows(UnsupportedOperationException.class, () -> sourceTransaction.setType(AccountTransaction.Type.DEPOSIT));
        assertThrows(UnsupportedOperationException.class, () -> sourceTransaction.setAmount(1L));
        sourceTransaction.getCrossEntry().updateFrom(sourceTransaction);
        assertTrue(fixture.source().getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions(), is(List.of(movedSourceTransaction)));
        assertThrows(UnsupportedOperationException.class,
                        () -> sourceTransaction.getCrossEntry().setOwner(sourceTransaction, otherAccount));
        assertThrows(UnsupportedOperationException.class, () -> sourceTransaction.getCrossEntry().insert());
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertThat(projectionUUIDs(loadXml(saveXml(fixture.client()))), is(expectedProjectionUUIDs));
        assertThat(projectionUUIDs(loadProtobuf(saveProtobuf(fixture.client()))), is(expectedProjectionUUIDs));
        assertValid(fixture.client());
    }

    /**
     * Verifies that invalid transfer units are rejected before a ledger entry is added.
     * The creator must not leave partial transfer truth behind.
     */
    @Test
    public void testCreateRejectsInvalidUnitsWithoutPartialLedgerEntry()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var creator = new LedgerAccountTransferTransactionCreator(fixture.client());
        var invalidUnits = LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.FEE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-1))));

        assertThrows(IllegalArgumentException.class,
                        () -> creator.create(fixture.source(), fixture.target(), DATE_TIME,
                                        Values.Amount.factorize(100), CurrencyUnit.EUR,
                                        Values.Amount.factorize(200), CurrencyUnit.USD, LedgerForexAmount.none(),
                                        LedgerForexAmount.none(), invalidUnits, "note", "source"));

        assertTrue(fixture.client().getLedger().getEntries().isEmpty());
        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
    }

    /**
     * Verifies that mutable legacy setters stay blocked on ledger-backed account transfers.
     * A failed setter attempt must leave the ledger and both owner lists unchanged.
     */
    @Test
    public void testReadOnlyWrapperRejectsAllMutableSettersWithoutPartialMutation()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var otherAccount = account("Other", CurrencyUnit.EUR);
        var otherSourceTransaction = new AccountTransaction();
        var otherTargetTransaction = new AccountTransaction();
        var transfer = createTransfer(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();

        assertThrows(UnsupportedOperationException.class, () -> transfer.setSourceAccount(otherAccount));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setTargetAccount(otherAccount));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setSourceTransaction(otherSourceTransaction));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setTargetTransaction(otherTargetTransaction));

        assertSame(fixture.source(), transfer.getSourceAccount());
        assertSame(fixture.target(), transfer.getTargetAccount());
        assertSame(sourceTransaction, transfer.getSourceTransaction());
        assertSame(targetTransaction, transfer.getTargetTransaction());
        assertCrossEntryReadCompatibility(sourceTransaction, targetTransaction, fixture.source(), fixture.target());
        assertValid(fixture.client());
    }

    /**
     * Verifies that normal legacy transfer entries still allow their mutable setters.
     * Ledger read-only protection must not change non-ledger transfer behavior.
     */
    @Test
    public void testLegacyTransferEntryMutableSettersStillWork()
    {
        var source = account("Source", CurrencyUnit.EUR);
        var target = account("Target", CurrencyUnit.EUR);
        var otherSource = account("Other Source", CurrencyUnit.EUR);
        var otherTarget = account("Other Target", CurrencyUnit.EUR);
        var replacementSourceTransaction = new AccountTransaction();
        var replacementTargetTransaction = new AccountTransaction();
        var transfer = new AccountTransferEntry(source, target);

        transfer.setSourceAccount(otherSource);
        transfer.setTargetAccount(otherTarget);
        transfer.setSourceTransaction(replacementSourceTransaction);
        transfer.setTargetTransaction(replacementTargetTransaction);

        assertSame(otherSource, transfer.getSourceAccount());
        assertSame(otherTarget, transfer.getTargetAccount());
        assertSame(replacementSourceTransaction, transfer.getSourceTransaction());
        assertSame(replacementTargetTransaction, transfer.getTargetTransaction());
    }

    /**
     * Verifies that XML save/load/save preserves account-transfer projection identities and fields.
     * Source and target rows must rematerialize from the same ledger entry.
     */
    @Test
    public void testXmlSaveLoadSavePreservesAccountTransferProjectionUUIDsAndFields() throws Exception
    {
        var client = transferClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadXml(saveXml(client));
        var reloaded = loadXml(saveXml(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(1));
        assertThat(reloaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getAccounts().get(1).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getAccounts().get(0).getTransactions().get(0).getType(),
                        is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(reloaded.getAccounts().get(1).getTransactions().get(0).getType(),
                        is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(reloaded.getAllTransactions().size(), is(1));
        assertValid(reloaded);
    }

    /**
     * Verifies that protobuf save/load/save preserves account-transfer projection identities and fields.
     * Source and target rows must rematerialize from the same ledger entry.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesAccountTransferProjectionUUIDsAndFields() throws Exception
    {
        var client = transferClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadProtobuf(saveProtobuf(client));
        var reloaded = loadProtobuf(saveProtobuf(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(1));
        assertThat(reloaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getAccounts().get(1).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getAccounts().get(0).getTransactions().get(0).getType(),
                        is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(reloaded.getAccounts().get(1).getTransactions().get(0).getType(),
                        is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(reloaded.getAllTransactions().size(), is(1));
        assertValid(reloaded);
    }

    private void assertCrossEntryReadCompatibility(AccountTransaction sourceTransaction,
                    AccountTransaction targetTransaction, Account source, Account target)
    {
        assertThat(sourceTransaction.getCrossEntry(), instanceOf(AccountTransferEntry.class));
        assertThat(targetTransaction.getCrossEntry(), instanceOf(AccountTransferEntry.class));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(sourceTransaction, targetTransaction.getCrossEntry().getCrossTransaction(targetTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertSame(source, targetTransaction.getCrossEntry().getCrossOwner(targetTransaction));
    }

    private AccountTransferEntry createTransfer(Fixture fixture)
    {
        return new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        DATE_TIME, Values.Amount.factorize(100), fixture.source().getCurrencyCode(),
                        Values.Amount.factorize(200), fixture.target().getCurrencyCode(),
                        Money.of(fixture.target().getCurrencyCode(), Values.Amount.factorize(200)), EXCHANGE_RATE,
                        "note", "source");
    }

    private Client transferClient()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);

        createTransfer(fixture);

        return fixture.client();
    }

    private LedgerProjectionRef projection(name.abuchen.portfolio.model.ledger.LedgerEntry entry,
                    LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private List<String> projectionUUIDs(Client client)
    {
        return client.getLedger().getEntries().stream()
                        .flatMap(entry -> entry.getProjectionRefs().stream())
                        .map(LedgerProjectionRef::getUUID)
                        .toList();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-account-transfer", ".xml");
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

    private Fixture fixture(String sourceCurrency, String targetCurrency)
    {
        var client = new Client();
        var source = account("Source", sourceCurrency);
        var target = account("Target", targetCurrency);

        client.addAccount(source);
        client.addAccount(target);

        return new Fixture(client, source, target);
    }

    private Account account(String name, String currency)
    {
        var account = new Account(name);
        account.setCurrencyCode(currency);
        return account;
    }

    private record Fixture(Client client, Account source, Account target)
    {
    }
}
