package name.abuchen.portfolio.model.ledger.configuration;

import java.util.HashSet;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Defines stable Ledger type codes used by persistence and validation. This is Ledger configuration metadata; normal
 * transaction-editing code should use higher-level write paths.
 */
@SuppressWarnings("nls")
public enum LedgerEntryType
{
    DEPOSIT("DEPOSIT", Shape.LEGACY_FIXED),
    REMOVAL("REMOVAL", Shape.LEGACY_FIXED),
    INTEREST("INTEREST", Shape.LEGACY_FIXED),
    INTEREST_CHARGE("INTEREST_CHARGE", Shape.LEGACY_FIXED),
    FEES("FEES", Shape.LEGACY_FIXED),
    FEES_REFUND("FEES_REFUND", Shape.LEGACY_FIXED),
    TAXES("TAXES", Shape.LEGACY_FIXED),
    TAX_REFUND("TAX_REFUND", Shape.LEGACY_FIXED),
    DIVIDENDS("DIVIDENDS", Shape.LEGACY_FIXED),
    BUY("BUY", Shape.LEGACY_FIXED),
    SELL("SELL", Shape.LEGACY_FIXED),
    CASH_TRANSFER("CASH_TRANSFER", Shape.LEGACY_FIXED),
    SECURITY_TRANSFER("SECURITY_TRANSFER", Shape.LEGACY_FIXED),
    DELIVERY_INBOUND("DELIVERY_INBOUND", Shape.LEGACY_FIXED),
    DELIVERY_OUTBOUND("DELIVERY_OUTBOUND", Shape.LEGACY_FIXED),
    SPIN_OFF("SPIN_OFF", Shape.LEDGER_NATIVE_TARGETED);

    private enum Shape
    {
        LEGACY_FIXED,
        LEDGER_NATIVE_TARGETED
    }

    private final String code;

    private final Shape shape;

    static
    {
        var codes = new HashSet<String>();

        for (LedgerEntryType type : values())
        {
            if (type.code.isBlank())
                throw new IllegalStateException(
                                LedgerDiagnosticCode.LEDGER_CORE_017.message("Blank LedgerEntryType code"));

            if (!codes.add(type.code))
                throw new IllegalStateException(
                                LedgerDiagnosticCode.LEDGER_CORE_017.message("Duplicate LedgerEntryType code: "
                                                + type.code));
        }
    }

    private LedgerEntryType(String code, Shape shape)
    {
        this.code = Objects.requireNonNull(code);
        this.shape = Objects.requireNonNull(shape);
    }

    public String getCode()
    {
        return code;
    }

    public static LedgerEntryType fromCode(String code)
    {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_017.message("Missing LedgerEntryType code"));

        for (LedgerEntryType type : values())
            if (type.code.equals(code))
                return type;

        throw new IllegalArgumentException(
                        LedgerDiagnosticCode.LEDGER_CORE_017.message("Unknown LedgerEntryType code: " + code));
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

    public boolean supportsDerivedDescriptors()
    {
        return shape == Shape.LEGACY_FIXED || shape == Shape.LEDGER_NATIVE_TARGETED;
    }

    public boolean usesSignedTargetedProjectionFacts()
    {
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }
}
