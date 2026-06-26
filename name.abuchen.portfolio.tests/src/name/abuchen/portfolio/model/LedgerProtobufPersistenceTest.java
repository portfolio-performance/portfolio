package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
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
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.model.proto.v1.PLedgerEntry;
import name.abuchen.portfolio.model.proto.v1.PLedgerParameter;
import name.abuchen.portfolio.model.proto.v1.PLedgerParameterValueKind;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionRef;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionRole;
import name.abuchen.portfolio.model.proto.v1.PTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests protobuf persistence for ledger-backed transactions.
 * These tests make sure loaded rows are rebuilt from ledger truth instead of derived compatibility shadows.
 */
@SuppressWarnings("nls")
public class LedgerProtobufPersistenceTest
{
    private static final byte[] SIGNATURE = new byte[] { 'P', 'P', 'P', 'B', 'V', '1' };
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 4);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2025, 12, 31, 0, 0);
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    /**
     * Verifies that protobuf save writes ledger truth and the account shadow row.
     * The shadow row is derived compatibility data, not a second booking source.
     */
    @Test
    public void testSaveWritesLedgerTruthOnField13AndAccountShadow() throws IOException
    {
        var fixture = fixture();
        var created = new LedgerTransactionCreator(fixture.client()).createDeposit(metadata(),
                        cashLeg(fixture.account(), 100));
        created.getEntry().setUpdatedAt(UPDATED_AT);

        var proto = saveProto(fixture.client());
        var projectionUUID = created.getEntry().getProjectionRefs().get(0).getUUID();

        assertThat(PClient.getDescriptor().findFieldByName("ledger").getNumber(), is(13));
        assertThat(PLedgerProjectionRef.getDescriptor().findFieldByName("primaryPostingUUID"), nullValue());
        assertThat(PLedgerProjectionRef.getDescriptor().findFieldByName("postingGroupUUID"), nullValue());
        assertTrue(proto.hasLedger());
        assertThat(proto.getLedger().getEntriesCount(), is(1));
        assertThat(proto.getLedger().getEntries(0).getUuid(), is(created.getEntry().getUUID()));
        assertThat(proto.getTransactionsCount(), is(1));
        assertThat(proto.getTransactions(0).getUuid(), is(projectionUUID));
        assertThat(proto.getTransactions(0).getType(), is(PTransaction.Type.DEPOSIT));

        var loaded = load(saveBytes(fixture.client()));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0).getUUID(), is(projectionUUID));
        assertThat(loaded.getAllTransactions().size(), is(1));
        assertValid(loaded);
    }

    /**
     * Verifies that dividend protobuf roundtrip preserves ex-date, units, forex, and UUIDs.
     * The restored ledger-backed projection must represent the same dividend booking.
     */
    @Test
    public void testDividendRoundtripPreservesExDateUnitsForexAndUUIDs() throws IOException
    {
        var fixture = fixture();
        var creator = new LedgerTransactionCreator(fixture.client());
        var units = LedgerCreationUnits.of(
                        LedgerCreationUnit.fee(money(2),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2)),
                                                        BigDecimal.ONE)),
                        LedgerCreationUnit.tax(money(4),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4)),
                                                        BigDecimal.ONE)),
                        LedgerCreationUnit.grossValue(money(120),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(120)),
                                                        BigDecimal.ONE)));
        var created = creator.createDividend(metadata(),
                        LedgerDividend.withExDate(
                                        LedgerAccountCashLeg.of(fixture.account(), money(100),
                                                        LedgerForexAmount.of(
                                                                        Money.of(CurrencyUnit.USD,
                                                                                        Values.Amount.factorize(110)),
                                                                        new BigDecimal("1.1000"))),
                                        LedgerOptionalSecurity.of(fixture.security()), units, EX_DATE));
        var entry = created.getEntry();
        var postingUUIDs = entry.getPostings().stream().map(LedgerPosting::getUUID).toList();
        var projectionUUID = entry.getProjectionRefs().get(0).getUUID();
        entry.setUpdatedAt(UPDATED_AT);

        var loaded = load(saveBytes(fixture.client()));
        var reloadedEntry = loaded.getLedger().getEntries().get(0);
        var reloadedProjection = loaded.getAccounts().get(0).getTransactions().get(0);
        var cashPosting = reloadedEntry.getPostings().get(0);

        assertThat(reloadedEntry.getUUID(), is(entry.getUUID()));
        assertThat(reloadedEntry.getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(reloadedEntry.getDateTime(), is(DATE_TIME));
        assertThat(reloadedEntry.getNote(), is("note"));
        assertThat(reloadedEntry.getSource(), is("source"));
        assertThat(reloadedEntry.getUpdatedAt(), is(UPDATED_AT));
        assertThat(reloadedEntry.getPostings().stream().map(LedgerPosting::getUUID).toList(), is(postingUUIDs));
        assertThat(reloadedEntry.getProjectionRefs().get(0).getUUID(), is(projectionUUID));
        assertThat(reloadedEntry.getProjectionRefs().get(0).getPrimaryMembership().orElseThrow().getPostingUUID(),
                        is(postingUUIDs.get(0)));
        assertTrue(reloadedEntry.getProjectionRefs().get(0)
                        .hasMembershipRole(ProjectionMembershipRole.FEE_UNIT));
        assertTrue(reloadedEntry.getProjectionRefs().get(0)
                        .hasMembershipRole(ProjectionMembershipRole.TAX_UNIT));
        assertTrue(reloadedEntry.getProjectionRefs().get(0)
                        .hasMembershipRole(ProjectionMembershipRole.GROSS_VALUE_UNIT));
        assertThat(cashPosting.getForexAmount(), is(Long.valueOf(Values.Amount.factorize(110))));
        assertThat(cashPosting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(cashPosting.getExchangeRate(), is(new BigDecimal("1.1000")));
        assertThat(reloadedProjection.getExDate(), is(EX_DATE));
        assertThat(reloadedProjection.getUnits().count(), is(3L));
        assertTrue(cashPosting.getParameters().stream()
                        .anyMatch(parameter -> parameter.getType() == LedgerParameterType.EX_DATE));
        assertValid(loaded);
    }

    /**
     * Verifies that protobuf load preserves ledger graph identity, order, and parameters.
     * Runtime projections must be rebuilt from the same persisted ledger facts.
     */
    @Test
    public void testProtobufLoadPreservesLedgerGraphIdentityOrderAndParameters() throws IOException
    {
        var fixture = fixture();
        var entry = new LedgerEntry("entry-protobuf-load-graph");
        var cashPosting = new LedgerPosting("posting-protobuf-load-a");
        var feePosting = new LedgerPosting("posting-protobuf-load-b");
        var projection = new LedgerProjectionRef("projection-protobuf-load");

        entry.setType(LedgerEntryType.DIVIDENDS);
        entry.setDateTime(DATE_TIME);
        entry.setNote("protobuf load note");
        entry.setSource("protobuf load source");
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE, "event-reference"));
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND, "SPIN_OFF"));

        cashPosting.setType(LedgerPostingType.CASH);
        cashPosting.setAccount(fixture.account());
        cashPosting.setSecurity(fixture.security());
        cashPosting.setAmount(Values.Amount.factorize(100));
        cashPosting.setCurrency(CurrencyUnit.EUR);
        cashPosting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, EX_DATE));
        cashPosting.addParameter(LedgerParameter.ofMoney(LedgerParameterType.FAIR_MARKET_VALUE, money(100)));

        feePosting.setType(LedgerPostingType.FEE);
        feePosting.setAccount(fixture.account());
        feePosting.setAmount(Values.Amount.factorize(1));
        feePosting.setCurrency(CurrencyUnit.EUR);
        feePosting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY, fixture.security()));

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(fixture.account());
        projection.setPrimaryPostingTargetUUID(cashPosting.getUUID());
        projection.setPostingGroupTargetUUID(feePosting.getUUID());

        entry.addPosting(cashPosting);
        entry.addPosting(feePosting);
        entry.addProjectionRef(projection);
        entry.setUpdatedAt(UPDATED_AT);
        fixture.client().getLedger().addEntry(entry);

        var proto = saveProto(fixture.client());
        var loaded = load(wrap(proto));
        var reloadedEntry = loaded.getLedger().getEntries().get(0);

        assertThat(reloadedEntry.getUUID(), is(entry.getUUID()));
        assertThat(reloadedEntry.getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(reloadedEntry.getDateTime(), is(DATE_TIME));
        assertThat(reloadedEntry.getNote(), is("protobuf load note"));
        assertThat(reloadedEntry.getSource(), is("protobuf load source"));
        assertThat(reloadedEntry.getUpdatedAt(), is(UPDATED_AT));
        assertThat(reloadedEntry.getParameters().stream().map(parameter -> parameter.getType()).toList(),
                        is(List.of(LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.CORPORATE_ACTION_KIND)));
        assertThat(reloadedEntry.getParameters().stream().map(parameter -> parameter.getValue()).toList(),
                        is(List.of("event-reference", "SPIN_OFF")));
        assertThat(reloadedEntry.getPostings().stream().map(LedgerPosting::getUUID).toList(),
                        is(List.of(cashPosting.getUUID(), feePosting.getUUID())));
        assertThat(reloadedEntry.getPostings().get(0).getParameters().stream()
                        .map(parameter -> parameter.getType()).toList(),
                        is(List.of(LedgerParameterType.EX_DATE, LedgerParameterType.FAIR_MARKET_VALUE)));
        assertThat(reloadedEntry.getPostings().get(1).getParameters().get(0).getType(),
                        is(LedgerParameterType.TARGET_SECURITY));
        assertSame(loaded.getSecurities().get(0), reloadedEntry.getPostings().get(1).getParameters().get(0).getValue());
        assertThat(reloadedEntry.getProjectionRefs().get(0).getUUID(), is(projection.getUUID()));
        assertThat(reloadedEntry.getProjectionRefs().get(0).getPrimaryMembership().orElseThrow().getPostingUUID(),
                        is(cashPosting.getUUID()));
        assertThat(reloadedEntry.getProjectionRefs().get(0)
                        .getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).get(0).getPostingUUID(),
                        is(feePosting.getUUID()));
        assertThat(saveProto(loaded).getLedger(), is(proto.getLedger()));
        assertValid(loaded);
    }

    /**
     * Verifies that protobuf save writes derived shadows for cross-entry families.
     * Those shadows must mirror the ledger without becoming independent transaction truth.
     */
    @Test
    public void testSaveWritesDerivedShadowsForCrossEntryFamilies() throws IOException
    {
        var fixture = fixture();
        var creator = new LedgerTransactionCreator(fixture.client());
        var units = LedgerCreationUnits.of(LedgerCreationUnit.fee(money(3),
                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)),
                                        new BigDecimal("0.5000"))));

        var buy = creator.createBuy(metadata(), cashLeg(fixture.account(), 100),
                        securityLeg(fixture.portfolio(), fixture.security(), 5, 100), units)
                        .getEntry();
        var sell = creator.createSell(metadata(), cashLeg(fixture.account(), 50),
                        securityLeg(fixture.portfolio(), fixture.security(), 2, 50), LedgerCreationUnits.none())
                        .getEntry();
        var cashTransfer = creator.createAccountTransfer(metadata(),
                        LedgerCashTransferLeg.of(fixture.account(), money(10)),
                        LedgerCashTransferLeg.of(fixture.otherAccount(), money(10)))
                        .getEntry();
        var securityTransfer = creator.createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(fixture.security(), Values.Share.factorize(3)),
                        LedgerPortfolioTransferLeg.of(fixture.portfolio(), money(30)),
                        LedgerPortfolioTransferLeg.of(fixture.otherPortfolio(), money(30))).getEntry();
        var delivery = creator.createOutboundDelivery(metadata(),
                        LedgerDeliveryLeg.of(fixture.portfolio(),
                                        LedgerSecurityQuantity.of(fixture.security(), Values.Share.factorize(1)),
                                        money(20)))
                        .getEntry();
        var inboundDelivery = creator.createInboundDelivery(metadata(),
                        LedgerDeliveryLeg.of(fixture.otherPortfolio(),
                                        LedgerSecurityQuantity.of(fixture.security(), Values.Share.factorize(4)),
                                        money(80)))
                        .getEntry();

        var proto = saveProto(fixture.client());
        var buyShadow = transaction(proto, PTransaction.Type.PURCHASE);
        var sellShadow = transaction(proto, PTransaction.Type.SALE);
        var cashTransferShadow = transaction(proto, PTransaction.Type.CASH_TRANSFER);
        var securityTransferShadow = transaction(proto, PTransaction.Type.SECURITY_TRANSFER);
        var outboundDeliveryShadow = transaction(proto, PTransaction.Type.OUTBOUND_DELIVERY);
        var inboundDeliveryShadow = transaction(proto, PTransaction.Type.INBOUND_DELIVERY);

        assertThat(proto.getLedger().getEntriesCount(), is(6));
        assertThat(proto.getTransactionsCount(), is(6));

        assertCommonShadowFields(buyShadow, projectionUUID(buy, LedgerProjectionRole.PORTFOLIO),
                        PTransaction.Type.PURCHASE, 100);
        assertThat(buyShadow.getPortfolio(), is(fixture.portfolio().getUUID()));
        assertThat(buyShadow.getAccount(), is(fixture.account().getUUID()));
        assertThat(buyShadow.getOtherUuid(), is(projectionUUID(buy, LedgerProjectionRole.ACCOUNT)));
        assertThat(buyShadow.getSecurity(), is(fixture.security().getUUID()));
        assertThat(buyShadow.getShares(), is(Values.Share.factorize(5)));
        assertThat(buyShadow.getUnitsCount(), is(1));
        assertUnit(buyShadow.getUnits(0), Transaction.Unit.Type.FEE, 3, CurrencyUnit.USD, 6, new BigDecimal("0.5000"));

        assertCommonShadowFields(sellShadow, projectionUUID(sell, LedgerProjectionRole.PORTFOLIO),
                        PTransaction.Type.SALE, 50);
        assertThat(sellShadow.getPortfolio(), is(fixture.portfolio().getUUID()));
        assertThat(sellShadow.getAccount(), is(fixture.account().getUUID()));
        assertThat(sellShadow.getOtherUuid(), is(projectionUUID(sell, LedgerProjectionRole.ACCOUNT)));
        assertThat(sellShadow.getSecurity(), is(fixture.security().getUUID()));
        assertThat(sellShadow.getShares(), is(Values.Share.factorize(2)));

        assertCommonShadowFields(cashTransferShadow, projectionUUID(cashTransfer, LedgerProjectionRole.SOURCE_ACCOUNT),
                        PTransaction.Type.CASH_TRANSFER, 10);
        assertThat(cashTransferShadow.getAccount(), is(fixture.account().getUUID()));
        assertThat(cashTransferShadow.getOtherAccount(), is(fixture.otherAccount().getUUID()));
        assertThat(cashTransferShadow.getOtherUuid(),
                        is(projectionUUID(cashTransfer, LedgerProjectionRole.TARGET_ACCOUNT)));

        assertCommonShadowFields(securityTransferShadow,
                        projectionUUID(securityTransfer, LedgerProjectionRole.SOURCE_PORTFOLIO),
                        PTransaction.Type.SECURITY_TRANSFER, 30);
        assertThat(securityTransferShadow.getPortfolio(), is(fixture.portfolio().getUUID()));
        assertThat(securityTransferShadow.getOtherPortfolio(), is(fixture.otherPortfolio().getUUID()));
        assertThat(securityTransferShadow.getOtherUuid(),
                        is(projectionUUID(securityTransfer, LedgerProjectionRole.TARGET_PORTFOLIO)));
        assertThat(securityTransferShadow.getSecurity(), is(fixture.security().getUUID()));
        assertThat(securityTransferShadow.getShares(), is(Values.Share.factorize(3)));

        assertCommonShadowFields(outboundDeliveryShadow, projectionUUID(delivery, LedgerProjectionRole.DELIVERY_OUTBOUND),
                        PTransaction.Type.OUTBOUND_DELIVERY, 20);
        assertThat(outboundDeliveryShadow.getPortfolio(), is(fixture.portfolio().getUUID()));
        assertThat(outboundDeliveryShadow.getSecurity(), is(fixture.security().getUUID()));
        assertThat(outboundDeliveryShadow.getShares(), is(Values.Share.factorize(1)));

        assertCommonShadowFields(inboundDeliveryShadow,
                        projectionUUID(inboundDelivery, LedgerProjectionRole.DELIVERY_INBOUND),
                        PTransaction.Type.INBOUND_DELIVERY, 80);
        assertThat(inboundDeliveryShadow.getPortfolio(), is(fixture.otherPortfolio().getUUID()));
        assertThat(inboundDeliveryShadow.getSecurity(), is(fixture.security().getUUID()));
        assertThat(inboundDeliveryShadow.getShares(), is(Values.Share.factorize(4)));

        var loaded = load(saveBytes(fixture.client()));

        assertThat(loaded.getLedger().getEntries().size(), is(6));
        assertThat(loaded.getAllTransactions().size(), is(6));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(3));
        assertThat(loaded.getPortfolios().get(0).getTransactions().size(), is(4));
        assertValid(loaded);
    }

    /**
     * Verifies that protobuf files without ledger truth migrate legacy rows on load.
     * The resulting transactions must be materialized from newly created ledger entries.
     */
    @Test
    public void testLoadWithoutLedgerMigratesLegacyRows() throws IOException
    {
        var fixture = fixture();
        var transaction = new AccountTransaction(AccountTransaction.Type.DEPOSIT);
        transaction.setDateTime(DATE_TIME);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(11));
        transaction.setUpdatedAt(UPDATED_AT);
        fixture.account().addTransaction(transaction);

        var oldProto = saveProto(fixture.client()).toBuilder().clearLedger().build();
        var loaded = load(wrap(oldProto));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.DEPOSIT));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0).getUUID(), is(transaction.getUUID()));
        assertValid(loaded);
    }

    /**
     * Verifies that legacy cross-entry families migrate into ledger entries when protobuf has no ledger truth.
     * Each business event must become one ledger entry with derived projections.
     */
    @Test
    public void testLoadWithoutLedgerMigratesLegacyCrossEntryFamilies() throws IOException
    {
        var fixture = fixture();
        var creator = new LedgerTransactionCreator(fixture.client());
        var buy = creator.createBuy(metadata(), cashLeg(fixture.account(), 100),
                        securityLeg(fixture.portfolio(), fixture.security(), 5, 100), LedgerCreationUnits.none())
                        .getEntry();
        var cashTransfer = creator.createAccountTransfer(metadata(),
                        LedgerCashTransferLeg.of(fixture.account(), money(10)),
                        LedgerCashTransferLeg.of(fixture.otherAccount(), money(10))).getEntry();
        var securityTransfer = creator.createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(fixture.security(), Values.Share.factorize(3)),
                        LedgerPortfolioTransferLeg.of(fixture.portfolio(), money(30)),
                        LedgerPortfolioTransferLeg.of(fixture.otherPortfolio(), money(30))).getEntry();
        var delivery = creator.createInboundDelivery(metadata(),
                        LedgerDeliveryLeg.of(fixture.otherPortfolio(),
                                        LedgerSecurityQuantity.of(fixture.security(), Values.Share.factorize(4)),
                                        money(80)))
                        .getEntry();

        var oldProto = saveProto(fixture.client()).toBuilder().clearLedger().build();
        var loaded = load(wrap(oldProto));

        assertThat(loaded.getLedger().getEntries().size(), is(4));
        assertThat(loaded.getAllTransactions().size(), is(4));
        assertProjectionUUIDs(loaded, LedgerEntryType.BUY, projectionUUID(buy, LedgerProjectionRole.ACCOUNT),
                        projectionUUID(buy, LedgerProjectionRole.PORTFOLIO));
        assertProjectionUUIDs(loaded, LedgerEntryType.CASH_TRANSFER,
                        projectionUUID(cashTransfer, LedgerProjectionRole.SOURCE_ACCOUNT),
                        projectionUUID(cashTransfer, LedgerProjectionRole.TARGET_ACCOUNT));
        assertProjectionUUIDs(loaded, LedgerEntryType.SECURITY_TRANSFER,
                        projectionUUID(securityTransfer, LedgerProjectionRole.SOURCE_PORTFOLIO),
                        projectionUUID(securityTransfer, LedgerProjectionRole.TARGET_PORTFOLIO));
        assertProjectionUUIDs(loaded, LedgerEntryType.DELIVERY_INBOUND,
                        projectionUUID(delivery, LedgerProjectionRole.DELIVERY_INBOUND));

        var loadedAccountBuy = ledgerBacked(loaded.getAccounts().get(0).getTransactions(),
                        projectionUUID(buy, LedgerProjectionRole.ACCOUNT));
        var loadedPortfolioBuy = ledgerBacked(loaded.getPortfolios().get(0).getTransactions(),
                        projectionUUID(buy, LedgerProjectionRole.PORTFOLIO));
        assertSame(loadedPortfolioBuy, loadedAccountBuy.getCrossEntry().getCrossTransaction(loadedAccountBuy));
        assertSame(loaded.getPortfolios().get(0), loadedAccountBuy.getCrossEntry().getCrossOwner(loadedAccountBuy));

        var loadedCashTransferOut = ledgerBacked(loaded.getAccounts().get(0).getTransactions(),
                        projectionUUID(cashTransfer, LedgerProjectionRole.SOURCE_ACCOUNT));
        var loadedCashTransferIn = ledgerBacked(loaded.getAccounts().get(1).getTransactions(),
                        projectionUUID(cashTransfer, LedgerProjectionRole.TARGET_ACCOUNT));
        assertThat(((AccountTransaction) loadedCashTransferOut).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(((AccountTransaction) loadedCashTransferIn).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertSame(loadedCashTransferIn, loadedCashTransferOut.getCrossEntry().getCrossTransaction(loadedCashTransferOut));
        assertSame(loaded.getAccounts().get(1), loadedCashTransferOut.getCrossEntry().getCrossOwner(loadedCashTransferOut));

        var loadedSecurityTransferOut = ledgerBacked(loaded.getPortfolios().get(0).getTransactions(),
                        projectionUUID(securityTransfer, LedgerProjectionRole.SOURCE_PORTFOLIO));
        var loadedSecurityTransferIn = ledgerBacked(loaded.getPortfolios().get(1).getTransactions(),
                        projectionUUID(securityTransfer, LedgerProjectionRole.TARGET_PORTFOLIO));
        assertThat(((PortfolioTransaction) loadedSecurityTransferOut).getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(((PortfolioTransaction) loadedSecurityTransferIn).getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertSame(loadedSecurityTransferIn,
                        loadedSecurityTransferOut.getCrossEntry().getCrossTransaction(loadedSecurityTransferOut));
        assertSame(loaded.getPortfolios().get(1),
                        loadedSecurityTransferOut.getCrossEntry().getCrossOwner(loadedSecurityTransferOut));
        assertValid(loaded);
    }

    /**
     * Verifies that a legacy dividend with ex-date, units, and forex migrates from protobuf.
     * The loaded ledger-backed dividend must preserve those facts.
     */
    @Test
    public void testLoadWithoutLedgerMigratesLegacyDividendWithExDateUnitsAndForex() throws IOException
    {
        var fixture = fixture();
        var units = LedgerCreationUnits.of(LedgerCreationUnit.fee(money(2)),
                        LedgerCreationUnit.tax(money(3),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)),
                                                        new BigDecimal("0.5000"))),
                        LedgerCreationUnit.grossValue(money(120),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(240)),
                                                        new BigDecimal("0.5000"))));
        var dividend = new LedgerTransactionCreator(fixture.client())
                        .createDividend(metadata(),
                                        LedgerDividend.withExDate(
                                                        LedgerAccountCashLeg.of(fixture.account(), money(100),
                                                                        LedgerForexAmount.of(
                                                                                        Money.of(CurrencyUnit.USD,
                                                                                                        Values.Amount
                                                                                                                        .factorize(200)),
                                                                                        new BigDecimal("0.5000"))),
                                                        LedgerOptionalSecurity.of(fixture.security()), units, EX_DATE))
                        .getEntry();

        var oldProto = saveProto(fixture.client()).toBuilder().clearLedger().build();
        var loaded = load(wrap(oldProto));
        var entry = onlyEntry(loaded, LedgerEntryType.DIVIDENDS);
        var projection = (AccountTransaction) ledgerBacked(loaded.getAccounts().get(0).getTransactions(),
                        projectionUUID(dividend, LedgerProjectionRole.ACCOUNT));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getAllTransactions().size(), is(1));
        assertSame(loaded.getSecurities().get(0), projection.getSecurity());
        assertThat(projection.getExDate(), is(EX_DATE));
        assertThat(projection.getUnits().count(), is(3L));
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.TAX
                        && Long.valueOf(Values.Amount.factorize(6)).equals(posting.getForexAmount())
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && new BigDecimal("0.5000").compareTo(posting.getExchangeRate()) == 0));
        assertTrue(entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.GROSS_VALUE
                        && Long.valueOf(Values.Amount.factorize(240)).equals(posting.getForexAmount())
                        && CurrencyUnit.USD.equals(posting.getForexCurrency())
                        && new BigDecimal("0.5000").compareTo(posting.getExchangeRate()) == 0));
        assertValid(loaded);
    }

    /**
     * Verifies that protobuf load with ledger truth ignores shadow rows as source truth.
     * Compatibility shadows must not remigrate into duplicate ledger entries.
     */
    @Test
    public void testLoadWithLedgerIgnoresShadowsAsSourceTruth() throws IOException
    {
        var fixture = fixture();
        new LedgerTransactionCreator(fixture.client()).createDeposit(metadata(), cashLeg(fixture.account(), 100));
        var loaded = load(saveBytes(fixture.client()));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(loaded.getAllTransactions().size(), is(1));
        assertValid(loaded);
    }

    /**
     * Verifies that ledger configuration identifiers are written to protobuf.
     * The serialized file must carry the vocabulary needed to load ledger entries.
     */
    @Test
    public void testLedgerConfigIdentifiersAreWritten() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();
        var proto = saveProto(fixture.client());
        var entry = proto.getLedger().getEntries(0);
        var posting = entry.getPostings(0);
        var parameter = posting.getParametersList().stream()
                        .filter(candidate -> candidate.getTypeCode().equals(LedgerParameterType.EX_DATE.getCode()))
                        .findFirst().orElseThrow();

        assertTrue(entry.hasTypeId());
        assertThat(entry.getTypeId(), is(LedgerEntryType.DIVIDENDS.getProtobufId()));
        assertTrue(posting.hasTypeCode());
        assertThat(posting.getTypeCode(), is(LedgerPostingType.CASH.getCode()));
        assertTrue(parameter.hasTypeCode());
        assertThat(parameter.getTypeCode(), is(LedgerParameterType.EX_DATE.getCode()));
    }

    /**
     * Verifies that entry-level parameters are stored under the ledger entry in protobuf.
     * Parameters must not be misplaced into posting or shadow data.
     */
    @Test
    public void testEntryLevelParametersAreWrittenUnderLedgerEntry() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();

        fixture.client().getLedger().getEntries().get(0)
                        .addParameter(LedgerParameter.ofString(
                                        LedgerParameterType.CORPORATE_ACTION_KIND, "SPIN_OFF"));

        var proto = saveProto(fixture.client());
        var entry = proto.getLedger().getEntries(0);
        var parameter = entry.getParameters(0);

        assertThat(PLedgerEntry.getDescriptor().findFieldByName("parameters").getNumber(), is(10));
        assertThat(entry.getParametersCount(), is(1));
        assertThat(parameter.getTypeCode(), is(LedgerParameterType.CORPORATE_ACTION_KIND.getCode()));
        assertThat(parameter.getValueKind(),
                        is(PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_STRING));
        assertThat(parameter.getStringValue(), is("SPIN_OFF"));
    }

    /**
     * Verifies that an unknown ledger entry type id fails with a clear protobuf load error.
     * Unsupported persisted ledger vocabulary must not be interpreted silently.
     */
    @Test
    public void testUnknownLedgerEntryTypeIdFailsClearly() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();
        var proto = saveProto(fixture.client()).toBuilder();

        proto.getLedgerBuilder().getEntriesBuilder(0).setTypeId(999999);

        assertUnknownTypeIdFailure(proto.build(), "LedgerEntryType", 999999);
    }

    /**
     * Verifies that an unknown ledger posting type code fails with a clear protobuf load error.
     * Unsupported posting vocabulary must not be guessed during load.
     */
    @Test
    public void testUnknownLedgerPostingTypeCodeFailsClearly() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();
        var proto = saveProto(fixture.client()).toBuilder();

        proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).setTypeCode("UNKNOWN_POSTING_TYPE");

        assertUnknownTypeCodeFailure(proto.build(), "LedgerPostingType", "UNKNOWN_POSTING_TYPE");
    }

    /**
     * Verifies that a persisted ledger posting without a type code fails during protobuf load.
     * A missing posting type must not be accepted as a default posting shape.
     */
    @Test
    public void testMissingLedgerPostingTypeCodeFailsClearly() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();
        var proto = saveProto(fixture.client()).toBuilder();

        proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).clearTypeCode();

        assertMissingTypeCodeFailure(proto.build(), "LedgerPostingType");
    }

    /**
     * Verifies that an unknown ledger parameter type code fails with a clear protobuf load error.
     * Unsupported parameter vocabulary must not be guessed during load.
     */
    @Test
    public void testUnknownLedgerParameterTypeCodeFailsClearly() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();
        var proto = saveProto(fixture.client()).toBuilder();
        var parameter = proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).getParametersBuilder(0);

        parameter.setTypeCode("UNKNOWN_PARAMETER_TYPE");

        assertUnknownTypeCodeFailure(proto.build(), "LedgerParameterType", "UNKNOWN_PARAMETER_TYPE");
    }

    /**
     * Verifies that a persisted ledger parameter without a type code fails during protobuf load.
     * A missing parameter type must not be accepted as a default parameter shape.
     */
    @Test
    public void testMissingLedgerParameterTypeCodeFailsClearly() throws IOException
    {
        var fixture = fixtureWithDividendAndExDate();
        var proto = saveProto(fixture.client()).toBuilder();
        var parameter = proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).getParametersBuilder(0);

        parameter.clearTypeCode();

        assertMissingTypeCodeFailure(proto.build(), "LedgerParameterType");
    }

    /**
     * Verifies that boolean and local-date ledger parameter fields exist in the protobuf model.
     * These fields are part of the persisted ledger fact vocabulary.
     */
    @Test
    public void testLedgerParameterBooleanAndLocalDateProtobufFieldsAreAvailable()
    {
        assertThat(PLedgerParameterValueKind.forNumber(11),
                        is(PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_BOOLEAN));
        assertThat(PLedgerParameterValueKind.forNumber(12),
                        is(PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE));
        assertThat(PLedgerParameter.getDescriptor().findFieldByName("booleanValue").getNumber(), is(14));
        assertThat(PLedgerParameter.getDescriptor().findFieldByName("localDateValue").getNumber(), is(15));
    }

    /**
     * Verifies that a boolean ledger parameter must use the matching protobuf value field.
     * Loading must reject a mismatched value shape instead of coercing it.
     */
    @Test
    public void testBooleanLedgerParameterProtobufRequiresMatchingValueField() throws IOException
    {
        var proto = fixtureWithDividendAndExDateValueKind(
                        PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_BOOLEAN);

        assertLedgerParameterValueKindFailure(proto.build(), LedgerDiagnosticCode.LEDGER_PERSIST_009
                        .message(Messages.LedgerProtobufBooleanParameterMissingBooleanValue));
    }

    /**
     * Verifies that a local-date ledger parameter must use the matching protobuf value field.
     * Loading must reject a mismatched value shape instead of coercing it.
     */
    @Test
    public void testLocalDateLedgerParameterProtobufRequiresMatchingValueField() throws IOException
    {
        var proto = fixtureWithDividendAndExDateValueKind(
                        PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE);

        assertLedgerParameterValueKindFailure(proto.build(), LedgerDiagnosticCode.LEDGER_PERSIST_010
                        .message(Messages.LedgerProtobufLocalDateParameterMissingLocalDateValue));
    }

    /**
     * Verifies that boolean parameter serialization follows the ledger type policy.
     * The persisted value must roundtrip through the boolean-specific field.
     */
    @Test
    public void testBooleanLedgerParameterProtobufUsesTypePolicy() throws IOException
    {
        var proto = fixtureWithDividendAndExDateValueKind(
                        PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_BOOLEAN);

        proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).getParametersBuilder(0)
                        .setBooleanValue(true);

        assertLedgerParameterValueKindFailure(proto.build(), LedgerDiagnosticCode.LEDGER_CORE_021
                        .message("EX_DATE does not support BOOLEAN; expected LOCAL_DATE_TIME"));
    }

    /**
     * Verifies that local-date parameter serialization follows the ledger type policy.
     * The persisted value must roundtrip through the local-date-specific field.
     */
    @Test
    public void testLocalDateLedgerParameterProtobufUsesTypePolicy() throws IOException
    {
        var proto = fixtureWithDividendAndExDateValueKind(
                        PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE);

        proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).getParametersBuilder(0)
                        .setLocalDateValue(LocalDate.of(2026, 1, 2).toEpochDay());

        assertLedgerParameterValueKindFailure(proto.build(), LedgerDiagnosticCode.LEDGER_CORE_021
                        .message("EX_DATE does not support LOCAL_DATE; expected LOCAL_DATE_TIME"));
    }

    /**
     * Verifies that plan execution refs roundtrip through protobuf.
     * The saved refs must still identify the generated ledger-backed booking after load.
     */
    @Test
    public void testInvestmentPlanLedgerExecutionRefsRoundtrip() throws IOException
    {
        var fixture = fixture();
        var buy = new LedgerTransactionCreator(fixture.client()).createBuy(metadata(), cashLeg(fixture.account(), 100),
                        securityLeg(fixture.portfolio(), fixture.security(), 5, 100), LedgerCreationUnits.none())
                        .getEntry();
        LedgerProjectionService.materialize(fixture.client());
        var portfolioProjection = fixture.portfolio().getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance).findFirst().orElseThrow();
        var plan = plan(fixture);

        plan.getTransactions().add(portfolioProjection);
        fixture.client().addPlan(plan);

        var proto = saveProto(fixture.client());

        assertThat(proto.getPlans(0).getTransactionsCount(), is(0));
        assertThat(proto.getPlans(0).getLedgerExecutionRefsCount(), is(1));
        assertThat(proto.getPlans(0).getLedgerExecutionRefs(0).getLedgerEntryUUID(), is(buy.getUUID()));
        assertThat(proto.getPlans(0).getLedgerExecutionRefs(0).getProjectionUUID(), is(portfolioProjection.getUUID()));
        assertThat(proto.getPlans(0).getLedgerExecutionRefs(0).getProjectionRole(),
                        is(PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_PORTFOLIO));

        var loaded = load(wrap(proto));
        var loadedPlan = loaded.getPlans().get(0);

        assertThat(loadedPlan.getTransactions().size(), is(0));
        assertThat(loadedPlan.getLedgerExecutionRefs().size(), is(1));
        assertThat(loadedPlan.getTransactions(loaded).size(), is(1));
        assertThat(loadedPlan.getTransactions(loaded).get(0).getOwner(), is(loaded.getPortfolios().get(0)));
        assertThat(loadedPlan.getTransactions(loaded).get(0).getTransaction().getUUID(),
                        is(portfolioProjection.getUUID()));
    }

    /**
     * Verifies that ambiguous plan execution refs are rejected during protobuf load.
     * A plan must not silently pick one of several possible projections.
     */
    @Test
    public void testAmbiguousInvestmentPlanLedgerExecutionRefIsRejected() throws IOException
    {
        var fixture = fixture();
        var buy = new LedgerTransactionCreator(fixture.client()).createBuy(metadata(), cashLeg(fixture.account(), 100),
                        securityLeg(fixture.portfolio(), fixture.security(), 5, 100), LedgerCreationUnits.none())
                        .getEntry();
        var plan = plan(fixture);

        plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(buy.getUUID(), null, null));
        fixture.client().addPlan(plan);

        var loaded = load(saveBytes(fixture.client()));

        assertThrows(IllegalArgumentException.class, () -> loaded.getPlans().get(0).getTransactions(loaded));
    }

    /**
     * Verifies that invalid ledger truth loads without protobuf shadow remigration.
     * Invalid persisted ledger entries must not be masked by compatibility rows.
     */
    @Test
    public void testInvalidLedgerTruthLoadsWithoutShadowRemigration() throws IOException
    {
        var fixture = fixture();
        new LedgerTransactionCreator(fixture.client()).createDeposit(metadata(), cashLeg(fixture.account(), 100));

        var proto = saveProto(fixture.client()).toBuilder();
        var entry = proto.getLedgerBuilder().getEntriesBuilder(0);
        entry.addPostings(entry.getPostings(0));
        var loaded = load(wrap(proto.build()));
        var result = LedgerStructuralValidator.validate(loaded.getLedger());

        assertFalse(result.isOK());
        assertTrue(result.hasIssue(LedgerStructuralValidator.IssueCode.DUPLICATE_POSTING_UUID));
        assertThat(loaded.getLedger().getEntries().size(), is(fixture.client().getLedger().getEntries().size()));
        assertTrue(loaded.getAllTransactions().isEmpty());
    }

    /**
     * Verifies that invalid ledger protobuf save failures include formatted diagnostics.
     * The caller must see the concrete validation issue.
     */
    @Test
    public void testInvalidLedgerProtobufSaveExceptionUsesFormattedDiagnostics() throws IOException
    {
        var fixture = fixture();
        var created = new LedgerTransactionCreator(fixture.client()).createDeposit(metadata(),
                        cashLeg(fixture.account(), 100));

        created.getEntry().getPostings().get(0).setCurrency(null);

        var exception = assertThrows(UnsupportedOperationException.class, () -> saveBytes(fixture.client()));

        assertTrue(exception.getMessage(), exception.getMessage().contains("[LEDGER-PERSIST-002] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("[POSTING_CURRENCY_REQUIRED] "));
        assertTrue(exception.getMessage(), exception.getMessage().contains("\n  Posting:\n"));
        assertTrue(exception.getMessage(), exception.getMessage().contains("Currency: <missing>"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":\n"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterDate + ": 2026-01-02T03:04"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterType + ": DEPOSIT"));
        assertTrue(exception.getMessage(), exception.getMessage()
                        .contains(Messages.LedgerDiagnosticMessageFormatterAccount + ": Account"));
    }

    /**
     * Verifies that unsupported legacy rows stay compatible when ledger truth exists.
     * Existing shadows must not override or duplicate the persisted ledger facts.
     */
    @Test
    public void testUnsupportedLegacyRowsRemainCompatibleWhenLedgerTruthExists() throws IOException
    {
        var fixture = fixture();
        new LedgerTransactionCreator(fixture.client()).createDeposit(metadata(), cashLeg(fixture.account(), 100));

        var legacyTransaction = new AccountTransaction(AccountTransaction.Type.FEES);
        legacyTransaction.setDateTime(DATE_TIME);
        legacyTransaction.setCurrencyCode(CurrencyUnit.EUR);
        legacyTransaction.setAmount(Values.Amount.factorize(7));
        legacyTransaction.setUpdatedAt(UPDATED_AT);
        fixture.account().addTransaction(legacyTransaction);

        var loaded = load(saveBytes(fixture.client()));

        assertThat(loaded.getLedger().getEntries().size(), is(1));
        assertThat(loaded.getAccounts().get(0).getTransactions().size(), is(2));
        assertThat(loaded.getAccounts().get(0).getTransactions().stream()
                        .filter(LedgerBackedTransaction.class::isInstance).count(), is(1L));
        assertThat(loaded.getAccounts().get(0).getTransactions().stream()
                        .filter(t -> legacyTransaction.getUUID().equals(t.getUUID())).count(), is(1L));
    }

    private ClientFixture fixture()
    {
        var client = new Client();
        var account = new Account();
        account.setName("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);
        var otherAccount = new Account();
        otherAccount.setName("Other Account");
        otherAccount.setCurrencyCode(CurrencyUnit.EUR);
        otherAccount.setUpdatedAt(UPDATED_AT);
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio");
        portfolio.setUpdatedAt(UPDATED_AT);
        var otherPortfolio = new Portfolio();
        otherPortfolio.setName("Other Portfolio");
        otherPortfolio.setUpdatedAt(UPDATED_AT);
        var security = new Security("Security", CurrencyUnit.EUR);
        security.setUpdatedAt(UPDATED_AT);

        client.addAccount(account);
        client.addAccount(otherAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(otherPortfolio);
        client.addSecurity(security);

        return new ClientFixture(client, account, otherAccount, portfolio, otherPortfolio, security);
    }

    private ClientFixture fixtureWithDividendAndExDate()
    {
        var fixture = fixture();

        new LedgerTransactionCreator(fixture.client()).createDividend(metadata(),
                        LedgerDividend.withExDate(cashLeg(fixture.account(), 100),
                                        LedgerOptionalSecurity.of(fixture.security()), LedgerCreationUnits.none(),
                                        EX_DATE));

        return fixture;
    }

    private LedgerTransactionMetadata metadata()
    {
        return LedgerTransactionMetadata.of(DATE_TIME).withNote("note").withSource("source");
    }

    private LedgerAccountCashLeg cashLeg(Account account, int amount)
    {
        return LedgerAccountCashLeg.of(account, money(amount));
    }

    private LedgerPortfolioSecurityLeg securityLeg(Portfolio portfolio, Security security, int shares, int amount)
    {
        return LedgerPortfolioSecurityLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security, Values.Share.factorize(shares)), money(amount));
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private InvestmentPlan plan(ClientFixture fixture)
    {
        var plan = new InvestmentPlan("Plan");

        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setSecurity(fixture.security());
        plan.setPortfolio(fixture.portfolio());
        plan.setAccount(fixture.account());
        plan.setStart(LocalDate.of(2026, 1, 1));
        plan.setInterval(1);
        plan.setAmount(Values.Amount.factorize(100));

        return plan;
    }

    private PTransaction transaction(PClient client, PTransaction.Type type)
    {
        return client.getTransactionsList().stream().filter(transaction -> transaction.getType() == type).findFirst()
                        .orElseThrow();
    }

    private void assertCommonShadowFields(PTransaction transaction, String uuid, PTransaction.Type type, int amount)
    {
        assertThat(transaction.getUuid(), is(uuid));
        assertThat(transaction.getType(), is(type));
        assertThat(transaction.getDate(), is(name.abuchen.portfolio.util.ProtobufUtil.asTimestamp(DATE_TIME)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(amount)));
        assertThat(transaction.getNote(), is("note"));
        assertThat(transaction.getSource(), is("source"));
    }

    private void assertUnit(name.abuchen.portfolio.model.proto.v1.PTransactionUnit unit, Transaction.Unit.Type type,
                    int amount, String forexCurrency, int forexAmount, BigDecimal exchangeRate)
    {
        assertThat(unit.getAmount(), is(Values.Amount.factorize(amount)));
        assertThat(unit.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(unit.getFxAmount(), is(Values.Amount.factorize(forexAmount)));
        assertThat(unit.getFxCurrencyCode(), is(forexCurrency));
        assertThat(name.abuchen.portfolio.util.ProtobufUtil.fromDecimalValue(unit.getFxRateToBase()),
                        is(exchangeRate));
        assertThat(unit.getType().name(), is(type.name()));
    }

    private void assertProjectionUUIDs(Client client, LedgerEntryType type, String... projectionUUIDs)
    {
        var actual = onlyEntry(client, type).getProjectionRefs().stream().map(ref -> ref.getUUID()).toList();

        assertThat(actual.size(), is(projectionUUIDs.length));
        for (String projectionUUID : projectionUUIDs)
            assertTrue(actual.contains(projectionUUID));
    }

    private name.abuchen.portfolio.model.ledger.LedgerEntry onlyEntry(Client client, LedgerEntryType type)
    {
        return client.getLedger().getEntries().stream().filter(entry -> entry.getType() == type).findFirst()
                        .orElseThrow();
    }

    private Transaction ledgerBacked(List<? extends Transaction> transactions, String uuid)
    {
        return transactions.stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> uuid.equals(transaction.getUUID())).findFirst().orElseThrow();
    }

    private String projectionUUID(name.abuchen.portfolio.model.ledger.LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow()
                        .getUUID();
    }

    private byte[] saveBytes(Client client) throws IOException
    {
        var stream = new ByteArrayOutputStream();

        new ProtobufWriter().save(client, stream);

        return stream.toByteArray();
    }

    private PClient saveProto(Client client) throws IOException
    {
        return parse(saveBytes(client));
    }

    private PClient parse(byte[] bytes) throws IOException
    {
        return PClient.parseFrom(new ByteArrayInputStream(bytes, SIGNATURE.length, bytes.length - SIGNATURE.length));
    }

    private byte[] wrap(PClient client) throws IOException
    {
        var stream = new ByteArrayOutputStream();

        stream.write(SIGNATURE);
        client.writeTo(stream);

        return stream.toByteArray();
    }

    private Client load(byte[] bytes) throws IOException
    {
        return new ProtobufWriter().load(new ByteArrayInputStream(bytes));
    }

    private void assertUnknownTypeIdFailure(PClient client, String typeName, int id) throws IOException
    {
        var exception = assertThrows(IllegalArgumentException.class, () -> load(wrap(client)));

        assertTrue(exception.getMessage(), exception.getMessage().contains(typeName));
        assertTrue(exception.getMessage(), exception.getMessage().contains(Integer.toString(id)));
    }

    private void assertUnknownTypeCodeFailure(PClient client, String typeName, String code) throws IOException
    {
        var exception = assertThrows(IllegalArgumentException.class, () -> load(wrap(client)));

        assertTrue(exception.getMessage(), exception.getMessage().contains(typeName));
        assertTrue(exception.getMessage(), exception.getMessage().contains(code));
    }

    private void assertMissingTypeCodeFailure(PClient client, String typeName) throws IOException
    {
        var exception = assertThrows(IllegalArgumentException.class, () -> load(wrap(client)));

        assertTrue(exception.getMessage(), exception.getMessage().contains("Unknown " + typeName + " code:"));
    }

    private void assertLedgerParameterValueKindFailure(PClient client, String message) throws IOException
    {
        var exception = assertThrows(RuntimeException.class, () -> load(wrap(client)));

        assertTrue(exception.getMessage(), exception.getMessage().contains(message));
    }

    private PClient.Builder fixtureWithDividendAndExDateValueKind(PLedgerParameterValueKind valueKind)
                    throws IOException
    {
        var proto = saveProto(fixtureWithDividendAndExDate().client()).toBuilder();

        proto.getLedgerBuilder().getEntriesBuilder(0).getPostingsBuilder(0).getParametersBuilder(0)
                        .setValueKind(valueKind);

        return proto;
    }

    private void assertValid(Client client)
    {
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    private record ClientFixture(Client client, Account account, Account otherAccount, Portfolio portfolio,
                    Portfolio otherPortfolio, Security security)
    {
    }
}
