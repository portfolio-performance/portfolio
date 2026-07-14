package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;

/**
 * The kind of typed fact a parameter carries; currently only the ex-date of a
 * dividend. Each type fixes the value kind its value must have. Codes are part of
 * the REST API: they must never be changed or reused for a different meaning.
 */
public enum LedgerParameterType
{
    EX_DATE(ValueKind.LOCAL_DATE_TIME);

    private final ValueKind expectedValueKind;

    private LedgerParameterType(ValueKind expectedValueKind)
    {
        this.expectedValueKind = Objects.requireNonNull(expectedValueKind);
    }

    public String getCode()
    {
        return name();
    }

    public static LedgerParameterType fromCode(String code)
    {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Missing LedgerParameterType code"); //$NON-NLS-1$

        for (LedgerParameterType type : values())
            if (type.getCode().equals(code))
                return type;

        throw new IllegalArgumentException("Unknown LedgerParameterType code: " + code); //$NON-NLS-1$
    }

    public ValueKind getExpectedValueKind()
    {
        return expectedValueKind;
    }

    public Class<?> getExpectedValueType()
    {
        return expectedValueKind.getValueType();
    }

    public boolean supportsValueKind(ValueKind valueKind)
    {
        return expectedValueKind == valueKind;
    }

    public void requireValueKind(ValueKind valueKind)
    {
        if (!supportsValueKind(valueKind))
            throw new IllegalArgumentException(
                            this + " does not support " + valueKind + "; expected " + expectedValueKind); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean supportsValue(Object value)
    {
        return expectedValueKind.supportsValue(value);
    }
}
