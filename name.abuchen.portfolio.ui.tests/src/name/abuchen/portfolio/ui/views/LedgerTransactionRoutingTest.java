package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.junit.Test;

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
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingField;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.util.viewers.DateTimeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ExDateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionTypeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;

/**
 * Tests UI routing for ledger-backed dialogs, actions, inline editing, and owner changes.
 * These tests make sure restored UI paths use ledger-aware editors while unsupported structural edits stay blocked.
 */
@SuppressWarnings("nls")
public class LedgerTransactionRoutingTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final LocalDateTime UPDATED_DATE_TIME = LocalDateTime.of(2026, 6, 8, 10, 11);
    private static final Instant FIXTURE_UPDATED_AT = Instant.parse("2026-06-07T08:09:00Z");

    /**
     * Verifies that account-transaction dialogs route ledger-backed rows through ledger-aware models.
     * Supported account bookings must stay editable without writing directly to runtime projections.
     */
    @Test
    public void testSupportedLedgerBackedAccountTransactionDialogRoutes() throws Exception
    {
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.DEPOSIT);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.REMOVAL);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.INTEREST);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.INTEREST_CHARGE);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.FEES);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.FEES_REFUND);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.TAXES);
        assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type.TAX_REFUND);
        assertLedgerDividendDialogRoute();
    }

    /**
     * Verifies that account-side rows of ledger-backed cross entries open the supported edit routes.
     * The UI must use the same ledger-safe dialog paths as normal actions.
     */
    @Test
    public void testSupportedLedgerBackedAccountSideCrossEntryDialogRoutes() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        assertSupportedAccountEditRoute(buy.client(), buy.account(), buy.account().getTransactions().get(0),
                        SecurityTransactionDialog.class, PortfolioTransaction.Type.BUY);

        var sell = ledgerBuySellFixture(PortfolioTransaction.Type.SELL);
        assertSupportedAccountEditRoute(sell.client(), sell.account(), sell.account().getTransactions().get(0),
                        SecurityTransactionDialog.class, PortfolioTransaction.Type.SELL);

        var transfer = ledgerAccountTransferFixture();
        assertSupportedAccountEditRoute(transfer.client(), transfer.source(), transfer.source().getTransactions().get(0),
                        AccountTransferDialog.class);
        assertSupportedAccountEditRoute(transfer.client(), transfer.target(), transfer.target().getTransactions().get(0),
                        AccountTransferDialog.class);
    }

    /**
     * Verifies that portfolio-side ledger-backed rows open only the supported transaction dialogs.
     * Unsupported portfolio shapes must not fall back to legacy setter mutation.
     */
    @Test
    public void testSupportedLedgerBackedPortfolioTransactionDialogRoutes() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        assertSupportedPortfolioEditRoute(buy.client(), buy.portfolio(), buy.portfolio().getTransactions().get(0),
                        SecurityTransactionDialog.class, PortfolioTransaction.Type.BUY);

        var sell = ledgerBuySellFixture(PortfolioTransaction.Type.SELL);
        assertSupportedPortfolioEditRoute(sell.client(), sell.portfolio(), sell.portfolio().getTransactions().get(0),
                        SecurityTransactionDialog.class, PortfolioTransaction.Type.SELL);

        var inbound = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertSupportedPortfolioEditRoute(inbound.client(), inbound.portfolio(),
                        inbound.portfolio().getTransactions().get(0), SecurityTransactionDialog.class,
                        PortfolioTransaction.Type.DELIVERY_INBOUND);

        var outbound = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        assertSupportedPortfolioEditRoute(outbound.client(), outbound.portfolio(),
                        outbound.portfolio().getTransactions().get(0), SecurityTransactionDialog.class,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND);

        var transfer = ledgerPortfolioTransferFixture();
        assertSupportedPortfolioEditRoute(transfer.client(), transfer.source(),
                        transfer.source().getTransactions().get(0), SecurityTransferDialog.class);
        assertSupportedPortfolioEditRoute(transfer.client(), transfer.target(),
                        transfer.target().getTransactions().get(0), SecurityTransferDialog.class);
    }

    /**
     * Verifies that context-menu conversion actions are offered for supported ledger-backed rows.
     * The action availability must match the ledger converters that can preserve booking truth.
     */
    @Test
    public void testSupportedLedgerBackedContextMenuConversionActions()
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        assertSupportedBuySellToDeliveryContextMenuRoute(buy.portfolio(), buy.portfolio().getTransactions().get(0));

        var sell = ledgerBuySellFixture(PortfolioTransaction.Type.SELL);
        assertSupportedBuySellToDeliveryContextMenuRoute(sell.portfolio(), sell.portfolio().getTransactions().get(0));

        var inbound = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertSupportedDeliveryToBuySellContextMenuRoute(inbound.portfolio(),
                        inbound.portfolio().getTransactions().get(0));

        var outbound = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        assertSupportedDeliveryToBuySellContextMenuRoute(outbound.portfolio(),
                        outbound.portfolio().getTransactions().get(0));
    }

    /**
     * Verifies that supported context-menu conversions execute through ledger converters.
     * The converted booking must keep a consistent ledger entry, projections, and owner lists.
     */
    @Test
    public void testSupportedLedgerBackedContextMenuConversionActionsExecute() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var buyPortfolioTransaction = buy.portfolio().getTransactions().get(0);
        var buyProjectionUUID = buyPortfolioTransaction.getUUID();

        new ConvertBuySellToDeliveryAction(buy.client(), new TransactionPair<>(buy.portfolio(), buyPortfolioTransaction))
                        .run();

        assertThat(buy.account().getTransactions().isEmpty(), is(true));
        assertThat(buy.portfolio().getTransactions().size(), is(1));
        assertThat(buy.portfolio().getTransactions().get(0).getUUID(), is(buyProjectionUUID));
        assertThat(buy.portfolio().getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(buy.portfolio().getTransactions().get(0).getCrossEntry(), is(nullValue()));
        assertLedgerStructurallyValid(buy.client());

        var inbound = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        var inboundPortfolioTransaction = inbound.portfolio().getTransactions().get(0);
        var inboundProjectionUUID = inboundPortfolioTransaction.getUUID();

        new ConvertDeliveryToBuySellAction(inbound.client(),
                        new TransactionPair<>(inbound.portfolio(), inboundPortfolioTransaction)).run();

        assertThat(inbound.account().getTransactions().size(), is(1));
        assertThat(inbound.portfolio().getTransactions().size(), is(1));
        assertThat(inbound.portfolio().getTransactions().get(0).getUUID(), is(inboundProjectionUUID));
        assertThat(inbound.portfolio().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(inbound.account().getTransactions().get(0).getType(), is(AccountTransaction.Type.BUY));
        assertSame(inbound.portfolio().getTransactions().get(0), inbound.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(inbound.account().getTransactions().get(0)));
        assertLedgerStructurallyValid(inbound.client());
    }

    /**
     * Verifies that unsupported ledger-backed action routes stay blocked in the UI.
     * Actions without a safe ledger path must not become legacy repairs or projection writes.
     */
    @Test
    public void testForbiddenLedgerBackedActionRoutes()
    {
        var transfer = ledgerPortfolioTransferFixture();
        var transferPair = new TransactionPair<>(transfer.source(), transfer.source().getTransactions().get(0));

        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(List.of(transferPair)), is(false));
        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(List.of(transferPair)), is(false));

        var buySell = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var buyPair = new TransactionPair<>(buySell.portfolio(), buySell.portfolio().getTransactions().get(0));
        var delivery = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        var deliveryPair = new TransactionPair<>(delivery.portfolio(), delivery.portfolio().getTransactions().get(0));

        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(List.of(buyPair, deliveryPair)), is(false));
        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(List.of(buyPair, deliveryPair)), is(false));
    }

    /**
     * Verifies that legacy transaction dialog routing is unchanged by the ledger guards.
     * Non-ledger rows must keep their existing UI behavior.
     */
    @Test
    public void testLegacyTransactionDialogRoutesRemainUnchanged() throws Exception
    {
        var accountOnly = legacyAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        assertSupportedAccountEditRoute(accountOnly.client(), accountOnly.source(),
                        accountOnly.source().getTransactions().get(0), AccountTransactionDialog.class,
                        AccountTransaction.Type.DEPOSIT);

        var buySell = legacyBuySellFixture(PortfolioTransaction.Type.BUY);
        assertSupportedAccountEditRoute(buySell.client(), buySell.account(), buySell.entry().getAccountTransaction(),
                        SecurityTransactionDialog.class, PortfolioTransaction.Type.BUY);
        assertSupportedPortfolioEditRoute(buySell.client(), buySell.portfolio(), buySell.entry().getPortfolioTransaction(),
                        SecurityTransactionDialog.class, PortfolioTransaction.Type.BUY);

        var accountTransfer = legacyAccountTransferFixture();
        assertSupportedAccountEditRoute(accountTransfer.client(), accountTransfer.source(),
                        accountTransfer.transfer().getSourceTransaction(), AccountTransferDialog.class);

        var delivery = legacyDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertSupportedPortfolioEditRoute(delivery.client(), delivery.portfolio(),
                        delivery.portfolio().getTransactions().get(0), SecurityTransactionDialog.class,
                        PortfolioTransaction.Type.DELIVERY_INBOUND);

        var portfolioTransfer = legacyPortfolioTransferFixture();
        assertSupportedPortfolioEditRoute(portfolioTransfer.client(), portfolioTransfer.source(),
                        portfolioTransfer.transfer().getSourceTransaction(), SecurityTransferDialog.class);
    }

    /**
     * Verifies that inline type editing offers the supported portfolio master transitions.
     * Buy/sell and delivery changes must route through the ledger converters.
     */
    @Test
    public void testSupportedLedgerBackedPortfolioInlineTypeConversions() throws Exception
    {
        assertInlinePortfolioBuySellReversal(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.SELL,
                        AccountTransaction.Type.SELL);
        assertInlinePortfolioBuySellReversal(PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.BUY,
                        AccountTransaction.Type.BUY);

        assertInlineBuySellToDelivery(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertInlineBuySellToDelivery(PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        assertInlineBuySellToDelivery(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        assertInlineBuySellToDelivery(PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.DELIVERY_INBOUND);

        assertInlineDeliveryReversal(PortfolioTransaction.Type.DELIVERY_INBOUND,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        assertInlineDeliveryReversal(PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                        PortfolioTransaction.Type.DELIVERY_INBOUND);

        assertInlineDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.BUY,
                        AccountTransaction.Type.BUY);
        assertInlineDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.SELL,
                        AccountTransaction.Type.SELL);
        assertInlineDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_INBOUND, PortfolioTransaction.Type.SELL,
                        AccountTransaction.Type.SELL);
        assertInlineDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_OUTBOUND, PortfolioTransaction.Type.BUY,
                        AccountTransaction.Type.BUY);
    }

    /**
     * Verifies that inline type editing offers the supported account master transitions.
     * Account-only changes must use the ledger converter instead of mutating the projection directly.
     */
    @Test
    public void testSupportedLedgerBackedAccountInlineTypeConversions() throws Exception
    {
        assertInlineAccountBuySellReversal(AccountTransaction.Type.BUY, AccountTransaction.Type.SELL,
                        PortfolioTransaction.Type.SELL);
        assertInlineAccountBuySellReversal(AccountTransaction.Type.SELL, AccountTransaction.Type.BUY,
                        PortfolioTransaction.Type.BUY);

        assertInlineAccountTransferReversal(AccountTransaction.Type.TRANSFER_IN,
                        AccountTransaction.Type.TRANSFER_OUT);
        assertInlineAccountTransferReversal(AccountTransaction.Type.TRANSFER_OUT,
                        AccountTransaction.Type.TRANSFER_IN);

        assertInlineAccountOnlyToggle(AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL);
        assertInlineAccountOnlyToggle(AccountTransaction.Type.REMOVAL, AccountTransaction.Type.DEPOSIT);
        assertInlineAccountOnlyToggle(AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE);
        assertInlineAccountOnlyToggle(AccountTransaction.Type.INTEREST_CHARGE, AccountTransaction.Type.INTEREST);
    }

    /**
     * Verifies that unsupported inline type changes are not exposed as ledger mutations.
     * Historical combo options without a safe converter must be blocked before any change.
     */
    @Test
    public void testForbiddenLedgerBackedInlineTypeConversions() throws Exception
    {
        var deposit = ledgerAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        assertForbiddenTypeEdit(deposit.client(), deposit.source().getTransactions().get(0),
                        AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.INTEREST);
        assertThat(deposit.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));

        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        assertForbiddenTypeEdit(buy.client(), buy.portfolio().getTransactions().get(0),
                        PortfolioTransaction.Type.BUY, AccountTransaction.Type.TRANSFER_IN);
        assertThat(buy.portfolio().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(buy.account().getTransactions().get(0).getType(), is(AccountTransaction.Type.BUY));

        var transfer = ledgerAccountTransferFixture();
        assertForbiddenTypeEdit(transfer.client(), transfer.source().getTransactions().get(0),
                        AccountTransaction.Type.TRANSFER_OUT, AccountTransaction.Type.REMOVAL);
        assertThat(transfer.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(transfer.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(transfer.source().getTransactions().get(0), transfer.target().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(transfer.target().getTransactions().get(0)));

        var fees = ledgerAccountOnlyFixture(AccountTransaction.Type.FEES);
        assertForbiddenTypeEdit(fees.client(), fees.source().getTransactions().get(0), AccountTransaction.Type.FEES,
                        AccountTransaction.Type.FEES_REFUND);
        assertThat(fees.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.FEES));

        var taxes = ledgerAccountOnlyFixture(AccountTransaction.Type.TAXES);
        assertForbiddenTypeEdit(taxes.client(), taxes.source().getTransactions().get(0), AccountTransaction.Type.TAXES,
                        AccountTransaction.Type.TAX_REFUND);
        assertThat(taxes.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TAXES));
    }

    /**
     * Verifies that matrix-allowed buy/sell type editing is still gated by concrete converter capability.
     * Posting-level forex is rejected by the buy/sell reversal converter, so inline editing must block it before execution.
     */
    @Test
    public void testLedgerBackedBuySellPostingForexInlineReversalIsBlockedBeforeExecution() throws Exception
    {
        assertUnsupportedPostingForexBuySellTypeEdit(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.SELL,
                        AccountTransaction.Type.SELL);
        assertUnsupportedPostingForexBuySellTypeEdit(PortfolioTransaction.Type.SELL, PortfolioTransaction.Type.BUY,
                        AccountTransaction.Type.BUY);
    }

    /**
     * Verifies that plan-referenced rows can use safe inline type converters.
     * The plan reference must still resolve when the converter can preserve or migrate the projection unambiguously.
     */
    @Test
    public void testInvestmentPlanReferencedLedgerBackedInlineTypeConversionsUseSafeConverters() throws Exception
    {
        var buyToDelivery = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var buyToDeliveryTransaction = buyToDelivery.portfolio().getTransactions().get(0);
        addInvestmentPlanRef(buyToDelivery.client(), buyToDeliveryTransaction);

        var deliverySupport = new TransactionTypeEditingSupport(buyToDelivery.client());
        assertThat(deliverySupport.canEdit(buyToDeliveryTransaction), is(true));
        setTypeValue(deliverySupport, buyToDeliveryTransaction, PortfolioTransaction.Type.BUY,
                        PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertThat(buyToDelivery.portfolio().getTransactions().get(0).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(buyToDelivery.client().getPlans().get(0).getTransactions(buyToDelivery.client()).get(0)
                        .getTransaction().getUUID(), is(buyToDeliveryTransaction.getUUID()));

        var deposit = ledgerAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        var accountTransaction = deposit.source().getTransactions().get(0);
        addInvestmentPlanRef(deposit.client(), accountTransaction);

        var accountSupport = new TransactionTypeEditingSupport(deposit.client());
        assertThat(accountSupport.canEdit(accountTransaction), is(true));
        setTypeValue(accountSupport, accountTransaction, AccountTransaction.Type.DEPOSIT,
                        AccountTransaction.Type.REMOVAL);
        assertThat(deposit.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(deposit.client().getPlans().get(0).getTransactions(deposit.client()).get(0).getTransaction()
                        .getUUID(), is(accountTransaction.getUUID()));
    }

    /**
     * Verifies that account-side plan references are not converted to delivery by inline editing.
     * That conversion would remove the account projection, so the editor must not offer it.
     */
    @Test
    public void testInvestmentPlanReferencedAccountSideBuySellInlineConversionToDeliveryIsNotEditable()
                    throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var portfolioTransaction = buy.portfolio().getTransactions().get(0);
        addInvestmentPlanRef(buy.client(), buy.account().getTransactions().get(0));

        var support = new TransactionTypeEditingSupport(buy.client());
        assertThat(support.canEdit(portfolioTransaction), is(true));
        assertForbiddenTypeEdit(buy.client(), portfolioTransaction, PortfolioTransaction.Type.BUY,
                        PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertThat(buy.portfolio().getTransactions().get(0).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(buy.account().getTransactions().get(0).getType(), is(AccountTransaction.Type.BUY));
    }

    /**
     * Verifies that ledger-backed metadata fields follow the inline policy.
     * Date and note remain editable, while source is blocked by the LedgerProjectionRole matrix.
     */
    @Test
    public void testLedgerBackedDepositMetadataInlinePolicy() throws Exception
    {
        var fixture = ledgerAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        var transaction = fixture.source().getTransactions().get(0);

        var dateTimeSupport = new DateTimeEditingSupport(AccountTransaction.class, "dateTime");
        assertThat(dateTimeSupport.canEdit(transaction), is(true));
        dateTimeSupport.setValue(transaction, UPDATED_DATE_TIME.toString());

        var noteSupport = new StringEditingSupport(AccountTransaction.class, "note");
        assertThat(noteSupport.canEdit(transaction), is(true));
        noteSupport.setValue(transaction, "updated note");

        var sourceSupport = new StringEditingSupport(AccountTransaction.class, "source");
        assertThat(sourceSupport.canEdit(transaction), is(false));
        assertThrows(UnsupportedOperationException.class, () -> sourceSupport.setValue(transaction, "updated source"));

        assertThat(transaction.getDateTime(), is(UPDATED_DATE_TIME));
        assertThat(transaction.getNote(), is("updated note"));
        assertThat(transaction.getSource(), is("source"));
        assertThat(ledgerEntryValue(transaction, "getDateTime"), is(UPDATED_DATE_TIME));
        assertThat(ledgerEntryValue(transaction, "getNote"), is("updated note"));
        assertThat(ledgerEntryValue(transaction, "getSource"), is("source"));
        assertThat(fixture.source().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(fixture.client());
    }

    /**
     * Verifies that the ledger inline-editing matrix leaves legacy source edits unchanged.
     * The source policy is only applied to ledger-backed projection rows.
     */
    @Test
    public void testLegacyDepositSourceInlineEditUsesLegacySetter() throws Exception
    {
        var fixture = legacyAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        var transaction = fixture.source().getTransactions().get(0);
        transaction.setSource("source");

        var sourceSupport = new StringEditingSupport(AccountTransaction.class, "source");
        assertThat(sourceSupport.canEdit(transaction), is(true));
        sourceSupport.setValue(transaction, "legacy source");

        assertThat(transaction.getSource(), is("legacy source"));
        assertThat(fixture.source().getTransactions().size(), is(1));
    }

    /**
     * Verifies that ledger-backed date inline editing accepts the UI's single-digit hour format.
     * A valid date edit must not be reported as an internal workbench error.
     */
    @Test
    public void testLedgerBackedDateTimeInlineEditAcceptsSingleDigitHour() throws Exception
    {
        var fixture = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var transaction = fixture.account().getTransactions().get(0);

        var dateTimeSupport = new DateTimeEditingSupport(AccountTransaction.class, "dateTime");
        dateTimeSupport.addListener((element, newValue, oldValue) -> ((AccountTransaction) element).getCrossEntry()
                        .updateFrom((AccountTransaction) element));
        dateTimeSupport.setValue(transaction, "02.02.2026, 0:05");

        var expected = LocalDateTime.of(2026, 2, 2, 0, 5);
        assertThat(transaction.getDateTime(), is(expected));
        assertThat(ledgerEntryValue(transaction, "getDateTime"), is(expected));
        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(fixture.client());
    }

    /**
     * Verifies that ledger-backed share inline editing follows the LedgerProjectionRole matrix.
     * Only dividends remain editable; portfolio and transfer shares are blocked before mutation.
     */
    @Test
    public void testLedgerBackedSharesInlineEditPolicy() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var portfolioTransaction = buy.portfolio().getTransactions().get(0);

        assertThat(TransactionsViewer.canEditShares(new TransactionPair<>(buy.portfolio(), portfolioTransaction)),
                        is(false));
        assertThat(TransactionsViewer.updateLedgerBackedShares(buy.client(),
                        new TransactionPair<>(buy.portfolio(), portfolioTransaction), Values.Share.factorize(6)),
                        is(false));
        assertThat(buy.portfolio().getTransactions().get(0).getShares(), is(Values.Share.factorize(5)));
        assertThat(buy.portfolio().getTransactions().size(), is(1));
        assertThat(buy.account().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(buy.client());

        var delivery = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        var deliveryTransaction = delivery.portfolio().getTransactions().get(0);
        assertThat(TransactionsViewer.canEditShares(new TransactionPair<>(delivery.portfolio(), deliveryTransaction)),
                        is(false));
        assertThat(TransactionsViewer.updateLedgerBackedShares(delivery.client(),
                        new TransactionPair<>(delivery.portfolio(), deliveryTransaction), Values.Share.factorize(7)),
                        is(false));
        assertThat(delivery.portfolio().getTransactions().get(0).getShares(), is(Values.Share.factorize(5)));
        assertThat(delivery.portfolio().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(delivery.client());

        var transfer = ledgerPortfolioTransferFixture();
        var sourceTransaction = transfer.source().getTransactions().get(0);
        assertThat(TransactionsViewer.canEditShares(new TransactionPair<>(transfer.source(), sourceTransaction)),
                        is(false));
        assertThat(TransactionsViewer.updateLedgerBackedShares(transfer.client(),
                        new TransactionPair<>(transfer.source(), sourceTransaction), Values.Share.factorize(8)),
                        is(false));
        assertThat(transfer.source().getTransactions().get(0).getShares(), is(Values.Share.factorize(5)));
        assertThat(transfer.target().getTransactions().get(0).getShares(), is(Values.Share.factorize(5)));
        assertThat(transfer.source().getTransactions().size(), is(1));
        assertThat(transfer.target().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(transfer.client());

        var dividend = ledgerDividendFixture();
        var dividendTransaction = dividend.source().getTransactions().get(0);
        assertThat(TransactionsViewer.canEditShares(new TransactionPair<>(dividend.source(), dividendTransaction)),
                        is(true));
        assertThat(TransactionsViewer.updateLedgerBackedShares(dividend.client(),
                        new TransactionPair<>(dividend.source(), dividendTransaction), Values.Share.factorize(9)),
                        is(true));
        assertThat(dividend.source().getTransactions().get(0).getShares(), is(Values.Share.factorize(9)));
        assertThat(ledgerPostingShares(dividend.source().getTransactions().get(0)), is(Values.Share.factorize(9)));
        assertThat(dividend.source().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(dividend.client());
    }

    /**
     * Verifies that dividend ex-date inline editing routes through the ledger editor.
     * The projection setter is not used and save/load keeps the changed ex-date.
     */
    @Test
    public void testLedgerBackedDividendExDateInlineEditRoutesThroughLedgerEditor() throws Exception
    {
        var dividend = ledgerDividendFixture();
        var transaction = dividend.source().getTransactions().get(0);
        var entryUUID = ledgerEntryValue(transaction, "getUUID");
        var projectionUUID = transaction.getUUID();
        var projectionRole = ledgerProjectionValue(transaction, "getRole");
        var newExDate = LocalDateTime.of(2026, 6, 20, 0, 0);

        var support = new ExDateEditingSupport(dividend.client());
        assertThat(support.canEdit(transaction), is(true));
        support.setValue(transaction, "2026-06-20");

        assertThat(transaction.getExDate(), is(newExDate));
        assertThat(ledgerEntryValue(transaction, "getUUID"), is(entryUUID));
        assertThat(transaction.getUUID(), is(projectionUUID));
        assertThat(ledgerProjectionValue(transaction, "getRole"), is(projectionRole));
        assertThat(ledgerPostingExDate(transaction), is(newExDate));
        assertThat(dividend.source().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(dividend.client());

        var xmlLoaded = reloadXml(dividend.client());
        var xmlTransaction = xmlLoaded.getAccounts().get(0).getTransactions().get(0);
        assertThat(xmlTransaction.getUUID(), is(projectionUUID));
        assertThat(xmlTransaction.getExDate(), is(newExDate));
        assertThat(ledgerPostingExDate(xmlTransaction), is(newExDate));
        assertLedgerStructurallyValid(xmlLoaded);

        var protobufLoaded = reloadProtobuf(dividend.client());
        var protobufTransaction = protobufLoaded.getAccounts().get(0).getTransactions().get(0);
        assertThat(protobufTransaction.getUUID(), is(projectionUUID));
        assertThat(protobufTransaction.getExDate(), is(newExDate));
        assertThat(ledgerPostingExDate(protobufTransaction), is(newExDate));
        assertLedgerStructurallyValid(protobufLoaded);
    }

    /**
     * Verifies that legacy dividend ex-date inline editing keeps its existing setter path.
     * The ledger-specific route must not change non-ledger transaction behavior.
     */
    @Test
    public void testLegacyDividendExDateInlineEditUsesLegacySetter()
    {
        var dividend = legacyDividendFixture();
        var transaction = dividend.source().getTransactions().get(0);
        var newExDate = LocalDateTime.of(2026, 6, 20, 0, 0);

        var support = new ExDateEditingSupport();
        assertThat(support.canEdit(transaction), is(true));
        support.setValue(transaction, "2026-06-20");

        assertThat(transaction.getExDate(), is(newExDate));
        assertThat(dividend.source().getTransactions().size(), is(1));
    }

    /**
     * Verifies that unsupported ledger-backed ex-date edits are rejected before mutation.
     * Only account transactions with a safe ex-date ledger path may be changed inline.
     */
    @Test
    public void testUnsupportedLedgerBackedExDateInlineEditRejectsBeforeMutation() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var accountProjection = buy.account().getTransactions().get(0);
        var accountOnly = ledgerAccountOnlyWithSecurityFixture(AccountTransaction.Type.FEES);
        var accountOnlyProjection = accountOnly.source().getTransactions().get(0);

        assertUnsupportedLedgerExDateEdit(buy.client(), accountProjection);
        assertUnsupportedLedgerExDateEdit(accountOnly.client(), accountOnlyProjection);
    }

    /**
     * Verifies that structural inline fields without a safe ledger editor remain blocked.
     * This prevents legacy setter writes against runtime projections.
     */
    @Test
    public void testForbiddenLedgerBackedStructuralFieldEdits() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var portfolioTransaction = buy.portfolio().getTransactions().get(0);
        var shares = portfolioTransaction.getShares();

        var exception = assertThrows(UnsupportedOperationException.class,
                        () -> new ValueEditingSupport(PortfolioTransaction.class, "shares", Values.Share)
                                        .setValue(portfolioTransaction, "6"));
        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_UI_011
                        .message(MessageFormat.format(Messages.LedgerPropertyEditingSupportUnsupportedInlineEdit,
                                        LedgerInlineEditingField.SHARES))));
        assertThat(portfolioTransaction.getShares(), is(shares));
        assertThat(buy.portfolio().getTransactions().size(), is(1));
        assertThat(buy.account().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(buy.client());
    }

    /**
     * Verifies that model-level transaction deduplication remains consistent for legacy and ledger-backed transfers.
     * The client model keeps its deduplicated transaction list for reporting-style callers.
     */
    @Test
    public void testAllTransactionsTransferDeduplicationMatchesLegacyAndLedgerBacked()
    {
        assertThat(ledgerAccountTransferFixture().client().getAllTransactions().size(), is(1));
        assertThat(legacyAccountTransferFixture().client().getAllTransactions().size(), is(1));
        assertThat(ledgerPortfolioTransferFixture().client().getAllTransactions().size(), is(1));
        assertThat(legacyPortfolioTransferFixture().client().getAllTransactions().size(), is(1));
    }

    /**
     * Verifies that the UI all-transactions view expands transfers into both owner sides.
     * The view shows source and target rows without changing Client.getAllTransactions.
     */
    @Test
    public void testAllTransactionsViewExpandsTransfersWithoutChangingClientDeduplication() throws Exception
    {
        var legacyAccountTransfer = legacyAccountTransferFixture();
        var legacyAccountTransactions = AllTransactionsView.getTransactionsForView(legacyAccountTransfer.client());

        assertThat(legacyAccountTransactions.size(), is(2));
        assertSame(legacyAccountTransfer.source(), legacyAccountTransactions.get(0).getOwner());
        assertThat(((AccountTransaction) legacyAccountTransactions.get(0).getTransaction()).getType(),
                        is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(legacyAccountTransfer.target(), legacyAccountTransactions.get(1).getOwner());
        assertThat(((AccountTransaction) legacyAccountTransactions.get(1).getTransaction()).getType(),
                        is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(legacyAccountTransfer.client().getAllTransactions().size(), is(1));

        var ledgerAccountTransfer = ledgerAccountTransferFixture();
        var ledgerAccountTransactions = AllTransactionsView.getTransactionsForView(ledgerAccountTransfer.client());

        assertThat(ledgerAccountTransactions.size(), is(2));
        assertSame(ledgerAccountTransfer.source(), ledgerAccountTransactions.get(0).getOwner());
        assertThat(((AccountTransaction) ledgerAccountTransactions.get(0).getTransaction()).getType(),
                        is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(ledgerAccountTransfer.target(), ledgerAccountTransactions.get(1).getOwner());
        assertThat(((AccountTransaction) ledgerAccountTransactions.get(1).getTransaction()).getType(),
                        is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(ledgerEntry(ledgerAccountTransactions.get(0).getTransaction()),
                        ledgerEntry(ledgerAccountTransactions.get(1).getTransaction()));
        assertThat(ledgerAccountTransfer.client().getAllTransactions().size(), is(1));
        assertThat(AllTransactionsView.matchesClientFilter(
                        new PortfolioClientFilter(List.of(), List.of(ledgerAccountTransfer.source())),
                        ledgerAccountTransactions.get(0)), is(true));
        assertThat(AllTransactionsView.matchesClientFilter(
                        new PortfolioClientFilter(List.of(), List.of(ledgerAccountTransfer.source())),
                        ledgerAccountTransactions.get(1)), is(false));

        var reloadedAccountTransfer = AllTransactionsView.getTransactionsForView(reloadXml(ledgerAccountTransfer.client()));
        assertThat(reloadedAccountTransfer.size(), is(2));
        assertThat(((AccountTransaction) reloadedAccountTransfer.get(0).getTransaction()).getType(),
                        is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(((AccountTransaction) reloadedAccountTransfer.get(1).getTransaction()).getType(),
                        is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(ledgerEntry(reloadedAccountTransfer.get(0).getTransaction()),
                        ledgerEntry(reloadedAccountTransfer.get(1).getTransaction()));

        var legacyPortfolioTransfer = legacyPortfolioTransferFixture();
        var legacyPortfolioTransactions = AllTransactionsView.getTransactionsForView(legacyPortfolioTransfer.client());

        assertThat(legacyPortfolioTransactions.size(), is(2));
        assertSame(legacyPortfolioTransfer.source(), legacyPortfolioTransactions.get(0).getOwner());
        assertThat(((PortfolioTransaction) legacyPortfolioTransactions.get(0).getTransaction()).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertSame(legacyPortfolioTransfer.target(), legacyPortfolioTransactions.get(1).getOwner());
        assertThat(((PortfolioTransaction) legacyPortfolioTransactions.get(1).getTransaction()).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(legacyPortfolioTransfer.client().getAllTransactions().size(), is(1));

        var ledgerPortfolioTransfer = ledgerPortfolioTransferFixture();
        var ledgerPortfolioTransactions = AllTransactionsView.getTransactionsForView(ledgerPortfolioTransfer.client());

        assertThat(ledgerPortfolioTransactions.size(), is(2));
        assertSame(ledgerPortfolioTransfer.source(), ledgerPortfolioTransactions.get(0).getOwner());
        assertThat(((PortfolioTransaction) ledgerPortfolioTransactions.get(0).getTransaction()).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertSame(ledgerPortfolioTransfer.target(), ledgerPortfolioTransactions.get(1).getOwner());
        assertThat(((PortfolioTransaction) ledgerPortfolioTransactions.get(1).getTransaction()).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(ledgerEntry(ledgerPortfolioTransactions.get(0).getTransaction()),
                        ledgerEntry(ledgerPortfolioTransactions.get(1).getTransaction()));
        assertThat(ledgerPortfolioTransfer.client().getAllTransactions().size(), is(1));
        assertThat(AllTransactionsView.matchesClientFilter(
                        new PortfolioClientFilter(List.of(ledgerPortfolioTransfer.source()), List.of()),
                        ledgerPortfolioTransactions.get(0)), is(true));
        assertThat(AllTransactionsView.matchesClientFilter(
                        new PortfolioClientFilter(List.of(ledgerPortfolioTransfer.source()), List.of()),
                        ledgerPortfolioTransactions.get(1)), is(false));

        var reloadedPortfolioTransfer = AllTransactionsView
                        .getTransactionsForView(reloadXml(ledgerPortfolioTransfer.client()));
        assertThat(reloadedPortfolioTransfer.size(), is(2));
        assertThat(((PortfolioTransaction) reloadedPortfolioTransfer.get(0).getTransaction()).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(((PortfolioTransaction) reloadedPortfolioTransfer.get(1).getTransaction()).getType(),
                        is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(ledgerEntry(reloadedPortfolioTransfer.get(0).getTransaction()),
                        ledgerEntry(reloadedPortfolioTransfer.get(1).getTransaction()));

        assertThat(AllTransactionsView.getTransactionsForView(legacyBuySellFixture(PortfolioTransaction.Type.BUY).client())
                        .size(), is(1));
        var ledgerBuySell = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var ledgerBuySellTransactions = AllTransactionsView.getTransactionsForView(ledgerBuySell.client());
        assertThat(ledgerBuySellTransactions.size(), is(1));
        assertThat(AllTransactionsView.matchesClientFilter(
                        new PortfolioClientFilter(List.of(), List.of(ledgerBuySell.account())),
                        ledgerBuySellTransactions.get(0)), is(true));
    }

    /**
     * Verifies that generic owner and cross-entry field edits stay blocked for unsupported ledger-backed rows.
     * The UI must not open the old delete/insert/replay path for runtime projections.
     */
    @Test
    public void testForbiddenLedgerBackedOwnerAndCrossEntryFieldEdits() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var replacement = new Portfolio("Replacement");
        buy.client().addPortfolio(replacement);

        var portfolioTransaction = buy.portfolio().getTransactions().get(0);
        var crossEntry = portfolioTransaction.getCrossEntry();

        assertThrows(UnsupportedOperationException.class,
                        () -> crossEntry.setOwner(portfolioTransaction, replacement));

        assertSame(buy.portfolio(), crossEntry.getOwner(portfolioTransaction));
        assertThat(buy.portfolio().getTransactions().size(), is(1));
        assertThat(buy.account().getTransactions().size(), is(1));
        assertLedgerStructurallyValid(buy.client());
    }

    /**
     * Verifies that buy/sell owner inline editing uses LedgerOwnerPatchHelper.
     * Account and portfolio changes must move the ledger facts while keeping one persisted booking truth.
     */
    @Test
    public void testLedgerBackedBuySellOwnerListEditingSupportUsesLedgerOwnerPatch() throws Exception
    {
        assertLedgerBackedBuySellOwnerListEditingSupportUsesLedgerOwnerPatch(PortfolioTransaction.Type.BUY);
        assertLedgerBackedBuySellOwnerListEditingSupportUsesLedgerOwnerPatch(PortfolioTransaction.Type.SELL);
    }

    private void assertLedgerBackedBuySellOwnerListEditingSupportUsesLedgerOwnerPatch(PortfolioTransaction.Type type)
                    throws Exception
    {
        var buy = ledgerBuySellFixture(type);
        var targetAccount = account("Target Account", CurrencyUnit.EUR);
        var targetPortfolio = new Portfolio("Target Portfolio");
        buy.client().addAccount(targetAccount);
        buy.client().addPortfolio(targetPortfolio);

        var accountTransaction = buy.account().getTransactions().get(0);
        var portfolioTransaction = buy.portfolio().getTransactions().get(0);
        var entryUUID = ledgerEntryValue(accountTransaction, "getUUID");
        var accountProjectionUUID = accountTransaction.getUUID();
        var portfolioProjectionUUID = portfolioTransaction.getUUID();
        var accountPlan = addInvestmentPlanRef(buy.client(), accountTransaction);
        var portfolioPlan = addInvestmentPlanRef(buy.client(), portfolioTransaction);

        setOwnerValue(buy.client(), accountTransaction, TransactionOwnerListEditingSupport.EditMode.OWNER,
                        targetAccount);

        assertThat(buy.account().getTransactions().isEmpty(), is(true));
        accountTransaction = findAccountTransaction(targetAccount, accountProjectionUUID);
        portfolioTransaction = findPortfolioTransaction(buy.portfolio(), portfolioProjectionUUID);
        assertThat(ledgerEntryValue(accountTransaction, "getUUID"), is(entryUUID));
        assertSame(targetAccount, accountTransaction.getCrossEntry().getOwner(accountTransaction));
        assertSame(buy.portfolio(), accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
        assertPlanRefResolves(accountPlan, buy.client(), targetAccount, accountProjectionUUID);

        setOwnerValue(buy.client(), portfolioTransaction, TransactionOwnerListEditingSupport.EditMode.OWNER,
                        targetPortfolio);

        assertThat(buy.portfolio().getTransactions().isEmpty(), is(true));
        accountTransaction = findAccountTransaction(targetAccount, accountProjectionUUID);
        portfolioTransaction = findPortfolioTransaction(targetPortfolio, portfolioProjectionUUID);
        assertThat(ledgerEntryValue(portfolioTransaction, "getUUID"), is(entryUUID));
        assertSame(targetPortfolio, portfolioTransaction.getCrossEntry().getOwner(portfolioTransaction));
        assertSame(targetAccount, portfolioTransaction.getCrossEntry().getCrossOwner(portfolioTransaction));
        assertPlanRefResolves(portfolioPlan, buy.client(), targetPortfolio, portfolioProjectionUUID);
        assertThat(targetAccount.getTransactions().size(), is(1));
        assertThat(targetPortfolio.getTransactions().size(), is(1));
        assertLedgerStructurallyValid(buy.client());

        var reloaded = reloadXml(buy.client());
        assertPlanRefResolves(reloaded.getPlans().get(0), reloaded, reloaded.getAccounts().get(1),
                        accountProjectionUUID);
        assertPlanRefResolves(reloaded.getPlans().get(1), reloaded, reloaded.getPortfolios().get(1),
                        portfolioProjectionUUID);
    }

    /**
     * Verifies that source-side transfer owner inline editing uses LedgerOwnerPatchHelper.
     * The moved projection must leave the old owner list and appear in the new owner list without duplication.
     */
    @Test
    public void testLedgerBackedTransferOwnerListEditingSupportUsesLedgerOwnerPatch() throws Exception
    {
        var transfer = ledgerAccountTransferFixture();
        var newSource = account("New Source", CurrencyUnit.EUR);
        var newTarget = account("New Target", CurrencyUnit.EUR);
        transfer.client().addAccount(newSource);
        transfer.client().addAccount(newTarget);

        var sourceTransaction = transfer.source().getTransactions().get(0);
        var targetTransaction = transfer.target().getTransactions().get(0);
        var entryUUID = ledgerEntryValue(sourceTransaction, "getUUID");
        var sourceProjectionUUID = sourceTransaction.getUUID();
        var targetProjectionUUID = targetTransaction.getUUID();
        var sourcePlan = addInvestmentPlanRef(transfer.client(), sourceTransaction);
        var targetPlan = addInvestmentPlanRef(transfer.client(), targetTransaction);

        setOwnerValue(transfer.client(), sourceTransaction, TransactionOwnerListEditingSupport.EditMode.OWNER,
                        newSource);
        sourceTransaction = findAccountTransaction(newSource, sourceProjectionUUID);
        targetTransaction = findAccountTransaction(transfer.target(), targetProjectionUUID);
        assertThat(transfer.source().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(sourceTransaction, "getUUID"), is(entryUUID));
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(transfer.target(), sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertPlanRefResolves(sourcePlan, transfer.client(), newSource, sourceProjectionUUID);

        setOwnerValue(transfer.client(), sourceTransaction, TransactionOwnerListEditingSupport.EditMode.CROSSOWNER,
                        newTarget);
        sourceTransaction = findAccountTransaction(newSource, sourceProjectionUUID);
        targetTransaction = findAccountTransaction(newTarget, targetProjectionUUID);
        assertThat(transfer.target().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(targetTransaction, "getUUID"), is(entryUUID));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(newTarget, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
        assertPlanRefResolves(targetPlan, transfer.client(), newTarget, targetProjectionUUID);
        assertThat(newSource.getTransactions().size(), is(1));
        assertThat(newTarget.getTransactions().size(), is(1));
        assertLedgerStructurallyValid(transfer.client());

        var portfolioTransfer = ledgerPortfolioTransferFixture();
        var newSourcePortfolio = new Portfolio("New Source Portfolio");
        var newTargetPortfolio = new Portfolio("New Target Portfolio");
        portfolioTransfer.client().addPortfolio(newSourcePortfolio);
        portfolioTransfer.client().addPortfolio(newTargetPortfolio);

        var sourcePortfolioTransaction = portfolioTransfer.source().getTransactions().get(0);
        var targetPortfolioTransaction = portfolioTransfer.target().getTransactions().get(0);
        var portfolioEntryUUID = ledgerEntryValue(sourcePortfolioTransaction, "getUUID");
        var sourcePortfolioProjectionUUID = sourcePortfolioTransaction.getUUID();
        var targetPortfolioProjectionUUID = targetPortfolioTransaction.getUUID();
        var sourcePortfolioPlan = addInvestmentPlanRef(portfolioTransfer.client(), sourcePortfolioTransaction);
        var targetPortfolioPlan = addInvestmentPlanRef(portfolioTransfer.client(), targetPortfolioTransaction);

        setOwnerValue(portfolioTransfer.client(), sourcePortfolioTransaction,
                        TransactionOwnerListEditingSupport.EditMode.OWNER, newSourcePortfolio);
        sourcePortfolioTransaction = findPortfolioTransaction(newSourcePortfolio, sourcePortfolioProjectionUUID);
        targetPortfolioTransaction = findPortfolioTransaction(portfolioTransfer.target(),
                        targetPortfolioProjectionUUID);
        assertThat(portfolioTransfer.source().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(sourcePortfolioTransaction, "getUUID"), is(portfolioEntryUUID));
        assertThat(sourcePortfolioTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertSame(portfolioTransfer.target(),
                        sourcePortfolioTransaction.getCrossEntry().getCrossOwner(sourcePortfolioTransaction));
        assertPlanRefResolves(sourcePortfolioPlan, portfolioTransfer.client(), newSourcePortfolio,
                        sourcePortfolioProjectionUUID);

        setOwnerValue(portfolioTransfer.client(), sourcePortfolioTransaction,
                        TransactionOwnerListEditingSupport.EditMode.CROSSOWNER, newTargetPortfolio);
        sourcePortfolioTransaction = findPortfolioTransaction(newSourcePortfolio, sourcePortfolioProjectionUUID);
        targetPortfolioTransaction = findPortfolioTransaction(newTargetPortfolio, targetPortfolioProjectionUUID);
        assertThat(portfolioTransfer.target().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(targetPortfolioTransaction, "getUUID"), is(portfolioEntryUUID));
        assertThat(targetPortfolioTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(newTargetPortfolio,
                        sourcePortfolioTransaction.getCrossEntry().getCrossOwner(sourcePortfolioTransaction));
        assertPlanRefResolves(targetPortfolioPlan, portfolioTransfer.client(), newTargetPortfolio,
                        targetPortfolioProjectionUUID);
        assertThat(newSourcePortfolio.getTransactions().size(), is(1));
        assertThat(newTargetPortfolio.getTransactions().size(), is(1));
        assertLedgerStructurallyValid(portfolioTransfer.client());
    }

    /**
     * Verifies that target-side transfer rows also use LedgerOwnerPatchHelper.
     * Cross-owner edits must update the correct target side and keep plan references resolvable.
     */
    @Test
    public void testLedgerBackedTransferTargetRowsUseLedgerOwnerPatch() throws Exception
    {
        var transfer = ledgerAccountTransferFixture();
        var newSource = account("New Source From Target Row", CurrencyUnit.EUR);
        var newTarget = account("New Target From Target Row", CurrencyUnit.EUR);
        transfer.client().addAccount(newSource);
        transfer.client().addAccount(newTarget);

        var sourceTransaction = transfer.source().getTransactions().get(0);
        var targetTransaction = transfer.target().getTransactions().get(0);
        var entryUUID = ledgerEntryValue(targetTransaction, "getUUID");
        var sourceProjectionUUID = sourceTransaction.getUUID();
        var targetProjectionUUID = targetTransaction.getUUID();
        var sourcePlan = addInvestmentPlanRef(transfer.client(), sourceTransaction);
        var targetPlan = addInvestmentPlanRef(transfer.client(), targetTransaction);

        setOwnerValue(transfer.client(), targetTransaction, TransactionOwnerListEditingSupport.EditMode.OWNER,
                        newTarget);
        sourceTransaction = findAccountTransaction(transfer.source(), sourceProjectionUUID);
        targetTransaction = findAccountTransaction(newTarget, targetProjectionUUID);
        assertThat(transfer.target().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(targetTransaction, "getUUID"), is(entryUUID));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(transfer.source(), targetTransaction.getCrossEntry().getCrossOwner(targetTransaction));
        assertPlanRefResolves(targetPlan, transfer.client(), newTarget, targetProjectionUUID);

        setOwnerValue(transfer.client(), targetTransaction, TransactionOwnerListEditingSupport.EditMode.CROSSOWNER,
                        newSource);
        sourceTransaction = findAccountTransaction(newSource, sourceProjectionUUID);
        targetTransaction = findAccountTransaction(newTarget, targetProjectionUUID);
        assertThat(transfer.source().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(sourceTransaction, "getUUID"), is(entryUUID));
        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(newSource, targetTransaction.getCrossEntry().getCrossOwner(targetTransaction));
        assertPlanRefResolves(sourcePlan, transfer.client(), newSource, sourceProjectionUUID);
        assertThat(newSource.getTransactions().size(), is(1));
        assertThat(newTarget.getTransactions().size(), is(1));
        assertLedgerStructurallyValid(transfer.client());

        var reloadedTransfer = reloadXml(transfer.client());
        assertPlanRefResolves(reloadedTransfer.getPlans().get(0), reloadedTransfer, reloadedTransfer.getAccounts().get(2),
                        sourceProjectionUUID);
        assertPlanRefResolves(reloadedTransfer.getPlans().get(1), reloadedTransfer, reloadedTransfer.getAccounts().get(3),
                        targetProjectionUUID);

        var portfolioTransfer = ledgerPortfolioTransferFixture();
        var newSourcePortfolio = new Portfolio("New Source Portfolio From Target Row");
        var newTargetPortfolio = new Portfolio("New Target Portfolio From Target Row");
        portfolioTransfer.client().addPortfolio(newSourcePortfolio);
        portfolioTransfer.client().addPortfolio(newTargetPortfolio);

        var sourcePortfolioTransaction = portfolioTransfer.source().getTransactions().get(0);
        var targetPortfolioTransaction = portfolioTransfer.target().getTransactions().get(0);
        var portfolioEntryUUID = ledgerEntryValue(targetPortfolioTransaction, "getUUID");
        var sourcePortfolioProjectionUUID = sourcePortfolioTransaction.getUUID();
        var targetPortfolioProjectionUUID = targetPortfolioTransaction.getUUID();
        var sourcePortfolioPlan = addInvestmentPlanRef(portfolioTransfer.client(), sourcePortfolioTransaction);
        var targetPortfolioPlan = addInvestmentPlanRef(portfolioTransfer.client(), targetPortfolioTransaction);

        setOwnerValue(portfolioTransfer.client(), targetPortfolioTransaction,
                        TransactionOwnerListEditingSupport.EditMode.OWNER, newTargetPortfolio);
        sourcePortfolioTransaction = findPortfolioTransaction(portfolioTransfer.source(),
                        sourcePortfolioProjectionUUID);
        targetPortfolioTransaction = findPortfolioTransaction(newTargetPortfolio, targetPortfolioProjectionUUID);
        assertThat(portfolioTransfer.target().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(targetPortfolioTransaction, "getUUID"), is(portfolioEntryUUID));
        assertThat(targetPortfolioTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(portfolioTransfer.source(),
                        targetPortfolioTransaction.getCrossEntry().getCrossOwner(targetPortfolioTransaction));
        assertPlanRefResolves(targetPortfolioPlan, portfolioTransfer.client(), newTargetPortfolio,
                        targetPortfolioProjectionUUID);

        setOwnerValue(portfolioTransfer.client(), targetPortfolioTransaction,
                        TransactionOwnerListEditingSupport.EditMode.CROSSOWNER, newSourcePortfolio);
        sourcePortfolioTransaction = findPortfolioTransaction(newSourcePortfolio, sourcePortfolioProjectionUUID);
        targetPortfolioTransaction = findPortfolioTransaction(newTargetPortfolio, targetPortfolioProjectionUUID);
        assertThat(portfolioTransfer.source().getTransactions().isEmpty(), is(true));
        assertThat(ledgerEntryValue(sourcePortfolioTransaction, "getUUID"), is(portfolioEntryUUID));
        assertThat(sourcePortfolioTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertSame(newSourcePortfolio,
                        targetPortfolioTransaction.getCrossEntry().getCrossOwner(targetPortfolioTransaction));
        assertPlanRefResolves(sourcePortfolioPlan, portfolioTransfer.client(), newSourcePortfolio,
                        sourcePortfolioProjectionUUID);
        assertThat(newSourcePortfolio.getTransactions().size(), is(1));
        assertThat(newTargetPortfolio.getTransactions().size(), is(1));
        assertLedgerStructurallyValid(portfolioTransfer.client());

        var reloadedPortfolioTransfer = reloadXml(portfolioTransfer.client());
        assertPlanRefResolves(reloadedPortfolioTransfer.getPlans().get(0), reloadedPortfolioTransfer,
                        reloadedPortfolioTransfer.getPortfolios().get(2), sourcePortfolioProjectionUUID);
        assertPlanRefResolves(reloadedPortfolioTransfer.getPlans().get(1), reloadedPortfolioTransfer,
                        reloadedPortfolioTransfer.getPortfolios().get(3), targetPortfolioProjectionUUID);
    }

    /**
     * Verifies that owner inline editing is restored only for master-supported cross-entry families.
     * Account-only, dividend, and delivery rows remain blocked because master did not expose the same edit.
     */
    @Test
    public void testLedgerBackedOwnerListEditingSupportKeepsUnsupportedFamiliesBlocked() throws Exception
    {
        var accountOnly = ledgerAccountOnlyFixture(AccountTransaction.Type.DEPOSIT);
        assertThat(new TransactionOwnerListEditingSupport(accountOnly.client(),
                        TransactionOwnerListEditingSupport.EditMode.OWNER)
                                        .canEdit(accountOnly.source().getTransactions().get(0)),
                        is(false));

        var dividend = ledgerDividendFixture();
        assertThat(new TransactionOwnerListEditingSupport(dividend.client(),
                        TransactionOwnerListEditingSupport.EditMode.OWNER)
                                        .canEdit(dividend.source().getTransactions().get(0)),
                        is(false));

        var delivery = ledgerDeliveryFixture(PortfolioTransaction.Type.DELIVERY_INBOUND);
        assertThat(new TransactionOwnerListEditingSupport(delivery.client(),
                        TransactionOwnerListEditingSupport.EditMode.OWNER)
                                        .canEdit(delivery.portfolio().getTransactions().get(0)),
                        is(false));
    }

    /**
     * Verifies that invalid owner choices are rejected before mutation.
     * Currency mismatches and source-equals-target changes must not leave partial ledger owner moves.
     */
    @Test
    public void testLedgerBackedOwnerListEditingSupportRejectsInvalidOwnersBeforeMutation() throws Exception
    {
        var buy = ledgerBuySellFixture(PortfolioTransaction.Type.BUY);
        var usdAccount = account("USD Account", CurrencyUnit.USD);
        buy.client().addAccount(usdAccount);

        var accountTransaction = buy.account().getTransactions().get(0);
        assertThrows(IllegalArgumentException.class,
                        () -> setOwnerValue(buy.client(), accountTransaction,
                                        TransactionOwnerListEditingSupport.EditMode.OWNER, usdAccount));
        assertThat(buy.account().getTransactions().size(), is(1));

        var transfer = ledgerAccountTransferFixture();
        var accountTransferException = assertThrows(IllegalArgumentException.class,
                        () -> setOwnerValue(transfer.client(), transfer.source().getTransactions().get(0),
                                        TransactionOwnerListEditingSupport.EditMode.OWNER, transfer.target()));
        assertThat(accountTransferException.getMessage(), is(LedgerDiagnosticCode.LEDGER_UI_012
                        .message(Messages.LedgerTransactionOwnerListEditingSupportDistinctOwnersRequired)));
        assertThat(transfer.source().getTransactions().size(), is(1));
        assertThat(transfer.target().getTransactions().size(), is(1));

        var portfolioTransfer = ledgerPortfolioTransferFixture();
        var portfolioTransferException = assertThrows(IllegalArgumentException.class,
                        () -> setOwnerValue(portfolioTransfer.client(),
                                        portfolioTransfer.source().getTransactions().get(0),
                                        TransactionOwnerListEditingSupport.EditMode.OWNER, portfolioTransfer.target()));
        assertThat(portfolioTransferException.getMessage(), is(LedgerDiagnosticCode.LEDGER_UI_012
                        .message(Messages.LedgerTransactionOwnerListEditingSupportDistinctOwnersRequired)));
        assertThat(portfolioTransfer.source().getTransactions().size(), is(1));
        assertThat(portfolioTransfer.target().getTransactions().size(), is(1));
    }

    private void assertLedgerAccountOnlyDialogRoute(AccountTransaction.Type type) throws Exception
    {
        var fixture = ledgerAccountOnlyFixture(type);
        assertSupportedAccountEditRoute(fixture.client(), fixture.source(), fixture.source().getTransactions().get(0),
                        AccountTransactionDialog.class, type);
    }

    private void assertLedgerDividendDialogRoute() throws Exception
    {
        var fixture = ledgerDividendFixture();
        assertSupportedAccountEditRoute(fixture.client(), fixture.source(), fixture.source().getTransactions().get(0),
                        AccountTransactionDialog.class, AccountTransaction.Type.DIVIDENDS);
    }

    private void assertSupportedBuySellToDeliveryContextMenuRoute(Portfolio owner, PortfolioTransaction transaction)
    {
        var pair = new TransactionPair<>(owner, transaction);

        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(List.of(pair)), is(true));
        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(List.of(pair)), is(false));
    }

    private void assertSupportedDeliveryToBuySellContextMenuRoute(Portfolio owner, PortfolioTransaction transaction)
    {
        var pair = new TransactionPair<>(owner, transaction);

        assertThat(TransactionContextMenu.supportsDeliveryToBuySellAction(List.of(pair)), is(true));
        assertThat(TransactionContextMenu.supportsBuySellToDeliveryAction(List.of(pair)), is(false));
    }

    private void assertSupportedAccountEditRoute(Client client, Account owner, AccountTransaction transaction,
                    Class<?> dialogType, Object... parameters) throws Exception
    {
        var action = createEditAccountTransactionAction(client, new TransactionPair<>(owner, transaction));

        assertThat(action, instanceOf(OpenDialogAction.class));
        assertSame(dialogType, dialogType(action));
        assertArrayEquals(parameters, parameters(action));
    }

    private void assertSupportedPortfolioEditRoute(Client client, Portfolio owner, PortfolioTransaction transaction,
                    Class<?> dialogType, Object... parameters) throws Exception
    {
        var action = createEditPortfolioTransactionAction(client, new TransactionPair<>(owner, transaction));

        assertThat(action, instanceOf(OpenDialogAction.class));
        assertSame(dialogType, dialogType(action));
        assertArrayEquals(parameters, parameters(action));
    }

    private Action createEditAccountTransactionAction(Client client, TransactionPair<AccountTransaction> transaction)
                    throws Exception
    {
        var method = TransactionContextMenu.class.getDeclaredMethod("createEditAccountTransactionAction",
                        TransactionPair.class);
        method.setAccessible(true);
        return (Action) method.invoke(new TransactionContextMenu(null), transaction);
    }

    private Action createEditPortfolioTransactionAction(Client client, TransactionPair<PortfolioTransaction> transaction)
                    throws Exception
    {
        var method = TransactionContextMenu.class.getDeclaredMethod("createEditPortfolioTransactionAction",
                        TransactionPair.class);
        method.setAccessible(true);
        return (Action) method.invoke(new TransactionContextMenu(null), transaction);
    }

    private Class<?> dialogType(Action action) throws Exception
    {
        return (Class<?>) field(action, "type");
    }

    private Object[] parameters(Action action) throws Exception
    {
        var parameters = (Object[]) field(action, "parameters");
        return parameters != null ? parameters : new Object[0];
    }

    private Object field(Object object, String name) throws Exception
    {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private void assertInlineBuySellToDelivery(PortfolioTransaction.Type from, PortfolioTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerBuySellFixture(from);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var portfolioUUID = portfolioTransaction.getUUID();

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(portfolioTransaction), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), portfolioTransaction, from, to);

        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.portfolio().getTransactions().get(0).getCrossEntry(), is(nullValue()));
    }

    private void assertInlinePortfolioBuySellReversal(PortfolioTransaction.Type from, PortfolioTransaction.Type to,
                    AccountTransaction.Type accountType) throws Exception
    {
        var fixture = ledgerBuySellFixture(from);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var accountTransaction = fixture.account().getTransactions().get(0);
        var portfolioUUID = portfolioTransaction.getUUID();
        var accountUUID = accountTransaction.getUUID();

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(portfolioTransaction), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), portfolioTransaction, from, to);

        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.account().getTransactions().get(0).getUUID(), is(accountUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.account().getTransactions().get(0).getType(), is(accountType));
        assertSame(fixture.portfolio().getTransactions().get(0), fixture.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.account().getTransactions().get(0)));
    }

    private void assertInlineDeliveryReversal(PortfolioTransaction.Type from, PortfolioTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerDeliveryFixture(from);
        var delivery = fixture.portfolio().getTransactions().get(0);
        var portfolioUUID = delivery.getUUID();

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(delivery), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), delivery, from, to);

        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.portfolio().getTransactions().get(0).getCrossEntry(), is(nullValue()));
        assertLedgerStructurallyValid(fixture.client());
    }

    private void assertInlineDeliveryToBuySell(PortfolioTransaction.Type from, PortfolioTransaction.Type to,
                    AccountTransaction.Type accountType) throws Exception
    {
        var fixture = ledgerDeliveryFixture(from);
        var delivery = fixture.portfolio().getTransactions().get(0);
        var portfolioUUID = delivery.getUUID();

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(delivery), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), delivery, from, to);

        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.account().getTransactions().get(0).getType(), is(accountType));
        assertSame(fixture.portfolio().getTransactions().get(0), fixture.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.account().getTransactions().get(0)));
        assertLedgerStructurallyValid(fixture.client());
    }

    private void assertInlineAccountTransferReversal(AccountTransaction.Type from, AccountTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerAccountTransferFixture();
        AccountTransaction transaction = from == AccountTransaction.Type.TRANSFER_OUT
                        ? fixture.source().getTransactions().get(0)
                        : fixture.target().getTransactions().get(0);

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(transaction), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), transaction, from, to);

        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.target().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(fixture.target().getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertSame(fixture.source().getTransactions().get(0), fixture.target().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.target().getTransactions().get(0)));
    }

    private void assertInlineAccountBuySellReversal(AccountTransaction.Type from, AccountTransaction.Type to,
                    PortfolioTransaction.Type portfolioType) throws Exception
    {
        var fixture = ledgerBuySellFixture(from == AccountTransaction.Type.BUY ? PortfolioTransaction.Type.BUY
                        : PortfolioTransaction.Type.SELL);
        var accountTransaction = fixture.account().getTransactions().get(0);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var accountUUID = accountTransaction.getUUID();
        var portfolioUUID = portfolioTransaction.getUUID();

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(accountTransaction), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), accountTransaction, from, to);

        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.account().getTransactions().get(0).getUUID(), is(accountUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.account().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(portfolioType));
        assertSame(fixture.portfolio().getTransactions().get(0), fixture.account().getTransactions().get(0)
                        .getCrossEntry().getCrossTransaction(fixture.account().getTransactions().get(0)));
    }

    private void assertInlineAccountOnlyToggle(AccountTransaction.Type from, AccountTransaction.Type to)
                    throws Exception
    {
        var fixture = ledgerAccountOnlyFixture(from);
        var transaction = fixture.source().getTransactions().get(0);
        var projectionUUID = transaction.getUUID();

        assertThat(new TransactionTypeEditingSupport(fixture.client()).canEdit(transaction), is(true));
        setTypeValue(new TransactionTypeEditingSupport(fixture.client()), transaction, from, to);

        assertThat(fixture.source().getTransactions().size(), is(1));
        assertThat(fixture.source().getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(fixture.source().getTransactions().get(0).getType(), is(to));
        assertThat(fixture.source().getTransactions().get(0).getAmount(), is(Values.Amount.factorize(123)));
    }

    private void assertForbiddenTypeEdit(Client client, Object element, Object current, Object target)
                    throws Exception
    {
        var support = new TransactionTypeEditingSupport(client);
        setTypeItems(support, current, target);
        assertThrows(UnsupportedOperationException.class, () -> support.setValue(element, 1));
        assertLedgerStructurallyValid(client);
    }

    private void assertUnsupportedPostingForexBuySellTypeEdit(PortfolioTransaction.Type from,
                    PortfolioTransaction.Type to, AccountTransaction.Type accountTo) throws Exception
    {
        var fixture = ledgerBuySellFixture(from);
        var accountTransaction = fixture.account().getTransactions().get(0);
        var portfolioTransaction = fixture.portfolio().getTransactions().get(0);
        var cashPosting = ledgerPosting(portfolioTransaction, "CASH");
        var accountUUID = accountTransaction.getUUID();
        var portfolioUUID = portfolioTransaction.getUUID();

        cashPosting.getClass().getMethod("setForexAmount", Long.class).invoke(cashPosting,
                        Values.Amount.factorize(246));
        cashPosting.getClass().getMethod("setForexCurrency", String.class).invoke(cashPosting, CurrencyUnit.USD);
        cashPosting.getClass().getMethod("setExchangeRate", java.math.BigDecimal.class).invoke(cashPosting,
                        java.math.BigDecimal.valueOf(0.5));

        var support = new TransactionTypeEditingSupport(fixture.client());

        assertThat(support.canEdit(accountTransaction), is(false));
        assertThat(supportsTypeTransition(support, accountTransaction, accountTransaction.getType(), accountTo),
                        is(false));
        assertThat(supportsTypeTransition(support, portfolioTransaction, from, to), is(false));

        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.account().getTransactions().get(0).getUUID(), is(accountUUID));
        assertThat(fixture.portfolio().getTransactions().get(0).getUUID(), is(portfolioUUID));
        assertThat(fixture.account().getTransactions().get(0).getType().name(), is(from.name()));
        assertThat(fixture.portfolio().getTransactions().get(0).getType(), is(from));
        assertThat(cashPosting.getClass().getMethod("getForexAmount").invoke(cashPosting),
                        is(Values.Amount.factorize(246)));
        assertThat(cashPosting.getClass().getMethod("getForexCurrency").invoke(cashPosting), is(CurrencyUnit.USD));
        assertThat(cashPosting.getClass().getMethod("getExchangeRate").invoke(cashPosting),
                        is(java.math.BigDecimal.valueOf(0.5)));
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

    private boolean supportsTypeTransition(TransactionTypeEditingSupport support, Transaction transaction,
                    Enum<?> current, Enum<?> target) throws Exception
    {
        Method method = TransactionTypeEditingSupport.class.getDeclaredMethod("supportsTransition", Transaction.class,
                        Enum.class, Enum.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(support, transaction, current, target);
    }

    private void setOwnerComboItems(TransactionOwnerListEditingSupport support, Object owner) throws Exception
    {
        Field field = TransactionOwnerListEditingSupport.class.getDeclaredField("comboBoxItems");
        field.setAccessible(true);
        field.set(support, new ArrayList<>(List.of(owner)));
    }

    private void setOwnerValue(Client client, Transaction transaction, TransactionOwnerListEditingSupport.EditMode mode,
                    Object owner) throws Exception
    {
        var support = new TransactionOwnerListEditingSupport(client, mode);

        assertThat(support.canEdit(transaction), is(true));
        setOwnerComboItems(support, owner);
        support.setValue(transaction, 0);
    }

    private void assertUnsupportedLedgerExDateEdit(Client client, AccountTransaction transaction) throws Exception
    {
        var originalExDate = transaction.getExDate();
        var entryUUID = ledgerEntryValue(transaction, "getUUID");
        var projectionUUID = transaction.getUUID();
        var support = new ExDateEditingSupport(client);

        assertThat(support.canEdit(transaction), is(false));
        var exception = assertThrows(UnsupportedOperationException.class,
                        () -> support.setValue(transaction, "2026-06-20"));

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_UI_009
                        .message(Messages.LedgerExDateEditingSupportNoSafeEditorPolicyBlocked)));
        assertThat(transaction.getExDate(), is(originalExDate));
        assertThat(ledgerEntryValue(transaction, "getUUID"), is(entryUUID));
        assertThat(transaction.getUUID(), is(projectionUUID));
        assertLedgerStructurallyValid(client);
    }

    private AccountTransaction findAccountTransaction(Account account, String uuid)
    {
        return account.getTransactions().stream().filter(transaction -> transaction.getUUID().equals(uuid)).findFirst()
                        .orElseThrow();
    }

    private PortfolioTransaction findPortfolioTransaction(Portfolio portfolio, String uuid)
    {
        return portfolio.getTransactions().stream().filter(transaction -> transaction.getUUID().equals(uuid))
                        .findFirst().orElseThrow();
    }

    private void assertPlanRefResolves(InvestmentPlan plan, Client client, Object owner, String projectionUUID)
    {
        var transactions = plan.getTransactions(client);

        assertThat(transactions.size(), is(1));
        assertSame(owner, transactions.get(0).getOwner());
        assertThat(transactions.get(0).getTransaction().getUUID(), is(projectionUUID));
    }

    private Object ledgerEntryValue(Transaction transaction, String method) throws Exception
    {
        Object entry = ledgerEntry(transaction);
        return entry.getClass().getMethod(method).invoke(entry);
    }

    private Object ledgerProjectionValue(Transaction transaction, String method) throws Exception
    {
        Object projection = transaction.getClass().getMethod("getLedgerProjectionRef").invoke(transaction);
        return projection.getClass().getMethod(method).invoke(projection);
    }

    private Object ledgerEntry(Transaction transaction) throws Exception
    {
        return transaction.getClass().getMethod("getLedgerEntry").invoke(transaction);
    }

    private Object ledgerPosting(Transaction transaction, String type) throws Exception
    {
        var entry = ledgerEntry(transaction);
        var postings = (Iterable<?>) entry.getClass().getMethod("getPostings").invoke(entry);

        for (var posting : postings)
        {
            if (type.equals(posting.getClass().getMethod("getType").invoke(posting).toString()))
                return posting;
        }

        throw new AssertionError("Missing ledger posting of type " + type);
    }

    private Client reloadXml(Client client) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(saveXml(client).getBytes(StandardCharsets.UTF_8)));
    }

    private Client reloadProtobuf(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-all-transactions-view", ".portfolio");

        try
        {
            Files.write(file.toPath(), saveProtobuf(client));
            return ClientFactory.load(file, null, new NullProgressMonitor());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private String saveXml(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-all-transactions-view", ".xml");

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

    private byte[] saveProtobuf(Client client) throws Exception
    {
        var file = File.createTempFile("ledger-all-transactions-view", ".portfolio");

        try
        {
            ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
            return Files.readAllBytes(file.toPath());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private long ledgerPostingShares(Transaction transaction) throws Exception
    {
        var entry = ledgerEntry(transaction);
        var postings = (List<?>) entry.getClass().getMethod("getPostings").invoke(entry);

        return postings.stream().map(posting -> {
            try
            {
                return Long.valueOf((long) posting.getClass().getMethod("getShares").invoke(posting));
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalStateException(e);
            }
        }).filter(shares -> shares.longValue() != 0).findFirst().orElseThrow().longValue();
    }

    private LocalDateTime ledgerPostingExDate(Transaction transaction) throws Exception
    {
        var entry = ledgerEntry(transaction);
        var postings = (List<?>) entry.getClass().getMethod("getPostings").invoke(entry);

        for (Object posting : postings)
        {
            var parameters = (List<?>) posting.getClass().getMethod("getParameters").invoke(posting);
            for (Object parameter : parameters)
            {
                if ("EX_DATE".equals(parameter.getClass().getMethod("getType").invoke(parameter).toString()))
                    return (LocalDateTime) parameter.getClass().getMethod("getValue").invoke(parameter);
            }
        }

        throw new IllegalStateException("Ledger EX_DATE parameter was not found");
    }

    private InvestmentPlan addInvestmentPlanRef(Client client, Transaction transaction) throws Exception
    {
        var plan = new InvestmentPlan("Plan");
        client.addPlan(plan);

        Object entry = ledgerEntry(transaction);
        Object projectionRef = transaction.getClass().getMethod("getLedgerProjectionRef").invoke(transaction);
        var executionRef = new InvestmentPlan.LedgerExecutionRef();

        setField(executionRef, "ledgerEntryUUID", entry.getClass().getMethod("getUUID").invoke(entry));
        setField(executionRef, "projectionUUID", projectionRef.getClass().getMethod("getUUID").invoke(projectionRef));
        setField(executionRef, "projectionRole", projectionRef.getClass().getMethod("getRole").invoke(projectionRef));

        plan.addLedgerExecutionRef(executionRef);

        return plan;
    }

    private void setField(Object object, String name, Object value) throws Exception
    {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }


    private void assertLedgerStructurallyValid(Client client) throws Exception
    {
        var ledger = Client.class.getMethod("getLedger").invoke(client);
        Class<?> validator = Class.forName("name.abuchen.portfolio.model.ledger.LedgerStructuralValidator", true,
                        Client.class.getClassLoader());
        Method validate = validator.getMethod("validate", ledger.getClass());
        Object result = validate.invoke(null, ledger);
        assertThat((Boolean) result.getClass().getMethod("isOK").invoke(result), is(true));
    }

    private Fixture ledgerAccountTransferFixture()
    {
        var fixture = baseAccountFixture();

        new LedgerAccountTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, Values.Amount.factorize(123),
                        CurrencyUnit.EUR, null, null, "note", "source");

        return fixture;
    }

    private Fixture ledgerAccountOnlyFixture(AccountTransaction.Type type)
    {
        var fixture = baseAccountFixture();

        new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.source(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, null, List.of(), "note", "source");

        return fixture;
    }

    private Fixture ledgerAccountOnlyWithSecurityFixture(AccountTransaction.Type type)
    {
        var fixture = baseAccountFixtureWithSecurity();

        new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.source(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(), List.of(), "note",
                        "source");

        return fixture;
    }

    private Fixture ledgerDividendFixture()
    {
        var fixture = baseAccountFixtureWithSecurity();

        new LedgerDividendTransactionCreator(fixture.client()).create(fixture.source(), DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), DATE_TIME.plusDays(2), null, null, List.of(), "note", "source");

        return fixture;
    }

    private Fixture legacyDividendFixture()
    {
        var fixture = baseAccountFixtureWithSecurity();
        var transaction = new AccountTransaction(AccountTransaction.Type.DIVIDENDS);

        transaction.setDateTime(DATE_TIME);
        transaction.setAmount(Values.Amount.factorize(123));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setSecurity(fixture.security());
        transaction.setShares(Values.Share.factorize(5));
        transaction.setExDate(DATE_TIME.plusDays(2));
        transaction.setNote("note");
        fixture.source().addTransaction(transaction);

        return fixture;
    }

    private BuySellFixture ledgerBuySellFixture(PortfolioTransaction.Type type)
    {
        var fixture = baseSecurityFixture();

        new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(), type,
                        DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), List.of(), "note", "source");

        return fixture;
    }

    private BuySellFixture ledgerDeliveryFixture(PortfolioTransaction.Type type)
    {
        var fixture = baseSecurityFixture();

        new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(), Values.Share.factorize(5),
                        null, null, List.of(), "note", "source");

        return fixture;
    }

    private PortfolioFixture ledgerPortfolioTransferFixture()
    {
        var fixture = basePortfolioFixture();

        new LedgerPortfolioTransferTransactionCreator(fixture.client()).create(fixture.source(), fixture.target(),
                        fixture.security(), DATE_TIME, Values.Share.factorize(5), Values.Amount.factorize(123),
                        CurrencyUnit.EUR, "note", "source");

        return fixture;
    }

    private Fixture legacyAccountOnlyFixture(AccountTransaction.Type type)
    {
        var fixture = baseAccountFixture();
        var transaction = new AccountTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setAmount(Values.Amount.factorize(123));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setNote("note");
        fixture.source().addTransaction(transaction);

        return fixture;
    }

    private LegacyAccountTransferFixture legacyAccountTransferFixture()
    {
        var fixture = baseAccountFixture();
        var transfer = new AccountTransferEntry(fixture.source(), fixture.target());

        transfer.setDate(DATE_TIME);
        transfer.setAmount(Values.Amount.factorize(123));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("note");
        transfer.insert();

        return new LegacyAccountTransferFixture(fixture.client(), fixture.source(), fixture.target(), transfer);
    }

    private LegacyBuySellFixture legacyBuySellFixture(PortfolioTransaction.Type type)
    {
        var fixture = baseSecurityFixture();
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

    private BuySellFixture legacyDeliveryFixture(PortfolioTransaction.Type type)
    {
        var fixture = baseSecurityFixture();
        var transaction = new PortfolioTransaction(type);

        transaction.setDateTime(DATE_TIME);
        transaction.setSecurity(fixture.security());
        transaction.setShares(Values.Share.factorize(5));
        transaction.setAmount(Values.Amount.factorize(123));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setNote("note");
        fixture.portfolio().addTransaction(transaction);

        return fixture;
    }

    private LegacyPortfolioTransferFixture legacyPortfolioTransferFixture()
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

        return new LegacyPortfolioTransferFixture(fixture.client(), fixture.source(), fixture.target(),
                        fixture.security(), transfer);
    }

    private Fixture baseAccountFixture()
    {
        var client = new Client();
        var source = account("Source", CurrencyUnit.EUR);
        var target = account("Target", CurrencyUnit.EUR);

        client.addAccount(source);
        client.addAccount(target);

        return new Fixture(client, source, target, null);
    }

    private Fixture baseAccountFixtureWithSecurity()
    {
        var client = new Client();
        var source = account("Source", CurrencyUnit.EUR);
        var target = account("Target", CurrencyUnit.EUR);
        var security = security();

        client.addAccount(source);
        client.addAccount(target);
        client.addSecurity(security);

        return new Fixture(client, source, target, security);
    }

    private BuySellFixture baseSecurityFixture()
    {
        var client = new Client();
        var account = account("Account", CurrencyUnit.EUR);
        var portfolio = new Portfolio("Portfolio");
        var security = security();

        portfolio.setReferenceAccount(account);
        portfolio.setUpdatedAt(FIXTURE_UPDATED_AT);

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new BuySellFixture(client, account, portfolio, security);
    }

    private PortfolioFixture basePortfolioFixture()
    {
        var client = new Client();
        var source = new Portfolio("Source");
        var target = new Portfolio("Target");
        var security = security();

        source.setUpdatedAt(FIXTURE_UPDATED_AT);
        target.setUpdatedAt(FIXTURE_UPDATED_AT);

        client.addPortfolio(source);
        client.addPortfolio(target);
        client.addSecurity(security);

        return new PortfolioFixture(client, source, target, security);
    }

    private Account account(String name, String currency)
    {
        var account = new Account(name);

        account.setCurrencyCode(currency);
        account.setUpdatedAt(FIXTURE_UPDATED_AT);

        return account;
    }

    private Security security()
    {
        var security = new Security("Security", CurrencyUnit.EUR);
        security.setUpdatedAt(FIXTURE_UPDATED_AT);

        return security;
    }

    private record Fixture(Client client, Account source, Account target, Security security)
    {
    }

    private record BuySellFixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record PortfolioFixture(Client client, Portfolio source, Portfolio target, Security security)
    {
    }

    private record LegacyAccountTransferFixture(Client client, Account source, Account target,
                    AccountTransferEntry transfer)
    {
    }

    private record LegacyBuySellFixture(Client client, Account account, Portfolio portfolio, Security security,
                    BuySellEntry entry)
    {
    }

    private record LegacyPortfolioTransferFixture(Client client, Portfolio source, Portfolio target, Security security,
                    PortfolioTransferEntry transfer)
    {
    }
}
