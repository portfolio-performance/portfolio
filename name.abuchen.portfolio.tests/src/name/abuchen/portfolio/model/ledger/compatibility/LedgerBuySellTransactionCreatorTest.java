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
import name.abuchen.portfolio.model.BuySellEntry;
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
public class LedgerBuySellTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);

    /**
     * Verifies that a buy booking is created directly in the ledger.
     * The account and portfolio rows must be derived projections of one persisted booking.
     */
    @Test
    public void testCreatesLedgerBuyDirectly()
    {
        var fixture = fixture();
        var entry = createBuy(fixture);

        assertCreatedBuySell(fixture, entry, LedgerEntryType.BUY, PortfolioTransaction.Type.BUY,
                        AccountTransaction.Type.BUY);
        assertValid(fixture.client());
    }

    /**
     * Verifies that a sell booking is created directly in the ledger.
     * The account and portfolio rows must be derived projections of one persisted booking.
     */
    @Test
    public void testCreatesLedgerSellDirectly()
    {
        var fixture = fixture();
        var entry = createSell(fixture);

        assertCreatedBuySell(fixture, entry, LedgerEntryType.SELL, PortfolioTransaction.Type.SELL,
                        AccountTransaction.Type.SELL);
        assertValid(fixture.client());
    }

    /**
     * Verifies that ledger-backed buy/sell projections expose gross-value read methods.
     * UI and import code must be able to read unit-derived values without mutating projections.
     */
    @Test
    public void testLedgerBackedBuySellSupportsGrossValueReadMethods()
    {
        var fixture = fixture();
        var buy = createBuy(fixture);
        var sell = createSell(fixture);

        assertThat(buy.getAccountTransaction().getGrossValueAmount(), is(Values.Amount.factorize(116)));
        assertThat(buy.getAccountTransaction().getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116))));
        assertTrue(buy.getAccountTransaction().toString().contains("BUY"));
        assertThat(buy.getPortfolioTransaction().getGrossValueAmount(), is(Values.Amount.factorize(116)));
        assertThat(buy.getPortfolioTransaction().getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116))));
        assertThat(buy.getPortfolioTransaction().getGrossPricePerShare().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertTrue(buy.getPortfolioTransaction().toString().contains("BUY"));

        assertThat(sell.getAccountTransaction().getGrossValueAmount(), is(Values.Amount.factorize(130)));
        assertThat(sell.getAccountTransaction().getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(130))));
        assertTrue(sell.getAccountTransaction().toString().contains("SELL"));
        assertThat(sell.getPortfolioTransaction().getGrossValueAmount(), is(Values.Amount.factorize(130)));
        assertThat(sell.getPortfolioTransaction().getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(130))));
        assertThat(sell.getPortfolioTransaction().getGrossPricePerShare().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertTrue(sell.getPortfolioTransaction().toString().contains("SELL"));
    }

    /**
     * Verifies that same-shape buy/sell edits and owner moves are applied through ledger paths.
     * The account and portfolio projections must refresh without creating duplicate booking truth.
     */
    @Test
    public void testFacadeAppliesSameShapeEditAndMovesOwners() throws Exception
    {
        var fixture = fixture();
        var otherAccount = account("Other Account");
        var otherPortfolio = new Portfolio("Other Portfolio");
        otherPortfolio.setUpdatedAt(Instant.now());
        var otherSecurity = new Security("Other Security", CurrencyUnit.EUR);
        otherSecurity.setUpdatedAt(Instant.now());
        fixture.client().addAccount(otherAccount);
        fixture.client().addPortfolio(otherPortfolio);
        fixture.client().addSecurity(otherSecurity);
        var creator = new LedgerBuySellTransactionCreator(fixture.client());
        var entry = createBuy(fixture);
        var accountTransaction = entry.getAccountTransaction();
        var portfolioTransaction = entry.getPortfolioTransaction();
        var cashPostingUUID = fixture.client().getLedger().getEntries().get(0).getPostings().get(0).getUUID();
        var securityPostingUUID = fixture.client().getLedger().getEntries().get(0).getPostings().get(1).getUUID();
        var entryUUID = fixture.client().getLedger().getEntries().get(0).getUUID();
        var expectedProjectionUUIDs = projectionUUIDs(fixture.client());

        creator.update(entry, fixture.portfolio(), fixture.account(), PortfolioTransaction.Type.BUY,
                        DATE_TIME.plusDays(1), Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity,
                        Values.Share.factorize(7), List.of(unit(Unit.Type.FEE, 5)), "updated", "updated source");

        var ledgerEntry = fixture.client().getLedger().getEntries().get(0);

        assertThat(ledgerEntry.getDateTime(), is(DATE_TIME.plusDays(1)));
        assertThat(ledgerEntry.getNote(), is("updated"));
        assertThat(ledgerEntry.getSource(), is("updated source"));
        assertThat(ledgerEntry.getPostings().get(0).getUUID(), is(cashPostingUUID));
        assertThat(ledgerEntry.getPostings().get(0).getAmount(), is(Values.Amount.factorize(150)));
        assertThat(ledgerEntry.getPostings().get(1).getUUID(), is(securityPostingUUID));
        assertSame(otherSecurity, ledgerEntry.getPostings().get(1).getSecurity());
        assertThat(ledgerEntry.getPostings().get(1).getShares(), is(Values.Share.factorize(7)));
        assertThat(portfolioTransaction.getUnits().count(), is(1L));
        assertThat(accountTransaction.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(150)));

        var moved = creator.update(entry, otherPortfolio, otherAccount, PortfolioTransaction.Type.BUY,
                        DATE_TIME.plusDays(2), Values.Amount.factorize(175), CurrencyUnit.EUR, otherSecurity,
                        Values.Share.factorize(8), List.of(), "moved note", "moved source");
        var movedAccountTransaction = moved.getAccountTransaction();
        var movedPortfolioTransaction = moved.getPortfolioTransaction();

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertTrue(fixture.portfolio().getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions(), is(List.of(movedAccountTransaction)));
        assertThat(otherPortfolio.getTransactions(), is(List.of(movedPortfolioTransaction)));
        assertThat(movedAccountTransaction.getUUID(), is(expectedProjectionUUIDs.get(0)));
        assertThat(movedPortfolioTransaction.getUUID(), is(expectedProjectionUUIDs.get(1)));
        assertThat(ledgerEntry.getUUID(), is(entryUUID));
        assertThat(ledgerEntry.getPostings().get(0).getUUID(), is(cashPostingUUID));
        assertThat(ledgerEntry.getPostings().get(1).getUUID(), is(securityPostingUUID));
        assertSame(otherAccount, ledgerEntry.getPostings().get(0).getAccount());
        assertSame(otherPortfolio, ledgerEntry.getPostings().get(1).getPortfolio());
        assertSame(otherAccount, projection(ledgerEntry, LedgerProjectionRole.ACCOUNT).getAccount());
        assertSame(otherPortfolio, projection(ledgerEntry, LedgerProjectionRole.PORTFOLIO).getPortfolio());
        assertCrossEntryReadCompatibility(movedAccountTransaction, movedPortfolioTransaction, otherAccount,
                        otherPortfolio);
        assertThrows(UnsupportedOperationException.class,
                        () -> creator.update(entry, fixture.portfolio(), fixture.account(), PortfolioTransaction.Type.SELL,
                                        DATE_TIME, Values.Amount.factorize(150), CurrencyUnit.EUR, otherSecurity,
                                        Values.Share.factorize(7), List.of(), "note", "source"));
        assertThrows(UnsupportedOperationException.class,
                        () -> portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThrows(UnsupportedOperationException.class, () -> portfolioTransaction.setAmount(1L));
        accountTransaction.getCrossEntry().updateFrom(accountTransaction);
        assertTrue(fixture.account().getTransactions().isEmpty());
        assertThat(otherAccount.getTransactions(), is(List.of(movedAccountTransaction)));
        assertThrows(UnsupportedOperationException.class,
                        () -> accountTransaction.getCrossEntry().setOwner(accountTransaction, otherAccount));
        assertThrows(UnsupportedOperationException.class, () -> accountTransaction.getCrossEntry().insert());
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertThat(projectionUUIDs(loadXml(saveXml(fixture.client()))), is(expectedProjectionUUIDs));
        assertThat(projectionUUIDs(loadProtobuf(saveProtobuf(fixture.client()))), is(expectedProjectionUUIDs));
        assertValid(fixture.client());
    }

    /**
     * Verifies that mutable legacy setters stay blocked on ledger-backed buy/sell wrappers.
     * A failed setter attempt must leave the ledger and both owner lists unchanged.
     */
    @Test
    public void testReadOnlyWrapperRejectsAllMutableSettersWithoutPartialMutation()
    {
        var fixture = fixture();
        var entry = createBuy(fixture);
        var accountTransaction = entry.getAccountTransaction();
        var portfolioTransaction = entry.getPortfolioTransaction();

        assertThrows(UnsupportedOperationException.class, () -> entry.setPortfolio(new Portfolio("Other")));
        assertThrows(UnsupportedOperationException.class, () -> entry.setAccount(account("Other")));
        assertThrows(UnsupportedOperationException.class, () -> entry.setDate(DATE_TIME.plusDays(1)));
        assertThrows(UnsupportedOperationException.class, () -> entry.setType(PortfolioTransaction.Type.SELL));
        assertThrows(UnsupportedOperationException.class, () -> entry.setSecurity(fixture.security()));
        assertThrows(UnsupportedOperationException.class, () -> entry.setShares(1L));
        assertThrows(UnsupportedOperationException.class, () -> entry.setAmount(1L));
        assertThrows(UnsupportedOperationException.class, () -> entry.setCurrencyCode(CurrencyUnit.USD));
        assertThrows(UnsupportedOperationException.class, () -> entry.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 1L)));
        assertThrows(UnsupportedOperationException.class, () -> entry.setNote("other"));
        assertThrows(UnsupportedOperationException.class, () -> entry.setSource("other"));

        assertSame(fixture.account(), entry.getAccount());
        assertSame(fixture.portfolio(), entry.getPortfolio());
        assertSame(accountTransaction, entry.getAccountTransaction());
        assertSame(portfolioTransaction, entry.getPortfolioTransaction());
        assertCrossEntryReadCompatibility(accountTransaction, portfolioTransaction, fixture.account(),
                        fixture.portfolio());
        assertValid(fixture.client());
    }

    /**
     * Verifies that normal legacy buy/sell entries still allow their mutable setters.
     * Ledger read-only protection must not change non-ledger transaction behavior.
     */
    @Test
    public void testLegacyBuySellEntryMutableSettersStillWork()
    {
        var portfolio = new Portfolio("Portfolio");
        var account = account("Account");
        var otherPortfolio = new Portfolio("Other Portfolio");
        var otherAccount = account("Other Account");
        var entry = new BuySellEntry(portfolio, account);

        entry.setPortfolio(otherPortfolio);
        entry.setAccount(otherAccount);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(DATE_TIME);
        entry.setAmount(Values.Amount.factorize(10));
        entry.setCurrencyCode(CurrencyUnit.EUR);

        assertSame(otherPortfolio, entry.getPortfolio());
        assertSame(otherAccount, entry.getAccount());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(10)));
        assertThat(entry.getAccountTransaction().getAmount(), is(Values.Amount.factorize(10)));
    }

    /**
     * Verifies that XML save/load/save preserves buy/sell projection identities and fields.
     * Both account and portfolio rows must rematerialize from the same ledger entry.
     */
    @Test
    public void testXmlSaveLoadSavePreservesBuySellProjectionUUIDsAndFields() throws Exception
    {
        var client = buySellClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadXml(saveXml(client));
        var reloaded = loadXml(saveXml(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(2));
        assertThat(reloaded.getAccounts().get(0).getTransactions().size(), is(2));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().size(), is(2));
        assertThat(reloaded.getAllTransactions().size(), is(2));
        assertValid(reloaded);
    }

    /**
     * Verifies that protobuf save/load/save preserves buy/sell projection identities and fields.
     * Both account and portfolio rows must rematerialize from the same ledger entry.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesBuySellProjectionUUIDsAndFields() throws Exception
    {
        var client = buySellClient();
        var expectedProjectionUUIDs = projectionUUIDs(client);

        var loaded = loadProtobuf(saveProtobuf(client));
        var reloaded = loadProtobuf(saveProtobuf(loaded));

        assertThat(projectionUUIDs(reloaded), is(expectedProjectionUUIDs));
        assertThat(reloaded.getLedger().getEntries().size(), is(2));
        assertThat(reloaded.getAccounts().get(0).getTransactions().size(), is(2));
        assertThat(reloaded.getPortfolios().get(0).getTransactions().size(), is(2));
        assertThat(reloaded.getAllTransactions().size(), is(2));
        assertValid(reloaded);
    }

    private void assertCreatedBuySell(Fixture fixture, BuySellEntry entry, LedgerEntryType entryType,
                    PortfolioTransaction.Type portfolioType, AccountTransaction.Type accountType)
    {
        var accountTransaction = entry.getAccountTransaction();
        var portfolioTransaction = entry.getPortfolioTransaction();
        var ledgerEntry = fixture.client().getLedger().getEntries().get(0);
        var cashPosting = ledgerEntry.getPostings().get(0);
        var securityPosting = ledgerEntry.getPostings().get(1);
        var feePosting = posting(ledgerEntry, LedgerPostingType.FEE);
        var taxPosting = posting(ledgerEntry, LedgerPostingType.TAX);
        var grossValuePosting = posting(ledgerEntry, LedgerPostingType.GROSS_VALUE);
        var accountProjection = projection(ledgerEntry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = projection(ledgerEntry, LedgerProjectionRole.PORTFOLIO);

        assertThat(ledgerEntry.getType(), is(entryType));
        assertThat(ledgerEntry.getDateTime(), is(DATE_TIME));
        assertThat(ledgerEntry.getNote(), is("note"));
        assertThat(ledgerEntry.getSource(), is("source"));
        assertThat(cashPosting.getType(), is(LedgerPostingType.CASH));
        assertThat(cashPosting.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(cashPosting.getCurrency(), is(CurrencyUnit.EUR));
        assertSame(fixture.account(), cashPosting.getAccount());
        assertThat(securityPosting.getType(), is(LedgerPostingType.SECURITY));
        assertThat(securityPosting.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(securityPosting.getCurrency(), is(CurrencyUnit.EUR));
        assertSame(fixture.portfolio(), securityPosting.getPortfolio());
        assertSame(fixture.security(), securityPosting.getSecurity());
        assertThat(securityPosting.getShares(), is(Values.Share.factorize(5)));
        assertThat(feePosting.getForexAmount(), is(Long.valueOf(Values.Amount.factorize(6))));
        assertThat(feePosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(feePosting.getExchangeRate(), is(new BigDecimal("0.5000")));
        assertThat(taxPosting.getAmount(), is(Values.Amount.factorize(4)));
        assertThat(grossValuePosting.getForexAmount(), is(Long.valueOf(Values.Amount.factorize(240))));
        assertThat(grossValuePosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(grossValuePosting.getExchangeRate(), is(new BigDecimal("0.5000")));
        assertTrue(ledgerEntry.getPostings().stream().noneMatch(posting -> posting.getAccount() != null
                        && posting.getPortfolio() != null));

        assertThat(accountProjection.getRole(), is(LedgerProjectionRole.ACCOUNT));
        assertSame(fixture.account(), accountProjection.getAccount());
        assertPrimaryMembership(accountProjection, cashPosting.getUUID());
        assertThat(portfolioProjection.getRole(), is(LedgerProjectionRole.PORTFOLIO));
        assertSame(fixture.portfolio(), portfolioProjection.getPortfolio());
        assertPrimaryMembership(portfolioProjection, securityPosting.getUUID());
        assertUnitMemberships(accountProjection);
        assertUnitMemberships(portfolioProjection);
        assertThat(accountTransaction.getUUID(), is(accountProjection.getUUID()));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjection.getUUID()));
        assertThat(accountTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(portfolioTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(accountTransaction.getType(), is(accountType));
        assertThat(portfolioTransaction.getType(), is(portfolioType));
        assertThat(accountTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertSame(fixture.security(), portfolioTransaction.getSecurity());
        assertThat(portfolioTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(portfolioTransaction.getUnits().count(), is(3L));
        assertThat(fixture.account().getTransactions(), is(List.of(accountTransaction)));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(portfolioTransaction)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(portfolioTransaction, fixture.client().getAllTransactions().get(0).getTransaction());
        assertCrossEntryReadCompatibility(accountTransaction, portfolioTransaction, fixture.account(),
                        fixture.portfolio());
    }

    private void assertCrossEntryReadCompatibility(AccountTransaction accountTransaction,
                    PortfolioTransaction portfolioTransaction, Account account, Portfolio portfolio)
    {
        assertThat(accountTransaction.getCrossEntry(), instanceOf(BuySellEntry.class));
        assertThat(portfolioTransaction.getCrossEntry(), instanceOf(BuySellEntry.class));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertSame(accountTransaction, portfolioTransaction.getCrossEntry().getCrossTransaction(portfolioTransaction));
        assertSame(portfolio, accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
        assertSame(account, portfolioTransaction.getCrossEntry().getCrossOwner(portfolioTransaction));
    }

    private void assertPrimaryMembership(LedgerProjectionRef projection, String postingUUID)
    {
        assertThat(projection.getPrimaryPostingUUID(), is(postingUUID));
        assertThat(projection.getPrimaryMembership().orElseThrow().getPostingUUID(), is(postingUUID));
    }

    private void assertUnitMemberships(LedgerProjectionRef projection)
    {
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.FEE_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.TAX_UNIT).size(), is(1));
        assertThat(projection.getMembershipsByRole(ProjectionMembershipRole.GROSS_VALUE_UNIT).size(), is(1));
    }

    private BuySellEntry createBuy(Fixture fixture)
    {
        return create(fixture, PortfolioTransaction.Type.BUY);
    }

    private BuySellEntry createSell(Fixture fixture)
    {
        return create(fixture, PortfolioTransaction.Type.SELL);
    }

    private BuySellEntry create(Fixture fixture, PortfolioTransaction.Type type)
    {
        return new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(),
                        type, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), units(), "note", "source");
    }

    private List<Unit> units()
    {
        return List.of(
                        new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)),
                                        new BigDecimal("0.5000")),
                        unit(Unit.Type.TAX, 4),
                        new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(240)),
                                        new BigDecimal("0.5000")));
    }

    private Unit unit(Unit.Type type, int amount)
    {
        return new Unit(type, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount)));
    }

    private Client buySellClient()
    {
        var fixture = fixture();

        createBuy(fixture);
        createSell(fixture);

        return fixture.client();
    }

    private LedgerProjectionRef projection(name.abuchen.portfolio.model.ledger.LedgerEntry entry,
                    LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(name.abuchen.portfolio.model.ledger.LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
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
        var file = File.createTempFile("ledger-buy-sell", ".xml");
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
        var account = account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);
        account.setUpdatedAt(Instant.now());
        portfolio.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    private Account account(String name)
    {
        var account = new Account(name);

        account.setCurrencyCode(CurrencyUnit.EUR);

        return account;
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }
}
