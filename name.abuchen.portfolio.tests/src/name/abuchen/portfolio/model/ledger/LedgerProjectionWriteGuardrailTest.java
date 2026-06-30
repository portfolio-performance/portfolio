package name.abuchen.portfolio.model.ledger;

import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.DATE_TIME;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.account;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.assertMetadataSetterUpdatesLedgerTruthOnly;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.assertRejectedWithoutChangingState;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.cashLeg;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.creator;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.deliveryLeg;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.metadata;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.money;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.portfolio;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.portfolioLeg;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.security;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.time.Instant;

import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividend;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerOptionalSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests write guardrails on ledger-backed runtime projections.
 * These tests make sure safe metadata edits update the ledger while structural setters remain blocked.
 */
@SuppressWarnings("nls")
public class LedgerProjectionWriteGuardrailTest
{
    /**
     * Verifies that ledger-backed metadata setters update the ledger entry itself.
     * Date, note, and source remain safe projection edits because they write the persisted truth.
     */
    @Test
    public void testLedgerBackedMetadataSettersUpdateLedgerTruth()
    {
        var client = new Client();
        var account = account();

        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);
        var ledgerBacked = (LedgerBackedTransaction) transaction;

        transaction.setDateTime(DATE_TIME.plusDays(2));
        transaction.setNote("");
        transaction.setSource(null);
        transaction.setSource("");

        assertThat(ledgerBacked.getLedgerEntry().getDateTime(), is(DATE_TIME.plusDays(2)));
        assertThat(ledgerBacked.getLedgerEntry().getNote(), is(""));
        assertThat(ledgerBacked.getLedgerEntry().getSource(), is(""));
        assertThat(transaction.getDateTime(), is(DATE_TIME.plusDays(2)));
        assertThat(transaction.getNote(), is(""));
        assertThat(transaction.getSource(), is(""));

        transaction.setNote(null);
        transaction.setSource(null);

        assertNull(ledgerBacked.getLedgerEntry().getNote());
        assertNull(ledgerBacked.getLedgerEntry().getSource());
        assertNull(transaction.getNote());
        assertNull(transaction.getSource());
    }

    /**
     * Verifies that setting a null date is rejected without changing ledger truth.
     * The projection and ledger entry must both keep the original date.
     */
    @Test
    public void testLedgerBackedSetDateTimeNullFailsWithoutMutatingLedgerTruth()
    {
        var client = new Client();
        var account = account();

        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);
        var ledgerBacked = (LedgerBackedTransaction) transaction;

        assertThrows(IllegalArgumentException.class, () -> transaction.setDateTime(null));

        assertThat(ledgerBacked.getLedgerEntry().getDateTime(), is(DATE_TIME));
        assertThat(transaction.getDateTime(), is(DATE_TIME));
    }

    /**
     * Verifies that structural setters stay blocked on ledger-backed projections.
     * Amount, currency, security, unit, and type changes need explicit ledger editors.
     */
    @Test
    public void testLedgerBackedStructuralSettersRemainBlocked()
    {
        var client = new Client();
        var account = account();

        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);

        assertThrows(UnsupportedOperationException.class, () -> transaction.setAmount(1L));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setCurrencyCode(CurrencyUnit.USD));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setSecurity(security()));
        assertThrows(UnsupportedOperationException.class, () -> transaction.addUnit(new Unit(Unit.Type.FEE, money(1))));
        assertThrows(UnsupportedOperationException.class, transaction::clearUnits);
        assertThrows(UnsupportedOperationException.class, () -> transaction.setType(AccountTransaction.Type.REMOVAL));
    }

    /**
     * Verifies that account projection setters reject structural writes without partial mutation.
     * Metadata writes remain safe, but business facts must not be changed through legacy setters.
     */
    @Test
    public void testLedgerBackedAccountProjectionSettersRejectWithoutPartialMutation()
    {
        var client = new Client();
        var source = account();
        var target = account();

        creator(client).createDeposit(metadata(), cashLeg(source, 100));
        creator(client).createDividend(metadata(), LedgerDividend.withExDate(cashLeg(source, 20),
                        LedgerOptionalSecurity.of(security()), LedgerCreationUnits.of(LedgerCreationUnit.fee(money(1))),
                        DATE_TIME.minusDays(1)));
        creator(client).createBuy(metadata(), cashLeg(source, 30), portfolioLeg(portfolio(), 30),
                        LedgerCreationUnits.of(LedgerCreationUnit.tax(money(2))));
        creator(client).createSell(metadata(), cashLeg(source, 35), portfolioLeg(portfolio(), 35),
                        LedgerCreationUnits.none());
        creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(40)),
                        LedgerCashTransferLeg.of(target, money(40)));
        LedgerProjectionService.materialize(client);

        var index = 0;
        for (var transaction : source.getTransactions())
        {
            assertMetadataSetterUpdatesLedgerTruthOnly(transaction, ++index);
            assertAccountProjectionRejectsStructuralMutationWithoutChangingState(transaction);
        }

        for (var transaction : target.getTransactions())
        {
            assertMetadataSetterUpdatesLedgerTruthOnly(transaction, ++index);
            assertAccountProjectionRejectsStructuralMutationWithoutChangingState(transaction);
        }
    }

    /**
     * Verifies that portfolio projection setters reject structural writes without partial mutation.
     * Metadata writes remain safe, but security and share facts must stay owned by the ledger.
     */
    @Test
    public void testLedgerBackedPortfolioProjectionSettersRejectWithoutPartialMutation()
    {
        var client = new Client();
        var source = portfolio();
        var target = portfolio();

        creator(client).createInboundDelivery(metadata(), deliveryLeg(source));
        creator(client).createBuy(metadata(), cashLeg(account(), 30), portfolioLeg(source, 30),
                        LedgerCreationUnits.of(LedgerCreationUnit.tax(money(2))));
        creator(client).createSell(metadata(), cashLeg(account(), 35), portfolioLeg(source, 35),
                        LedgerCreationUnits.none());
        creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(40)),
                        LedgerPortfolioTransferLeg.of(target, money(40)));
        LedgerProjectionService.materialize(client);

        var index = 0;
        for (var transaction : source.getTransactions())
        {
            assertMetadataSetterUpdatesLedgerTruthOnly(transaction, ++index);
            assertPortfolioProjectionRejectsStructuralMutationWithoutChangingState(transaction);
        }

        for (var transaction : target.getTransactions())
        {
            assertMetadataSetterUpdatesLedgerTruthOnly(transaction, ++index);
            assertPortfolioProjectionRejectsStructuralMutationWithoutChangingState(transaction);
        }
    }

    private void assertAccountProjectionRejectsStructuralMutationWithoutChangingState(AccountTransaction transaction)
    {
        assertRejectedWithoutChangingState(transaction, t -> t.setType(AccountTransaction.Type.REMOVAL));
        assertRejectedWithoutChangingState(transaction, t -> t.setAmount(1L));
        assertRejectedWithoutChangingState(transaction, t -> t.setCurrencyCode(CurrencyUnit.USD));
        assertRejectedWithoutChangingState(transaction, t -> t.setMonetaryAmount(Money.of(CurrencyUnit.USD, 1L)));
        assertRejectedWithoutChangingState(transaction, t -> t.setSecurity(security()));
        assertRejectedWithoutChangingState(transaction, t -> t.setShares(1L));
        assertRejectedWithoutChangingState(transaction, t -> t.setUpdatedAt(Instant.EPOCH));
        assertRejectedWithoutChangingState(transaction, t -> t.addUnit(new Unit(Unit.Type.FEE, money(1))));
        assertRejectedWithoutChangingState(transaction, Transaction::clearUnits);
        assertRejectedWithoutChangingState(transaction, t -> t.removeUnits(Unit.Type.FEE));
    }

    private void assertPortfolioProjectionRejectsStructuralMutationWithoutChangingState(PortfolioTransaction transaction)
    {
        assertRejectedWithoutChangingState(transaction, t -> t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertRejectedWithoutChangingState(transaction, t -> t.setAmount(1L));
        assertRejectedWithoutChangingState(transaction, t -> t.setCurrencyCode(CurrencyUnit.USD));
        assertRejectedWithoutChangingState(transaction, t -> t.setMonetaryAmount(Money.of(CurrencyUnit.USD, 1L)));
        assertRejectedWithoutChangingState(transaction, t -> t.setSecurity(security()));
        assertRejectedWithoutChangingState(transaction, t -> t.setShares(1L));
        assertRejectedWithoutChangingState(transaction, t -> t.setUpdatedAt(Instant.EPOCH));
        assertRejectedWithoutChangingState(transaction, t -> t.addUnit(new Unit(Unit.Type.FEE, money(1))));
        assertRejectedWithoutChangingState(transaction, Transaction::clearUnits);
        assertRejectedWithoutChangingState(transaction, t -> t.removeUnits(Unit.Type.FEE));
    }
}
