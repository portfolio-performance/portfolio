package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests the ledger-aware conversion from an account transfer to separate deposit and removal transactions.
 * These tests make sure generated bookings stay traceable and stale transfer references are not left behind.
 */
@SuppressWarnings("nls")
public class LedgerAccountTransferToDepositRemovalConverterTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 16, 10, 11);
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("0.5000");

    /**
     * Verifies that an account transfer can be split into one removal and one deposit.
     * The source side must remain the removal and the target side must remain the deposit after save/load.
     * This protects the split from leaving duplicate transfer truth behind.
     */
    @Test
    public void testSplitsSameCurrencyTransferPreservingIdentityAndTruth() throws Exception
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var transferEntryUUID = entry.getUUID();
        var sourcePostingUUID = posting(entry, fixture.source()).getUUID();
        var targetPostingUUID = posting(entry, fixture.target()).getUUID();
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID();
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID();

        var result = converter(fixture).split(transfer);

        assertSplit(fixture, transferEntryUUID, sourcePostingUUID, targetPostingUUID, sourceProjectionUUID,
                        targetProjectionUUID, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR);
        assertThat(result.removal().getUUID(), is(sourceProjectionUUID));
        assertThat(result.deposit().getUUID(), is(targetProjectionUUID));
        assertThat(posting(removalEntry(fixture.client()), fixture.source()).getExchangeRate(), is(nullValue()));
        assertThat(posting(depositEntry(fixture.client()), fixture.target()).getExchangeRate(), is(nullValue()));

        assertRoundtrip(loadXml(saveXml(fixture.client())), transferEntryUUID, sourcePostingUUID, targetPostingUUID,
                        sourceProjectionUUID, targetProjectionUUID, CurrencyUnit.EUR, CurrencyUnit.EUR, null);
        assertRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), transferEntryUUID, sourcePostingUUID,
                        targetPostingUUID, sourceProjectionUUID, targetProjectionUUID, CurrencyUnit.EUR,
                        CurrencyUnit.EUR, null);
    }

    /**
     * Verifies that a cross-currency transfer split keeps the source-side forex facts explicit.
     * When no better rate exists, the removal side uses the default exchange rate one and survives save/load.
     */
    @Test
    public void testSplitsCrossCurrencyTransferWithFallbackExchangeRateOne() throws Exception
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var transfer = createTransfer(fixture, Values.Amount.factorize(100), CurrencyUnit.EUR,
                        Values.Amount.factorize(200), CurrencyUnit.USD, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var transferEntryUUID = entry.getUUID();
        var sourcePostingUUID = posting(entry, fixture.source()).getUUID();
        var targetPostingUUID = posting(entry, fixture.target()).getUUID();
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID();
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID();

        converter(fixture).split(transfer);

        assertSplit(fixture, transferEntryUUID, sourcePostingUUID, targetPostingUUID, sourceProjectionUUID,
                        targetProjectionUUID, Values.Amount.factorize(100), CurrencyUnit.EUR,
                        Values.Amount.factorize(200), CurrencyUnit.USD);

        var removalPosting = posting(removalEntry(fixture.client()), fixture.source());

        assertThat(removalPosting.getForexAmount(), is(Values.Amount.factorize(200)));
        assertThat(removalPosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(removalPosting.getExchangeRate(), is(BigDecimal.ONE));
        assertThat(posting(depositEntry(fixture.client()), fixture.target()).getExchangeRate(), is(nullValue()));

        assertRoundtrip(loadXml(saveXml(fixture.client())), transferEntryUUID, sourcePostingUUID, targetPostingUUID,
                        sourceProjectionUUID, targetProjectionUUID, CurrencyUnit.EUR, CurrencyUnit.USD,
                        BigDecimal.ONE);
        assertRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), transferEntryUUID, sourcePostingUUID,
                        targetPostingUUID, sourceProjectionUUID, targetProjectionUUID, CurrencyUnit.EUR,
                        CurrencyUnit.USD, BigDecimal.ONE);
    }

    /**
     * Verifies that an existing valid forex amount is preserved when a transfer is split.
     * The converter must not replace known user facts with the default exchange rate.
     */
    @Test
    public void testPreservesExistingValidForexWhenSplittingCrossCurrencyTransfer()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.USD);
        var transfer = createTransfer(fixture, Values.Amount.factorize(100), CurrencyUnit.EUR,
                        Values.Amount.factorize(200), CurrencyUnit.USD,
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(200)), EXCHANGE_RATE);

        converter(fixture).split(transfer);

        var removalPosting = posting(removalEntry(fixture.client()), fixture.source());

        assertThat(removalPosting.getForexAmount(), is(Values.Amount.factorize(200)));
        assertThat(removalPosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(removalPosting.getExchangeRate(), is(EXCHANGE_RATE));
    }

    /**
     * Verifies that transfers with extra unit postings are not split automatically.
     * Without a defined split policy for those facts, the converter must reject before changing the ledger.
     */
    @Test
    public void testUnitBearingTransferRejectsBeforeMutation()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var fee = new LedgerPosting();

        fee.setType(LedgerPostingType.FEE);
        fee.setAmount(Values.Amount.factorize(1));
        fee.setCurrency(CurrencyUnit.EUR);
        entry.addPosting(fee);

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).split(transfer),
                        UnsupportedOperationException.class);
    }

    /**
     * Verifies that a cash transfer carrying security facts is not split into deposit and removal.
     * The converter must reject before mutation because the security side cannot be inferred safely.
     */
    @Test
    public void testSecurityBearingTransferRejectsBeforeMutation()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var security = new Security("Security", CurrencyUnit.EUR);

        fixture.client().addSecurity(security);
        posting(entry, fixture.source()).setSecurity(security);
        posting(entry, fixture.source()).setShares(Values.Share.factorize(1));

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).split(transfer),
                        UnsupportedOperationException.class);
    }

    /**
     * Verifies that a malformed transfer shape is rejected before the split starts.
     * The original ledger entry and owner lists must stay unchanged when a required projection is missing.
     */
    @Test
    public void testMalformedTransferRejectsBeforeMutation()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.TARGET_ACCOUNT));

        assertRejectsWithoutMutation(fixture, () -> converter(fixture).split(transfer), IllegalArgumentException.class);
    }

    /**
     * Verifies that a plan-generated transfer referenced on the source side continues as a removal.
     * The old cash transfer must no longer be referenced, and the execution ref must resolve after save/load.
     */
    @Test
    public void testInvestmentPlanSourceExecutionRefMigratesToRemovalAfterTransferSplit() throws Exception
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID();
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID();
        var plan = planWithExecutionRef(fixture.client(), (LedgerBackedTransaction) transfer.getSourceTransaction());

        converter(fixture).split(transfer);

        var removalEntry = removalEntry(fixture.client());

        assertExecutionRef(plan, 0, removalEntry.getUUID(), sourceProjectionUUID, LedgerProjectionRole.ACCOUNT);
        assertResolvedPlanTransaction(fixture.client(), plan, 0, AccountTransaction.Type.REMOVAL,
                        sourceProjectionUUID);
        assertNoExecutionRefTargetsCashTransfer(fixture.client(), plan);

        assertExecutionRefResolvesAfterRoundtrip(loadXml(saveXml(fixture.client())), 0, removalEntry.getUUID(),
                        sourceProjectionUUID, AccountTransaction.Type.REMOVAL);
        assertExecutionRefResolvesAfterRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), 0,
                        removalEntry.getUUID(), sourceProjectionUUID, AccountTransaction.Type.REMOVAL);
        assertThat(projection(depositEntry(fixture.client()), LedgerProjectionRole.ACCOUNT).getUUID(),
                        is(targetProjectionUUID));
    }

    /**
     * Verifies that a plan-generated transfer referenced on the target side continues as a deposit.
     * The execution ref must point to the new deposit booking and resolve after save/load.
     */
    @Test
    public void testInvestmentPlanTargetExecutionRefMigratesToDepositAfterTransferSplit() throws Exception
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID();
        var plan = planWithExecutionRef(fixture.client(), (LedgerBackedTransaction) transfer.getTargetTransaction());

        converter(fixture).split(transfer);

        var depositEntry = depositEntry(fixture.client());

        assertExecutionRef(plan, 0, depositEntry.getUUID(), targetProjectionUUID, LedgerProjectionRole.ACCOUNT);
        assertResolvedPlanTransaction(fixture.client(), plan, 0, AccountTransaction.Type.DEPOSIT,
                        targetProjectionUUID);
        assertNoExecutionRefTargetsCashTransfer(fixture.client(), plan);

        assertExecutionRefResolvesAfterRoundtrip(loadXml(saveXml(fixture.client())), 0, depositEntry.getUUID(),
                        targetProjectionUUID, AccountTransaction.Type.DEPOSIT);
        assertExecutionRefResolvesAfterRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), 0,
                        depositEntry.getUUID(), targetProjectionUUID, AccountTransaction.Type.DEPOSIT);
    }

    /**
     * Verifies that source and target execution refs can both survive one transfer split.
     * The source ref must follow the removal and the target ref must follow the deposit independently.
     */
    @Test
    public void testInvestmentPlanExecutionRefsOnBothTransferSidesMigrateAfterTransferSplit() throws Exception
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID();
        var targetProjectionUUID = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getUUID();
        var plan = newPlan();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of(
                        (LedgerBackedTransaction) transfer.getSourceTransaction()));
        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of(
                        (LedgerBackedTransaction) transfer.getTargetTransaction()));
        fixture.client().addPlan(plan);

        converter(fixture).split(transfer);

        var removalEntry = removalEntry(fixture.client());
        var depositEntry = depositEntry(fixture.client());

        assertExecutionRef(plan, 0, removalEntry.getUUID(), sourceProjectionUUID, LedgerProjectionRole.ACCOUNT);
        assertExecutionRef(plan, 1, depositEntry.getUUID(), targetProjectionUUID, LedgerProjectionRole.ACCOUNT);
        assertResolvedPlanTransaction(fixture.client(), plan, 0, AccountTransaction.Type.REMOVAL,
                        sourceProjectionUUID);
        assertResolvedPlanTransaction(fixture.client(), plan, 1, AccountTransaction.Type.DEPOSIT,
                        targetProjectionUUID);
        assertNoExecutionRefTargetsCashTransfer(fixture.client(), plan);

        var xmlClient = loadXml(saveXml(fixture.client()));
        assertExecutionRefResolvesAfterRoundtrip(xmlClient, 0, removalEntry.getUUID(), sourceProjectionUUID,
                        AccountTransaction.Type.REMOVAL);
        assertExecutionRefResolvesAfterRoundtrip(xmlClient, 1, depositEntry.getUUID(), targetProjectionUUID,
                        AccountTransaction.Type.DEPOSIT);

        var protobufClient = loadProtobuf(saveProtobuf(fixture.client()));
        assertExecutionRefResolvesAfterRoundtrip(protobufClient, 0, removalEntry.getUUID(), sourceProjectionUUID,
                        AccountTransaction.Type.REMOVAL);
        assertExecutionRefResolvesAfterRoundtrip(protobufClient, 1, depositEntry.getUUID(), targetProjectionUUID,
                        AccountTransaction.Type.DEPOSIT);
    }

    /**
     * Verifies that an entry-only plan reference blocks an account-transfer split.
     * Without a source or target side, the generated booking cannot be continued as either removal or deposit.
     */
    @Test
    public void testEntryOnlyInvestmentPlanExecutionRefRejectsTransferSplitBeforeMutation()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = newPlan();

        plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(entry.getUUID(), null, null));
        fixture.client().addPlan(plan);

        var exception = assertRejectsWithoutMutation(fixture, () -> converter(fixture).split(transfer),
                        UnsupportedOperationException.class);
        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_045
                        .message("Ledger plan reference cannot be mapped to a split transfer side")));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(nullValue()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(), is(nullValue()));
    }

    /**
     * Verifies that a contradictory plan reference is rejected before mutation.
     * A ref may not point to the source projection while asking to be treated as the target side.
     */
    @Test
    public void testConflictingInvestmentPlanExecutionRefRoleRejectsTransferSplitBeforeMutation()
    {
        var fixture = fixture(CurrencyUnit.EUR, CurrencyUnit.EUR);
        var transfer = createTransfer(fixture, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, null);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var sourceProjectionUUID = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getUUID();
        var plan = newPlan();

        plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(entry.getUUID(), sourceProjectionUUID,
                        LedgerProjectionRole.TARGET_ACCOUNT));
        fixture.client().addPlan(plan);

        var exception = assertRejectsWithoutMutation(fixture, () -> converter(fixture).split(transfer),
                        UnsupportedOperationException.class);
        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_045
                        .message("Ledger plan reference cannot be mapped to a split transfer side")));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(sourceProjectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(), is(LedgerProjectionRole.TARGET_ACCOUNT));
    }

    private InvestmentPlan planWithExecutionRef(Client client, LedgerBackedTransaction transaction)
    {
        var plan = newPlan();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of(transaction));
        client.addPlan(plan);

        return plan;
    }

    private InvestmentPlan newPlan()
    {
        var plan = new InvestmentPlan("Plan");

        plan.setStart(DATE_TIME);
        plan.setType(InvestmentPlan.Type.DEPOSIT);

        return plan;
    }

    private void assertExecutionRef(InvestmentPlan plan, int index, String entryUUID, String projectionUUID,
                    LedgerProjectionRole role)
    {
        var ref = plan.getLedgerExecutionRefs().get(index);

        assertThat(ref.getLedgerEntryUUID(), is(entryUUID));
        assertThat(ref.getProjectionUUID(), is(projectionUUID));
        assertThat(ref.getProjectionRole(), is(role));
    }

    private void assertResolvedPlanTransaction(Client client, InvestmentPlan plan, int index,
                    AccountTransaction.Type type, String transactionUUID)
    {
        var transactions = plan.getTransactions(client);
        var transaction = transactions.get(index).getTransaction();

        assertThat(transactions.size(), is(plan.getLedgerExecutionRefs().size()));
        assertThat(transaction, instanceOf(AccountTransaction.class));
        assertThat(((AccountTransaction) transaction).getType(), is(type));
        assertThat(transaction.getUUID(), is(transactionUUID));
    }

    private void assertNoExecutionRefTargetsCashTransfer(Client client, InvestmentPlan plan)
    {
        for (var pair : plan.getTransactions(client))
        {
            var transaction = pair.getTransaction();

            assertThat(transaction, instanceOf(AccountTransaction.class));
            assertThat(((AccountTransaction) transaction).getType() == AccountTransaction.Type.TRANSFER_IN
                            || ((AccountTransaction) transaction).getType() == AccountTransaction.Type.TRANSFER_OUT,
                            is(false));
        }
    }

    private void assertExecutionRefResolvesAfterRoundtrip(Client client, int index, String entryUUID,
                    String projectionUUID, AccountTransaction.Type type)
    {
        var plan = client.getPlans().get(0);

        assertExecutionRef(plan, index, entryUUID, projectionUUID, LedgerProjectionRole.ACCOUNT);
        assertResolvedPlanTransaction(client, plan, index, type, projectionUUID);
        assertNoExecutionRefTargetsCashTransfer(client, plan);
        assertValid(client);
    }

    private void assertSplit(Fixture fixture, String transferEntryUUID, String sourcePostingUUID,
                    String targetPostingUUID, String sourceProjectionUUID, String targetProjectionUUID,
                    long sourceAmount, String sourceCurrency, long targetAmount, String targetCurrency)
    {
        assertThat(fixture.client().getLedger().getEntries().size(), is(2));

        var removalEntry = removalEntry(fixture.client());
        var depositEntry = depositEntry(fixture.client());
        var sourcePosting = posting(removalEntry, fixture.source());
        var targetPosting = posting(depositEntry, fixture.target());
        var sourceProjection = projection(removalEntry, LedgerProjectionRole.ACCOUNT);
        var targetProjection = projection(depositEntry, LedgerProjectionRole.ACCOUNT);
        var removal = fixture.source().getTransactions().get(0);
        var deposit = fixture.target().getTransactions().get(0);

        assertThat(removalEntry.getUUID(), is(transferEntryUUID));
        assertThat(removalEntry.getType(), is(LedgerEntryType.REMOVAL));
        assertThat(depositEntry.getType(), is(LedgerEntryType.DEPOSIT));
        assertThat(depositEntry.getUUID().equals(transferEntryUUID), is(false));
        assertThat(sourcePosting.getUUID(), is(sourcePostingUUID));
        assertThat(targetPosting.getUUID(), is(targetPostingUUID));
        assertThat(sourceProjection.getUUID(), is(sourceProjectionUUID));
        assertThat(targetProjection.getUUID(), is(targetProjectionUUID));
        assertThat(sourceProjection.getPrimaryPostingUUID(), is(sourcePostingUUID));
        assertThat(targetProjection.getPrimaryPostingUUID(), is(targetPostingUUID));
        assertSame(fixture.source(), sourceProjection.getAccount());
        assertSame(fixture.target(), targetProjection.getAccount());
        assertSame(fixture.source(), sourcePosting.getAccount());
        assertSame(fixture.target(), targetPosting.getAccount());
        assertThat(sourcePosting.getAmount(), is(sourceAmount));
        assertThat(sourcePosting.getCurrency(), is(sourceCurrency));
        assertThat(targetPosting.getAmount(), is(targetAmount));
        assertThat(targetPosting.getCurrency(), is(targetCurrency));

        assertThat(fixture.source().getTransactions(), is(List.of(removal)));
        assertThat(fixture.target().getTransactions(), is(List.of(deposit)));
        assertThat(removal, instanceOf(LedgerBackedTransaction.class));
        assertThat(deposit, instanceOf(LedgerBackedTransaction.class));
        assertThat(removal.getUUID(), is(sourceProjectionUUID));
        assertThat(deposit.getUUID(), is(targetProjectionUUID));
        assertThat(removal.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(deposit.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(removal.getCrossEntry(), is(nullValue()));
        assertThat(deposit.getCrossEntry(), is(nullValue()));
        assertThat(removal.getDateTime(), is(DATE_TIME));
        assertThat(deposit.getDateTime(), is(DATE_TIME));
        assertThat(removal.getNote(), is("note"));
        assertThat(deposit.getNote(), is("note"));
        assertThat(removal.getSource(), is("source"));
        assertThat(deposit.getSource(), is("source"));
        assertThat(removal.getAmount(), is(sourceAmount));
        assertThat(deposit.getAmount(), is(targetAmount));
        assertThat(removal.getCurrencyCode(), is(sourceCurrency));
        assertThat(deposit.getCurrencyCode(), is(targetCurrency));
        assertThat(fixture.client().getAllTransactions().size(), is(2));
        assertValid(fixture.client());
    }

    private void assertRoundtrip(Client client, String transferEntryUUID, String sourcePostingUUID,
                    String targetPostingUUID, String sourceProjectionUUID, String targetProjectionUUID,
                    String sourceCurrency, String targetCurrency, BigDecimal expectedSourceExchangeRate)
    {
        var source = client.getAccounts().get(0);
        var target = client.getAccounts().get(1);
        var removalEntry = removalEntry(client);
        var depositEntry = depositEntry(client);
        var sourcePosting = posting(removalEntry, source);
        var targetPosting = posting(depositEntry, target);

        assertThat(client.getLedger().getEntries().size(), is(2));
        assertThat(source.getTransactions().size(), is(1));
        assertThat(target.getTransactions().size(), is(1));
        assertThat(removalEntry.getUUID(), is(transferEntryUUID));
        assertThat(sourcePosting.getUUID(), is(sourcePostingUUID));
        assertThat(targetPosting.getUUID(), is(targetPostingUUID));
        assertThat(projection(removalEntry, LedgerProjectionRole.ACCOUNT).getUUID(), is(sourceProjectionUUID));
        assertThat(projection(depositEntry, LedgerProjectionRole.ACCOUNT).getUUID(), is(targetProjectionUUID));
        assertThat(source.getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(target.getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(sourcePosting.getCurrency(), is(sourceCurrency));
        assertThat(targetPosting.getCurrency(), is(targetCurrency));
        assertThat(sourcePosting.getExchangeRate(), is(expectedSourceExchangeRate));
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

    private AccountTransferEntry createTransfer(Fixture fixture, long sourceAmount, String sourceCurrency,
                    long targetAmount, String targetCurrency, Money sourceForexAmount, BigDecimal sourceExchangeRate)
    {
        return new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        DATE_TIME, sourceAmount, sourceCurrency, targetAmount, targetCurrency, sourceForexAmount,
                        sourceExchangeRate, "note", "source");
    }

    private LedgerAccountTransferToDepositRemovalConverter converter(Fixture fixture)
    {
        return new LedgerAccountTransferToDepositRemovalConverter(fixture.client());
    }

    private LedgerEntry removalEntry(Client client)
    {
        return client.getLedger().getEntries().stream().filter(entry -> entry.getType() == LedgerEntryType.REMOVAL)
                        .findFirst().orElseThrow();
    }

    private LedgerEntry depositEntry(Client client)
    {
        return client.getLedger().getEntries().stream().filter(entry -> entry.getType() == LedgerEntryType.DEPOSIT)
                        .findFirst().orElseThrow();
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

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-account-transfer-split", ".xml");
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

    @FunctionalInterface
    private interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    private record Fixture(Client client, Account source, Account target)
    {
    }

    private record Snapshot(List<EntrySnapshot> entries, List<String> accountTransactions,
                    List<String> allTransactions)
    {
        static Snapshot capture(Client client)
        {
            return new Snapshot(client.getLedger().getEntries().stream().map(EntrySnapshot::capture)
                            .sorted(Comparator.comparing(EntrySnapshot::uuid)).toList(),
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

    private record PostingSnapshot(String uuid, LedgerPostingType type, long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, Security security, long shares, Account account)
    {
        static PostingSnapshot capture(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount());
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
