package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the quotation style Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum QuotationStyle implements LedgerCode
{
    UNIT("UNIT"),
    PERCENT("PERCENT"),
    NOMINAL("NOMINAL"),
    OTHER("OTHER");

    private final String code;

    private QuotationStyle(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.QUOTATION_STYLE;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
