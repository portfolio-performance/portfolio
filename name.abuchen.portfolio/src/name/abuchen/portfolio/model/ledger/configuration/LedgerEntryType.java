package name.abuchen.portfolio.model.ledger.configuration;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Defines stable Ledger type codes used by persistence and validation.
 * This is Ledger configuration metadata. Existing persistence ids must stay stable, and
 * normal transaction-editing code should use higher-level write paths.
 *
 * <p>
 * Protobuf stores {@link #getProtobufId()} in {@code PLedgerEntry.typeId}. Existing ids must
 * never be changed or reused, and new entry types must receive a new stable id.
 * </p>
 */
public enum LedgerEntryType
{
    DEPOSIT(1, Shape.LEGACY_FIXED),
    REMOVAL(2, Shape.LEGACY_FIXED),
    INTEREST(3, Shape.LEGACY_FIXED),
    INTEREST_CHARGE(4, Shape.LEGACY_FIXED),
    FEES(5, Shape.LEGACY_FIXED),
    FEES_REFUND(6, Shape.LEGACY_FIXED),
    TAXES(7, Shape.LEGACY_FIXED),
    TAX_REFUND(8, Shape.LEGACY_FIXED),
    DIVIDENDS(9, Shape.LEGACY_FIXED),
    BUY(10, Shape.LEGACY_FIXED),
    SELL(11, Shape.LEGACY_FIXED),
    CASH_TRANSFER(12, Shape.LEGACY_FIXED),
    SECURITY_TRANSFER(13, Shape.LEGACY_FIXED),
    DELIVERY_INBOUND(14, Shape.LEGACY_FIXED),
    DELIVERY_OUTBOUND(15, Shape.LEGACY_FIXED),
    SPIN_OFF(16, Shape.LEDGER_NATIVE_TARGETED),
    STOCK_DIVIDEND(17, Shape.LEDGER_NATIVE_TARGETED),
    BONUS_ISSUE(18, Shape.LEDGER_NATIVE_TARGETED),
    RIGHTS_DISTRIBUTION(19, Shape.LEDGER_NATIVE_TARGETED),
    BOND_CONVERSION(20, Shape.LEDGER_NATIVE_TARGETED);

    private enum Shape
    {
        LEGACY_FIXED,
        LEDGER_NATIVE_TARGETED
    }

    // Protobuf persistence ID.
    // Never change existing IDs.
    // Never reuse removed IDs.
    // New enum constants must receive a new unique ID.
    private final int protobufId;

    private final Shape shape;

    private LedgerEntryType(int protobufId, Shape shape)
    {
        this.protobufId = protobufId;
        this.shape = shape;
    }

    public int getProtobufId()
    {
        return protobufId;
    }

    public static LedgerEntryType fromProtobufId(int id)
    {
        for (LedgerEntryType type : values())
            if (type.protobufId == id)
                return type;

        throw new IllegalArgumentException(
                        LedgerDiagnosticCode.LEDGER_CORE_017.message("Unknown LedgerEntryType protobuf ID: " + id)); //$NON-NLS-1$
    }

    public boolean isLegacyFixedShape()
    {
        return shape == Shape.LEGACY_FIXED;
    }

    public boolean isLedgerNativeTargeted()
    {
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }

    public boolean requiresTargetedProjectionRefs()
    {
        return isLedgerNativeTargeted();
    }

    public boolean usesSignedTargetedProjectionFacts()
    {
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }
}
