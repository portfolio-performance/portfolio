package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerOwnerPatchHelper;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionDeleter;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests atomic ledger mutation and projection refresh behavior.
 * These tests make sure failed or repeated updates do not leave partial ledger changes or duplicate runtime rows.
 */
@SuppressWarnings("nls")
public class LedgerMutationContextTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);

    /**
     * Checks the ledger mutation scenario: owner patch removes stale projection and materializes current projection.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testOwnerPatchRemovesStaleProjectionAndMaterializesCurrentProjection()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(source, 100)).getEntry();
        var entryUUID = entry.getUUID();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedAccountTransaction) source.getTransactions().get(0);

        new LedgerOwnerPatchHelper(client).moveAccountOnly(transaction, target);

        assertTrue(source.getTransactions().isEmpty());
        assertThat(target.getTransactions().size(), is(1));
        assertThat(target.getTransactions().get(0), instanceOf(LedgerBackedAccountTransaction.class));
        assertThat(target.getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(((LedgerBackedTransaction) target.getTransactions().get(0)).getLedgerEntry().getUUID(), is(entryUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertSame(target, entry.getPostings().get(0).getAccount());
        assertSame(target, entry.getProjectionRefs().get(0).getAccount());
    }

    /**
     * Checks the ledger mutation scenario: repeated refresh does not duplicate ledger backed projections.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testRepeatedRefreshDoesNotDuplicateLedgerBackedProjections()
    {
        var client = new Client();
        var account = register(client, account());

        creator(client).createDeposit(metadata(), cashLeg(account, 100));

        var context = new LedgerMutationContext(client);

        context.refresh();
        context.refresh();

        assertThat(account.getTransactions().size(), is(1));
    }

    /**
     * Checks the ledger mutation scenario: attach entry adds validated copy without projection refresh.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testAttachEntryAddsValidatedCopyWithoutProjectionRefresh()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = targetedAccountEntry(account);
        var context = new LedgerMutationContext(client);
        var entryUUID = entry.getUUID();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();

        var liveEntry = context.attachEntry(entry);

        assertSame(liveEntry, client.getLedger().getEntries().get(0));
        assertNotSame(entry, liveEntry);
        assertThat(liveEntry.getUUID(), is(entryUUID));
        assertThat(liveEntry.getPostings().get(0).getUUID(), is(postingUUID));
        assertThat(liveEntry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(liveEntry.getProjectionRefs().get(0).getPrimaryPostingUUID(), is(postingUUID));
        assertTrue(account.getTransactions().isEmpty());

        context.refresh();

        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionUUID));
    }

    /**
     * Checks the ledger mutation scenario: attach entry validates before live mutation.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testAttachEntryValidatesBeforeLiveMutation()
    {
        var client = new Client();
        var entry = new LedgerEntry();

        entry.setType(LedgerEntryType.DEPOSIT);
        entry.setDateTime(DATE_TIME);

        assertThrows(IllegalArgumentException.class, () -> new LedgerMutationContext(client).attachEntry(entry));

        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    /**
     * Checks the ledger mutation scenario: remove entry fails without partial mutation for unknown entry.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testRemoveEntryFailsWithoutPartialMutationForUnknownEntry()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var unknown = creator(new Client()).createDeposit(metadata(), cashLeg(account, 200)).getEntry();

        LedgerProjectionService.materialize(client);

        assertThrows(IllegalArgumentException.class, () -> new LedgerMutationContext(client).removeEntry(unknown));

        assertSame(entry, client.getLedger().getEntries().get(0));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionUUID));
    }

    /**
     * Checks the ledger mutation scenario: failed context mutation does not refresh owner lists or mutate live ledger.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testFailedContextMutationDoesNotRefreshOwnerListsOrMutateLiveLedger()
    {
        var client = new Client();
        var account = register(client, account());
        var target = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var originalProjectionAccount = entry.getProjectionRefs().get(0).getAccount();
        var originalPostingAccount = entry.getPostings().get(0).getAccount();

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerMutationContext(client).mutateEntry(entry,
                                        editedEntry -> editedEntry.getProjectionRefs().get(0).setAccount(null)));

        assertTrue(exception.getMessage(), exception.getMessage().contains("[PROJECTION_REF_ACCOUNT_REQUIRED] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("\n  Projection:\n"));

        assertSame(originalProjectionAccount, entry.getProjectionRefs().get(0).getAccount());
        assertSame(originalPostingAccount, entry.getPostings().get(0).getAccount());
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getProjectionRefs().size(), is(1));
        assertTrue(target.getTransactions().isEmpty());
    }

    /**
     * Checks the ledger mutation scenario: failed context mutation rolls back entry parameters.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testFailedContextMutationRollsBackEntryParameters()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();

        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE,
                        "original"));

        assertThrows(IllegalArgumentException.class, () -> new LedgerMutationContext(client).mutateEntry(entry,
                        editedEntry -> {
                            editedEntry.addParameter(LedgerParameter.ofString(
                                            LedgerParameterType.CORPORATE_ACTION_KIND,
                                            CorporateActionKind.SPIN_OFF.getCode()));
                            editedEntry.getProjectionRefs().get(0).setAccount(null);
                        }));

        assertThat(entry.getParameters().size(), is(1));
        assertThat(entry.getParameters().get(0).getType(), is(LedgerParameterType.EVENT_REFERENCE));
        assertThat(entry.getParameters().get(0).getValue(), is("original"));
    }

    /**
     * Checks the ledger mutation scenario: context mutation synchronizes entry parameters.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testContextMutationSynchronizesEntryParameters()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();

        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE,
                        "original"));

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> {
            editedEntry.removeParameter(editedEntry.getParameters().get(0));
            editedEntry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                            CorporateActionKind.SPIN_OFF.getCode()));
        });

        assertThat(entry.getParameters().size(), is(1));
        assertThat(entry.getParameters().get(0).getType(), is(LedgerParameterType.CORPORATE_ACTION_KIND));
        assertThat(entry.getParameters().get(0).getValue(), is(CorporateActionKind.SPIN_OFF.getCode()));
    }

    /**
     * Checks the ledger mutation scenario: mutation lambda is applied only to candidate.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testMutationLambdaIsAppliedOnlyToCandidate()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var applications = new AtomicInteger();

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> {
            applications.incrementAndGet();
            editedEntry.setNote("changed");
        });

        assertThat(applications.get(), is(1));
        assertThat(entry.getNote(), is("changed"));
    }

    /**
     * Checks the ledger mutation scenario: mutation that would fail on second application still synchronizes from candidate.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testMutationThatWouldFailOnSecondApplicationStillSynchronizesFromCandidate()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(source, 100)).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var applications = new AtomicInteger();

        LedgerProjectionService.materialize(client);

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> {
            if (applications.incrementAndGet() > 1)
                throw new AssertionError("Mutation lambda must not be applied to the live ledger");

            editedEntry.getProjectionRefs().get(0).setAccount(target);
            editedEntry.getPostings().get(0).setAccount(target);
        });

        assertThat(applications.get(), is(1));
        assertTrue(source.getTransactions().isEmpty());
        assertThat(target.getTransactions().size(), is(1));
        assertThat(target.getTransactions().get(0).getUUID(), is(projectionUUID));
        assertSame(target, entry.getProjectionRefs().get(0).getAccount());
        assertSame(target, entry.getPostings().get(0).getAccount());
    }

    /**
     * Checks the ledger mutation scenario: projection membership and role changes refresh materialized projections.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testProjectionMembershipAndRoleChangesRefreshMaterializedProjections()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = targetedAccountEntry(account);
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var context = new LedgerMutationContext(client);

        client.getLedger().addEntry(entry);
        context.refresh();
        context.mutateEntry(entry,
                        editedEntry -> editedEntry.getProjectionRefs().get(0).setRole(LedgerProjectionRole.CASH_COMPENSATION));

        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getProjectionRefs().get(0).getRole(), is(LedgerProjectionRole.CASH_COMPENSATION));

        context.mutateEntry(entry, editedEntry -> editedEntry.removeProjectionRef(editedEntry.getProjectionRefs().get(0)));

        assertTrue(entry.getProjectionRefs().isEmpty());
        assertTrue(account.getTransactions().isEmpty());
    }

    /**
     * Checks the ledger mutation scenario: mutation context preserves entry local projection targeting during copy sync.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testMutationContextPreservesEntryLocalProjectionTargetingDuringCopySync()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = targetedAccountEntry(account);
        var projection = entry.getProjectionRefs().get(0);
        var posting = entry.getPostings().get(0);
        var projectionUUID = projection.getUUID();
        var postingUUID = posting.getUUID();
        var context = new LedgerMutationContext(client);

        client.getLedger().addEntry(entry);
        context.refresh();

        context.mutateEntry(entry,
                        editedEntry -> editedEntry.getPostings().get(0).setAmount(Values.Amount.factorize(200)));

        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getProjectionRefs().get(0).getPrimaryPostingUUID(), is(postingUUID));
        assertSame(entry.getPostings().get(0),
                        LedgerProjectionSupport.primaryPosting(entry, entry.getProjectionRefs().get(0)));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionUUID));
    }

    private LedgerEntry targetedAccountEntry(Account account)
    {
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();
        var projection = new LedgerProjectionRef();

        entry.setType(LedgerEntryType.SPIN_OFF);
        entry.setDateTime(DATE_TIME);
        posting.setType(LedgerPostingType.CASH_COMPENSATION);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(account);
        projection.setPrimaryPosting(posting);
        entry.addPosting(posting);
        entry.addProjectionRef(projection);

        return entry;
    }

    /**
     * Checks the ledger mutation scenario: unrelated legacy and ledger backed transactions remain untouched.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testUnrelatedLegacyAndLedgerBackedTransactionsRemainUntouched()
    {
        var client = new Client();
        var movedSource = register(client, account());
        var movedTarget = register(client, account());
        var unrelated = register(client, account());
        var legacy = legacyDeposit();

        unrelated.getTransactions().add(legacy);
        creator(client).createDeposit(metadata(), cashLeg(movedSource, 100));
        var unrelatedEntry = creator(client).createDeposit(metadata(), cashLeg(unrelated, 200)).getEntry();
        var unrelatedProjectionUUID = unrelatedEntry.getProjectionRefs().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var movedTransaction = (LedgerBackedAccountTransaction) movedSource.getTransactions().get(0);
        var unrelatedLedgerTransaction = unrelated.getTransactions().stream()
                        .filter(transaction -> transaction instanceof LedgerBackedTransaction).findFirst().orElseThrow();

        new LedgerOwnerPatchHelper(client).moveAccountOnly(movedTransaction, movedTarget);

        assertSame(legacy, unrelated.getTransactions().get(0));
        assertTrue(unrelated.getTransactions().contains(unrelatedLedgerTransaction));
        assertThat(unrelatedLedgerTransaction.getUUID(), is(unrelatedProjectionUUID));
    }

    /**
     * Checks the ledger mutation scenario: deleting buy/sell projection deletes whole entry and both materialized projections.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testDeletingBuySellProjectionDeletesWholeEntryAndBothMaterializedProjections()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none()).getEntry();

        LedgerProjectionService.materialize(client);

        new LedgerTransactionDeleter(client).delete((LedgerBackedTransaction) account.getTransactions().get(0));

        assertTrue(client.getLedger().getEntries().stream().noneMatch(item -> item.getUUID().equals(entry.getUUID())));
        assertTrue(account.getTransactions().isEmpty());
        assertTrue(portfolio.getTransactions().isEmpty());
    }

    /**
     * Checks the ledger mutation scenario: deleting transfer and delivery entries removes their materialized projections.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testDeletingTransferAndDeliveryEntriesRemovesTheirMaterializedProjections()
    {
        assertDeleteRemovesMaterializedAccountTransfer();
        assertDeleteRemovesMaterializedPortfolioTransfer();
        assertDeleteRemovesMaterializedDelivery();
    }

    /**
     * Checks the ledger mutation scenario: delete does not delete legacy transactions.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testDeleteDoesNotDeleteLegacyTransactions()
    {
        var client = new Client();
        var account = register(client, account());
        var legacy = legacyDeposit();

        account.getTransactions().add(legacy);
        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        LedgerProjectionService.materialize(client);

        var ledgerTransaction = account.getTransactions().stream() //
                        .filter(transaction -> transaction instanceof LedgerBackedTransaction) //
                        .map(LedgerBackedTransaction.class::cast) //
                        .findFirst().orElseThrow();

        new LedgerTransactionDeleter(client).delete(ledgerTransaction);

        assertThat(account.getTransactions().size(), is(1));
        assertSame(legacy, account.getTransactions().get(0));
    }

    /**
     * Checks the ledger mutation scenario: delete leaves unrelated ledger entry projection and legacy transaction untouched.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testDeleteLeavesUnrelatedLedgerEntryProjectionAndLegacyTransactionUntouched()
    {
        var client = new Client();
        var account = register(client, account());
        var legacy = legacyDeposit();

        account.getTransactions().add(legacy);

        var deletedEntry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var survivingEntry = creator(client).createDeposit(metadata(), cashLeg(account, 200)).getEntry();
        var survivingEntryUUID = survivingEntry.getUUID();
        var survivingProjectionUUID = survivingEntry.getProjectionRefs().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var deletedTransaction = account.getTransactions().stream() //
                        .filter(transaction -> transaction instanceof LedgerBackedTransaction
                                        && transaction.getUUID().equals(deletedEntry.getProjectionRefs().get(0).getUUID()))
                        .map(LedgerBackedTransaction.class::cast) //
                        .findFirst().orElseThrow();

        new LedgerTransactionDeleter(client).delete(deletedTransaction);

        assertTrue(client.getLedger().getEntries().stream()
                        .noneMatch(entry -> entry.getUUID().equals(deletedEntry.getUUID())));
        assertTrue(client.getLedger().getEntries().stream()
                        .anyMatch(entry -> entry.getUUID().equals(survivingEntryUUID)));
        assertSame(legacy, account.getTransactions().get(0));
        assertTrue(account.getTransactions().stream().anyMatch(transaction -> transaction instanceof LedgerBackedTransaction
                        && transaction.getUUID().equals(survivingProjectionUUID)));
    }

    /**
     * Checks the ledger mutation scenario: delivery owner patch updates posting and projection portfolio.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testDeliveryOwnerPatchUpdatesPostingAndProjectionPortfolio()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var entry = creator(client).createInboundDelivery(metadata(), LedgerDeliveryLeg.of(source,
                        LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)), money(100))).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        new LedgerOwnerPatchHelper(client).moveDelivery((LedgerBackedPortfolioTransaction) source.getTransactions().get(0),
                        target);

        assertTrue(source.getTransactions().isEmpty());
        assertThat(target.getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertSame(target, entry.getPostings().get(0).getPortfolio());
        assertSame(target, entry.getProjectionRefs().get(0).getPortfolio());
    }

    /**
     * Checks the ledger mutation scenario: buy/sell owner patch preserves cross entry read compatibility.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testBuySellOwnerPatchPreservesCrossEntryReadCompatibility()
    {
        var client = new Client();
        var account = register(client, account());
        var targetAccount = register(client, account());
        var portfolio = register(client, portfolio());
        var targetPortfolio = register(client, portfolio());
        var entry = creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none()).getEntry();
        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = projection(entry, LedgerProjectionRole.PORTFOLIO);
        var accountProjectionUUID = accountProjection.getUUID();
        var portfolioProjectionUUID = portfolioProjection.getUUID();
        var cashPostingUUID = LedgerProjectionSupport.primaryPosting(entry, accountProjection).getUUID();
        var securityPostingUUID = LedgerProjectionSupport.primaryPosting(entry, portfolioProjection).getUUID();

        LedgerProjectionService.materialize(client);

        var helper = new LedgerOwnerPatchHelper(client);

        helper.moveBuySellAccountSide(entry, targetAccount);

        assertSame(targetAccount, projection(entry, LedgerProjectionRole.ACCOUNT).getAccount());
        assertSame(portfolio, projection(entry, LedgerProjectionRole.PORTFOLIO).getPortfolio());
        assertSame(targetAccount, posting(entry, cashPostingUUID).getAccount());
        assertSame(portfolio, posting(entry, securityPostingUUID).getPortfolio());

        helper.moveBuySellPortfolioSide(entry, targetPortfolio);

        var accountTransaction = targetAccount.getTransactions().get(0);
        var portfolioTransaction = targetPortfolio.getTransactions().get(0);

        assertThat(accountTransaction.getUUID(), is(accountProjectionUUID));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjectionUUID));
        assertSame(targetAccount, projection(entry, LedgerProjectionRole.ACCOUNT).getAccount());
        assertSame(targetPortfolio, projection(entry, LedgerProjectionRole.PORTFOLIO).getPortfolio());
        assertSame(targetAccount, posting(entry, cashPostingUUID).getAccount());
        assertSame(targetPortfolio, posting(entry, securityPostingUUID).getPortfolio());
        assertSame(targetPortfolio, accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
    }

    /**
     * Checks the ledger mutation scenario: transfer owner patches do not swap direction.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testTransferOwnerPatchesDoNotSwapDirection()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var newSource = register(client, account());
        var newTarget = register(client, account());
        var entry = creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100))).getEntry();
        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePostingUUID = LedgerProjectionSupport.primaryPosting(entry, sourceProjection).getUUID();
        var targetPostingUUID = LedgerProjectionSupport.primaryPosting(entry, targetProjection).getUUID();

        LedgerProjectionService.materialize(client);

        var helper = new LedgerOwnerPatchHelper(client);

        helper.moveAccountTransferSource(entry, newSource);
        helper.moveAccountTransferTarget(entry, newTarget);

        assertThat(newSource.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(newTarget.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(newSource, projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getAccount());
        assertSame(newTarget, projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getAccount());
        assertSame(newSource, posting(entry, sourcePostingUUID).getAccount());
        assertSame(newTarget, posting(entry, targetPostingUUID).getAccount());
    }

    /**
     * Checks the ledger mutation scenario: portfolio transfer owner patches do not swap direction.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testPortfolioTransferOwnerPatchesDoNotSwapDirection()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var newSource = register(client, portfolio());
        var newTarget = register(client, portfolio());
        var entry = creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();
        var sourceProjection = projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);
        var sourcePostingUUID = LedgerProjectionSupport.primaryPosting(entry, sourceProjection).getUUID();
        var targetPostingUUID = LedgerProjectionSupport.primaryPosting(entry, targetProjection).getUUID();

        LedgerProjectionService.materialize(client);

        var helper = new LedgerOwnerPatchHelper(client);

        helper.movePortfolioTransferSource(entry, newSource);
        helper.movePortfolioTransferTarget(entry, newTarget);

        assertThat(newSource.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(newTarget.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(newSource, projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getPortfolio());
        assertSame(newTarget, projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getPortfolio());
        assertSame(newSource, posting(entry, sourcePostingUUID).getPortfolio());
        assertSame(newTarget, posting(entry, targetPostingUUID).getPortfolio());
    }

    /**
     * Checks the ledger mutation scenario: unsupported owner patch fails without partial mutation.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testUnsupportedOwnerPatchFailsWithoutPartialMutation()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var entry = creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none()).getEntry();
        var duplicate = new LedgerProjectionRef();

        duplicate.setRole(LedgerProjectionRole.ACCOUNT);
        duplicate.setAccount(account);
        entry.addProjectionRef(duplicate);

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerOwnerPatchHelper(client).moveBuySellAccountSide(entry, register(client, account())));
        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_PROJ_042
                        .message("Expected one projection for role ACCOUNT but found 2")));

        assertSame(account, projection(entry, LedgerProjectionRole.ACCOUNT).getAccount());
        assertSame(account, entry.getPostings().get(0).getAccount());
    }

    /**
     * Checks the ledger mutation scenario: same shape replacement preserves entry and projection uuids.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testSameShapeReplacementPreservesEntryAndProjectionUUIDs()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var entryUUID = entry.getUUID();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var originalPostingUUID = entry.getPostings().get(0).getUUID();
        var replacement = creator(new Client()).createDeposit(metadata(), cashLeg(account, 200)).getEntry();
        var replacementPostingUUID = replacement.getPostings().get(0).getUUID();

        assertTrue(!originalPostingUUID.equals(replacementPostingUUID));

        LedgerProjectionService.materialize(client);

        new LedgerMutationContext(client).replaceSameShapeEntry(entry, replacement);

        var replacedEntry = client.getLedger().getEntries().get(0);

        assertThat(replacedEntry.getUUID(), is(entryUUID));
        assertThat(replacedEntry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(replacedEntry.getPostings().get(0).getUUID(), is(replacementPostingUUID));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getAmount(), is(Values.Amount.factorize(200)));
    }

    /**
     * Checks the ledger mutation scenario: entry replacement fails when projection identity is ambiguous.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testEntryReplacementFailsWhenProjectionIdentityIsAmbiguous()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var replacement = creator(new Client()).createDeposit(metadata(), cashLeg(account, 200)).getEntry();
        var duplicate = new LedgerProjectionRef();

        duplicate.setRole(LedgerProjectionRole.ACCOUNT);
        duplicate.setAccount(account);
        replacement.addProjectionRef(duplicate);

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerMutationContext(client).replaceSameShapeEntry(entry, replacement));

        assertTrue(exception.getMessage(), exception.getMessage().contains(LedgerDiagnosticCode.LEDGER_PROJ_003.prefix()));
        assertSame(entry, client.getLedger().getEntries().get(0));
        assertThat(entry.getPostings().get(0).getAmount(), is(Values.Amount.factorize(100)));
    }

    /**
     * Checks the ledger mutation scenario: entry split requires replacement entries.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testEntrySplitRequiresReplacementEntries()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerMutationContext(client).splitEntry(entry, List.of()));

        assertTrue(exception.getMessage(), exception.getMessage().contains(LedgerDiagnosticCode.LEDGER_CORE_007.prefix()));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(entry, client.getLedger().getEntries().get(0));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionUUID));
    }

    /**
     * Checks the ledger mutation scenario: direct owner list removal can be restored by explicit refresh.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testDirectOwnerListRemovalCanBeRestoredByExplicitRefresh()
    {
        var client = new Client();
        var account = register(client, account());

        creator(client).createDeposit(metadata(), cashLeg(account, 100));

        var context = new LedgerMutationContext(client);

        context.refresh();
        account.getTransactions().remove(0);

        assertTrue(account.getTransactions().isEmpty());

        context.refresh();

        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedAccountTransaction.class));
    }

    /**
     * Checks the ledger mutation scenario: cross entry write methods still reject mutation except replay noop.
     * Failed or repeated operations must not leave partial changes or duplicate projections.
     * This protects atomic ledger mutation behavior.
     */
    @Test
    public void testCrossEntryWriteMethodsStillRejectMutationExceptReplayNoop()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());

        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);

        assertThrows(UnsupportedOperationException.class, () -> transaction.getCrossEntry().insert());
        transaction.getCrossEntry().updateFrom(transaction);
        assertThat(account.getTransactions(), is(List.of(transaction)));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertThrows(UnsupportedOperationException.class, () -> transaction.getCrossEntry().setOwner(transaction, account));
        assertThrows(UnsupportedOperationException.class, () -> transaction.getCrossEntry().setSource("new source"));
    }

    private void assertDeleteRemovesMaterializedAccountTransfer()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());
        var entry = creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100))).getEntry();

        LedgerProjectionService.materialize(client);

        new LedgerTransactionDeleter(client).delete((LedgerBackedTransaction) source.getTransactions().get(0));

        assertTrue(client.getLedger().getEntries().stream().noneMatch(item -> item.getUUID().equals(entry.getUUID())));
        assertTrue(source.getTransactions().isEmpty());
        assertTrue(target.getTransactions().isEmpty());
    }

    private void assertDeleteRemovesMaterializedPortfolioTransfer()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());
        var entry = creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();

        LedgerProjectionService.materialize(client);

        new LedgerTransactionDeleter(client).delete((LedgerBackedTransaction) source.getTransactions().get(0));

        assertTrue(client.getLedger().getEntries().stream().noneMatch(item -> item.getUUID().equals(entry.getUUID())));
        assertTrue(source.getTransactions().isEmpty());
        assertTrue(target.getTransactions().isEmpty());
    }

    private void assertDeleteRemovesMaterializedDelivery()
    {
        var client = new Client();
        var portfolio = register(client, portfolio());
        var entry = creator(client).createInboundDelivery(metadata(), LedgerDeliveryLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)), money(100))).getEntry();

        LedgerProjectionService.materialize(client);

        new LedgerTransactionDeleter(client).delete((LedgerBackedTransaction) portfolio.getTransactions().get(0));

        assertTrue(client.getLedger().getEntries().stream().noneMatch(item -> item.getUUID().equals(entry.getUUID())));
        assertTrue(portfolio.getTransactions().isEmpty());
    }

    private LedgerTransactionCreator creator(Client client)
    {
        return new LedgerTransactionCreator(client);
    }

    private LedgerTransactionMetadata metadata()
    {
        return LedgerTransactionMetadata.of(DATE_TIME).withNote("note").withSource("source");
    }

    private Account register(Client client, Account account)
    {
        client.addAccount(account);
        return account;
    }

    private Portfolio register(Client client, Portfolio portfolio)
    {
        client.addPortfolio(portfolio);
        return portfolio;
    }

    private Account account()
    {
        var account = new Account();

        account.setCurrencyCode(CurrencyUnit.EUR);

        return account;
    }

    private Portfolio portfolio()
    {
        return new Portfolio();
    }

    private name.abuchen.portfolio.model.Security security()
    {
        return new name.abuchen.portfolio.model.Security("Security", CurrencyUnit.EUR);
    }

    private LedgerAccountCashLeg cashLeg(Account account, int amount)
    {
        return LedgerAccountCashLeg.of(account, money(amount));
    }

    private LedgerPortfolioSecurityLeg portfolioLeg(Portfolio portfolio, int amount)
    {
        return LedgerPortfolioSecurityLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)), money(amount));
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private AccountTransaction legacyDeposit()
    {
        var transaction = new AccountTransaction();

        transaction.setType(AccountTransaction.Type.DEPOSIT);
        transaction.setDateTime(DATE_TIME);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(1));

        return transaction;
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream().filter(posting -> posting.getUUID().equals(uuid)).findFirst()
                        .orElseThrow();
    }
}
