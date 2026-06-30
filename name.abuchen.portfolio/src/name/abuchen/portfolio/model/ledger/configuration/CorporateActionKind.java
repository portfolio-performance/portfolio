package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Optional;

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
    SPIN_OFF("SPIN_OFF", LedgerEntryType.SPIN_OFF),
    STOCK_DIVIDEND("STOCK_DIVIDEND", LedgerEntryType.STOCK_DIVIDEND),
    BONUS_ISSUE("BONUS_ISSUE", LedgerEntryType.BONUS_ISSUE),
    RIGHTS_DISTRIBUTION("RIGHTS_DISTRIBUTION", LedgerEntryType.RIGHTS_DISTRIBUTION),
    BOND_CONVERSION("BOND_CONVERSION", LedgerEntryType.BOND_CONVERSION),
    OTHER("OTHER");

    private final String code;
    private final LedgerEntryType relatedEntryType;

    private CorporateActionKind(String code)
    {
        this(code, null);
    }

    private CorporateActionKind(String code, LedgerEntryType relatedEntryType)
    {
        this.code = code;
        this.relatedEntryType = relatedEntryType;
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

    public Optional<LedgerEntryType> getRelatedEntryType()
    {
        return Optional.ofNullable(relatedEntryType);
    }
}
