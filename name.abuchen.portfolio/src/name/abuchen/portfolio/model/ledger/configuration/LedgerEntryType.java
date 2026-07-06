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
    CORPORATE_ACTION("CORPORATE_ACTION", Shape.LEDGER_NATIVE_TARGETED);

    private enum Shape
    {
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
                                LedgerDiagnosticCode.LEDGER_CORE_014.message("Blank LedgerEntryType code"));

            if (!codes.add(type.code))
                throw new IllegalStateException(
                                LedgerDiagnosticCode.LEDGER_CORE_014.message("Duplicate LedgerEntryType code: "
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
                            LedgerDiagnosticCode.LEDGER_CORE_014.message("Missing LedgerEntryType code"));

        for (LedgerEntryType type : values())
            if (type.code.equals(code))
                return type;

        throw new IllegalArgumentException(
                        LedgerDiagnosticCode.LEDGER_CORE_014.message("Unknown LedgerEntryType code: " + code));
    }

    public boolean isLegacyFixedShape()
    {
        return false;
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
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }

    public boolean usesSignedTargetedProjectionFacts()
    {
        return shape == Shape.LEDGER_NATIVE_TARGETED;
    }
}
