package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the corporate action subtype Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum CorporateActionSubtype implements LedgerCode
{
    STANDARD("STANDARD"),
    OPTIONAL("OPTIONAL"),
    MANDATORY("MANDATORY"),
    CASH_AND_STOCK("CASH_AND_STOCK"),
    OTHER("OTHER");

    private final String code;

    private CorporateActionSubtype(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.CORPORATE_ACTION_SUBTYPE;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
