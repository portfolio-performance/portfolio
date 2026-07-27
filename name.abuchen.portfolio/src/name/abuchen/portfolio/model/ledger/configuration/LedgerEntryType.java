package name.abuchen.portfolio.model.ledger.configuration;

/**
 * The type of a ledger entry, for example a buy, a dividend, or a cash
 * transfer.
 * <p>
 * Every type has a stable code. Codes are part of the REST API: they must never
 * be changed or reused for a different meaning.
 */
public enum LedgerEntryType
{
    DEPOSIT, //
    REMOVAL, //
    INTEREST, //
    INTEREST_CHARGE, //
    FEES, //
    FEES_REFUND, //
    TAXES, //
    TAX_REFUND, //
    DIVIDENDS, //
    BUY, //
    SELL, //
    CASH_TRANSFER, //
    SECURITY_TRANSFER, //
    DELIVERY_INBOUND, //
    DELIVERY_OUTBOUND; //

    public String getCode()
    {
        return name();
    }

    public static LedgerEntryType fromCode(String code)
    {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Missing LedgerEntryType code"); //$NON-NLS-1$

        for (LedgerEntryType type : values())
            if (type.getCode().equals(code))
                return type;

        throw new IllegalArgumentException("Unknown LedgerEntryType code: " + code); //$NON-NLS-1$
    }
}
