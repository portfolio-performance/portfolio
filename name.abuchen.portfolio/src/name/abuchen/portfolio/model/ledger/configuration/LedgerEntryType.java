package name.abuchen.portfolio.model.ledger.configuration;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Defines stable Ledger type codes used by persistence and validation. This is Ledger configuration metadata; normal
 * transaction-editing code should use higher-level write paths.
 */
public enum LedgerEntryType
{
    DEPOSIT(Shape.LEGACY_FIXED),
    REMOVAL(Shape.LEGACY_FIXED),
    INTEREST(Shape.LEGACY_FIXED),
    INTEREST_CHARGE(Shape.LEGACY_FIXED),
    FEES(Shape.LEGACY_FIXED),
    FEES_REFUND(Shape.LEGACY_FIXED),
    TAXES(Shape.LEGACY_FIXED),
    TAX_REFUND(Shape.LEGACY_FIXED),
    DIVIDENDS(Shape.LEGACY_FIXED),
    BUY(Shape.LEGACY_FIXED),
    SELL(Shape.LEGACY_FIXED),
    CASH_TRANSFER(Shape.LEGACY_FIXED),
    SECURITY_TRANSFER(Shape.LEGACY_FIXED),
    DELIVERY_INBOUND(Shape.LEGACY_FIXED),
    DELIVERY_OUTBOUND(Shape.LEGACY_FIXED),
    SPIN_OFF(Shape.LEDGER_NATIVE_TARGETED),
    STOCK_DIVIDEND(Shape.LEDGER_NATIVE_TARGETED),
    BONUS_ISSUE(Shape.LEDGER_NATIVE_TARGETED),
    RIGHTS_DISTRIBUTION(Shape.LEDGER_NATIVE_TARGETED),
    BOND_CONVERSION(Shape.LEDGER_NATIVE_TARGETED);

    private enum Shape
    {
        LEGACY_FIXED,
        LEDGER_NATIVE_TARGETED
    }

    private final String code;

    private final Shape shape;

    private LedgerEntryType(Shape shape)
    {
        this.code = name();
        this.shape = shape;
    }

    public String getCode()
    {
        return code;
    }

    public static LedgerEntryType fromCode(String code)
    {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_017.message("Missing LedgerEntryType code")); //$NON-NLS-1$

        for (LedgerEntryType type : values())
            if (type.code.equals(code))
                return type;

        throw new IllegalArgumentException(
                        LedgerDiagnosticCode.LEDGER_CORE_017.message("Unknown LedgerEntryType code: " + code)); //$NON-NLS-1$
    }

    public boolean isLegacyFixedShape()
    {
        return shape == Shape.LEGACY_FIXED;
    }

    public boolean isLedgerNativeTargeted()
    {
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }

    public boolean requiresTargetedDerivedDescriptors()
    {
        return isLedgerNativeTargeted();
    }

    public boolean usesSignedTargetedProjectionFacts()
    {
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }
}
