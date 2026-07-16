package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the fee reason Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum FeeReason implements LedgerCode
{
    BROKER_FEE("BROKER_FEE"),
    EXCHANGE_FEE("EXCHANGE_FEE"),
    CORPORATE_ACTION_FEE("CORPORATE_ACTION_FEE"),
    STAMP_DUTY("STAMP_DUTY"),
    OTHER("OTHER");

    private final String code;

    private FeeReason(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.FEE_REASON;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
