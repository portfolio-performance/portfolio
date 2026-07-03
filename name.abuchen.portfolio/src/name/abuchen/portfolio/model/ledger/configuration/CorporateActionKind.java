package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Optional;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;

/**
 * Defines the corporate action kind Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * The enum object is not persisted directly. Its {@link #getCode()} value can be stored as a
 * string Ledger parameter value for parameters in the matching code domain.
 * </p>
 */
@SuppressWarnings("nls")
public enum CorporateActionKind implements LedgerCode
{
    STOCK_DIVIDEND("STOCK_DIVIDEND"),
    SPIN_OFF("SPIN_OFF"),
    BONUS_ISSUE("BONUS_ISSUE"),
    RIGHTS_DISTRIBUTION("RIGHTS_DISTRIBUTION"),
    COUPON_PAYMENT("COUPON_PAYMENT"),
    PIK_INTEREST("PIK_INTEREST"),
    DEFAULTED_INTEREST("DEFAULTED_INTEREST"),
    MATURITY("MATURITY"),
    PARTIAL_REDEMPTION("PARTIAL_REDEMPTION"),
    CALL("CALL"),
    PUT("PUT"),
    CONVERSION("CONVERSION"),
    EXCHANGE("EXCHANGE"),
    RESTRUCTURING("RESTRUCTURING"),
    DEFAULT("DEFAULT");

    private final String code;

    private CorporateActionKind(String code)
    {
        this.code = code;
    }

    @Override
    public LedgerParameterCodeDomain getDomain()
    {
        return LedgerParameterCodeDomain.CORPORATE_ACTION_KIND;
    }

    @Override
    public String getCode()
    {
        return code;
    }

    public static Optional<CorporateActionKind> fromCode(String code)
    {
        if (code == null || code.isBlank())
            return Optional.empty();

        for (var kind : values())
            if (kind.code.equals(code))
                return Optional.of(kind);

        return Optional.empty();
    }

    public static Optional<CorporateActionKind> fromEntry(LedgerEntry entry)
    {
        if (entry == null)
            return Optional.empty();

        return entry.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == LedgerParameterType.CORPORATE_ACTION_KIND) //
                        .map(LedgerParameter::getValue) //
                        .filter(String.class::isInstance) //
                        .map(String.class::cast) //
                        .map(CorporateActionKind::fromCode) //
                        .filter(Optional::isPresent) //
                        .map(Optional::get) //
                        .findFirst();
    }
}
