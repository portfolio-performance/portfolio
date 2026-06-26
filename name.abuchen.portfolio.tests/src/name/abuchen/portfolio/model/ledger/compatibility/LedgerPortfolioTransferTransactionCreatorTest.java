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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
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
public class LedgerPortfolioTransferTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);

    /**
     * Verifies that a portfolio transfer is created directly in the ledger.
     * Source and target depot rows must be projections of one persisted transfer booking.
     */
    @Test
    public void testCreatesLedgerPortfolioTransferDirectly()
    {
        var fixture = fixture();
        var transfer = createTransfer(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var sourcePosting = entry.getPostings().get(0);
        var targetPosting = entry.getPostings().get(1);
        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);

        assertThat(entry.getType(), is(LedgerEntryType.SECURITY_TRANSFER));
        assertThat(entry.getDateTime(), is(DATE_TIME));
        assertThat(entry.getNote(), is("note"));
        assertThat(entry.getSource(), is("source"));
        assertThat(sourcePosting.getType(), is(LedgerPostingType.SECURITY));
        assertThat(sourcePosting.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(sourcePosting.getCurrency(), is(CurrencyUnit.EUR));
        assertSame(fixture.source(), sourcePosting.getPortfolio());
        assertSame(fixture.security(), sourcePosting.getSecurity());
        assertThat(sourcePosting.getShares(), is(Values.Share.factorize(5)));
        assertThat(targetPosting.getType(), is(LedgerPostingType.SECURITY));
        assertThat(targetPosting.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(targetPosting.getCurrency(), is(CurrencyUnit.EUR));
        assertSame(fixture.target(), targetPosting.getPortfolio());
        assertSame(fixture.security(), targetPosting.getSecurity());
        assertThat(targetPosting.getShares(), is(Values.Share.factorize(5)));
        assertTrue(entry.getPostings().stream().allMatch(posting -> posting.getAccount() == null));

        assertThat(sourceProjection.getRole(), is(LedgerProjectionRole.SOURCE_PORTFOLIO));
        assertSame(fixture.source(), sourceProjection.getPortfolio());
        assertThat(sourceProjection.getPrimaryPostingUUID(), is(sourcePosting.getUUID()));
        assertThat(sourceProjection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(sourcePosting.getUUID()));
        assertThat(targetProjection.getRole(), is(LedgerProjectionRole.TARGET_PORTFOLIO));
        assertSame(fixture.target(), targetProjection.getPortfolio());
        assertThat(targetProjection.getPrimaryPostingUUID(), is(targetPosting.getUUID()));
        assertThat(targetProjection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(targetPosting.getUUID()));
        assertThat(sourceTransaction.getUUID(), is(sourceProjection.getUUID()));
        assertThat(targetTransaction.getUUID(), is(targetProjection.getUUID()));
        assertThat(sourceTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(targetTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(sourceTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(targetTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertSame(fixture.security(), sourceTransaction.getSecurity());
        assertSame(fixture.security(), targetTransaction.getSecurity());
        assertThat(sourceTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(targetTransaction.getShares(), is(Values.Share.factorize(5)));
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
     * Verifies that same-shape portfolio transfer edits and owner moves are applied through ledger paths.
     * Source and target projections must move without legacy delete/insert replay.
     */
    @Test
    public void testFacadeAppliesSameShapeEditAndMovesOwners() throws Exception
    {
        var fixture = fixture();
        var otherPortfolio = new Portfolio("Other");
        var otherTarget = new Portfolio("Other Target");
        otherPortfolio.setUpdatedAt(Instant.now());
        otherTarget.setUpdatedAt(Instant.now());
        var otherSecurity = new Security("Other Security", CurrencyUnit.EUR);
        otherSecurity.setUpdatedAt(Instant.now());
        fixture.client().addPortfolio(otherPortfolio);
        fixture.client().addPortfolio(otherTarget);
        fixture.client().addSecurity(otherSecurity);
        var creator = new LedgerPortfolioTransferTransactionCreator(fixture.client());
        var transfer = createTransfer(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();
        var sourcePostingUUID = fixture.client().getLedger().getEntries().get(0).getPostings().get(0).getUUID();
        var targetPostingUUID = fixture.client().getLedger().getEntries().get(0).getPostings().get(1).getUUID();
        var entryUUID = fixture.client().getLedger().getEntries().get(0).getUUID();
        var expectedProjectionUUIDs = projectionUUIDs(fixture.client());

        creator.update(transfer, fixture.source(), fixture.target(), otherSecurity, DATE_TIME.plusDays(1),
                        Values.Share.factorize(7), Values.Amount.factorize(150), CurrencyUnit.EUR, "updated",
                        "updated source");

        var entry = fixture.client().getLedger().getEntries().get(0);

        assertThat(entry.getDateTime(), is(DATE_TIME.plusDays(1)));
        assertThat(entry.getNote(), is("updated"));
        assertThat(entry.getSource(), is("updated source"));
        assertThat(entry.getPostings().get(0).getUUID(), is(sourcePostingUUID));
        assertThat(entry.getPostings().get(0).getAmount(), is(Values.Amount.factorize(150)));
        assertSame(otherSecurity, entry.getPostings().get(0).getSecurity());
        assertThat(entry.getPostings().get(0).getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getPostings().get(1).getUUID(), is(targetPostingUUID));
        assertThat(entry.getPostings().get(1).getAmount(), is(Values.Amount.factorize(150)));
        assertSame(otherSecurity, entry.getPostings().get(1).getSecurity());
        assertThat(entry.getPostings().get(1).getShares(), is(Values.Share.factorize(7)));
        assertThat(sourceTransaction.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(targetTransaction.getAmount(), is(Values.Amount.factorize(150)));

        var moved = creator.update(transfer, otherPortfolio, otherTarget, otherSecurity, DATE_TIME.plusDays(2),
                        Values.Share.factorize(8), Values.Amount.factorize(175), CurrencyUnit.EUR, "moved note",
                        "moved source");
        var movedSourceTransaction = moved.getSourceTransaction();
        var movedTargetTransaction = moved.getTargetTransaction();

        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
        assertThat(otherPortfolio.getTransactions(), is(List.of(movedSourceTransaction)));
        assertThat(otherTarget.getTransactions(), is(List.of(movedTargetTransaction)));
        assertThat(movedSourceTransaction.getUUID(), is(expectedProjectionUUIDs.get(0)));
        assertThat(movedTargetTransaction.getUUID(), is(expectedProjectionUUIDs.get(1)));
        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(sourcePostingUUID));
        assertThat(entry.getPostings().get(1).getUUID(), is(targetPostingUUID));
        assertSame(otherPortfolio, entry.getPostings().get(0).getPortfolio());
        assertSame(otherTarget, entry.getPostings().get(1).getPortfolio());
        assertSame(otherPortfolio, projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getPortfolio());
        assertSame(otherTarget, projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getPortfolio());
        assertCrossEntryReadCompatibility(movedSourceTransaction, movedTargetTransaction, otherPortfolio, otherTarget);
        assertThrows(UnsupportedOperationException.class,
                        () -> sourceTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThrows(UnsupportedOperationException.class, () -> sourceTransaction.setAmount(1L));
        sourceTransaction.getCrossEntry().updateFrom(sourceTransaction);
        assertTrue(fixture.source().getTransactions().isEmpty());
        assertThat(otherPortfolio.getTransactions(), is(List.of(movedSourceTransaction)));
        assertThrows(UnsupportedOperationException.class,
                        () -> sourceTransaction.getCrossEntry().setOwner(sourceTransaction, otherPortfolio));
        assertThrows(UnsupportedOperationException.class, () -> sourceTransaction.getCrossEntry().insert());
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertThat(projectionUUIDs(loadXml(saveXml(fixture.client()))), is(expectedProjectionUUIDs));
        assertThat(projectionUUIDs(loadProtobuf(saveProtobuf(fixture.client()))), is(expectedProjectionUUIDs));
        assertValid(fixture.client());
    }

    /**
     * Verifies that invalid portfolio transfer units are rejected before a ledger entry is added.
     * The creator must not leave partial transfer truth behind.
     */
    @Test
    public void testCreateRejectsInvalidUnitsWithoutPartialLedgerEntry()
    {
        var fixture = fixture();
        var creator = new LedgerPortfolioTransferTransactionCreator(fixture.client());
        var invalidUnits = LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.FEE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-1))));

        assertThrows(IllegalArgumentException.class,
                        () -> creator.create(fixture.source(), fixture.target(), fixture.security(), DATE_TIME,
                                        Values.Share.factorize(5), Values.Amount.factorize(100), CurrencyUnit.EUR,
                                        LedgerForexAmount.none(), LedgerForexAmount.none(), invalidUnits, "note",
                                        "source"));

        assertTrue(fixture.client().getLedger().getEntries().isEmpty());
        assertTrue(fixture.source().getTransactions().isEmpty());
        assertTrue(fixture.target().getTransactions().isEmpty());
    }

    /**
     * Verifies that mutable legacy setters stay blocked on ledger-backed portfolio transfers.
     * A failed setter attempt must leave the ledger and both owner lists unchanged.
     */
    @Test
    public void testReadOnlyWrapperRejectsAllMutableSettersWithoutPartialMutation()
    {
        var fixture = fixture();
        var otherPortfolio = new Portfolio("Other");
        var otherSourceTransaction = new PortfolioTransaction();
        var otherTargetTransaction = new PortfolioTransaction();
        var transfer = createTransfer(fixture);
        var sourceTransaction = transfer.getSourceTransaction();
        var targetTransaction = transfer.getTargetTransaction();

        assertThrows(UnsupportedOperationException.class, () -> transfer.setSourcePortfolio(otherPortfolio));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setTargetPortfolio(otherPortfolio));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setSourceTransaction(otherSourceTransaction));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setTargetTransaction(otherTargetTransaction));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setDate(DATE_TIME.plusDays(1)));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setSecurity(fixture.security()));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setShares(1L));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setAmount(1L));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setCurrencyCode(CurrencyUnit.USD));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setNote("other"));
        assertThrows(UnsupportedOperationException.class, () -> transfer.setSource("other"));

        assertSame(fixture.source(), transfer.getSourcePortfolio());
        assertSame(fixture.target(), transfer.getTargetPortfolio());
        assertSame(sourceTransaction, transfer.getSourceTransaction());
        assertSame(targetTransaction, transfer.getTargetTransaction());
        assertCrossEntryReadCompatibility(sourceTransaction, targetTransaction, fixture.source(), fixture.target());
        assertValid(fixture.client());
    }

    /**
     * Verifies that normal legacy transfer entries still allow their mutable setters.
     * Ledger read-only protection must not change non-ledger portfolio transfer behavior.
     */
    @Test
    public void testLegacyTransferEntryMutableSettersStillWork()
    {
        var source = new Portfolio("Source");
        var target = new Portfolio("Target");
        var otherSource = new Portfolio("Other Source");
        var otherTarget = new Portfolio("Other Target");
        var replacementSourceTransaction = new PortfolioTransaction();
        var replacementTargetTransaction = new PortfolioTransaction();
        var transfer = new PortfolioTransferEntry(source, target);

        transfer.setSourcePortfolio(otherSource);
        transfer.setTargetPortfolio(otherTarget);
        transfer.setSourceTransaction(replacementSourceTransaction);
        transfer.setTargetTransaction(replacementTargetTransaction);

        assertSame(otherSource, transfer.getSourcePortfolio());
        assertSame(otherTarget, transfer.getTargetPortfolio());
        assertSame(replacementSourceTransaction, transfer.getSourceTransaction());
        assertSame(replacementTargetTransaction, transfer.getTargetTransaction());
    }

    /**
     * Verifies that XML save/load/save preserves portfolio-transfer projection identities and fields.
     * Source and target depot rows must rematerialize from the same ledger entry.
     */
    @Test
    public void testXmlSaveLoadSavePreservesPortfolioTransferProjectionUUIDsAndFields() throws Exception
    {
        var client = transferClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadXml(saveXml(client));
        var reloaded = loadXml(saveXml(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(1));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getPortfolios().get(1).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(reloaded.getPortfolios().get(1).getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(reloaded.getAllTransactions().size(), is(1));
        assertValid(reloaded);
    }

    /**
     * Verifies that protobuf save/load/save preserves portfolio-transfer projection identities and fields.
     * Source and target depot rows must rematerialize from the same ledger entry.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesPortfolioTransferProjectionUUIDsAndFields() throws Exception
    {
        var client = transferClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadProtobuf(saveProtobuf(client));
        var reloaded = loadProtobuf(saveProtobuf(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(1));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getPortfolios().get(1).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(reloaded.getPortfolios().get(1).getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(reloaded.getAllTransactions().size(), is(1));
        assertValid(reloaded);
    }

    private void assertCrossEntryReadCompatibility(PortfolioTransaction sourceTransaction,
                    PortfolioTransaction targetTransaction, Portfolio source, Portfolio target)
    {
        assertThat(sourceTransaction.getCrossEntry(), instanceOf(PortfolioTransferEntry.class));
        assertThat(targetTransaction.getCrossEntry(), instanceOf(PortfolioTransferEntry.class));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(sourceTransaction, targetTransaction.getCrossEntry().getCrossTransaction(targetTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertSame(source, targetTransaction.getCrossEntry().getCrossOwner(targetTransaction));
    }

    private PortfolioTransferEntry createTransfer(Fixture fixture)
    {
        return new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.source(),
                        fixture.target(), fixture.security(), DATE_TIME, Values.Share.factorize(5),
                        Values.Amount.factorize(100), CurrencyUnit.EUR, "note", "source");
    }

    private Client transferClient()
    {
        var fixture = fixture();

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
        var file = File.createTempFile("ledger-portfolio-transfer", ".xml");
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

    private Fixture fixture()
    {
        var client = new Client();
        var source = new Portfolio("Source");
        var target = new Portfolio("Target");
        var security = new Security("Security", CurrencyUnit.EUR);
        source.setUpdatedAt(Instant.now());
        target.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());

        client.addPortfolio(source);
        client.addPortfolio(target);
        client.addSecurity(security);

        return new Fixture(client, source, target, security);
    }

    private record Fixture(Client client, Portfolio source, Portfolio target, Security security)
    {
    }
}
