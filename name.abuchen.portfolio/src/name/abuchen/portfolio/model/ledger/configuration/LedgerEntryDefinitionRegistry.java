package name.abuchen.portfolio.model.ledger.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerComponentRequirement;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerParameterRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingGroupRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPrimaryMovement;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerProjectionRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirement;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirementGroup;

/**
 * Registers Ledger configuration definitions used by validation and assembly.
 * This is configuration infrastructure. Normal transaction-editing code should not update
 * the registry directly.
 *
 * <p>
 * The registry is static Java configuration. It is not persisted in XML or Protobuf; files
 * store the resulting Ledger entries, postings, parameters, and semantic projection facts.
 * </p>
 */
public final class LedgerEntryDefinitionRegistry
{
    private static final String CASH_COMPENSATION_GROUP = "CASH_COMPENSATION_GROUP"; //$NON-NLS-1$

    private static final SetBuilder SETS = new SetBuilder();

    private static final Map<LedgerEntryType, LedgerEntryDefinition> DEFINITIONS = definitions();
    private static final Map<CorporateActionKind, LedgerEntryDefinition> CORPORATE_ACTION_DEFINITIONS = corporateActionDefinitions();

    private LedgerEntryDefinitionRegistry()
    {
    }

    public static Optional<LedgerEntryDefinition> lookup(LedgerEntryType entryType)
    {
        if (entryType == LedgerEntryType.CORPORATE_ACTION)
            return lookup(entryType, CorporateActionKind.SPIN_OFF);

        return Optional.ofNullable(DEFINITIONS.get(entryType));
    }

    public static Optional<LedgerEntryDefinition> lookup(LedgerEntry entry)
    {
        if (entry == null || entry.getType() == null)
            return Optional.empty();

        if (entry.getType() == LedgerEntryType.CORPORATE_ACTION)
            return CorporateActionKind.fromEntry(entry).flatMap(kind -> lookup(entry.getType(), kind));

        return lookup(entry.getType());
    }

    public static Optional<LedgerEntryDefinition> lookup(LedgerEntryType entryType, CorporateActionKind kind)
    {
        if (entryType != LedgerEntryType.CORPORATE_ACTION)
            return lookup(entryType);

        if (kind == null)
            return Optional.empty();

        return Optional.ofNullable(CORPORATE_ACTION_DEFINITIONS.get(kind));
    }

    public static Collection<LedgerEntryDefinition> getDefinitions()
    {
        var definitions = new ArrayList<LedgerEntryDefinition>();
        definitions.addAll(DEFINITIONS.values());
        definitions.addAll(CORPORATE_ACTION_DEFINITIONS.values());
        return Collections.unmodifiableList(definitions);
    }

    public static boolean hasDefinition(LedgerEntryType entryType)
    {
        if (entryType == LedgerEntryType.CORPORATE_ACTION)
            return !CORPORATE_ACTION_DEFINITIONS.isEmpty();

        return DEFINITIONS.containsKey(entryType);
    }

    private static Map<LedgerEntryType, LedgerEntryDefinition> definitions()
    {
        var definitions = new EnumMap<LedgerEntryType, LedgerEntryDefinition>(LedgerEntryType.class);

        return Collections.unmodifiableMap(definitions);
    }

    private static Map<CorporateActionKind, LedgerEntryDefinition> corporateActionDefinitions()
    {
        var definitions = new EnumMap<CorporateActionKind, LedgerEntryDefinition>(CorporateActionKind.class);

        register(definitions, CorporateActionKind.CASH_DISTRIBUTION, cashDistribution());
        register(definitions, CorporateActionKind.STOCK_DIVIDEND, stockDividend());
        register(definitions, CorporateActionKind.SPIN_OFF, spinOff());
        register(definitions, CorporateActionKind.BONUS_ISSUE, bonusIssue());
        register(definitions, CorporateActionKind.RIGHTS_DISTRIBUTION, rightsDistribution());
        register(definitions, CorporateActionKind.COUPON_PAYMENT, couponPayment());
        register(definitions, CorporateActionKind.PIK_INTEREST, pikInterest());
        register(definitions, CorporateActionKind.DEFAULTED_INTEREST, defaultedInterest());
        register(definitions, CorporateActionKind.MATURITY, maturity());
        register(definitions, CorporateActionKind.PARTIAL_REDEMPTION, partialRedemption());
        register(definitions, CorporateActionKind.CALL, call());
        register(definitions, CorporateActionKind.PUT, put());
        register(definitions, CorporateActionKind.CONVERSION, conversion());
        register(definitions, CorporateActionKind.EXCHANGE, exchange());
        register(definitions, CorporateActionKind.RESTRUCTURING, restructuring());
        register(definitions, CorporateActionKind.DEFAULT, defaultAction());

        return Collections.unmodifiableMap(definitions);
    }

    private static void register(Map<CorporateActionKind, LedgerEntryDefinition> definitions,
                    CorporateActionKind kind, LedgerEntryDefinition definition)
    {
        if (definitions.put(kind, definition) != null)
            throw new IllegalStateException(LedgerDiagnosticCode.LEDGER_CORE_013
                            .message("Duplicate Ledger corporate action definition: " + kind)); //$NON-NLS-1$
    }

    private static LedgerEntryDefinition spinOff()
    {
        return LedgerEntryDefinition.of(LedgerEntryType.CORPORATE_ACTION,
                        LedgerNativeEntryShape.DUAL_INSTRUMENT_PLUS_ACCOUNT,
                        SETS.postingRules(
                                        optionalPosting(LedgerPostingType.SECURITY, requiredSecurityLegParameters(),
                                                        spinOffSecurityOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH_COMPENSATION, SETS.parameterTypes(),
                                                        cashCompensationOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FEE, SETS.parameterTypes(),
                                                        feeOptionalParameters()),
                                        optionalPosting(LedgerPostingType.TAX, SETS.parameterTypes(),
                                                        taxOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FOREX, SETS.parameterTypes(),
                                                        forexOptionalParameters())),
                        corporateActionEntryParameterRules(),
                        SETS.parameterRules(repeatableRequiredPostingParameter(LedgerParameterType.CORPORATE_ACTION_LEG),
                                        repeatableRequiredPostingParameter(LedgerParameterType.SOURCE_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.TARGET_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_NUMERATOR),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_DENOMINATOR),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CASH_COMPENSATION_KIND),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CASH_IN_LIEU_AMOUNT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CASH_IN_LIEU_APPLIED),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_QUANTITY),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_TREATMENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.ROUNDING_MODE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.COST_ALLOCATION_METHOD),
                                        repeatableOptionalPostingParameter(LedgerParameterType.SOURCE_COST_PERCENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TARGET_COST_PERCENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.REFERENCE_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FAIR_MARKET_VALUE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.VALUATION_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FEE_REASON),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAX_REASON),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAXABLE_DISTRIBUTION),
                                        repeatableOptionalPostingParameter(LedgerParameterType.WITHHOLDING_TAX),
                                        repeatableOptionalPostingParameter(LedgerParameterType.RECLAIMABLE_TAX),
                                        repeatableOptionalPostingParameter(LedgerParameterType.MANUAL_VALUATION_OVERRIDE)),
                        SETS.projectionRules(
                                        optionalProjection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false),
                                        optionalProjection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false),
                                        optionalProjection(LedgerProjectionRole.CASH_COMPENSATION, true, true),
                                        optionalProjection(LedgerProjectionRole.DELIVERY_INBOUND, true, false)),
                        cashCompensationPostingGroupRules(),
                        SETS.alternativeGroups(dateAlternative("SPIN_OFF_DATE")), //$NON-NLS-1$
                        spinOffLegDefinitions(),
                        LedgerReportingClass.SECURITIES_DISTRIBUTION,
                        LedgerPerformanceTreatment.COST_BASIS_REALLOCATION, downstreamResults());
    }

    private static LedgerEntryDefinition stockDividend()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.OPTIONAL),
                                        targetSecurityLeg(LedgerLegCardinality.AT_LEAST_ONE), cashCompensationLeg(),
                                        feeLeg(), taxLeg(), forexLeg()),
                        LedgerReportingClass.SECURITIES_DISTRIBUTION,
                        LedgerPerformanceTreatment.SECURITY_DISTRIBUTION);
    }

    private static LedgerEntryDefinition cashDistribution()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        cashLeg(LedgerLegCardinality.AT_LEAST_ONE), feeLeg(), taxLeg()),
                        primaryMovementGroup("CASH_DISTRIBUTION_PRIMARY_MOVEMENT", //$NON-NLS-1$
                                        LedgerPrimaryMovement.CASH),
                        LedgerReportingClass.CASH_DIVIDEND,
                        LedgerPerformanceTreatment.INCOME_DISTRIBUTION);
    }

    private static LedgerEntryDefinition bonusIssue()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.OPTIONAL),
                                        targetSecurityLeg(LedgerLegCardinality.AT_LEAST_ONE), cashCompensationLeg(),
                                        feeLeg(), taxLeg(), forexLeg()),
                        LedgerReportingClass.SECURITIES_DISTRIBUTION,
                        LedgerPerformanceTreatment.SECURITY_DISTRIBUTION);
    }

    private static LedgerEntryDefinition rightsDistribution()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.OPTIONAL),
                                        distributedRightLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        cashCompensationLeg(), feeLeg(), taxLeg(), forexLeg()),
                        LedgerReportingClass.RIGHTS_EVENT,
                        LedgerPerformanceTreatment.SECURITY_DISTRIBUTION);
    }

    private static LedgerEntryDefinition couponPayment()
    {
        return corporateActionDefinitionWithComponents(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        cashLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        accruedInterestLeg(LedgerLegCardinality.OPTIONAL), feeLeg(), taxLeg(),
                                        forexLeg()),
                        SETS.componentRequirements(interestComponentRequirement(
                                        "COUPON_PAYMENT_INTEREST_DETAIL", LedgerLegRole.CASH_LEG)), //$NON-NLS-1$
                        LedgerReportingClass.FIXED_INCOME_COUPON,
                        LedgerPerformanceTreatment.INCOME_DISTRIBUTION);
    }

    private static LedgerEntryDefinition pikInterest()
    {
        return corporateActionDefinitionWithComponents(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        targetSecurityLeg(LedgerLegCardinality.AT_LEAST_ONE), cashCompensationLeg(),
                                        accruedInterestLeg(LedgerLegCardinality.OPTIONAL), feeLeg(), taxLeg(),
                                        forexLeg()),
                        SETS.componentRequirements(interestComponentRequirement(
                                        "PIK_INTEREST_INTEREST_DETAIL", LedgerLegRole.TARGET_SECURITY_LEG)), //$NON-NLS-1$
                        LedgerReportingClass.FIXED_INCOME_COUPON,
                        LedgerPerformanceTreatment.INCOME_DISTRIBUTION);
    }

    private static LedgerEntryDefinition defaultedInterest()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        targetSecurityLeg(LedgerLegCardinality.REPEATABLE),
                                        cashLeg(LedgerLegCardinality.REPEATABLE), feeLeg(), taxLeg()),
                        primaryMovementGroup("DEFAULTED_INTEREST_PRIMARY_MOVEMENT", //$NON-NLS-1$
                                        LedgerPrimaryMovement.CASH, LedgerPrimaryMovement.TARGET_SECURITY,
                                        LedgerPrimaryMovement.FEE, LedgerPrimaryMovement.TAX),
                        LedgerReportingClass.FIXED_INCOME_COUPON,
                        LedgerPerformanceTreatment.INCOME_DISTRIBUTION);
    }

    private static LedgerEntryDefinition maturity()
    {
        return fixedIncomeRedemption();
    }

    private static LedgerEntryDefinition partialRedemption()
    {
        return fixedIncomeRedemption();
    }

    private static LedgerEntryDefinition call()
    {
        return fixedIncomeRedemption();
    }

    private static LedgerEntryDefinition put()
    {
        return fixedIncomeRedemption();
    }

    private static LedgerEntryDefinition fixedIncomeRedemption()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        sourceSecurityLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        cashLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        principalRedemptionLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        accruedInterestLeg(LedgerLegCardinality.OPTIONAL), feeLeg(), taxLeg(),
                                        forexLeg()),
                        LedgerReportingClass.PRINCIPAL_REDEMPTION,
                        LedgerPerformanceTreatment.PRINCIPAL_RETURN);
    }

    private static LedgerEntryDefinition conversion()
    {
        return securityReorganization();
    }

    private static LedgerEntryDefinition exchange()
    {
        return securityReorganization();
    }

    private static LedgerEntryDefinition securityReorganization()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        sourceSecurityLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        targetSecurityLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        cashCompensationLeg(),
                                        accruedInterestLeg(LedgerLegCardinality.OPTIONAL), feeLeg(), taxLeg(),
                                        forexLeg()),
                        LedgerReportingClass.SECURITY_REORGANIZATION,
                        LedgerPerformanceTreatment.COST_BASIS_REALLOCATION);
    }

    private static LedgerEntryDefinition restructuring()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.OPTIONAL),
                                        sourceSecurityLeg(LedgerLegCardinality.REPEATABLE),
                                        targetSecurityLeg(LedgerLegCardinality.REPEATABLE),
                                        cashLeg(LedgerLegCardinality.REPEATABLE),
                                        principalRedemptionLeg(LedgerLegCardinality.REPEATABLE),
                                        accruedInterestLeg(LedgerLegCardinality.REPEATABLE),
                                        feeLeg(), taxLeg(), forexLeg()),
                        primaryMovementGroup("RESTRUCTURING_PRIMARY_MOVEMENT", //$NON-NLS-1$
                                        LedgerPrimaryMovement.SOURCE_SECURITY, LedgerPrimaryMovement.TARGET_SECURITY,
                                        LedgerPrimaryMovement.CASH, LedgerPrimaryMovement.PRINCIPAL_REDEMPTION,
                                        LedgerPrimaryMovement.ACCRUED_INTEREST),
                        LedgerReportingClass.SECURITY_REORGANIZATION,
                        LedgerPerformanceTreatment.COST_BASIS_REALLOCATION);
    }

    private static LedgerEntryDefinition defaultAction()
    {
        return corporateActionDefinition(
                        SETS.legDefinitions(securityContextLeg(LedgerLegCardinality.AT_LEAST_ONE),
                                        sourceSecurityLeg(LedgerLegCardinality.REPEATABLE),
                                        targetSecurityLeg(LedgerLegCardinality.REPEATABLE),
                                        cashLeg(LedgerLegCardinality.REPEATABLE), feeLeg(), taxLeg()),
                        primaryMovementGroup("DEFAULT_PRIMARY_MOVEMENT", //$NON-NLS-1$
                                        LedgerPrimaryMovement.SOURCE_SECURITY, LedgerPrimaryMovement.TARGET_SECURITY,
                                        LedgerPrimaryMovement.CASH, LedgerPrimaryMovement.FEE,
                                        LedgerPrimaryMovement.TAX),
                        LedgerReportingClass.NONE,
                        LedgerPerformanceTreatment.PERFORMANCE_NEUTRAL);
    }

    private static LedgerEntryDefinition corporateActionDefinition(Set<LedgerLegDefinition> legDefinitions,
                    LedgerReportingClass reportingClass, LedgerPerformanceTreatment performanceTreatment)
    {
        return corporateActionDefinition(legDefinitions, Set.of(), Set.of(), reportingClass, performanceTreatment);
    }

    private static LedgerEntryDefinition corporateActionDefinitionWithComponents(Set<LedgerLegDefinition> legDefinitions,
                    Set<LedgerComponentRequirement> componentRequirements, LedgerReportingClass reportingClass,
                    LedgerPerformanceTreatment performanceTreatment)
    {
        return corporateActionDefinition(legDefinitions, Set.of(), componentRequirements, reportingClass,
                        performanceTreatment);
    }

    private static LedgerEntryDefinition corporateActionDefinition(Set<LedgerLegDefinition> legDefinitions,
                    Set<LedgerRequirementGroup> alternativeRequirementGroups, LedgerReportingClass reportingClass,
                    LedgerPerformanceTreatment performanceTreatment)
    {
        return corporateActionDefinition(legDefinitions, alternativeRequirementGroups, Set.of(), reportingClass,
                        performanceTreatment);
    }

    private static LedgerEntryDefinition corporateActionDefinition(Set<LedgerLegDefinition> legDefinitions,
                    Set<LedgerRequirementGroup> alternativeRequirementGroups,
                    Set<LedgerComponentRequirement> componentRequirements, LedgerReportingClass reportingClass,
                    LedgerPerformanceTreatment performanceTreatment)
    {
        return LedgerEntryDefinition.of(LedgerEntryType.CORPORATE_ACTION,
                        LedgerNativeEntryShape.DUAL_INSTRUMENT_PLUS_ACCOUNT,
                        optionalPostingRulesFor(legDefinitions),
                        corporateActionEntryParameterRules(),
                        corporateActionPostingParameterRulesFor(legDefinitions),
                        Set.of(),
                        Set.of(),
                        alternativeRequirementGroups,
                        componentRequirements,
                        legDefinitions,
                        reportingClass,
                        performanceTreatment,
                        downstreamResults());
    }

    private static Set<LedgerPostingRule> optionalPostingRulesFor(Set<LedgerLegDefinition> legDefinitions)
    {
        var postingTypes = EnumSet.noneOf(LedgerPostingType.class);

        for (var legDefinition : legDefinitions)
            postingTypes.add(legDefinition.getPostingType());

        var rules = new LinkedHashSet<LedgerPostingRule>();
        for (var postingType : postingTypes)
            rules.add(optionalPosting(postingType, SETS.parameterTypes(),
                            corporateActionPostingOptionalParametersFor(postingType)));

        return Collections.unmodifiableSet(rules);
    }

    private static Set<LedgerParameterRule> corporateActionEntryParameterRules()
    {
        return SETS.parameterRules(requiredEntryParameter(LedgerParameterType.CORPORATE_ACTION_KIND),
                        optionalEntryParameter(LedgerParameterType.EX_DATE),
                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE),
                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_BASIS_STATUS),
                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_BASIS_METHOD),
                        repeatableOptionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_BASIS_ALLOCATION),
                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_BASIS_VALUATION_DATE),
                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_BASIS_MANUAL_OVERRIDE),
                        optionalEntryParameter(LedgerParameterType.EVENT_REFERENCE),
                        optionalEntryParameter(LedgerParameterType.EVENT_STAGE),
                        optionalEntryParameter(LedgerParameterType.RECORD_DATE),
                        optionalEntryParameter(LedgerParameterType.PAYMENT_DATE),
                        optionalEntryParameter(LedgerParameterType.EFFECTIVE_DATE),
                        optionalEntryParameter(LedgerParameterType.SETTLEMENT_DATE));
    }

    private static Set<LedgerParameterRule> corporateActionPostingParameterRulesFor(
                    Set<LedgerLegDefinition> legDefinitions)
    {
        var parameters = EnumSet.noneOf(LedgerParameterType.class);

        for (var legDefinition : legDefinitions)
        {
            parameters.addAll(legDefinition.getRequiredParameterTypes());
            parameters.addAll(legDefinition.getOptionalParameterTypes());
            parameters.addAll(corporateActionPostingOptionalParametersFor(legDefinition.getPostingType()));
        }

        var rules = new LinkedHashSet<LedgerParameterRule>();
        for (var parameter : parameters)
            rules.add(repeatableOptionalPostingParameter(parameter));

        return Collections.unmodifiableSet(rules);
    }

    private static EnumSet<LedgerParameterType> corporateActionPostingOptionalParametersFor(
                    LedgerPostingType postingType)
    {
        return switch (postingType)
        {
            case SECURITY -> SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                            LedgerParameterType.SOURCE_SECURITY, LedgerParameterType.TARGET_SECURITY,
                            LedgerParameterType.RATIO_NUMERATOR, LedgerParameterType.RATIO_DENOMINATOR,
                            LedgerParameterType.FRACTION_QUANTITY, LedgerParameterType.FRACTION_TREATMENT,
                            LedgerParameterType.ROUNDING_MODE, LedgerParameterType.COST_ALLOCATION_METHOD,
                            LedgerParameterType.SOURCE_COST_PERCENT, LedgerParameterType.TARGET_COST_PERCENT,
                            LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                            LedgerParameterType.VALUATION_PRICE, LedgerParameterType.MANUAL_VALUATION_OVERRIDE);
            case RIGHT -> SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                            LedgerParameterType.RIGHT_SECURITY, LedgerParameterType.SOURCE_SECURITY,
                            LedgerParameterType.SUBSCRIPTION_PRICE, LedgerParameterType.ELECTION_DEADLINE,
                            LedgerParameterType.FRACTION_QUANTITY, LedgerParameterType.FRACTION_TREATMENT,
                            LedgerParameterType.ROUNDING_MODE);
            case CASH -> SETS.parameterTypes(LedgerParameterType.SOURCE_ACCOUNT,
                            LedgerParameterType.TARGET_ACCOUNT, LedgerParameterType.CASH_ACCOUNT,
                            LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.PAYMENT_DATE,
                            LedgerParameterType.SETTLEMENT_DATE);
            case CASH_COMPENSATION -> cashCompensationOptionalParameters();
            case FEE -> feeOptionalParameters();
            case TAX -> taxOptionalParameters();
            case FOREX -> forexOptionalParameters();
            case ACCRUED_INTEREST -> SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                            LedgerParameterType.ACCRUED_INTEREST_AMOUNT, LedgerParameterType.COUPON_RATE,
                            LedgerParameterType.INTEREST_PERIOD_START, LedgerParameterType.INTEREST_PERIOD_END,
                            LedgerParameterType.PAYMENT_DATE, LedgerParameterType.SETTLEMENT_DATE,
                            LedgerParameterType.WITHHOLDING_TAX, LedgerParameterType.RECLAIMABLE_TAX,
                            LedgerParameterType.TAX_REASON);
            case PRINCIPAL_REDEMPTION -> SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                            LedgerParameterType.NOMINAL_VALUE, LedgerParameterType.PARTIAL_REDEMPTION_FACTOR,
                            LedgerParameterType.REDEMPTION_PRICE_PERCENT, LedgerParameterType.SOURCE_SECURITY,
                            LedgerParameterType.CASH_ACCOUNT, LedgerParameterType.PAYMENT_DATE,
                            LedgerParameterType.SETTLEMENT_DATE);
            default -> SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG);
        };
    }

    private static LedgerLegDefinition sourceSecurityLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.SOURCE_SECURITY_LEG, LedgerPostingType.SECURITY,
                        cardinality)
                        .requiredParameters(SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                        LedgerParameterType.SOURCE_SECURITY))
                        .optionalParameters(corporateActionPostingOptionalParametersFor(LedgerPostingType.SECURITY))
                        .build();
    }

    private static LedgerLegDefinition targetSecurityLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                        cardinality)
                        .requiredParameters(SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                        LedgerParameterType.TARGET_SECURITY))
                        .optionalParameters(corporateActionPostingOptionalParametersFor(LedgerPostingType.SECURITY))
                        .build();
    }

    private static LedgerLegDefinition securityContextLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.SECURITY_CONTEXT_LEG, LedgerPostingType.SECURITY,
                        cardinality)
                        .requiredParameters(SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG))
                        .optionalParameters(corporateActionPostingOptionalParametersFor(LedgerPostingType.SECURITY))
                        .build();
    }

    private static LedgerLegDefinition distributedRightLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.DISTRIBUTED_RIGHT_LEG, LedgerPostingType.RIGHT,
                        cardinality)
                        .requiredParameters(SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                        LedgerParameterType.RIGHT_SECURITY))
                        .optionalParameters(corporateActionPostingOptionalParametersFor(LedgerPostingType.RIGHT))
                        .build();
    }

    private static LedgerLegDefinition cashLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.CASH_LEG, LedgerPostingType.CASH, cardinality)
                        .optionalParameters(corporateActionPostingOptionalParametersFor(LedgerPostingType.CASH))
                        .build();
    }

    private static LedgerLegDefinition cashCompensationLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.CASH_COMPENSATION_LEG,
                        LedgerPostingType.CASH_COMPENSATION, LedgerLegCardinality.REPEATABLE)
                        .optionalParameters(cashCompensationOptionalParameters()).build();
    }

    private static LedgerLegDefinition accruedInterestLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.ACCRUED_INTEREST_LEG,
                        LedgerPostingType.ACCRUED_INTEREST, cardinality)
                        .optionalParameters(corporateActionPostingOptionalParametersFor(
                                        LedgerPostingType.ACCRUED_INTEREST))
                        .build();
    }

    private static LedgerLegDefinition principalRedemptionLeg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.PRINCIPAL_REDEMPTION_LEG,
                        LedgerPostingType.PRINCIPAL_REDEMPTION, cardinality)
                        .requiredParameters(SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG))
                        .optionalParameters(corporateActionPostingOptionalParametersFor(
                                        LedgerPostingType.PRINCIPAL_REDEMPTION))
                        .build();
    }

    private static LedgerLegDefinition feeLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.FEE_LEG, LedgerPostingType.FEE,
                        LedgerLegCardinality.REPEATABLE)
                        .optionalParameters(feeOptionalParameters()).build();
    }

    private static LedgerLegDefinition taxLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.TAX_LEG, LedgerPostingType.TAX,
                        LedgerLegCardinality.REPEATABLE)
                        .optionalParameters(taxOptionalParameters()).build();
    }

    private static LedgerLegDefinition forexLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.FOREX_CONTEXT_LEG, LedgerPostingType.FOREX,
                        LedgerLegCardinality.OPTIONAL)
                        .optionalParameters(forexOptionalParameters()).build();
    }

    private static LedgerPostingRule optionalPosting(LedgerPostingType postingType,
                    EnumSet<LedgerParameterType> requiredParameterTypes,
                    EnumSet<LedgerParameterType> optionalParameterTypes)
    {
        return LedgerPostingRule.optional(postingType, requiredParameterTypes, optionalParameterTypes);
    }

    private static LedgerParameterRule requiredEntryParameter(LedgerParameterType parameterType)
    {
        return LedgerParameterRule.required(parameterType);
    }

    private static LedgerParameterRule optionalEntryParameter(LedgerParameterType parameterType)
    {
        return LedgerParameterRule.optional(parameterType);
    }

    private static LedgerParameterRule repeatableRequiredPostingParameter(LedgerParameterType parameterType)
    {
        return LedgerParameterRule.repeatable(parameterType, LedgerRequirement.REQUIRED);
    }

    private static LedgerParameterRule repeatableOptionalEntryParameter(LedgerParameterType parameterType)
    {
        return LedgerParameterRule.repeatable(parameterType, LedgerRequirement.OPTIONAL);
    }

    private static LedgerParameterRule repeatableOptionalPostingParameter(LedgerParameterType parameterType)
    {
        return LedgerParameterRule.repeatable(parameterType, LedgerRequirement.OPTIONAL);
    }

    private static LedgerProjectionRule optionalProjection(LedgerProjectionRole role, boolean primaryPostingExpected,
                    boolean postingGroupExpected)
    {
        return LedgerProjectionRule.optional(role, primaryPostingExpected, postingGroupExpected);
    }

    private static LedgerRequirementGroup dateAlternative(String name)
    {
        return LedgerRequirementGroup.parameterTypes(name, LedgerRequirement.REQUIRED,
                        SETS.parameterTypes(LedgerParameterType.EX_DATE, LedgerParameterType.EFFECTIVE_DATE));
    }

    private static Set<LedgerRequirementGroup> primaryMovementGroup(String name, LedgerPrimaryMovement first,
                    LedgerPrimaryMovement... rest)
    {
        return SETS.alternativeGroups(LedgerRequirementGroup.primaryMovements(name, LedgerRequirement.REQUIRED,
                        SETS.primaryMovements(first, rest)));
    }

    private static LedgerComponentRequirement interestComponentRequirement(String name, LedgerLegRole primaryLegRole)
    {
        return LedgerComponentRequirement.sameGroupKey(name, primaryLegRole, LedgerLegRole.ACCRUED_INTEREST_LEG);
    }

    private static EnumSet<LedgerParameterType> requiredSecurityLegParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                        LedgerParameterType.SOURCE_SECURITY, LedgerParameterType.TARGET_SECURITY,
                        LedgerParameterType.RATIO_NUMERATOR, LedgerParameterType.RATIO_DENOMINATOR);
    }

    private static Set<LedgerLegDefinition> spinOffLegDefinitions()
    {
        return SETS.legDefinitions(
                        LedgerLegDefinition.of(LedgerLegRole.SOURCE_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.REPEATABLE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.SOURCE_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(spinOffSourceSecurityLegOptionalParameters())
                                        .projection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.AT_LEAST_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.TARGET_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(spinOffTargetSecurityLegOptionalParameters())
                                        .projection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.SECURITY_CONTEXT_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.AT_LEAST_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG))
                                        .optionalParameters(spinOffSecurityOptionalParameters()).build(),
                        LedgerLegDefinition.of(LedgerLegRole.CASH_COMPENSATION_LEG,
                                        LedgerPostingType.CASH_COMPENSATION, LedgerLegCardinality.REPEATABLE)
                                        .optionalParameters(cashCompensationOptionalParameters())
                                        .projection(LedgerProjectionRole.CASH_COMPENSATION, true, true)
                                        .group(CASH_COMPENSATION_GROUP).build(),
                        LedgerLegDefinition.of(LedgerLegRole.FEE_LEG, LedgerPostingType.FEE,
                                        LedgerLegCardinality.REPEATABLE)
                                        .optionalParameters(feeOptionalParameters())
                                        .group(CASH_COMPENSATION_GROUP).build(),
                        LedgerLegDefinition.of(LedgerLegRole.TAX_LEG, LedgerPostingType.TAX,
                                        LedgerLegCardinality.REPEATABLE)
                                        .optionalParameters(taxOptionalParameters())
                                        .group(CASH_COMPENSATION_GROUP).build(),
                        LedgerLegDefinition.of(LedgerLegRole.FOREX_CONTEXT_LEG, LedgerPostingType.FOREX,
                                        LedgerLegCardinality.OPTIONAL)
                                        .optionalParameters(forexOptionalParameters()).build());
    }

    private static EnumSet<LedgerParameterType> spinOffSourceSecurityLegOptionalParameters()
    {
        var parameters = spinOffSecurityOptionalParameters();
        parameters.add(LedgerParameterType.TARGET_SECURITY);
        return parameters;
    }

    private static EnumSet<LedgerParameterType> spinOffTargetSecurityLegOptionalParameters()
    {
        var parameters = spinOffSecurityOptionalParameters();
        parameters.add(LedgerParameterType.SOURCE_SECURITY);
        return parameters;
    }

    private static EnumSet<LedgerParameterType> spinOffSecurityOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.FRACTION_QUANTITY,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.ROUNDING_MODE,
                        LedgerParameterType.COST_ALLOCATION_METHOD, LedgerParameterType.SOURCE_COST_PERCENT,
                        LedgerParameterType.TARGET_COST_PERCENT, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.FAIR_MARKET_VALUE, LedgerParameterType.VALUATION_PRICE,
                        LedgerParameterType.MANUAL_VALUATION_OVERRIDE);
    }

    private static EnumSet<LedgerParameterType> cashCompensationOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.CASH_ACCOUNT,
                        LedgerParameterType.CASH_COMPENSATION_KIND, LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                        LedgerParameterType.CASH_IN_LIEU_APPLIED, LedgerParameterType.FRACTION_QUANTITY,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.ROUNDING_MODE,
                        LedgerParameterType.PAYMENT_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.FEE_REASON, LedgerParameterType.TAX_REASON,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static EnumSet<LedgerParameterType> feeOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.FEE_REASON, LedgerParameterType.STAMP_DUTY,
                        LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.PAYMENT_DATE,
                        LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static EnumSet<LedgerParameterType> taxOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.TAX_REASON,
                        LedgerParameterType.TAXABLE_DISTRIBUTION, LedgerParameterType.WITHHOLDING_TAX,
                        LedgerParameterType.TRANSACTION_TAX, LedgerParameterType.STAMP_DUTY,
                        LedgerParameterType.RECLAIMABLE_TAX, LedgerParameterType.EVENT_REFERENCE,
                        LedgerParameterType.PAYMENT_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static EnumSet<LedgerParameterType> forexOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.EVENT_REFERENCE);
    }

    private static Set<LedgerPostingGroupRule> cashCompensationPostingGroupRules()
    {
        return SETS.postingGroupRules(LedgerPostingGroupRule.of(CASH_COMPENSATION_GROUP,
                        LedgerRequirement.OPTIONAL,
                        SETS.postingTypes(LedgerPostingType.CASH_COMPENSATION, LedgerPostingType.FEE,
                                        LedgerPostingType.TAX),
                        SETS.projectionRoles(LedgerProjectionRole.CASH_COMPENSATION), true));
    }

    private static EnumSet<LedgerDownstreamResult> downstreamResults()
    {
        return EnumSet.allOf(LedgerDownstreamResult.class);
    }

    private static final class SetBuilder
    {
        private EnumSet<LedgerPostingType> postingTypes(LedgerPostingType first, LedgerPostingType... rest)
        {
            return EnumSet.of(first, rest);
        }

        private EnumSet<LedgerParameterType> parameterTypes(LedgerParameterType... values)
        {
            var set = EnumSet.noneOf(LedgerParameterType.class);

            for (var value : values)
                set.add(value);

            return set;
        }

        private EnumSet<LedgerProjectionRole> projectionRoles(LedgerProjectionRole first, LedgerProjectionRole... rest)
        {
            return EnumSet.of(first, rest);
        }

        private EnumSet<LedgerPrimaryMovement> primaryMovements(LedgerPrimaryMovement first,
                        LedgerPrimaryMovement... rest)
        {
            return EnumSet.of(first, rest);
        }

        private Set<LedgerPostingRule> postingRules(LedgerPostingRule first, LedgerPostingRule... rest)
        {
            return setOf(first, rest);
        }

        private Set<LedgerParameterRule> parameterRules(LedgerParameterRule first, LedgerParameterRule... rest)
        {
            return setOf(first, rest);
        }

        private Set<LedgerProjectionRule> projectionRules(LedgerProjectionRule first, LedgerProjectionRule... rest)
        {
            return setOf(first, rest);
        }

        private Set<LedgerPostingGroupRule> postingGroupRules(LedgerPostingGroupRule first,
                        LedgerPostingGroupRule... rest)
        {
            return setOf(first, rest);
        }

        private Set<LedgerRequirementGroup> alternativeGroups(LedgerRequirementGroup first,
                        LedgerRequirementGroup... rest)
        {
            return setOf(first, rest);
        }

        private Set<LedgerComponentRequirement> componentRequirements(LedgerComponentRequirement first,
                        LedgerComponentRequirement... rest)
        {
            return setOf(first, rest);
        }

        private Set<LedgerLegDefinition> legDefinitions(LedgerLegDefinition first, LedgerLegDefinition... rest)
        {
            return setOf(first, rest);
        }

        @SafeVarargs
        private final <T> Set<T> setOf(T first, T... rest)
        {
            var set = new LinkedHashSet<T>();
            set.add(first);

            for (var value : rest)
                set.add(value);

            return Collections.unmodifiableSet(set);
        }
    }
}
