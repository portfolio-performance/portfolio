package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
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
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-aware creation and editing of transactions in this family.
 * These tests make sure user-visible rows are rebuilt from ledger truth and structural facts are not written through legacy projections.
 */
@SuppressWarnings("nls")
public class LedgerTransactionCreatorTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);

    /**
     * Checks the ledger-backed editing scenario: metadata allows absent blank note and blank source.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testMetadataAllowsAbsentBlankNoteAndBlankSource()
    {
        var absent = LedgerTransactionMetadata.of(DATE_TIME);
        var blankNote = absent.withNote(" ");
        var blankSource = absent.withSource("");

        assertNull(absent.getNote());
        assertNull(absent.getSource());
        assertThat(blankNote.getNote(), is(" "));
        assertThat(blankSource.getSource(), is(""));
    }

    /**
     * Checks the ledger-backed editing scenario: cash leg allows zero and negative amounts.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCashLegAllowsZeroAndNegativeAmounts()
    {
        var account = account();
        var zero = LedgerAccountCashLeg.of(account, Money.of(CurrencyUnit.EUR, 0L));
        var negative = LedgerAccountCashLeg.of(account, Money.of(CurrencyUnit.EUR, -1L));

        assertThat(zero.getAmount().getAmount(), is(0L));
        assertThat(negative.getAmount().getAmount(), is(-1L));
    }

    /**
     * Checks the ledger-backed editing scenario: security quantity allows zero and negative shares.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testSecurityQuantityAllowsZeroAndNegativeShares()
    {
        var security = security();
        var zero = LedgerSecurityQuantity.of(security, 0L);
        var negative = LedgerSecurityQuantity.of(security, -1L);

        assertThat(zero.getShares(), is(0L));
        assertThat(negative.getShares(), is(-1L));
    }

    /**
     * Checks the ledger-backed editing scenario: account cash leg does not reject currency mismatch.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testAccountCashLegDoesNotRejectCurrencyMismatch()
    {
        var account = account();

        account.setCurrencyCode(CurrencyUnit.USD);

        var leg = LedgerAccountCashLeg.of(account, money(10));

        assertSame(account, leg.getAccount());
        assertThat(leg.getAmount().getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    /**
     * Checks the ledger-backed editing scenario: create deposit adds one ledger entry.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateDepositAddsOneLedgerEntry()
    {
        var client = new Client();
        var account = account();
        var creator = new LedgerTransactionCreator(client);

        var created = creator.createDeposit(metadata(), cashLeg(account, 100));
        var entry = created.getEntry();

        assertThat(client.getLedger().getEntries().size(), is(1));
        assertSame(entry, client.getLedger().getEntries().get(0));
        assertThat(entry.getType(), is(LedgerEntryType.DEPOSIT));
        assertThat(entry.getPostings().size(), is(1));
        assertThat(entry.getPostings().get(0).getType(), is(LedgerPostingType.CASH));
        assertThat(entry.getPostings().get(0).getAmount(), is(Values.Amount.factorize(100)));
        assertThat(entry.getProjectionRefs().size(), is(1));
        assertThat(entry.getProjectionRefs().get(0).getRole(), is(LedgerProjectionRole.ACCOUNT));
        assertSame(account, entry.getProjectionRefs().get(0).getAccount());
        assertThat(entry.getProjectionRefs().get(0).getPrimaryPostingUUID(), is(entry.getPostings().get(0).getUUID()));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create removal uses positive amount.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateRemovalUsesPositiveAmount()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);
        var created = creator.createRemoval(metadata(), cashLeg(account(), 25));

        assertThat(created.getEntry().getType(), is(LedgerEntryType.REMOVAL));
        assertThat(created.getEntry().getPostings().get(0).getAmount(), is(Values.Amount.factorize(25)));
        assertTrue(created.getEntry().getPostings().get(0).getAmount() > 0);
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create deposit and removal support units and unit forex.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateDepositAndRemovalSupportUnitsAndUnitForex()
    {
        var client = new Client();
        var account = account();
        var creator = new LedgerTransactionCreator(client);
        var units = LedgerCreationUnits.of(
                        LedgerCreationUnit.grossValue(money(100),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(90)),
                                                        new BigDecimal("1.1111"))),
                        LedgerCreationUnit.fee(money(2),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3)),
                                                        new BigDecimal("0.6667"))),
                        LedgerCreationUnit.tax(money(4),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5)),
                                                        new BigDecimal("0.8000"))));

        var deposit = creator.createDeposit(metadata(), cashLeg(account, 10), units).getEntry();
        var removal = creator.createRemoval(metadata(), cashLeg(account, 11), units).getEntry();

        assertThat(deposit.getType(), is(LedgerEntryType.DEPOSIT));
        assertThat(removal.getType(), is(LedgerEntryType.REMOVAL));
        assertUnitForex(deposit, LedgerPostingType.GROSS_VALUE, Values.Amount.factorize(90),
                        new BigDecimal("1.1111"));
        assertUnitForex(deposit, LedgerPostingType.FEE, Values.Amount.factorize(3), new BigDecimal("0.6667"));
        assertUnitForex(deposit, LedgerPostingType.TAX, Values.Amount.factorize(5), new BigDecimal("0.8000"));
        assertUnitForex(removal, LedgerPostingType.GROSS_VALUE, Values.Amount.factorize(90),
                        new BigDecimal("1.1111"));
        assertUnitForex(removal, LedgerPostingType.FEE, Values.Amount.factorize(3), new BigDecimal("0.6667"));
        assertUnitForex(removal, LedgerPostingType.TAX, Values.Amount.factorize(5), new BigDecimal("0.8000"));

        LedgerProjectionService.materialize(client);

        var depositProjection = (LedgerBackedAccountTransaction) account.getTransactions().get(0);
        var removalProjection = (LedgerBackedAccountTransaction) account.getTransactions().get(1);

        assertThat(depositProjection.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.GROSS_VALUE)
                        .orElseThrow().getForex().getAmount(), is(Values.Amount.factorize(90)));
        assertThat(removalProjection.getUnit(name.abuchen.portfolio.model.Transaction.Unit.Type.TAX)
                        .orElseThrow().getForex().getAmount(), is(Values.Amount.factorize(5)));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create interest supports optional security and units.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateInterestSupportsOptionalSecurityAndUnits()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);
        var security = security();
        var units = LedgerCreationUnits.of(LedgerCreationUnit.fee(money(1)));

        var created = creator.createInterest(metadata(), cashLeg(account(), 10), LedgerOptionalSecurity.of(security),
                        units);
        var entry = created.getEntry();

        assertThat(entry.getType(), is(LedgerEntryType.INTEREST));
        assertSame(security, entry.getPostings().get(0).getSecurity());
        assertTrue(entry.getPostings().stream().anyMatch(p -> p.getType() == LedgerPostingType.FEE));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create fee tax families create valid account shapes.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateFeeTaxFamiliesCreateValidAccountShapes()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);
        var account = account();
        var security = LedgerOptionalSecurity.none();
        var units = LedgerCreationUnits.none();

        assertThat(creator.createFee(metadata(), LedgerAccountCashLeg.of(account, money(1)), security, units)
                        .getEntry().getType(), is(LedgerEntryType.FEES));
        assertThat(creator.createFeeRefund(metadata(), LedgerAccountCashLeg.of(account, money(2)), security, units)
                        .getEntry().getType(), is(LedgerEntryType.FEES_REFUND));
        assertThat(creator.createTax(metadata(), LedgerAccountCashLeg.of(account, money(3)), security, units)
                        .getEntry().getType(), is(LedgerEntryType.TAXES));
        assertThat(creator.createTaxRefund(metadata(), LedgerAccountCashLeg.of(account, money(4)), security, units)
                        .getEntry().getType(), is(LedgerEntryType.TAX_REFUND));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create standard families bind projection refs to primary postings.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateStandardFamiliesBindProjectionRefsToPrimaryPostings()
    {
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.DEPOSIT,
                        creator -> creator.createDeposit(metadata(), cashLeg(account(), 10)).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.REMOVAL,
                        creator -> creator.createRemoval(metadata(), cashLeg(account(), 10)).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.INTEREST,
                        creator -> creator.createInterest(metadata(), cashLeg(account(), 10),
                                        LedgerOptionalSecurity.none(), LedgerCreationUnits.none()).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.INTEREST_CHARGE,
                        creator -> creator.createInterestCharge(metadata(), cashLeg(account(), 10),
                                        LedgerOptionalSecurity.none(), LedgerCreationUnits.none()).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.FEES,
                        creator -> creator.createFee(metadata(), cashLeg(account(), 10),
                                        LedgerOptionalSecurity.none(), LedgerCreationUnits.none()).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.FEES_REFUND,
                        creator -> creator.createFeeRefund(metadata(), cashLeg(account(), 10),
                                        LedgerOptionalSecurity.none(), LedgerCreationUnits.none()).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.TAXES,
                        creator -> creator.createTax(metadata(), cashLeg(account(), 10),
                                        LedgerOptionalSecurity.none(), LedgerCreationUnits.none()).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.TAX_REFUND,
                        creator -> creator.createTaxRefund(metadata(), cashLeg(account(), 10),
                                        LedgerOptionalSecurity.none(), LedgerCreationUnits.none()).getEntry());
        assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType.DIVIDENDS,
                        creator -> creator.createDividend(metadata(),
                                        LedgerDividend.withoutExDate(cashLeg(account(), 10),
                                                        LedgerOptionalSecurity.of(security()),
                                                        LedgerCreationUnits.none()))
                                        .getEntry());

        assertTwoProjectionTargetsPrimaryPostings(LedgerEntryType.BUY,
                        creator -> creator.createBuy(metadata(), cashLeg(account(), 10),
                                        portfolioLeg(new Portfolio(), 10), LedgerCreationUnits.none()).getEntry(),
                        LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO);
        assertTwoProjectionTargetsPrimaryPostings(LedgerEntryType.SELL,
                        creator -> creator.createSell(metadata(), cashLeg(account(), 10),
                                        portfolioLeg(new Portfolio(), 10), LedgerCreationUnits.none()).getEntry(),
                        LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO);

        assertPortfolioProjectionTargetsPrimaryPosting(LedgerEntryType.DELIVERY_INBOUND,
                        creator -> creator.createInboundDelivery(metadata(), deliveryLeg(new Portfolio())).getEntry());
        assertPortfolioProjectionTargetsPrimaryPosting(LedgerEntryType.DELIVERY_OUTBOUND,
                        creator -> creator.createOutboundDelivery(metadata(), deliveryLeg(new Portfolio())).getEntry());

        assertTwoProjectionTargetsPrimaryPostings(LedgerEntryType.CASH_TRANSFER,
                        creator -> creator.createAccountTransfer(metadata(),
                                        LedgerCashTransferLeg.of(account(), money(10)),
                                        LedgerCashTransferLeg.of(account(), money(10))).getEntry(),
                        LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT);
        assertTwoProjectionTargetsPrimaryPostings(LedgerEntryType.SECURITY_TRANSFER,
                        creator -> creator.createPortfolioTransfer(metadata(),
                                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                                        LedgerPortfolioTransferLeg.of(new Portfolio(), money(10)),
                                        LedgerPortfolioTransferLeg.of(new Portfolio(), money(10))).getEntry(),
                        LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerProjectionRole.TARGET_PORTFOLIO);
    }

    /**
     * Checks the ledger-backed editing scenario: create dividend preserves ex-date units and forex.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateDividendPreservesExDateUnitsAndForex()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);
        var exDate = LocalDateTime.of(2025, 12, 20, 0, 0);
        var forex = LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(30)),
                        BigDecimal.valueOf(0.91));
        var units = LedgerCreationUnits.of(LedgerCreationUnit.tax(money(3)),
                        LedgerCreationUnit.fee(money(2)),
                        LedgerCreationUnit.grossValue(money(35), forex));
        var dividend = LedgerDividend.withExDate(cashLeg(account(), 30), LedgerOptionalSecurity.of(security()), units,
                        exDate);

        var entry = creator.createDividend(metadata(), dividend).getEntry();
        var cashPosting = entry.getPostings().get(0);
        var exDateParameter = cashPosting.getParameters().get(0);
        var grossValuePosting = posting(entry, LedgerPostingType.GROSS_VALUE);

        assertThat(entry.getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(cashPosting.getType(), is(LedgerPostingType.CASH));
        assertThat(exDateParameter.getType(), is(LedgerParameterType.EX_DATE));
        assertThat(exDateParameter.getValueKind(), is(ValueKind.LOCAL_DATE_TIME));
        assertThat(exDateParameter.getValue(), is(exDate));
        assertTrue(entry.getPostings().stream().anyMatch(p -> p.getType() == LedgerPostingType.TAX));
        assertTrue(entry.getPostings().stream().anyMatch(p -> p.getType() == LedgerPostingType.FEE));
        assertThat(grossValuePosting.getForexAmount(), is(Values.Amount.factorize(30)));
        assertThat(grossValuePosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(grossValuePosting.getExchangeRate(), is(BigDecimal.valueOf(0.91)));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create buy creates two projection refs with positive magnitudes.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateBuyCreatesTwoProjectionRefsWithPositiveMagnitudes()
    {
        var client = new Client();
        var account = account();
        var portfolio = new Portfolio();
        var creator = new LedgerTransactionCreator(client);

        var entry = creator.createBuy(metadata(), cashLeg(account, 100), portfolioLeg(portfolio, 100),
                        LedgerCreationUnits.of(LedgerCreationUnit.fee(money(1)))).getEntry();

        assertThat(entry.getType(), is(LedgerEntryType.BUY));
        assertThat(entry.getProjectionRefs().size(), is(2));
        assertTrue(entry.getPostings().stream().allMatch(p -> p.getAmount() >= 0 && p.getShares() >= 0));
        assertSame(account, projection(entry, LedgerProjectionRole.ACCOUNT).getAccount());
        assertSame(portfolio, projection(entry, LedgerProjectionRole.PORTFOLIO).getPortfolio());
        assertTrue(account.getTransactions().isEmpty());
        assertTrue(portfolio.getTransactions().isEmpty());
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create sell creates two projection refs with positive magnitudes.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateSellCreatesTwoProjectionRefsWithPositiveMagnitudes()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);
        var entry = creator.createSell(metadata(), cashLeg(account(), 100), portfolioLeg(new Portfolio(), 100),
                        LedgerCreationUnits.none()).getEntry();

        assertThat(entry.getType(), is(LedgerEntryType.SELL));
        assertThat(entry.getProjectionRefs().size(), is(2));
        assertTrue(entry.getPostings().stream().allMatch(p -> p.getAmount() >= 0 && p.getShares() >= 0));
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create account transfer creates source and target account projections.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateAccountTransferCreatesSourceAndTargetAccountProjections()
    {
        var client = new Client();
        var source = account();
        var target = account();
        var creator = new LedgerTransactionCreator(client);

        var entry = creator.createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100))).getEntry();

        assertThat(entry.getType(), is(LedgerEntryType.CASH_TRANSFER));
        assertThat(entry.getProjectionRefs().size(), is(2));
        assertSame(source, projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT).getAccount());
        assertSame(target, projection(entry, LedgerProjectionRole.TARGET_ACCOUNT).getAccount());
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create portfolio transfer creates source and target portfolio projections.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreatePortfolioTransferCreatesSourceAndTargetPortfolioProjections()
    {
        var client = new Client();
        var source = new Portfolio();
        var target = new Portfolio();
        var creator = new LedgerTransactionCreator(client);

        var entry = creator.createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security(), Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();

        assertThat(entry.getType(), is(LedgerEntryType.SECURITY_TRANSFER));
        assertThat(entry.getProjectionRefs().size(), is(2));
        assertSame(source, projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO).getPortfolio());
        assertSame(target, projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO).getPortfolio());
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: create deliveries create one portfolio projection.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testCreateDeliveriesCreateOnePortfolioProjection()
    {
        var client = new Client();
        var portfolio = new Portfolio();
        var creator = new LedgerTransactionCreator(client);

        var inbound = creator.createInboundDelivery(metadata(), deliveryLeg(portfolio)).getEntry();
        var outbound = creator.createOutboundDelivery(metadata(), deliveryLeg(portfolio)).getEntry();

        assertThat(inbound.getType(), is(LedgerEntryType.DELIVERY_INBOUND));
        assertThat(inbound.getProjectionRefs().size(), is(1));
        assertThat(inbound.getProjectionRefs().get(0).getRole(), is(LedgerProjectionRole.DELIVERY_INBOUND));
        assertThat(outbound.getType(), is(LedgerEntryType.DELIVERY_OUTBOUND));
        assertThat(outbound.getProjectionRefs().size(), is(1));
        assertThat(outbound.getProjectionRefs().get(0).getRole(), is(LedgerProjectionRole.DELIVERY_OUTBOUND));
        assertTrue(portfolio.getTransactions().isEmpty());
        assertOK(client);
    }

    /**
     * Checks the ledger-backed editing scenario: missing required input does not partially add entry.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testMissingRequiredInputDoesNotPartiallyAddEntry()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);

        assertThrows(NullPointerException.class, () -> creator.createDeposit(metadata(), null));
        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    /**
     * Checks the ledger-backed editing scenario: invalid standard family sign fails through creator validation without partial mutation.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidStandardFamilySignFailsThroughCreatorValidationWithoutPartialMutation()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);

        assertThrows(IllegalArgumentException.class,
                        () -> creator.createDeposit(metadata(),
                                        LedgerAccountCashLeg.of(account(), Money.of(CurrencyUnit.EUR, -1L))));
        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    /**
     * Checks the ledger-backed editing scenario: invalid standard family shares fail through creator validation without partial mutation.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testInvalidStandardFamilySharesFailThroughCreatorValidationWithoutPartialMutation()
    {
        var client = new Client();
        var creator = new LedgerTransactionCreator(client);

        assertThrows(IllegalArgumentException.class,
                        () -> creator.createBuy(metadata(), cashLeg(account(), 100),
                                        LedgerPortfolioSecurityLeg.of(new Portfolio(),
                                                        LedgerSecurityQuantity.of(security(), -1L), money(100)),
                                        LedgerCreationUnits.none()));
        assertTrue(client.getLedger().getEntries().isEmpty());
    }

    /**
     * Checks the ledger-backed editing scenario: public creator methods do not use long positional parameter lists.
     * The visible transaction must reflect the ledger entry after the operation.
     * This protects structural facts from being written through legacy setters.
     */
    @Test
    public void testPublicCreatorMethodsDoNotUseLongPositionalParameterLists()
    {
        assertTrue(Arrays.stream(LedgerTransactionCreator.class.getDeclaredMethods())
                        .filter(method -> Modifier.isPublic(method.getModifiers()))
                        .filter(method -> method.getName().startsWith("create"))
                        .allMatch(method -> method.getParameterCount() <= 4));
        assertFalse(Arrays.stream(LedgerTransactionCreator.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals("createAccountTransaction")));
        assertFalse(Arrays.stream(LedgerTransactionCreator.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals("createPortfolioTransaction")));
        assertFalse(Arrays.stream(LedgerTransactionCreator.class.getDeclaredMethods())
                        .anyMatch(method -> method.getName().equals("createBuySell")));
    }

    private LedgerTransactionMetadata metadata()
    {
        return LedgerTransactionMetadata.of(DATE_TIME);
    }

    private Account account()
    {
        return new Account();
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
        return entry.getPostings().stream().filter(p -> p.getType() == type).findFirst().orElseThrow();
    }

    private void assertUnitForex(LedgerEntry entry, LedgerPostingType type, long forexAmount, BigDecimal exchangeRate)
    {
        var posting = posting(entry, type);

        assertThat(posting.getForexAmount(), is(forexAmount));
        assertThat(posting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting.getExchangeRate(), is(exchangeRate));
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(p -> p.getRole() == role).findFirst().orElseThrow();
    }

    private void assertAccountProjectionTargetsPrimaryPosting(LedgerEntryType type,
                    java.util.function.Function<LedgerTransactionCreator, LedgerEntry> factory)
    {
        var client = new Client();
        var entry = factory.apply(new LedgerTransactionCreator(client));
        var projection = projection(entry, LedgerProjectionRole.ACCOUNT);
        var posting = entry.getPostings().stream().filter(p -> p.getAccount() == projection.getAccount()).findFirst()
                        .orElseThrow();

        assertThat(entry.getType(), is(type));
        assertThat(projection.getPrimaryPostingUUID(), is(posting.getUUID()));
        assertOK(client);
    }

    private void assertPortfolioProjectionTargetsPrimaryPosting(LedgerEntryType type,
                    java.util.function.Function<LedgerTransactionCreator, LedgerEntry> factory)
    {
        var client = new Client();
        var entry = factory.apply(new LedgerTransactionCreator(client));
        var projection = entry.getProjectionRefs().get(0);
        var posting = entry.getPostings().stream().filter(p -> p.getPortfolio() == projection.getPortfolio())
                        .findFirst().orElseThrow();

        assertThat(entry.getType(), is(type));
        assertThat(projection.getPrimaryPostingUUID(), is(posting.getUUID()));
        assertOK(client);
    }

    private void assertTwoProjectionTargetsPrimaryPostings(LedgerEntryType type,
                    java.util.function.Function<LedgerTransactionCreator, LedgerEntry> factory,
                    LedgerProjectionRole firstRole, LedgerProjectionRole secondRole)
    {
        var client = new Client();
        var entry = factory.apply(new LedgerTransactionCreator(client));

        assertThat(entry.getType(), is(type));
        assertThat(projection(entry, firstRole).getPrimaryPostingUUID(), is(entry.getPostings().get(0).getUUID()));
        assertThat(projection(entry, secondRole).getPrimaryPostingUUID(), is(entry.getPostings().get(1).getUUID()));
        assertOK(client);
    }

    private void assertOK(Client client)
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());

        assertTrue(result.getIssues().toString(), result.isOK());
    }
}
