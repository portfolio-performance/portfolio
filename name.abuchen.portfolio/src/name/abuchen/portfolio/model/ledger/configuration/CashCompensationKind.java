package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the cash compensation kind Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum CashCompensationKind implements LedgerCode
{
    CASH_IN_LIEU("CASH_IN_LIEU"),
    FRACTIONAL_SHARE_COMPENSATION("FRACTIONAL_SHARE_COMPENSATION"),
    ROUNDING_COMPENSATION("ROUNDING_COMPENSATION"),
    OTHER("OTHER");

    private final String code;

    private CashCompensationKind(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.CASH_COMPENSATION_KIND;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
