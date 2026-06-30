package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the tax reason Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum TaxReason implements LedgerCode
{
    WITHHOLDING_TAX("WITHHOLDING_TAX"),
    CAPITAL_GAINS_TAX("CAPITAL_GAINS_TAX"),
    TRANSACTION_TAX("TRANSACTION_TAX"),
    STAMP_DUTY("STAMP_DUTY"),
    RECLAIMABLE_TAX("RECLAIMABLE_TAX"),
    OTHER("OTHER");

    private final String code;

    private TaxReason(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.TAX_REASON;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
