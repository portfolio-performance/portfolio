package name.abuchen.portfolio.ui.views.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.viewers.TransactionTypeEditingSupport;

/**
 * Tests that legacy account-transfer actions stay guarded for ledger-backed rows.
 * These tests make sure unsupported UI actions do not mutate ledger-backed projections through legacy paths.
 */
@SuppressWarnings("nls")
public class AccountTransferLegacyActionGuardTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);

    /**
     * Checks the guardrail scenario: convert transfer to deposit removal splits ledger backed transfer through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertTransferToDepositRemovalSplitsLedgerBackedTransferThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerFixture();
        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);
        var sourceUUID = sourceTransaction.getUUID();
        var targetUUID = targetTransaction.getUUID();

        new ConvertTransferToDepositRemovalAction(fixture.client(), List.of(sourceTransaction)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(2));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(sourceUUID));
        assertThat(fixture.target().getTransactions().get(0).getUUID(), is(targetUUID));
        assertThat(fixture.source().getTransactions().get(0).getCrossEntry(), is(nullValue()));
        assertThat(fixture.target().getTransactions().get(0).getCrossEntry(), is(nullValue()));
    }

    /**
     * Checks the guardrail scenario: convert transfer to deposit removal deduplicates ledger backed transfer selection.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertTransferToDepositRemovalDeduplicatesLedgerBackedTransferSelection()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerFixture();
        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);

        new ConvertTransferToDepositRemovalAction(fixture.client(), List.of(sourceTransaction, targetTransaction))
                        .run();

        assertThat(ledgerEntryCount(fixture.client()), is(2));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
    }

    /**
     * Checks the guardrail scenario: convert transfer to deposit removal still converts legacy transfer.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertTransferToDepositRemovalStillConvertsLegacyTransfer()
    {
        var fixture = legacyFixture();
        var sourceTransaction = fixture.transfer().getSourceTransaction();

        new ConvertTransferToDepositRemovalAction(fixture.client(), List.of(sourceTransaction)).run();

        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(fixture.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
        assertThat(fixture.target().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: convert transfer to deposit removal rejects non-transfer rows with a UI code.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertTransferToDepositRemovalRejectsUnsupportedRowsWithDiagnostic()
    {
        var fixture = legacyFixture();
        var transaction = new AccountTransaction(AccountTransaction.Type.DEPOSIT);

        transaction.setDateTime(DATE_TIME);
        transaction.setAmount(Values.Amount.factorize(123));
        transaction.setCurrencyCode(CurrencyUnit.EUR);

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> new ConvertTransferToDepositRemovalAction(fixture.client(), List.of(transaction)).run());

        assertThat(exception.getMessage(), containsString(LedgerDiagnosticCode.LEDGER_UI_018.prefix()));
    }

    /**
     * Checks the guardrail scenario: revert transfer action reverses ledger backed transfer through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertTransferActionReversesLedgerBackedTransferThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerFixture();
        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);
        var sourceUUID = sourceTransaction.getUUID();
        var targetUUID = targetTransaction.getUUID();

        new RevertTransferAction(fixture.client(), new TransactionPair<>(fixture.source(), sourceTransaction)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(sourceUUID));
        assertThat(fixture.target().getTransactions().get(0).getUUID(), is(targetUUID));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(fixture.source().getTransactions().get(0), fixture.target().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.target().getTransactions().get(0)));
    }

    /**
     * Checks the guardrail scenario: revert transfer action still reverts legacy transfer.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertTransferActionStillRevertsLegacyTransfer()
    {
        var fixture = legacyFixture();
        var sourceTransaction = fixture.transfer().getSourceTransaction();
        var targetTransaction = fixture.transfer().getTargetTransaction();

        new RevertTransferAction(fixture.client(), new TransactionPair<>(fixture.source(), sourceTransaction)).run();

        assertSame(fixture.target(), fixture.transfer().getSourceAccount());
        assertSame(fixture.source(), fixture.transfer().getTargetAccount());
        assertSame(targetTransaction, fixture.transfer().getSourceTransaction());
        assertSame(sourceTransaction, fixture.transfer().getTargetTransaction());
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support offers ledger backed transfer reversal.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportOffersLedgerBackedTransferReversal() throws Exception
    {
        var fixture = ledgerFixture();
        var sourceTransaction = fixture.source().getTransactions().get(0);
        var support = new TransactionTypeEditingSupport(fixture.client());

        assertThat(support.canEdit(sourceTransaction), is(true));

        setTypeValue(support, sourceTransaction, AccountTransaction.Type.TRANSFER_OUT,
                        AccountTransaction.Type.TRANSFER_IN);

        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support still offers legacy transfer reversal.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportStillOffersLegacyTransferReversal()
    {
        var fixture = legacyFixture();
        var sourceTransaction = fixture.transfer().getSourceTransaction();
        var support = new TransactionTypeEditingSupport(fixture.client());

        assertThat(support.canEdit(sourceTransaction), is(true));
    }

    /**
     * Checks the guardrail scenario: revert transfer action reverses ledger backed portfolio transfer through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertTransferActionReversesLedgerBackedPortfolioTransferThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerPortfolioFixture();
        var sourceTransaction = fixture.source().getTransactions().get(0);
        var targetTransaction = fixture.target().getTransactions().get(0);
        var sourceUUID = sourceTransaction.getUUID();
        var targetUUID = targetTransaction.getUUID();

        new RevertTransferAction(fixture.client(), new TransactionPair<>(fixture.source(), sourceTransaction)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(sourceUUID));
        assertThat(fixture.target().getTransactions().get(0).getUUID(), is(targetUUID));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertSame(fixture.source().getTransactions().get(0), fixture.target().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.target().getTransactions().get(0)));
    }

    /**
     * Checks the guardrail scenario: revert transfer action still reverts legacy portfolio transfer.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertTransferActionStillRevertsLegacyPortfolioTransfer()
    {
        var fixture = legacyPortfolioFixture();
        var sourceTransaction = fixture.transfer().getSourceTransaction();
        var targetTransaction = fixture.transfer().getTargetTransaction();

        new RevertTransferAction(fixture.client(), new TransactionPair<>(fixture.source(), sourceTransaction)).run();

        assertSame(fixture.target(), fixture.transfer().getSourcePortfolio());
        assertSame(fixture.source(), fixture.transfer().getTargetPortfolio());
        assertSame(targetTransaction, fixture.transfer().getSourceTransaction());
        assertSame(sourceTransaction, fixture.transfer().getTargetTransaction());
        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support does not offer ledger backed portfolio transfer type edit.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportDoesNotOfferLedgerBackedPortfolioTransferTypeEdit()
    {
        var fixture = ledgerPortfolioFixture();
        var sourceTransaction = fixture.source().getTransactions().get(0);
        var support = new TransactionTypeEditingSupport(fixture.client());

        assertThat(support.canEdit(sourceTransaction), is(false));
    }

    /**
     * Checks the guardrail scenario: revert buy/sell action reverses ledger backed buy/sell through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertBuySellActionReversesLedgerBackedBuySellThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var accountTransaction = fixture.account().getTransactions().get(0);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var accountUUID = accountTransaction.getUUID();
        var portfolioUUID = portfolioTransaction.getUUID();

        new RevertBuySellAction(fixture.client(), new TransactionPair<>(fixture.portfolio(), portfolioTransaction))
                        .run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.account().getTransactions().get(0).getType(), is(AccountTransaction.Type.SELL));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(fixture.account().getTransactions().get(0).getUUID(), is(accountUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertSame(fixture.portfolio().getTransactions().get(0), fixture.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.account().getTransactions().get(0)));
    }

    /**
     * Checks the guardrail scenario: revert buy/sell action still reverts legacy buy/sell.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertBuySellActionStillRevertsLegacyBuySell()
    {
        var fixture = legacyBuySellFixture(PortfolioTransaction.Type.BUY);
        var portfolioTransaction = fixture.entry().getPortfolioTransaction();

        new RevertBuySellAction(fixture.client(), new TransactionPair<>(fixture.portfolio(), portfolioTransaction))
                        .run();

        assertThat(fixture.entry().getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(portfolioTransaction.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: convert buy/sell to delivery converts ledger backed buy/sell through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertBuySellToDeliveryConvertsLedgerBackedBuySellThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerBuySellFixture(PortfolioTransaction.Type.SELL);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var portfolioProjectionUUID = portfolioTransaction.getUUID();

        new ConvertBuySellToDeliveryAction(fixture.client(),
                        new TransactionPair<>(fixture.portfolio(), portfolioTransaction)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioProjectionUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(fixture.portfolio().getTransactions().get(0).getCrossEntry(), is(nullValue()));
    }

    /**
     * Checks the guardrail scenario: convert buy/sell to delivery still converts legacy buy/sell.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertBuySellToDeliveryStillConvertsLegacyBuySell()
    {
        var fixture = legacyBuySellFixture(PortfolioTransaction.Type.BUY);
        var portfolioTransaction = fixture.entry().getPortfolioTransaction();

        new ConvertBuySellToDeliveryAction(fixture.client(),
                        new TransactionPair<>(fixture.portfolio(), portfolioTransaction)).run();

        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
    }

    /**
     * Checks the guardrail scenario: convert delivery to buy/sell converts ledger backed delivery through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testConvertDeliveryToBuySellConvertsLedgerBackedDeliveryThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerDeliveryFixture();
        var delivery = fixture.portfolio().getTransactions().get(0);
        var portfolioProjectionUUID = delivery.getUUID();

        new ConvertDeliveryToBuySellAction(fixture.client(), new TransactionPair<>(fixture.portfolio(), delivery))
                        .run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioProjectionUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(fixture.account().getTransactions().get(0).getType(), is(AccountTransaction.Type.BUY));
        assertSame(fixture.portfolio().getTransactions().get(0), fixture.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.account().getTransactions().get(0)));
    }

    /**
     * Checks the guardrail scenario: revert delivery action reverses ledger backed delivery through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertDeliveryActionReversesLedgerBackedDeliveryThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerDeliveryFixture();
        var delivery = fixture.portfolio().getTransactions().get(0);
        var projectionUUID = delivery.getUUID();

        new RevertDeliveryAction(fixture.client(), new TransactionPair<>(fixture.portfolio(), delivery)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(fixture.portfolio().getTransactions().get(0).getCrossEntry(), is(nullValue()));
        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
    }

    /**
     * Checks the guardrail scenario: revert delivery action still reverts legacy delivery.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertDeliveryActionStillRevertsLegacyDelivery()
    {
        var fixture = legacyDeliveryFixture();
        var delivery = fixture.portfolio().getTransactions().get(0);

        new RevertDeliveryAction(fixture.client(), new TransactionPair<>(fixture.portfolio(), delivery)).run();

        assertThat(delivery.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(delivery.getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: revert deposit removal action reverses ledger backed account only through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertDepositRemovalActionReversesLedgerBackedAccountOnlyThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        var transaction = fixture.source().getTransactions().get(0);
        var projectionUUID = transaction.getUUID();

        new RevertDepositRemovalAction(fixture.client(), new TransactionPair<>(fixture.source(), transaction)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(fixture.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: revert deposit removal action still reverts legacy account only.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertDepositRemovalActionStillRevertsLegacyAccountOnly()
    {
        var fixture = legacyAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        var transaction = fixture.source().getTransactions().get(0);

        new RevertDepositRemovalAction(fixture.client(), new TransactionPair<>(fixture.source(), transaction)).run();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: revert interest action reverses ledger backed account only through ledger truth.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertInterestActionReversesLedgerBackedAccountOnlyThroughLedgerTruth()
                    throws ReflectiveOperationException
    {
        var fixture = ledgerAccountOnlyFixture(AccountTransaction.Type.INTEREST);
        var transaction = fixture.source().getTransactions().get(0);
        var projectionUUID = transaction.getUUID();

        new RevertInterestAction(fixture.client(), new TransactionPair<>(fixture.source(), transaction)).run();

        assertThat(ledgerEntryCount(fixture.client()), is(1));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(fixture.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: revert interest action still reverts legacy account only.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testRevertInterestActionStillRevertsLegacyAccountOnly()
    {
        var fixture = legacyAccountOnlyFixture(AccountTransaction.Type.INTEREST);
        var transaction = fixture.source().getTransactions().get(0);

        new RevertInterestAction(fixture.client(), new TransactionPair<>(fixture.source(), transaction)).run();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support offers converter backed buy/sell delivery type edits.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportOffersConverterBackedBuySellDeliveryTypeEdits() throws Exception
    {
        var buySell = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var delivery = ledgerDeliveryFixture();
        var support = new TransactionTypeEditingSupport(buySell.client());

        var portfolioTransaction = buySell.portfolio().getTransactions().get(0);
        assertThat(support.canEdit(portfolioTransaction), is(true));

        setTypeValue(support, portfolioTransaction, PortfolioTransaction.Type.BUY,
                        PortfolioTransaction.Type.DELIVERY_INBOUND);

        assertThat(buySell.portfolio().getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(buySell.account().getTransactions().isEmpty(), is(true));

        var deliverySupport = new TransactionTypeEditingSupport(delivery.client());
        var deliveryTransaction = delivery.portfolio().getTransactions().get(0);
        assertThat(deliverySupport.canEdit(deliveryTransaction), is(true));

        setTypeValue(deliverySupport, deliveryTransaction, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        PortfolioTransaction.Type.BUY);

        assertThat(delivery.portfolio().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(delivery.account().getTransactions().get(0).getType(), is(AccountTransaction.Type.BUY));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support routes master security type edits through converters.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportRoutesMasterSecurityTypeEditsThroughConverters()
                    throws Exception
    {
        assertInlineBuySellToDelivery(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertInlineBuySellToDelivery(PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_OUTBOUND);

        assertInlineDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.BUY,
                        AccountTransaction.Type.BUY);
        assertInlineDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.SELL,
                        AccountTransaction.Type.SELL);
    }

    /**
     * Checks the guardrail scenario: transaction type editing support offers ledger backed account only type toggles.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportOffersLedgerBackedAccountOnlyTypeToggles() throws Exception
    {
        var deposit = ledgerAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        var depositSupport = new TransactionTypeEditingSupport(deposit.client());
        var depositTransaction = deposit.source().getTransactions().get(0);

        assertThat(depositSupport.canEdit(depositTransaction), is(true));

        setTypeValue(depositSupport, depositTransaction, AccountTransaction.Type.DEPOSIT,
                        AccountTransaction.Type.REMOVAL);

        assertThat(deposit.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(deposit.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));

        var interest = ledgerAccountOnlyFixture(AccountTransaction.Type.INTEREST);
        var interestSupport = new TransactionTypeEditingSupport(interest.client());
        var interestTransaction = interest.source().getTransactions().get(0);

        assertThat(interestSupport.canEdit(interestTransaction), is(true));

        setTypeValue(interestSupport, interestTransaction, AccountTransaction.Type.INTEREST,
                        AccountTransaction.Type.INTEREST_CHARGE);

        assertThat(interest.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(interest.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support routes all ledger backed account type edits through converters.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportRoutesAllLedgerBackedAccountTypeEditsThroughConverters()
                    throws Exception
    {
        assertInlineAccountTransferReversal(AccountTransaction.Type.TRANSFER_OUT,
                        AccountTransaction.Type.TRANSFER_IN);
        assertInlineAccountTransferReversal(AccountTransaction.Type.TRANSFER_IN,
                        AccountTransaction.Type.TRANSFER_OUT);

        assertInlineAccountOnlyToggle(AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL);
        assertInlineAccountOnlyToggle(AccountTransaction.Type.REMOVAL, AccountTransaction.Type.DEPOSIT);
        assertInlineAccountOnlyToggle(AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE);
        assertInlineAccountOnlyToggle(AccountTransaction.Type.INTEREST_CHARGE, AccountTransaction.Type.INTEREST);
    }

    /**
     * Checks the guardrail scenario: transaction type editing support rejects unsupported ledger backed type edits.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportRejectsUnsupportedLedgerBackedTypeEdits() throws Exception
    {
        var delivery = ledgerDeliveryFixture();
        var support = new TransactionTypeEditingSupport(delivery.client());
        var transaction = delivery.portfolio().getTransactions().get(0);

        assertThat(support.canEdit(transaction), is(true));

        setTypeItems(support, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        PortfolioTransaction.Type.TRANSFER_IN);

        assertThrows(UnsupportedOperationException.class, () -> support.setValue(transaction, 1));

        assertThat(delivery.portfolio().getTransactions().get(0), is(transaction));
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(delivery.account().getTransactions().isEmpty(), is(true));
    }

    /**
     * Checks the guardrail scenario: transaction type editing support blocks ledger backed fee and tax refund toggles.
     * Runtime projections are views only and must not become persisted truth.
     * This protects Ledger-V6 from duplicate or partial transaction state.
     */
    @Test
    public void testTransactionTypeEditingSupportBlocksLedgerBackedFeeTaxRefundToggles()
    {
        var fees = ledgerAccountOnlyFixture(AccountTransaction.Type.FEES);
        assertThat(new TransactionTypeEditingSupport(fees.client()).canEdit(fees.source().getTransactions().get(0)),
                        is(false));

        var feesRefund = ledgerAccountOnlyFixture(AccountTransaction.Type.FEES_REFUND);
        assertThat(new TransactionTypeEditingSupport(feesRefund.client())
                        .canEdit(feesRefund.source().getTransactions().get(0)), is(false));

        var taxes = ledgerAccountOnlyFixture(AccountTransaction.Type.TAXES);
        assertThat(new TransactionTypeEditingSupport(taxes.client()).canEdit(taxes.source().getTransactions().get(0)),
                        is(false));

        var taxRefund = ledgerAccountOnlyFixture(AccountTransaction.Type.TAX_REFUND);
        assertThat(new TransactionTypeEditingSupport(taxRefund.client())
                        .canEdit(taxRefund.source().getTransactions().get(0)), is(false));
    }

    private Fixture ledgerFixture()
    {
        var fixture = baseFixture();

        new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, Values.Amount.factorize(123),
                        CurrencyUnit.EUR, null, null, "note", "source");

        return fixture;
    }

    private Fixture ledgerAccountOnlyFixture(AccountTransaction.Type type)
    {
        var fixture = baseFixture();

        new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.source(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, List.of(), "note", "source");

        return fixture;
    }

    private Fixture legacyAccountOnlyFixture(AccountTransaction.Type type)
    {
        var fixture = baseFixture();
        var transaction = new AccountTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setAmount(Values.Amount.factorize(123));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setNote("note");
        fixture.source().addTransaction(transaction);

        return fixture;
    }

    private PortfolioFixture ledgerPortfolioFixture()
    {
        var fixture = basePortfolioFixture();

        new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        fixture.security(), DATE_TIME, Values.Share.factorize(5), Values.Amount.factorize(123),
                        CurrencyUnit.EUR, "note", "source");

        return fixture;
    }

    private LegacyFixture legacyFixture()
    {
        var fixture = baseFixture();
        var transfer = new AccountTransferEntry(fixture.source(), fixture.target());

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(123));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("note");
        transfer.insert();

        return new LegacyFixture(fixture.client(), fixture.source(), fixture.target(), transfer);
    }

    private LegacyPortfolioFixture legacyPortfolioFixture()
    {
        var fixture = basePortfolioFixture();
        var transfer = new PortfolioTransferEntry(fixture.source(), fixture.target());

        transfer.setDate(DATE_TIME);
        transfer.setSecurity(fixture.security());
        transfer.setShares(Values.Share.factorize(5));
        transfer.setAmount(Values.Amount.factorize(123));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("note");
        transfer.insert();

        return new LegacyPortfolioFixture(fixture.client(), fixture.source(), fixture.target(), fixture.security(),
                        transfer);
    }

    private BuySellFixture ledgerBuySellFixture(PortfolioTransaction.Type type)
    {
        var fixture = buySellBaseFixture();

        new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(), type,
                        DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), List.of(), "note", "source");

        return fixture;
    }

    private BuySellFixture ledgerDeliveryFixture()
    {
        return ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
    }

    private BuySellFixture ledgerDeliveryFixture(PortfolioTransaction.Type type)
    {
        var fixture = buySellBaseFixture();

        new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(),
                        type, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), null, null, List.of(), "note", "source");

        return fixture;
    }

    private BuySellFixture legacyDeliveryFixture()
    {
        var fixture = buySellBaseFixture();
        var delivery = new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND);

        delivery.setDateTime(DATE_TIME);
        delivery.setSecurity(fixture.security());
        delivery.setShares(Values.Share.factorize(5));
        delivery.setAmount(Values.Amount.factorize(123));
        delivery.setCurrencyCode(CurrencyUnit.EUR);
        delivery.setNote("note");
        fixture.portfolio().addTransaction(delivery);

        return fixture;
    }

    private LegacyBuySellFixture legacyBuySellFixture(PortfolioTransaction.Type type)
    {
        var fixture = buySellBaseFixture();
        var entry = new BuySellEntry(fixture.portfolio(), fixture.account());

        entry.setType(type);
        entry.setDate(DATE_TIME);
        entry.setSecurity(fixture.security());
        entry.setShares(Values.Share.factorize(5));
        entry.setAmount(Values.Amount.factorize(123));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setNote("note");
        entry.insert();

        return new LegacyBuySellFixture(fixture.client(), fixture.account(), fixture.portfolio(), fixture.security(),
                        entry);
    }

    private BuySellFixture buySellBaseFixture()
    {
        var client = new Client();
        var account = account("Account", CurrencyUnit.EUR);
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);

        portfolio.setReferenceAccount(account);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new BuySellFixture(client, account, portfolio, security);
    }

    private Fixture baseFixture()
    {
        var client = new Client();
        var source = account("Source", CurrencyUnit.EUR);
        var target = account("Target", CurrencyUnit.EUR);

        client.addAccount(source);
        client.addAccount(target);

        return new Fixture(client, source, target);
    }

    private PortfolioFixture basePortfolioFixture()
    {
        var client = new Client();
        var source = new Portfolio("Source");
        var target = new Portfolio("Target");
        var security = new Security("Security", CurrencyUnit.EUR);

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

    private int ledgerEntryCount(Client client) throws ReflectiveOperationException
    {
        try
        {
            var ledger = Client.class.getMethod("getLedger").invoke(client);
            var entries = ledger.getClass().getMethod("getEntries").invoke(ledger);
            return ((java.util.List<?>) entries).size();
        }
        catch (InvocationTargetException e)
        {
            throw new AssertionError(e.getCause());
        }
    }

    private void assertInlineBuySellToDelivery(PortfolioTransaction.Type from, PortfolioTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerBuySellFixture(from);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var portfolioUUID = portfolioTransaction.getUUID();

        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), portfolioTransaction, from, to);

        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.portfolio().getTransactions().get(0).getCrossEntry(), is(nullValue()));
    }

    private void assertInlineDeliveryToBuySell(PortfolioTransaction.Type from, PortfolioTransaction.Type to,
                    AccountTransaction.Type accountType) throws Exception
    {
        var fixture = ledgerDeliveryFixture(from);
        var delivery = fixture.portfolio().getTransactions().get(0);
        var portfolioUUID = delivery.getUUID();

        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), delivery, from, to);

        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.account().getTransactions().get(0).getType(), is(accountType));
        assertSame(fixture.portfolio().getTransactions().get(0), fixture.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.account().getTransactions().get(0)));
    }

    private void assertInlineAccountTransferReversal(AccountTransaction.Type from, AccountTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerFixture();
        AccountTransaction transaction = from == AccountTransaction.Type.TRANSFER_OUT
                        ? fixture.source().getTransactions().get(0)
                        : fixture.target().getTransactions().get(0);

        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), transaction, from, to);

        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(fixture.source().getTransactions().get(0), fixture.target().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.target().getTransactions().get(0)));
    }

    private void assertInlineAccountOnlyToggle(AccountTransaction.Type from, AccountTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerAccountOnlyFixture(from);
        var transaction = fixture.source().getTransactions().get(0);
        var projectionUUID = transaction.getUUID();

        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), transaction, from, to);

        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
    }

    private void setTypeValue(TransactionTypeEditingSupport support, Object element, Object current, Object target)
                    throws Exception
    {
        setTypeItems(support, current, target);
        support.setValue(element, 1);
    }

    private void setTypeItems(TransactionTypeEditingSupport support, Object current, Object target) throws Exception
    {
        Field field = TransactionTypeEditingSupport.class.getDeclaredField("comboBoxItems");
        field.setAccessible(true);
        field.set(support, new ArrayList<>(List.of(current, target)));
    }

    private record Fixture(Client client, Account source, Account target)
    {
    }

    private record LegacyFixture(Client client, Account source, Account target, AccountTransferEntry transfer)
    {
    }

    private record PortfolioFixture(Client client, Portfolio source, Portfolio target, Security security)
    {
    }

    private record LegacyPortfolioFixture(Client client, Portfolio source, Portfolio target, Security security,
                    PortfolioTransferEntry transfer)
    {
    }

    private record BuySellFixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record LegacyBuySellFixture(Client client, Account account, Portfolio portfolio, Security security,
                    BuySellEntry entry)
    {
    }
}
