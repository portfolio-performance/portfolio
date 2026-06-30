package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-aware reversal of account and portfolio transfers.
 * These tests make sure transfer sides swap without guessing missing facts or leaving stale owner rows.
 */
@SuppressWarnings("nls")
public class LedgerTransferDirectionConverterTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(0.5);

    /**
     * Verifies that an account transfer can be reversed without creating a new booking.
     * Source and target sides must swap while the ledger entry and projections stay consistent after save/load.
     */
    @Test
    public void testReversesLedgerBackedAccountTransferPreservingIdentityAndTruth() throws Exception
    {
        var fixture = accountFixture();
        var transfer = createAccountTransfer(fixture);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var sourcePostingUUID = posting(entry, fixture.source()).getUUID();
        var targetPostingUUID = posting(entry, fixture.target()).getUUID();
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID();
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID();

        var reversed = converter(fixture.client()).reverse(transfer);
        var reversedSource = reversed.getSourceTransaction();
        var reversedTarget = reversed.getTargetTransaction();

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(LedgerEntryType.CASH_TRANSFER));
        assertThat(posting(entry, fixture.source()).getUUID(), is(sourcePostingUUID));
        assertThat(posting(entry, fixture.target()).getUUID(), is(targetPostingUUID));
        assertThat(projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID(), is(targetProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID(), is(sourceProjectionUUID));
        assertSame(fixture.target(), projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getAccount());
        assertSame(fixture.source(), projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getAccount());
        assertSame(fixture.source(), posting(entry, fixture.source()).getAccount());
        assertSame(fixture.target(), posting(entry, fixture.target()).getAccount());
        assertThat(posting(entry, fixture.source()).getAmount(), is(Values.Amount.factorize(100)));
        assertThat(posting(entry, fixture.source()).getCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting(entry, fixture.source()).getForexAmount(), is((Long) null));
        assertThat(posting(entry, fixture.source()).getForexCurrency(), is((String) null));
        assertThat(posting(entry, fixture.source()).getExchangeRate(), is((BigDecimal) null));
        assertThat(posting(entry, fixture.target()).getAmount(), is(Values.Amount.factorize(200)));
        assertThat(posting(entry, fixture.target()).getCurrency(), is(CurrencyUnit.USD));
        assertThat(posting(entry, fixture.target()).getForexAmount(), is(Values.Amount.factorize(100)));
        assertThat(posting(entry, fixture.target()).getForexCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting(entry, fixture.target()).getExchangeRate().compareTo(ExchangeRate.inverse(EXCHANGE_RATE)),
                        is(0));

        assertThat(fixture.source().getTransactions(), is(List.of(reversedTarget)));
        assertThat(fixture.target().getTransactions(), is(List.of(reversedSource)));
        assertThat(reversedSource.getUUID(), is(targetProjectionUUID));
        assertThat(reversedTarget.getUUID(), is(sourceProjectionUUID));
        assertThat(reversedSource.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(reversedTarget.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(reversedSource.getAmount(), is(Values.Amount.factorize(200)));
        assertThat(reversedSource.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(reversedTarget.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(reversedTarget.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(reversedSource.getDateTime(), is(DATE_TIME));
        assertThat(reversedSource.getNote(), is("note"));
        assertThat(reversedSource.getSource(), is("source"));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(reversedSource, fixture.client().getAllTransactions().get(0).getTransaction());
        assertAccountCrossEntry(reversedSource, reversedTarget, fixture.target(), fixture.source());
        assertThat(new LedgerAccountTransferTransactionCreator(fixture.client()).getSourceExchangeRate(reversed)
                        .orElseThrow().compareTo(EXCHANGE_RATE), is(0));
        assertValid(fixture.client());

        assertAccountTransferRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, sourcePostingUUID,
                        targetPostingUUID, sourceProjectionUUID, targetProjectionUUID);
        assertAccountTransferRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, sourcePostingUUID,
                        targetPostingUUID, sourceProjectionUUID, targetProjectionUUID);
    }

    /**
     * Verifies that a portfolio transfer can be reversed without creating a new booking.
     * Source and target depots must swap while shares, security, and projections remain consistent after save/load.
     */
    @Test
    public void testReversesLedgerBackedPortfolioTransferPreservingIdentityAndTruth() throws Exception
    {
        var fixture = portfolioFixture();
        var transfer = createPortfolioTransfer(fixture);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var sourcePostingUUID = posting(entry, fixture.source()).getUUID();
        var targetPostingUUID = posting(entry, fixture.target()).getUUID();
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getUUID();
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getUUID();

        var reversed = converter(fixture.client()).reverse(transfer);
        var reversedSource = reversed.getSourceTransaction();
        var reversedTarget = reversed.getTargetTransaction();

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(LedgerEntryType.SECURITY_TRANSFER));
        assertThat(posting(entry, fixture.source()).getUUID(), is(sourcePostingUUID));
        assertThat(posting(entry, fixture.target()).getUUID(), is(targetPostingUUID));
        assertThat(projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getUUID(), is(targetProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getUUID(), is(sourceProjectionUUID));
        assertSame(fixture.target(), projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getPortfolio());
        assertSame(fixture.source(), projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getPortfolio());
        assertSame(fixture.source(), posting(entry, fixture.source()).getPortfolio());
        assertSame(fixture.target(), posting(entry, fixture.target()).getPortfolio());
        assertSame(fixture.security(), posting(entry, fixture.source()).getSecurity());
        assertSame(fixture.security(), posting(entry, fixture.target()).getSecurity());
        assertThat(posting(entry, fixture.source()).getShares(), is(Values.Share.factorize(5)));
        assertThat(posting(entry, fixture.target()).getShares(), is(Values.Share.factorize(5)));
        assertThat(posting(entry, fixture.source()).getAmount(), is(Values.Amount.factorize(100)));
        assertThat(posting(entry, fixture.target()).getAmount(), is(Values.Amount.factorize(100)));
        assertThat(posting(entry, fixture.source()).getCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting(entry, fixture.target()).getCurrency(), is(CurrencyUnit.EUR));

        assertThat(fixture.source().getTransactions(), is(List.of(reversedTarget)));
        assertThat(fixture.target().getTransactions(), is(List.of(reversedSource)));
        assertThat(reversedSource.getUUID(), is(targetProjectionUUID));
        assertThat(reversedTarget.getUUID(), is(sourceProjectionUUID));
        assertThat(reversedSource.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(reversedTarget.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(fixture.security(), reversedSource.getSecurity());
        assertSame(fixture.security(), reversedTarget.getSecurity());
        assertThat(reversedSource.getShares(), is(Values.Share.factorize(5)));
        assertThat(reversedTarget.getShares(), is(Values.Share.factorize(5)));
        assertThat(reversedSource.getDateTime(), is(DATE_TIME));
        assertThat(reversedSource.getNote(), is("note"));
        assertThat(reversedSource.getSource(), is("source"));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(reversedSource, fixture.client().getAllTransactions().get(0).getTransaction());
        assertPortfolioCrossEntry(reversedSource, reversedTarget, fixture.target(), fixture.source());
        assertValid(fixture.client());

        assertPortfolioTransferRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, sourcePostingUUID,
                        targetPostingUUID, sourceProjectionUUID, targetProjectionUUID);
        assertPortfolioTransferRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, sourcePostingUUID,
                        targetPostingUUID, sourceProjectionUUID, targetProjectionUUID);
    }

    /**
     * Verifies that a malformed account transfer is rejected before reversal.
     * The converter must not guess a missing transfer side from the remaining projection.
     */
    @Test
    public void testMalformedAccountTransferRejectsBeforeMutation()
    {
        var fixture = accountFixture();
        var transfer = createAccountTransfer(fixture);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var projection = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);

        entry.removeProjectionRef(projection);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class, () -> converter(fixture.client()).reverse(transfer));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that an account transfer without its posting is rejected before reversal.
     * Missing cash facts must not be reconstructed from owner-list projections.
     */
    @Test
    public void testAccountTransferMissingPostingRejectsBeforeMutation()
    {
        var fixture = accountFixture();
        var transfer = createAccountTransfer(fixture);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var posting = posting(entry, fixture.source());

        entry.removePosting(posting);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class, () -> converter(fixture.client()).reverse(transfer));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that a plan-generated account transfer can be reversed safely.
     * The plan reference must follow the same generated booking after the source and target sides swap.
     */
    @Test
    public void testInvestmentPlanReferencedAccountTransferReversesAndUpdatesPlanReference() throws Exception
    {
        var fixture = accountFixture();
        var transfer = createAccountTransfer(fixture);
        var plan = new InvestmentPlan("Plan");
        var entry = fixture.client().getLedger().getEntries().get(0);
        var projectionUUID = transfer.getSourceTransaction().getUUID();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of(
                        (LedgerBackedTransaction) transfer.getSourceTransaction()));
        fixture.client().addPlan(plan);

        converter(fixture.client()).reverse(transfer);

        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(), is(LedgerProjectionRole.TARGET_ACCOUNT));
        assertThat(plan.getTransactions(fixture.client()).get(0).getTransaction().getUUID(), is(projectionUUID));

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(projectionUUID));
    }

    /**
     * Verifies that a malformed portfolio transfer is rejected before reversal.
     * The converter must not guess a missing depot side from the remaining projection.
     */
    @Test
    public void testMalformedPortfolioTransferRejectsBeforeMutation()
    {
        var fixture = portfolioFixture();
        var transfer = createPortfolioTransfer(fixture);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var projection = projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);

        entry.removeProjectionRef(projection);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class, () -> converter(fixture.client()).reverse(transfer));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that a portfolio transfer without its security posting is rejected before reversal.
     * Missing share and security facts must not be reconstructed from owner-list projections.
     */
    @Test
    public void testPortfolioTransferMissingPostingRejectsBeforeMutation()
    {
        var fixture = portfolioFixture();
        var transfer = createPortfolioTransfer(fixture);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var posting = posting(entry, fixture.target());

        entry.removePosting(posting);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class, () -> converter(fixture.client()).reverse(transfer));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that a plan-generated portfolio transfer can be reversed safely.
     * The plan reference must follow the same generated booking after the depot sides swap.
     */
    @Test
    public void testInvestmentPlanReferencedPortfolioTransferReversesAndUpdatesPlanReference() throws Exception
    {
        var fixture = portfolioFixture();
        var transfer = createPortfolioTransfer(fixture);
        var plan = new InvestmentPlan("Plan");
        var entry = fixture.client().getLedger().getEntries().get(0);
        var projectionUUID = transfer.getSourceTransaction().getUUID();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of(
                        (LedgerBackedTransaction) transfer.getSourceTransaction()));
        fixture.client().addPlan(plan);

        converter(fixture.client()).reverse(transfer);

        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(),
                        is(LedgerProjectionRole.TARGET_PORTFOLIO));
        assertThat(plan.getTransactions(fixture.client()).get(0).getTransaction().getUUID(), is(projectionUUID));

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(projectionUUID));
    }

    private void assertAccountTransferRoundtrip(Client client, String entryUUID, String sourcePostingUUID,
                    String targetPostingUUID, String sourceProjectionUUID, String targetProjectionUUID)
    {
        var source = client.getAccounts().get(0);
        var target = client.getAccounts().get(1);
        var entry = client.getLedger().getEntries().get(0);
        var sourceTransaction = target.getTransactions().get(0);
        var targetTransaction = source.getTransactions().get(0);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(posting(entry, source).getUUID(), is(sourcePostingUUID));
        assertThat(posting(entry, target).getUUID(), is(targetPostingUUID));
        assertThat(projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID(), is(targetProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID(), is(sourceProjectionUUID));
        assertThat(sourceTransaction.getUUID(), is(targetProjectionUUID));
        assertThat(targetTransaction.getUUID(), is(sourceProjectionUUID));
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertAccountCrossEntry(sourceTransaction, targetTransaction, target, source);
        assertValid(client);
    }

    private void assertPortfolioTransferRoundtrip(Client client, String entryUUID, String sourcePostingUUID,
                    String targetPostingUUID, String sourceProjectionUUID, String targetProjectionUUID)
    {
        var source = client.getPortfolios().get(0);
        var target = client.getPortfolios().get(1);
        var entry = client.getLedger().getEntries().get(0);
        var sourceTransaction = target.getTransactions().get(0);
        var targetTransaction = source.getTransactions().get(0);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(posting(entry, source).getUUID(), is(sourcePostingUUID));
        assertThat(posting(entry, target).getUUID(), is(targetPostingUUID));
        assertThat(projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getUUID(), is(targetProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getUUID(), is(sourceProjectionUUID));
        assertThat(sourceTransaction.getUUID(), is(targetProjectionUUID));
        assertThat(targetTransaction.getUUID(), is(sourceProjectionUUID));
        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertPortfolioCrossEntry(sourceTransaction, targetTransaction, target, source);
        assertValid(client);
    }

    private void assertAccountCrossEntry(AccountTransaction sourceTransaction, AccountTransaction targetTransaction,
                    Account source, Account target)
    {
        assertThat(sourceTransaction.getCrossEntry(), instanceOf(AccountTransferEntry.class));
        assertThat(targetTransaction.getCrossEntry(), instanceOf(AccountTransferEntry.class));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(sourceTransaction, targetTransaction.getCrossEntry().getCrossTransaction(targetTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertSame(source, targetTransaction.getCrossEntry().getCrossOwner(targetTransaction));
    }

    private void assertPortfolioCrossEntry(PortfolioTransaction sourceTransaction,
                    PortfolioTransaction targetTransaction, Portfolio source, Portfolio target)
    {
        assertThat(sourceTransaction.getCrossEntry(), instanceOf(PortfolioTransferEntry.class));
        assertThat(targetTransaction.getCrossEntry(), instanceOf(PortfolioTransferEntry.class));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(sourceTransaction, targetTransaction.getCrossEntry().getCrossTransaction(targetTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertSame(source, targetTransaction.getCrossEntry().getCrossOwner(targetTransaction));
    }

    private LedgerTransferDirectionConverter converter(Client client)
    {
        return new LedgerTransferDirectionConverter(client);
    }

    private AccountTransferEntry createAccountTransfer(AccountFixture fixture)
    {
        return new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        DATE_TIME, Values.Amount.factorize(100), CurrencyUnit.EUR, Values.Amount.factorize(200),
                        CurrencyUnit.USD, Money.of(CurrencyUnit.USD, Values.Amount.factorize(200)), EXCHANGE_RATE,
                        "note", "source");
    }

    private PortfolioTransferEntry createPortfolioTransfer(PortfolioFixture fixture)
    {
        return new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.source(),
                        fixture.target(), fixture.security(), DATE_TIME, Values.Share.factorize(5),
                        Values.Amount.factorize(100), CurrencyUnit.EUR, "note", "source");
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, Account account)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.CASH)
                        .filter(posting -> posting.getAccount() == account).findFirst().orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, Portfolio portfolio)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.SECURITY)
                        .filter(posting -> posting.getPortfolio() == portfolio).findFirst().orElseThrow();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-transfer-direction", ".xml");
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

    private AccountFixture accountFixture()
    {
        var client = new Client();
        var source = account("Source", CurrencyUnit.EUR);
        var target = account("Target", CurrencyUnit.USD);

        client.addAccount(source);
        client.addAccount(target);

        return new AccountFixture(client, source, target);
    }

    private PortfolioFixture portfolioFixture()
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

        return new PortfolioFixture(client, source, target, security);
    }

    private Account account(String name, String currency)
    {
        var account = new Account(name);

        account.setCurrencyCode(currency);

        return account;
    }

    private record AccountFixture(Client client, Account source, Account target)
    {
    }

    private record PortfolioFixture(Client client, Portfolio source, Portfolio target, Security security)
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
