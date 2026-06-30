package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerParameterRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingGroupRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingRule;
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
 * store the resulting Ledger entries, postings, parameters, and projection refs.
 * </p>
 */
public final class LedgerEntryDefinitionRegistry
{
    private static final String CASH_COMPENSATION_GROUP = "CASH_COMPENSATION_GROUP"; //$NON-NLS-1$

    private static final SetBuilder SETS = new SetBuilder();

    private static final Map<LedgerEntryType, LedgerEntryDefinition> DEFINITIONS = definitions();

    private LedgerEntryDefinitionRegistry()
    {
    }

    public static Optional<LedgerEntryDefinition> lookup(LedgerEntryType entryType)
    {
        return Optional.ofNullable(DEFINITIONS.get(entryType));
    }

    public static Collection<LedgerEntryDefinition> getDefinitions()
    {
        return DEFINITIONS.values();
    }

    public static boolean hasDefinition(LedgerEntryType entryType)
    {
        return DEFINITIONS.containsKey(entryType);
    }

    private static Map<LedgerEntryType, LedgerEntryDefinition> definitions()
    {
        var definitions = new EnumMap<LedgerEntryType, LedgerEntryDefinition>(LedgerEntryType.class);

        register(definitions, spinOff());
        register(definitions, stockDividend());
        register(definitions, bonusIssue());
        register(definitions, rightsDistribution());
        register(definitions, bondConversion());

        return Collections.unmodifiableMap(definitions);
    }

    private static void register(Map<LedgerEntryType, LedgerEntryDefinition> definitions,
                    LedgerEntryDefinition definition)
    {
        if (definitions.put(definition.getEntryType(), definition) != null)
            throw new IllegalStateException(LedgerDiagnosticCode.LEDGER_CORE_016
                            .message("Duplicate Ledger entry definition: " + definition.getEntryType())); //$NON-NLS-1$
    }

    private static LedgerEntryDefinition spinOff()
    {
        return LedgerEntryDefinition.of(LedgerEntryType.SPIN_OFF, LedgerNativeEntryShape.DUAL_INSTRUMENT_PLUS_ACCOUNT,
                        SETS.postingRules(
                                        requiredPosting(LedgerPostingType.SECURITY, requiredSecurityLegParameters(),
                                                        spinOffSecurityOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH_COMPENSATION, SETS.parameterTypes(),
                                                        cashCompensationOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FEE, SETS.parameterTypes(),
                                                        feeOptionalParameters()),
                                        optionalPosting(LedgerPostingType.TAX, SETS.parameterTypes(),
                                                        taxOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FOREX, SETS.parameterTypes(),
                                                        forexOptionalParameters())),
                        SETS.parameterRules(requiredEntryParameter(LedgerParameterType.CORPORATE_ACTION_KIND),
                                        optionalEntryParameter(LedgerParameterType.EX_DATE),
                                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_REFERENCE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_STAGE),
                                        optionalEntryParameter(LedgerParameterType.RECORD_DATE),
                                        optionalEntryParameter(LedgerParameterType.PAYMENT_DATE),
                                        optionalEntryParameter(LedgerParameterType.EFFECTIVE_DATE),
                                        optionalEntryParameter(LedgerParameterType.SETTLEMENT_DATE)),
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
                                        requiredProjection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false),
                                        requiredProjection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false),
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
        return LedgerEntryDefinition.of(LedgerEntryType.STOCK_DIVIDEND, LedgerNativeEntryShape.SINGLE_INSTRUMENT,
                        SETS.postingRules(
                                        requiredPosting(LedgerPostingType.SECURITY,
                                                        SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                                                        LedgerParameterType.TARGET_SECURITY,
                                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                                        LedgerParameterType.RATIO_DENOMINATOR),
                                                        stockDividendSecurityOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH_COMPENSATION, SETS.parameterTypes(),
                                                        cashCompensationOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FEE, SETS.parameterTypes(),
                                                        feeOptionalParameters()),
                                        optionalPosting(LedgerPostingType.TAX, SETS.parameterTypes(),
                                                        taxOptionalParameters())),
                        SETS.parameterRules(requiredEntryParameter(LedgerParameterType.CORPORATE_ACTION_KIND),
                                        optionalEntryParameter(LedgerParameterType.EX_DATE),
                                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_REFERENCE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_STAGE),
                                        optionalEntryParameter(LedgerParameterType.SOURCE_SECURITY),
                                        optionalEntryParameter(LedgerParameterType.RECORD_DATE),
                                        optionalEntryParameter(LedgerParameterType.PAYMENT_DATE),
                                        optionalEntryParameter(LedgerParameterType.EFFECTIVE_DATE),
                                        optionalEntryParameter(LedgerParameterType.SETTLEMENT_DATE)),
                        SETS.parameterRules(repeatableRequiredPostingParameter(LedgerParameterType.CORPORATE_ACTION_LEG),
                                        repeatableRequiredPostingParameter(LedgerParameterType.TARGET_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_NUMERATOR),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_DENOMINATOR),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_QUANTITY),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_TREATMENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CASH_IN_LIEU_AMOUNT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CASH_IN_LIEU_APPLIED),
                                        repeatableOptionalPostingParameter(LedgerParameterType.COST_ALLOCATION_METHOD),
                                        repeatableOptionalPostingParameter(LedgerParameterType.REFERENCE_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FAIR_MARKET_VALUE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.VALUATION_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAXABLE_DISTRIBUTION),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FEE_REASON),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAX_REASON)),
                        SETS.projectionRules(
                                        requiredProjection(LedgerProjectionRole.DELIVERY_INBOUND, true, false),
                                        optionalProjection(LedgerProjectionRole.CASH_COMPENSATION, true, true)),
                        cashCompensationPostingGroupRules(),
                        SETS.alternativeGroups(dateAlternative("STOCK_DIVIDEND_DATE")), //$NON-NLS-1$
                        stockDividendLegDefinitions(),
                        LedgerReportingClass.SECURITIES_DISTRIBUTION,
                        LedgerPerformanceTreatment.SECURITY_DISTRIBUTION, downstreamResults());
    }

    private static LedgerEntryDefinition bonusIssue()
    {
        return LedgerEntryDefinition.of(LedgerEntryType.BONUS_ISSUE, LedgerNativeEntryShape.SINGLE_INSTRUMENT,
                        SETS.postingRules(
                                        requiredPosting(LedgerPostingType.SECURITY,
                                                        SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                                                        LedgerParameterType.TARGET_SECURITY,
                                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                                        LedgerParameterType.RATIO_DENOMINATOR),
                                                        bonusIssueSecurityOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH_COMPENSATION, SETS.parameterTypes(),
                                                        cashCompensationOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FEE, SETS.parameterTypes(),
                                                        feeOptionalParameters()),
                                        optionalPosting(LedgerPostingType.TAX, SETS.parameterTypes(),
                                                        taxOptionalParameters())),
                        SETS.parameterRules(requiredEntryParameter(LedgerParameterType.CORPORATE_ACTION_KIND),
                                        optionalEntryParameter(LedgerParameterType.EX_DATE),
                                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_REFERENCE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_STAGE),
                                        optionalEntryParameter(LedgerParameterType.SOURCE_SECURITY),
                                        optionalEntryParameter(LedgerParameterType.RECORD_DATE),
                                        optionalEntryParameter(LedgerParameterType.PAYMENT_DATE),
                                        optionalEntryParameter(LedgerParameterType.EFFECTIVE_DATE),
                                        optionalEntryParameter(LedgerParameterType.SETTLEMENT_DATE)),
                        SETS.parameterRules(repeatableRequiredPostingParameter(LedgerParameterType.CORPORATE_ACTION_LEG),
                                        repeatableRequiredPostingParameter(LedgerParameterType.TARGET_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_NUMERATOR),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_DENOMINATOR),
                                        repeatableOptionalPostingParameter(LedgerParameterType.SAME_SECURITY_AS_SOURCE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_TREATMENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.ROUNDING_MODE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.COST_ALLOCATION_METHOD),
                                        repeatableOptionalPostingParameter(LedgerParameterType.REFERENCE_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FAIR_MARKET_VALUE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.VALUATION_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FEE_REASON),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAX_REASON)),
                        SETS.projectionRules(
                                        requiredProjection(LedgerProjectionRole.DELIVERY_INBOUND, true, false),
                                        optionalProjection(LedgerProjectionRole.CASH_COMPENSATION, true, true)),
                        cashCompensationPostingGroupRules(),
                        SETS.alternativeGroups(dateAlternative("BONUS_ISSUE_DATE")), //$NON-NLS-1$
                        bonusIssueLegDefinitions(),
                        LedgerReportingClass.SECURITIES_DISTRIBUTION,
                        LedgerPerformanceTreatment.PERFORMANCE_NEUTRAL, downstreamResults());
    }

    private static LedgerEntryDefinition rightsDistribution()
    {
        return LedgerEntryDefinition.of(LedgerEntryType.RIGHTS_DISTRIBUTION,
                        LedgerNativeEntryShape.DUAL_INSTRUMENT_PLUS_ACCOUNT,
                        SETS.postingRules(
                                        optionalPosting(LedgerPostingType.RIGHT,
                                                        SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                                                        LedgerParameterType.SOURCE_SECURITY,
                                                                        LedgerParameterType.RIGHT_SECURITY,
                                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                                        LedgerParameterType.RATIO_DENOMINATOR),
                                                        rightsOptionalParameters()),
                                        optionalPosting(LedgerPostingType.SECURITY,
                                                        SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG),
                                                        rightsOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH_COMPENSATION, SETS.parameterTypes(),
                                                        cashCompensationOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FEE, SETS.parameterTypes(),
                                                        feeOptionalParameters()),
                                        optionalPosting(LedgerPostingType.TAX, SETS.parameterTypes(),
                                                        taxOptionalParameters())),
                        SETS.parameterRules(requiredEntryParameter(LedgerParameterType.CORPORATE_ACTION_KIND),
                                        optionalEntryParameter(LedgerParameterType.EX_DATE),
                                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_REFERENCE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_STAGE),
                                        optionalEntryParameter(LedgerParameterType.RECORD_DATE),
                                        optionalEntryParameter(LedgerParameterType.PAYMENT_DATE),
                                        optionalEntryParameter(LedgerParameterType.EFFECTIVE_DATE),
                                        optionalEntryParameter(LedgerParameterType.SETTLEMENT_DATE),
                                        optionalEntryParameter(LedgerParameterType.ELECTION_DEADLINE)),
                        SETS.parameterRules(repeatableRequiredPostingParameter(LedgerParameterType.CORPORATE_ACTION_LEG),
                                        repeatableRequiredPostingParameter(LedgerParameterType.SOURCE_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RIGHT_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_NUMERATOR),
                                        repeatableRequiredPostingParameter(LedgerParameterType.RATIO_DENOMINATOR),
                                        repeatableOptionalPostingParameter(LedgerParameterType.SUBSCRIPTION_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.REFERENCE_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_QUANTITY),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FRACTION_TREATMENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.ROUNDING_MODE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FEE_REASON),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAX_REASON)),
                        SETS.projectionRules(
                                        requiredProjection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false),
                                        optionalProjection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false),
                                        optionalProjection(LedgerProjectionRole.CASH_COMPENSATION, true, true)),
                        cashCompensationPostingGroupRules(),
                        SETS.alternativeGroups(dateAlternative("RIGHTS_DISTRIBUTION_DATE"), //$NON-NLS-1$
                                        LedgerRequirementGroup.postingTypes("RIGHTS_DISTRIBUTED_INSTRUMENT", //$NON-NLS-1$
                                                        LedgerRequirement.REQUIRED,
                                                        SETS.postingTypes(LedgerPostingType.RIGHT,
                                                                        LedgerPostingType.SECURITY))),
                        rightsDistributionLegDefinitions(),
                        LedgerReportingClass.RIGHTS_EVENT, LedgerPerformanceTreatment.PERFORMANCE_NEUTRAL,
                        downstreamResults());
    }

    private static LedgerEntryDefinition bondConversion()
    {
        return LedgerEntryDefinition.of(LedgerEntryType.BOND_CONVERSION,
                        LedgerNativeEntryShape.DUAL_INSTRUMENT_PLUS_ACCOUNT,
                        SETS.postingRules(
                                        requiredPosting(LedgerPostingType.BOND,
                                                        SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                                                        LedgerParameterType.SOURCE_SECURITY,
                                                                        LedgerParameterType.NOMINAL_VALUE,
                                                                        LedgerParameterType.QUOTATION_STYLE),
                                                        bondOptionalParameters()),
                                        requiredPosting(LedgerPostingType.SECURITY,
                                                        SETS.parameterTypes(LedgerParameterType.CORPORATE_ACTION_LEG,
                                                                        LedgerParameterType.TARGET_SECURITY),
                                                        bondOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH, SETS.parameterTypes(),
                                                        cashOptionalParameters()),
                                        optionalPosting(LedgerPostingType.CASH_COMPENSATION, SETS.parameterTypes(),
                                                        cashCompensationOptionalParameters()),
                                        optionalPosting(LedgerPostingType.ACCRUED_INTEREST, SETS.parameterTypes(),
                                                        accruedInterestOptionalParameters()),
                                        optionalPosting(LedgerPostingType.FEE, SETS.parameterTypes(),
                                                        feeOptionalParameters()),
                                        optionalPosting(LedgerPostingType.TAX, SETS.parameterTypes(),
                                                        taxOptionalParameters())),
                        SETS.parameterRules(requiredEntryParameter(LedgerParameterType.CORPORATE_ACTION_KIND),
                                        optionalEntryParameter(LedgerParameterType.EFFECTIVE_DATE),
                                        optionalEntryParameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_REFERENCE),
                                        optionalEntryParameter(LedgerParameterType.EVENT_STAGE),
                                        optionalEntryParameter(LedgerParameterType.SETTLEMENT_DATE)),
                        SETS.parameterRules(repeatableRequiredPostingParameter(LedgerParameterType.CORPORATE_ACTION_LEG),
                                        repeatableRequiredPostingParameter(LedgerParameterType.SOURCE_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.TARGET_SECURITY),
                                        repeatableRequiredPostingParameter(LedgerParameterType.NOMINAL_VALUE),
                                        repeatableRequiredPostingParameter(LedgerParameterType.QUOTATION_STYLE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CONVERSION_RATIO),
                                        repeatableOptionalPostingParameter(LedgerParameterType.RATIO_NUMERATOR),
                                        repeatableOptionalPostingParameter(LedgerParameterType.RATIO_DENOMINATOR),
                                        repeatableOptionalPostingParameter(LedgerParameterType.REFERENCE_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FAIR_MARKET_VALUE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.VALUATION_PRICE),
                                        repeatableOptionalPostingParameter(LedgerParameterType.ACCRUED_INTEREST_AMOUNT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.INTEREST_PERIOD_START),
                                        repeatableOptionalPostingParameter(LedgerParameterType.INTEREST_PERIOD_END),
                                        repeatableOptionalPostingParameter(LedgerParameterType.CASH_IN_LIEU_AMOUNT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.COST_ALLOCATION_METHOD),
                                        repeatableOptionalPostingParameter(LedgerParameterType.SOURCE_COST_PERCENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TARGET_COST_PERCENT),
                                        repeatableOptionalPostingParameter(LedgerParameterType.FEE_REASON),
                                        repeatableOptionalPostingParameter(LedgerParameterType.TAX_REASON)),
                        SETS.projectionRules(
                                        requiredProjection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false),
                                        requiredProjection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false),
                                        optionalProjection(LedgerProjectionRole.CASH_COMPENSATION, true, true)),
                        cashCompensationPostingGroupRules(),
                        SETS.alternativeGroups(
                                        LedgerRequirementGroup.parameterTypes("BOND_CONVERSION_RATIO", //$NON-NLS-1$
                                                        LedgerRequirement.REQUIRED,
                                                        SETS.parameterTypes(LedgerParameterType.CONVERSION_RATIO,
                                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                                        LedgerParameterType.RATIO_DENOMINATOR)),
                                        LedgerRequirementGroup.parameterTypes("BOND_CONVERSION_DATE", //$NON-NLS-1$
                                                        LedgerRequirement.REQUIRED,
                                                        SETS.parameterTypes(LedgerParameterType.EFFECTIVE_DATE,
                                                                        LedgerParameterType.SETTLEMENT_DATE))),
                        bondConversionLegDefinitions(),
                        LedgerReportingClass.SECURITY_REORGANIZATION,
                        LedgerPerformanceTreatment.INTERNAL_RECLASSIFICATION, downstreamResults());
    }

    private static LedgerPostingRule requiredPosting(LedgerPostingType postingType,
                    EnumSet<LedgerParameterType> requiredParameterTypes,
                    EnumSet<LedgerParameterType> optionalParameterTypes)
    {
        return LedgerPostingRule.required(postingType, requiredParameterTypes, optionalParameterTypes);
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

    private static LedgerParameterRule repeatableOptionalPostingParameter(LedgerParameterType parameterType)
    {
        return LedgerParameterRule.repeatable(parameterType, LedgerRequirement.OPTIONAL);
    }

    private static LedgerProjectionRule requiredProjection(LedgerProjectionRole role, boolean primaryPostingExpected,
                    boolean postingGroupExpected)
    {
        return LedgerProjectionRule.required(role, primaryPostingExpected, postingGroupExpected);
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
                                        LedgerLegCardinality.EXACTLY_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.SOURCE_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(spinOffSourceSecurityLegOptionalParameters())
                                        .projection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.EXACTLY_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.TARGET_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(spinOffTargetSecurityLegOptionalParameters())
                                        .projection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.CASH_COMPENSATION_LEG,
                                        LedgerPostingType.CASH_COMPENSATION, LedgerLegCardinality.OPTIONAL)
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

    private static Set<LedgerLegDefinition> stockDividendLegDefinitions()
    {
        return SETS.legDefinitions(
                        LedgerLegDefinition.of(LedgerLegRole.RECEIVED_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.EXACTLY_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.TARGET_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(stockDividendSecurityOptionalParameters())
                                        .projection(LedgerProjectionRole.DELIVERY_INBOUND, true, false).build(),
                        cashCompensationLeg(),
                        feeLeg(),
                        taxLeg());
    }

    private static Set<LedgerLegDefinition> bonusIssueLegDefinitions()
    {
        return SETS.legDefinitions(
                        LedgerLegDefinition.of(LedgerLegRole.RECEIVED_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.EXACTLY_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.TARGET_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(bonusIssueSecurityOptionalParameters())
                                        .projection(LedgerProjectionRole.DELIVERY_INBOUND, true, false).build(),
                        cashCompensationLeg(),
                        feeLeg(),
                        taxLeg());
    }

    private static Set<LedgerLegDefinition> rightsDistributionLegDefinitions()
    {
        return SETS.legDefinitions(
                        LedgerLegDefinition.of(LedgerLegRole.DISTRIBUTED_RIGHT_LEG, LedgerPostingType.RIGHT,
                                        LedgerLegCardinality.OPTIONAL)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.SOURCE_SECURITY,
                                                        LedgerParameterType.RIGHT_SECURITY,
                                                        LedgerParameterType.RATIO_NUMERATOR,
                                                        LedgerParameterType.RATIO_DENOMINATOR))
                                        .optionalParameters(rightsOptionalParameters())
                                        .projection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.DISTRIBUTED_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.OPTIONAL)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG))
                                        .optionalParameters(rightsOptionalParameters())
                                        .projection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.SOURCE_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.OPTIONAL)
                                        .optionalParameters(rightsOptionalParameters())
                                        .projection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false).build(),
                        cashCompensationLeg(),
                        feeLeg(),
                        taxLeg());
    }

    private static Set<LedgerLegDefinition> bondConversionLegDefinitions()
    {
        return SETS.legDefinitions(
                        LedgerLegDefinition.of(LedgerLegRole.SOURCE_BOND_LEG, LedgerPostingType.BOND,
                                        LedgerLegCardinality.EXACTLY_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.SOURCE_SECURITY,
                                                        LedgerParameterType.NOMINAL_VALUE,
                                                        LedgerParameterType.QUOTATION_STYLE))
                                        .optionalParameters(bondOptionalParameters())
                                        .projection(LedgerProjectionRole.OLD_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                                        LedgerLegCardinality.EXACTLY_ONE)
                                        .requiredParameters(SETS.parameterTypes(
                                                        LedgerParameterType.CORPORATE_ACTION_LEG,
                                                        LedgerParameterType.TARGET_SECURITY))
                                        .optionalParameters(bondOptionalParameters())
                                        .projection(LedgerProjectionRole.NEW_SECURITY_LEG, true, false).build(),
                        LedgerLegDefinition.of(LedgerLegRole.CASH_LEG, LedgerPostingType.CASH,
                                        LedgerLegCardinality.OPTIONAL)
                                        .optionalParameters(cashOptionalParameters()).build(),
                        cashCompensationLeg(),
                        LedgerLegDefinition.of(LedgerLegRole.ACCRUED_INTEREST_LEG,
                                        LedgerPostingType.ACCRUED_INTEREST, LedgerLegCardinality.OPTIONAL)
                                        .optionalParameters(accruedInterestOptionalParameters()).build(),
                        feeLeg(),
                        taxLeg());
    }

    private static LedgerLegDefinition cashCompensationLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.CASH_COMPENSATION_LEG,
                        LedgerPostingType.CASH_COMPENSATION, LedgerLegCardinality.OPTIONAL)
                        .optionalParameters(cashCompensationOptionalParameters())
                        .projection(LedgerProjectionRole.CASH_COMPENSATION, true, true)
                        .group(CASH_COMPENSATION_GROUP).build();
    }

    private static LedgerLegDefinition feeLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.FEE_LEG, LedgerPostingType.FEE, LedgerLegCardinality.REPEATABLE)
                        .optionalParameters(feeOptionalParameters())
                        .group(CASH_COMPENSATION_GROUP).build();
    }

    private static LedgerLegDefinition taxLeg()
    {
        return LedgerLegDefinition.of(LedgerLegRole.TAX_LEG, LedgerPostingType.TAX, LedgerLegCardinality.REPEATABLE)
                        .optionalParameters(taxOptionalParameters())
                        .group(CASH_COMPENSATION_GROUP).build();
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

    private static EnumSet<LedgerParameterType> stockDividendSecurityOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.SOURCE_SECURITY, LedgerParameterType.FRACTION_QUANTITY,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                        LedgerParameterType.CASH_IN_LIEU_APPLIED, LedgerParameterType.COST_ALLOCATION_METHOD,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.TAXABLE_DISTRIBUTION);
    }

    private static EnumSet<LedgerParameterType> bonusIssueSecurityOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.SOURCE_SECURITY,
                        LedgerParameterType.SAME_SECURITY_AS_SOURCE, LedgerParameterType.FRACTION_TREATMENT,
                        LedgerParameterType.ROUNDING_MODE, LedgerParameterType.COST_ALLOCATION_METHOD,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                        LedgerParameterType.VALUATION_PRICE);
    }

    private static EnumSet<LedgerParameterType> rightsOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.SUBSCRIPTION_PRICE, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.FRACTION_QUANTITY, LedgerParameterType.FRACTION_TREATMENT,
                        LedgerParameterType.ROUNDING_MODE, LedgerParameterType.ELECTION_DEADLINE,
                        LedgerParameterType.EX_DATE, LedgerParameterType.RECORD_DATE,
                        LedgerParameterType.EFFECTIVE_DATE, LedgerParameterType.SETTLEMENT_DATE);
    }

    private static EnumSet<LedgerParameterType> bondOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.CONVERSION_RATIO, LedgerParameterType.RATIO_NUMERATOR,
                        LedgerParameterType.RATIO_DENOMINATOR, LedgerParameterType.REDEMPTION_PRICE_PERCENT,
                        LedgerParameterType.PARTIAL_REDEMPTION_FACTOR, LedgerParameterType.COUPON_RATE,
                        LedgerParameterType.INTEREST_PERIOD_START, LedgerParameterType.INTEREST_PERIOD_END,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.VALUATION_PRICE,
                        LedgerParameterType.FAIR_MARKET_VALUE, LedgerParameterType.MANUAL_VALUATION_OVERRIDE,
                        LedgerParameterType.EFFECTIVE_DATE, LedgerParameterType.SETTLEMENT_DATE);
    }

    private static EnumSet<LedgerParameterType> cashOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.SOURCE_ACCOUNT, LedgerParameterType.TARGET_ACCOUNT,
                        LedgerParameterType.CASH_ACCOUNT, LedgerParameterType.EVENT_REFERENCE,
                        LedgerParameterType.PAYMENT_DATE, LedgerParameterType.SETTLEMENT_DATE);
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

    private static EnumSet<LedgerParameterType> accruedInterestOptionalParameters()
    {
        return SETS.parameterTypes(LedgerParameterType.ACCRUED_INTEREST_AMOUNT,
                        LedgerParameterType.COUPON_RATE, LedgerParameterType.INTEREST_PERIOD_START,
                        LedgerParameterType.INTEREST_PERIOD_END, LedgerParameterType.PAYMENT_DATE,
                        LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.WITHHOLDING_TAX,
                        LedgerParameterType.RECLAIMABLE_TAX, LedgerParameterType.TAX_REASON,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
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
