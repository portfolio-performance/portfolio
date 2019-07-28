package name.abuchen.portfolio.ui.views.dataseries;

import java.util.Objects;

import name.abuchen.portfolio.util.Interval;

public class CacheKey
{
    private final Object object;
    private final Interval interval;

    public CacheKey(Object object, Interval interval)
    {
        this.object = Objects.requireNonNull(object);
        this.interval = Objects.requireNonNull(interval);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(object, interval);
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

        CacheKey other = (CacheKey) obj;
        if (!object.equals(other.object))
            return false;
        if (!interval.equals(other.interval)) // NOSONAR
            return false;
        return true;
    }
}