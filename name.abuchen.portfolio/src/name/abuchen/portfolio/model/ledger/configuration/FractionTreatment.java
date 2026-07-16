package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the fraction treatment Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum FractionTreatment implements LedgerCode
{
    NONE("NONE"),
    CASH_IN_LIEU("CASH_IN_LIEU"),
    ROUND_DOWN("ROUND_DOWN"),
    ROUND_UP("ROUND_UP"),
    DROP("DROP"),
    OTHER("OTHER");

    private final String code;

    private FractionTreatment(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.FRACTION_TREATMENT;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
