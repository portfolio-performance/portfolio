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
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisMethod;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisStatus;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.FractionTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
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
     * Checks that Registry.md requires at least one received spin-off security.
     * Missing target movement rows violate the supported native definition subset.
     */
    @Test
    public void testMissingTargetSecurityLegIsRejected()
    {
        var entry = copyValidSpinOff();
        var targetPosting = postingFor(entry, LedgerProjectionRole.NEW_SECURITY_LEG);

        entry.removePosting(targetPosting);

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    @Test
    public void testSpinOffDefinitionRejectsMissingSecurityContextLeg()
    {
        var entry = copyValidSpinOff();

        removePosting(entry, CorporateActionLeg.SECURITY_CONTEXT);

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    @Test
    public void testSpinOffDefinitionAcceptsSecurityContextLeg()
    {
        var fixture = fixture();
        var entry = copyValidSpinOff();

        entry.addPosting(securityContextPosting(fixture, "context-2")); //$NON-NLS-1$

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

    @Test
    public void testStockDividendDefinitionAcceptsMissingOptionalSecurityContextLeg()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.STOCK_DIVIDEND);

        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testStockDividendDefinitionAcceptsSecurityContextLeg()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.STOCK_DIVIDEND);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testCouponPaymentDefinitionRequiresSecurityContextLeg()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.COUPON_PAYMENT);

        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    @Test
    public void testCouponPaymentDefinitionRejectsCashWithoutInterestDetail()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.COUPON_PAYMENT);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_COMPONENT_MISSING);
    }

    @Test
    public void testCouponPaymentDefinitionAcceptsCashWithSameGroupInterestDetail()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.COUPON_PAYMENT);
        var interest = accruedInterestPosting(fixture, "interest-1"); //$NON-NLS-1$

        interest.setGroupKey("cash-1"); //$NON-NLS-1$
        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$
        entry.addPosting(interest);

        assertOK(entry);
    }

    @Test
    public void testCouponPaymentDefinitionRejectsDifferentGroupInterestDetail()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.COUPON_PAYMENT);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$
        entry.addPosting(accruedInterestPosting(fixture, "interest-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_COMPONENT_MISSING);
    }

    @Test
    public void testCouponPaymentDefinitionRejectsCashWithoutGroupKey()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.COUPON_PAYMENT);
        var cash = cashPosting(fixture, "cash-1"); //$NON-NLS-1$
        var interest = accruedInterestPosting(fixture, "interest-1"); //$NON-NLS-1$

        cash.setGroupKey(null);
        interest.setGroupKey("cash-1"); //$NON-NLS-1$
        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cash);
        entry.addPosting(interest);

        assertIssue(entry, IssueCode.REQUIRED_COMPONENT_MISSING);
    }

    @Test
    public void testCouponPaymentDefinitionRejectsInterestWithoutCash()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.COUPON_PAYMENT);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(accruedInterestPosting(fixture, "interest-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    @Test
    public void testPikInterestDefinitionRejectsTargetSecurityWithoutInterestDetail()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.PIK_INTEREST);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_COMPONENT_MISSING);
    }

    @Test
    public void testPikInterestDefinitionAcceptsTargetSecurityWithSameGroupInterestDetail()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.PIK_INTEREST);
        var interest = accruedInterestPosting(fixture, "interest-1"); //$NON-NLS-1$

        interest.setGroupKey("main"); //$NON-NLS-1$
        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$
        entry.addPosting(interest);

        assertOK(entry);
    }

    @Test
    public void testPikInterestDefinitionRejectsDifferentGroupInterestDetail()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.PIK_INTEREST);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$
        entry.addPosting(accruedInterestPosting(fixture, "interest-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_COMPONENT_MISSING);
    }

    @Test
    public void testCashDistributionAcceptsSecurityContextAndCash()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testCashDistributionRejectsSecurityContextOnly()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testCashDistributionRejectsCashWithoutSecurityContext()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION);

        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    @Test
    public void testCashDistributionRejectsFeeOrTaxOnly()
    {
        var fixture = fixture();

        assertIssue(corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION,
                        securityContextPosting(fixture, "context-1"), feePosting(fixture, "fee-1")), //$NON-NLS-1$ //$NON-NLS-2$
                        IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
        assertIssue(corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION,
                        securityContextPosting(fixture, "context-1"), taxPosting(fixture, "tax-1")), //$NON-NLS-1$ //$NON-NLS-2$
                        IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testCashDistributionRejectsSecurityContextWithoutPortfolio()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION);
        var context = securityContextPosting(fixture, "context-1"); //$NON-NLS-1$

        context.setPortfolio(null);
        entry.addPosting(context);
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        var ledger = new Ledger();

        ledger.addEntry(entry);

        var result = LedgerStructuralValidator.validate(ledger);

        assertTrue(result.format(), result.hasIssue(
                        LedgerStructuralValidator.IssueCode.POSTING_PORTFOLIO_REQUIRED_FOR_SECURITY));
    }

    @Test
    public void testCashDistributionAcceptsRepeatedCashWithDistinctLocalKeys()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-2")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testCashDistributionRejectsDuplicateCashLocalKey()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.LEG_LOCAL_KEY_DUPLICATE);
    }

    @Test
    public void testCashDistributionRejectsSourceOrTargetSecurityMovement()
    {
        var fixture = fixture();

        assertIssue(corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION,
                        securityContextPosting(fixture, "context-1"), cashPosting(fixture, "cash-1"), //$NON-NLS-1$ //$NON-NLS-2$
                        sourceSecurityPosting(fixture, "source-1")), IssueCode.LEG_POSTING_NOT_ALLOWED); //$NON-NLS-1$
        assertIssue(corporateActionEntry(CorporateActionKind.CASH_DISTRIBUTION,
                        securityContextPosting(fixture, "context-1"), cashPosting(fixture, "cash-1"), //$NON-NLS-1$ //$NON-NLS-2$
                        targetSecurityPosting(fixture, "target-1")), IssueCode.LEG_POSTING_NOT_ALLOWED); //$NON-NLS-1$
    }

    @Test
    public void testMaturityDefinitionRequiresSecurityContextLeg()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.MATURITY);

        entry.addPosting(sourceSecurityPosting(fixture, "source-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$
        entry.addPosting(principalPosting(fixture, "principal-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.LEG_CARDINALITY_VIOLATED);
    }

    @Test
    public void testMaturityDefinitionAcceptsSecurityContextSourceCashAndPrincipal()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.MATURITY);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(sourceSecurityPosting(fixture, "source-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$
        entry.addPosting(principalPosting(fixture, "principal-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testDefaultedInterestRequiresPrimaryMovementBeyondContext()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.DEFAULTED_INTEREST);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testDefaultedInterestAcceptsCashPrimaryMovement()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.DEFAULTED_INTEREST);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(cashPosting(fixture, "cash-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testDefaultedInterestAcceptsFeeOrTaxPrimaryMovement()
    {
        var fixture = fixture();
        var feeEntry = corporateActionEntry(CorporateActionKind.DEFAULTED_INTEREST);
        var taxEntry = corporateActionEntry(CorporateActionKind.DEFAULTED_INTEREST);

        feeEntry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        feeEntry.addPosting(feePosting(fixture, "fee-1")); //$NON-NLS-1$
        taxEntry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        taxEntry.addPosting(taxPosting(fixture, "tax-1")); //$NON-NLS-1$

        assertOK(feeEntry);
        assertOK(taxEntry);
    }

    @Test
    public void testDefaultedInterestAcceptsClaimSecurityPrimaryMovement()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.DEFAULTED_INTEREST);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$

        assertOK(entry);
    }

    @Test
    public void testDefaultedInterestRejectsBasisLikeMetadataAsPrimaryMovement()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.DEFAULTED_INTEREST);
        var context = securityContextPosting(fixture, "context-1"); //$NON-NLS-1$

        context.addParameter(LedgerParameter.ofMoney(LedgerParameterType.FAIR_MARKET_VALUE, money(100)));
        entry.addPosting(context);

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testRestructuringRequiresPrimaryMovementBeyondContext()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.RESTRUCTURING);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testRestructuringAcceptsSecurityCashPrincipalOrInterestPrimaryMovement()
    {
        var fixture = fixture();

        assertOK(corporateActionEntry(CorporateActionKind.RESTRUCTURING,
                        sourceSecurityPosting(fixture, "source-1"))); //$NON-NLS-1$
        assertOK(corporateActionEntry(CorporateActionKind.RESTRUCTURING,
                        targetSecurityPosting(fixture, "target-1"))); //$NON-NLS-1$
        assertOK(corporateActionEntry(CorporateActionKind.RESTRUCTURING, cashPosting(fixture, "cash-1"))); //$NON-NLS-1$
        assertOK(corporateActionEntry(CorporateActionKind.RESTRUCTURING,
                        principalPosting(fixture, "principal-1"))); //$NON-NLS-1$
        assertOK(corporateActionEntry(CorporateActionKind.RESTRUCTURING,
                        accruedInterestPosting(fixture, "interest-1"))); //$NON-NLS-1$
    }

    @Test
    public void testRestructuringRejectsFeeOrTaxOnlyPrimaryMovement()
    {
        var fixture = fixture();

        assertIssue(corporateActionEntry(CorporateActionKind.RESTRUCTURING, feePosting(fixture, "fee-1")), //$NON-NLS-1$
                        IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
        assertIssue(corporateActionEntry(CorporateActionKind.RESTRUCTURING, taxPosting(fixture, "tax-1")), //$NON-NLS-1$
                        IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testDefaultRequiresPrimaryMovementBeyondContext()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.DEFAULT);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testDefaultAcceptsOneBookedPrimaryMovement()
    {
        var fixture = fixture();

        assertOK(corporateActionEntry(CorporateActionKind.DEFAULT, securityContextPosting(fixture, "context-1"), //$NON-NLS-1$
                        sourceSecurityPosting(fixture, "source-1"))); //$NON-NLS-1$
        assertOK(corporateActionEntry(CorporateActionKind.DEFAULT, securityContextPosting(fixture, "context-1"), //$NON-NLS-1$
                        cashPosting(fixture, "cash-1"))); //$NON-NLS-1$
        assertOK(corporateActionEntry(CorporateActionKind.DEFAULT, securityContextPosting(fixture, "context-1"), //$NON-NLS-1$
                        taxPosting(fixture, "tax-1"))); //$NON-NLS-1$
    }

    @Test
    public void testForexDoesNotSatisfyPrimaryMovement()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.RESTRUCTURING);

        entry.addPosting(forexPosting(fixture, "forex-1")); //$NON-NLS-1$

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testBasisUnknownWithoutAllocationsIsAccepted()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addBasisStatus(entry, CorporateActionBasisStatus.UNKNOWN);

        assertOK(entry);
    }

    @Test
    public void testBasisNotApplicableWithoutAllocationsIsAccepted()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addBasisStatus(entry, CorporateActionBasisStatus.NOT_APPLICABLE);

        assertOK(entry);
    }

    @Test
    public void testBasisNotApplicableWithAllocationsIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addBasisStatus(entry, CorporateActionBasisStatus.NOT_APPLICABLE);
        addBasisMethod(entry, CorporateActionBasisMethod.PERCENTAGE_ALLOCATION);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "100"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.BASIS_ALLOCATION_NOT_ALLOWED);
    }

    @Test
    public void testBasisProvidedWithoutAllocationsIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addBasisStatus(entry, CorporateActionBasisStatus.PROVIDED);
        addBasisMethod(entry, CorporateActionBasisMethod.PERCENTAGE_ALLOCATION);

        assertIssue(entry, IssueCode.BASIS_ALLOCATION_REQUIRED);
    }

    @Test
    public void testBasisPercentageAllocationTotalingOneHundredIsAccepted()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "80"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "20"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertOK(entry);
    }

    @Test
    public void testBasisPercentageAllocationAcrossTwoTargetsIsAccepted()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        entry.addPosting(spinOffTargetSecurityPosting(fixture, "target-2")); //$NON-NLS-1$
        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "75"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "20"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main", "5"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertOK(entry);
    }

    @Test
    public void testBasisPercentageAllocationTotalingNinetyIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "70"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "20"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.BASIS_PERCENT_TOTAL_INVALID);
    }

    @Test
    public void testBasisPercentageAllocationTotalingOneHundredTenIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "90"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "20"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.BASIS_PERCENT_TOTAL_INVALID);
    }

    @Test
    public void testBasisNegativePercentIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "110"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "-10"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.BASIS_PERCENT_INVALID);
    }

    @Test
    public void testBasisUnknownAllocationTargetIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "missing", "main", "100"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.BASIS_ALLOCATION_TARGET_NOT_FOUND);
    }

    @Test
    public void testBasisDuplicateAllocationTargetIsRejected()
    {
        var fixture = fixture();
        var entry = spinOffWithContextAndTarget(fixture);

        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "50"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "50"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.BASIS_ALLOCATION_TARGET_DUPLICATE);
    }

    @Test
    public void testBasisDoesNotSatisfyPrimaryMovement()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.DEFAULT);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main", "100"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertIssue(entry, IssueCode.REQUIRED_PRIMARY_MOVEMENT_MISSING);
    }

    @Test
    public void testConversionBasisAllocationResolvesSemanticTarget()
    {
        var fixture = fixture();
        var entry = corporateActionEntry(CorporateActionKind.CONVERSION);

        entry.addPosting(securityContextPosting(fixture, "context-1")); //$NON-NLS-1$
        entry.addPosting(sourceSecurityPosting(fixture, "source-1")); //$NON-NLS-1$
        entry.addPosting(targetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$
        addProvidedPercentageBasis(entry);
        addBasisAllocation(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", "100"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertOK(entry);
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
        var result = baseSpinOff(fixture) //
                        .securityLeg(contextLeg(fixture).build()) //
                        .securityLeg(targetLeg(fixture).build()).buildDetached();

        assertTrue(result.getValidationResult().isOK());
        assertThat(result.getEntry().getPostings().size(), is(2));
    }

    /**
     * Checks that buildAndAdd accepts partial spin-off movement shapes and
     * materializes the descriptors that are present.
     */
    @Test
    public void testAssemblerBuildAndAddAcceptsPartialSpinOffEntry()
    {
        var fixture = fixture();
        var result = baseSpinOff(fixture) //
                        .securityLeg(contextLeg(fixture).build()) //
                        .securityLeg(targetLeg(fixture).build()).buildAndAdd();

        assertTrue(result.getValidationResult().isOK());
        assertThat(fixture.client.getLedger().getEntries().size(), is(1));
        assertThat(fixture.account.getTransactions().size(), is(0));
        assertThat(fixture.portfolio.getTransactions().size(), is(1));
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
                        .securityLeg(contextLeg(fixture).build()) //
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

    private static NativeSecurityLeg.Builder contextLeg(Fixture fixture)
    {
        return NativeSecurityLeg.context() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemens) //
                        .amount(Money.of(CurrencyUnit.EUR, 0)) //
                        .shares(0L) //
                        .groupKey("main") //
                        .localKey("context-1");
    }

    private static LedgerEntry corporateActionEntry(CorporateActionKind kind)
    {
        var entry = new LedgerEntry("corporate-action-" + kind.getCode()); //$NON-NLS-1$

        entry.setType(name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType.CORPORATE_ACTION);
        entry.setDateTime(LocalDateTime.of(2020, 9, 28, 0, 0));
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_KIND, kind));

        return entry;
    }

    private static LedgerEntry corporateActionEntry(CorporateActionKind kind, LedgerPosting... postings)
    {
        var entry = corporateActionEntry(kind);

        for (var posting : postings)
            entry.addPosting(posting);

        return entry;
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

    private static LedgerPosting targetSecurityPosting(Fixture fixture, String localKey)
    {
        var posting = securityPosting(fixture, localKey, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY, fixture.siemensEnergy);

        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY,
                        fixture.siemensEnergy));

        return posting;
    }

    private static LedgerPosting spinOffTargetSecurityPosting(Fixture fixture, String localKey)
    {
        var posting = targetSecurityPosting(fixture, localKey);

        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR, BigDecimal.ONE));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_DENOMINATOR, BigDecimal.TEN));

        return posting;
    }

    private static LedgerPosting sourceSecurityPosting(Fixture fixture, String localKey)
    {
        var posting = securityPosting(fixture, localKey, LedgerPostingDirection.OUTBOUND,
                        CorporateActionLeg.SOURCE_SECURITY, fixture.siemens);

        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY, fixture.siemens));

        return posting;
    }

    private static LedgerPosting securityPosting(Fixture fixture, String localKey, LedgerPostingDirection direction,
                    CorporateActionLeg leg, Security security)
    {
        var posting = new LedgerPosting("security-" + localKey); //$NON-NLS-1$

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(fixture.portfolio);
        posting.setSecurity(security);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setShares(Values.Share.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(direction);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setCorporateActionLeg(leg);
        posting.setGroupKey("main"); //$NON-NLS-1$
        posting.setLocalKey(localKey);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        return posting;
    }

    private static LedgerPosting cashPosting(Fixture fixture, String localKey)
    {
        var posting = new LedgerPosting("cash-" + localKey); //$NON-NLS-1$

        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(fixture.account);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(localKey);
        posting.setLocalKey(localKey);

        return posting;
    }

    private static LedgerPosting principalPosting(Fixture fixture, String localKey)
    {
        var posting = new LedgerPosting("principal-" + localKey); //$NON-NLS-1$

        posting.setType(LedgerPostingType.PRINCIPAL_REDEMPTION);
        posting.setAccount(fixture.account);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.PRINCIPAL_REDEMPTION);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setCorporateActionLeg(CorporateActionLeg.PRINCIPAL);
        posting.setGroupKey(localKey);
        posting.setLocalKey(localKey);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.PRINCIPAL.getCode()));

        return posting;
    }

    private static LedgerPosting accruedInterestPosting(Fixture fixture, String localKey)
    {
        var posting = new LedgerPosting("interest-" + localKey); //$NON-NLS-1$

        posting.setType(LedgerPostingType.ACCRUED_INTEREST);
        posting.setAccount(fixture.account);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.ACCRUED_INTEREST);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setCorporateActionLeg(CorporateActionLeg.ACCRUED_INTEREST);
        posting.setGroupKey(localKey);
        posting.setLocalKey(localKey);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.ACCRUED_INTEREST.getCode()));

        return posting;
    }

    private static LedgerEntry spinOffWithContextAndTarget(Fixture fixture)
    {
        var entry = corporateActionEntry(CorporateActionKind.SPIN_OFF,
                        securityContextPosting(fixture, "context-1"), //$NON-NLS-1$
                        spinOffTargetSecurityPosting(fixture, "target-1")); //$NON-NLS-1$

        entry.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.EFFECTIVE_DATE,
                        LocalDate.of(2026, 1, 2)));

        return entry;
    }

    private static void addProvidedPercentageBasis(LedgerEntry entry)
    {
        addBasisStatus(entry, CorporateActionBasisStatus.PROVIDED);
        addBasisMethod(entry, CorporateActionBasisMethod.PERCENTAGE_ALLOCATION);
    }

    private static void addBasisStatus(LedgerEntry entry, CorporateActionBasisStatus status)
    {
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_BASIS_STATUS, status));
    }

    private static void addBasisMethod(LedgerEntry entry, CorporateActionBasisMethod method)
    {
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_BASIS_METHOD, method));
    }

    private static void addBasisAllocation(LedgerEntry entry, LedgerLegRole targetRole, String targetLocalKey,
                    String targetGroupKey, String percent)
    {
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_BASIS_ALLOCATION,
                        CorporateActionBasisAllocation
                                        .percentage(targetRole, targetLocalKey, targetGroupKey, new BigDecimal(percent))
                                        .toParameterValue()));
    }

    private static LedgerPosting feePosting(Fixture fixture, String localKey)
    {
        var posting = unitPosting(fixture, localKey, LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE,
                        LedgerPostingUnitRole.FEE, CorporateActionLeg.FEE);

        posting.addParameter(LedgerParameter.ofCode(LedgerParameterType.FEE_REASON,
                        FeeReason.CORPORATE_ACTION_FEE));

        return posting;
    }

    private static LedgerPosting taxPosting(Fixture fixture, String localKey)
    {
        var posting = unitPosting(fixture, localKey, LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX,
                        LedgerPostingUnitRole.TAX, CorporateActionLeg.TAX);

        posting.addParameter(LedgerParameter.ofCode(LedgerParameterType.TAX_REASON,
                        TaxReason.WITHHOLDING_TAX));

        return posting;
    }

    private static LedgerPosting unitPosting(Fixture fixture, String localKey, LedgerPostingType type,
                    LedgerPostingSemanticRole semanticRole, LedgerPostingUnitRole unitRole, CorporateActionLeg leg)
    {
        var posting = new LedgerPosting(type.getCode().toLowerCase() + "-" + localKey); //$NON-NLS-1$

        posting.setType(type);
        posting.setAccount(fixture.account);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(semanticRole);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(unitRole);
        posting.setCorporateActionLeg(leg);
        posting.setGroupKey(localKey);
        posting.setLocalKey(localKey);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        return posting;
    }

    private static LedgerPosting forexPosting(Fixture fixture, String localKey)
    {
        var posting = new LedgerPosting("forex-" + localKey); //$NON-NLS-1$

        posting.setType(LedgerPostingType.FOREX);
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.FOREX_CONTEXT);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.FOREX_CONTEXT);
        posting.setGroupKey(localKey);
        posting.setLocalKey(localKey);
        posting.addParameter(LedgerParameter.ofMoney(LedgerParameterType.REFERENCE_PRICE, money(1)));

        return posting;
    }

    private static void removePosting(LedgerEntry entry, CorporateActionLeg leg)
    {
        entry.getPostings().stream().filter(posting -> posting.getCorporateActionLeg() == leg).findFirst()
                        .ifPresent(entry::removePosting);
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
