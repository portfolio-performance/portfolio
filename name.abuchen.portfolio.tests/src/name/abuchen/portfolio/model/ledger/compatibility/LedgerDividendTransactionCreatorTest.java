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
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
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
public class LedgerDividendTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(2);

    /**
     * Verifies that a dividend booking is created directly in the ledger.
     * The account row must expose dividend facts from the persisted ledger entry.
     */
    @Test
    public void testCreatesLedgerDividendDirectly()
    {
        var fixture = fixture();
        var transaction = createDividend(fixture.client(), fixture.account(), fixture.security());
        var entry = fixture.client().getLedger().getEntries().get(0);
        var cashPosting = entry.getPostings().get(0);
        var projection = entry.getProjectionRefs().get(0);

        assertThat(entry.getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(entry.getDateTime(), is(DATE_TIME));
        assertThat(entry.getNote(), is("note"));
        assertThat(entry.getSource(), is("source"));
        assertThat(cashPosting.getType(), is(LedgerPostingType.CASH));
        assertThat(cashPosting.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(cashPosting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(cashPosting.getForexAmount(), is(Values.Amount.factorize(70)));
        assertThat(cashPosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(cashPosting.getExchangeRate(), is(EXCHANGE_RATE));
        assertSame(fixture.account(), cashPosting.getAccount());
        assertSame(fixture.security(), cashPosting.getSecurity());
        assertThat(cashPosting.getShares(), is(Values.Share.factorize(12)));
        assertThat(exDate(cashPosting), is(EX_DATE));
        assertThat(entry.getProjectionRefs().size(), is(1));
        assertSame(fixture.account(), projection.getAccount());
        assertThat(projection.getPrimaryPostingUUID(), is(cashPosting.getUUID()));
        assertThat(projection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(cashPosting.getUUID()));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.GROSS_VALUE_UNIT).size(), is(1));
        assertThat(transaction.getUUID(), is(projection.getUUID()));
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(DATE_TIME));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(140)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertSame(fixture.security(), transaction.getSecurity());
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getExDate(), is(EX_DATE));
        assertThat(transaction.getNote(), is("note"));
        assertThat(transaction.getSource(), is("source"));
        assertThat(fixture.account().getTransactions(), is(List.of(transaction)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertUnitPostings(entry.getPostings());
        assertValid(fixture.client());
    }

    /**
     * Verifies that metadata setters remain allowed on dividend projections.
     * Structural setters must stay blocked so runtime projections do not become a second truth.
     */
    @Test
    public void testCreatedDividendAllowsMetadataSetterAndRejectsStructuralSetters()
    {
        var fixture = fixture();
        var transaction = createDividend(fixture.client(), fixture.account(), fixture.security());
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
        assertThrows(UnsupportedOperationException.class, () -> transaction.setShares(1L));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setExDate(EX_DATE.plusDays(1)));
        assertThrows(UnsupportedOperationException.class, transaction::clearUnits);
        assertValid(fixture.client());
    }

    /**
     * Verifies that ledger-backed dividend projections expose gross-value read methods.
     * UI code must be able to read unit-derived values without mutating projections.
     */
    @Test
    public void testLedgerBackedDividendSupportsGrossValueReadMethods()
    {
        var fixture = fixture();
        var transaction = createDividend(fixture.client(), fixture.account(), fixture.security());

        assertThat(transaction.getGrossValueAmount(), is(Values.Amount.factorize(200)));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));
        assertThat(transaction.getUnits().count(), is(3L));
        assertTrue(transaction.toString().contains("DIVIDENDS"));
    }

    /**
     * Verifies that same-shape dividend edits and owner moves are applied through ledger paths.
     * The projected dividend must refresh without creating duplicate booking truth.
     */
    @Test
    public void testFacadeAppliesDividendSameShapeEditAndMovesOwner() throws Exception
    {
        var fixture = fixture();
        var otherAccount = account();
        fixture.client().addAccount(otherAccount);
        var otherSecurity = new Security("Other Security", CurrencyUnit.USD);
        otherSecurity.setUpdatedAt(Instant.now());
        fixture.client().addSecurity(otherSecurity);
        var creator = new LedgerDividendTransactionCreator(fixture.client());
        var transaction = createDividend(fixture.client(), fixture.account(), fixture.security());
        var projectionUUID = transaction.getUUID();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();
        var updatedUnits = List.of(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(2)), BigDecimal.valueOf(1.5)));

        creator.update(transaction, fixture.account(), AccountTransaction.Type.DIVIDENDS, DATE_TIME.plusDays(2),
                        Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity, Values.Share.factorize(20),
                        EX_DATE.plusDays(1), Money.of(CurrencyUnit.USD, Values.Amount.factorize(75)), EXCHANGE_RATE,
                        updatedUnits, "updated note", "updated source");

        var cashPosting = entry.getPostings().get(0);

        assertThat(entry.getDateTime(), is(DATE_TIME.plusDays(2)));
        assertThat(entry.getNote(), is("updated note"));
        assertThat(entry.getSource(), is("updated source"));
        assertThat(cashPosting.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(cashPosting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(cashPosting.getForexAmount(), is(Values.Amount.factorize(75)));
        assertThat(cashPosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(cashPosting.getExchangeRate(), is(EXCHANGE_RATE));
        assertSame(otherSecurity, cashPosting.getSecurity());
        assertThat(cashPosting.getShares(), is(Values.Share.factorize(20)));
        assertThat(exDate(cashPosting), is(EX_DATE.plusDays(1)));
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.FEE
                        && posting.getAmount() == Values.Amount.factorize(3)
                        && posting.getForexAmount() == Values.Amount.factorize(2)
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && BigDecimal.valueOf(1.5).equals(posting.getExchangeRate())));
        var moved = creator.update(transaction, otherAccount, AccountTransaction.Type.DIVIDENDS, DATE_TIME.plusDays(3),
                        Values.Amount.factorize(151), CurrencyUnit.EUR, otherSecurity, Values.Share.factorize(21),
                        EX_DATE.plusDays(2), null, null, List.of(), "moved note", "moved source");

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions(), is(List.of(moved)));
        assertThat(moved.getUUID(), is(projectionUUID));
        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertSame(otherAccount, entry.getPostings().get(0).getAccount());
        assertSame(otherAccount, entry.getProjectionRefs().get(0).getAccount());
        assertThat(moved.getAmount(), is(Values.Amount.factorize(151)));
        assertThat(moved.getShares(), is(Values.Share.factorize(21)));
        assertThat(moved.getNote(), is("moved note"));
        assertThrows(UnsupportedOperationException.class,
                        () -> creator.update(transaction, fixture.account(), AccountTransaction.Type.BUY, DATE_TIME,
                                        Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity,
                                        Values.Share.factorize(20), EX_DATE, null, null, List.of(), "note", "source"));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertThat(projectionUUIDs(loadXml(saveXml(fixture.client()))), is(List.of(projectionUUID)));
        assertThat(projectionUUIDs(loadProtobuf(saveProtobuf(fixture.client()))), is(List.of(projectionUUID)));
        assertValid(fixture.client());
    }

    /**
     * Verifies that XML save/load/save preserves dividend projection identity and fields.
     * The dividend row must rematerialize from the same ledger entry with ex-date and units intact.
     */
    @Test
    public void testXmlSaveLoadSavePreservesDividendProjectionUUIDAndFields() throws Exception
    {
        var client = dividendClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadXml(saveXml(client));
        var reloaded = loadXml(saveXml(loaded));
        var transaction = reloaded.getAccounts().get(0).getTransactions().get(0);

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(1));
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getExDate(), is(EX_DATE));
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getUnits().count(), is(3L));
        assertValid(reloaded);
    }

    /**
     * Verifies that protobuf save/load/save preserves dividend projection identity and fields.
     * The dividend row must rematerialize from the same ledger entry with ex-date and units intact.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesDividendProjectionUUIDAndFields() throws Exception
    {
        var client = dividendClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadProtobuf(saveProtobuf(client));
        var reloaded = loadProtobuf(saveProtobuf(loaded));
        var transaction = reloaded.getAccounts().get(0).getTransactions().get(0);

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(1));
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getExDate(), is(EX_DATE));
        assertThat(transaction.getShares(), is(Values.Share.factorize(12)));
        assertThat(transaction.getUnits().count(), is(3L));
        assertValid(reloaded);
    }

    private Client dividendClient()
    {
        var fixture = fixture();
        createDividend(fixture.client(), fixture.account(), fixture.security());
        return fixture.client();
    }

    private AccountTransaction createDividend(Client client, Account account, Security security)
    {
        return new LedgerDividendTransactionCreator(client).create(account, DATE_TIME, Values.Amount.factorize(140),
                        CurrencyUnit.EUR, security, Values.Share.factorize(12), EX_DATE,
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(70)), EXCHANGE_RATE, dividendUnits(),
                        "note", "source");
    }

    private List<Unit> dividendUnits()
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

    private LocalDateTime exDate(LedgerPosting posting)
    {
        return posting.getParameters().stream()
                        .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE)
                        .filter(parameter -> parameter.getValueKind() == LedgerParameter.ValueKind.LOCAL_DATE_TIME)
                        .map(LedgerParameter::getValue)
                        .map(LocalDateTime.class::cast)
                        .findFirst()
                        .orElse(null);
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
        var file = File.createTempFile("ledger-dividend", ".xml");
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
        var security = new Security("Security", CurrencyUnit.USD);
        security.setUpdatedAt(Instant.now());

        client.addAccount(account);
        client.addSecurity(security);

        return new Fixture(client, account, security);
    }

    private Account account()
    {
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        return account;
    }

    private record Fixture(Client client, Account account, Security security)
    {
    }
}
