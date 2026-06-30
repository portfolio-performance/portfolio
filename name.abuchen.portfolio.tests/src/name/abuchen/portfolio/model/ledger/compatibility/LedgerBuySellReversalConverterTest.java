package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

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
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
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
 * Tests ledger-aware reversal between buy and sell transactions.
 * These tests make sure the same booking changes direction without replacing projections or plan references incorrectly.
 */
@SuppressWarnings("nls")
public class LedgerBuySellReversalConverterTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 8, 9, 10);
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("0.5000");

    /**
     * Verifies that a ledger-backed buy can be reversed into a sell through the converter.
     * The booking identity, projections, units, and owner lists must stay consistent after save/load.
     */
    @Test
    public void testReversesLedgerBackedBuyToSellPreservingIdentityAndTruth() throws Exception
    {
        assertReversesBuySell(PortfolioTransaction.Type.BUY, LedgerEntryType.SELL, PortfolioTransaction.Type.SELL,
                        Values.Amount.factorize(113));
    }

    /**
     * Verifies that a ledger-backed sell can be reversed into a buy through the converter.
     * The booking identity, projections, units, and owner lists must stay consistent after save/load.
     */
    @Test
    public void testReversesLedgerBackedSellToBuyPreservingIdentityAndTruth() throws Exception
    {
        assertReversesBuySell(PortfolioTransaction.Type.SELL, LedgerEntryType.BUY, PortfolioTransaction.Type.BUY,
                        Values.Amount.factorize(127));
    }

    /**
     * Verifies that reversal stops before mutation when the account-side projection is missing.
     * The converter must not rebuild a missing runtime view from guesses.
     */
    @Test
    public void testMalformedBuySellMissingAccountProjectionRejectsBeforeMutation()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.ACCOUNT));
        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(buySell), IllegalArgumentException.class);
    }

    /**
     * Verifies that reversal stops before mutation when the portfolio-side projection is missing.
     * The ledger entry and owner lists must remain unchanged instead of creating a second truth.
     */
    @Test
    public void testMalformedBuySellMissingPortfolioProjectionRejectsBeforeMutation()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.PORTFOLIO));
        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(buySell), IllegalArgumentException.class);
    }

    /**
     * Verifies that reversal stops before mutation when the cash posting is missing.
     * A buy/sell direction change cannot infer the missing account-side cash facts safely.
     */
    @Test
    public void testMalformedBuySellMissingCashPostingRejectsBeforeMutation()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removePosting(posting(entry, LedgerPostingType.CASH));
        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(buySell), IllegalArgumentException.class);
    }

    /**
     * Verifies that reversal stops before mutation when the security posting is missing.
     * The converter must not infer security facts from the projected portfolio transaction.
     */
    @Test
    public void testMalformedBuySellMissingSecurityPostingRejectsBeforeMutation()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removePosting(posting(entry, LedgerPostingType.SECURITY));
        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(buySell), IllegalArgumentException.class);
    }

    /**
     * Verifies that buy/sell reversal does not reinterpret unsupported forex cash facts.
     * The converter must reject before mutation when it cannot preserve those facts safely.
     */
    @Test
    public void testUnsupportedCashForexRejectsBeforeMutation()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var cashPosting = posting(fixture.client().getLedger().getEntries().get(0), LedgerPostingType.CASH);

        cashPosting.setForexAmount(Values.Amount.factorize(246));
        cashPosting.setForexCurrency(CurrencyUnit.USD);
        cashPosting.setExchangeRate(EXCHANGE_RATE);

        assertThat(converter(fixture).canReverseSafely(buySell), is(false));
        assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(buySell),
                        UnsupportedOperationException.class);
    }

    /**
     * Verifies that a plan-generated buy/sell booking can be reversed without losing the plan reference.
     * The execution ref must still resolve to the same projected booking after save/load.
     */
    @Test
    public void testInvestmentPlanReferencedBuySellReversesAndKeepsPlanReference() throws Exception
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");
        var projectionUUID = buySell.getPortfolioTransaction().getUUID();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef
                        .of((LedgerBackedTransaction) buySell.getPortfolioTransaction()));
        fixture.client().addPlan(plan);

        converter(fixture).reverse(buySell);

        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(), is(LedgerProjectionRole.PORTFOLIO));
        assertThat(plan.getTransactions(fixture.client()).get(0).getTransaction().getUUID(), is(projectionUUID));

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(projectionUUID));
    }

    /**
     * Verifies that an entry-only plan reference blocks buy/sell reversal.
     * The converter must not guess which projection the plan intended to follow.
     */
    @Test
    public void testEntryOnlyInvestmentPlanExecutionRefRejectsBuySellReversalBeforeMutation()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");

        plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(entry.getUUID(), null, null));
        fixture.client().addPlan(plan);

        var exception = assertRejectsWithoutMutation(fixture, () -> converter(fixture).reverse(buySell),
                        UnsupportedOperationException.class);
        assertThat(exception.getMessage(), containsString("ambiguous"));
        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
    }

    private void assertReversesBuySell(PortfolioTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    PortfolioTransaction.Type targetTransactionType, long expectedAmount) throws Exception
    {
        var fixture = fixture();
        var buySell = create(fixture, sourceType);
        var accountTransaction = buySell.getAccountTransaction();
        var portfolioTransaction = buySell.getPortfolioTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var cashPostingUUID = posting(entry, LedgerPostingType.CASH).getUUID();
        var securityPostingUUID = posting(entry, LedgerPostingType.SECURITY).getUUID();
        var accountProjectionUUID = projection(entry, LedgerProjectionRole.ACCOUNT).getUUID();
        var portfolioProjectionUUID = projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID();
        var unitSnapshots = unitSnapshots(entry);

        var reversed = converter(fixture).reverse(buySell);
        var reversedAccount = reversed.getAccountTransaction();
        var reversedPortfolio = reversed.getPortfolioTransaction();

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(posting(entry, LedgerPostingType.CASH).getUUID(), is(cashPostingUUID));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(posting(entry, LedgerPostingType.CASH).getAmount(), is(expectedAmount));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getAmount(), is(expectedAmount));
        assertThat(unitSnapshots(entry), is(unitSnapshots));
        assertThat(projection(entry, LedgerProjectionRole.ACCOUNT).getUUID(), is(accountProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID(), is(portfolioProjectionUUID));

        assertThat(fixture.account().getTransactions(), is(List.of(reversedAccount)));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(reversedPortfolio)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(reversedPortfolio, fixture.client().getAllTransactions().get(0).getTransaction());
        assertThat(reversedAccount.getUUID(), is(accountProjectionUUID));
        assertThat(reversedPortfolio.getUUID(), is(portfolioProjectionUUID));
        assertThat(reversedAccount, instanceOf(LedgerBackedTransaction.class));
        assertThat(reversedPortfolio, instanceOf(LedgerBackedTransaction.class));
        assertThat(reversedAccount.getType().name(), is(targetTransactionType.name()));
        assertThat(reversedPortfolio.getType(), is(targetTransactionType));
        assertSame(reversedPortfolio, reversedAccount.getCrossEntry().getCrossTransaction(reversedAccount));
        assertSame(reversedAccount, reversedPortfolio.getCrossEntry().getCrossTransaction(reversedPortfolio));
        assertThat(reversedAccount.getDateTime(), is(DATE_TIME));
        assertThat(reversedPortfolio.getDateTime(), is(DATE_TIME));
        assertThat(reversedAccount.getNote(), is("note"));
        assertThat(reversedPortfolio.getNote(), is("note"));
        assertThat(reversedAccount.getSource(), is("source"));
        assertThat(reversedPortfolio.getSource(), is("source"));
        assertSame(fixture.security(), reversedPortfolio.getSecurity());
        assertThat(reversedPortfolio.getShares(), is(Values.Share.factorize(5)));
        assertThat(reversedAccount.getAmount(), is(expectedAmount));
        assertThat(reversedPortfolio.getAmount(), is(expectedAmount));
        assertThat(reversedPortfolio.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120))));
        assertThat(reversedPortfolio.getUnit(Unit.Type.GROSS_VALUE).orElseThrow().getAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120))));
        assertThat(reversedPortfolio.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR,
                        Values.Amount.factorize(3))));
        assertThat(reversedPortfolio.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR,
                        Values.Amount.factorize(4))));
        assertThat(accountTransaction.getUUID(), is(accountProjectionUUID));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjectionUUID));
        assertValid(fixture.client());

        assertRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, cashPostingUUID, securityPostingUUID,
                        accountProjectionUUID, portfolioProjectionUUID, targetEntryType, targetTransactionType,
                        expectedAmount);
        assertRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, cashPostingUUID, securityPostingUUID,
                        accountProjectionUUID, portfolioProjectionUUID, targetEntryType, targetTransactionType,
                        expectedAmount);
    }

    private void assertRoundtrip(Client client, String entryUUID, String cashPostingUUID, String securityPostingUUID,
                    String accountProjectionUUID, String portfolioProjectionUUID, LedgerEntryType entryType,
                    PortfolioTransaction.Type transactionType, long expectedAmount)
    {
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        var entry = client.getLedger().getEntries().get(0);
        var accountTransaction = client.getAccounts().get(0).getTransactions().get(0);
        var portfolioTransaction = client.getPortfolios().get(0).getTransactions().get(0);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(entryType));
        assertThat(posting(entry, LedgerPostingType.CASH).getUUID(), is(cashPostingUUID));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(posting(entry, LedgerPostingType.CASH).getAmount(), is(expectedAmount));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getAmount(), is(expectedAmount));
        assertThat(projection(entry, LedgerProjectionRole.ACCOUNT).getUUID(), is(accountProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID(), is(portfolioProjectionUUID));
        assertThat(accountTransaction.getType().name(), is(transactionType.name()));
        assertThat(portfolioTransaction.getType(), is(transactionType));
        assertThat(accountTransaction.getUUID(), is(accountProjectionUUID));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjectionUUID));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertSame(accountTransaction, portfolioTransaction.getCrossEntry().getCrossTransaction(portfolioTransaction));
        assertThat(portfolioTransaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120))));
        assertValid(client);
    }

    private <T extends Throwable> T assertRejectsWithoutMutation(Fixture fixture, ThrowingRunnable runnable,
                    Class<T> expectedType)
    {
        var snapshot = Snapshot.capture(fixture.client());

        var exception = assertThrows(expectedType, runnable::run);
        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
        return exception;
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
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)), EXCHANGE_RATE),
                        new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4))),
                        new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(240)), EXCHANGE_RATE));
    }

    private LedgerBuySellReversalConverter converter(Fixture fixture)
    {
        return new LedgerBuySellReversalConverter(fixture.client());
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
    }

    private List<PostingSnapshot> unitSnapshots(LedgerEntry entry)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.FEE
                        || posting.getType() == LedgerPostingType.TAX
                        || posting.getType() == LedgerPostingType.GROSS_VALUE).map(PostingSnapshot::capture).toList();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-buy-sell-reversal-converter", ".xml");
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

        assertThat(result.format(), result.isOK(), is(true));
    }

    private Fixture fixture()
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        account.setUpdatedAt(Instant.now());
        portfolio.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    @FunctionalInterface
    private interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record Snapshot(List<EntrySnapshot> entries, List<String> accountTransactions,
                    List<String> portfolioTransactions, List<String> allTransactions)
    {
        static Snapshot capture(Client client)
        {
            return new Snapshot(client.getLedger().getEntries().stream().map(EntrySnapshot::capture).toList(),
                            client.getAccounts().stream().flatMap(account -> account.getTransactions().stream())
                                            .map(Transaction::getUUID).toList(),
                            client.getPortfolios().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                                            .map(Transaction::getUUID).toList(),
                            client.getAllTransactions().stream().map(pair -> pair.getTransaction().getUUID())
                                            .toList());
        }
    }

    private record EntrySnapshot(String uuid, LedgerEntryType type, List<PostingSnapshot> postings,
                    List<ProjectionSnapshot> projections)
    {
        static EntrySnapshot capture(LedgerEntry entry)
        {
            return new EntrySnapshot(entry.getUUID(), entry.getType(),
                            entry.getPostings().stream().map(PostingSnapshot::capture).toList(),
                            entry.getProjectionRefs().stream().map(ProjectionSnapshot::capture).toList());
        }
    }

    private record PostingSnapshot(String uuid, LedgerPostingType type, Long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, Security security, Long shares, Account account,
                    Portfolio portfolio)
    {
        static PostingSnapshot capture(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(), posting.getPortfolio());
        }
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, Account account, Portfolio portfolio,
                    String primaryPostingUUID, String postingGroupUUID)
    {
        static ProjectionSnapshot capture(LedgerProjectionRef projection)
        {
            return new ProjectionSnapshot(projection.getUUID(), projection.getRole(), projection.getAccount(),
                            projection.getPortfolio(), projection.getPrimaryPostingUUID(),
                            projection.getPostingGroupUUID());
        }
    }
}
