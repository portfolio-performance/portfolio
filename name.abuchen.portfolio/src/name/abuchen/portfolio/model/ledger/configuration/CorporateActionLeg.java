package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the corporate action leg Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum CorporateActionLeg implements LedgerCode
{
    SOURCE_SECURITY("SOURCE_SECURITY"),
    TARGET_SECURITY("TARGET_SECURITY"),
    DISTRIBUTED_SECURITY("DISTRIBUTED_SECURITY"),
    RIGHT_SECURITY("RIGHT_SECURITY"),
    CASH_COMPENSATION("CASH_COMPENSATION"),
    CASH_IN_LIEU("CASH_IN_LIEU"),
    FEE("FEE"),
    TAX("TAX"),
    ACCRUED_INTEREST("ACCRUED_INTEREST"),
    PRINCIPAL("PRINCIPAL"),
    REDEMPTION("REDEMPTION"),
    CONVERSION_SOURCE("CONVERSION_SOURCE"),
    CONVERSION_TARGET("CONVERSION_TARGET"),
    OTHER("OTHER");

    private final String code;

    private CorporateActionLeg(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.CORPORATE_ACTION_LEG;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
