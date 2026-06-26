package name.abuchen.portfolio.model.ledger.compatibility;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerNativeComponentInspectorModel.HeaderField;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCorporateActionEvent;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeEntryMetadata;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeSecurityLeg;
import name.abuchen.portfolio.model.ledger.nativeentry.Ratio;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests the read-only model behind the Ledger entry inspector.
 * The tests make sure selected ledger-backed projections can be displayed from Ledger facts
 * and optional Java-only native leg definitions without mutating the entry or legacy projection.
 */
public class LedgerNativeComponentInspectorModelTest
{
    /**
     * Checks that a spin-off entry can be inspected from the selected old-security projection.
     * The inspector model must expose entry parameters, functional legs, postings, posting
     * parameters, and projection refs from the persisted Ledger entry.
     */
    @Test
    public void testSpinOffInspectionIncludesEntryLegPostingParameterAndProjectionRows()
    {
        var entry = spinOffEntry();
        var selectedProjectionRef = entry.getProjectionRefs().get(0);
        var updatedAt = entry.getUpdatedAt();

        var model = LedgerNativeComponentInspectorModel
                        .from(entry, selectedProjectionRef, LedgerEntryDefinitionRegistry::lookup).orElseThrow();

        assertThat(model.getHeaderRows().stream()
                        .anyMatch(row -> row.field() == HeaderField.ENTRY_TYPE && "SPIN_OFF".equals(row.value())),
                        is(true));
        assertThat(model.getHeaderRows().stream()
                        .anyMatch(row -> row.field() == HeaderField.NATIVE_TARGETED && "true".equals(row.value())),
                        is(true));
        assertThat(model.getHeaderRows().stream()
                        .anyMatch(row -> row.field() == HeaderField.SELECTED_PROJECTION_UUID
                                        && "projection-old".equals(row.value())),
                        is(true));
        assertTrue(model.isNativeEntryDefinitionAvailable());
        assertThat(model.getEntryParameters().stream()
                        .anyMatch(row -> "CORPORATE_ACTION_KIND".equals(row.parameter())
                                        && "SPIN_OFF".equals(row.value())),
                        is(true));
        assertThat(model.getLegs().stream().anyMatch(row -> "SOURCE_SECURITY_LEG".equals(row.legRole())), is(true));
        assertThat(model.getLegs().stream().anyMatch(row -> "TARGET_SECURITY_LEG".equals(row.legRole())), is(true));
        assertThat(model.getPostings().stream()
                        .anyMatch(row -> "posting-source".equals(row.postingUUID())
                                        && "SECURITY".equals(row.postingType())
                                        && "Old Security".equals(row.security())),
                        is(true));
        assertThat(model.getPostingParameters().stream()
                        .anyMatch(row -> "posting-source".equals(row.postingUUID())
                                        && "SOURCE_SECURITY".equals(row.parameter())
                                        && "Old Security".equals(row.value())),
                        is(true));
        assertThat(model.getProjectionRefs().stream()
                        .anyMatch(row -> "OLD_SECURITY_LEG".equals(row.projectionRole())
                                        && "projection-old".equals(row.projectionUUID())
                                        && "posting-source".equals(row.primaryPostingUUID())),
                        is(true));

        assertThat(entry.getUpdatedAt(), is(updatedAt));
        assertThat(entry.getPostings().size(), is(2));
        assertThat(entry.getProjectionRefs().size(), is(2));
    }

    /**
     * Checks that other configured native corporate-action entries can be inspected as native
     * Ledger entries.
     * Each smoke case must expose at least the leg rows from its entry definition.
     */
    @Test
    public void testOtherNativeEntryTypesExposeConfiguredLegs()
    {
        assertHasLegRows(LedgerEntryType.STOCK_DIVIDEND, LedgerProjectionRole.DELIVERY_INBOUND);
        assertHasLegRows(LedgerEntryType.BONUS_ISSUE, LedgerProjectionRole.DELIVERY_INBOUND);
        assertHasLegRows(LedgerEntryType.BOND_CONVERSION, LedgerProjectionRole.OLD_SECURITY_LEG);
    }

    /**
     * Checks that a materialized native corporate-action projection is recognized by the
     * inspector support method.
     * UI guards use this result to offer inspection without legacy edit, duplicate, delete, or
     * convert actions.
     */
    @Test
    public void testMaterializedNativeProjectionIsRecognizedAsNativeTargeted()
    {
        var fixture = fixture();

        LedgerNativeEntryAssembler.forClient(fixture.client()).spinOff()
                        .metadata(NativeEntryMetadata.of(LocalDateTime.of(2026, 6, 23, 9, 0)))
                        .event(NativeCorporateActionEvent.builder()
                                        .kind(CorporateActionKind.SPIN_OFF)
                                        .effectiveDate(LocalDate.of(2026, 6, 20))
                                        .build())
                        .securityLeg(NativeSecurityLeg.source()
                                        .portfolio(fixture.portfolio())
                                        .security(fixture.security())
                                        .shares(Values.Share.factorize(10))
                                        .amount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)))
                                        .sourceSecurity(fixture.security())
                                        .targetSecurity(fixture.targetSecurity())
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.TEN))
                                        .build())
                        .securityLeg(NativeSecurityLeg.target()
                                        .portfolio(fixture.portfolio())
                                        .security(fixture.targetSecurity())
                                        .shares(Values.Share.factorize(1))
                                        .amount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)))
                                        .sourceSecurity(fixture.security())
                                        .targetSecurity(fixture.targetSecurity())
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.TEN))
                                        .build())
                        .buildAndAdd();

        var transaction = fixture.portfolio().getTransactions().get(0);

        assertTrue(LedgerNativeComponentInspectorModel.isLedgerNativeTargetedProjection(transaction));
        assertTrue(LedgerNativeComponentInspectorModel.from(transaction).isPresent());
    }

    /**
     * Checks that a legacy fixed-shape Ledger-backed transaction can still be inspected.
     * The model must show Ledger facts without native leg definitions because standard
     * transaction families are not configured through native legs.
     */
    @Test
    public void testLegacyFixedShapeEntryShowsLedgerFactsWithoutNativeLegDefinitions()
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        var buySell = new LedgerBuySellTransactionCreator(client).create(portfolio, account,
                        PortfolioTransaction.Type.BUY, LocalDateTime.of(2026, 6, 23, 9, 0),
                        Values.Amount.factorize(123), CurrencyUnit.EUR, security, Values.Share.factorize(5),
                        List.of(), "note", "source");

        var model = LedgerNativeComponentInspectorModel.from(buySell.getPortfolioTransaction()).orElseThrow();

        assertThat(model.getHeaderRows().stream()
                        .anyMatch(row -> row.field() == HeaderField.ENTRY_TYPE && "BUY".equals(row.value())),
                        is(true));
        assertFalse(model.isNativeEntryDefinitionAvailable());
        assertTrue(model.getLegs().isEmpty());
        assertFalse(model.getPostings().isEmpty());
        assertFalse(model.getProjectionRefs().isEmpty());
    }

    /**
     * Checks that a native entry without a matching Java definition still shows persisted facts.
     * The model must not guess functional legs when the registry cannot describe the entry.
     */
    @Test
    public void testNativeEntryWithMissingDefinitionShowsLedgerFactsWithoutLegs()
    {
        var entry = new LedgerEntry("entry-spin-off");
        entry.setType(LedgerEntryType.SPIN_OFF);
        entry.setDateTime(LocalDateTime.of(2026, 6, 23, 9, 0));
        entry.addProjectionRef(projection("projection-old", LedgerProjectionRole.OLD_SECURITY_LEG, null));

        var model = LedgerNativeComponentInspectorModel.from(entry, entry.getProjectionRefs().get(0),
                        ignored -> Optional.empty()).orElseThrow();

        assertFalse(model.isNativeEntryDefinitionAvailable());
        assertTrue(model.getLegs().isEmpty());
        assertThat(model.getProjectionRefs().size(), is(1));
    }

    /**
     * Checks that normal legacy transactions are not offered to the Ledger inspector.
     * The action must only appear when a selected row resolves to a ledger-backed projection.
     */
    @Test
    public void testNonLedgerBackedTransactionIsUnsupported()
    {
        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DEPOSIT);

        assertFalse(LedgerNativeComponentInspectorModel.from(transaction).isPresent());
    }

    private static void assertHasLegRows(LedgerEntryType type, LedgerProjectionRole projectionRole)
    {
        var entry = new LedgerEntry("entry-" + type.name());
        entry.setType(type);
        entry.setDateTime(LocalDateTime.of(2026, 6, 23, 9, 0));
        var projectionRef = projection("projection-" + type.name(), projectionRole, null);
        entry.addProjectionRef(projectionRef);

        var model = LedgerNativeComponentInspectorModel.from(entry, projectionRef, LedgerEntryDefinitionRegistry::lookup)
                        .orElseThrow();

        assertTrue(type + " should expose configured legs", model.getLegs().size() > 0);
    }

    private static LedgerEntry spinOffEntry()
    {
        var entry = new LedgerEntry("entry-spin-off");
        entry.setType(LedgerEntryType.SPIN_OFF);
        entry.setDateTime(LocalDateTime.of(2026, 6, 23, 9, 0));
        entry.setNote("Spin-off note");
        entry.setSource("manual");
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF));
        entry.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.EFFECTIVE_DATE,
                        LocalDate.of(2026, 6, 20)));

        var portfolio = new Portfolio("Growth Portfolio");
        var oldSecurity = new Security("Old Security", CurrencyUnit.EUR);
        var newSecurity = new Security("New Security", CurrencyUnit.EUR);

        var sourcePosting = securityPosting("posting-source", oldSecurity, CorporateActionLeg.SOURCE_SECURITY,
                        LedgerParameterType.SOURCE_SECURITY, oldSecurity);
        sourcePosting.setPortfolio(portfolio);
        entry.addPosting(sourcePosting);

        var targetPosting = securityPosting("posting-target", newSecurity, CorporateActionLeg.TARGET_SECURITY,
                        LedgerParameterType.TARGET_SECURITY, newSecurity);
        targetPosting.setPortfolio(portfolio);
        entry.addPosting(targetPosting);

        var oldProjection = projection("projection-old", LedgerProjectionRole.OLD_SECURITY_LEG,
                        sourcePosting.getUUID());
        oldProjection.setPortfolio(portfolio);
        entry.addProjectionRef(oldProjection);

        var newProjection = projection("projection-new", LedgerProjectionRole.NEW_SECURITY_LEG,
                        targetPosting.getUUID());
        newProjection.setPortfolio(portfolio);
        entry.addProjectionRef(newProjection);

        return entry;
    }

    private static Fixture fixture()
    {
        var client = new Client();
        var account = new Account("Account");
        var portfolio = new Portfolio("Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);
        var targetSecurity = new Security("Target Security", CurrencyUnit.EUR);

        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(account);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(security);
        client.addSecurity(targetSecurity);

        return new Fixture(client, account, portfolio, security, targetSecurity);
    }

    private static LedgerPosting securityPosting(String uuid, Security security, CorporateActionLeg leg,
                    LedgerParameterType securityParameterType, Security parameterSecurity)
    {
        var posting = new LedgerPosting(uuid);
        posting.setType(LedgerPostingType.SECURITY);
        posting.setSecurity(security);
        posting.setShares(123_00000000L);
        posting.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_LEG, leg));
        posting.addParameter(LedgerParameter.ofSecurity(securityParameterType, parameterSecurity));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR, BigDecimal.ONE));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_DENOMINATOR, BigDecimal.TEN));
        return posting;
    }

    private static LedgerProjectionRef projection(String uuid, LedgerProjectionRole role, String primaryPostingUUID)
    {
        var projectionRef = new LedgerProjectionRef(uuid);
        projectionRef.setRole(role);
        projectionRef.setPrimaryPostingUUID(primaryPostingUUID);
        return projectionRef;
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security security, Security targetSecurity)
    {
    }
}
