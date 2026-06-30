package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividend;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerForexAmount;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerOptionalSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.legacy.LegacyTransactionToLedgerMigrator;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCashCompensation;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCorporateActionEvent;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeEntryMetadata;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeSecurityLeg;
import name.abuchen.portfolio.model.ledger.nativeentry.Ratio;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests persistence compatibility for ledger-backed transactions.
 * These tests make sure save/load rebuilds runtime rows from ledger truth and keeps compatibility data stable.
 */
@SuppressWarnings("nls")
public class LedgerXmlPersistenceTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 2, 3, 0, 0);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2026, 1, 30, 0, 0);

    /**
     * Verifies that old XML account transactions are migrated into ledger truth during load.
     * The loaded client must expose the same account booking through a ledger-backed projection.
     */
    @Test
    public void testOldXmlAccountOnlyTransactionLoadsAndMigratesIntoLedger() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var transaction = accountTransaction(AccountTransaction.Type.DEPOSIT, 100);

        account.addTransaction(transaction);

        var loaded = load(oldXmlWithoutLedger(client));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.DEPOSIT));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0).getUUID(), is(transaction.getUUID()));
        assertValid(loaded);
    }

    /**
     * Verifies that old XML dividend rows keep ex-date, units, and forex facts when migrated.
     * The ledger-backed projection must show the same business values after load.
     */
    @Test
    public void testOldXmlDividendLoadsWithExDateUnitsAndForex() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var security = register(client, security());
        var dividend = accountTransaction(AccountTransaction.Type.DIVIDENDS, 120);

        dividend.setSecurity(security);
        dividend.setShares(Values.Share.factorize(4));
        dividend.setExDate(EX_DATE);
        dividend.addUnit(new Unit(Unit.Type.FEE, money(2)));
        dividend.addUnit(new Unit(Unit.Type.TAX, money(3),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(250)), BigDecimal.valueOf(0.012)));
        dividend.addUnit(new Unit(Unit.Type.GROSS_VALUE, money(12),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1000)), BigDecimal.valueOf(0.012)));
        account.addTransaction(dividend);

        var loaded = load(oldXmlWithoutLedger(client));
        var projection = loaded.getAccounts().get(0).getTransactions().get(0);
        var entry = loaded.getLedger().getEntries().get(0);
        var cashPosting = entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.CASH)
                        .findFirst().orElseThrow();

        assertThat(entry.getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(projection, instanceOf(LedgerBackedTransaction.class));
        assertThat(projection.getSecurity().getName(), is(security.getName()));
        assertThat(projection.getExDate(), is(EX_DATE));
        assertThat(projection.getUnits().count(), is(3L));
        assertThat(entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.TAX)
                        .findFirst().orElseThrow().getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(cashPosting.getSecurity().getName(), is(security.getName()));
        assertValid(loaded);
    }

    /**
     * Verifies that old XML cross-entry families load into one ledger entry per business event.
     * Buy/sell and transfers must not become separate persisted truths during migration.
     */
    @Test
    public void testOldXmlCrossEntryFamiliesLoadIntoSingleLedgerEntries() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var targetAccount = register(client, account());
        var portfolio = register(client, portfolio());
        var targetPortfolio = register(client, portfolio());
        var deliveryPortfolio = register(client, portfolio());
        var security = register(client, security());

        var buy = new BuySellEntry(portfolio, account);
        buy.setType(PortfolioTransaction.Type.BUY);
        buy.setDate(DATE_TIME);
        buy.setSecurity(security);
        buy.setShares(Values.Share.factorize(5));
        buy.setAmount(Values.Amount.factorize(100));
        buy.setCurrencyCode(CurrencyUnit.EUR);
        buy.insert();

        var accountTransfer = new AccountTransferEntry(account, targetAccount);
        accountTransfer.setDate(DATE_TIME);
        accountTransfer.setAmount(Values.Amount.factorize(25));
        accountTransfer.setCurrencyCode(CurrencyUnit.EUR);
        accountTransfer.insert();

        var portfolioTransfer = new PortfolioTransferEntry(portfolio, targetPortfolio);
        portfolioTransfer.setDate(DATE_TIME);
        portfolioTransfer.setSecurity(security);
        portfolioTransfer.setShares(Values.Share.factorize(2));
        portfolioTransfer.setAmount(Values.Amount.factorize(40));
        portfolioTransfer.setCurrencyCode(CurrencyUnit.EUR);
        portfolioTransfer.insert();

        deliveryPortfolio.addTransaction(portfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND, security, 30));

        var loaded = load(oldXmlWithoutLedger(client));

        assertThat(loaded.getLedger().getEntries().stream().filter(entry -> entry.getType() == LedgerEntryType.BUY)
                        .count(), is(1L));
        assertThat(loaded.getLedger().getEntries().stream()
                        .filter(entry -> entry.getType() == LedgerEntryType.CASH_TRANSFER).count(), is(1L));
        assertThat(loaded.getLedger().getEntries().stream()
                        .filter(entry -> entry.getType() == LedgerEntryType.SECURITY_TRANSFER).count(), is(1L));
        assertThat(loaded.getLedger().getEntries().stream()
                        .filter(entry -> entry.getType() == LedgerEntryType.DELIVERY_INBOUND).count(), is(1L));
        assertValid(loaded);
    }

    /**
     * Verifies that XML roundtrip persists the ledger as truth, not runtime projections.
     * Owner lists must be restored from the ledger after load.
     */
    @Test
    public void testLedgerXmlRoundtripPreservesTruthAndDoesNotPersistRuntimeProjections() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var security = register(client, security());
        var metadata = LedgerTransactionMetadata.of(DATE_TIME).withNote("note").withSource("source");
        var units = LedgerCreationUnits.of(LedgerCreationUnit.fee(money(2)),
                        LedgerCreationUnit.tax(money(3), LedgerForexAmount.of(
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(250)),
                                        BigDecimal.valueOf(0.012))),
                        LedgerCreationUnit.grossValue(money(12), LedgerForexAmount.of(
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1000)),
                                        BigDecimal.valueOf(0.012))));
        var dividend = LedgerDividend.withExDate(
                        LedgerAccountCashLeg.of(account, money(120),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(100)),
                                                        BigDecimal.valueOf(1.2))),
                        LedgerOptionalSecurity.of(security), units, EX_DATE);

        new LedgerTransactionCreator(client).createDividend(metadata, dividend);
        client.getLedger().getEntries().get(0)
                        .addParameter(LedgerParameter.ofString(
                                        LedgerParameterType.CORPORATE_ACTION_KIND,
                                        CorporateActionKind.SPIN_OFF.getCode()));
        LedgerProjectionService.materialize(client);

        var entryUUID = client.getLedger().getEntries().get(0).getUUID();
        var postingUUIDs = client.getLedger().getEntries().get(0).getPostings().stream().map(LedgerPosting::getUUID)
                        .toList();
        var projectionUUID = client.getLedger().getEntries().get(0).getProjectionRefs().get(0).getUUID();
        var xml = save(client);

        assertTrue(xml.contains("<ledger>"));
        assertFalse(xml.contains("<account-transaction"));
        assertFalse(xml.contains("LedgerBacked"));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));

        var loaded = load(xml);
        var reloadedEntry = loaded.getLedger().getEntries().get(0);
        var reloadedProjection = loaded.getAccounts().get(0).getTransactions().get(0);
        var reloadedPostingUUIDs = reloadedEntry.getPostings().stream().map(LedgerPosting::getUUID).toList();

        assertThat(reloadedEntry.getUUID(), is(entryUUID));
        assertThat(reloadedPostingUUIDs, is(postingUUIDs));
        assertThat(reloadedEntry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(reloadedEntry.getDateTime(), is(DATE_TIME));
        assertThat(reloadedEntry.getNote(), is("note"));
        assertThat(reloadedEntry.getSource(), is("source"));
        assertThat(reloadedEntry.getParameters().size(), is(1));
        assertThat(reloadedEntry.getParameters().get(0).getType(), is(LedgerParameterType.CORPORATE_ACTION_KIND));
        assertThat(reloadedEntry.getParameters().get(0).getValue(), is(CorporateActionKind.SPIN_OFF.getCode()));
        assertThat(reloadedProjection, instanceOf(LedgerBackedTransaction.class));
        assertThat(reloadedProjection.getExDate(), is(EX_DATE));
        assertThat(reloadedProjection.getUnits().count(), is(3L));
        assertValid(loaded);
    }

    /**
     * Verifies that loading XML with ledger truth does not migrate compatibility rows again.
     * Shadow rows must stay derived data and not create duplicate ledger entries.
     */
    @Test
    public void testLedgerXmlLoadDoesNotRemigrateCompatibilityRows() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var transaction = accountTransaction(AccountTransaction.Type.DEPOSIT, 100);

        account.addTransaction(transaction);
        new LegacyTransactionToLedgerMigrator().migrate(client);

        account.getTransactions().add(0, transaction);

        var loaded = load(save(client));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
    }

    /**
     * Verifies that legacy ledger parameter aliases remain readable.
     * Saving must write the current XML form after load.
     */
    @Test
    public void testLegacyLedgerParameterAliasesLoadAndSaveCurrentForm() throws Exception
    {
        var client = legacyLedgerParameterCompatibilityClient();
        var entry = client.getLedger().getEntries().get(0);
        var sourceProjection = projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG);
        var cashProjection = projection(entry, LedgerProjectionRole.CASH_COMPENSATION);
        var expectedSourcePostingUUID = sourceProjection.getPrimaryPostingUUID();
        var expectedCashPostingUUID = cashProjection.getPrimaryPostingUUID();
        var expectedCashGroupUUID = cashProjection.getPostingGroupUUID();
        var expectedParameterCount = parameterCount(entry);
        var xml = save(client);
        var legacyXml = legacyLedgerParameterXml(xml);

        assertTrue(legacyXml.contains("<ledger-posting-parameter>"));
        assertTrue(legacyXml.contains("class=\"ledger-posting-parameter-type\""));

        var loaded = load(legacyXml);
        var loadedEntry = loaded.getLedger().getEntries().get(0);
        var loadedSourceProjection = projection(loadedEntry, LedgerProjectionRole.OLD_SECURITY_LEG);
        var loadedCashProjection = projection(loadedEntry, LedgerProjectionRole.CASH_COMPENSATION);
        var loadedSourcePosting = posting(loadedEntry, expectedSourcePostingUUID);
        var loadedCashPosting = posting(loadedEntry, expectedCashPostingUUID);
        var loadedAccount = loaded.getAccounts().get(0);
        var loadedPortfolio = loaded.getPortfolios().get(0);
        var loadedSourceSecurity = loaded.getSecurities().get(0);

        assertValid(loaded);
        assertThat(parameterCount(loadedEntry), is(expectedParameterCount));
        assertThat(parameter(loadedSourcePosting.getParameters(), LedgerParameterType.CORPORATE_ACTION_LEG).getValue(),
                        is(CorporateActionLeg.SOURCE_SECURITY.getCode()));
        assertThat(parameter(loadedCashPosting.getParameters(), LedgerParameterType.CASH_COMPENSATION_KIND).getValue(),
                        is(CashCompensationKind.CASH_IN_LIEU.getCode()));
        assertSame(loadedPortfolio, loadedSourcePosting.getPortfolio());
        assertSame(loadedSourceSecurity, loadedSourcePosting.getSecurity());
        assertSame(loadedPortfolio, loadedSourceProjection.getPortfolio());
        assertSame(loadedAccount, loadedCashPosting.getAccount());
        assertSame(loadedAccount, loadedCashProjection.getAccount());
        assertThat(primaryPostingUUID(loadedSourceProjection), is(expectedSourcePostingUUID));
        assertThat(primaryPostingUUID(loadedCashProjection), is(expectedCashPostingUUID));
        assertThat(postingGroupUUID(loadedCashProjection), is(expectedCashGroupUUID));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(loaded.getPortfolios().get(0).getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance).count(), is(2L));

        var currentXml = save(loaded);

        assertFalse(currentXml.contains("<ledger-posting-parameter>"));
        assertFalse(currentXml.contains("ledger-posting-parameter-type"));
        assertFalse(currentXml.contains("LedgerBacked"));
        assertTrue(currentXml.contains("<ledger-parameter type=\"CORPORATE_ACTION_LEG\" "
                        + "valueKind=\"STRING\" value=\"SOURCE_SECURITY\"/>"));
    }

    /**
     * Verifies that XML LedgerParameter diagnostics have stable persistence codes.
     * Malformed persisted parameters must fail clearly without changing load semantics.
     */
    @Test
    public void testLedgerParameterXmlMissingTypeHasPersistCode() throws Exception
    {
        var xml = save(legacyLedgerParameterCompatibilityClient());
        var brokenXml = replaceRequired(xml,
                        "<ledger-parameter type=\"CORPORATE_ACTION_LEG\" valueKind=\"STRING\" value=\"SOURCE_SECURITY\"/>",
                        "<ledger-parameter valueKind=\"STRING\" value=\"SOURCE_SECURITY\"/>");

        assertLedgerParameterXmlFailure(brokenXml, LedgerDiagnosticCode.LEDGER_PERSIST_004);
    }

    /**
     * Verifies that XML LedgerParameter diagnostics have stable persistence codes.
     * Malformed persisted parameters must fail clearly without changing load semantics.
     */
    @Test
    public void testLedgerParameterXmlMissingValueKindHasPersistCode() throws Exception
    {
        var xml = save(legacyLedgerParameterCompatibilityClient());
        var brokenXml = replaceRequired(xml,
                        "<ledger-parameter type=\"CORPORATE_ACTION_LEG\" valueKind=\"STRING\" value=\"SOURCE_SECURITY\"/>",
                        "<ledger-parameter type=\"CORPORATE_ACTION_LEG\" value=\"SOURCE_SECURITY\"/>");

        assertLedgerParameterXmlFailure(brokenXml, LedgerDiagnosticCode.LEDGER_PERSIST_005);
    }

    /**
     * Verifies that XML LedgerParameter diagnostics have stable persistence codes.
     * Malformed persisted parameters must fail clearly without changing load semantics.
     */
    @Test
    public void testLedgerParameterXmlReferenceValueMissingNodeHasPersistCode() throws Exception
    {
        var xml = save(legacyLedgerParameterCompatibilityClient());
        var brokenXml = replaceRequiredPattern(xml,
                        "(?s)<ledger-parameter type=\"SOURCE_SECURITY\" valueKind=\"SECURITY\">\\s*<value[^>]*/>\\s*</ledger-parameter>",
                        "<ledger-parameter type=\"SOURCE_SECURITY\" valueKind=\"SECURITY\"/>");

        assertLedgerParameterXmlFailure(brokenXml, LedgerDiagnosticCode.LEDGER_PERSIST_006);
    }

    /**
     * Verifies that XML LedgerParameter diagnostics have stable persistence codes.
     * Malformed persisted parameters must fail clearly without changing load semantics.
     */
    @Test
    public void testLedgerParameterXmlMissingAttributeHasPersistCode() throws Exception
    {
        var xml = save(legacyLedgerParameterCompatibilityClient());
        var brokenXml = replaceRequired(xml,
                        "<ledger-parameter type=\"CORPORATE_ACTION_LEG\" valueKind=\"STRING\" value=\"SOURCE_SECURITY\"/>",
                        "<ledger-parameter type=\"CORPORATE_ACTION_LEG\" valueKind=\"STRING\"/>");

        assertLedgerParameterXmlFailure(brokenXml, LedgerDiagnosticCode.LEDGER_PERSIST_008);
    }

    /**
     * Verifies that preparing a client for XML save does not visibly mutate owner lists.
     * Runtime projections must be restored after the persistence filter runs.
     */
    @Test
    public void testSaveLeavesOwnerListsUnchanged() throws Exception
    {
        var fixture = saveRestoreFixture(new InvestmentPlan("plan"));
        var accountBefore = List.copyOf(fixture.account().getTransactions());
        var portfolioBefore = List.copyOf(fixture.portfolio().getTransactions());
        var planBefore = List.copyOf(fixture.plan().getTransactions());

        var xml = save(fixture.client());

        assertTrue(xml.contains("<ledger>"));
        assertFalse(xml.contains("LedgerBacked"));
        assertThat(fixture.account().getTransactions(), is(accountBefore));
        assertThat(fixture.portfolio().getTransactions(), is(portfolioBefore));
        assertThat(fixture.plan().getTransactions(), is(planBefore));
    }

    /**
     * Verifies that serialization failures restore owner lists and their order.
     * A failed save must not leave the UI model without its runtime projections.
     */
    @Test
    public void testSerializationFailureRestoresOwnerListsAndOrder() throws Exception
    {
        var fixture = saveRestoreFixture(new InvestmentPlan("plan"));
        var accountBefore = List.copyOf(fixture.account().getTransactions());
        var portfolioBefore = List.copyOf(fixture.portfolio().getTransactions());
        var planBefore = List.copyOf(fixture.plan().getTransactions());

        assertThrows(com.thoughtworks.xstream.io.StreamException.class,
                        () -> saveWithOutputStream(fixture.client(), new FailingOutputStream()));

        assertThat(fixture.account().getTransactions(), is(accountBefore));
        assertThat(fixture.portfolio().getTransactions(), is(portfolioBefore));
        assertThat(fixture.plan().getTransactions(), is(planBefore));
    }

    /**
     * Verifies that preparation failures restore owner lists, projection refs, and order.
     * Save-time cleanup must be atomic from the caller's point of view.
     */
    @Test
    public void testPreparationFailureRestoresOwnerListsRefsAndOrder() throws Exception
    {
        var plan = new FailingLedgerExecutionRefPlan();
        var fixture = saveRestoreFixture(plan);
        var accountBefore = List.copyOf(fixture.account().getTransactions());
        var portfolioBefore = List.copyOf(fixture.portfolio().getTransactions());
        var planBefore = List.copyOf(fixture.plan().getTransactions());
        var refsBefore = List.copyOf(fixture.plan().getLedgerExecutionRefs());

        assertThrows(IllegalStateException.class, () -> save(fixture.client()));

        assertThat(fixture.account().getTransactions(), is(accountBefore));
        assertThat(fixture.portfolio().getTransactions(), is(portfolioBefore));
        assertThat(fixture.plan().getTransactions(), is(planBefore));
        assertThat(fixture.plan().getLedgerExecutionRefs(), is(refsBefore));
    }

    /**
     * Verifies that invalid ledger XML save failures include formatted diagnostics.
     * The caller must see the concrete ledger validation problem instead of a generic error.
     */
    @Test
    public void testInvalidLedgerSaveExceptionUsesFormattedDiagnostics()
    {
        var client = new Client();
        var account = register(client, account());

        account.setName("Save Account");
        var created = new LedgerTransactionCreator(client).createDeposit(LedgerTransactionMetadata.of(DATE_TIME),
                        LedgerAccountCashLeg.of(account, money(100)));

        created.getEntry().getPostings().get(0).setCurrency(null);

        var exception = assertThrows(IOException.class, () -> save(client));

        assertTrue(exception.getMessage(), exception.getMessage().contains("[LEDGER-PERSIST-001] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("[POSTING_CURRENCY_REQUIRED] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("[LEDGER-STRUCT-014] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("\n  Posting:\n"));
        assertTrue(exception.getMessage(), exception.getMessage().contains("Currency: <missing>"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":\n"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterDate + ": 2026-02-03T00:00"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterType + ": DEPOSIT"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterAccount + ": Save Account"));
    }

    /**
     * Verifies that plan execution refs roundtrip through XML and still resolve.
     * A generated booking must be found from the ledger entry and projection after reload.
     */
    @Test
    public void testInvestmentPlanLedgerExecutionRefsRoundtripAndResolve() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var security = register(client, security());
        var plan = new InvestmentPlan("plan");

        client.addPlan(plan);
        new LedgerTransactionCreator(client).createBuy(LedgerTransactionMetadata.of(DATE_TIME),
                        LedgerAccountCashLeg.of(account, money(100)),
                        LedgerPortfolioSecurityLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100)),
                        LedgerCreationUnits.none());
        LedgerProjectionService.materialize(client);

        var portfolioProjection = portfolio.getTransactions().get(0);
        plan.getTransactions().add(portfolioProjection);

        var xml = save(client);

        assertTrue(xml.contains("<ledger-execution-ref>"));
        assertFalse(xml.contains("LedgerBacked"));

        var loaded = load(xml);
        var loadedPlan = loaded.getPlans().get(0);

        assertThat(loadedPlan.getTransactions().size(), is(0));
        assertThat(loadedPlan.getLedgerExecutionRefs().size(), is(1));
        assertThat(loadedPlan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(portfolioProjection.getUUID()));
        assertThat(loadedPlan.getTransactions(loaded).size(), is(1));
        assertThat(loadedPlan.getTransactions(loaded).get(0).getOwner(), is(loaded.getPortfolios().get(0)));
        assertThat(loadedPlan.getTransactions(loaded).get(0).getTransaction().getUUID(), is(portfolioProjection.getUUID()));
    }

    /**
     * Verifies that ambiguous plan execution refs are rejected during XML load.
     * A plan must not silently choose one of several possible ledger projections.
     */
    @Test
    public void testAmbiguousInvestmentPlanLedgerExecutionRefIsRejected() throws Exception
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var security = register(client, security());
        var plan = new InvestmentPlan("plan");

        client.addPlan(plan);
        new LedgerTransactionCreator(client).createBuy(LedgerTransactionMetadata.of(DATE_TIME),
                        LedgerAccountCashLeg.of(account, money(100)),
                        LedgerPortfolioSecurityLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100)),
                        LedgerCreationUnits.none());
        LedgerProjectionService.materialize(client);
        plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(
                        client.getLedger().getEntries().get(0).getUUID(), null, null));

        assertThrows(IllegalArgumentException.class, () -> plan.getTransactions(client));
    }

    private String oldXmlWithoutLedger(Client client) throws Exception
    {
        return save(client).replaceFirst("(?s)\\s*<ledger>.*?</ledger>", "")
                        .replaceFirst("(?s)\\s*<ledger/>", "");
    }

    private String save(Client client) throws Exception
    {
        var file = File.createTempFile("ledger", ".xml");

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

    private Client load(String xml) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void assertLedgerParameterXmlFailure(String xml, LedgerDiagnosticCode code)
    {
        var exception = assertThrows(IOException.class, () -> load(xml));

        assertTrue(exception.getMessage(), exception.getMessage().contains(code.prefix()));
    }

    private String replaceRequired(String xml, String target, String replacement)
    {
        var replaced = xml.replace(target, replacement);

        assertFalse(replaced.equals(xml));

        return replaced;
    }

    private String replaceRequiredPattern(String xml, String pattern, String replacement)
    {
        var replaced = xml.replaceFirst(pattern, replacement);

        assertFalse(replaced.equals(xml));

        return replaced;
    }

    private void saveWithOutputStream(Client client, OutputStream output) throws Exception
    {
        var type = Class.forName("name.abuchen.portfolio.model.ClientFactory$XmlSerialization");
        var constructor = type.getDeclaredConstructor(boolean.class);
        var save = type.getDeclaredMethod("save", Client.class, OutputStream.class);

        constructor.setAccessible(true);
        save.setAccessible(true);

        try
        {
            save.invoke(constructor.newInstance(false), client, output);
        }
        catch (InvocationTargetException e)
        {
            var cause = e.getCause();

            if (cause instanceof Exception exception)
                throw exception;
            if (cause instanceof Error error)
                throw error;

            throw new AssertionError(cause);
        }
    }

    private void assertValid(Client client)
    {
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
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

    private Security register(Client client, Security security)
    {
        client.addSecurity(security);
        return security;
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

    private AccountTransaction accountTransaction(AccountTransaction.Type type, int amount)
    {
        var transaction = new AccountTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(amount));
        transaction.setNote("note");
        transaction.setSource("source");

        return transaction;
    }

    private PortfolioTransaction portfolioTransaction(PortfolioTransaction.Type type, Security security, int amount)
    {
        var transaction = new PortfolioTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(amount));
        transaction.setSecurity(security);
        transaction.setShares(Values.Share.factorize(5));
        transaction.setNote("note");
        transaction.setSource("source");

        return transaction;
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private Client legacyLedgerParameterCompatibilityClient()
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var sourceSecurity = register(client, new Security("Source Security", CurrencyUnit.EUR));
        var targetSecurity = register(client, new Security("Target Security", CurrencyUnit.EUR));

        LedgerNativeEntryAssembler.forClient(client).spinOff() //
                        .metadata(NativeEntryMetadata.of(DATE_TIME).note("legacy alias test")
                                        .source("ledger-xml-compatibility-test")) //
                        .event(NativeCorporateActionEvent.builder() //
                                        .kind(CorporateActionKind.SPIN_OFF) //
                                        .subtype(CorporateActionSubtype.STANDARD) //
                                        .reference("legacy-ledger-parameter-alias") //
                                        .stage(EventStage.SETTLED) //
                                        .effectiveDate(LocalDate.of(2026, 2, 3)) //
                                        .build()) //
                        .securityLeg(NativeSecurityLeg.source() //
                                        .portfolio(portfolio) //
                                        .security(sourceSecurity) //
                                        .shares(Values.Share.factorize(10)) //
                                        .amount(money(100)) //
                                        .sourceSecurity(sourceSecurity) //
                                        .targetSecurity(targetSecurity) //
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                                        .build()) //
                        .securityLeg(NativeSecurityLeg.target() //
                                        .portfolio(portfolio) //
                                        .security(targetSecurity) //
                                        .shares(Values.Share.factorize(5)) //
                                        .amount(money(50)) //
                                        .sourceSecurity(sourceSecurity) //
                                        .targetSecurity(targetSecurity) //
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                                        .build()) //
                        .cashCompensation(NativeCashCompensation.builder() //
                                        .account(account) //
                                        .amount(money(5)) //
                                        .kind(CashCompensationKind.CASH_IN_LIEU) //
                                        .applied(true) //
                                        .build()) //
                        .buildAndAdd();

        return client;
    }

    private String legacyLedgerParameterXml(String xml)
    {
        return xml.replace("<ledger-parameter type=\"CORPORATE_ACTION_LEG\" valueKind=\"STRING\" "
                        + "value=\"SOURCE_SECURITY\"/>",
                        "<ledger-posting-parameter>"
                                        + "<type class=\"ledger-posting-parameter-type\">CORPORATE_ACTION_LEG</type>"
                                        + "<valueKind>STRING</valueKind>"
                                        + "<value class=\"string\">SOURCE_SECURITY</value>"
                                        + "</ledger-posting-parameter>");
    }

    private int parameterCount(LedgerEntry entry)
    {
        return entry.getParameters().size() + entry.getPostings().stream().mapToInt(posting -> posting.getParameters().size())
                        .sum();
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private String primaryPostingUUID(LedgerProjectionRef projection)
    {
        return projection.getPrimaryMembership().map(ProjectionMembership::getPostingUUID)
                        .orElse(projection.getPrimaryPostingUUID());
    }

    private String postingGroupUUID(LedgerProjectionRef projection)
    {
        return projection.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).stream().findFirst()
                        .map(ProjectionMembership::getPostingUUID).orElse(projection.getPostingGroupUUID());
    }

    private LedgerPosting posting(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream().filter(posting -> uuid.equals(posting.getUUID())).findFirst()
                        .orElseThrow();
    }

    private LedgerParameter<?> parameter(List<LedgerParameter<?>> parameters, LedgerParameterType type)
    {
        return parameters.stream().filter(parameter -> parameter.getType() == type).findFirst().orElseThrow();
    }

    private SaveRestoreFixture saveRestoreFixture(InvestmentPlan plan)
    {
        var client = new Client();
        var account = register(client, account());
        var portfolio = register(client, portfolio());
        var security = register(client, security());
        var legacyAccountTransaction = accountTransaction(AccountTransaction.Type.DEPOSIT, 1);
        var legacyPortfolioTransaction = portfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND, security, 2);

        account.addTransaction(legacyAccountTransaction);
        portfolio.addTransaction(legacyPortfolioTransaction);
        client.addPlan(plan);

        new LedgerTransactionCreator(client).createBuy(LedgerTransactionMetadata.of(DATE_TIME),
                        LedgerAccountCashLeg.of(account, money(100)),
                        LedgerPortfolioSecurityLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100)),
                        LedgerCreationUnits.none());
        LedgerProjectionService.materialize(client);

        var accountProjection = account.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .findFirst().orElseThrow();
        var portfolioProjection = portfolio.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .findFirst().orElseThrow();

        plan.getTransactions().add(legacyAccountTransaction);
        plan.getTransactions().add(portfolioProjection);
        plan.getTransactions().add(legacyPortfolioTransaction);

        assertThat(account.getTransactions(), is(List.of(legacyAccountTransaction, accountProjection)));
        assertThat(portfolio.getTransactions(), is(List.of(legacyPortfolioTransaction, portfolioProjection)));
        assertThat(plan.getTransactions(), is(List.of(legacyAccountTransaction, portfolioProjection,
                        legacyPortfolioTransaction)));

        return new SaveRestoreFixture(client, account, portfolio, plan);
    }

    private record SaveRestoreFixture(Client client, Account account, Portfolio portfolio, InvestmentPlan plan)
    {
    }

    private static final class FailingOutputStream extends OutputStream
    {
        @Override
        public void write(int b) throws IOException
        {
            throw new IOException("forced serialization failure");
        }
    }

    private static final class FailingLedgerExecutionRefPlan extends InvestmentPlan
    {
        private final List<LedgerExecutionRef> refs = new FailingLedgerExecutionRefList();

        @Override
        public List<LedgerExecutionRef> getLedgerExecutionRefs()
        {
            return refs;
        }
    }

    private static final class FailingLedgerExecutionRefList extends ArrayList<InvestmentPlan.LedgerExecutionRef>
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean add(InvestmentPlan.LedgerExecutionRef ref)
        {
            super.add(ref);
            throw new IllegalStateException("forced preparation failure");
        }
    }
}
