package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the rounding mode code Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum RoundingModeCode implements LedgerCode
{
    NONE("NONE"),
    FLOOR("FLOOR"),
    CEILING("CEILING"),
    HALF_UP("HALF_UP"),
    HALF_EVEN("HALF_EVEN"),
    OTHER("OTHER");

    private final String code;

    private RoundingModeCode(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.ROUNDING_MODE;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
