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
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-aware type changes for account-only transactions.
 * These tests make sure supported account booking changes keep the generated booking traceable and reject unsafe shapes.
 */
@SuppressWarnings("nls")
public class LedgerAccountTypeToggleConverterTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 11, 12, 13);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2026, 6, 10, 0, 0);
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("0.5000");

    /**
     * Verifies that a ledger-backed deposit can be toggled into a removal.
     * The same booking identity and owner-list projection must remain valid after save/load.
     */
    @Test
    public void testTogglesLedgerBackedDepositToRemovalPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesCashType(AccountTransaction.Type.DEPOSIT, LedgerEntryType.REMOVAL,
                        AccountTransaction.Type.REMOVAL);
    }

    /**
     * Verifies that a ledger-backed removal can be toggled into a deposit.
     * The same booking identity and owner-list projection must remain valid after save/load.
     */
    @Test
    public void testTogglesLedgerBackedRemovalToDepositPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesCashType(AccountTransaction.Type.REMOVAL, LedgerEntryType.DEPOSIT,
                        AccountTransaction.Type.DEPOSIT);
    }

    /**
     * Verifies that a ledger-backed interest booking can be toggled into an interest charge.
     * The converter must change the type without replacing the generated account booking.
     */
    @Test
    public void testTogglesLedgerBackedInterestToInterestChargePreservingIdentityAndTruth() throws Exception
    {
        assertTogglesInterestType(AccountTransaction.Type.INTEREST, LedgerEntryType.INTEREST_CHARGE,
                        AccountTransaction.Type.INTEREST_CHARGE);
    }

    /**
     * Verifies that a ledger-backed interest charge can be toggled into interest.
     * The converter must change the type without replacing the generated account booking.
     */
    @Test
    public void testTogglesLedgerBackedInterestChargeToInterestPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesInterestType(AccountTransaction.Type.INTEREST_CHARGE, LedgerEntryType.INTEREST,
                        AccountTransaction.Type.INTEREST);
    }

    @Test
    public void testTogglesLedgerBackedFeesToFeeRefundPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesFeeTaxType(AccountTransaction.Type.FEES, LedgerEntryType.FEES_REFUND,
                        AccountTransaction.Type.FEES_REFUND);
    }

    @Test
    public void testTogglesLedgerBackedFeeRefundToFeesPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesFeeTaxType(AccountTransaction.Type.FEES_REFUND, LedgerEntryType.FEES,
                        AccountTransaction.Type.FEES);
    }

    @Test
    public void testTogglesLedgerBackedTaxesToTaxRefundPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesFeeTaxType(AccountTransaction.Type.TAXES, LedgerEntryType.TAX_REFUND,
                        AccountTransaction.Type.TAX_REFUND);
    }

    @Test
    public void testTogglesLedgerBackedTaxRefundToTaxesPreservingIdentityAndTruth() throws Exception
    {
        assertTogglesFeeTaxType(AccountTransaction.Type.TAX_REFUND, LedgerEntryType.TAXES,
                        AccountTransaction.Type.TAXES);
    }

    /**
     * Verifies that account-only type toggling rejects a missing projection before mutation.
     * The converter must not recreate the account view from partial ledger facts.
     */
    @Test
    public void testMalformedAccountOnlyMissingProjectionRejectsBeforeMutation()
    {
        var fixture = fixture();
        var transaction = createCash(fixture, AccountTransaction.Type.DEPOSIT);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removeProjectionRef(projection(entry));

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).toggle(pair(fixture, transaction)),
                        IllegalArgumentException.class);
    }

    /**
     * Verifies that account-only type toggling rejects a missing cash posting before mutation.
     * The converter must not infer amount or currency facts from the projection.
     */
    @Test
    public void testMalformedAccountOnlyMissingCashPostingRejectsBeforeMutation()
    {
        var fixture = fixture();
        var transaction = createCash(fixture, AccountTransaction.Type.DEPOSIT);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removePosting(posting(entry));

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).toggle(pair(fixture, transaction)),
                        IllegalArgumentException.class);
    }

    /**
     * Verifies that a plan-generated account-only booking can be toggled safely.
     * The plan reference must still resolve to the same generated booking after save/load.
     */
    @Test
    public void testInvestmentPlanReferencedAccountOnlyTogglesAndKeepsPlanReference() throws Exception
    {
        var fixture = fixture();
        var transaction = createCash(fixture, AccountTransaction.Type.DEPOSIT);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");
        var projectionUUID = transaction.getUUID();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of((LedgerBackedTransaction) transaction));
        fixture.client().addPlan(plan);

        converter(fixture).toggle(pair(fixture, transaction));

        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(), is(LedgerProjectionRole.ACCOUNT));
        assertThat(plan.getTransactions(fixture.client()).get(0).getTransaction().getUUID(), is(projectionUUID));

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(projectionUUID));
    }

    private void assertTogglesCashType(AccountTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    AccountTransaction.Type targetTransactionType) throws Exception
    {
        var fixture = fixture();
        var transaction = createCash(fixture, sourceType);
        var entry = fixture.client().getLedger().getEntries().get(0);

        assertToggles(fixture, transaction, entry, targetEntryType, targetTransactionType, null, List.of(), null);
    }

    private void assertTogglesInterestType(AccountTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    AccountTransaction.Type targetTransactionType) throws Exception
    {
        var fixture = fixture();
        var transaction = createInterest(fixture, sourceType);
        var entry = fixture.client().getLedger().getEntries().get(0);

        posting(entry).addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        EX_DATE));

        assertToggles(fixture, transaction, entry, targetEntryType, targetTransactionType, fixture.security(),
                        unitSnapshots(entry), EX_DATE);
    }

    private void assertTogglesFeeTaxType(AccountTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    AccountTransaction.Type targetTransactionType) throws Exception
    {
        var fixture = fixture();
        var transaction = createFeeTax(fixture, sourceType);
        var entry = fixture.client().getLedger().getEntries().get(0);

        assertToggles(fixture, transaction, entry, targetEntryType, targetTransactionType, fixture.security(),
                        List.of(), null);
    }

    private void assertToggles(Fixture fixture, AccountTransaction transaction, LedgerEntry entry,
                    LedgerEntryType targetEntryType, AccountTransaction.Type targetTransactionType, Security security,
                    List<PostingSnapshot> expectedUnitPostings, LocalDateTime expectedExDate) throws Exception
    {
        var entryUUID = entry.getUUID();
        var cashPostingUUID = posting(entry).getUUID();
        var projectionUUID = projection(entry).getUUID();

        var toggled = converter(fixture).toggle(pair(fixture, transaction));

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(posting(entry).getUUID(), is(cashPostingUUID));
        assertThat(projection(entry).getUUID(), is(projectionUUID));
        assertSame(fixture.account(), projection(entry).getAccount());
        assertThat(unitSnapshots(entry), is(expectedUnitPostings));

        assertThat(fixture.account().getTransactions(), is(List.of(toggled)));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(toggled, fixture.client().getAllTransactions().get(0).getTransaction());
        assertThat(toggled.getUUID(), is(projectionUUID));
        assertThat(toggled, instanceOf(LedgerBackedTransaction.class));
        assertThat(toggled.getType(), is(targetTransactionType));
        assertThat(toggled.getDateTime(), is(DATE_TIME));
        assertThat(toggled.getNote(), is("note"));
        assertThat(toggled.getSource(), is("source"));
        assertThat(toggled.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(toggled.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertSame(security, toggled.getSecurity());
        assertThat(toggled.getShares(), is(0L));
        assertThat(toggled.getExDate(), is(expectedExDate));
        assertValid(fixture.client());

        assertRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, cashPostingUUID, projectionUUID, targetEntryType,
                        targetTransactionType, security != null, expectedExDate);
        assertRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, cashPostingUUID, projectionUUID,
                        targetEntryType, targetTransactionType, security != null, expectedExDate);
    }

    private void assertRoundtrip(Client client, String entryUUID, String cashPostingUUID, String projectionUUID,
                    LedgerEntryType entryType, AccountTransaction.Type transactionType, boolean hasSecurity,
                    LocalDateTime expectedExDate)
    {
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        var entry = client.getLedger().getEntries().get(0);
        var transaction = client.getAccounts().get(0).getTransactions().get(0);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(entryType));
        assertThat(posting(entry).getUUID(), is(cashPostingUUID));
        assertThat(projection(entry).getUUID(), is(projectionUUID));
        assertThat(transaction.getUUID(), is(projectionUUID));
        assertThat(transaction.getType(), is(transactionType));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(transaction.getSecurity() != null, is(hasSecurity));
        assertThat(transaction.getExDate(), is(expectedExDate));
        assertValid(client);
    }

    private <T extends Throwable> void assertRejectsWithoutMutation(Fixture fixture, ThrowingRunnable runnable,
                    Class<T> expectedType)
    {
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(expectedType, runnable::run);
        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    private AccountTransaction createCash(Fixture fixture, AccountTransaction.Type type)
    {
        return new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.account(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, List.of(), "note", "source");
    }

    private AccountTransaction createInterest(Fixture fixture, AccountTransaction.Type type)
    {
        return new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.account(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(), units(), "note",
                        "source");
    }

    private AccountTransaction createFeeTax(Fixture fixture, AccountTransaction.Type type)
    {
        return new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.account(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(), List.of(), "note",
                        "source");
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

    private LedgerAccountTypeToggleConverter converter(Fixture fixture)
    {
        return new LedgerAccountTypeToggleConverter(fixture.client());
    }

    private TransactionPair<AccountTransaction> pair(Fixture fixture, AccountTransaction transaction)
    {
        return new TransactionPair<>(fixture.account(), transaction);
    }

    private LedgerProjectionRef projection(LedgerEntry entry)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT)
                        .findFirst().orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry)
    {
        return LedgerProjectionSupport.primaryPosting(entry, projection(entry));
    }

    private List<PostingSnapshot> unitSnapshots(LedgerEntry entry)
    {
        var primaryPosting = posting(entry);

        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.FEE
                        || posting.getType() == LedgerPostingType.TAX
                        || posting.getType() == LedgerPostingType.GROSS_VALUE).filter(posting -> posting != primaryPosting)
                        .map(PostingSnapshot::capture).toList();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-account-type-toggle-converter", ".xml");
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
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());
        client.addAccount(account);
        client.addSecurity(security);

        return new Fixture(client, account, security);
    }

    @FunctionalInterface
    private interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    private record Fixture(Client client, Account account, Security security)
    {
    }

    private record Snapshot(List<EntrySnapshot> entries, List<String> accountTransactions,
                    List<String> allTransactions)
    {
        static Snapshot capture(Client client)
        {
            return new Snapshot(client.getLedger().getEntries().stream().map(EntrySnapshot::capture).toList(),
                            client.getAccounts().stream().flatMap(account -> account.getTransactions().stream())
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
                    List<ParameterSnapshot> parameters)
    {
        static PostingSnapshot capture(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(),
                            posting.getParameters().stream().map(ParameterSnapshot::capture).toList());
        }
    }

    private record ParameterSnapshot(LedgerParameterType type,
                    LedgerParameter.ValueKind valueKind, Object value)
    {
        static ParameterSnapshot capture(LedgerParameter<?> parameter)
        {
            return new ParameterSnapshot(parameter.getType(), parameter.getValueKind(), parameter.getValue());
        }
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, Account account,
                    String primaryPostingUUID, String postingGroupUUID)
    {
        static ProjectionSnapshot capture(LedgerProjectionRef projection)
        {
            return new ProjectionSnapshot(projection.getUUID(), projection.getRole(), projection.getAccount(),
                            projection.getPrimaryPostingUUID(), projection.getPostingGroupUUID());
        }
    }
}
