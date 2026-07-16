package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines persisted Corporate Action basis treatment method codes.
 */
@SuppressWarnings("nls")
public enum CorporateActionBasisMethod implements LedgerCode
{
    UNSPECIFIED("UNSPECIFIED"),
    PERCENTAGE_ALLOCATION("PERCENTAGE_ALLOCATION"),
    AMOUNT_ALLOCATION("AMOUNT_ALLOCATION"),
    REFERENCE_PRICE_RATIO("REFERENCE_PRICE_RATIO"),
    FAIR_MARKET_VALUE_RATIO("FAIR_MARKET_VALUE_RATIO"),
    MANUAL_OVERRIDE("MANUAL_OVERRIDE");

    private final String code;

    private CorporateActionBasisMethod(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.CORPORATE_ACTION_BASIS_METHOD;
    }

    @Override
    public String getCode()
    {
        return code;
    }

    public static CorporateActionBasisMethod valueOfCode(String code)
    {
        for (var value : values())
            if (value.code.equals(code))
                return value;

        throw new IllegalArgumentException("Unknown CorporateActionBasisMethod code: " + code); //$NON-NLS-1$
    }
}
