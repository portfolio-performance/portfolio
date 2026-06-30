package name.abuchen.portfolio.model.ledger;

import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.DATE_TIME;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.account;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.assertAcceptedWithoutChangingState;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.assertRejectedWithoutChangingState;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.cashLeg;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.creator;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.metadata;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.money;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.portfolio;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.portfolioLeg;
import static name.abuchen.portfolio.model.ledger.LedgerGuardrailTestSupport.security;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests write guardrails on ledger-backed cross-entry compatibility wrappers.
 * These tests make sure legacy cross-entry APIs cannot mutate runtime projections as a second source of truth.
 */
@SuppressWarnings("nls")
public class LedgerCrossEntryWriteGuardrailTest
{
    /**
     * Verifies that ledger-backed cross entries allow only replay no-op behavior.
     * Legacy write methods must not mutate runtime projections or create a second truth.
     */
    @Test
    public void testCrossEntryWriteMethodsStillRejectMutationExceptReplayNoop()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();

        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);

        assertThrows(UnsupportedOperationException.class, () -> transaction.getCrossEntry().insert());
        transaction.getCrossEntry().updateFrom(transaction);
        assertSame(portfolio, transaction.getCrossEntry().getCrossOwner(transaction));
        assertSame(portfolio.getTransactions().get(0), transaction.getCrossEntry().getCrossTransaction(transaction));
        assertThrows(UnsupportedOperationException.class,
                        () -> transaction.getCrossEntry().setOwner(transaction, account));
        assertThrows(UnsupportedOperationException.class,
                        () -> transaction.getCrossEntry().setSource("legacy source"));
        assertSame(portfolio, transaction.getCrossEntry().getCrossOwner(transaction));
        assertSame(portfolio.getTransactions().get(0), transaction.getCrossEntry().getCrossTransaction(transaction));
    }

    /**
     * Verifies that all ledger-backed cross-entry families reject legacy write methods.
     * Buy/sell, account transfers, and portfolio transfers must stay unchanged after rejected writes.
     */
    @Test
    public void testCrossEntryWriteMethodsRejectForAllLedgerBackedCrossEntryFamiliesWithoutPartialMutation()
    {
        var client = new Client();
        var account = account();
        var targetAccount = account();
        var portfolio = portfolio();
        var targetPortfolio = portfolio();

        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(account, money(40)),
                        LedgerCashTransferLeg.of(targetAccount, money(40)));
        creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(portfolio, money(50)),
                        LedgerPortfolioTransferLeg.of(targetPortfolio, money(50)));
        LedgerProjectionService.materialize(client);

        assertCrossEntryWritesRejectWithoutChangingState(account.getTransactions().get(0));
        assertCrossEntryWritesRejectWithoutChangingState(account.getTransactions().get(1));
        assertCrossEntryWritesRejectWithoutChangingState(targetAccount.getTransactions().get(0));
        assertCrossEntryWritesRejectWithoutChangingState(portfolio.getTransactions().get(1));
        assertCrossEntryWritesRejectWithoutChangingState(targetPortfolio.getTransactions().get(0));
    }

    /**
     * Verifies that ledger-backed legacy wrapper mutators reject without partial mutation.
     * Compatibility wrappers may expose old APIs, but they must not write business facts directly.
     */
    @Test
    public void testLedgerBackedLegacyWrapperMutatorsRejectWithoutPartialMutation()
    {
        var client = new Client();
        var account = account();
        var targetAccount = account();
        var portfolio = portfolio();
        var targetPortfolio = portfolio();

        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(account, money(40)),
                        LedgerCashTransferLeg.of(targetAccount, money(40)));
        creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(portfolio, money(50)),
                        LedgerPortfolioTransferLeg.of(targetPortfolio, money(50)));
        LedgerProjectionService.materialize(client);

        var buySellTransaction = account.getTransactions().get(0);
        var buySell = (BuySellEntry) buySellTransaction.getCrossEntry();
        assertSame(portfolio, buySell.getCrossOwner(buySellTransaction));
        assertSame(portfolio.getTransactions().get(0), buySell.getCrossTransaction(buySellTransaction));
        assertBuySellWrapperMutatorsRejectWithoutChangingState(buySellTransaction, buySell);

        var accountTransferTransaction = account.getTransactions().get(1);
        var accountTransfer = (AccountTransferEntry) accountTransferTransaction.getCrossEntry();
        assertSame(targetAccount, accountTransfer.getCrossOwner(accountTransferTransaction));
        assertSame(targetAccount.getTransactions().get(0),
                        accountTransfer.getCrossTransaction(accountTransferTransaction));
        assertAccountTransferWrapperMutatorsRejectWithoutChangingState(accountTransferTransaction, accountTransfer);

        var portfolioTransferTransaction = portfolio.getTransactions().get(1);
        var portfolioTransfer = (PortfolioTransferEntry) portfolioTransferTransaction.getCrossEntry();
        assertSame(targetPortfolio, portfolioTransfer.getCrossOwner(portfolioTransferTransaction));
        assertSame(targetPortfolio.getTransactions().get(0),
                        portfolioTransfer.getCrossTransaction(portfolioTransferTransaction));
        assertPortfolioTransferWrapperMutatorsRejectWithoutChangingState(portfolioTransferTransaction,
                        portfolioTransfer);
    }

    private void assertCrossEntryWritesRejectWithoutChangingState(Transaction transaction)
    {
        var crossEntry = transaction.getCrossEntry();
        var owner = crossEntry.getOwner(transaction);
        var crossTransaction = crossEntry.getCrossTransaction(transaction);
        var crossOwner = crossEntry.getCrossOwner(transaction);

        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, crossEntry::insert);
        assertAcceptedWithoutChangingState((LedgerBackedTransaction) transaction, () -> crossEntry.updateFrom(transaction));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> crossEntry.setOwner(transaction, owner));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> crossEntry.setSource("legacy source"));

        assertSame(owner, crossEntry.getOwner(transaction));
        assertSame(crossTransaction, crossEntry.getCrossTransaction(transaction));
        assertSame(crossOwner, crossEntry.getCrossOwner(transaction));
    }

    private void assertBuySellWrapperMutatorsRejectWithoutChangingState(Transaction transaction, BuySellEntry wrapper)
    {
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setPortfolio(portfolio()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setAccount(account()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setDate(DATE_TIME.plusDays(1)));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setType(PortfolioTransaction.Type.SELL));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setSecurity(security()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setShares(1L));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setAmount(1L));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setCurrencyCode(CurrencyUnit.USD));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setMonetaryAmount(Money.of(CurrencyUnit.USD, 1L)));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setNote("note"));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setSource("source"));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, wrapper::insert);
        assertAcceptedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.updateFrom(transaction));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setOwner(transaction, wrapper.getOwner(transaction)));
    }

    private void assertAccountTransferWrapperMutatorsRejectWithoutChangingState(Transaction transaction,
                    AccountTransferEntry wrapper)
    {
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setSourceTransaction(new AccountTransaction()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setTargetTransaction(new AccountTransaction()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setSourceAccount(account()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setTargetAccount(account()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setDate(DATE_TIME.plusDays(1)));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setAmount(1L));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setCurrencyCode(CurrencyUnit.USD));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setNote("note"));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setSource("source"));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, wrapper::insert);
        assertAcceptedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.updateFrom(transaction));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setOwner(transaction, wrapper.getOwner(transaction)));
    }

    private void assertPortfolioTransferWrapperMutatorsRejectWithoutChangingState(Transaction transaction,
                    PortfolioTransferEntry wrapper)
    {
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setSourceTransaction(new PortfolioTransaction()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setTargetTransaction(new PortfolioTransaction()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setSourcePortfolio(portfolio()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setTargetPortfolio(portfolio()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setDate(DATE_TIME.plusDays(1)));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setSecurity(security()));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setShares(1L));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setAmount(1L));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setCurrencyCode(CurrencyUnit.USD));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setNote("note"));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.setSource("source"));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction, wrapper::insert);
        assertAcceptedWithoutChangingState((LedgerBackedTransaction) transaction, () -> wrapper.updateFrom(transaction));
        assertRejectedWithoutChangingState((LedgerBackedTransaction) transaction,
                        () -> wrapper.setOwner(transaction, wrapper.getOwner(transaction)));
    }
}
