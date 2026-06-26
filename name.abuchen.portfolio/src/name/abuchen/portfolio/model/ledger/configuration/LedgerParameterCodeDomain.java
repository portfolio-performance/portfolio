package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the parameter code domain Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * A domain is selected by {@link LedgerParameterType}; it is not stored as its own Ledger
 * fact. The allowed codes are persisted only as string parameter values, so code strings
 * must remain stable after they have been written to files.
 * </p>
 */
public enum LedgerParameterCodeDomain
{
    CORPORATE_ACTION_LEG,
    CORPORATE_ACTION_KIND,
    CORPORATE_ACTION_SUBTYPE,
    EVENT_STAGE,
    CASH_COMPENSATION_KIND,
    FRACTION_TREATMENT,
    ROUNDING_MODE,
    COST_ALLOCATION_METHOD,
    QUOTATION_STYLE,
    FEE_REASON,
    TAX_REASON;

    public List<String> getAllowedCodes()
    {
        return switch (this)
        {
            case CORPORATE_ACTION_LEG -> codes(CorporateActionLeg.values());
            case CORPORATE_ACTION_KIND -> codes(CorporateActionKind.values());
            case CORPORATE_ACTION_SUBTYPE -> codes(CorporateActionSubtype.values());
            case EVENT_STAGE -> codes(EventStage.values());
            case CASH_COMPENSATION_KIND -> codes(CashCompensationKind.values());
            case FRACTION_TREATMENT -> codes(FractionTreatment.values());
            case ROUNDING_MODE -> codes(RoundingModeCode.values());
            case COST_ALLOCATION_METHOD -> codes(CostAllocationMethod.values());
            case QUOTATION_STYLE -> codes(QuotationStyle.values());
            case FEE_REASON -> codes(FeeReason.values());
            case TAX_REASON -> codes(TaxReason.values());
        };
    }

    public boolean allows(String code)
    {
        return getAllowedCodes().contains(code);
    }

    private static List<String> codes(LedgerCode... codes)
    {
        return Arrays.stream(codes).map(LedgerCode::getCode).toList();
    }
}
