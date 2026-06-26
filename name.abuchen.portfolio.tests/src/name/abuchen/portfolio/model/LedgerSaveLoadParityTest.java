package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.ProjectionMembership;
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
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingPatch;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerUnitPostingUpdater;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionRef;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests persistence compatibility for ledger-backed transactions.
 * These tests make sure save/load rebuilds runtime rows from ledger truth and keeps compatibility data stable.
 */
@SuppressWarnings("nls")
public class LedgerSaveLoadParityTest
{
    private static final byte[] PROTOBUF_SIGNATURE = new byte[] { 'P', 'P', 'P', 'B', 'V', '1' };
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 3, 4, 5, 6);
    private static final LocalDateTime EX_DATE = LocalDateTime.of(2026, 2, 27, 0, 0);
    private static final Instant UPDATED_AT = Instant.parse("2026-03-04T05:06:07Z");

    /**
     * Verifies that XML save-load-save keeps ledger truth and runtime projections equivalent.
     * Repeated persistence must not drift owner lists away from the ledger.
     */
    @Test
    public void testXmlSaveLoadSavePreservesLedgerTruthAndRuntimeProjectionParity() throws Exception
    {
        var fixture = parityFixture();
        var expectedLedger = ledgerSnapshot(fixture.client());
        var expectedTransactions = transactionSnapshots(fixture.client());
        var expectedPlans = planSnapshots(fixture.client());

        var firstXml = saveXml(fixture.client());
        var loaded = loadXml(firstXml);
        var secondXml = saveXml(loaded);
        var reloaded = loadXml(secondXml);

        assertXmlContainsLedgerTruthOnly(secondXml);
        assertParity(reloaded, expectedLedger, expectedTransactions, expectedPlans);
    }

    /**
     * Verifies that protobuf save-load-save keeps ledger truth and runtime projections equivalent.
     * Repeated persistence must not drift owner lists away from the ledger.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesLedgerTruthAndRuntimeProjectionParity() throws Exception
    {
        var fixture = parityFixture();
        var expectedLedger = ledgerSnapshot(fixture.client());
        var expectedTransactions = transactionSnapshots(fixture.client());
        var expectedPlans = planSnapshots(fixture.client());
        var expectedMemberships = membershipSnapshots(fixture.client());

        var loaded = loadProtobuf(saveProtobuf(fixture.client()));
        var secondBytes = saveProtobuf(loaded);
        var reloaded = loadProtobuf(secondBytes);
        var secondProto = parseProtobuf(secondBytes);

        assertThat(secondProto.getLedger().getEntriesCount(), is(8));
        assertThat(secondProto.getTransactionsCount(), is(8));
        assertThat(membershipSnapshots(loaded), is(expectedMemberships));
        assertThat(membershipSnapshots(reloaded), is(expectedMemberships));
        assertParity(reloaded, expectedLedger, expectedTransactions, expectedPlans);
    }

    /**
     * Verifies that XML and protobuf restore equivalent ledger truth and projections.
     * Both formats must materialize the same owner-list views from the persisted ledger.
     */
    @Test
    public void testXmlAndProtobufRoundtripsRestoreEquivalentLedgerTruthAndProjections() throws Exception
    {
        var fixture = parityFixture();
        var xmlLoaded = loadXml(saveXml(fixture.client()));
        var protobufLoaded = loadProtobuf(saveProtobuf(fixture.client()));

        assertThat(ledgerSnapshot(protobufLoaded), is(ledgerSnapshot(xmlLoaded)));
        assertThat(transactionSnapshots(protobufLoaded), is(transactionSnapshots(xmlLoaded)));
        assertThat(planSnapshots(protobufLoaded), is(planSnapshots(xmlLoaded)));
    }

    /**
     * Verifies that ledger parameters remain owned by their entry or posting after both roundtrips.
     * Persistence must not move business facts between ledger levels.
     */
    @Test
    public void testLedgerParametersRemainOwnedByEntryOrPostingAfterXmlAndProtobufRoundtrip() throws Exception
    {
        var client = new Client();
        var account = register(client, account("Account"));
        var security = register(client, security());
        var targetSecurity = register(client, security());
        var entry = new LedgerEntry("entry-parameter-ownership");
        var cashPosting = new LedgerPosting("posting-a");
        var targetedPosting = new LedgerPosting("posting-b");
        var projection = new LedgerProjectionRef("projection-parameter-ownership");

        entry.setType(LedgerEntryType.DIVIDENDS);
        entry.setDateTime(DATE_TIME);
        entry.setUpdatedAt(UPDATED_AT);
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                        "SPIN_OFF"));
        entry.addParameter(LedgerParameter.ofBoolean(LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        Boolean.TRUE));

        cashPosting.setType(LedgerPostingType.CASH);
        cashPosting.setAccount(account);
        cashPosting.setSecurity(security);
        cashPosting.setAmount(Values.Amount.factorize(10));
        cashPosting.setCurrency(CurrencyUnit.EUR);
        cashPosting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        EX_DATE));

        targetedPosting.setType(LedgerPostingType.FEE);
        targetedPosting.setAccount(account);
        targetedPosting.setAmount(Values.Amount.factorize(1));
        targetedPosting.setCurrency(CurrencyUnit.EUR);
        targetedPosting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY,
                        targetSecurity));

        entry.addPosting(cashPosting);
        entry.addPosting(targetedPosting);

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(account);
        projection.setPrimaryPosting(cashPosting);
        entry.addProjectionRef(projection);

        client.getLedger().addEntry(entry);

        assertLedgerParameterOwnership(client, targetSecurity.getUUID());

        var xmlLoaded = loadXml(saveXml(client));

        assertLedgerParameterOwnership(xmlLoaded, targetSecurity.getUUID());

        var protobufLoaded = loadProtobuf(saveProtobuf(client));

        assertLedgerParameterOwnership(protobufLoaded, targetSecurity.getUUID());
    }

    /**
     * Verifies that the new ledger parameter vocabulary roundtrips through XML and protobuf.
     * Boolean and local-date facts must survive in both persistence formats.
     */
    @Test
    public void testNewLedgerParameterVocabularyRoundtripsThroughXmlAndProtobuf() throws Exception
    {
        var client = new Client();
        var account = register(client, account("Account"));
        var portfolio = register(client, portfolio("Portfolio"));
        var security = register(client, security());
        var rightSecurity = register(client, security());
        var entry = new LedgerEntry("entry-new-parameter-vocabulary");
        var cashPosting = new LedgerPosting("posting-new-vocabulary-a");
        var feePosting = new LedgerPosting("posting-new-vocabulary-b");
        var projection = new LedgerProjectionRef("projection-new-parameter-vocabulary");
        var recordDate = LocalDate.of(2026, 3, 1);
        var nominalValue = money(42);

        entry.setType(LedgerEntryType.DIVIDENDS);
        entry.setDateTime(DATE_TIME);
        entry.setUpdatedAt(UPDATED_AT);

        cashPosting.setType(LedgerPostingType.CASH);
        cashPosting.setAccount(account);
        cashPosting.setSecurity(security);
        cashPosting.setAmount(Values.Amount.factorize(10));
        cashPosting.setCurrency(CurrencyUnit.EUR);
        cashPosting.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.RECORD_DATE,
                        recordDate));
        cashPosting.addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        EX_DATE));
        cashPosting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR,
                        new BigDecimal("1.25")));
        cashPosting.addParameter(LedgerParameter.ofBoolean(LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        Boolean.TRUE));
        cashPosting.addParameter(LedgerParameter.ofMoney(LedgerParameterType.NOMINAL_VALUE,
                        nominalValue));

        feePosting.setType(LedgerPostingType.FEE);
        feePosting.setAccount(account);
        feePosting.setAmount(Values.Amount.factorize(1));
        feePosting.setCurrency(CurrencyUnit.EUR);
        feePosting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.RIGHT_SECURITY,
                        rightSecurity));
        feePosting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_KIND,
                        "SPIN_OFF"));
        feePosting.addParameter(LedgerParameter.ofAccount(LedgerParameterType.SOURCE_ACCOUNT,
                        account));
        feePosting.addParameter(LedgerParameter.ofPortfolio(LedgerParameterType.SOURCE_PORTFOLIO,
                        portfolio));

        entry.addPosting(cashPosting);
        entry.addPosting(feePosting);

        projection.setRole(LedgerProjectionRole.ACCOUNT);
        projection.setAccount(account);
        projection.setPrimaryPosting(cashPosting);
        entry.addProjectionRef(projection);

        client.getLedger().addEntry(entry);

        assertNewLedgerParameterVocabulary(client, recordDate, nominalValue, rightSecurity.getUUID(), account.getUUID(),
                        portfolio.getUUID());

        var xml = saveXml(client);

        assertCompactLedgerParameterXml(xml, nominalValue);

        var xmlLoaded = loadXml(xml);

        assertNewLedgerParameterVocabulary(xmlLoaded, recordDate, nominalValue, rightSecurity.getUUID(),
                        account.getUUID(), portfolio.getUUID());

        var protobufLoaded = loadProtobuf(saveProtobuf(client));

        assertNewLedgerParameterVocabulary(protobufLoaded, recordDate, nominalValue, rightSecurity.getUUID(),
                        account.getUUID(), portfolio.getUUID());
    }

    /**
     * Verifies that hidden transfer units and primary forex facts survive both formats.
     * Transfer persistence must not lose facts that are not visible as normal runtime projections.
     */
    @Test
    public void testXmlAndProtobufRoundtripsPreserveHiddenTransferUnitsAndPrimaryForex() throws Exception
    {
        var fixture = hiddenTransferFactsFixture();
        var expectedLedger = ledgerSnapshot(fixture.client());
        var expectedTransactions = transactionSnapshots(fixture.client());

        var xmlLoaded = loadXml(saveXml(fixture.client()));
        var protobufLoaded = loadProtobuf(saveProtobuf(fixture.client()));

        assertValid(xmlLoaded);
        assertThat(ledgerSnapshot(xmlLoaded), is(expectedLedger));
        assertThat(transactionSnapshots(xmlLoaded), is(expectedTransactions));

        assertValid(protobufLoaded);
        assertThat(ledgerSnapshot(protobufLoaded), is(expectedLedger));
        assertThat(transactionSnapshots(protobufLoaded), is(expectedTransactions));
    }

    /**
     * Verifies that deposit and removal units survive XML and protobuf roundtrips.
     * Splitting or standalone cash bookings must keep their unit facts.
     */
    @Test
    public void testXmlAndProtobufRoundtripsPreserveDepositAndRemovalUnits() throws Exception
    {
        var fixture = depositRemovalUnitFactsFixture();
        var expectedLedger = ledgerSnapshot(fixture.client());
        var expectedTransactions = transactionSnapshots(fixture.client());

        var xmlLoaded = loadXml(saveXml(fixture.client()));
        var protobufLoaded = loadProtobuf(saveProtobuf(fixture.client()));

        assertValid(xmlLoaded);
        assertThat(ledgerSnapshot(xmlLoaded), is(expectedLedger));
        assertThat(transactionSnapshots(xmlLoaded), is(expectedTransactions));

        assertValid(protobufLoaded);
        assertThat(ledgerSnapshot(protobufLoaded), is(expectedLedger));
        assertThat(transactionSnapshots(protobufLoaded), is(expectedTransactions));
    }

    /**
     * Verifies that newly created standard ledger transactions rematerialize and roundtrip.
     * Creator output must remain stable through both persistence formats.
     */
    @Test
    public void testNewlyCreatedStandardTransactionsRematerializeAndRoundtrip() throws Exception
    {
        var fixture = standardCreationFixture();
        var expectedLedger = ledgerSnapshot(fixture.client());
        var expectedProjectionUUIDs = projectionUUIDs(fixture.client());

        clearLedgerBackedRuntimeProjections(fixture.client());
        var result = LedgerProjectionService.restoreIfValid(fixture.client());

        assertTrue(result.format(), result.isOK());
        assertThat(ledgerSnapshot(fixture.client()), is(expectedLedger));
        assertMaterializedProjectionUUIDs(fixture.client(), expectedProjectionUUIDs);
        assertThat(fixture.client().getAllTransactions().size(), is(fixture.client().getLedger().getEntries().size()));

        var xmlLoaded = loadXml(saveXml(fixture.client()));
        assertThat(ledgerSnapshot(xmlLoaded), is(expectedLedger));
        assertMaterializedProjectionUUIDs(xmlLoaded, expectedProjectionUUIDs);
        assertThat(xmlLoaded.getAllTransactions().size(), is(xmlLoaded.getLedger().getEntries().size()));

        var protobufLoaded = loadProtobuf(saveProtobuf(fixture.client()));
        assertThat(ledgerSnapshot(protobufLoaded), is(expectedLedger));
        assertMaterializedProjectionUUIDs(protobufLoaded, expectedProjectionUUIDs);
        assertThat(protobufLoaded.getAllTransactions().size(), is(protobufLoaded.getLedger().getEntries().size()));
    }

    /**
     * Verifies that old scalar-only XML remains loadable and is adapted in memory.
     * Backward compatibility must not require persisted projection memberships.
     */
    @Test
    public void testOldScalarOnlyXmlLoadsAndSavesMemberships() throws Exception
    {
        var fixture = standardCreationFixture();
        var scalarOnlyXml = stripMemberships(addScalarProjectionTargets(saveXml(fixture.client()), fixture.client()));
        var loaded = loadXml(scalarOnlyXml);
        var resavedXml = saveXml(loaded);

        assertValid(loaded);
        assertMaterializedProjectionUUIDs(loaded, projectionUUIDs(fixture.client()));
        assertTrue(membershipSnapshots(loaded).stream()
                        .anyMatch(membership -> membership.role() == ProjectionMembershipRole.PRIMARY));
        assertTrue(resavedXml.contains("<membership postingUUID=\""));
        assertFalse(resavedXml.contains("<primaryPostingUUID>"));
        assertFalse(resavedXml.contains("<postingGroupUUID>"));
    }

    /**
     * Verifies that XML persists projection memberships on new files.
     * Membership roles and posting targets must survive save/load/save.
     */
    @Test
    public void testXmlSaveLoadSavePreservesProjectionMemberships() throws Exception
    {
        var fixture = standardCreationFixture();
        var expectedMemberships = membershipSnapshots(fixture.client());
        var firstXml = saveXml(fixture.client());
        var loaded = loadXml(firstXml);
        var secondXml = saveXml(loaded);
        var reloaded = loadXml(secondXml);
        var legacyEmptyParametersLoaded = loadXml(addEmptyLedgerParameterCollections(firstXml));

        assertTrue(firstXml.contains("<membership postingUUID=\""));
        assertFalse(firstXml.contains("<parameters/>"));
        assertFalse(firstXml.contains("<primaryPostingUUID>"));
        assertFalse(firstXml.contains("<postingGroupUUID>"));
        assertTrue(firstXml.matches("(?s).*<ledger-entry(?=[^>]* uuid=\")(?=[^>]* type=\")" //$NON-NLS-1$
                        + "(?=[^>]* dateTime=\")(?=[^>]* updatedAt=\")[^>]*>.*")); //$NON-NLS-1$
        assertTrue(firstXml.matches("(?s).*<ledger-posting(?=[^>]* uuid=\")(?=[^>]* type=\")" //$NON-NLS-1$
                        + "(?=[^>]* amount=\")(?=[^>]* currency=\")(?=[^>]* shares=\")[^>]*>.*")); //$NON-NLS-1$
        assertTrue(firstXml.matches("(?s).*<ledger-projection-ref(?=[^>]* uuid=\")(?=[^>]* role=\")[^>]*>.*")); //$NON-NLS-1$
        assertFalse(firstXml.matches("(?s).*<ledger-projection-ref[^>]*>(?:(?!</ledger-projection-ref>).)*<uuid>.*")); //$NON-NLS-1$
        assertFalse(firstXml.matches("(?s).*<ledger-projection-ref[^>]*>(?:(?!</ledger-projection-ref>).)*<role>.*")); //$NON-NLS-1$
        assertTrue(firstXml.contains("<account reference=\""));
        assertTrue(firstXml.contains("<security reference=\""));
        assertTrue(firstXml.contains("<portfolio reference=\""));
        assertThat(membershipSnapshots(loaded), is(expectedMemberships));
        assertThat(membershipSnapshots(reloaded), is(expectedMemberships));
        assertTrue(legacyEmptyParametersLoaded.getLedger().getEntries().get(0).getParameters().isEmpty());
        assertTrue(legacyEmptyParametersLoaded.getLedger().getEntries().get(0).getPostings().get(0).getParameters()
                        .isEmpty());
        assertMaterializedProjectionUUIDs(legacyEmptyParametersLoaded, projectionUUIDs(fixture.client()));
    }

    /**
     * Verifies that protobuf persists projection memberships on new files.
     * Membership roles and posting targets must survive save/load/save.
     */
    @Test
    public void testProtobufSaveLoadSavePreservesProjectionMemberships() throws Exception
    {
        var fixture = parityFixture();
        var expectedMemberships = membershipSnapshots(fixture.client());
        var firstBytes = saveProtobuf(fixture.client());
        var loaded = loadProtobuf(firstBytes);
        var secondBytes = saveProtobuf(loaded);
        var reloaded = loadProtobuf(secondBytes);

        assertTrue(parseProtobuf(firstBytes).getLedger().getEntriesList().stream()
                        .flatMap(entry -> entry.getProjectionRefsList().stream())
                        .anyMatch(projection -> projection.getMembershipsCount() > 0));
        assertThat(PLedgerProjectionRef.getDescriptor().findFieldByName("primaryPostingUUID"), nullValue());
        assertThat(PLedgerProjectionRef.getDescriptor().findFieldByName("postingGroupUUID"), nullValue());
        assertTrue(expectedMemberships.stream()
                        .anyMatch(membership -> membership.role() == ProjectionMembershipRole.FEE_UNIT));
        assertTrue(expectedMemberships.stream()
                        .anyMatch(membership -> membership.role() == ProjectionMembershipRole.TAX_UNIT));
        assertTrue(expectedMemberships.stream()
                        .anyMatch(membership -> membership.role() == ProjectionMembershipRole.GROSS_VALUE_UNIT));
        assertThat(membershipSnapshots(loaded), is(expectedMemberships));
        assertThat(membershipSnapshots(reloaded), is(expectedMemberships));
    }

    /**
     * Verifies that XML scalar and membership target conflicts are diagnosed.
     * Load recovery may return the client, but validation must keep the conflict visible.
     */
    @Test
    public void testXmlScalarMembershipConflictIsDiagnosed() throws Exception
    {
        var fixture = standardCreationFixture();
        var entry = fixture.client().getLedger().getEntries().stream()
                        .filter(candidate -> candidate.getPostings().size() > 1).findFirst().orElseThrow();
        var projection = entry.getProjectionRefs().get(0);
        var alternatePostingUUID = entry.getPostings().stream()
                        .map(LedgerPosting::getUUID)
                        .filter(uuid -> !uuid.equals(projection.getPrimaryPostingUUID()))
                        .findFirst().orElseThrow();
        var conflictingXml = addScalarProjectionTargets(saveXml(fixture.client()), fixture.client()).replaceFirst(
                        "(?s)(<membership postingUUID=\")" //$NON-NLS-1$
                                        + java.util.regex.Pattern.quote(projection.getPrimaryPostingUUID())
                                        + "(\" role=\"PRIMARY\"\\s*/>)", //$NON-NLS-1$
                        "$1" + alternatePostingUUID + "$2"); //$NON-NLS-1$ //$NON-NLS-2$
        var loaded = loadXml(conflictingXml);
        var result = LedgerStructuralValidator.validate(loaded.getLedger());

        assertFalse(result.isOK());
        assertTrue(result.hasIssue(LedgerStructuralValidator.IssueCode.PROJECTION_PRIMARY_TARGET_CONFLICT));
    }

    /**
     * Verifies that invalid XML projection memberships do not make a parseable file unloadable.
     * The invalid Ledger entry remains available and is not materialized.
     */
    @Test
    public void testInvalidXmlProjectionMembershipLoadsWithRecovery() throws Exception
    {
        var fixture = standardCreationFixture();
        var brokenProjectionUUID = fixture.client().getLedger().getEntries().get(0).getProjectionRefs().get(0)
                        .getUUID();
        var projection = fixture.client().getLedger().getEntries().get(0).getProjectionRefs().get(0);
        var invalidXml = saveXml(fixture.client()).replaceFirst(
                        "<membership postingUUID=\"" + projection.getPrimaryPostingUUID() + "\"",
                        "<membership postingUUID=\"missing-membership-posting\"");
        var loaded = loadXml(invalidXml);
        var result = LedgerStructuralValidator.validate(loaded.getLedger());

        assertFalse(result.isOK());
        assertTrue(result.hasIssue(LedgerStructuralValidator.IssueCode.PROJECTION_MEMBERSHIP_REF_NOT_FOUND));
        assertInvalidLedgerLoadInvariants(loaded, fixture, brokenProjectionUUID);
    }

    /**
     * Verifies that invalid protobuf projection memberships do not make a parseable file unloadable.
     * The invalid Ledger entry remains available and is not materialized.
     */
    @Test
    public void testInvalidProtobufProjectionMembershipLoadsWithRecovery() throws Exception
    {
        var fixture = standardCreationFixture();
        var brokenProjectionUUID = fixture.client().getLedger().getEntries().get(0).getProjectionRefs().get(0)
                        .getUUID();
        var proto = parseProtobuf(saveProtobuf(fixture.client())).toBuilder();

        proto.getLedgerBuilder().getEntriesBuilder(0).getProjectionRefsBuilder(0).getMembershipsBuilder(0)
                        .setPostingUUID("missing-membership-posting");

        var loaded = loadProtobuf(wrapProtobuf(proto.build()));
        var result = LedgerStructuralValidator.validate(loaded.getLedger());

        assertFalse(result.isOK());
        assertTrue(result.hasIssue(LedgerStructuralValidator.IssueCode.PROJECTION_MEMBERSHIP_REF_NOT_FOUND));
        assertInvalidLedgerLoadInvariants(loaded, fixture, brokenProjectionUUID);
    }

    /**
     * Verifies that XML ledger truth with a broken primary posting ref loads without materialization.
     * The invalid Ledger entry must remain available for later diagnostic and repair workflows.
     */
    @Test
    public void testInvalidXmlLedgerPrimaryPostingRefLoadsWithClientDataAndValidProjections() throws Exception
    {
        var fixture = parityFixture();
        var brokenProjectionUUID = fixture.client().getLedger().getEntries().get(0).getProjectionRefs().get(0)
                        .getUUID();
        var invalidXml = addScalarProjectionTargets(saveXml(fixture.client()), fixture.client()).replaceFirst(
                        "<primaryPostingUUID>[^<]+</primaryPostingUUID>",
                        "<primaryPostingUUID>missing-primary-posting</primaryPostingUUID>");
        var loaded = loadXml(invalidXml);
        var result = LedgerStructuralValidator.validate(loaded.getLedger());

        assertFalse(result.isOK());
        assertTrue(result.hasIssue(LedgerStructuralValidator.IssueCode.PRIMARY_POSTING_REF_NOT_FOUND));
        assertInvalidLedgerLoadInvariants(loaded, fixture, brokenProjectionUUID);
    }

    /**
     * Verifies that invalid protobuf ledger truth loads without shadow remigration.
     * Compatibility rows must not hide an invalid persisted ledger entry.
     */
    @Test
    public void testInvalidProtobufLedgerLoadsWithoutShadowRemigration() throws Exception
    {
        var fixture = parityFixture();
        var brokenProjectionUUID = fixture.client().getLedger().getEntries().get(0).getProjectionRefs().get(0)
                        .getUUID();
        var proto = parseProtobuf(saveProtobuf(fixture.client())).toBuilder();
        var entry = proto.getLedgerBuilder().getEntriesBuilder(0);

        entry.addPostings(entry.getPostings(0));
        var loaded = loadProtobuf(wrapProtobuf(proto.build()));
        var result = LedgerStructuralValidator.validate(loaded.getLedger());

        assertFalse(result.isOK());
        assertTrue(result.hasIssue(LedgerStructuralValidator.IssueCode.DUPLICATE_POSTING_UUID));
        assertInvalidLedgerLoadInvariants(loaded, fixture, brokenProjectionUUID);
    }

    /**
     * Verifies that strict XML save validation does not create or truncate the Save As target.
     */
    @Test
    public void testInvalidLedgerXmlSaveAsDoesNotCreateOrTruncateTarget() throws Exception
    {
        assertInvalidLedgerSaveAsDoesNotCreateOrTruncateTarget(EnumSet.of(SaveFlag.XML));
    }

    /**
     * Verifies that strict protobuf save validation does not create or truncate the Save As target.
     */
    @Test
    public void testInvalidLedgerProtobufSaveAsDoesNotCreateOrTruncateTarget() throws Exception
    {
        assertInvalidLedgerSaveAsDoesNotCreateOrTruncateTarget(EnumSet.of(SaveFlag.BINARY));
    }

    /**
     * Verifies that ambiguous multi-projection plan refs stay rejected after roundtrips.
     * Persistence must not turn an unresolved generated booking into a guessed match.
     */
    @Test
    public void testAmbiguousMultiProjectionInvestmentPlanRefStaysRejectedAfterRoundtrips() throws Exception
    {
        var ambiguousXml = ambiguousPlanFixture();
        var xmlLoaded = loadXml(saveXml(ambiguousXml.client()));
        var protobufLoaded = loadProtobuf(saveProtobuf(ambiguousXml.client()));

        assertThrows(IllegalArgumentException.class, () -> plan(xmlLoaded, "Ambiguous Plan").getTransactions(xmlLoaded));
        assertThrows(IllegalArgumentException.class,
                        () -> plan(protobufLoaded, "Ambiguous Plan").getTransactions(protobufLoaded));
    }

    private void assertParity(Client client, LedgerSnapshot expectedLedger, List<TransactionSnapshot> expectedTransactions,
                    List<PlanSnapshot> expectedPlans)
    {
        assertValid(client);
        assertThat(ledgerSnapshot(client), is(expectedLedger));
        assertThat(transactionSnapshots(client), is(expectedTransactions));
        assertThat(planSnapshots(client), is(expectedPlans));
        assertPlanExecutionRefsRestored(client);
        assertNoDuplicates(client);
    }

    private void assertNoDuplicates(Client client)
    {
        var projectionUUIDs = client.getLedger().getEntries().stream()
                        .flatMap(entry -> entry.getProjectionRefs().stream()).map(LedgerProjectionRef::getUUID)
                        .toList();
        var materializedUUIDs = client.getAccounts().stream().flatMap(account -> account.getTransactions().stream())
                        .filter(LedgerBackedTransaction.class::isInstance).map(Transaction::getUUID)
                        .toList();
        materializedUUIDs = java.util.stream.Stream
                        .concat(materializedUUIDs.stream(),
                                        client.getPortfolios().stream()
                                                        .flatMap(portfolio -> portfolio.getTransactions().stream())
                                                        .filter(LedgerBackedTransaction.class::isInstance)
                                                        .map(Transaction::getUUID))
                        .toList();

        assertThat(client.getLedger().getEntries().size(), is(8));
        assertThat(client.getAllTransactions().size(), is(8));
        assertThat(projectionUUIDs.stream().distinct().count(), is((long) projectionUUIDs.size()));
        assertThat(materializedUUIDs.stream().distinct().count(), is((long) materializedUUIDs.size()));
        assertThat(materializedUUIDs.stream().sorted().toList(), is(projectionUUIDs.stream().sorted().toList()));
    }

    private void assertMaterializedProjectionUUIDs(Client client, List<String> expectedProjectionUUIDs)
    {
        assertValid(client);
        assertThat(materializedProjectionUUIDs(client), is(expectedProjectionUUIDs));
        assertTrue(client.getAllTransactions().stream().allMatch(pair -> pair.getTransaction()
                        instanceof LedgerBackedTransaction));
    }

    private List<String> projectionUUIDs(Client client)
    {
        return client.getLedger().getEntries().stream() //
                        .flatMap(entry -> entry.getProjectionRefs().stream()) //
                        .map(LedgerProjectionRef::getUUID) //
                        .sorted() //
                        .toList();
    }

    private List<MembershipSnapshot> membershipSnapshots(Client client)
    {
        return client.getLedger().getEntries().stream() //
                        .flatMap(entry -> entry.getProjectionRefs().stream()) //
                        .flatMap(projection -> projection.getMemberships().stream()
                                        .map(membership -> new MembershipSnapshot(projection.getUUID(),
                                                        membership.getPostingUUID(), membership.getRole()))) //
                        .sorted(Comparator.comparing(MembershipSnapshot::projectionUUID)
                                        .thenComparing(MembershipSnapshot::postingUUID)
                                        .thenComparing(MembershipSnapshot::role)) //
                        .toList();
    }

    private String stripMemberships(String xml)
    {
        return xml.replaceAll("(?s)\\s*<memberships>.*?</memberships>", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String addScalarProjectionTargets(String xml, Client client)
    {
        var updatedXml = xml;

        for (var entry : client.getLedger().getEntries())
        {
            for (var projection : entry.getProjectionRefs())
            {
                var scalarTargets = new StringBuilder();

                if (projection.getPrimaryPostingUUID() != null)
                    scalarTargets.append("<primaryPostingUUID>").append(projection.getPrimaryPostingUUID())
                                    .append("</primaryPostingUUID>"); //$NON-NLS-1$ //$NON-NLS-2$

                if (projection.getPostingGroupUUID() != null)
                    scalarTargets.append("<postingGroupUUID>").append(projection.getPostingGroupUUID())
                                    .append("</postingGroupUUID>"); //$NON-NLS-1$ //$NON-NLS-2$

                if (scalarTargets.length() == 0)
                    continue;

                updatedXml = updatedXml.replaceFirst(
                                "(?s)(<ledger-projection-ref[^>]*\\buuid=\"" //$NON-NLS-1$
                                                + java.util.regex.Pattern.quote(projection.getUUID())
                                                + "\"[^>]*>)", //$NON-NLS-1$
                                "$1" + java.util.regex.Matcher.quoteReplacement(scalarTargets.toString())); //$NON-NLS-1$
            }
        }

        return updatedXml;
    }

    private String addEmptyLedgerParameterCollections(String xml)
    {
        return xml.replaceFirst("(<ledger-entry[^>]*>)", "$1<parameters/>") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceFirst("(<ledger-posting[^>]*>)", "$1<parameters/>"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private List<String> materializedProjectionUUIDs(Client client)
    {
        return java.util.stream.Stream
                        .concat(client.getAccounts().stream().flatMap(account -> account.getTransactions().stream()),
                                        client.getPortfolios().stream()
                                                        .flatMap(portfolio -> portfolio.getTransactions().stream()))
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .map(Transaction::getUUID) //
                        .sorted() //
                        .toList();
    }

    private void clearLedgerBackedRuntimeProjections(Client client)
    {
        for (var account : client.getAccounts())
            account.getTransactions().removeIf(LedgerBackedTransaction.class::isInstance);

        for (var portfolio : client.getPortfolios())
            portfolio.getTransactions().removeIf(LedgerBackedTransaction.class::isInstance);
    }

    private void assertXmlContainsLedgerTruthOnly(String xml)
    {
        assertTrue(xml.contains("<ledger>"));
        assertThat(xml.contains("LedgerBacked"), is(false));
        assertThat(xml.contains("<account-transaction"), is(false));
        assertThat(xml.contains("<portfolio-transaction"), is(false));
    }

    private LedgerSnapshot ledgerSnapshot(Client client)
    {
        return new LedgerSnapshot(client.getLedger().getEntries().stream().map(this::entrySnapshot)
                        .sorted(Comparator.comparing(EntrySnapshot::uuid)).toList());
    }

    private EntrySnapshot entrySnapshot(LedgerEntry entry)
    {
        return new EntrySnapshot(entry.getUUID(), entry.getType(), entry.getDateTime(), entry.getNote(),
                        entry.getSource(), entry.getUpdatedAt(),
                        entry.getParameters().stream().map(this::parameterSnapshot)
                                        .sorted(Comparator.comparing(ParameterSnapshot::type)
                                                        .thenComparing(ParameterSnapshot::value))
                                        .toList(),
                        entry.getPostings().stream().map(this::postingSnapshot)
                                        .sorted(Comparator.comparing(PostingSnapshot::uuid)).toList(),
                        entry.getProjectionRefs().stream().map(this::projectionSnapshot)
                                        .sorted(Comparator.comparing(ProjectionSnapshot::uuid)).toList());
    }

    private PostingSnapshot postingSnapshot(LedgerPosting posting)
    {
        return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                        posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                        uuid(posting.getSecurity()), posting.getShares(), uuid(posting.getAccount()),
                        uuid(posting.getPortfolio()),
                        posting.getParameters().stream().map(this::parameterSnapshot)
                                        .sorted(Comparator.comparing(ParameterSnapshot::type)
                                                        .thenComparing(ParameterSnapshot::value))
                                        .toList());
    }

    private ParameterSnapshot parameterSnapshot(LedgerParameter<?> parameter)
    {
        return new ParameterSnapshot(parameter.getType(), parameter.getValueKind(), String.valueOf(parameter.getValue()));
    }

    private ProjectionSnapshot projectionSnapshot(LedgerProjectionRef projectionRef)
    {
        return new ProjectionSnapshot(projectionRef.getUUID(), projectionRef.getRole(), uuid(projectionRef.getAccount()),
                        uuid(projectionRef.getPortfolio()), effectivePrimaryPostingUUID(projectionRef),
                        effectivePostingGroupUUID(projectionRef),
                        projectionRef.getMemberships().stream()
                                        .map(membership -> new ProjectionMembershipSnapshot(membership.getPostingUUID(),
                                                        membership.getRole()))
                                        .sorted(Comparator.comparing(ProjectionMembershipSnapshot::postingUUID)
                                                        .thenComparing(ProjectionMembershipSnapshot::role))
                                        .toList());
    }

    private String effectivePrimaryPostingUUID(LedgerProjectionRef projectionRef)
    {
        if (projectionRef.getPrimaryPostingUUID() != null)
            return projectionRef.getPrimaryPostingUUID();

        return projectionRef.getPrimaryMembership().map(ProjectionMembership::getPostingUUID).orElse(null);
    }

    private String effectivePostingGroupUUID(LedgerProjectionRef projectionRef)
    {
        return projectionRef.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).stream().findFirst()
                        .map(ProjectionMembership::getPostingUUID).orElse(null);
    }

    private List<TransactionSnapshot> transactionSnapshots(Client client)
    {
        return client.getAllTransactions().stream().map(pair -> transactionSnapshot(pair.getOwner(), pair.getTransaction()))
                        .sorted(Comparator.comparing(TransactionSnapshot::uuid)).toList();
    }

    private TransactionSnapshot transactionSnapshot(TransactionOwner<?> owner, Transaction transaction)
    {
        var crossEntry = transaction.getCrossEntry();

        return new TransactionSnapshot(owner.getUUID(), transaction.getUUID(), transaction.getClass().getSimpleName(),
                        typeName(transaction), transaction.getDateTime(), transaction.getAmount(),
                        transaction.getCurrencyCode(), uuid(transaction.getSecurity()), transaction.getShares(),
                        transaction.getNote(), transaction.getSource(), exDate(transaction), unitSnapshots(transaction),
                        crossEntry != null ? crossEntry.getCrossTransaction(transaction).getUUID() : null,
                        crossEntry != null ? crossEntry.getCrossOwner(transaction).getUUID() : null);
    }

    private String typeName(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return accountTransaction.getType().name();
        if (transaction instanceof PortfolioTransaction portfolioTransaction)
            return portfolioTransaction.getType().name();
        throw new AssertionError(transaction.getClass().getName());
    }

    private LocalDateTime exDate(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return accountTransaction.getExDate();
        return null;
    }

    private List<UnitSnapshot> unitSnapshots(Transaction transaction)
    {
        return transaction.getUnits().map(this::unitSnapshot)
                        .sorted(Comparator.comparing(UnitSnapshot::type).thenComparing(UnitSnapshot::amount)
                                        .thenComparing(UnitSnapshot::forexAmount,
                                                        Comparator.nullsFirst(Comparator.naturalOrder())))
                        .toList();
    }

    private UnitSnapshot unitSnapshot(Unit unit)
    {
        return new UnitSnapshot(unit.getType(), unit.getAmount().getAmount(), unit.getAmount().getCurrencyCode(),
                        unit.getForex() != null ? unit.getForex().getAmount() : null,
                        unit.getForex() != null ? unit.getForex().getCurrencyCode() : null, unit.getExchangeRate());
    }

    private List<PlanSnapshot> planSnapshots(Client client)
    {
        return client.getPlans().stream()
                        .map(plan -> new PlanSnapshot(plan.getName(),
                                        plan.getTransactions(client).stream()
                                                        .map(pair -> new PlanTransactionSnapshot(pair.getOwner().getUUID(),
                                                                        pair.getTransaction().getUUID()))
                                                        .toList()))
                        .toList();
    }

    private void assertPlanExecutionRefsRestored(Client client)
    {
        assertThat(client.getPlans().stream().flatMap(plan -> plan.getTransactions().stream()).count(), is(0L));
        assertThat(client.getPlans().stream().flatMap(plan -> plan.getLedgerExecutionRefs().stream()).count(), is(3L));
        assertTrue(client.getPlans().stream().flatMap(plan -> plan.getLedgerExecutionRefs().stream())
                        .allMatch(ref -> ref.getLedgerEntryUUID() != null && ref.getProjectionUUID() != null
                                        && ref.getProjectionRole() != null));
    }

    private ParityFixture parityFixture()
    {
        var client = new Client();
        var account = register(client, account("Account"));
        var otherAccount = register(client, account("Other Account"));
        var portfolio = register(client, portfolio("Portfolio"));
        var otherPortfolio = register(client, portfolio("Other Portfolio"));
        var security = register(client, security());
        var creator = new LedgerTransactionCreator(client);

        var deposit = creator.createDeposit(metadata("deposit"), LedgerAccountCashLeg.of(account, money(11))).getEntry();
        var dividend = creator.createDividend(metadata("dividend"),
                        LedgerDividend.withExDate(
                                        LedgerAccountCashLeg.of(account, money(120),
                                                        LedgerForexAmount.of(
                                                                        Money.of(CurrencyUnit.USD,
                                                                                        Values.Amount.factorize(132)),
                                                                        new BigDecimal("1.1000"))),
                                        LedgerOptionalSecurity.of(security),
                                        LedgerCreationUnits.of(
                                                        LedgerCreationUnit.fee(money(2),
                                                                        LedgerForexAmount.of(
                                                                                        Money.of(CurrencyUnit.USD,
                                                                                                        Values.Amount
                                                                                                                        .factorize(4)),
                                                                                        new BigDecimal("0.5000"))),
                                                        LedgerCreationUnit.tax(money(3)),
                                                        LedgerCreationUnit.grossValue(money(125),
                                                                        LedgerForexAmount.of(
                                                                                        Money.of(CurrencyUnit.USD,
                                                                                                        Values.Amount
                                                                                                                        .factorize(250)),
                                                                                        new BigDecimal("0.5000")))),
                                        EX_DATE))
                        .getEntry();
        var buy = creator.createBuy(metadata("buy"),
                        LedgerAccountCashLeg.of(account, money(100),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(80)),
                                                        new BigDecimal("1.2500"))),
                        LedgerPortfolioSecurityLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(5)), money(100),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(125)),
                                                        new BigDecimal("0.8000"))),
                        LedgerCreationUnits.of(LedgerCreationUnit.fee(money(4)))).getEntry();
        creator.createSell(metadata("sell"), LedgerAccountCashLeg.of(account, money(50)),
                        securityLeg(portfolio, security, 2, 50), LedgerCreationUnits.none());
        creator.createAccountTransfer(metadata("account transfer"), LedgerCashTransferLeg.of(account, money(15)),
                        LedgerCashTransferLeg.of(otherAccount, money(15)));
        creator.createPortfolioTransfer(metadata("portfolio transfer"),
                        LedgerPortfolioTransferSecurity.of(security, Values.Share.factorize(3)),
                        LedgerPortfolioTransferLeg.of(portfolio, money(30)),
                        LedgerPortfolioTransferLeg.of(otherPortfolio, money(30)));
        var inboundDelivery = creator.createInboundDelivery(metadata("inbound delivery"),
                        LedgerDeliveryLeg.of(otherPortfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(4)), money(80)))
                        .getEntry();
        creator.createOutboundDelivery(metadata("outbound delivery"),
                        LedgerDeliveryLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(1)), money(20)));

        dividend.getProjectionRefs().get(0).setPrimaryPostingUUID(dividend.getPostings().get(0).getUUID());
        dividend.getProjectionRefs().get(0).setPostingGroupUUID("dividend-posting-group");
        client.getLedger().getEntries().forEach(entry -> entry.setUpdatedAt(UPDATED_AT));

        LedgerProjectionService.materialize(client);

        addPlan(client, "Deposit Plan", account, portfolio, security,
                        ledgerBacked(account.getTransactions(), projectionUUID(deposit, LedgerProjectionRole.ACCOUNT)));
        addPlan(client, "Buy Plan", account, portfolio, security,
                        ledgerBacked(portfolio.getTransactions(), projectionUUID(buy, LedgerProjectionRole.PORTFOLIO)));
        addPlan(client, "Delivery Plan", account, otherPortfolio, security,
                        ledgerBacked(otherPortfolio.getTransactions(),
                                        projectionUUID(inboundDelivery, LedgerProjectionRole.DELIVERY_INBOUND)));

        assertValid(client);

        return new ParityFixture(client);
    }

    private ParityFixture standardCreationFixture()
    {
        var client = new Client();
        var account = register(client, account("Account"));
        var otherAccount = register(client, account("Other Account"));
        var portfolio = register(client, portfolio("Portfolio"));
        var otherPortfolio = register(client, portfolio("Other Portfolio"));
        var security = register(client, security());
        var creator = new LedgerTransactionCreator(client);

        creator.createDeposit(metadata("deposit"), LedgerAccountCashLeg.of(account, money(11)));
        creator.createBuy(metadata("buy"), LedgerAccountCashLeg.of(account, money(100)),
                        securityLeg(portfolio, security, 5, 100), LedgerCreationUnits.none());
        creator.createSell(metadata("sell"), LedgerAccountCashLeg.of(account, money(50)),
                        securityLeg(portfolio, security, 2, 50), LedgerCreationUnits.none());
        creator.createInboundDelivery(metadata("inbound delivery"),
                        LedgerDeliveryLeg.of(otherPortfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(4)), money(80)));
        creator.createOutboundDelivery(metadata("outbound delivery"),
                        LedgerDeliveryLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security, Values.Share.factorize(1)), money(20)));
        creator.createAccountTransfer(metadata("account transfer"), LedgerCashTransferLeg.of(account, money(15)),
                        LedgerCashTransferLeg.of(otherAccount, money(15)));
        creator.createPortfolioTransfer(metadata("portfolio transfer"),
                        LedgerPortfolioTransferSecurity.of(security, Values.Share.factorize(3)),
                        LedgerPortfolioTransferLeg.of(portfolio, money(30)),
                        LedgerPortfolioTransferLeg.of(otherPortfolio, money(30)));

        LedgerProjectionService.materialize(client);

        assertValid(client);
        assertMaterializedProjectionUUIDs(client, projectionUUIDs(client));

        return new ParityFixture(client);
    }

    private ParityFixture hiddenTransferFactsFixture()
    {
        var client = new Client();
        var account = register(client, account("Account"));
        var otherAccount = register(client, account("Other Account"));
        var portfolio = register(client, portfolio("Portfolio"));
        var otherPortfolio = register(client, portfolio("Other Portfolio"));
        var security = register(client, security());
        var creator = new LedgerTransactionCreator(client);
        var accountTransfer = creator.createAccountTransfer(metadata("account transfer"),
                        LedgerCashTransferLeg.of(account, money(15),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(12)),
                                                        new BigDecimal("1.2500"))),
                        LedgerCashTransferLeg.of(otherAccount, money(16),
                                        LedgerForexAmount.of(Money.of(CurrencyUnit.USD, Values.Amount.factorize(20)),
                                                        new BigDecimal("0.8000"))))
                        .getEntry();
        var portfolioTransfer = creator.createPortfolioTransfer(metadata("portfolio transfer"),
                        LedgerPortfolioTransferSecurity.of(security, Values.Share.factorize(3)),
                        LedgerPortfolioTransferLeg.of(portfolio, money(30),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(24)),
                                                        new BigDecimal("1.2500"))),
                        LedgerPortfolioTransferLeg.of(otherPortfolio, money(30),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(40)),
                                                        new BigDecimal("0.7500"))))
                        .getEntry();

        addHiddenUnits(accountTransfer);
        addHiddenUnits(portfolioTransfer);

        client.getLedger().getEntries().forEach(entry -> entry.setUpdatedAt(UPDATED_AT));
        LedgerProjectionService.materialize(client);

        assertValid(client);

        return new ParityFixture(client);
    }

    private ParityFixture depositRemovalUnitFactsFixture()
    {
        var client = new Client();
        var account = register(client, account("Account"));
        var creator = new LedgerTransactionCreator(client);
        var units = LedgerCreationUnits.of(
                        LedgerCreationUnit.grossValue(money(30),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(24)),
                                                        new BigDecimal("1.2500"))),
                        LedgerCreationUnit.fee(money(3),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)),
                                                        new BigDecimal("0.5000"))),
                        LedgerCreationUnit.tax(money(4),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(5)),
                                                        new BigDecimal("0.8000"))));

        creator.createDeposit(metadata("deposit"), LedgerAccountCashLeg.of(account, money(11)), units);
        creator.createRemoval(metadata("removal"), LedgerAccountCashLeg.of(account, money(12)), units);

        client.getLedger().getEntries().forEach(entry -> entry.setUpdatedAt(UPDATED_AT));
        LedgerProjectionService.materialize(client);

        assertValid(client);

        return new ParityFixture(client);
    }

    private void addHiddenUnits(LedgerEntry entry)
    {
        new LedgerUnitPostingUpdater().apply(entry, LedgerUnitPostingPatch.of(
                        LedgerUnitPostingEdit.add(LedgerPostingType.GROSS_VALUE, money(30),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(24)),
                                                        new BigDecimal("1.2500"))),
                        LedgerUnitPostingEdit.add(LedgerPostingType.FEE, money(3),
                                        LedgerForexAmount.of(
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(6)),
                                                        new BigDecimal("0.5000"))),
                        LedgerUnitPostingEdit.add(LedgerPostingType.TAX, money(4))));
    }

    private ParityFixture ambiguousPlanFixture()
    {
        var fixture = parityFixture();
        var buy = fixture.client().getLedger().getEntries().stream().filter(entry -> entry.getType() == LedgerEntryType.BUY)
                        .findFirst().orElseThrow();
        var plan = plan("Ambiguous Plan", fixture.client().getAccounts().get(0), fixture.client().getPortfolios().get(0),
                        fixture.client().getSecurities().get(0));

        plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(buy.getUUID(), null, null));
        fixture.client().addPlan(plan);

        return fixture;
    }

    private void addPlan(Client client, String name, Account account, Portfolio portfolio, Security security,
                    Transaction transaction)
    {
        var plan = plan(name, account, portfolio, security);

        plan.getTransactions().add(transaction);
        client.addPlan(plan);
    }

    private InvestmentPlan plan(Client client, String name)
    {
        return client.getPlans().stream().filter(plan -> name.equals(plan.getName())).findFirst().orElseThrow();
    }

    private InvestmentPlan plan(String name, Account account, Portfolio portfolio, Security security)
    {
        var plan = new InvestmentPlan(name);

        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setStart(LocalDate.of(2026, 3, 1));
        plan.setInterval(1);
        plan.setAmount(Values.Amount.factorize(100));

        return plan;
    }

    private LedgerTransactionMetadata metadata(String note)
    {
        return LedgerTransactionMetadata.of(DATE_TIME).withNote(note).withSource("source-" + note);
    }

    private LedgerPortfolioSecurityLeg securityLeg(Portfolio portfolio, Security security, int shares, int amount)
    {
        return LedgerPortfolioSecurityLeg.of(portfolio,
                        LedgerSecurityQuantity.of(security, Values.Share.factorize(shares)), money(amount));
    }

    private Transaction ledgerBacked(List<? extends Transaction> transactions, String uuid)
    {
        return transactions.stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> uuid.equals(transaction.getUUID())).findFirst().orElseThrow();
    }

    private String projectionUUID(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow()
                        .getUUID();
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-parity", ".xml");

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
        var stream = new ByteArrayOutputStream();

        new ProtobufWriter().save(client, stream);

        return stream.toByteArray();
    }

    private Client loadProtobuf(byte[] bytes) throws IOException
    {
        return new ProtobufWriter().load(new ByteArrayInputStream(bytes));
    }

    private PClient parseProtobuf(byte[] bytes) throws IOException
    {
        return PClient.parseFrom(new ByteArrayInputStream(bytes, PROTOBUF_SIGNATURE.length,
                        bytes.length - PROTOBUF_SIGNATURE.length));
    }

    private byte[] wrapProtobuf(PClient client) throws IOException
    {
        var stream = new ByteArrayOutputStream();

        stream.write(PROTOBUF_SIGNATURE);
        client.writeTo(stream);

        return stream.toByteArray();
    }

    private void assertValid(Client client)
    {
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    private void assertInvalidLedgerLoadInvariants(Client loaded, ParityFixture fixture, String brokenProjectionUUID)
    {
        assertThat(loaded.getAccounts().size(), is(fixture.client().getAccounts().size()));
        assertThat(loaded.getPortfolios().size(), is(fixture.client().getPortfolios().size()));
        assertThat(loaded.getSecurities().size(), is(fixture.client().getSecurities().size()));
        assertThat(loaded.getLedger().getEntries().size(), is(fixture.client().getLedger().getEntries().size()));
        assertTrue(loaded.getLedger().getEntries().stream()
                        .flatMap(entry -> entry.getProjectionRefs().stream())
                        .anyMatch(projectionRef -> brokenProjectionUUID.equals(projectionRef.getUUID())));

        var expectedProjectionUUIDs = projectionUUIDs(fixture.client()).stream()
                        .filter(uuid -> !brokenProjectionUUID.equals(uuid)).toList();

        assertThat(materializedProjectionUUIDs(loaded), is(expectedProjectionUUIDs));
        assertTrue(loaded.getAllTransactions().stream().allMatch(pair -> pair.getTransaction()
                        instanceof LedgerBackedTransaction));
        assertTrue(materializedProjectionUUIDs(loaded).stream().noneMatch(brokenProjectionUUID::equals));
    }

    private void assertInvalidLedgerSaveAsDoesNotCreateOrTruncateTarget(EnumSet<SaveFlag> flags) throws Exception
    {
        var client = parityFixture().client();
        client.getLedger().getEntries().get(0).getProjectionRefs().get(0)
                        .setPrimaryPostingUUID("missing-primary-posting");
        var directory = Files.createTempDirectory("ledger-save-failure");
        var missingTarget = directory.resolve("missing.portfolio");
        var existingTarget = directory.resolve("existing.portfolio");
        var previousContent = "previous content";

        try
        {
            var newTargetException = assertThrows(Exception.class,
                            () -> ClientFactory.saveAs(client, missingTarget.toFile(), null, EnumSet.copyOf(flags)));

            assertInvalidLedgerSaveMessage(flags, newTargetException);
            assertFalse(Files.exists(missingTarget));

            Files.writeString(existingTarget, previousContent, StandardCharsets.UTF_8);

            var existingTargetException = assertThrows(Exception.class,
                            () -> ClientFactory.saveAs(client, existingTarget.toFile(), null, EnumSet.copyOf(flags)));

            assertInvalidLedgerSaveMessage(flags, existingTargetException);
            assertThat(Files.readString(existingTarget, StandardCharsets.UTF_8), is(previousContent));
        }
        finally
        {
            Files.deleteIfExists(missingTarget);
            Files.deleteIfExists(existingTarget);
            Files.deleteIfExists(directory);
        }
    }

    private void assertInvalidLedgerSaveMessage(EnumSet<SaveFlag> flags, Exception exception)
    {
        var message = exception.getMessage();
        var expectedCode = flags.contains(SaveFlag.BINARY) ? LedgerDiagnosticCode.LEDGER_PERSIST_002
                        : LedgerDiagnosticCode.LEDGER_PERSIST_001;

        assertTrue(message, message.contains(expectedCode.prefix()));
        assertTrue(message, message.contains(LedgerDiagnosticCode.LEDGER_STRUCT_025.prefix()));
        assertTrue(message, message.contains("PROJECTION_PRIMARY_TARGET_CONFLICT"));
        assertTrue(message, message.contains("missing-primary-posting"));
    }

    private void assertLedgerParameterOwnership(Client client, String targetSecurityUUID)
    {
        assertValid(client);

        var entry = client.getLedger().getEntries().stream()
                        .filter(candidate -> "entry-parameter-ownership".equals(candidate.getUUID()))
                        .findFirst().orElseThrow();
        var cashPosting = posting(entry, "posting-a");
        var feePosting = posting(entry, "posting-b");

        assertThat(entry.getParameters().size(), is(2));
        assertEntryParameter(entry, LedgerParameterType.CORPORATE_ACTION_KIND,
                        LedgerParameter.ValueKind.STRING, "SPIN_OFF");
        assertEntryParameter(entry, LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        LedgerParameter.ValueKind.BOOLEAN, Boolean.TRUE);
        assertThat(entry.getParameters().stream()
                        .noneMatch(parameter -> parameter.getType() == LedgerParameterType.EX_DATE), is(true));
        assertThat(entry.getParameters().stream()
                        .noneMatch(parameter -> parameter.getType() == LedgerParameterType.TARGET_SECURITY),
                        is(true));
        assertThat(entry.getPostings().size(), is(2));
        assertOnlyParameter(cashPosting, LedgerParameterType.EX_DATE,
                        LedgerParameter.ValueKind.LOCAL_DATE_TIME, EX_DATE);
        assertOnlySecurityParameter(feePosting, LedgerParameterType.TARGET_SECURITY, targetSecurityUUID);
        assertThat(cashPosting.getParameters().stream()
                        .noneMatch(parameter -> parameter.getType() == LedgerParameterType.CORPORATE_ACTION_KIND),
                        is(true));
        assertThat(feePosting.getParameters().stream()
                        .noneMatch(parameter -> parameter.getType() == LedgerParameterType.CASH_IN_LIEU_APPLIED),
                        is(true));
    }

    private void assertCompactLedgerParameterXml(String xml, Money nominalValue)
    {
        assertTrue(xml.contains("<ledger-parameter type=\"CORPORATE_ACTION_KIND\" valueKind=\"STRING\" "
                        + "value=\"SPIN_OFF\"/>"));
        assertTrue(xml.contains("<ledger-parameter type=\"RATIO_NUMERATOR\" valueKind=\"DECIMAL\" "
                        + "value=\"1.25\"/>"));
        assertTrue(xml.contains("<ledger-parameter type=\"RECORD_DATE\" valueKind=\"LOCAL_DATE\" "
                        + "value=\"2026-03-01\"/>"));
        assertTrue(xml.contains("<ledger-parameter type=\"EX_DATE\" valueKind=\"LOCAL_DATE_TIME\" "
                        + "value=\"2026-02-27T00:00\"/>"));
        assertTrue(xml.contains("<ledger-parameter type=\"CASH_IN_LIEU_APPLIED\" valueKind=\"BOOLEAN\" "
                        + "value=\"true\"/>"));
        assertTrue(xml.contains("<ledger-parameter type=\"NOMINAL_VALUE\" valueKind=\"MONEY\" amount=\""
                        + nominalValue.getAmount() + "\" currency=\"EUR\"/>"));
        assertTrue(xml.matches("(?s).*<ledger-parameter type=\"RIGHT_SECURITY\" valueKind=\"SECURITY\">\\s*" //$NON-NLS-1$
                        + "<value[^>]*reference=\"[^\"]+\"[^>]*/>\\s*</ledger-parameter>.*")); //$NON-NLS-1$
        assertTrue(xml.matches("(?s).*<ledger-parameter type=\"SOURCE_ACCOUNT\" valueKind=\"ACCOUNT\">\\s*" //$NON-NLS-1$
                        + "<value[^>]*reference=\"[^\"]+\"[^>]*/>\\s*</ledger-parameter>.*")); //$NON-NLS-1$
        assertTrue(xml.matches("(?s).*<ledger-parameter type=\"SOURCE_PORTFOLIO\" valueKind=\"PORTFOLIO\">\\s*" //$NON-NLS-1$
                        + "<value[^>]*reference=\"[^\"]+\"[^>]*/>\\s*</ledger-parameter>.*")); //$NON-NLS-1$
        assertFalse(xml.contains("<valueKind>"));
        assertFalse(xml.contains("<value class=\"string\">"));
        assertFalse(xml.contains("<value class=\"big-decimal\">"));
        assertFalse(xml.contains("<value class=\"boolean\">"));
        assertFalse(xml.contains("<value class=\"local-date\">"));
        assertFalse(xml.contains("<value class=\"local-date-time\">"));
    }

    private void assertNewLedgerParameterVocabulary(Client client, LocalDate recordDate, Money nominalValue,
                    String rightSecurityUUID, String accountUUID, String portfolioUUID)
    {
        assertValid(client);

        var entry = client.getLedger().getEntries().stream()
                        .filter(candidate -> "entry-new-parameter-vocabulary".equals(candidate.getUUID()))
                        .findFirst().orElseThrow();
        var cashPosting = posting(entry, "posting-new-vocabulary-a");
        var feePosting = posting(entry, "posting-new-vocabulary-b");

        assertThat(entry.getParameters().isEmpty(), is(true));
        assertThat(entry.getPostings().size(), is(2));
        assertParameter(cashPosting, LedgerParameterType.RECORD_DATE, LedgerParameter.ValueKind.LOCAL_DATE,
                        recordDate);
        assertParameter(cashPosting, LedgerParameterType.EX_DATE, LedgerParameter.ValueKind.LOCAL_DATE_TIME,
                        EX_DATE);
        assertParameter(cashPosting, LedgerParameterType.RATIO_NUMERATOR, LedgerParameter.ValueKind.DECIMAL,
                        new BigDecimal("1.25"));
        assertParameter(cashPosting, LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        LedgerParameter.ValueKind.BOOLEAN, Boolean.TRUE);
        assertParameter(cashPosting, LedgerParameterType.NOMINAL_VALUE, LedgerParameter.ValueKind.MONEY,
                        nominalValue);
        assertThat(cashPosting.getParameters().size(), is(5));

        assertSecurityParameter(feePosting, LedgerParameterType.RIGHT_SECURITY, rightSecurityUUID);
        assertParameter(feePosting, LedgerParameterType.CORPORATE_ACTION_KIND,
                        LedgerParameter.ValueKind.STRING, "SPIN_OFF");
        assertAccountParameter(feePosting, LedgerParameterType.SOURCE_ACCOUNT, accountUUID);
        assertPortfolioParameter(feePosting, LedgerParameterType.SOURCE_PORTFOLIO, portfolioUUID);
        assertThat(feePosting.getParameters().size(), is(4));
    }

    private LedgerPosting posting(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream().filter(candidate -> uuid.equals(candidate.getUUID())).findFirst()
                        .orElseThrow();
    }

    private void assertOnlyParameter(LedgerPosting posting, LedgerParameterType type,
                    LedgerParameter.ValueKind valueKind, Object value)
    {
        assertThat(posting.getParameters().size(), is(1));

        var parameter = posting.getParameters().get(0);

        assertThat(parameter.getType(), is(type));
        assertThat(parameter.getValueKind(), is(valueKind));
        assertThat(parameter.getValue(), is(value));
    }

    private void assertOnlySecurityParameter(LedgerPosting posting, LedgerParameterType type,
                    String securityUUID)
    {
        assertThat(posting.getParameters().size(), is(1));

        var parameter = posting.getParameters().get(0);
        var security = (Security) parameter.getValue();

        assertThat(parameter.getType(), is(type));
        assertThat(parameter.getValueKind(), is(LedgerParameter.ValueKind.SECURITY));
        assertThat(security.getUUID(), is(securityUUID));
    }

    private void assertEntryParameter(LedgerEntry entry, LedgerParameterType type,
                    LedgerParameter.ValueKind valueKind, Object value)
    {
        var parameter = entry.getParameters().stream().filter(candidate -> candidate.getType() == type).findFirst()
                        .orElseThrow();

        assertThat(parameter.getValueKind(), is(valueKind));
        assertThat(parameter.getValue(), is(value));
    }

    private void assertParameter(LedgerPosting posting, LedgerParameterType type,
                    LedgerParameter.ValueKind valueKind, Object value)
    {
        var parameter = posting.getParameters().stream().filter(candidate -> candidate.getType() == type).findFirst()
                        .orElseThrow();

        assertThat(parameter.getValueKind(), is(valueKind));
        assertThat(parameter.getValue(), is(value));
    }

    private void assertSecurityParameter(LedgerPosting posting, LedgerParameterType type, String securityUUID)
    {
        var parameter = posting.getParameters().stream().filter(candidate -> candidate.getType() == type).findFirst()
                        .orElseThrow();
        var security = (Security) parameter.getValue();

        assertThat(parameter.getValueKind(), is(LedgerParameter.ValueKind.SECURITY));
        assertThat(security.getUUID(), is(securityUUID));
    }

    private void assertAccountParameter(LedgerPosting posting, LedgerParameterType type, String accountUUID)
    {
        var parameter = posting.getParameters().stream().filter(candidate -> candidate.getType() == type).findFirst()
                        .orElseThrow();
        var account = (Account) parameter.getValue();

        assertThat(parameter.getValueKind(), is(LedgerParameter.ValueKind.ACCOUNT));
        assertThat(account.getUUID(), is(accountUUID));
    }

    private void assertPortfolioParameter(LedgerPosting posting, LedgerParameterType type, String portfolioUUID)
    {
        var parameter = posting.getParameters().stream().filter(candidate -> candidate.getType() == type).findFirst()
                        .orElseThrow();
        var portfolio = (Portfolio) parameter.getValue();

        assertThat(parameter.getValueKind(), is(LedgerParameter.ValueKind.PORTFOLIO));
        assertThat(portfolio.getUUID(), is(portfolioUUID));
    }

    private String uuid(Object object)
    {
        if (object instanceof Account account)
            return account.getUUID();
        if (object instanceof Portfolio portfolio)
            return portfolio.getUUID();
        if (object instanceof Security security)
            return security.getUUID();
        return null;
    }

    private Account register(Client client, Account account)
    {
        client.addAccount(account);
        return account;
    }

    private Portfolio register(Client client, Portfolio portfolio)
    {
        client.addPortfolio(portfolio);
        return portfolio;
    }

    private Security register(Client client, Security security)
    {
        client.addSecurity(security);
        return security;
    }

    private Account account(String name)
    {
        var account = new Account();

        account.setName(name);
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);

        return account;
    }

    private Portfolio portfolio(String name)
    {
        var portfolio = new Portfolio();

        portfolio.setName(name);
        portfolio.setUpdatedAt(UPDATED_AT);

        return portfolio;
    }

    private Security security()
    {
        var security = new Security("Security", CurrencyUnit.EUR);

        security.setUpdatedAt(UPDATED_AT);

        return security;
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private record ParityFixture(Client client)
    {
    }

    private record LedgerSnapshot(List<EntrySnapshot> entries)
    {
    }

    private record EntrySnapshot(String uuid, LedgerEntryType type, LocalDateTime dateTime, String note, String source,
                    Instant updatedAt, List<ParameterSnapshot> parameters, List<PostingSnapshot> postings,
                    List<ProjectionSnapshot> projectionRefs)
    {
    }

    private record PostingSnapshot(String uuid, LedgerPostingType type, long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, String securityUUID, long shares, String accountUUID,
                    String portfolioUUID, List<ParameterSnapshot> parameters)
    {
    }

    private record ParameterSnapshot(LedgerParameterType type, LedgerParameter.ValueKind valueKind,
                    String value)
    {
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, String accountUUID, String portfolioUUID,
                    String primaryPostingUUID, String postingGroupUUID, List<ProjectionMembershipSnapshot> memberships)
    {
    }

    private record ProjectionMembershipSnapshot(String postingUUID, ProjectionMembershipRole role)
    {
    }

    private record MembershipSnapshot(String projectionUUID, String postingUUID, ProjectionMembershipRole role)
    {
    }

    private record TransactionSnapshot(String ownerUUID, String uuid, String transactionClass, String type,
                    LocalDateTime dateTime, long amount, String currency, String securityUUID, long shares, String note,
                    String source, LocalDateTime exDate, List<UnitSnapshot> units, String crossTransactionUUID,
                    String crossOwnerUUID)
    {
    }

    private record UnitSnapshot(Unit.Type type, long amount, String currency, Long forexAmount, String forexCurrency,
                    BigDecimal exchangeRate)
    {
    }

    private record PlanSnapshot(String name, List<PlanTransactionSnapshot> transactions)
    {
    }

    private record PlanTransactionSnapshot(String ownerUUID, String transactionUUID)
    {
    }

}
