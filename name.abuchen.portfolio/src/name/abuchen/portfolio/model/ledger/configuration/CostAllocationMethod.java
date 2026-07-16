package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the cost allocation method Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum CostAllocationMethod implements LedgerCode
{
    NONE("NONE"),
    FMV_RATIO("FMV_RATIO"),
    MANUAL_PERCENTAGE("MANUAL_PERCENTAGE"),
    ZERO_COST_TARGET("ZERO_COST_TARGET"),
    CARRY_OVER("CARRY_OVER"),
    OTHER("OTHER");

    private final String code;

    private CostAllocationMethod(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.COST_ALLOCATION_METHOD;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}
