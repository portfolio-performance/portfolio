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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
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
public class LedgerDeliveryTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(2);

    /**
     * Verifies that inbound and outbound deliveries are created directly in the ledger.
     * The portfolio row must be a projection of the persisted delivery booking.
     */
    @Test
    public void testCreatesLedgerDeliveryDirectlyForInboundAndOutbound()
    {
        assertCreatesDelivery(PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerEntryType.DELIVERY_INBOUND,
                        LedgerProjectionRole.DELIVERY_INBOUND);
        assertCreatesDelivery(PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerEntryType.DELIVERY_OUTBOUND,
                        LedgerProjectionRole.DELIVERY_OUTBOUND);
    }

    /**
     * Verifies that metadata setters remain allowed on delivery projections.
     * Structural setters must stay blocked so runtime projections do not become a second truth.
     */
    @Test
    public void testCreatedDeliveryAllowsMetadataSetterAndRejectsStructuralSetters()
    {
        var fixture = fixture();
        var transaction = createDelivery(fixture.client(), fixture.portfolio(), fixture.security(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND);
        var updatedDateTime = DATE_TIME.plusDays(1);

        transaction.setDateTime(updatedDateTime);
        transaction.setNote("updated note");
        transaction.setSource("updated source");

        var entry = fixture.client().getLedger().getEntries().get(0);

        assertThat(entry.getDateTime(), is(updatedDateTime));
        assertThat(entry.getNote(), is("updated note"));
        assertThat(entry.getSource(), is("updated source"));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setAmount(1L));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setCurrencyCode(CurrencyUnit.USD));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setSecurity(fixture.security()));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setShares(1L));
        assertThrows(UnsupportedOperationException.class, transaction::clearUnits);
        assertThrows(UnsupportedOperationException.class,
                        () -> transaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertValid(fixture.client());
    }

    /**
     * Verifies that ledger-backed delivery projections expose gross-value read methods.
     * UI code must be able to read unit-derived values without mutating projections.
     */
    @Test
    public void testLedgerBackedDeliverySupportsGrossValueReadMethods()
    {
        var fixture = fixture();
        var inbound = createDelivery(fixture.client(), fixture.portfolio(), fixture.security(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND);
        var outbound = createDelivery(fixture.client(), fixture.portfolio(), fixture.security(),
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND);

        assertThat(inbound.getGrossValueAmount(), is(Values.Amount.factorize(80)));
        assertThat(inbound.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80))));
        assertThat(inbound.getGrossPricePerShare().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(inbound.getUnits().count(), is(3L));
        assertTrue(inbound.toString().contains("DELIVERY_INBOUND"));

        assertThat(outbound.getGrossValueAmount(), is(Values.Amount.factorize(200)));
        assertThat(outbound.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));
        assertThat(outbound.getGrossPricePerShare().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(outbound.getUnits().count(), is(3L));
        assertTrue(outbound.toString().contains("DELIVERY_OUTBOUND"));
    }

    /**
     * Verifies that same-shape delivery edits and owner moves are applied through ledger paths.
     * The projected delivery must refresh without creating duplicate booking truth.
     */
    @Test
    public void testFacadeAppliesDeliverySameShapeEditAndMovesOwner() throws Exception
    {
        var fixture = fixture();
        var otherPortfolio = portfolio(fixture.account());
        otherPortfolio.setUpdatedAt(Instant.now());
        fixture.client().addPortfolio(otherPortfolio);
        var otherSecurity = new Security("Other Security", CurrencyUnit.USD);
        otherSecurity.setUpdatedAt(Instant.now());
        fixture.client().addSecurity(otherSecurity);
        var creator = new LedgerDeliveryTransactionCreator(fixture.client());
        var transaction = createDelivery(fixture.client(), fixture.portfolio(), fixture.security(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND);
        var projectionUUID = transaction.getUUID();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();
        var updatedUnits = List.of(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(2)), BigDecimal.valueOf(1.5)));

        creator.update(transaction, fixture.portfolio(), PortfolioTransaction.Type.DELIVERY_INBOUND,
                        DATE_TIME.plusDays(2), Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity,
                        Values.Share.factorize(20), Money.of(CurrencyUnit.USD, Values.Amount.factorize(75)),
                        EXCHANGE_RATE, updatedUnits, "updated note", "updated source");

        var posting = entry.getPostings().get(0);

        assertThat(entry.getDateTime(), is(DATE_TIME.plusDays(2)));
        assertThat(entry.getNote(), is("updated note"));
        assertThat(entry.getSource(), is("updated source"));
        assertThat(posting.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(posting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting.getForexAmount(), is(Values.Amount.factorize(75)));
        assertThat(posting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting.getExchangeRate(), is(EXCHANGE_RATE));
        assertSame(otherSecurity, posting.getSecurity());
        assertThat(posting.getShares(), is(Values.Share.factorize(20)));
        assertTrue(entry.getPostings().stream().anyMatch(p -> p.getType() == LedgerPostingType.FEE
                        && p.getAmount() == Values.Amount.factorize(3)
                        && p.getForexAmount() == Values.Amount.factorize(2)
                        && CurrencyUnit.USD.equals(p.getForexCurrency())
                        && BigDecimal.valueOf(1.5).equals(p.getExchangeRate())));
        var moved = creator.update(transaction, otherPortfolio, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        DATE_TIME.plusDays(3), Values.Amount.factorize(151), CurrencyUnit.EUR, otherSecurity,
                        Values.Share.factorize(21), null, null, List.of(), "moved note", "moved source");

        assertTrue(fixture.portfolio().getTransactions().isEmpty());
        assertThat(otherPortfolio.getTransactions(), is(List.of(moved)));
        assertThat(moved.getUUID(), is(projectionUUID));
        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertSame(otherPortfolio, entry.getPostings().get(0).getPortfolio());
        assertSame(otherPortfolio, entry.getProjectionRefs().get(0).getPortfolio());
        assertThat(moved.getAmount(), is(Values.Amount.factorize(151)));
        assertThat(moved.getShares(), is(Values.Share.factorize(21)));
        assertThat(moved.getNote(), is("moved note"));
        assertThrows(UnsupportedOperationException.class,
                        () -> creator.update(transaction, fixture.portfolio(),
                                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, DATE_TIME,
                                        Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity,
                                        Values.Share.factorize(20), null, null, List.of(), "note", "source"));
        assertThrows(UnsupportedOperationException.class,
                        () -> creator.update(transaction, fixture.portfolio(), PortfolioTransaction.Type.BUY,
                                        DATE_TIME, Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity,
                                        Values.Share.factorize(20), null, null, List.of(), "note", "source"));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertThat(projectionUUIDs(loadXml(saveXml(fixture.client()))), is(List.of(projectionUUID)));
        assertThat(projectionUUIDs(loadProtobuf(saveProtobuf(fixture.client()))), is(List.of(projectionUUID)));
        assertValid(fixture.client());
    }

    /**
     * Verifies that XML save/load/save preserves delivery projection identity and fields.
     * The delivery row must rematerialize from the same ledger entry.
     */
    @Test
    public void testXmlSaveLoadSavePreservesDeliveryProjectionUUIDAndFields() throws Exception
    {
        var client = deliveryClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadXml(saveXml(client));
        var reloaded = loadXml(saveXml(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(2));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().size(), is(2));
        assertTrue(reloaded.getPortfolios().get(0).getTransactions().stream()
                        .allMatch(LedgerBackedTransaction.class::isInstance));
        assertTrue(reloaded.getPortfolios().get(0).getTransactions().stream()
                        .allMatch(transaction -> transaction.getUnits().count() == 3L));
        assertThat(reloaded.getAllTransactions().size(), is(2));
        assertValid(reloaded);
    }

    /**
     * Verifies that protobuf save/load/save preserves delivery projection identity and fields.
     * The delivery row must rematerialize from the same ledger entry.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesDeliveryProjectionUUIDAndFields() throws Exception
    {
        var client = deliveryClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadProtobuf(saveProtobuf(client));
        var reloaded = loadProtobuf(saveProtobuf(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(2));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().size(), is(2));
        assertTrue(reloaded.getPortfolios().get(0).getTransactions().stream()
                        .allMatch(LedgerBackedTransaction.class::isInstance));
        assertTrue(reloaded.getPortfolios().get(0).getTransactions().stream()
                        .allMatch(transaction -> transaction.getUnits().count() == 3L));
        assertThat(reloaded.getAllTransactions().size(), is(2));
        assertValid(reloaded);
    }

    private void assertCreatesDelivery(PortfolioTransaction.Type type, LedgerEntryType entryType,
                    LedgerProjectionRole projectionRole)
    {
        var fixture = fixture();
        var transaction = createDelivery(fixture.client(), fixture.portfolio(), fixture.security(), type);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var posting = entry.getPostings().get(0);
        var projection = entry.getProjectionRefs().get(0);

        assertThat(entry.getType(), is(entryType));
        assertThat(entry.getDateTime(), is(DATE_TIME));
        assertThat(entry.getNote(), is("note"));
        assertThat(entry.getSource(), is("source"));
        assertThat(posting.getType(), is(LedgerPostingType.SECURITY));
        assertThat(posting.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(posting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting.getForexAmount(), is(Values.Amount.factorize(70)));
        assertThat(posting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting.getExchangeRate(), is(EXCHANGE_RATE));
        assertSame(fixture.portfolio(), posting.getPortfolio());
        assertSame(fixture.security(), posting.getSecurity());
        assertThat(posting.getShares(), is(Values.Share.factorize(12)));
        assertTrue(entry.getPostings().stream().allMatch(p -> p.getAccount() == null));
        assertThat(entry.getProjectionRefs().size(), is(1));
        assertThat(projection.getRole(), is(projectionRole));
        assertSame(fixture.portfolio(), projection.getPortfolio());
        assertThat(projection.getPrimaryPostingUUID(), is(posting.getUUID()));
        assertThat(projection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(posting.getUUID()));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.GROSS_VALUE_UNIT).size(), is(1));
        assertThat(transaction.getUUID(), is(projection.getUUID()));
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction.getType(), is(type));
        assertThat(transaction.getDateTime(), is(DATE_TIME));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertSame(fixture.security(), transaction.getSecurity());
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getNote(), is("note"));
        assertThat(transaction.getSource(), is("source"));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(transaction)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertUnitPostings(entry.getPostings());
        assertValid(fixture.client());
    }

    private Client deliveryClient()
    {
        var fixture = fixture();

        createDelivery(fixture.client(), fixture.portfolio(), fixture.security(),
                        PortfolioTransaction.Type.DELIVERY_INBOUND);
        createDelivery(fixture.client(), fixture.portfolio(), fixture.security(),
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND);

        return fixture.client();
    }

    private PortfolioTransaction createDelivery(Client client, Portfolio portfolio, Security security,
                    PortfolioTransaction.Type type)
    {
        return new LedgerDeliveryTransactionCreator(client).create(portfolio, type, DATE_TIME,
                        Values.Amount.factorize(140), CurrencyUnit.EUR, security, Values.Share.factorize(12),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(70)), EXCHANGE_RATE, deliveryUnits(),
                        "note", "source");
    }

    private List<Unit> deliveryUnits()
    {
        return List.of(
                        new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(100)), EXCHANGE_RATE),
                        new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(10)), EXCHANGE_RATE),
                        new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(20)), EXCHANGE_RATE));
    }

    private void assertUnitPostings(List<LedgerPosting> postings)
    {
        assertTrue(postings.stream().anyMatch(posting -> posting.getType() == LedgerPostingType.GROSS_VALUE
                        && posting.getAmount() == Values.Amount.factorize(200)
                        && posting.getForexAmount() == Values.Amount.factorize(100)
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && EXCHANGE_RATE.equals(posting.getExchangeRate())));
        assertTrue(postings.stream().anyMatch(posting -> posting.getType() == LedgerPostingType.FEE
                        && posting.getAmount() == Values.Amount.factorize(20)
                        && posting.getForexAmount() == Values.Amount.factorize(10)
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && EXCHANGE_RATE.equals(posting.getExchangeRate())));
        assertTrue(postings.stream().anyMatch(posting -> posting.getType() == LedgerPostingType.TAX
                        && posting.getAmount() == Values.Amount.factorize(40)
                        && posting.getForexAmount() == Values.Amount.factorize(20)
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && EXCHANGE_RATE.equals(posting.getExchangeRate())));
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
        var file = File.createTempFile("ledger-delivery", ".xml");
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
        var account = account();
        var portfolio = portfolio(account);
        var security = new Security("Security", CurrencyUnit.USD);
        security.setUpdatedAt(Instant.now());

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    private Account account()
    {
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        return account;
    }

    private Portfolio portfolio(Account account)
    {
        var portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        return portfolio;
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }
}
