package name.abuchen.portfolio.model.ledger;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Describes one field-level metadata edit for a persisted Ledger entry.
 * This is internal edit data used by Ledger mutation support. It does not mutate Ledger
 * truth by itself.
 */
public final class LedgerFieldEdit<T>
{
    public enum State
    {
        OMITTED,
        SET,
        CLEAR
    }

    private static final LedgerFieldEdit<?> OMITTED = new LedgerFieldEdit<>(State.OMITTED, null);
    private static final LedgerFieldEdit<?> CLEAR = new LedgerFieldEdit<>(State.CLEAR, null);

    private final State state;
    private final T value;

    private LedgerFieldEdit(State state, T value)
    {
        this.state = state;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> LedgerFieldEdit<T> omitted()
    {
        return (LedgerFieldEdit<T>) OMITTED;
    }

    public static <T> LedgerFieldEdit<T> set(T value)
    {
        return new LedgerFieldEdit<>(State.SET, Objects.requireNonNull(value));
    }

    @SuppressWarnings("unchecked")
    public static <T> LedgerFieldEdit<T> clear()
    {
        return (LedgerFieldEdit<T>) CLEAR;
    }

    public State getState()
    {
        return state;
    }

    public T getValue()
    {
        if (state != State.SET)
            throw new IllegalStateException(
                            LedgerDiagnosticCode.LEDGER_CORE_006.message("Only set edits have a value")); //$NON-NLS-1$

        return value;
    }

    public boolean isOmitted()
    {
        return state == State.OMITTED;
    }

    public boolean isSet()
    {
        return state == State.SET;
    }

    public boolean isClear()
    {
        return state == State.CLEAR;
    }
}
