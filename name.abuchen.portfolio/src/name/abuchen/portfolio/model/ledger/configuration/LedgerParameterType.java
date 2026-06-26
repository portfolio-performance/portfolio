package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;

/**
 * Defines stable Ledger parameter codes used by persistence and validation.
 * This is Ledger configuration metadata. Existing persistence codes must stay stable, and
 * normal transaction-editing code should use higher-level write paths.
 *
 * <p>
 * Protobuf stores {@link #getCode()} in {@code PLedgerParameter.typeCode}. The
 * parameter value kind is stored separately, so codes, value kinds, and controlled code
 * domains must stay compatible with already persisted files.
 * </p>
 */
@SuppressWarnings("nls")
public enum LedgerParameterType
{
    EX_DATE("EX_DATE", Scope.GENERAL, ValueKind.LOCAL_DATE_TIME),
    CORPORATE_ACTION_LEG("CORPORATE_ACTION_LEG", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.CORPORATE_ACTION_LEG),
    SOURCE_SECURITY("SOURCE_SECURITY", Scope.CORPORATE_ACTION, ValueKind.SECURITY),
    TARGET_SECURITY("TARGET_SECURITY", Scope.CORPORATE_ACTION, ValueKind.SECURITY),
    RATIO_NUMERATOR("RATIO_NUMERATOR", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    RATIO_DENOMINATOR("RATIO_DENOMINATOR", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    CASH_COMPENSATION_KIND("CASH_COMPENSATION_KIND", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.CASH_COMPENSATION_KIND),
    FEE_REASON("FEE_REASON", Scope.CORPORATE_ACTION, ValueKind.STRING, LedgerParameterCodeDomain.FEE_REASON),
    TAX_REASON("TAX_REASON", Scope.CORPORATE_ACTION, ValueKind.STRING, LedgerParameterCodeDomain.TAX_REASON),
    CORPORATE_ACTION_KIND("CORPORATE_ACTION_KIND", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.CORPORATE_ACTION_KIND),
    CORPORATE_ACTION_SUBTYPE("CORPORATE_ACTION_SUBTYPE", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.CORPORATE_ACTION_SUBTYPE),
    EVENT_REFERENCE("EVENT_REFERENCE", Scope.CORPORATE_ACTION, ValueKind.STRING),
    EVENT_STAGE("EVENT_STAGE", Scope.CORPORATE_ACTION, ValueKind.STRING, LedgerParameterCodeDomain.EVENT_STAGE),
    RECORD_DATE("RECORD_DATE", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    PAYMENT_DATE("PAYMENT_DATE", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    EFFECTIVE_DATE("EFFECTIVE_DATE", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    SETTLEMENT_DATE("SETTLEMENT_DATE", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    ELECTION_DEADLINE("ELECTION_DEADLINE", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    INTEREST_PERIOD_START("INTEREST_PERIOD_START", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    INTEREST_PERIOD_END("INTEREST_PERIOD_END", Scope.CORPORATE_ACTION, ValueKind.LOCAL_DATE),
    RIGHT_SECURITY("RIGHT_SECURITY", Scope.CORPORATE_ACTION, ValueKind.SECURITY),
    SOURCE_ACCOUNT("SOURCE_ACCOUNT", Scope.CORPORATE_ACTION, ValueKind.ACCOUNT),
    TARGET_ACCOUNT("TARGET_ACCOUNT", Scope.CORPORATE_ACTION, ValueKind.ACCOUNT),
    CASH_ACCOUNT("CASH_ACCOUNT", Scope.CORPORATE_ACTION, ValueKind.ACCOUNT),
    SOURCE_PORTFOLIO("SOURCE_PORTFOLIO", Scope.CORPORATE_ACTION, ValueKind.PORTFOLIO),
    TARGET_PORTFOLIO("TARGET_PORTFOLIO", Scope.CORPORATE_ACTION, ValueKind.PORTFOLIO),
    FRACTION_QUANTITY("FRACTION_QUANTITY", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    CONVERSION_RATIO("CONVERSION_RATIO", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    PARTIAL_REDEMPTION_FACTOR("PARTIAL_REDEMPTION_FACTOR", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    COUPON_RATE("COUPON_RATE", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    REDEMPTION_PRICE_PERCENT("REDEMPTION_PRICE_PERCENT", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    SOURCE_COST_PERCENT("SOURCE_COST_PERCENT", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    TARGET_COST_PERCENT("TARGET_COST_PERCENT", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    AFFECTED_SOURCE_QUANTITY("AFFECTED_SOURCE_QUANTITY", Scope.CORPORATE_ACTION, ValueKind.DECIMAL),
    NOMINAL_VALUE("NOMINAL_VALUE", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    SUBSCRIPTION_PRICE("SUBSCRIPTION_PRICE", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    REFERENCE_PRICE("REFERENCE_PRICE", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    CASH_IN_LIEU_AMOUNT("CASH_IN_LIEU_AMOUNT", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    VALUATION_PRICE("VALUATION_PRICE", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    FAIR_MARKET_VALUE("FAIR_MARKET_VALUE", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    ACCRUED_INTEREST_AMOUNT("ACCRUED_INTEREST_AMOUNT", Scope.CORPORATE_ACTION, ValueKind.MONEY),
    FRACTION_TREATMENT("FRACTION_TREATMENT", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.FRACTION_TREATMENT),
    ROUNDING_MODE("ROUNDING_MODE", Scope.CORPORATE_ACTION, ValueKind.STRING, LedgerParameterCodeDomain.ROUNDING_MODE),
    COST_ALLOCATION_METHOD("COST_ALLOCATION_METHOD", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.COST_ALLOCATION_METHOD),
    QUOTATION_STYLE("QUOTATION_STYLE", Scope.CORPORATE_ACTION, ValueKind.STRING,
                    LedgerParameterCodeDomain.QUOTATION_STYLE),
    CASH_IN_LIEU_APPLIED("CASH_IN_LIEU_APPLIED", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    TAXABLE_DISTRIBUTION("TAXABLE_DISTRIBUTION", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    MANUAL_VALUATION_OVERRIDE("MANUAL_VALUATION_OVERRIDE", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    SAME_SECURITY_AS_SOURCE("SAME_SECURITY_AS_SOURCE", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    FRACTION_ROUNDED("FRACTION_ROUNDED", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    RECLAIMABLE_TAX("RECLAIMABLE_TAX", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    WITHHOLDING_TAX("WITHHOLDING_TAX", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    TRANSACTION_TAX("TRANSACTION_TAX", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN),
    STAMP_DUTY("STAMP_DUTY", Scope.CORPORATE_ACTION, ValueKind.BOOLEAN);

    public enum Scope
    {
        GENERAL,
        CORPORATE_ACTION
    }

    private final String code;
    private final Scope scope;
    private final ValueKind expectedValueKind;
    private final LedgerParameterCodeDomain codeDomain;

    private LedgerParameterType(String code, Scope scope, ValueKind expectedValueKind)
    {
        this(code, scope, expectedValueKind, null);
    }

    private LedgerParameterType(String code, Scope scope, ValueKind expectedValueKind,
                    LedgerParameterCodeDomain codeDomain)
    {
        this.code = Objects.requireNonNull(code);
        this.scope = Objects.requireNonNull(scope);
        this.expectedValueKind = Objects.requireNonNull(expectedValueKind);
        this.codeDomain = codeDomain;

        if (codeDomain != null && expectedValueKind != ValueKind.STRING)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_019
                            .message(this + " must use STRING for controlled code domain " + codeDomain)); //$NON-NLS-1$
    }

    public String getCode()
    {
        return code;
    }

    public static LedgerParameterType fromCode(String code)
    {
        for (LedgerParameterType type : values())
            if (type.code.equals(code))
                return type;

        throw new IllegalArgumentException(
                        LedgerDiagnosticCode.LEDGER_CORE_020.message("Unknown LedgerParameterType code: " + code)); //$NON-NLS-1$
    }

    public Scope getScope()
    {
        return scope;
    }

    public ValueKind getExpectedValueKind()
    {
        return expectedValueKind;
    }

    public Class<?> getExpectedValueType()
    {
        return expectedValueKind.getValueType();
    }

    public Class<?> getExpectedJavaType()
    {
        return getExpectedValueType();
    }

    public boolean isGeneral()
    {
        return scope == Scope.GENERAL;
    }

    public boolean isCorporateAction()
    {
        return scope == Scope.CORPORATE_ACTION;
    }

    public boolean supportsValueKind(ValueKind valueKind)
    {
        return expectedValueKind == valueKind;
    }

    public void requireValueKind(ValueKind valueKind)
    {
        if (!supportsValueKind(valueKind))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_021
                            .message(this + " does not support " + valueKind + "; expected " + expectedValueKind)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean supportsValue(Object value)
    {
        return expectedValueKind.supportsValue(value);
    }

    public boolean hasCodeDomain()
    {
        return codeDomain != null;
    }

    public boolean isControlledCode()
    {
        return hasCodeDomain();
    }

    public LedgerParameterCodeDomain getCodeDomain()
    {
        return codeDomain;
    }

    public boolean supportsCode(String code)
    {
        return codeDomain == null || codeDomain.allows(code);
    }

    public boolean isReferenceParameter()
    {
        return switch (expectedValueKind)
        {
            case ACCOUNT, PORTFOLIO, SECURITY -> true;
            default -> false;
        };
    }

    public boolean isDateParameter()
    {
        return expectedValueKind == ValueKind.LOCAL_DATE || expectedValueKind == ValueKind.LOCAL_DATE_TIME;
    }

    public boolean isBooleanParameter()
    {
        return expectedValueKind == ValueKind.BOOLEAN;
    }
}
