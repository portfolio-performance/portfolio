package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
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
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCashCompensation;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCorporateActionEvent;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeEntryMetadata;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeSecurityLeg;
import name.abuchen.portfolio.model.ledger.nativeentry.Ratio;
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
        var descriptor = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).get(0);
        var projection = LedgerProjectionService.createProjection(entry, descriptor.getRole());

        assertThat(projection, instanceOf(LedgerBackedAccountTransaction.class));
        assertThat(projection.getUUID(), is(descriptor.getRuntimeProjectionId()));
        assertThat(((AccountTransaction) projection).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(projection.getDateTime(), is(DATE_TIME));
        assertThat(projection.getNote(), is("note"));
        assertThat(projection.getSource(), is("source"));
        assertThat(projection.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(projection.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    @Test
    public void testMaterializationUsesDerivedDescriptorWhenProjectionRefsAreAbsent()
    {
        var client = new Client();
        var account = account();
        var entry = creator(client).createDeposit(metadata(), cashLeg(account, 100)).getEntry();

        LedgerProjectionService.materialize(client);

        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(account.getTransactions().get(0).getAmount(), is(Values.Amount.factorize(100)));
    }

    @Test
    public void testFixedShapeMaterializationUsesDescriptorsWithoutProjectionRefs()
    {
        assertDescriptorMaterializesBuySell(PortfolioTransaction.Type.BUY, AccountTransaction.Type.BUY);
        assertDescriptorMaterializesBuySell(PortfolioTransaction.Type.SELL, AccountTransaction.Type.SELL);
        assertDescriptorMaterializesDelivery(true);
        assertDescriptorMaterializesDelivery(false);
        assertDescriptorMaterializesCashTransfer();
        assertDescriptorMaterializesSecurityTransfer();
    }

    @Test
    public void testSiemensSpinOffMaterializesFromDerivedDescriptorsWithoutProjectionRefs()
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        var siemens = new Security("Siemens AG", CurrencyUnit.EUR);
        var siemensEnergy = new Security("Siemens Energy AG", CurrencyUnit.EUR);

        siemens.setIsin("DE0007236101");
        siemensEnergy.setIsin("DE000ENER6Y0");
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(siemens);
        client.addSecurity(siemensEnergy);

        var entry = LedgerNativeEntryAssembler.forClient(client).spinOff() //
                        .metadata(NativeEntryMetadata.of(DATE_TIME).note("Siemens spin-off").source("test")) //
                        .event(NativeCorporateActionEvent.builder() //
                                        .kind(CorporateActionKind.SPIN_OFF) //
                                        .subtype(CorporateActionSubtype.STANDARD) //
                                        .stage(EventStage.SETTLED) //
                                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                                        .build()) //
                        .securityLeg(NativeSecurityLeg.source() //
                                        .portfolio(portfolio) //
                                        .security(siemens) //
                                        .shares(Values.Share.factorize(10)) //
                                        .amount(money(100)) //
                                        .sourceSecurity(siemens) //
                                        .targetSecurity(siemensEnergy) //
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                                        .build()) //
                        .securityLeg(NativeSecurityLeg.target() //
                                        .portfolio(portfolio) //
                                        .security(siemens) //
                                        .shares(Values.Share.factorize(10)) //
                                        .amount(money(100)) //
                                        .sourceSecurity(siemens) //
                                        .targetSecurity(siemensEnergy) //
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                                        .projectAs(LedgerProjectionRole.DELIVERY_INBOUND) //
                                        .build()) //
                        .securityLeg(NativeSecurityLeg.target() //
                                        .portfolio(portfolio) //
                                        .security(siemensEnergy) //
                                        .shares(Values.Share.factorize(5)) //
                                        .amount(money(50)) //
                                        .sourceSecurity(siemens) //
                                        .targetSecurity(siemensEnergy) //
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                                        .projectAs(LedgerProjectionRole.NEW_SECURITY_LEG) //
                                        .build()) //
                        .cashCompensation(NativeCashCompensation.builder() //
                                        .account(account) //
                                        .amount(money(5)) //
                                        .kind(CashCompensationKind.CASH_IN_LIEU) //
                                        .build()) //
                        .buildDetached().getEntry();

        client.getLedger().addEntry(entry);

        LedgerProjectionService.materialize(client);

        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(4));
        assertThat(portfolio.getTransactions().size(), is(3));
        assertThat(account.getTransactions().size(), is(1));
        assertTrue(portfolio.getTransactions().stream()
                        .anyMatch(transaction -> transaction.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND
                                        && transaction.getSecurity() == siemens));
        assertTrue(portfolio.getTransactions().stream()
                        .anyMatch(transaction -> transaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                        && transaction.getSecurity() == siemens));
        assertTrue(portfolio.getTransactions().stream()
                        .anyMatch(transaction -> transaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                        && transaction.getSecurity() == siemensEnergy));
        assertThat(account.getTransactions().get(0).getType(), is(AccountTransaction.Type.DEPOSIT));
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

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerProjectionService.createProjection(entry,
                                        name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport
                                                        .descriptors(entry).get(0).getRole()));

        assertThat(exception.getMessage(), is(
                        "[MISSING_SEMANTIC_PRIMARY] entry=" + entry.getUUID() //$NON-NLS-1$
                                        + " role=DELIVERY_INBOUND Missing semantic primary posting")); //$NON-NLS-1$
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

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerProjectionService.createProjection(entry,
                                        name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport
                                                        .descriptors(entry).get(0).getRole()));

        assertThat(exception.getMessage(), is("[MISSING_SEMANTIC_PRIMARY] entry=" + entry.getUUID() //$NON-NLS-1$
                        + " role=ACCOUNT Semantic account owner is missing")); //$NON-NLS-1$
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

        entry.setType(type);
        posting.setType(LedgerPostingType.CASH);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setAccount(account);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        entry.addPosting(posting);

        return entry;
    }

    private LedgerEntry portfolioProjectionEntry(LedgerEntryType type)
    {
        var portfolio = portfolio();
        var entry = new LedgerEntry();
        var posting = new LedgerPosting("posting-1");

        entry.setType(type);
        posting.setType(LedgerPostingType.SECURITY);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSecurity(security());
        posting.setShares(Values.Share.factorize(5));
        posting.setPortfolio(portfolio);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        entry.addPosting(posting);

        return entry;
    }

    private void assertDescriptorMaterializesBuySell(PortfolioTransaction.Type portfolioType,
                    AccountTransaction.Type accountType)
    {
        var client = new Client();
        var account = account();
        var portfolio = portfolio();
        var entry = portfolioType == PortfolioTransaction.Type.BUY
                        ? creator(client).createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                                        LedgerCreationUnits.of(LedgerCreationUnit.fee(money(1)))).getEntry()
                        : creator(client).createSell(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                                        LedgerCreationUnits.of(LedgerCreationUnit.tax(money(2)))).getEntry();

        LedgerProjectionService.materialize(client);

        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(2));
        assertThat(account.getTransactions().get(0).getType(), is(accountType));
        assertThat(portfolio.getTransactions().get(0).getType(), is(portfolioType));
        assertSame(account.getTransactions().get(0).getCrossEntry(), portfolio.getTransactions().get(0).getCrossEntry());
    }

    private void assertDescriptorMaterializesDelivery(boolean inbound)
    {
        var client = new Client();
        var portfolio = portfolio();
        var security = security();
        var entry = inbound
                        ? creator(client).createInboundDelivery(metadata(), LedgerDeliveryLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100)))
                                        .getEntry()
                        : creator(client).createOutboundDelivery(metadata(), LedgerDeliveryLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100)))
                                        .getEntry();

        LedgerProjectionService.materialize(client);

        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(1));
        assertThat(portfolio.getTransactions().get(0).getType(), is(inbound
                        ? PortfolioTransaction.Type.DELIVERY_INBOUND
                        : PortfolioTransaction.Type.DELIVERY_OUTBOUND));
    }

    private void assertDescriptorMaterializesCashTransfer()
    {
        var client = new Client();
        var source = account();
        var target = account();
        var entry = creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100))).getEntry();

        moveFirstPostingToEnd(entry);
        LedgerProjectionService.materialize(client);

        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(2));
        assertThat(source.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(target.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(target.getTransactions().get(0),
                        source.getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(source.getTransactions().get(0)));
    }

    private void assertDescriptorMaterializesSecurityTransfer()
    {
        var client = new Client();
        var source = portfolio();
        var target = portfolio();
        var entry = creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();

        moveFirstPostingToEnd(entry);
        LedgerProjectionService.materialize(client);

        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(2));
        assertThat(source.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(target.getTransactions().get(0).getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(target.getTransactions().get(0),
                        source.getTransactions().get(0).getCrossEntry()
                                        .getCrossTransaction(source.getTransactions().get(0)));
    }

    private void moveFirstPostingToEnd(LedgerEntry entry)
    {
        var posting = entry.getPostings().get(0);

        entry.removePosting(posting);
        entry.addPosting(posting);
    }
}
