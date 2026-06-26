package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
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
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssemblyException;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssemblyIssue;
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
     * Checks that a native entry without its required corporate-action kind is
     * rejected before it can be used by supported create paths.
     */
    @Test
    public void testMissingRequiredEntryParameterIsRejected()
    {
        var entry = copyValidSpinOff();
        removeEntryParameters(entry, LedgerParameterType.CORPORATE_ACTION_KIND);

        assertIssue(entry, IssueCode.REQUIRED_ENTRY_PARAMETER_MISSING);
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
     * Checks that the source security leg is required for spin-offs.
     * Removing its posting and projection leaves the entry definition incomplete.
     */
    @Test
    public void testMissingSourceSecurityLegIsRejected()
    {
        var entry = copyValidSpinOff();
        var sourcePosting = postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG);

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG));
        entry.removePosting(sourcePosting);

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    /**
     * Checks that the target security leg is required for spin-offs.
     * Removing its posting and projection prevents the new security side from
     * being represented as a native leg.
     */
    @Test
    public void testMissingTargetSecurityLegIsRejected()
    {
        var entry = copyValidSpinOff();
        var targetPosting = postingFor(entry, LedgerProjectionRole.NEW_SECURITY_LEG);

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));
        entry.removePosting(targetPosting);

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    /**
     * Checks that an exactly-one source leg cannot map to multiple postings.
     * The validator reports the ambiguity instead of guessing which posting is
     * the real source side.
     */
    @Test
    public void testDuplicateSourceLegIsRejectedAsAmbiguous()
    {
        var entry = copyValidSpinOff();
        var sourcePosting = LedgerModelCopy.copyPosting(postingFor(entry, LedgerProjectionRole.OLD_SECURITY_LEG));
        sourcePosting.setUUID("duplicate-source-posting");
        entry.addPosting(sourcePosting);

        var projection = new LedgerProjectionRef("duplicate-source-projection");
        projection.setRole(LedgerProjectionRole.OLD_SECURITY_LEG);
        projection.setPortfolio(sourcePosting.getPortfolio());
        projection.setPrimaryPosting(sourcePosting);
        entry.addProjectionRef(projection);

        assertIssue(entry, IssueCode.AMBIGUOUS_LEG_MATCH);
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

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG));

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

        entry.removeProjectionRef(projection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));

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

        projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG).setPrimaryPosting(targetPosting);

        assertIssue(entry, IssueCode.PROJECTION_PRIMARY_POSTING_MISMATCH);
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

        projection(entry, LedgerProjectionRole.NEW_SECURITY_LEG).setPrimaryPosting(sourcePosting);

        assertIssue(entry, IssueCode.PROJECTION_PRIMARY_POSTING_MISMATCH);
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

        projection(entry, LedgerProjectionRole.CASH_COMPENSATION).setPostingGroupUUID(null);

        assertIssue(entry, IssueCode.PROJECTION_POSTING_GROUP_REQUIRED);
    }

    /**
     * Checks that buildDetached rejects native entries that are incomplete
     * against the definition.
     * Raw model construction remains possible for tests, but supported builder
     * paths must not return invalid native entries.
     */
    @Test
    public void testAssemblerBuildDetachedRejectsDefinitionIncompleteEntry()
    {
        var fixture = fixture();
        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(targetLeg(fixture).build()).buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
    }

    /**
     * Checks that buildAndAdd does not attach a definition-incomplete native
     * entry to the live client.
     * The failure happens before any Ledger entry or runtime projection is added.
     */
    @Test
    public void testAssemblerBuildAndAddRejectsDefinitionIncompleteEntryBeforeMutation()
    {
        var fixture = fixture();
        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(targetLeg(fixture).build()).buildAndAdd());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertThat(fixture.account.getTransactions().size(), is(0));
        assertThat(fixture.portfolio.getTransactions().size(), is(0));
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

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow();
    }

    private static LedgerPosting postingFor(LedgerEntry entry, LedgerProjectionRole role)
    {
        var postingUUID = projection(entry, role).getPrimaryPostingUUID();

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
