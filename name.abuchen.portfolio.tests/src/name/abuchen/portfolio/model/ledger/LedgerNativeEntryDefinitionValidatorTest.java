package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.FractionTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator.IssueCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.RoundingModeCode;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCashCompensation;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCorporateActionEvent;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeEntryMetadata;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeFee;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeSecurityLeg;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeTax;
import name.abuchen.portfolio.model.ledger.nativeentry.Ratio;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests native Ledger entry validation against Java-owned entry and leg definitions.
 * These tests keep native create paths strict while leaving load, save, and raw
 * model fixtures outside native completeness enforcement.
 */
@SuppressWarnings("nls")
public class LedgerNativeEntryDefinitionValidatorTest
{
    /**
     * Checks that a complete spin-off assembled through the native builder
     * satisfies the Java-owned native definition.
     * The fee and tax postings do not need first-class group membership.
     */
    @Test
    public void testValidSpinOffIsNativeDefinitionValid()
    {
        var entry = validSpinOff(fixture()).buildDetached().getEntry();
        var result = LedgerNativeEntryDefinitionValidator.validate(entry);

        assertTrue(result.format(), result.isOK());
        assertThat(entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.FEE).count(),
                        is(1L));
        assertThat(entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.TAX).count(),
                        is(1L));
    }

    /**
     * Checks that a native corporate-action entry without a kind is rejected
     * before it can select a kind-specific native definition.
     */
    @Test
    public void testCorporateActionWithoutKindIsRejected()
    {
        var entry = copyValidSpinOff();
        removeEntryParameters(entry, LedgerParameterType.CORPORATE_ACTION_KIND);

        assertIssue(entry, IssueCode.ENTRY_DEFINITION_MISSING);
    }

    /**
     * Checks that a native entry must satisfy the configured date alternative.
     * A spin-off without EX_DATE and EFFECTIVE_DATE is structurally possible
     * but incomplete against the native definition.
     */
    @Test
    public void testMissingDateAlternativeIsRejected()
    {
        var entry = copyValidSpinOff();
        removeEntryParameters(entry, LedgerParameterType.EX_DATE);
        removeEntryParameters(entry, LedgerParameterType.EFFECTIVE_DATE);

        assertIssue(entry, IssueCode.REQUIRED_ALTERNATIVE_GROUP_MISSING);
    }

    /**
     * Checks that entry parameters stay within the entry definition vocabulary.
     * Source-account facts must not be smuggled into the event-level parameter list.
     */
    @Test
    public void testEntryParameterNotAllowedByDefinitionIsRejected()
    {
        var fixture = fixture();
        var entry = validSpinOff(fixture).buildDetached().getEntry();

        entry.addParameter(LedgerParameter.ofAccount(LedgerParameterType.SOURCE_ACCOUNT, fixture.account));

        assertIssue(entry, IssueCode.ENTRY_PARAMETER_NOT_ALLOWED);
    }

    /**
     * Checks that source security legs are optional after the spin-off cardinality cleanup.
     * Supported create paths may persist partial native shapes while repeated present legs still need keys.
     */
    @Test
    public void testMissingSourceSecurityLegIsAccepted()
    {
        var entry = copyValidSpinOff();
        var sourcePosting = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);

        projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG).getPrimaryPosting().setUnitRole(null);
        entry.removePosting(sourcePosting);

        assertOK(entry);
    }

    /**
     * Checks that target security legs are optional after the spin-off cardinality cleanup.
     * Missing movement rows no longer violate the configured native definition.
     */
    @Test
    public void testMissingTargetSecurityLegIsAccepted()
    {
        var entry = copyValidSpinOff();
        var targetPosting = postingFor(entry, LedgerProjectionRole.NEW_SECURITY_LEG);

        entry.removePosting(targetPosting);

        assertOK(entry);
    }

    @Test
    public void testSpinOffDefinitionAcceptsSecurityContextLeg()
    {
        var fixture = fixture();
        var entry = copyValidSpinOff();

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testSpinOffDefinitionRejectsDuplicateSecurityContextLocalKey()
    {
        var fixture = fixture();
        var entry = copyValidSpinOff();

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.LEG_LOCAL_KEY_DUPLICATE);
    }

    /**
     * Checks that repeated source legs follow the same semantic-key policy as
     * repeated target and cash movement legs.
     */
    @Test
    public void testSpinOffDefinitionAcceptsRepeatedSourceLegsWithDistinctLocalKeys()
    {
        var entry = copyValidSpinOff();
        var originalSource = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);
        var sourcePosting = LedgerModelCopy.copyPosting(postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG));

        originalSource.setLocalKey("source-1");
        originalSource.setGroupKey("main");
        sourcePosting.setUUID("duplicate-source-posting");
        sourcePosting.setLocalKey("source-2");
        sourcePosting.setGroupKey("main");
        entry.addPosting(sourcePosting);

        assertOK(entry);
    }

    @Test
    public void testSpinOffDefinitionRejectsDuplicateSourceLocalKey()
    {
        var entry = copyValidSpinOff();
        var originalSource = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);
        var sourcePosting = LedgerModelCopy.copyPosting(originalSource);

        originalSource.setLocalKey("source-1");
        originalSource.setGroupKey("main");
        sourcePosting.setUUID("duplicate-source-posting");
        sourcePosting.setLocalKey("source-1");
        sourcePosting.setGroupKey("main");
        entry.addPosting(sourcePosting);

        assertIssue(entry, IssueCode.REQUIRED_PROJECTION_MISSING);
    }

    @Test
    public void testSpinOffDefinitionAcceptsRepeatedTargetLegsWithDistinctLocalKeys()
    {
        var entry = copyValidSpinOff();
        var targetPosting = postingFor(entry, LedgerProjectionRole.NEW_SECURITY_LEG);
        var duplicateTarget = LedgerModelCopy.copyPosting(targetPosting);

        targetPosting.setLocalKey("target-1");
        targetPosting.setGroupKey("main");
        duplicateTarget.setUUID("duplicate-target-posting");
        duplicateTarget.setLocalKey("target-2");
        duplicateTarget.setGroupKey("main");
        entry.addPosting(duplicateTarget);

        assertOK(entry);
    }

    @Test
    public void testSpinOffDefinitionAcceptsRepeatedCashCompensationWithDistinctLocalKeys()
    {
        var entry = copyValidSpinOff();
        var compensation = postingFor(entry, LedgerProjectionRole.CASH_COMPENSATION);
        var duplicateCompensation = LedgerModelCopy.copyPosting(compensation);

        compensation.setLocalKey("cash-1");
        compensation.setGroupKey("cash-1");
        duplicateCompensation.setUUID("duplicate-cash-compensation");
        duplicateCompensation.setLocalKey("cash-2");
        duplicateCompensation.setGroupKey("cash-2");
        entry.addPosting(duplicateCompensation);

        assertOK(entry);
    }

    @Test
    public void testSpinOffDefinitionRejectsDuplicateCashCompensationLocalKey()
    {
        var entry = copyValidSpinOff();
        var compensation = postingFor(entry, LedgerProjectionRole.CASH_COMPENSATION);
        var duplicateCompensation = LedgerModelCopy.copyPosting(compensation);

        compensation.setLocalKey("cash-1");
        compensation.setGroupKey("cash-1");
        duplicateCompensation.setUUID("duplicate-cash-compensation");
        duplicateCompensation.setLocalKey("cash-1");
        duplicateCompensation.setGroupKey("cash-2");
        entry.addPosting(duplicateCompensation);

        assertIssue(entry, IssueCode.REQUIRED_PROJECTION_MISSING);
    }

    /**
     * Checks that a projection cannot satisfy a security leg with a posting of
     * the wrong type.
     * The validator rejects the entry before a supported create path can attach it.
     */
    @Test
    public void testWrongPostingTypeForLegIsRejected()
    {
        var entry = copyValidSpinOff();

        postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG).setType(LedgerPostingType.FEE);

        assertIssue(entry, IssueCode.PROJECTION_PRIMARY_POSTING_MISMATCH);
    }

    /**
     * Checks that the source security projection is required for the source leg.
     * A source posting without its expected projection cannot become a supported
     * runtime view.
     */
    @Test
    public void testMissingOldSecurityProjectionIsRejected()
    {
        var entry = copyValidSpinOff();

        projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG).getPrimaryPosting().setCorporateActionLeg(null);

        assertIssue(entry, IssueCode.REQUIRED_PROJECTION_MISSING);
    }

    /**
     * Checks that the target security projection is required for the target leg.
     * The native definition needs a projection that points at the new security side.
     */
    @Test
    public void testMissingNewSecurityProjectionIsRejected()
    {
        var entry = copyValidSpinOff();

        projection(entry, LedgerProjectionRole.NEW_SECURITY_LEG).getPrimaryPosting().setCorporateActionLeg(null);

        assertIssue(entry, IssueCode.REQUIRED_PROJECTION_MISSING);
    }

    /**
     * Checks that the old security projection must point to the source posting.
     * Pointing it at the target posting would make the source and target sides
     * contradict the native leg definitions.
     */
    @Test
    public void testOldSecurityProjectionPointingToTargetPostingIsRejected()
    {
        var entry = copyValidSpinOff();
        var targetPosting = postingFor(entry, LedgerProjectionRole.NEW_SECURITY_LEG);

        projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG).getPrimaryPosting().setCorporateActionLeg(targetPosting.getCorporateActionLeg());

        assertIssue(entry, IssueCode.REQUIRED_PROJECTION_MISSING);
    }

    /**
     * Checks that the new security projection must point to the target posting.
     * Pointing it at the source posting would mix the old and received security
     * sides.
     */
    @Test
    public void testNewSecurityProjectionPointingToSourcePostingIsRejected()
    {
        var entry = copyValidSpinOff();
        var sourcePosting = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);

        projection(entry, LedgerProjectionRole.NEW_SECURITY_LEG).getPrimaryPosting().setCorporateActionLeg(sourcePosting.getCorporateActionLeg());

        assertIssue(entry, IssueCode.REQUIRED_PROJECTION_MISSING);
    }

    /**
     * Checks that required leg parameters must be stored on the matching posting.
     * The validator does not derive missing ratio facts from another location.
     */
    @Test
    public void testMissingRequiredLegParameterIsRejected()
    {
        var entry = copyValidSpinOff();

        for (var posting : entry.getPostings())
            removePostingParameters(posting, LedgerParameterType.RATIO_NUMERATOR);

        assertIssue(entry, IssueCode.REQUIRED_LEG_PARAMETER_MISSING);
    }

    /**
     * Checks that a leg parameter placed at entry level does not satisfy the leg.
     * This prevents event facts and leg facts from becoming two competing truths.
     */
    @Test
    public void testLegParameterOnEntryLevelIsRejected()
    {
        var fixture = fixture();
        var entry = validSpinOff(fixture).buildDetached().getEntry();
        var sourcePosting = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);

        removePostingParameters(sourcePosting, LedgerParameterType.SOURCE_SECURITY);
        entry.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY, fixture.siemens));

        assertIssue(entry, IssueCode.PARAMETER_PLACEMENT_INVALID);
    }

    /**
     * Checks that a required source-leg parameter on another posting does not
     * satisfy the source leg.
     * The validator keeps source and target facts tied to their matching postings.
     */
    @Test
    public void testRequiredLegParameterOnWrongPostingIsRejected()
    {
        var entry = copyValidSpinOff();
        var sourcePosting = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);

        removePostingParameters(sourcePosting, LedgerParameterType.SOURCE_SECURITY);

        assertIssue(entry, IssueCode.PARAMETER_PLACEMENT_INVALID);
    }

    /**
     * Checks that the cash compensation projection carries its configured group
     * anchor when the leg definition expects it.
     * The validator only checks the anchor, not fee or tax group membership.
     */
    @Test
    public void testMissingPostingGroupUUIDForCashCompensationProjectionIsRejected()
    {
        var entry = copyValidSpinOff();

        projection(entry, LedgerProjectionRole.CASH_COMPENSATION).getPrimaryPosting().setGroupKey(null);

        assertIssue(entry, IssueCode.PROJECTION_POSTING_GROUP_REQUIRED);
    }

    /**
     * Checks that buildDetached accepts partial spin-off movement shapes after
     * the definition cardinality cleanup.
     */
    @Test
    public void testAssemblerBuildDetachedAcceptsPartialSpinOffEntry()
    {
        var fixture = fixture();
        var result = baseSpinOff(fixture).securityLeg(targetLeg(fixture).build()).buildDetached();

        assertTrue(result.getValidationResult().isOK());
        assertThat(result.getEntry().getPostings().size(), is(1));
    }

    /**
     * Checks that buildAndAdd accepts partial spin-off movement shapes and
     * materializes the descriptors that are present.
     */
    @Test
    public void testAssemblerBuildAndAddAcceptsPartialSpinOffEntry()
    {
        var fixture = fixture();
        var result = baseSpinOff(fixture).securityLeg(targetLeg(fixture).build()).buildAndAdd();

        assertTrue(result.getValidationResult().isOK());
        assertThat(fixture.client.getLedger().getEntries().size(), is(1));
        assertThat(fixture.account.getTransactions().size(), is(0));
        assertThat(fixture.portfolio.getTransactions().size(), is(1));
    }

    /**
     * Checks that legacy fixed-shape entries stay outside native definition
     * validation.
     * Legacy compatibility paths remain governed by their existing creators,
     * converters, and structural validation rules.
     */
    @Test
    public void testLegacyFixedShapeEntryIsIgnoredByNativeDefinitionValidator()
    {
        var entry = new LedgerEntry("legacy-buy");
        entry.setType(name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType.BUY);

        assertTrue(LedgerNativeEntryDefinitionValidator.validate(entry).isOK());
    }

    private static void assertIssue(LedgerEntry entry, IssueCode code)
    {
        var result = LedgerNativeEntryDefinitionValidator.validate(entry);

        assertTrue(result.format(), result.hasIssue(code));
        assertTrue(result.format(), result.format().contains("[LEDGER-STRUCT-"));
    }

    private static void assertOK(LedgerEntry entry)
    {
        var result = LedgerNativeEntryDefinitionValidator.validate(entry);

        assertTrue(result.format(), result.isOK());
    }

    private static LedgerEntry copyValidSpinOff()
    {
        return LedgerModelCopy.copyEntry(validSpinOff(fixture()).buildDetached().getEntry());
    }

    private static LedgerNativeEntryAssembler.EntryBuilder validSpinOff(Fixture fixture)
    {
        return baseSpinOff(fixture) //
                        .securityLeg(sourceLeg(fixture).build()) //
                        .securityLeg(targetLeg(fixture).build()) //
                        .cashCompensation(NativeCashCompensation.builder() //
                                        .account(fixture.account) //
                                        .amount(money(5)) //
                                        .kind(CashCompensationKind.CASH_IN_LIEU) //
                                        .applied(true) //
                                        .fractionQuantity(new BigDecimal("0.5")) //
                                        .fractionTreatment(FractionTreatment.CASH_IN_LIEU) //
                                        .roundingMode(RoundingModeCode.FLOOR) //
                                        .build()) //
                        .fee(NativeFee.of(fixture.account, money(2), FeeReason.CORPORATE_ACTION_FEE)) //
                        .tax(NativeTax.builder() //
                                        .account(fixture.account) //
                                        .amount(money(1)) //
                                        .reason(TaxReason.WITHHOLDING_TAX) //
                                        .taxableDistribution(true) //
                                        .withholdingTax(true) //
                                        .transactionTax(false) //
                                        .reclaimableTax(false) //
                                        .build());
    }

    private static LedgerNativeEntryAssembler.EntryBuilder baseSpinOff(Fixture fixture)
    {
        return LedgerNativeEntryAssembler.forClient(fixture.client).spinOff() //
                        .metadata(metadata()) //
                        .event(event());
    }

    private static NativeEntryMetadata metadata()
    {
        return NativeEntryMetadata.of(LocalDateTime.of(2020, 9, 28, 0, 0)) //
                        .note("Native corporate action") //
                        .source("native-definition-validator-test");
    }

    private static NativeCorporateActionEvent event()
    {
        return NativeCorporateActionEvent.builder() //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .subtype(CorporateActionSubtype.STANDARD) //
                        .reference("SPIN_OFF-2020") //
                        .stage(EventStage.SETTLED) //
                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                        .build();
    }

    private static NativeSecurityLeg.Builder sourceLeg(Fixture fixture)
    {
        return NativeSecurityLeg.source() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemens) //
                        .shares(Values.Share.factorize(10)) //
                        .amount(money(100)) //
                        .sourceSecurity(fixture.siemens) //
                        .targetSecurity(fixture.siemensEnergy) //
                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)));
    }

    private static NativeSecurityLeg.Builder targetLeg(Fixture fixture)
    {
        return NativeSecurityLeg.target() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemensEnergy) //
                        .shares(Values.Share.factorize(5)) //
                        .amount(money(50)) //
                        .sourceSecurity(fixture.siemens) //
                        .targetSecurity(fixture.siemensEnergy) //
                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)));
    }

    private static LedgerPosting securityContextPosting(Fixture fixture, String localKey)
    {
        var posting = new LedgerPosting("security-context-" + localKey); //$NON-NLS-1$

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(fixture.portfolio);
        posting.setSecurity(fixture.siemens);
        posting.setAmount(0L);
        posting.setShares(0L);
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setCorporateActionLeg(CorporateActionLeg.SECURITY_CONTEXT);
        posting.setGroupKey("main"); //$NON-NLS-1$
        posting.setLocalKey(localKey);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.SECURITY_CONTEXT.getCode()));

        return posting;
    }

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor projection(
                    LedgerEntry entry, LedgerProjectionRole role)
    {
        return name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow();
    }

    private static LedgerPosting postingFor(LedgerEntry entry, LedgerProjectionRole role)
    {
        var postingUUID = projection(entry, role).getPrimaryPosting().getUUID();

        return entry.getPostings().stream().filter(posting -> posting.getUUID().equals(postingUUID)).findFirst()
                        .orElseThrow();
    }

    private static void removeEntryParameters(LedgerEntry entry, LedgerParameterType type)
    {
        for (var parameter : new ArrayList<>(entry.getParameters()))
            if (parameter.getType() == type)
                entry.removeParameter(parameter);
    }

    private static void removePostingParameters(LedgerPosting posting, LedgerParameterType type)
    {
        for (var parameter : new ArrayList<>(posting.getParameters()))
            if (parameter.getType() == type)
                posting.removeParameter(parameter);
    }

    private static Fixture fixture()
    {
        var client = new Client();

        var account = new Account();
        account.setName("Cash");
        client.addAccount(account);

        var portfolio = new Portfolio();
        portfolio.setName("Portfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        var siemens = new Security("Siemens AG", CurrencyUnit.EUR);
        client.addSecurity(siemens);

        var siemensEnergy = new Security("Siemens Energy AG", CurrencyUnit.EUR);
        client.addSecurity(siemensEnergy);

        return new Fixture(client, account, portfolio, siemens, siemensEnergy);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security siemens, Security siemensEnergy)
    {
    }
}
