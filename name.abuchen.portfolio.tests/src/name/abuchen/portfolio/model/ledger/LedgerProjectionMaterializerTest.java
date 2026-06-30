package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividend;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerForexAmount;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerOptionalSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests rebuilding runtime transaction rows from ledger entries.
 * These tests make sure account and portfolio views are derived from ledger truth without duplicate rows.
 */
@SuppressWarnings("nls")
public class LedgerProjectionMaterializerTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);

    /**
     * Checks the projection rebuild scenario: service creates account backed deposit projection.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testServiceCreatesAccountBackedDepositProjection()
    {
        var client = new Client();
        var account = account();
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();
        var projectionRef = entry.getProjectionRefs().get(0);
        var projection = LedgerProjectionService.createProjection(entry, projectionRef);

        assertThat(projection, instanceOf(LedgerBackedAccountTransaction.class));
        assertThat(projection.getUUID(), is(projectionRef.getUUID()));
        assertThat(((AccountTransaction) projection).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(projection.getDateTime(), is(DATE_TIME));
        assertThat(projection.getNote(), is("note"));
        assertThat(projection.getSource(), is("source"));
        assertThat(projection.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(projection.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    /**
     * Checks the projection rebuild scenario: materializer adds account projection only.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testMaterializerAddsAccountProjectionOnly()
    {
        var client = new Client();
        var account = account();
        var portfolio = new Portfolio();

        client.addAccount(account);
        client.addPortfolio(portfolio);
        creator(client).createDeposit(metadata(), cashLeg(account, 100));

        LedgerProjectionService.materialize(client);

        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedAccountTransaction.class));
        assertTrue(portfolio.getTransactions().isEmpty());
    }

    /**
     * Checks the projection rebuild scenario: dividend projection exposes ex-date and units.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testDividendProjectionExposesExDateAndUnits()
    {
        var client = new Client();
        var account = account();
        var security = security();
        var exDate = LocalDateTime.of(2025, 12, 20, 0, 0);
        var forex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(40)),
                        BigDecimal.valueOf(0.90));
        var units = LedgerCreationUnits.of(LedgerCreationUnit.tax(money(3)), LedgerCreationUnit.fee(money(2)),
                        LedgerCreationUnit.grossValue(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36)),
                                        forex));
        var dividend = LedgerDividend.withExDate(cashLeg(account, 30), LedgerOptionalSecurity.of(security), units,
                        exDate);

        creator(client).createDividend(metadata(), dividend);

        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);
        var grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getExDate(), is(exDate));
        assertThat(transaction.getUnit(Unit.Type.TAX).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(3)));
        assertThat(transaction.getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(2)));
        assertThat(grossValue.getAmount().getAmount(), is(Values.Amount.factorize(36)));
        assertThat(grossValue.getForex().getAmount(), is(Values.Amount.factorize(40)));
        assertThat(grossValue.getExchangeRate(), is(BigDecimal.valueOf(0.90)));
    }

    /**
     * Checks the projection rebuild scenario: buy materializes linked account and portfolio projections.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testBuyMaterializesLinkedAccountAndPortfolioProjections()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();

        client.addAccount(account);
        client.addPortfolio(portfolio);
        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.of(LedgerCreationUnit.fee(money(1))));

        LedgerProjectionService.materialize(client);

        var accountTransaction = account.getTransactions().get(0);
        var portfolioTransaction = portfolio.getTransactions().get(0);

        assertThat(accountTransaction, instanceOf(LedgerBackedAccountTransaction.class));
        assertThat(portfolioTransaction, instanceOf(LedgerBackedPortfolioTransaction.class));
        assertThat(accountTransaction.getType(), is(AccountTransaction.Type.BUY));
        assertThat(portfolioTransaction.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(account.getTransactions(), is(List.of(accountTransaction)));
        assertThat(portfolio.getTransactions(), is(List.of(portfolioTransaction)));
        assertSame(accountTransaction.getCrossEntry(), portfolioTransaction.getCrossEntry());
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertSame(portfolio, accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
        assertThat(portfolioTransaction.getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(1)));
    }

    /**
     * Checks the projection rebuild scenario: sell materializes linked projections.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testSellMaterializesLinkedProjections()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();

        creator(client).createSell(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());

        LedgerProjectionService.materialize(client);

        assertThat(account.getTransactions().get(0).getType(), is(AccountTransaction.Type.SELL));
        assertThat(portfolio.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.SELL));
        assertSame(account.getTransactions().get(0).getCrossEntry(), portfolio.getTransactions().get(0).getCrossEntry());
    }

    /**
     * Checks the projection rebuild scenario: account transfer materializes transfer out and transfer in.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testAccountTransferMaterializesTransferOutAndTransferIn()
    {
        var client = new Client();
        var source = account();
        var target = account();

        creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100)));

        LedgerProjectionService.materialize(client);

        var sourceTransaction = source.getTransactions().get(0);
        var targetTransaction = target.getTransactions().get(0);

        assertThat(sourceTransaction.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(source.getTransactions(), is(List.of(sourceTransaction)));
        assertThat(target.getTransactions(), is(List.of(targetTransaction)));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
    }

    /**
     * Checks the projection rebuild scenario: portfolio transfer materializes transfer out and transfer in.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testPortfolioTransferMaterializesTransferOutAndTransferIn()
    {
        var client = new Client();
        var source = portfolio();
        var target = portfolio();

        creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100)));

        LedgerProjectionService.materialize(client);

        var sourceTransaction = source.getTransactions().get(0);
        var targetTransaction = target.getTransactions().get(0);

        assertThat(sourceTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(targetTransaction.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(source.getTransactions(), is(List.of(sourceTransaction)));
        assertThat(target.getTransactions(), is(List.of(targetTransaction)));
        assertSame(targetTransaction, sourceTransaction.getCrossEntry().getCrossTransaction(sourceTransaction));
        assertSame(target, sourceTransaction.getCrossEntry().getCrossOwner(sourceTransaction));
    }

    /**
     * Checks the projection rebuild scenario: delivery materializes one portfolio projection.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testDeliveryMaterializesOnePortfolioProjection()
    {
        var client = new Client();
        var portfolio = portfolio();
        var security = security();

        creator(client).createInboundDelivery(metadata(), LedgerDeliveryLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100)));

        LedgerProjectionService.materialize(client);

        var transaction = portfolio.getTransactions().get(0);

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getShares(), is(Values.Share.factorize(5)));
        assertNull(transaction.getCrossEntry());
    }

    /**
     * Checks the projection rebuild scenario: repeated materialization does not duplicate ledger backed projections.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testRepeatedMaterializationDoesNotDuplicateLedgerBackedProjections()
    {
        var client = new Client();
        var account = account();

        creator(client).createDeposit(metadata(), cashLeg(account, 100));

        LedgerProjectionService.materialize(client);
        LedgerProjectionService.materialize(client);

        assertThat(account.getTransactions().size(), is(1));
    }

    /**
     * Checks the projection rebuild scenario: client all transactions sees deduplicated materialized views.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testClientAllTransactionsSeesDeduplicatedMaterializedViews()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        var source = account();
        var target = account();

        client.addAccount(account);
        client.addAccount(source);
        client.addAccount(target);
        client.addPortfolio(portfolio);

        creator(client).createDeposit(metadata(), cashLeg(account, 100));
        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(10)),
                        LedgerCashTransferLeg.of(target, money(10)));

        LedgerProjectionService.materialize(client);

        List<Transaction> transactions = client.getAllTransactions().stream().map(pair -> (Transaction) pair.getTransaction())
                        .toList();

        assertThat(transactions.size(), is(3));
        assertTrue(transactions.stream().anyMatch(t -> t instanceof LedgerBackedAccountTransaction
                        && ((AccountTransaction) t).getType() == AccountTransaction.Type.DEPOSIT));
        assertTrue(transactions.stream().anyMatch(t -> t instanceof LedgerBackedPortfolioTransaction
                        && ((PortfolioTransaction) t).getType() == PortfolioTransaction.Type.BUY));
        assertTrue(transactions.stream().anyMatch(t -> t instanceof LedgerBackedAccountTransaction
                        && ((AccountTransaction) t).getType() == AccountTransaction.Type.TRANSFER_OUT));
    }

    /**
     * Checks the projection rebuild scenario: ledger backed setters are rejected.
     * Account and portfolio lists must be derived from the ledger entry.
     * This protects Ledger-V6 from stale or duplicated runtime projections.
     */
    @Test
    public void testLedgerBackedSettersAreRejected()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();

        creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.none());
        LedgerProjectionService.materialize(client);

        var transaction = account.getTransactions().get(0);

        transaction.setDateTime(DATE_TIME.plusDays(1));
        transaction.setNote("new note");
        transaction.setSource("new source");

        var ledgerTransaction = (LedgerBackedTransaction) transaction;

        assertThat(ledgerTransaction.getLedgerEntry().getDateTime(), is(DATE_TIME.plusDays(1)));
        assertThat(ledgerTransaction.getLedgerEntry().getNote(), is("new note"));
        assertThat(ledgerTransaction.getLedgerEntry().getSource(), is("new source"));

        assertThrows(UnsupportedOperationException.class, () -> transaction.setAmount(1L));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setCurrencyCode(CurrencyUnit.USD));
        assertThrows(UnsupportedOperationException.class, () -> transaction.setSecurity(security()));
        assertThrows(UnsupportedOperationException.class, () -> transaction.addUnit(new Unit(Unit.Type.FEE, money(1))));
        assertThrows(UnsupportedOperationException.class, transaction::clearUnits);
        assertThrows(UnsupportedOperationException.class, () -> transaction.setType(AccountTransaction.Type.REMOVAL));
        assertThrows(UnsupportedOperationException.class, () -> transaction.getCrossEntry().insert());
        assertThrows(UnsupportedOperationException.class, () -> transaction.getCrossEntry().setSource("new source"));
    }

    /**
     * Checks the projection rebuild scenario: unsupported account projection messages carry stable projection codes.
     * Account and portfolio lists must be derived from ledger truth.
     * This protects support diagnostics from ambiguous projection materialization failures.
     */
    @Test
    public void testUnsupportedAccountProjectionMessageHasProjectionCode()
    {
        var entry = accountProjectionEntry(LedgerEntryType.DELIVERY_INBOUND);
        var transaction = (AccountTransaction) LedgerProjectionService.createProjection(entry,
                        entry.getProjectionRefs().get(0));

        var exception = assertThrows(UnsupportedOperationException.class, transaction::getType);

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_PROJ_066
                        .message("Unsupported account projection for DELIVERY_INBOUND")));
    }

    /**
     * Checks the projection rebuild scenario: unsupported portfolio projection messages carry stable projection codes.
     * Account and portfolio lists must be derived from ledger truth.
     * This protects support diagnostics from ambiguous projection materialization failures.
     */
    @Test
    public void testUnsupportedPortfolioProjectionMessageHasProjectionCode()
    {
        var entry = portfolioProjectionEntry(LedgerEntryType.DEPOSIT);
        var transaction = (PortfolioTransaction) LedgerProjectionService.createProjection(entry,
                        entry.getProjectionRefs().get(0));

        var exception = assertThrows(UnsupportedOperationException.class, transaction::getType);

        assertThat(exception.getMessage(), is(LedgerDiagnosticCode.LEDGER_PROJ_069
                        .message("Unsupported portfolio projection for DEPOSIT")));
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

    private LedgerPortfolioSecurityLeg portfolioLeg(Portfolio portfolio, int amount)
    {
        return LedgerPortfolioSecurityLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security(), Values.Share.factorize(5)), money(amount));
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private LedgerEntry accountProjectionEntry(LedgerEntryType type)
    {
        var account = account();
        var entry = new LedgerEntry();
        var posting = new LedgerPosting("posting-1");
        var projectionRef = new LedgerProjectionRef("projection-1");

        entry.setType(type);
        posting.setType(LedgerPostingType.CASH);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setAccount(account);
        entry.addPosting(posting);

        projectionRef.setRole(LedgerProjectionRole.ACCOUNT);
        projectionRef.setAccount(account);
        projectionRef.addMembership(posting.getUUID(), ProjectionMembershipRole.PRIMARY);
        entry.addProjectionRef(projectionRef);

        return entry;
    }

    private LedgerEntry portfolioProjectionEntry(LedgerEntryType type)
    {
        var portfolio = portfolio();
        var entry = new LedgerEntry();
        var posting = new LedgerPosting("posting-1");
        var projectionRef = new LedgerProjectionRef("projection-1");

        entry.setType(type);
        posting.setType(LedgerPostingType.SECURITY);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSecurity(security());
        posting.setShares(Values.Share.factorize(5));
        posting.setPortfolio(portfolio);
        entry.addPosting(posting);

        projectionRef.setRole(LedgerProjectionRole.PORTFOLIO);
        projectionRef.setPortfolio(portfolio);
        projectionRef.addMembership(posting.getUUID(), ProjectionMembershipRole.PRIMARY);
        entry.addProjectionRef(projectionRef);

        return entry;
    }
}
