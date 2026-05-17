package name.abuchen.portfolio.util;

import java.util.function.Supplier;

/**
 * Computes a value on first access via the given {@link Supplier} and caches
 * it for all subsequent calls. Not thread-safe.
 */
public final class LazyValue<V>
{
    private V value;
    private final Supplier<V> computeFunction;

    public LazyValue(Supplier<V> computeFunction)
    {
        this.computeFunction = computeFunction;
    }

    public V get()
    {
        if (value == null)
            value = computeFunction.get();
        return value;
    }
}
