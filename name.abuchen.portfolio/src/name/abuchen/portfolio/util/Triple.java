package name.abuchen.portfolio.util;

import java.util.Objects;

/**
 * An immutable triple consisting of three non-null <code>Object</code>
 * elements.
 */
public class Triple<A, B, C>
{
    private final A first;
    private final B second;
    private final C third;

    public Triple(A first, B second, C third)
    {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
        this.third = Objects.requireNonNull(third);
    }

    public A getFirst()
    {
        return first;
    }

    public B getSecond()
    {
        return second;
    }

    public C getThird()
    {
        return third;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(first, second, third);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
        return Objects.equals(first, other.first) && Objects.equals(second, other.second)
                        && Objects.equals(third, other.third);
    }
}
