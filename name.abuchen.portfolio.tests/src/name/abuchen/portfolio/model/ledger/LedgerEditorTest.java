package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransactionEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransactionEditor;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferEditor;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellEditor;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionEditor;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividend;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerForexAmount;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerOptionalSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferEditor;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingPatch;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingUpdater;
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
 * Tests same-shape ledger editing for existing transaction entries.
 * These tests make sure supported edits update ledger facts while projections remain derived runtime views.
 */
@SuppressWarnings("nls")
public class LedgerEditorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);

    /**
     * Checks the ledger-backed editing scenario: field edit states are explicit.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testFieldEditStatesAreExplicit()
    {
        assertTrue(LedgerFieldEdit.omitted().isOmitted());
        assertTrue(LedgerFieldEdit.set("value").isSet());
        assertThat(LedgerFieldEdit.set("value").getValue(), is("value"));
        assertTrue(LedgerFieldEdit.clear().isClear());
        var exception = assertThrows(IllegalStateException.class, () -> LedgerFieldEdit.clear().getValue());

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_CORE_006
                        .message("Only set edits have a value")));
    }

    /**
     * Checks the ledger-backed editing scenario: metadata patch omitted set and clear.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testMetadataPatchOmittedSetAndClear()
    {
        var client = new Client();
        var entry = creator(client).createDeposit(
                        LedgerTransactionMetadata.of(DATE_TIME).withNote("old note").withSource("old source"),
                        cashLeg(account(), 100)).getEntry();

        LedgerEntryMetadataPatchHelper.apply(entry, LedgerEntryMetadataPatch.none());

        assertThat(entry.getDateTime(), is(DATE_TIME));
        assertThat(entry.getNote(), is("old note"));
        assertThat(entry.getSource(), is("old source"));

        LedgerEntryMetadataPatchHelper.apply(entry, LedgerEntryMetadataPatch.builder()
                        .dateTime(DATE_TIME.plusDays(1)).note("new note").source("new source").build());

        assertThat(entry.getDateTime(), is(DATE_TIME.plusDays(1)));
        assertThat(entry.getNote(), is("new note"));
        assertThat(entry.getSource(), is("new source"));

        LedgerEntryMetadataPatchHelper.apply(entry,
                        LedgerEntryMetadataPatch.builder().clearNote().clearSource().build());

        assertNull(entry.getNote());
        assertNull(entry.getSource());
    }

    /**
     * Checks the ledger-backed editing scenario: invalid metadata patch does not partially mutate note or source.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidMetadataPatchDoesNotPartiallyMutateNoteOrSource()
    {
        var entry = new LedgerEntry();

        entry.setType(LedgerEntryType.DEPOSIT);
        entry.setDateTime(DATE_TIME);
        entry.setNote("old note");
        entry.setSource("old source");
        entry.addPosting(new LedgerPosting());

        assertThrows(IllegalArgumentException.class, () -> LedgerEntryMetadataPatchHelper.apply(entry,
                        LedgerEntryMetadataPatch.builder().note("new note").source("new source").build()));

        assertThat(entry.getNote(), is("old note"));
        assertThat(entry.getSource(), is("old source"));
    }

    /**
     * Checks the ledger-backed editing scenario: account transaction editor patches amount and currency.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountTransactionEditorPatchesAmountAndCurrency()
    {
        var client = new Client();
        var account = account();
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);

        new LedgerAccountTransactionEditor().apply(transaction,
                        LedgerAccountTransactionEdit.builder().amount(150).currency(CurrencyUnit.USD).build());

        assertThat(entry.getUUID(), is(((LedgerBackedTransaction) transaction).getLedgerEntry().getUUID()));
        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertThat(transaction.getAmount(), is(150L));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.USD));
    }

    /**
     * Checks the ledger-backed editing scenario: dividend editor patches ex-date and preserves surviving unit uuids.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testDividendEditorPatchesExDateAndPreservesSurvivingUnitUUIDs()
    {
        var client = new Client();
        var account = account();
        var forex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 40L), BigDecimal.valueOf(0.90));
        var units = LedgerCreationUnits.of(LedgerCreationUnit.tax(money(3)),
                        LedgerCreationUnit.fee(money(2)),
                        LedgerCreationUnit.grossValue(Money.of(CurrencyUnit.EUR, 36L), forex));
        var dividend = LedgerDividend.withExDate(cashLeg(account, 30), LedgerOptionalSecurity.of(security()), units,
                        DATE_TIME.minusDays(5));
        var entry = creator(client).createDividend(metadata(), dividend).getEntry();
        var taxUUID = posting(entry, LedgerPostingType.TAX).getUUID();
        var feeUUID = posting(entry, LedgerPostingType.FEE).getUUID();
        var grossValueUUID = posting(entry, LedgerPostingType.GROSS_VALUE).getUUID();

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);

        new LedgerAccountTransactionEditor().apply(transaction, LedgerAccountTransactionEdit.builder()
                        .exDate(DATE_TIME.minusDays(1))
                        .units(LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.update(taxUUID,
                                        Money.of(CurrencyUnit.EUR, 4L))))
                        .build());

        assertThat(transaction.getExDate(), is(DATE_TIME.minusDays(1)));
        assertThat(posting(entry, LedgerPostingType.TAX).getUUID(), is(taxUUID));
        assertThat(posting(entry, LedgerPostingType.TAX).getAmount(), is(4L));
        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(entry, LedgerPostingType.GROSS_VALUE).getUUID(), is(grossValueUUID));

        new LedgerAccountTransactionEditor().apply(transaction,
                        LedgerAccountTransactionEdit.builder().clearExDate().build());

        assertNull(transaction.getExDate());
    }

    /**
     * Checks the ledger-backed editing scenario: unit posting updater adds updates and removes units without changing projections.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testUnitPostingUpdaterAddsUpdatesAndRemovesUnitsWithoutChangingProjections()
    {
        var client = new Client();
        var account = account();
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionCount = entry.getProjectionRefs().size();
        var updater = new LedgerUnitPostingUpdater();

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(1))));

        var feeUUID = posting(entry, LedgerPostingType.FEE).getUUID();

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.update(feeUUID,
                        Money.of(CurrencyUnit.EUR, 2L))));

        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(entry, LedgerPostingType.FEE).getAmount(), is(2L));

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.update(feeUUID,
                        Money.of(CurrencyUnit.EUR, 3L),
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 4L), new BigDecimal("0.7500")))));

        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(entry, LedgerPostingType.FEE).getForexAmount(), is(4L));
        assertThat(posting(entry, LedgerPostingType.FEE).getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting(entry, LedgerPostingType.FEE).getExchangeRate(), is(new BigDecimal("0.7500")));

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.update(feeUUID,
                        Money.of(CurrencyUnit.EUR, 5L))));

        assertThat(posting(entry, LedgerPostingType.FEE).getForexAmount(), is(4L));
        assertThat(posting(entry, LedgerPostingType.FEE).getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting(entry, LedgerPostingType.FEE).getExchangeRate(), is(new BigDecimal("0.7500")));

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.clearForex(feeUUID)));

        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertNull(posting(entry, LedgerPostingType.FEE).getForexAmount());
        assertNull(posting(entry, LedgerPostingType.FEE).getForexCurrency());
        assertNull(posting(entry, LedgerPostingType.FEE).getExchangeRate());

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.remove(feeUUID)));

        assertThat(entry.getProjectionRefs().size(), is(projectionCount));
        assertTrue(entry.getPostings().stream().noneMatch(posting -> posting.getUUID().equals(feeUUID)));
    }

    /**
     * Checks the ledger-backed editing scenario: delivery editor patches security shares and amount.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testDeliveryEditorPatchesSecuritySharesAndAmount()
    {
        var client = new Client();
        var portfolio = portfolio();
        var newSecurity = security();
        var entry = creator(client).createInboundDelivery(metadata(), deliveryLeg(portfolio)).getEntry();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        var postingUUID = entry.getPostings().get(0).getUUID();

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedPortfolioTransaction) portfolio.getTransactions().get(0);

        new LedgerDeliveryTransactionEditor().apply(transaction, LedgerDeliveryTransactionEdit.builder()
                        .security(newSecurity).shares(Values.Share.factorize(7)).amount(77L).build());

        assertThat(entry.getType(), is(LedgerEntryType.DELIVERY_INBOUND));
        assertThat(entry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(entry.getPostings().get(0).getUUID(), is(postingUUID));
        assertSame(newSecurity, transaction.getSecurity());
        assertThat(transaction.getShares(), is(Values.Share.factorize(7)));
        assertThat(transaction.getAmount(), is(77L));
    }

    /**
     * Checks the ledger-backed editing scenario: buy/sell editor patches cash and security postings.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testBuySellEditorPatchesCashAndSecurityPostings()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        var newSecurity = security();
        var entry = creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none()).getEntry();
        var accountProjectionUUID = projection(entry, LedgerProjectionRole.ACCOUNT).getUUID();
        var portfolioProjectionUUID = projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID();
        var cashPostingUUID = posting(entry, LedgerPostingType.CASH).getUUID();
        var securityPostingUUID = posting(entry, LedgerPostingType.SECURITY).getUUID();

        LedgerProjectionService.materialize(client);

        var accountTransaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);
        var portfolioTransaction = (LedgerBackedPortfolioTransaction) portfolio.getTransactions().get(0);

        new LedgerBuySellEditor().apply(accountTransaction, LedgerBuySellEdit.builder().cashAmount(120L)
                        .securityAmount(121L).security(newSecurity).shares(Values.Share.factorize(9)).build());

        assertThat(projection(entry, LedgerProjectionRole.ACCOUNT).getUUID(), is(accountProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID(), is(portfolioProjectionUUID));
        assertThat(posting(entry, LedgerPostingType.CASH).getUUID(), is(cashPostingUUID));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(accountTransaction.getAmount(), is(120L));
        assertThat(portfolioTransaction.getAmount(), is(121L));
        assertSame(newSecurity, portfolioTransaction.getSecurity());
        assertThat(portfolioTransaction.getShares(), is(Values.Share.factorize(9)));
    }

    /**
     * Checks the ledger-backed editing scenario: metadata patch rejects date clear.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testMetadataPatchRejectsDateTimeClear() throws ReflectiveOperationException
    {
        var entry = new LedgerEntry();

        entry.setType(LedgerEntryType.DEPOSIT);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(new LedgerPosting());

        var builder = LedgerEntryMetadataPatch.builder();
        Field field = builder.getClass().getDeclaredField("dateTime");
        field.setAccessible(true);
        field.set(builder, LedgerFieldEdit.clear());

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerEntryMetadataPatchHelper.apply(entry, builder.build()));

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_CORE_005
                        .message("Ledger entry date/time cannot be cleared")));
        assertThat(entry.getDateTime(), is(DATE_TIME));
    }

    /**
     * Checks the ledger-backed editing scenario: unsupported account transaction editor apply and validate branches
     * keep distinct diagnostic code positions.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testUnsupportedAccountTransactionEditorMessagesUseDistinctConvertCodes()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);
        var edit = LedgerAccountTransactionEdit.builder().build();

        var applyFailure = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerAccountTransactionEditor().apply(transaction, edit));
        assertThat(applyFailure.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_007
                        .message("Unsupported account transaction edit for BUY")));

        var validateFailure = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerAccountTransactionEditor().validate(transaction, edit));
        assertThat(validateFailure.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_008
                        .message("Unsupported account transaction edit for BUY")));
    }

    /**
     * Checks the ledger-backed editing scenario: unsupported buy/sell editor apply and validate branches keep distinct
     * diagnostic code positions.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testUnsupportedBuySellEditorMessagesUseDistinctConvertCodes()
    {
        var entry = new LedgerEntry();
        entry.setType(LedgerEntryType.DEPOSIT);
        var edit = LedgerBuySellEdit.builder().build();

        var applyFailure = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerBuySellEditor().apply(entry, edit));
        assertThat(applyFailure.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_025
                        .message("Unsupported buy/sell edit for DEPOSIT")));

        var validateFailure = assertThrows(IllegalArgumentException.class,
                        () -> new LedgerBuySellEditor().validate(entry, edit));
        assertThat(validateFailure.getMessage(), is(LedgerDiagnosticCode.LEDGER_CONVERT_026
                        .message("Unsupported buy/sell edit for DEPOSIT")));
    }

    /**
     * Checks the ledger-backed editing scenario: projection-removal guard reports the projection diagnostic code.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects support diagnostics from ambiguous projection edit failures.
     */
    @Test
    public void testProjectionRemovedByEditMessageUsesProjectionCode() throws Exception
    {
        var method = LedgerAccountTransactionEditor.class.getDeclaredMethod("ensureProjectionExists", LedgerEntry.class, //$NON-NLS-1$
                        String.class);
        var failureUUID = "missing-projection";

        method.setAccessible(true);

        var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> method.invoke(new LedgerAccountTransactionEditor(), new LedgerEntry(), failureUUID));

        assertThat(failure.getCause().getMessage(), is(LedgerDiagnosticCode.LEDGER_PROJ_004
                        .message("Projection was removed by edit: " + failureUUID)));
    }

    /**
     * Checks the ledger-backed editing scenario: account transfer editor patches both cash postings without swapping owners.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountTransferEditorPatchesBothCashPostingsWithoutSwappingOwners()
    {
        var client = new Client();
        var source = account();
        var target = account();
        var entry = creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100))).getEntry();

        LedgerProjectionService.materialize(client);

        var sourceTransaction = (LedgerBackedAccountTransaction) source.getTransactions().get(0);
        var targetTransaction = (LedgerBackedAccountTransaction) target.getTransactions().get(0);

        new LedgerAccountTransferEditor().apply(sourceTransaction,
                        LedgerAccountTransferEdit.builder().sourceAmount(110L).targetAmount(111L).build());

        assertSame(source, projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getAccount());
        assertSame(target, projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getAccount());
        assertThat(sourceTransaction.getAmount(), is(110L));
        assertThat(targetTransaction.getAmount(), is(111L));
    }

    /**
     * Checks the ledger-backed editing scenario: account transfer editor preserves primary forex and patches hidden units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountTransferEditorPreservesPrimaryForexAndPatchesHiddenUnits()
    {
        var client = new Client();
        var source = account();
        var target = account();
        var sourceForex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10"));
        var targetForex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 180L), new BigDecimal("1.20"));
        var entry = creator(client).createAccountTransfer(metadata(),
                        LedgerCashTransferLeg.of(source, money(100), sourceForex),
                        LedgerCashTransferLeg.of(target, money(200), targetForex)).getEntry();

        new LedgerUnitPostingUpdater().apply(entry, LedgerUnitPostingPatch.of(
                        LedgerUnitPostingEdit.add(LedgerPostingType.GROSS_VALUE, money(300),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD,
                                                        Values.Amount.factorize(270)),
                                                        new BigDecimal("1.1111"))),
                        LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(3)),
                        LedgerUnitPostingEdit.add(LedgerPostingType.TAX, money(4))));

        var feeUUID = posting(entry, LedgerPostingType.FEE).getUUID();
        var taxUUID = posting(entry, LedgerPostingType.TAX).getUUID();

        LedgerProjectionService.materialize(client);

        var sourceTransaction = (LedgerBackedAccountTransaction) source.getTransactions().get(0);
        var targetTransaction = (LedgerBackedAccountTransaction) target.getTransactions().get(0);

        new LedgerAccountTransferEditor().apply(sourceTransaction,
                        LedgerAccountTransferEdit.builder().sourceAmount(110L).targetAmount(210L).build());

        assertPrimaryForex(entry, LedgerProjectionRole.SOURCE_ACCOUNT, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertPrimaryForex(entry, LedgerProjectionRole.TARGET_ACCOUNT, CurrencyUnit.USD, 180L,
                        new BigDecimal("1.20"));
        assertThat(sourceTransaction.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.GROSS_VALUE)
                        .orElseThrow().getForex().getAmount(), is(Values.Amount.factorize(270)));
        assertThat(targetTransaction.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.FEE).orElseThrow()
                        .getAmount().getAmount(), is(money(3).getAmount()));

        new LedgerAccountTransferEditor().apply(sourceTransaction,
                        LedgerAccountTransferEdit.builder()
                                        .units(LedgerUnitPostingPatch.of(
                                                        LedgerUnitPostingEdit.update(feeUUID, money(5)),
                                                        LedgerUnitPostingEdit.remove(taxUUID),
                                                        LedgerUnitPostingEdit.add(LedgerPostingType.TAX, money(6))))
                                        .build());

        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(entry, LedgerPostingType.FEE).getAmount(), is(money(5).getAmount()));
        assertTrue(entry.getPostings().stream().noneMatch(posting -> posting.getUUID().equals(taxUUID)));
        assertThat(posting(entry, LedgerPostingType.TAX).getAmount(), is(money(6).getAmount()));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: account transfer creator update preserves omitted primary forex.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountTransferCreatorUpdatePreservesOmittedPrimaryForex()
    {
        var client = new Client();
        var source = account();
        var target = account();
        var sourceForex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10"));
        var targetForex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 180L), new BigDecimal("1.20"));
        var entry = creator(client).createAccountTransfer(metadata(),
                        LedgerCashTransferLeg.of(source, money(100), sourceForex),
                        LedgerCashTransferLeg.of(target, money(200), targetForex)).getEntry();

        LedgerProjectionService.materialize(client);

        var sourceTransaction = (LedgerBackedAccountTransaction) source.getTransactions().get(0);
        var targetTransaction = (LedgerBackedAccountTransaction) target.getTransactions().get(0);
        var transfer = AccountTransferEntry.readOnly(source, sourceTransaction, target, targetTransaction);

        new LedgerAccountTransferTransactionCreator(client).update(transfer, source, target, DATE_TIME.plusDays(1),
                        111L, CurrencyUnit.EUR, 222L, CurrencyUnit.EUR, null, null, "changed", "changed-source");

        assertPrimaryForex(entry, LedgerProjectionRole.SOURCE_ACCOUNT, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertPrimaryForex(entry, LedgerProjectionRole.TARGET_ACCOUNT, CurrencyUnit.USD, 180L,
                        new BigDecimal("1.20"));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: portfolio transfer editor patches both security postings without swapping owners.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testPortfolioTransferEditorPatchesBothSecurityPostingsWithoutSwappingOwners()
    {
        var client = new Client();
        var source = portfolio();
        var target = portfolio();
        var security = security();
        var entry = creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security, Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();

        LedgerProjectionService.materialize(client);

        var sourceTransaction = (LedgerBackedPortfolioTransaction) source.getTransactions().get(0);
        var targetTransaction = (LedgerBackedPortfolioTransaction) target.getTransactions().get(0);

        new LedgerPortfolioTransferEditor().apply(sourceTransaction,
                        LedgerPortfolioTransferEdit.builder().sourceAmount(110L).targetAmount(111L)
                                        .sourceShares(Values.Share.factorize(6))
                                        .targetShares(Values.Share.factorize(6)).build());

        assertSame(source, projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getPortfolio());
        assertSame(target, projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getPortfolio());
        assertThat(sourceTransaction.getAmount(), is(110L));
        assertThat(targetTransaction.getAmount(), is(111L));
        assertThat(sourceTransaction.getShares(), is(Values.Share.factorize(6)));
        assertThat(targetTransaction.getShares(), is(Values.Share.factorize(6)));
    }

    /**
     * Checks the ledger-backed editing scenario: portfolio transfer editor preserves primary forex and patches hidden units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testPortfolioTransferEditorPreservesPrimaryForexAndPatchesHiddenUnits()
    {
        var client = new Client();
        var source = portfolio();
        var target = portfolio();
        var security = security();
        var entry = creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security, Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();
        var sourcePosting = primaryPosting(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetPosting = primaryPosting(entry, LedgerProjectionRole.TARGET_PORTFOLIO);

        sourcePosting.setForexAmount(90L);
        sourcePosting.setForexCurrency(CurrencyUnit.USD);
        sourcePosting.setExchangeRate(new BigDecimal("1.10"));
        targetPosting.setForexAmount(180L);
        targetPosting.setForexCurrency(CurrencyUnit.USD);
        targetPosting.setExchangeRate(new BigDecimal("1.20"));

        new LedgerUnitPostingUpdater().apply(entry, LedgerUnitPostingPatch.of(
                        LedgerUnitPostingEdit.add(LedgerPostingType.GROSS_VALUE, money(300),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD,
                                                        Values.Amount.factorize(270)),
                                                        new BigDecimal("1.1111"))),
                        LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(3)),
                        LedgerUnitPostingEdit.add(LedgerPostingType.TAX, money(4))));

        var feeUUID = posting(entry, LedgerPostingType.FEE).getUUID();
        var taxUUID = posting(entry, LedgerPostingType.TAX).getUUID();

        LedgerProjectionService.materialize(client);

        var sourceTransaction = (LedgerBackedPortfolioTransaction) source.getTransactions().get(0);
        var targetTransaction = (LedgerBackedPortfolioTransaction) target.getTransactions().get(0);

        new LedgerPortfolioTransferEditor().apply(sourceTransaction,
                        LedgerPortfolioTransferEdit.builder().sourceAmount(110L).targetAmount(111L).build());

        assertPrimaryForex(entry, LedgerProjectionRole.SOURCE_PORTFOLIO, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertPrimaryForex(entry, LedgerProjectionRole.TARGET_PORTFOLIO, CurrencyUnit.USD, 180L,
                        new BigDecimal("1.20"));
        assertThat(sourceTransaction.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.GROSS_VALUE)
                        .orElseThrow().getForex().getAmount(), is(Values.Amount.factorize(270)));
        assertThat(targetTransaction.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.FEE).orElseThrow()
                        .getAmount().getAmount(), is(money(3).getAmount()));

        new LedgerPortfolioTransferEditor().apply(sourceTransaction,
                        LedgerPortfolioTransferEdit.builder()
                                        .units(LedgerUnitPostingPatch.of(
                                                        LedgerUnitPostingEdit.update(feeUUID, money(5)),
                                                        LedgerUnitPostingEdit.remove(taxUUID),
                                                        LedgerUnitPostingEdit.add(LedgerPostingType.TAX, money(6))))
                                        .build());

        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(entry, LedgerPostingType.FEE).getAmount(), is(money(5).getAmount()));
        assertTrue(entry.getPostings().stream().noneMatch(posting -> posting.getUUID().equals(taxUUID)));
        assertThat(posting(entry, LedgerPostingType.TAX).getAmount(), is(money(6).getAmount()));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: buy/sell editor applies primary posting forex when supplied.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testBuySellEditorAppliesPrimaryPostingForexWhenSupplied()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        var entry = creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none()).getEntry();

        LedgerProjectionService.materialize(client);

        var accountTransaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);

        new LedgerBuySellEditor().apply(accountTransaction,
                        LedgerBuySellEdit.builder()
                                        .cashForexAmount(90L)
                                        .cashForexCurrency(CurrencyUnit.USD)
                                        .cashExchangeRate(new BigDecimal("1.10"))
                                        .securityForexAmount(91L)
                                        .securityForexCurrency(CurrencyUnit.USD)
                                        .securityExchangeRate(new BigDecimal("1.11"))
                                        .build());

        assertPrimaryForex(entry, LedgerProjectionRole.ACCOUNT, CurrencyUnit.USD, 90L, new BigDecimal("1.10"));
        assertPrimaryForex(entry, LedgerProjectionRole.PORTFOLIO, CurrencyUnit.USD, 91L,
                        new BigDecimal("1.11"));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: delivery creator update preserves omitted primary forex.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testDeliveryCreatorUpdatePreservesOmittedPrimaryForex()
    {
        var client = new Client();
        var portfolio = portfolio();
        var security = security();
        var transaction = new LedgerDeliveryTransactionCreator(client).create(portfolio,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, DATE_TIME, 100L, CurrencyUnit.EUR, security,
                        Values.Share.factorize(5), Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10"),
                        List.of(), "note", "source");
        var entry = ((LedgerBackedPortfolioTransaction) transaction).getLedgerEntry();

        new LedgerDeliveryTransactionCreator(client).update(transaction, portfolio,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, DATE_TIME.plusDays(1), 110L, CurrencyUnit.EUR,
                        security, Values.Share.factorize(6), null, null, List.of(), "changed", "changed-source");

        assertPrimaryForex(entry, LedgerProjectionRole.DELIVERY_INBOUND, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: account only facade preserves and clears primary forex and hidden units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountOnlyFacadePreservesAndClearsPrimaryForexAndHiddenUnits()
    {
        var client = new Client();
        var account = account();
        var security = security();
        var creator = new LedgerAccountOnlyTransactionCreator(client);
        var transaction = creator.create(account, name.abuchen.portfolio.model.AccountTransaction.Type.INTEREST,
                        DATE_TIME, 100L, CurrencyUnit.EUR, security,
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10")),
                        List.of(new name.abuchen.portfolio.model.Transaction.Unit(
                                        name.abuchen.portfolio.model.Transaction.Unit.Type.FEE, money(3),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(4)),
                                        new BigDecimal("0.7500"))),
                        "note", "source");
        var ledgerTransaction = (LedgerBackedAccountTransaction) transaction;
        var entry = ledgerTransaction.getLedgerEntry();
        var feeUUID = posting(entry, LedgerPostingType.FEE).getUUID();

        creator.update(transaction, account, name.abuchen.portfolio.model.AccountTransaction.Type.INTEREST,
                        DATE_TIME.plusDays(1), 101L, CurrencyUnit.EUR, security, null,
                        LedgerUnitPostingPatch.none(), "changed", "changed-source");

        assertPrimaryForex(entry, LedgerProjectionRole.ACCOUNT, CurrencyUnit.USD, 90L, new BigDecimal("1.10"));
        assertThat(posting(entry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(entry, LedgerPostingType.FEE).getForexAmount(), is(Values.Amount.factorize(4)));

        creator.update(transaction, account, name.abuchen.portfolio.model.AccountTransaction.Type.INTEREST,
                        DATE_TIME.plusDays(2), 102L, CurrencyUnit.EUR, security, LedgerForexAmount.none(),
                        LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.clearForex(feeUUID)), "changed",
                        "changed-source");

        assertNoPrimaryForex(entry, LedgerProjectionRole.ACCOUNT);
        assertNull(posting(entry, LedgerPostingType.FEE).getForexAmount());
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: deposit and removal facade creates units and preserves them on unrelated edit.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testDepositAndRemovalFacadeCreatesUnitsAndPreservesThemOnUnrelatedEdit()
    {
        var client = new Client();
        var account = account();
        var creator = new LedgerAccountOnlyTransactionCreator(client);
        var units = List.of(new name.abuchen.portfolio.model.Transaction.Unit(
                        name.abuchen.portfolio.model.Transaction.Unit.Type.GROSS_VALUE, money(20),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(18)), new BigDecimal("1.1111")),
                        new name.abuchen.portfolio.model.Transaction.Unit(
                                        name.abuchen.portfolio.model.Transaction.Unit.Type.FEE, money(2),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(3)),
                                        new BigDecimal("0.6667")),
                        new name.abuchen.portfolio.model.Transaction.Unit(
                                        name.abuchen.portfolio.model.Transaction.Unit.Type.TAX, money(4),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(5)),
                                        new BigDecimal("0.8000")));

        var deposit = creator.create(account, name.abuchen.portfolio.model.AccountTransaction.Type.DEPOSIT, DATE_TIME,
                        100L, CurrencyUnit.EUR, null, LedgerForexAmount.none(), units, "note", "source");
        var removal = creator.create(account, name.abuchen.portfolio.model.AccountTransaction.Type.REMOVAL, DATE_TIME,
                        101L, CurrencyUnit.EUR, null, LedgerForexAmount.none(), units, "note", "source");
        var depositEntry = ((LedgerBackedAccountTransaction) deposit).getLedgerEntry();
        var removalEntry = ((LedgerBackedAccountTransaction) removal).getLedgerEntry();
        var depositFeeUUID = posting(depositEntry, LedgerPostingType.FEE).getUUID();
        var removalTaxUUID = posting(removalEntry, LedgerPostingType.TAX).getUUID();

        assertThat(deposit.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.GROSS_VALUE).orElseThrow()
                        .getForex().getAmount(), is(Values.Amount.factorize(18)));
        assertThat(deposit.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.FEE).orElseThrow()
                        .getForex().getAmount(), is(Values.Amount.factorize(3)));
        assertThat(removal.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.TAX).orElseThrow()
                        .getForex().getAmount(), is(Values.Amount.factorize(5)));

        creator.update(deposit, account, name.abuchen.portfolio.model.AccountTransaction.Type.DEPOSIT,
                        DATE_TIME.plusDays(1), 102L, CurrencyUnit.EUR, null, null, LedgerUnitPostingPatch.none(),
                        "changed", "changed-source");
        creator.update(removal, account, name.abuchen.portfolio.model.AccountTransaction.Type.REMOVAL,
                        DATE_TIME.plusDays(1), 103L, CurrencyUnit.EUR, null, null, LedgerUnitPostingPatch.none(),
                        "changed", "changed-source");

        assertThat(posting(depositEntry, LedgerPostingType.FEE).getUUID(), is(depositFeeUUID));
        assertThat(posting(depositEntry, LedgerPostingType.FEE).getForexAmount(), is(Values.Amount.factorize(3)));
        assertThat(posting(removalEntry, LedgerPostingType.TAX).getUUID(), is(removalTaxUUID));
        assertThat(posting(removalEntry, LedgerPostingType.TAX).getForexAmount(), is(Values.Amount.factorize(5)));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: buy/sell facade carries primary forex and preserves hidden units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testBuySellFacadeCarriesPrimaryForexAndPreservesHiddenUnits()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        var security = security();
        var creator = new name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator(client);
        BuySellEntry entry = creator.create(portfolio, account, PortfolioTransaction.Type.BUY, DATE_TIME, 100L,
                        CurrencyUnit.EUR, security, Values.Share.factorize(5),
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10")),
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 91L), new BigDecimal("1.11")),
                        List.of(new name.abuchen.portfolio.model.Transaction.Unit(
                                        name.abuchen.portfolio.model.Transaction.Unit.Type.TAX, money(4),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(5)),
                                        new BigDecimal("0.8000"))),
                        "note", "source");
        var ledgerEntry = ((LedgerBackedAccountTransaction) entry.getAccountTransaction()).getLedgerEntry();
        var taxUUID = posting(ledgerEntry, LedgerPostingType.TAX).getUUID();

        creator.update(entry, portfolio, account, PortfolioTransaction.Type.BUY, DATE_TIME.plusDays(1), 101L,
                        CurrencyUnit.EUR, security, Values.Share.factorize(6), null, null,
                        LedgerUnitPostingPatch.none(), "changed", "changed-source");

        assertPrimaryForex(ledgerEntry, LedgerProjectionRole.ACCOUNT, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertPrimaryForex(ledgerEntry, LedgerProjectionRole.PORTFOLIO, CurrencyUnit.USD, 91L,
                        new BigDecimal("1.11"));
        assertThat(posting(ledgerEntry, LedgerPostingType.TAX).getUUID(), is(taxUUID));
        assertThat(posting(ledgerEntry, LedgerPostingType.TAX).getForexAmount(), is(Values.Amount.factorize(5)));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: account transfer facade carries target forex and hidden units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountTransferFacadeCarriesTargetForexAndHiddenUnits()
    {
        var client = new Client();
        var source = account();
        var target = account();
        var creator = new LedgerAccountTransferTransactionCreator(client);
        var transfer = creator.create(source, target, DATE_TIME, 100L, CurrencyUnit.EUR, 200L, CurrencyUnit.EUR,
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10")),
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 180L), new BigDecimal("1.20")),
                        LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.GROSS_VALUE, money(300),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 270L),
                                                        new BigDecimal("1.1111")))),
                        "note", "source");
        var ledgerEntry = ((LedgerBackedAccountTransaction) transfer.getSourceTransaction()).getLedgerEntry();
        var grossValueUUID = posting(ledgerEntry, LedgerPostingType.GROSS_VALUE).getUUID();

        creator.update(transfer, source, target, DATE_TIME.plusDays(1), 101L, CurrencyUnit.EUR, 201L,
                        CurrencyUnit.EUR, null, null, LedgerUnitPostingPatch.none(), "changed",
                        "changed-source");

        assertPrimaryForex(ledgerEntry, LedgerProjectionRole.SOURCE_ACCOUNT, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertPrimaryForex(ledgerEntry, LedgerProjectionRole.TARGET_ACCOUNT, CurrencyUnit.USD, 180L,
                        new BigDecimal("1.20"));
        assertThat(posting(ledgerEntry, LedgerPostingType.GROSS_VALUE).getUUID(), is(grossValueUUID));
        assertThat(posting(ledgerEntry, LedgerPostingType.GROSS_VALUE).getForexAmount(), is(270L));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: portfolio transfer facade carries primary forex and hidden units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testPortfolioTransferFacadeCarriesPrimaryForexAndHiddenUnits()
    {
        var client = new Client();
        var source = portfolio();
        var target = portfolio();
        var security = security();
        var creator = new name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator(
                        client);
        PortfolioTransferEntry transfer = creator.create(source, target, security, DATE_TIME,
                        Values.Share.factorize(5), 100L, CurrencyUnit.EUR,
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 90L), new BigDecimal("1.10")),
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 180L), new BigDecimal("1.20")),
                        LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(3),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, 4L),
                                                        new BigDecimal("0.7500")))),
                        "note", "source");
        var ledgerEntry = ((LedgerBackedPortfolioTransaction) transfer.getSourceTransaction()).getLedgerEntry();
        var feeUUID = posting(ledgerEntry, LedgerPostingType.FEE).getUUID();

        creator.update(transfer, source, target, security, DATE_TIME.plusDays(1), Values.Share.factorize(6), 101L,
                        CurrencyUnit.EUR, null, null, LedgerUnitPostingPatch.none(), "changed",
                        "changed-source");

        assertPrimaryForex(ledgerEntry, LedgerProjectionRole.SOURCE_PORTFOLIO, CurrencyUnit.USD, 90L,
                        new BigDecimal("1.10"));
        assertPrimaryForex(ledgerEntry, LedgerProjectionRole.TARGET_PORTFOLIO, CurrencyUnit.USD, 180L,
                        new BigDecimal("1.20"));
        assertThat(posting(ledgerEntry, LedgerPostingType.FEE).getUUID(), is(feeUUID));
        assertThat(posting(ledgerEntry, LedgerPostingType.FEE).getForexAmount(), is(4L));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: invalid edit does not partially mutate ledger truth.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidEditDoesNotPartiallyMutateLedgerTruth()
    {
        var client = new Client();
        var account = account();
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var originalAmount = entry.getPostings().get(0).getAmount();
        var originalCurrency = entry.getPostings().get(0).getCurrency();

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);

        assertThrows(IllegalArgumentException.class, () -> new LedgerAccountTransactionEditor().apply(transaction,
                        LedgerAccountTransactionEdit.builder().amount(-1L).currency(CurrencyUnit.USD).build()));

        assertThat(entry.getPostings().get(0).getAmount(), is(originalAmount));
        assertThat(entry.getPostings().get(0).getCurrency(), is(originalCurrency));
    }

    /**
     * Checks the ledger-backed editing scenario: invalid security posting edit does not partially mutate ledger truth.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidSecurityPostingEditDoesNotPartiallyMutateLedgerTruth()
    {
        var client = new Client();
        var portfolio = portfolio();
        var entry = creator(client).createInboundDelivery(metadata(), deliveryLeg(portfolio)).getEntry();
        var posting = posting(entry, LedgerPostingType.SECURITY);
        var originalSecurity = posting.getSecurity();
        var originalShares = posting.getShares();
        var replacementSecurity = security();

        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedPortfolioTransaction) portfolio.getTransactions().get(0);

        assertThrows(IllegalArgumentException.class, () -> new LedgerDeliveryTransactionEditor().apply(transaction,
                        LedgerDeliveryTransactionEdit.builder().security(replacementSecurity).shares(-1L).build()));

        assertSame(originalSecurity, posting(entry, LedgerPostingType.SECURITY).getSecurity());
        assertThat(posting(entry, LedgerPostingType.SECURITY).getShares(), is(originalShares));
    }

    /**
     * Checks the ledger-backed editing scenario: invalid ex-date parameter edit does not partially mutate ledger truth.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidExDateParameterEditDoesNotPartiallyMutateLedgerTruth()
    {
        var client = new Client();
        var account = account();
        var dividend = LedgerDividend.withExDate(cashLeg(account, 30), LedgerOptionalSecurity.of(security()),
                        LedgerCreationUnits.none(), DATE_TIME.minusDays(5));
        var entry = creator(client).createDividend(metadata(), dividend).getEntry();
        var postingUUID = entry.getPostings().get(0).getUUID();

        assertThrows(IllegalArgumentException.class, () -> LedgerEntryEditSupport.applyValidated(entry, editedEntry -> {
            var posting = LedgerEntryEditSupport.postingByUUID(editedEntry, postingUUID);

            posting.getParameters().stream() //
                            .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE) //
                            .toList().forEach(posting::removeParameter);
            posting.addParameter(LedgerParameter.unchecked(LedgerParameterType.EX_DATE,
                            LedgerParameter.ValueKind.STRING, "invalid"));
        }));

        assertThat(exDate(entry.getPostings().get(0)), is(DATE_TIME.minusDays(5)));
    }

    /**
     * Checks the ledger-backed editing scenario: invalid unit posting add update and remove do not partially mutate ledger truth.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidUnitPostingAddUpdateAndRemoveDoNotPartiallyMutateLedgerTruth()
    {
        var client = new Client();
        var account = account();
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var updater = new LedgerUnitPostingUpdater();
        var originalPostingCount = entry.getPostings().size();

        assertThrows(IllegalArgumentException.class, () -> updater.apply(entry,
                        LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(-1)))));

        assertThat(entry.getPostings().size(), is(originalPostingCount));

        updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(1))));

        var fee = posting(entry, LedgerPostingType.FEE);
        var feeUUID = fee.getUUID();
        var originalFeeAmount = fee.getAmount();
        var originalFeeCurrency = fee.getCurrency();

        assertThrows(IllegalArgumentException.class, () -> updater.apply(entry,
                        LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.update(feeUUID,
                                        Money.of(CurrencyUnit.USD, -1L)))));

        assertThat(posting(entry, LedgerPostingType.FEE).getAmount(), is(originalFeeAmount));
        assertThat(posting(entry, LedgerPostingType.FEE).getCurrency(), is(originalFeeCurrency));

        var cashPostingUUID = posting(entry, LedgerPostingType.CASH).getUUID();
        var postingCountBeforeInvalidRemove = entry.getPostings().size();

        assertThrows(IllegalArgumentException.class,
                        () -> updater.apply(entry, LedgerUnitPostingPatch.of(LedgerUnitPostingEdit.remove(cashPostingUUID))));

        assertThat(entry.getPostings().size(), is(postingCountBeforeInvalidRemove));
        assertThat(posting(entry, LedgerPostingType.CASH).getUUID(), is(cashPostingUUID));
    }

    /**
     * Checks the ledger-backed editing scenario: materialized projection reflects edited ledger truth.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testMaterializedProjectionReflectsEditedLedgerTruth()
    {
        var client = new Client();
        var account = account();

        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        LedgerProjectionService.materialize(client);

        var transaction = (LedgerBackedAccountTransaction) account.getTransactions().get(0);

        new LedgerAccountTransactionEditor().apply(transaction, LedgerAccountTransactionEdit.builder().amount(222L).build());

        assertThat(transaction.getAmount(), is(222L));
    }

    private LedgerTransactionCreator creator(Client client)
    {
        return new LedgerTransactionCreator(client);
    }

    private LedgerTransactionMetadata metadata()
    {
        return LedgerTransactionMetadata.of(DATE_TIME).withNote("note").withSource("source");
    }

    private Account account()
    {
        return new Account();
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

    private LedgerDeliveryLeg deliveryLeg(Portfolio portfolio)
    {
        return LedgerDeliveryLeg.of(portfolio, LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)),
                        money(100));
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

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
    }

    private LedgerPosting primaryPosting(LedgerEntry entry, LedgerProjectionRole role)
    {
        return LedgerProjectionSupport.primaryPosting(entry, projection(entry, role));
    }

    private void assertPrimaryForex(LedgerEntry entry, LedgerProjectionRole role, String currency, long amount,
                    BigDecimal exchangeRate)
    {
        var posting = primaryPosting(entry, role);

        assertThat(posting.getForexCurrency(), is(currency));
        assertThat(posting.getForexAmount(), is(amount));
        assertThat(posting.getExchangeRate(), is(exchangeRate));
    }

    private void assertNoPrimaryForex(LedgerEntry entry, LedgerProjectionRole role)
    {
        var posting = primaryPosting(entry, role);

        assertThat(posting.getForexCurrency(), is((String) null));
        assertThat(posting.getForexAmount(), is((Long) null));
        assertThat(posting.getExchangeRate(), is((BigDecimal) null));
    }

    private void assertOK(Client client)
    {
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    private LocalDateTime exDate(LedgerPosting posting)
    {
        return posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE) //
                        .map(LedgerParameter::getValue) //
                        .filter(LocalDateTime.class::isInstance) //
                        .map(LocalDateTime.class::cast) //
                        .findFirst().orElseThrow();
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

}
