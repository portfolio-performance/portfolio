package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;

/**
 * Carries native parameter value input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler converts it into a
 * {@code LedgerParameter} with a stable parameter type code and a matching value kind.
 * </p>
 */
final class NativeParameterValue
{
    private final LedgerParameterType type;
    private final Object value;

    NativeParameterValue(LedgerParameterType type, Object value)
    {
        this.type = Objects.requireNonNull(type);
        this.value = Objects.requireNonNull(value);
    }

    LedgerParameterType getType()
    {
        return type;
    }

    Object getValue()
    {
        return value;
    }
}
