package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-aware conversion between buy/sell transactions and deliveries.
 * These tests make sure supported master transitions keep ledger truth consistent and do not leave removed rows referenced.
 */
@SuppressWarnings("nls")
public class LedgerBuySellDeliveryConverterTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("0.5000");

    /**
     * Verifies that a ledger-backed buy can become an inbound delivery.
     * The portfolio-side booking remains the surviving projection and no account-side truth is left behind.
     */
    @Test
    public void testConvertsLedgerBackedBuyToInboundDeliveryPreservingIdentityAndTruth() throws Exception
    {
        assertConvertsBuySellToDelivery(PortfolioTransaction.Type.BUY, LedgerEntryType.DELIVERY_INBOUND,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerProjectionRole.DELIVERY_INBOUND);
    }

    /**
     * Verifies that a ledger-backed sell can become an outbound delivery.
     * The portfolio-side booking remains the surviving projection and no account-side truth is left behind.
     */
    @Test
    public void testConvertsLedgerBackedSellToOutboundDeliveryPreservingIdentityAndTruth() throws Exception
    {
        assertConvertsBuySellToDelivery(PortfolioTransaction.Type.SELL, LedgerEntryType.DELIVERY_OUTBOUND,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerProjectionRole.DELIVERY_OUTBOUND);
    }

    /**
     * Verifies that an inbound delivery can become the matching buy booking.
     * The converter must create the cash side while preserving the portfolio projection identity.
     */
    @Test
    public void testConvertsLedgerBackedInboundDeliveryToBuyPreservingIdentityAndTruth() throws Exception
    {
        assertConvertsDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerEntryType.BUY,
                        PortfolioTransaction.Type.BUY, LedgerProjectionRole.DELIVERY_INBOUND);
    }

    /**
     * Verifies that an outbound delivery can become the matching sell booking.
     * The converter must create the cash side while preserving the portfolio projection identity.
     */
    @Test
    public void testConvertsLedgerBackedOutboundDeliveryToSellPreservingIdentityAndTruth() throws Exception
    {
        assertConvertsDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerEntryType.SELL,
                        PortfolioTransaction.Type.SELL, LedgerProjectionRole.DELIVERY_OUTBOUND);
    }

    /**
     * Verifies that the composite converter reverses direction and changes buy/sell shape in one mutation.
     * The surviving portfolio projection must keep its UUID and no account-side projection may survive.
     */
    @Test
    public void testCompositeConvertsLedgerBackedBuySellToOppositeDeliveryAtomically() throws Exception
    {
        assertCompositeConvertsBuySellToDelivery(PortfolioTransaction.Type.BUY, LedgerEntryType.DELIVERY_OUTBOUND,
                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        Values.Amount.factorize(113));
        assertCompositeConvertsBuySellToDelivery(PortfolioTransaction.Type.SELL, LedgerEntryType.DELIVERY_INBOUND,
                        PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerProjectionRole.DELIVERY_INBOUND,
                        Values.Amount.factorize(127));
    }

    /**
     * Verifies that the composite converter reverses direction and creates the buy/sell shape in one mutation.
     * The portfolio projection survives while the new account side is materialized consistently.
     */
    @Test
    public void testCompositeConvertsLedgerBackedDeliveryToOppositeBuySellAtomically() throws Exception
    {
        assertCompositeConvertsDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_INBOUND, LedgerEntryType.SELL,
                        PortfolioTransaction.Type.SELL, LedgerProjectionRole.DELIVERY_INBOUND,
                        Values.Amount.factorize(113));
        assertCompositeConvertsDeliveryToBuySell(PortfolioTransaction.Type.DELIVERY_OUTBOUND, LedgerEntryType.BUY,
                        PortfolioTransaction.Type.BUY, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        Values.Amount.factorize(127));
    }

    /**
     * Verifies that a plan-generated buy/sell can be converted to delivery when the portfolio projection survives.
     * The execution ref must keep resolving to the same projected booking after save/load.
     */
    @Test
    public void testInvestmentPlanReferencedBuySellConvertsToDeliveryAndUpdatesPlanReference() throws Exception
    {
        var fixture = fixture();
        var portfolioTransaction = create(fixture, PortfolioTransaction.Type.BUY).getPortfolioTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");
        var projectionUUID = portfolioTransaction.getUUID();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of((LedgerBackedTransaction) portfolioTransaction));
        fixture.client().addPlan(plan);

        converter(fixture).convertBuySellToDelivery(pair(fixture, portfolioTransaction));

        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(),
                        is(LedgerProjectionRole.DELIVERY_INBOUND));
        assertThat(plan.getTransactions(fixture.client()).get(0).getTransaction().getUUID(), is(projectionUUID));

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(projectionUUID));
    }

    /**
     * Verifies that a plan ref on the account side blocks a buy/sell to delivery conversion.
     * That projection would be removed, so the converter must reject before changing the ledger.
     */
    @Test
    public void testInvestmentPlanReferencedBuySellAccountProjectionCannotConvertToDelivery()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef
                        .of((LedgerBackedTransaction) buySell.getAccountTransaction()));
        fixture.client().addPlan(plan);
        var snapshot = Snapshot.capture(fixture.client());

        var exception = assertThrows(UnsupportedOperationException.class,
                        () -> converter(fixture).convertBuySellToDelivery(pair(fixture,
                                        buySell.getPortfolioTransaction())));

        assertThat(exception.getMessage(), containsString("projection would be removed"));
        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
    }

    /**
     * Verifies that buy/sell to delivery conversion rejects a malformed buy/sell entry before mutation.
     * The converter must not continue when the cash side needed for removal is already inconsistent.
     */
    @Test
    public void testMalformedBuySellShapeRejectsBeforeMutation()
    {
        var fixture = fixture();
        var portfolioTransaction = create(fixture, PortfolioTransaction.Type.BUY).getPortfolioTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var cashPosting = posting(entry, LedgerPostingType.CASH);

        entry.removePosting(cashPosting);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class,
                        () -> converter(fixture).convertBuySellToDelivery(pair(fixture, portfolioTransaction)));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that delivery to buy/sell conversion needs a reference account for the cash side.
     * Without that owner, the converter must reject before creating an incomplete account projection.
     */
    @Test
    public void testDeliveryWithoutReferenceAccountRejectsBeforeMutation()
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);

        fixture.portfolio().setReferenceAccount(null);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class,
                        () -> converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery)));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that a delivery can become a buy/sell even when the reference account uses another currency.
     * If no better rate exists, the new cash side records explicit forex facts with exchange rate one.
     */
    @Test
    public void testDeliveryReferenceAccountCurrencyMismatchConvertsWithDefaultForex() throws Exception
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);

        fixture.account().setCurrencyCode(CurrencyUnit.USD);
        var converted = converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery));
        var entry = fixture.client().getLedger().getEntries().get(0);
        var cashPosting = posting(entry, LedgerPostingType.CASH);

        assertThat(converted.getAccountTransaction().getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(converted.getAccountTransaction().getAmount(), is(Values.Amount.factorize(123)));
        assertThat(cashPosting.getCurrency(), is(CurrencyUnit.USD));
        assertThat(cashPosting.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(cashPosting.getForexCurrency(), is(CurrencyUnit.EUR));
        assertThat(cashPosting.getForexAmount(), is(Values.Amount.factorize(123)));
        assertThat(cashPosting.getExchangeRate(), is(BigDecimal.ONE));
        assertValid(fixture.client());

        var loaded = loadXml(saveXml(fixture.client()));
        var loadedCashPosting = posting(loaded.getLedger().getEntries().get(0), LedgerPostingType.CASH);
        assertThat(loadedCashPosting.getCurrency(), is(CurrencyUnit.USD));
        assertThat(loadedCashPosting.getForexCurrency(), is(CurrencyUnit.EUR));
        assertThat(loadedCashPosting.getExchangeRate(), is(BigDecimal.ONE));
    }

    /**
     * Verifies that existing delivery forex metadata is used when creating the cash side.
     * The converter must preserve the known relationship between delivery amount and account currency.
     */
    @Test
    public void testDeliveryWithForexMetadataConvertsToReferenceAccountCurrency() throws Exception
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(246)), EXCHANGE_RATE);

        fixture.account().setCurrencyCode(CurrencyUnit.USD);
        var converted = converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery));
        var entry = fixture.client().getLedger().getEntries().get(0);
        var cashPosting = posting(entry, LedgerPostingType.CASH);

        assertThat(converted.getAccountTransaction().getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(converted.getAccountTransaction().getAmount(), is(Values.Amount.factorize(246)));
        assertThat(cashPosting.getCurrency(), is(CurrencyUnit.USD));
        assertThat(cashPosting.getAmount(), is(Values.Amount.factorize(246)));
        assertThat(cashPosting.getForexCurrency(), is(CurrencyUnit.EUR));
        assertThat(cashPosting.getForexAmount(), is(Values.Amount.factorize(123)));
        assertThat(cashPosting.getExchangeRate(), is(new BigDecimal("2.0000000000")));
        assertValid(fixture.client());

        var loaded = loadXml(saveXml(fixture.client()));
        var loadedCashPosting = posting(loaded.getLedger().getEntries().get(0), LedgerPostingType.CASH);
        assertThat(loadedCashPosting.getCurrency(), is(CurrencyUnit.USD));
        assertThat(loadedCashPosting.getForexCurrency(), is(CurrencyUnit.EUR));
        assertThat(loadedCashPosting.getExchangeRate(), is(new BigDecimal("2.0000000000")));
    }

    /**
     * Verifies that invalid delivery forex metadata reports the exact converter diagnostic.
     */
    @Test
    public void testDeliveryWithNonPositiveForexMetadataRejectsWithDiagnostic()
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(246)), EXCHANGE_RATE);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var securityPosting = posting(entry, LedgerPostingType.SECURITY);

        securityPosting.setExchangeRate(BigDecimal.ZERO);
        fixture.account().setCurrencyCode(CurrencyUnit.USD);
        var snapshot = Snapshot.capture(fixture.client());

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery)));

        assertThat(exception.getMessage(), containsString(LedgerDiagnosticCode.LEDGER_FOREX_003.prefix()));
        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that the composite converter keeps its own delivery forex diagnostic.
     */
    @Test
    public void testCompositeDeliveryCashPostingWithNonPositiveForexMetadataUsesDistinctDiagnostic() throws Exception
    {
        var fixture = fixture();
        var securityPosting = new LedgerPosting("security-posting");
        var cashPosting = new LedgerPosting("cash-posting");
        var method = LedgerPortfolioCompositeTypeConverter.class.getDeclaredMethod("applyDeliveryCashPosting",
                        LedgerPosting.class, LedgerPosting.class, Account.class);

        securityPosting.setAmount(Values.Amount.factorize(123));
        securityPosting.setCurrency(CurrencyUnit.EUR);
        securityPosting.setForexAmount(Values.Amount.factorize(246));
        securityPosting.setForexCurrency(CurrencyUnit.USD);
        securityPosting.setExchangeRate(BigDecimal.ZERO);
        fixture.account().setCurrencyCode(CurrencyUnit.USD);
        method.setAccessible(true);

        var exception = assertThrows(InvocationTargetException.class,
                        () -> method.invoke(compositeConverter(fixture), securityPosting, cashPosting,
                                        fixture.account()));

        assertThat(exception.getCause(), instanceOf(IllegalArgumentException.class));
        assertThat(exception.getCause().getMessage(), containsString(LedgerDiagnosticCode.LEDGER_FOREX_004.prefix()));
    }

    /**
     * Verifies that composite buy/sell conversion reports posting forex rejection with its FOREX code.
     */
    @Test
    public void testCompositeBuySellPostingForexRejectsWithDiagnostic()
    {
        var fixture = fixture();
        var buySell = create(fixture, PortfolioTransaction.Type.BUY);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var cashPosting = posting(entry, LedgerPostingType.CASH);

        cashPosting.setForexAmount(Values.Amount.factorize(123));
        cashPosting.setForexCurrency(CurrencyUnit.USD);
        cashPosting.setExchangeRate(EXCHANGE_RATE);
        var snapshot = Snapshot.capture(fixture.client());

        var exception = assertThrows(UnsupportedOperationException.class,
                        () -> compositeConverter(fixture).convert(pair(fixture, buySell.getPortfolioTransaction())));

        assertThat(exception.getMessage(), containsString(LedgerDiagnosticCode.LEDGER_FOREX_005.prefix()));
        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    /**
     * Verifies that a plan-generated delivery can become buy/sell without losing its plan reference.
     * The execution ref must follow the surviving portfolio projection after save/load.
     */
    @Test
    public void testInvestmentPlanReferencedDeliveryConvertsToBuySellAndUpdatesPlanReference() throws Exception
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var plan = new InvestmentPlan("Plan");
        var projectionUUID = delivery.getUUID();

        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of((LedgerBackedTransaction) delivery));
        fixture.client().addPlan(plan);

        converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery));

        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(entry.getUUID()));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionRole(), is(LedgerProjectionRole.PORTFOLIO));
        assertThat(plan.getTransactions(fixture.client()).get(0).getTransaction().getUUID(), is(projectionUUID));

        var loaded = loadXml(saveXml(fixture.client()));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(projectionUUID));
    }

    /**
     * Verifies that delivery to buy/sell conversion rejects a malformed delivery before mutation.
     * The converter must not infer missing security facts from the runtime projection.
     */
    @Test
    public void testMalformedDeliveryShapeRejectsBeforeMutation()
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, PortfolioTransaction.Type.DELIVERY_INBOUND);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var securityPosting = posting(entry, LedgerPostingType.SECURITY);

        entry.removePosting(securityPosting);
        var snapshot = Snapshot.capture(fixture.client());

        assertThrows(IllegalArgumentException.class,
                        () -> converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery)));

        assertThat(Snapshot.capture(fixture.client()), is(snapshot));
    }

    private void assertConvertsBuySellToDelivery(PortfolioTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    PortfolioTransaction.Type targetPortfolioType, LedgerProjectionRole targetProjectionRole)
                    throws Exception
    {
        var fixture = fixture();
        var buySell = create(fixture, sourceType);
        var accountTransaction = buySell.getAccountTransaction();
        var portfolioTransaction = buySell.getPortfolioTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var securityPosting = posting(entry, LedgerPostingType.SECURITY);
        var cashPostingUUID = posting(entry, LedgerPostingType.CASH).getUUID();
        var securityPostingUUID = securityPosting.getUUID();
        var accountProjectionUUID = projection(entry, LedgerProjectionRole.ACCOUNT).getUUID();
        var portfolioProjectionUUID = projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID();
        var unitPostingUUIDs = unitPostingUUIDs(entry);

        var converted = converter(fixture).convertBuySellToDelivery(pair(fixture, portfolioTransaction));

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(entry.getPostings().stream().noneMatch(posting -> posting.getUUID().equals(cashPostingUUID)),
                        is(true));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(unitPostingUUIDs(entry), is(unitPostingUUIDs));
        assertThat(entry.getProjectionRefs().stream()
                        .noneMatch(projection -> projection.getUUID().equals(accountProjectionUUID)), is(true));
        assertThat(projection(entry, targetProjectionRole).getUUID(), is(portfolioProjectionUUID));
        assertSame(fixture.portfolio(), projection(entry, targetProjectionRole).getPortfolio());

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertThat(fixture.portfolio().getTransactions(), is(List.of(converted)));
        assertThat(converted.getUUID(), is(portfolioProjectionUUID));
        assertThat(converted, instanceOf(LedgerBackedTransaction.class));
        assertThat(converted.getType(), is(targetPortfolioType));
        assertThat(converted.getCrossEntry(), is(nullValue()));
        assertThat(converted.getDateTime(), is(DATE_TIME));
        assertThat(converted.getNote(), is("note"));
        assertThat(converted.getSource(), is("source"));
        assertThat(converted.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(converted.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertSame(fixture.security(), converted.getSecurity());
        assertThat(converted.getShares(), is(Values.Share.factorize(5)));
        assertThat(converted.getUnits().count(), is(3L));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(converted, fixture.client().getAllTransactions().get(0).getTransaction());
        assertThat(accountTransaction.getUUID(), is(accountProjectionUUID));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjectionUUID));
        assertValid(fixture.client());

        assertConvertedRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, securityPostingUUID,
                        portfolioProjectionUUID, targetEntryType, targetPortfolioType, targetProjectionRole);
        assertConvertedRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, securityPostingUUID,
                        portfolioProjectionUUID, targetEntryType, targetPortfolioType, targetProjectionRole);
    }

    private void assertConvertsDeliveryToBuySell(PortfolioTransaction.Type sourceType, LedgerEntryType targetEntryType,
                    PortfolioTransaction.Type targetTransactionType, LedgerProjectionRole sourceProjectionRole)
                    throws Exception
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, sourceType);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var securityPosting = posting(entry, LedgerPostingType.SECURITY);
        var securityPostingUUID = securityPosting.getUUID();
        var portfolioProjectionUUID = projection(entry, sourceProjectionRole).getUUID();
        var unitPostingUUIDs = unitPostingUUIDs(entry);

        var converted = converter(fixture).convertDeliveryToBuySell(pair(fixture, delivery));
        var accountTransaction = converted.getAccountTransaction();
        var portfolioTransaction = converted.getPortfolioTransaction();
        var cashPosting = posting(entry, LedgerPostingType.CASH);
        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = projection(entry, LedgerProjectionRole.PORTFOLIO);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(unitPostingUUIDs(entry), is(unitPostingUUIDs));
        assertThat(cashPosting.getUUID().equals(securityPostingUUID), is(false));
        assertSame(fixture.account(), cashPosting.getAccount());
        assertThat(cashPosting.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(cashPosting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(cashPosting.getForexAmount(), is(nullValue()));
        assertThat(cashPosting.getForexCurrency(), is(nullValue()));
        assertThat(cashPosting.getExchangeRate(), is(nullValue()));
        assertThat(accountProjection.getUUID().equals(portfolioProjectionUUID), is(false));
        assertSame(fixture.account(), accountProjection.getAccount());
        assertThat(portfolioProjection.getUUID(), is(portfolioProjectionUUID));
        assertSame(fixture.portfolio(), portfolioProjection.getPortfolio());

        assertThat(fixture.account().getTransactions(), is(List.of(accountTransaction)));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(portfolioTransaction)));
        assertThat(accountTransaction.getUUID(), is(accountProjection.getUUID()));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjectionUUID));
        assertThat(accountTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(portfolioTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(accountTransaction.getType().name(), is(targetTransactionType.name()));
        assertThat(portfolioTransaction.getType(), is(targetTransactionType));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertSame(accountTransaction, portfolioTransaction.getCrossEntry().getCrossTransaction(portfolioTransaction));
        assertSame(fixture.portfolio(), accountTransaction.getCrossEntry().getCrossOwner(accountTransaction));
        assertSame(fixture.account(), portfolioTransaction.getCrossEntry().getCrossOwner(portfolioTransaction));
        assertThat(accountTransaction.getDateTime(), is(DATE_TIME));
        assertThat(portfolioTransaction.getDateTime(), is(DATE_TIME));
        assertThat(accountTransaction.getNote(), is("note"));
        assertThat(portfolioTransaction.getNote(), is("note"));
        assertThat(accountTransaction.getSource(), is("source"));
        assertThat(portfolioTransaction.getSource(), is("source"));
        assertThat(accountTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(accountTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(portfolioTransaction.getAmount(), is(Values.Amount.factorize(123)));
        assertThat(portfolioTransaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertSame(fixture.security(), portfolioTransaction.getSecurity());
        assertThat(portfolioTransaction.getShares(), is(Values.Share.factorize(5)));
        assertThat(portfolioTransaction.getUnits().count(), is(3L));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(portfolioTransaction, fixture.client().getAllTransactions().get(0).getTransaction());
        assertValid(fixture.client());

        assertDeliveryToBuySellRoundtrip(loadXml(saveXml(fixture.client())), entryUUID, securityPostingUUID,
                        cashPosting.getUUID(), portfolioProjectionUUID, accountProjection.getUUID(), targetEntryType,
                        targetTransactionType);
        assertDeliveryToBuySellRoundtrip(loadProtobuf(saveProtobuf(fixture.client())), entryUUID, securityPostingUUID,
                        cashPosting.getUUID(), portfolioProjectionUUID, accountProjection.getUUID(), targetEntryType,
                        targetTransactionType);
    }

    private void assertCompositeConvertsBuySellToDelivery(PortfolioTransaction.Type sourceType,
                    LedgerEntryType targetEntryType, PortfolioTransaction.Type targetPortfolioType,
                    LedgerProjectionRole targetProjectionRole, long targetAmount) throws Exception
    {
        var fixture = fixture();
        var buySell = create(fixture, sourceType);
        var portfolioTransaction = buySell.getPortfolioTransaction();
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var cashPostingUUID = posting(entry, LedgerPostingType.CASH).getUUID();
        var securityPostingUUID = posting(entry, LedgerPostingType.SECURITY).getUUID();
        var accountProjectionUUID = projection(entry, LedgerProjectionRole.ACCOUNT).getUUID();
        var portfolioProjectionUUID = projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID();
        var unitPostingUUIDs = unitPostingUUIDs(entry);

        var converted = compositeConverter(fixture).convert(pair(fixture, portfolioTransaction));

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(entry.getPostings().stream().noneMatch(posting -> posting.getUUID().equals(cashPostingUUID)),
                        is(true));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getAmount(), is(targetAmount));
        assertThat(unitPostingUUIDs(entry), is(unitPostingUUIDs));
        assertThat(entry.getProjectionRefs().stream()
                        .noneMatch(projection -> projection.getUUID().equals(accountProjectionUUID)), is(true));
        assertThat(projection(entry, targetProjectionRole).getUUID(), is(portfolioProjectionUUID));

        assertTrue(fixture.account().getTransactions().isEmpty());
        assertThat(fixture.portfolio().getTransactions(), is(List.of(converted)));
        assertThat(converted.getUUID(), is(portfolioProjectionUUID));
        assertThat(converted.getType(), is(targetPortfolioType));
        assertThat(converted.getAmount(), is(targetAmount));
        assertThat(converted.getCrossEntry(), is(nullValue()));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(converted, fixture.client().getAllTransactions().get(0).getTransaction());
        assertValid(fixture.client());
    }

    private void assertCompositeConvertsDeliveryToBuySell(PortfolioTransaction.Type sourceType,
                    LedgerEntryType targetEntryType, PortfolioTransaction.Type targetTransactionType,
                    LedgerProjectionRole sourceProjectionRole, long targetAmount) throws Exception
    {
        var fixture = fixture();
        var delivery = createDelivery(fixture, sourceType);
        var entry = fixture.client().getLedger().getEntries().get(0);
        var entryUUID = entry.getUUID();
        var securityPostingUUID = posting(entry, LedgerPostingType.SECURITY).getUUID();
        var portfolioProjectionUUID = projection(entry, sourceProjectionRole).getUUID();
        var unitPostingUUIDs = unitPostingUUIDs(entry);

        var converted = compositeConverter(fixture).convert(pair(fixture, delivery));
        var accountTransaction = fixture.account().getTransactions().get(0);
        var cashPosting = posting(entry, LedgerPostingType.CASH);
        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = projection(entry, LedgerProjectionRole.PORTFOLIO);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(targetEntryType));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getAmount(), is(targetAmount));
        assertThat(unitPostingUUIDs(entry), is(unitPostingUUIDs));
        assertThat(cashPosting.getAmount(), is(targetAmount));
        assertSame(fixture.account(), cashPosting.getAccount());
        assertThat(portfolioProjection.getUUID(), is(portfolioProjectionUUID));
        assertSame(fixture.portfolio(), portfolioProjection.getPortfolio());

        assertThat(fixture.account().getTransactions(), is(List.of(accountTransaction)));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(converted)));
        assertThat(accountTransaction.getUUID(), is(accountProjection.getUUID()));
        assertThat(converted.getUUID(), is(portfolioProjectionUUID));
        assertThat(accountTransaction.getType().name(), is(targetTransactionType.name()));
        assertThat(converted.getType(), is(targetTransactionType));
        assertSame(accountTransaction, converted.getCrossEntry().getCrossTransaction(converted));
        assertSame(converted, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertThat(accountTransaction.getAmount(), is(targetAmount));
        assertThat(converted.getAmount(), is(targetAmount));
        assertThat(fixture.client().getAllTransactions().size(), is(1));
        assertSame(converted, fixture.client().getAllTransactions().get(0).getTransaction());
        assertValid(fixture.client());
    }

    private void assertConvertedRoundtrip(Client client, String entryUUID, String securityPostingUUID,
                    String projectionUUID, LedgerEntryType entryType, PortfolioTransaction.Type transactionType,
                    LedgerProjectionRole projectionRole)
    {
        assertThat(client.getAccounts().get(0).getTransactions().isEmpty(), is(true));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        var entry = client.getLedger().getEntries().get(0);
        var transaction = client.getPortfolios().get(0).getTransactions().get(0);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(entryType));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(entry.getPostings().stream().noneMatch(posting -> posting.getType() == LedgerPostingType.CASH),
                        is(true));
        assertThat(entry.getProjectionRefs().size(), is(1));
        assertThat(projection(entry, projectionRole).getUUID(), is(projectionUUID));
        assertThat(transaction.getUUID(), is(projectionUUID));
        assertThat(transaction.getType(), is(transactionType));
        assertThat(transaction.getCrossEntry(), is(nullValue()));
        assertThat(transaction.getUnits().count(), is(3L));
        assertValid(client);
    }

    private void assertDeliveryToBuySellRoundtrip(Client client, String entryUUID, String securityPostingUUID,
                    String cashPostingUUID, String portfolioProjectionUUID, String accountProjectionUUID,
                    LedgerEntryType entryType, PortfolioTransaction.Type transactionType)
    {
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        var entry = client.getLedger().getEntries().get(0);
        var accountTransaction = client.getAccounts().get(0).getTransactions().get(0);
        var portfolioTransaction = client.getPortfolios().get(0).getTransactions().get(0);

        assertThat(entry.getUUID(), is(entryUUID));
        assertThat(entry.getType(), is(entryType));
        assertThat(posting(entry, LedgerPostingType.SECURITY).getUUID(), is(securityPostingUUID));
        assertThat(posting(entry, LedgerPostingType.CASH).getUUID(), is(cashPostingUUID));
        assertThat(projection(entry, LedgerProjectionRole.ACCOUNT).getUUID(), is(accountProjectionUUID));
        assertThat(projection(entry, LedgerProjectionRole.PORTFOLIO).getUUID(), is(portfolioProjectionUUID));
        assertThat(accountTransaction.getUUID(), is(accountProjectionUUID));
        assertThat(portfolioTransaction.getUUID(), is(portfolioProjectionUUID));
        assertThat(accountTransaction.getType().name(), is(transactionType.name()));
        assertThat(portfolioTransaction.getType(), is(transactionType));
        assertSame(portfolioTransaction, accountTransaction.getCrossEntry().getCrossTransaction(accountTransaction));
        assertSame(accountTransaction, portfolioTransaction.getCrossEntry().getCrossTransaction(portfolioTransaction));
        assertThat(portfolioTransaction.getUnits().count(), is(3L));
        assertValid(client);
    }

    private name.abuchen.portfolio.model.BuySellEntry create(Fixture fixture, PortfolioTransaction.Type type)
    {
        return new LedgerBuySellTransactionCreator(fixture.client()).create(fixture.portfolio(), fixture.account(),
                        type, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), units(), "note", "source");
    }

    private PortfolioTransaction createDelivery(Fixture fixture, PortfolioTransaction.Type type)
    {
        return createDelivery(fixture, type, null, null);
    }

    private PortfolioTransaction createDelivery(Fixture fixture, PortfolioTransaction.Type type, Money forexAmount,
                    BigDecimal exchangeRate)
    {
        return new LedgerDeliveryTransactionCreator(fixture.client()).create(fixture.portfolio(), type, DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(), Values.Share.factorize(5),
                        forexAmount, exchangeRate, units(), "note", "source");
    }

    private List<Unit> units()
    {
        return List.of(
                        new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)), EXCHANGE_RATE),
                        new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4))),
                        new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(240)), EXCHANGE_RATE));
    }

    private LedgerBuySellDeliveryConverter converter(Fixture fixture)
    {
        return new LedgerBuySellDeliveryConverter(fixture.client());
    }

    private LedgerPortfolioCompositeTypeConverter compositeConverter(Fixture fixture)
    {
        return new LedgerPortfolioCompositeTypeConverter(fixture.client());
    }

    private TransactionPair<PortfolioTransaction> pair(Fixture fixture, PortfolioTransaction transaction)
    {
        return new TransactionPair<>(fixture.portfolio(), transaction);
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingType type)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type).findFirst().orElseThrow();
    }

    private List<String> unitPostingUUIDs(LedgerEntry entry)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.FEE
                        || posting.getType() == LedgerPostingType.TAX
                        || posting.getType() == LedgerPostingType.GROSS_VALUE).map(LedgerPosting::getUUID).toList();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-buy-sell-delivery-converter", ".xml");
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

    private Client loadXml(String xml) throws IOException
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] saveProtobuf(Client client) throws IOException
    {
        return ProtobufTestUtilities.save(client);
    }

    private Client loadProtobuf(byte[] bytes) throws IOException
    {
        return ProtobufTestUtilities.load(bytes);
    }

    private void assertValid(Client client)
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());

        assertThat(result.format(), result.isOK(), is(true));
    }

    private Fixture fixture()
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        account.setUpdatedAt(Instant.now());
        portfolio.setUpdatedAt(Instant.now());
        security.setUpdatedAt(Instant.now());
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, portfolio, security);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security)
    {
    }

    private record Snapshot(List<EntrySnapshot> entries, List<String> accountTransactions,
                    List<String> portfolioTransactions, List<String> allTransactions)
    {
        static Snapshot capture(Client client)
        {
            return new Snapshot(client.getLedger().getEntries().stream().map(EntrySnapshot::capture).toList(),
                            client.getAccounts().stream().flatMap(account -> account.getTransactions().stream())
                                            .map(Transaction::getUUID).toList(),
                            client.getPortfolios().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                                            .map(Transaction::getUUID).toList(),
                            client.getAllTransactions().stream().map(pair -> pair.getTransaction().getUUID())
                                            .toList());
        }
    }

    private record EntrySnapshot(String uuid, LedgerEntryType type, List<PostingSnapshot> postings,
                    List<ProjectionSnapshot> projections)
    {
        static EntrySnapshot capture(LedgerEntry entry)
        {
            return new EntrySnapshot(entry.getUUID(), entry.getType(),
                            entry.getPostings().stream().map(PostingSnapshot::capture).toList(),
                            entry.getProjectionRefs().stream().map(ProjectionSnapshot::capture).toList());
        }
    }

    private record PostingSnapshot(String uuid, LedgerPostingType type, Long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, Security security, Long shares, Account account,
                    Portfolio portfolio)
    {
        static PostingSnapshot capture(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(), posting.getPortfolio());
        }
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, Account account, Portfolio portfolio,
                    String primaryPostingUUID, String postingGroupUUID)
    {
        static ProjectionSnapshot capture(LedgerProjectionRef projection)
        {
            return new ProjectionSnapshot(projection.getUUID(), projection.getRole(), projection.getAccount(),
                            projection.getPortfolio(), projection.getPrimaryPostingUUID(),
                            projection.getPostingGroupUUID());
        }
    }
}
