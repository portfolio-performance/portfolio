package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Registers Ledger configuration definitions used by validation and assembly.
 * This is configuration infrastructure. Normal transaction-editing code should not update
 * the registry directly.
 *
 * <p>
 * The registry is static Java configuration. It is not persisted in XML or Protobuf; files
 * store postings with stable posting type codes and concrete parameter values.
 * </p>
 */
public final class LedgerPostingTypeDefinitionRegistry
{
    private static final SetBuilder SETS = new SetBuilder();

    private static final Map<LedgerPostingType, LedgerPostingTypeDefinition> DEFINITIONS = definitions();

    private LedgerPostingTypeDefinitionRegistry()
    {
    }

    public static Optional<LedgerPostingTypeDefinition> lookup(LedgerPostingType postingType)
    {
        return Optional.ofNullable(DEFINITIONS.get(postingType));
    }

    public static Collection<LedgerPostingTypeDefinition> getDefinitions()
    {
        return DEFINITIONS.values();
    }

    public static boolean hasDefinition(LedgerPostingType postingType)
    {
        return DEFINITIONS.containsKey(postingType);
    }

    private static Map<LedgerPostingType, LedgerPostingTypeDefinition> definitions()
    {
        var definitions = new EnumMap<LedgerPostingType, LedgerPostingTypeDefinition>(LedgerPostingType.class);

        register(definitions, cash());
        register(definitions, security());
        register(definitions, cashCompensation());
        register(definitions, fee());
        register(definitions, tax());
        register(definitions, grossValue());
        register(definitions, forex());
        register(definitions, right());
        register(definitions, bond());
        register(definitions, accruedInterest());
        register(definitions, principalRedemption());

        return Collections.unmodifiableMap(definitions);
    }

    private static void register(Map<LedgerPostingType, LedgerPostingTypeDefinition> definitions,
                    LedgerPostingTypeDefinition definition)
    {
        if (definitions.put(definition.getPostingType(), definition) != null)
            throw new IllegalStateException(LedgerDiagnosticCode.LEDGER_CORE_023
                            .message("Duplicate Ledger posting type definition: " + definition.getPostingType())); //$NON-NLS-1$
    }

    private static LedgerPostingTypeDefinition cash()
    {
        return definition(LedgerPostingType.CASH, LedgerParameterType.SOURCE_ACCOUNT,
                        LedgerParameterType.TARGET_ACCOUNT, LedgerParameterType.CASH_ACCOUNT,
                        LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.PAYMENT_DATE,
                        LedgerParameterType.SETTLEMENT_DATE);
    }

    private static LedgerPostingTypeDefinition security()
    {
        return definition(LedgerPostingType.SECURITY, LedgerParameterType.SOURCE_SECURITY,
                        LedgerParameterType.TARGET_SECURITY, LedgerParameterType.RIGHT_SECURITY,
                        LedgerParameterType.SAME_SECURITY_AS_SOURCE, LedgerParameterType.RATIO_NUMERATOR,
                        LedgerParameterType.RATIO_DENOMINATOR, LedgerParameterType.FRACTION_QUANTITY,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.ROUNDING_MODE,
                        LedgerParameterType.COST_ALLOCATION_METHOD, LedgerParameterType.SOURCE_COST_PERCENT,
                        LedgerParameterType.TARGET_COST_PERCENT, LedgerParameterType.AFFECTED_SOURCE_QUANTITY,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.MANUAL_VALUATION_OVERRIDE,
                        LedgerParameterType.EX_DATE, LedgerParameterType.RECORD_DATE,
                        LedgerParameterType.EFFECTIVE_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition cashCompensation()
    {
        return definition(LedgerPostingType.CASH_COMPENSATION, LedgerParameterType.CASH_ACCOUNT,
                        LedgerParameterType.CASH_COMPENSATION_KIND, LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                        LedgerParameterType.CASH_IN_LIEU_APPLIED, LedgerParameterType.FRACTION_QUANTITY,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.ROUNDING_MODE,
                        LedgerParameterType.PAYMENT_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.FEE_REASON, LedgerParameterType.TAX_REASON,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition fee()
    {
        return definition(LedgerPostingType.FEE, LedgerParameterType.FEE_REASON, LedgerParameterType.STAMP_DUTY,
                        LedgerParameterType.EVENT_REFERENCE, LedgerParameterType.PAYMENT_DATE,
                        LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition tax()
    {
        return definition(LedgerPostingType.TAX, LedgerParameterType.TAX_REASON,
                        LedgerParameterType.TAXABLE_DISTRIBUTION, LedgerParameterType.WITHHOLDING_TAX,
                        LedgerParameterType.TRANSACTION_TAX, LedgerParameterType.STAMP_DUTY,
                        LedgerParameterType.RECLAIMABLE_TAX, LedgerParameterType.EVENT_REFERENCE,
                        LedgerParameterType.PAYMENT_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition grossValue()
    {
        return definition(LedgerPostingType.GROSS_VALUE, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                        LedgerParameterType.MANUAL_VALUATION_OVERRIDE, LedgerParameterType.EVENT_REFERENCE);
    }

    private static LedgerPostingTypeDefinition forex()
    {
        return definition(LedgerPostingType.FOREX, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.EVENT_REFERENCE);
    }

    private static LedgerPostingTypeDefinition right()
    {
        return definition(LedgerPostingType.RIGHT, LedgerParameterType.RIGHT_SECURITY,
                        LedgerParameterType.SOURCE_SECURITY, LedgerParameterType.RATIO_NUMERATOR,
                        LedgerParameterType.RATIO_DENOMINATOR, LedgerParameterType.SUBSCRIPTION_PRICE,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.FRACTION_QUANTITY,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.ROUNDING_MODE,
                        LedgerParameterType.ELECTION_DEADLINE, LedgerParameterType.EX_DATE,
                        LedgerParameterType.RECORD_DATE, LedgerParameterType.EFFECTIVE_DATE,
                        LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition bond()
    {
        return definition(LedgerPostingType.BOND, LedgerParameterType.SOURCE_SECURITY,
                        LedgerParameterType.TARGET_SECURITY, LedgerParameterType.NOMINAL_VALUE,
                        LedgerParameterType.QUOTATION_STYLE, LedgerParameterType.CONVERSION_RATIO,
                        LedgerParameterType.RATIO_NUMERATOR, LedgerParameterType.RATIO_DENOMINATOR,
                        LedgerParameterType.REDEMPTION_PRICE_PERCENT, LedgerParameterType.PARTIAL_REDEMPTION_FACTOR,
                        LedgerParameterType.COUPON_RATE, LedgerParameterType.INTEREST_PERIOD_START,
                        LedgerParameterType.INTEREST_PERIOD_END, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.FAIR_MARKET_VALUE,
                        LedgerParameterType.MANUAL_VALUATION_OVERRIDE, LedgerParameterType.EFFECTIVE_DATE,
                        LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition accruedInterest()
    {
        return definition(LedgerPostingType.ACCRUED_INTEREST, LedgerParameterType.ACCRUED_INTEREST_AMOUNT,
                        LedgerParameterType.COUPON_RATE, LedgerParameterType.INTEREST_PERIOD_START,
                        LedgerParameterType.INTEREST_PERIOD_END, LedgerParameterType.PAYMENT_DATE,
                        LedgerParameterType.SETTLEMENT_DATE, LedgerParameterType.WITHHOLDING_TAX,
                        LedgerParameterType.RECLAIMABLE_TAX, LedgerParameterType.TAX_REASON,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition principalRedemption()
    {
        return definition(LedgerPostingType.PRINCIPAL_REDEMPTION, LedgerParameterType.NOMINAL_VALUE,
                        LedgerParameterType.REDEMPTION_PRICE_PERCENT, LedgerParameterType.PARTIAL_REDEMPTION_FACTOR,
                        LedgerParameterType.SOURCE_SECURITY, LedgerParameterType.CASH_ACCOUNT,
                        LedgerParameterType.EFFECTIVE_DATE, LedgerParameterType.SETTLEMENT_DATE,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.VALUATION_PRICE,
                        LedgerParameterType.FAIR_MARKET_VALUE, LedgerParameterType.MANUAL_VALUATION_OVERRIDE,
                        LedgerParameterType.CORPORATE_ACTION_LEG);
    }

    private static LedgerPostingTypeDefinition definition(LedgerPostingType postingType,
                    LedgerParameterType firstParameterType, LedgerParameterType... rest)
    {
        return LedgerPostingTypeDefinition.of(postingType, SETS.parameterTypes(firstParameterType, rest));
    }

    private static final class SetBuilder
    {
        private EnumSet<LedgerParameterType> parameterTypes(LedgerParameterType first, LedgerParameterType... rest)
        {
            return EnumSet.of(first, rest);
        }
    }
}
