package name.abuchen.portfolio.model.ledger.projection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests rebuilding runtime transaction rows from ledger entries.
 * These tests make sure account and portfolio views are derived from ledger truth without duplicate rows.
 */
@SuppressWarnings("nls")
public class LedgerRuntimeProjectionRestorerTest
{
    private record LogEvent(LedgerRuntimeProjectionRestorer.Severity severity, String message)
    {
    }

    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 17, 0, 0);

    /**
     * Checks the projection rebuild scenario: missing owner list projection is restored with same runtime projection id.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testMissingOwnerListProjectionIsRestoredWithSameRuntimeProjectionId()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionId = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0)
                        .getRuntimeProjectionId();
        assertTrue(account.getTransactions().isEmpty());

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(account.getTransactions().get(0).getUUID(), is(projectionId));
    }

    /**
     * Checks the projection rebuild scenario: semantic primary targeting drives descriptor materialization.
     * Account and portfolio lists must be derived from descriptor targeting.
     * This protects Ledger-V6 from stale posting-order targeting.
     */
    @Test
    public void testSemanticPrimaryMaterializesAccountProjection()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var alternatePosting = cashPosting("membership-primary-posting", account, 200);

        alternatePosting.setType(LedgerPostingType.FEE);
        entry.addPosting(alternatePosting);

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getAmount(), is(Values.Amount.factorize(100)));
    }

    /**
     * Checks the projection rebuild scenario: semantic group keys drive native targeted units.
     * Unit projections must be derived from posting semantics.
     * This protects Ledger-V6 from relying on persisted projection membership targeting.
     */
    @Test
    public void testSemanticGroupKeyMaterializesNativeTargetedUnits()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var sourceSecurity = security();
        var targetSecurity = security();
        var entry = new LedgerEntry("entry-1");
        var sourceLeg = securityPosting("old-security", portfolio, sourceSecurity, LedgerProjectionRole.OLD_SECURITY_LEG,
                        CorporateActionLeg.SOURCE_SECURITY);
        var targetLeg = securityPosting("new-security", portfolio, targetSecurity, LedgerProjectionRole.NEW_SECURITY_LEG,
                        CorporateActionLeg.TARGET_SECURITY);
        var compensation = cashPosting("cash-compensation", account, 5);
        var fee = unitPosting("fee-posting", LedgerPostingType.FEE, account, 2);
        var tax = unitPosting("tax-posting", LedgerPostingType.TAX, account, 1);

        entry.setType(LedgerEntryType.SPIN_OFF);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(sourceLeg);
        entry.addPosting(targetLeg);
        compensation.setType(LedgerPostingType.CASH_COMPENSATION);
        compensation.setSemanticRole(LedgerPostingSemanticRole.CASH_COMPENSATION);
        compensation.setDirection(LedgerPostingDirection.NEUTRAL);
        compensation.setCorporateActionLeg(CorporateActionLeg.CASH_COMPENSATION);
        compensation.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        compensation.setGroupKey(LedgerProjectionRole.CASH_COMPENSATION.name());
        compensation.setLocalKey(LedgerProjectionRole.CASH_COMPENSATION.name());
        fee.setSemanticRole(LedgerPostingSemanticRole.FEE);
        fee.setUnitRole(LedgerPostingUnitRole.FEE);
        fee.setGroupKey(LedgerProjectionRole.CASH_COMPENSATION.name());
        tax.setSemanticRole(LedgerPostingSemanticRole.TAX);
        tax.setUnitRole(LedgerPostingUnitRole.TAX);
        tax.setGroupKey(LedgerProjectionRole.CASH_COMPENSATION.name());
        entry.addPosting(compensation);
        entry.addPosting(fee);
        entry.addPosting(tax);
        client.getLedger().addEntry(entry);

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUnits().map(Transaction.Unit::getType).toList(),
                        is(List.of(Transaction.Unit.Type.FEE, Transaction.Unit.Type.TAX)));
    }

    /**
     * Checks the projection rebuild scenario: missing owner list projection does not log info.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testMissingOwnerListProjectionDoesNotLogInfo()
    {
        var client = new Client();
        var account = register(client, account());

        creator(client).createDeposit(metadata(), cashLeg(account, 100));

        var statuses = ledgerLogStatuses(client);

        assertTrue(statuses.isEmpty());
    }

    /**
     * Checks the projection rebuild scenario: duplicate ledger backed projection is removed and rematerialized once.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testDuplicateLedgerBackedProjectionIsRemovedAndRematerializedOnce()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var descriptor = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0);

        new LedgerProjectionMaterializer().materialize(client);
        account.getTransactions().add((AccountTransaction) new LedgerProjectionFactory().createProjection(entry,
                        descriptor.getRole()));

        assertThat(account.getTransactions().size(), is(2));

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getUUID(), is(descriptor.getRuntimeProjectionId()));
    }

    /**
     * Checks the projection rebuild scenario: duplicate ledger backed projection logs info.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testDuplicateLedgerBackedProjectionLogsInfo()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var descriptor = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0);

        new LedgerProjectionMaterializer().materialize(client);
        account.getTransactions().add((AccountTransaction) new LedgerProjectionFactory().createProjection(entry,
                        descriptor.getRole()));

        var statuses = ledgerLogStatuses(client);

        assertThat(statuses.size(), is(1));
        assertThat(statuses.get(0).severity(), is(LedgerRuntimeProjectionRestorer.Severity.INFO));
        assertThat(statuses.get(0).message(), is(LedgerRuntimeProjectionRestorer.restoredLedgerMessage(2, 1, 1,
                        Set.of(), Set.of(descriptor.getRuntimeProjectionId()), Set.of())));
    }

    /**
     * Checks the projection rebuild scenario: stale ledger backed projection without a backing descriptor is removed.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testStaleLedgerBackedProjectionWithoutBackingDescriptorIsRemoved()
    {
        var client = new Client();
        var account = register(client, account());

        account.setName("Restored Account");
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();

        new LedgerProjectionMaterializer().materialize(client);
        client.getLedger().removeEntry(entry);

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertTrue(account.getTransactions().isEmpty());
    }

    /**
     * Checks the projection rebuild scenario: stale ledger backed projection logs info.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testStaleLedgerBackedProjectionLogsInfo()
    {
        var client = new Client();
        var account = register(client, account());
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionId = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0)
                        .getRuntimeProjectionId();

        new LedgerProjectionMaterializer().materialize(client);
        client.getLedger().removeEntry(entry);

        var statuses = ledgerLogStatuses(client);

        assertThat(statuses.size(), is(1));
        assertThat(statuses.get(0).severity(), is(LedgerRuntimeProjectionRestorer.Severity.INFO));
        assertThat(statuses.get(0).message(), is(LedgerRuntimeProjectionRestorer.restoredLedgerMessage(1, 0, 1,
                        Set.of(), Set.of(), Set.of(projectionId))));
    }

    /**
     * Checks the projection rebuild scenario: valid no op restoration does not log.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testValidNoOpRestorationDoesNotLog()
    {
        var client = new Client();
        var account = register(client, account());

        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        new LedgerProjectionMaterializer().materialize(client);

        var statuses = ledgerLogStatuses(client);

        assertTrue(statuses.isEmpty());
    }

    /**
     * Checks the projection rebuild scenario: non ledger transactions remain untouched.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testNonLedgerTransactionsRemainUntouched()
    {
        var client = new Client();
        var account = register(client, account());
        var legacy = legacyDeposit();

        account.addTransaction(legacy);
        creator(client).createDeposit(metadata(), cashLeg(account, 100));

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertThat(account.getTransactions().size(), is(2));
        assertSame(legacy, account.getTransactions().get(0));
        assertThat(account.getTransactions().get(1), instanceOf(LedgerBackedTransaction.class));
    }

    /**
     * Checks the projection rebuild scenario: buy/sell cross entry is rematerialized.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testBuySellCrossEntryIsRematerialized()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());

        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());

        var accountTransaction = account.getTransactions().get(0);
        var portfolioTransaction = portfolio.getTransactions().get(0);

        assertThat(accountTransaction.getType(), is(AccountTransaction.Type.BUY));
        assertThat(portfolioTransaction.getType(), is(PortfolioTransaction.Type.BUY));
        assertSame(accountTransaction.getCrossEntry(), portfolioTransaction.getCrossEntry());
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertSame(portfolio, accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
    }

    /**
     * Checks the projection rebuild scenario: account transfer cross entry is rematerialized.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testAccountTransferCrossEntryIsRematerialized()
    {
        var client = new Client();
        var source = register(client, account());
        var target = register(client, account());

        creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100)));

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());

        var sourceTransaction = source.getTransactions().get(0);
        var targetTransaction = target.getTransactions().get(0);

        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
    }

    /**
     * Checks the projection rebuild scenario: portfolio transfer cross entry is rematerialized.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testPortfolioTransferCrossEntryIsRematerialized()
    {
        var client = new Client();
        var source = register(client, portfolio());
        var target = register(client, portfolio());

        creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100)));

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());

        var sourceTransaction = source.getTransactions().get(0);
        var targetTransaction = target.getTransactions().get(0);

        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
    }

    /**
     * Checks the projection rebuild scenario: investment plan ledger execution refs remain unchanged and resolvable.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testInvestmentPlanLedgerExecutionRefsRemainUnchangedAndResolvable()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var plan = new InvestmentPlan("plan");

        client.addPlan(plan);
        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        new LedgerProjectionMaterializer().materialize(client);

        var portfolioProjection = portfolio.getTransactions().get(0);
        var ref = InvestmentPlan.LedgerExecutionRef.of((LedgerBackedTransaction) portfolioProjection);

        plan.addLedgerExecutionRef(ref);
        account.getTransactions().clear();
        portfolio.getTransactions().clear();

        var result = new LedgerRuntimeProjectionRestorer().restoreIfValid(client);

        assertTrue(result.isOK());
        assertThat(plan.getLedgerExecutionRefs(), is(List.of(ref)));
        assertThat(plan.getTransactions(client).size(), is(1));
        assertThat(plan.getTransactions(client).get(0).getOwner(), is(portfolio));
        assertThat(plan.getTransactions(client).get(0).getTransaction().getUUID(),
                        is(portfolioProjection.getUUID()));
    }

    /**
     * Checks the projection rebuild scenario: invalid ledger is not repaired and is not materialized.
     * Account and portfolio lists must be derived from valid ledger entries only.
     * This protects Ledger-V6 from silently changing persisted ledger truth.
     */
    @Test
    public void testInvalidLedgerIsNotRepairedAndInvalidEntryIsNotMaterialized()
    {
        var client = new Client();
        var account = register(client, account());

        account.setName("Restored Account");
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();

        new LedgerProjectionMaterializer().materialize(client);

        entry.getPostings().get(0).setCurrency(null);
        var postingUUID = entry.getPostings().get(0).getUUID();
        var projectionId = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0)
                        .getRuntimeProjectionId();

        final LedgerStructuralValidator.ValidationResult[] result = new LedgerStructuralValidator.ValidationResult[1];
        var statuses = ledgerLogStatuses(client, value -> result[0] = value);

        assertFalse(result[0].isOK());
        assertTrue(account.getTransactions().isEmpty());
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertThat(entry.getPostings().get(0).getCurrency(), is((String) null));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0)
                        .getRuntimeProjectionId(), is(projectionId));
        assertTrue(result[0].hasIssue(LedgerStructuralValidator.IssueCode.POSTING_CURRENCY_REQUIRED));
        assertThat(statuses.size(), is(1));
        assertThat(statuses.get(0).severity(), is(LedgerRuntimeProjectionRestorer.Severity.WARNING));
        assertThat(statuses.get(0).message(), containsString("[POSTING_CURRENCY_REQUIRED] "));
        assertThat(statuses.get(0).message(), containsString("\n  Entry:\n"));
        assertThat(statuses.get(0).message(), containsString("\n  Posting:\n"));
        assertThat(statuses.get(0).message(), containsString("UUID: " + postingUUID));
        assertThat(statuses.get(0).message(), containsString("Type: CASH"));
        assertThat(statuses.get(0).message(),
                        containsString(Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":\n"));
        assertThat(statuses.get(0).message(),
                        containsString(Messages.LedgerDiagnosticMessageFormatterDate + ": 2026-06-17T00:00"));
        assertThat(statuses.get(0).message(),
                        containsString(Messages.LedgerDiagnosticMessageFormatterAccount + ": Restored Account"));
        assertThat(statuses.get(0).message(),
                        containsString(Messages.LedgerDiagnosticMessageFormatterSource + ": source"));
    }

    private List<LogEvent> ledgerLogStatuses(Client client)
    {
        var statuses = new ArrayList<LogEvent>();
        var restorer = new LedgerRuntimeProjectionRestorer(new LedgerProjectionMaterializer(),
                        (severity, message) -> statuses.add(new LogEvent(severity, message)));

        restorer.restoreIfValid(client);
        return statuses;
    }

    private List<LogEvent> ledgerLogStatuses(Client client,
                    Consumer<LedgerStructuralValidator.ValidationResult> resultConsumer)
    {
        var statuses = new ArrayList<LogEvent>();
        var restorer = new LedgerRuntimeProjectionRestorer(new LedgerProjectionMaterializer(),
                        (severity, message) -> statuses.add(new LogEvent(severity, message)));

        resultConsumer.accept(restorer.restoreIfValid(client));
        return statuses;
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

    private Security security()
    {
        return new Security("Security", CurrencyUnit.EUR);
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

    private LedgerPosting cashPosting(String uuid, Account account, int amount)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);

        return posting;
    }

    private LedgerPosting unitPosting(String uuid, LedgerPostingType type, Account account, int amount)
    {
        var posting = cashPosting(uuid, account, amount);

        posting.setType(type);

        return posting;
    }

    private LedgerPosting securityPosting(String uuid, Portfolio portfolio, Security security, LedgerProjectionRole role,
                    CorporateActionLeg leg)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(portfolio);
        posting.setSecurity(security);
        posting.setShares(Values.Share.factorize(5));
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(role == LedgerProjectionRole.OLD_SECURITY_LEG
                        ? LedgerPostingDirection.OUTBOUND
                        : LedgerPostingDirection.INBOUND);
        posting.setCorporateActionLeg(leg);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(role.name());
        posting.setLocalKey(role.name());

        return posting;
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
}
