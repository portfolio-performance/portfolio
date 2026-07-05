package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines persisted Corporate Action basis treatment status codes.
 */
@SuppressWarnings("nls")
public enum CorporateActionBasisStatus implements LedgerCode
{
    NOT_APPLICABLE("NOT_APPLICABLE"),
    UNKNOWN("UNKNOWN"),
    PROVIDED("PROVIDED");

    private final String code;

    private CorporateActionBasisStatus(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.CORPORATE_ACTION_BASIS_STATUS;
    }

    @Override
    public String getCode()
    {
        return code;
    }

    public static CorporateActionBasisStatus valueOfCode(String code)
    {
        for (var value : values())
            if (value.code.equals(code))
                return value;

        throw new IllegalArgumentException("Unknown CorporateActionBasisStatus code: " + code); //$NON-NLS-1$
    }
}
